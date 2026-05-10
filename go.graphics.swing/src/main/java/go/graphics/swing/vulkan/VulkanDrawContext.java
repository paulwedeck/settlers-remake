/*******************************************************************************
 * Copyright (c) 2019
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package go.graphics.swing.vulkan;

import go.graphics.ImageData;
import go.graphics.swing.vulkan.memory.AbstractVulkanBuffer;
import go.graphics.swing.vulkan.memory.EVulkanBufferUsage;
import go.graphics.swing.vulkan.memory.EVulkanMemoryType;
import go.graphics.swing.vulkan.memory.VulkanBufferHandle;
import go.graphics.swing.vulkan.memory.VulkanImage;
import go.graphics.swing.vulkan.memory.VulkanMemoryManager;
import go.graphics.swing.vulkan.memory.VulkanMultiBufferHandle;
import go.graphics.swing.vulkan.pipeline.EVulkanPipelineType;
import go.graphics.swing.vulkan.pipeline.VulkanPipelineManager;
import java.awt.Dimension;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCopy;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkBufferMemoryBarrier;
import org.lwjgl.vulkan.VkClearAttachment;
import org.lwjgl.vulkan.VkClearRect;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import go.graphics.AbstractColor;
import go.graphics.BackgroundDrawHandle;
import go.graphics.BufferHandle;
import go.graphics.EPrimitiveType;
import go.graphics.ETextureType;
import go.graphics.GLDrawContext;
import go.graphics.ManagedHandle;
import go.graphics.MultiDrawHandle;
import go.graphics.TextureHandle;
import go.graphics.UnifiedDrawHandle;
import go.graphics.VkDrawContext;
import go.graphics.swing.text.LWJGLTextDrawer;

import org.lwjgl.system.Platform;
import org.lwjgl.vulkan.VkExtensionProperties;

import static org.lwjgl.vulkan.KHRPortabilitySubset.VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanDrawContext extends GLDrawContext implements VkDrawContext {

	private VkDevice device = null;
	private VkPhysicalDevice physicalDevice;

	private final AbstractVulkanOutput output;
	private VkInstance instance;

	private int fbWidth;
	private int fbHeight;

	private long commandPool;
	private VkCommandBuffer graphCommandBuffer = null;
	private VkCommandBuffer memCommandBuffer = null;
	private VkCommandBuffer fbCommandBuffer = null;

	private long renderPass;

	private VulkanDescriptorPool universalDescPool = null;
	private VulkanDescriptorPool textureDescPool = null;
	private VulkanDescriptorPool multiDescPool = null;

	public VulkanDescriptorSetLayout textureDescLayout = null;
	public VulkanDescriptorSetLayout multiDescLayout = null;

	final VulkanMemoryManager memoryManager;
	private VulkanPipelineManager pipelineManager;
	final QueueManager queueManager;

	final long[] samplers = new long[ETextureType.values().length];

	private final Semaphore resourceMutex = new Semaphore(1);
	private final Semaphore closeMutex = new Semaphore(1);

	protected final List<VulkanTextureHandle> textures = new ArrayList<>();

	// Resources whose destruction has been deferred until the GPU is known to be
	// idle on the work that referenced them. Drained at the start of every frame
	// once the per-frame fence has been awaited (see startFrame -> drainPendingDestroys()).
	private final java.util.Deque<Runnable> pendingDestroys = new java.util.ArrayDeque<>();

	private float guiScale;

	/**
	 * Queue a resource destruction so it happens at the start of a future frame,
	 * after we have waited on the per-frame fence and know that the GPU is no
	 * longer executing the command buffer that may have referenced the resource.
	 * Safe to call at any point during command-buffer recording.
	 */
	public void deferDestroy(Runnable destroyTask) {
		pendingDestroys.add(destroyTask);
	}

	private void drainPendingDestroys() {
		while (!pendingDestroys.isEmpty()) {
			Runnable task = pendingDestroys.poll();
			try {
				task.run();
			} catch (Throwable t) {
				System.err.println("[VK] deferred destroy failed: " + t);
			}
		}
	}

	private static boolean physicalDeviceSupportsPortabilitySubset(MemoryStack stack, VkPhysicalDevice physicalDevice) {
		IntBuffer count = stack.ints(0);
		if(vkEnumerateDeviceExtensionProperties(physicalDevice, (CharSequence) null, count, null) != VK_SUCCESS) return false;
		int n = count.get(0);
		if(n == 0) return false;
		VkExtensionProperties.Buffer props = VkExtensionProperties.malloc(n, stack);
		if(vkEnumerateDeviceExtensionProperties(physicalDevice, (CharSequence) null, count, props) != VK_SUCCESS) return false;
		for(int i = 0; i < n; i++) {
			if(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME.equals(props.get(i).extensionNameString())) return true;
		}
		return false;
	}

	public VulkanDrawContext(VkInstance instance, AbstractVulkanOutput output, float guiScale) {
		this.instance = instance;
		this.guiScale = guiScale;
		this.output = output;

		try(MemoryStack stack = MemoryStack.stackPush()) {
			VkPhysicalDevice[] allPhysicalDevices = VulkanUtils.listPhysicalDevices(stack, instance);
			physicalDevice = VulkanUtils.findPhysicalDevice(allPhysicalDevices);

			VkPhysicalDeviceProperties props = VkPhysicalDeviceProperties.calloc(stack);
			vkGetPhysicalDeviceProperties(physicalDevice, props);
			maxTextureSize = props.limits().maxImageDimension2D();
			maxUniformBlockSize = Integer.MAX_VALUE;

			queueManager = new QueueManager(stack, this, physicalDevice, output.getPresentQueueCond(physicalDevice));

			if(!queueManager.hasGraphicsSupport()) throw new Error("Could not find any graphics queue.");
			if(!queueManager.hasPresentSupport() && output.needsPresentQueue()) throw new Error("Could not find any present queue.");

			// device extensions
			List<String> deviceExtensions = new ArrayList<>();
			if(queueManager.hasPresentSupport()) {
				deviceExtensions.add(VK_KHR_SWAPCHAIN_EXTENSION_NAME);
			}
			if(Platform.get() == Platform.MACOSX && physicalDeviceSupportsPortabilitySubset(stack, physicalDevice)) {
				// Required when the physical device exposes VK_KHR_portability_subset
				// (true for MoltenVK on macOS); see VK_KHR_portability_enumeration.
				deviceExtensions.add(VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME);
			}

			device = VulkanUtils.createDevice(stack, physicalDevice, deviceExtensions, queueManager.getQueueIndices());

			queueManager.registerQueues();

			memoryManager = new VulkanMemoryManager(stack, this);

			commandPool = VulkanUtils.createCommandPool(device, queueManager.getGraphicsIndex());
			graphCommandBuffer = VulkanUtils.createCommandBuffer(device, commandPool);
			memCommandBuffer = VulkanUtils.createCommandBuffer(device, commandPool);
			fbCommandBuffer = VulkanUtils.createCommandBuffer(device, commandPool);


			final Map<Integer, Integer> universalAllocateAmounts = new HashMap<>();
			universalAllocateAmounts.put(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VulkanUtils.ALLOCATE_UBO_SLOTS);
			universalAllocateAmounts.put(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VulkanUtils.ALLOCATE_TEXTURE_SLOTS);

			universalDescPool = new VulkanDescriptorPool(device, VulkanUtils.ALLOCATE_SET_SLOTS, universalAllocateAmounts);


			final Map<Integer, Integer> textureAllocateAmounts = new HashMap<>();
			textureAllocateAmounts.put(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VulkanUtils.TEXTURE_POOL_SIZE);

			textureDescPool = new VulkanDescriptorPool(device, VulkanUtils.TEXTURE_POOL_SIZE, textureAllocateAmounts);


			final Map<Integer, Integer> multiAllocateAmounts = new HashMap<>();
			multiAllocateAmounts.put(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, VulkanUtils.MULTI_POOL_SIZE);

			multiDescPool = new VulkanDescriptorPool(device, VulkanUtils.MULTI_POOL_SIZE, multiAllocateAmounts);


			VkDescriptorSetLayoutBinding.Buffer textureBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
			textureBindings.get(0).set(0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1, VK_SHADER_STAGE_FRAGMENT_BIT, null);

			textureDescLayout = new VulkanDescriptorSetLayout(device, textureBindings);


			VkDescriptorSetLayoutBinding.Buffer multiBindings = VkDescriptorSetLayoutBinding.calloc(1, stack);
			multiBindings.get(0).set(0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 1, VK_SHADER_STAGE_VERTEX_BIT, null);

			multiDescLayout = new VulkanDescriptorSetLayout(device, multiBindings);


			VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.calloc(stack)
					.sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
					.magFilter(VK_FILTER_NEAREST)
					.minFilter(VK_FILTER_NEAREST)
					.addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
					.mipmapMode(VK_SAMPLER_MIPMAP_MODE_NEAREST)
					.minLod(0)
					.maxLod(0)
					.compareEnable(false)
					.anisotropyEnable(false)
					.unnormalizedCoordinates(false);
			samplers[ETextureType.NEAREST_FILTER.ordinal()] = VulkanUtils.createSampler(stack, samplerCreateInfo, device);

			samplerCreateInfo.minFilter(VK_FILTER_LINEAR)
					.magFilter(VK_FILTER_LINEAR);
			samplers[ETextureType.LINEAR_FILTER.ordinal()] = VulkanUtils.createSampler(stack, samplerCreateInfo, device);

			int globalUniformBufferSize = 4*4*4*(VulkanUtils.MAX_GLOBALTRANS_COUNT+1); // mat4+(1+MAX_GLOBALTRANS_COUNT)
			globalUniformStagingBuffer = memoryManager.createBuffer(globalUniformBufferSize, EVulkanMemoryType.STAGING, EVulkanBufferUsage.UNIFORM_BUFFER);
			globalUniformBufferData = BufferUtils.createByteBuffer(globalUniformBufferSize);
			globalUniformBuffer = memoryManager.createBuffer(globalUniformBufferSize,EVulkanMemoryType.STATIC, EVulkanBufferUsage.UNIFORM_BUFFER);
			// mat4
			backgroundUniformBfr = memoryManager.createBuffer(4*4*4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.UNIFORM_BUFFER);
			unifiedUniformBfr = memoryManager.createBuffer(4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.UNIFORM_BUFFER);

			if(globalUniformBuffer == null || backgroundUniformBfr == null || unifiedUniformBfr == null) throw new Error("Could not create uniform buffers.");

			unifiedArrayBfr = memoryManager.createMultiBuffer(2*100*4*4, EVulkanMemoryType.DYNAMIC, EVulkanBufferUsage.VERTEX_BUFFER);

			output.init(this);
		} finally {
			if(unifiedUniformBfr == null) invalidate();
		}
	}

	@Override
	public void invalidate() {
		closeMutex.acquireUninterruptibly();
		resourceMutex.acquireUninterruptibly();
		closeMutex.release();

		vkDeviceWaitIdle(device);

		// GPU is idle; flush any destroys that were queued but not yet executed.
		drainPendingDestroys();

		for(long sampler : samplers) {
			if(sampler != 0) vkDestroySampler(device, sampler, null);
		}

		if(pipelineManager != null) pipelineManager.destroy();
		if(multiDescLayout != null) multiDescLayout.destroy();
		if(textureDescLayout != null) textureDescLayout.destroy();
		if(multiDescPool != null) multiDescPool.destroy();
		if(textureDescPool != null) textureDescPool.destroy();
		if(universalDescPool != null) universalDescPool.destroy();

		if(commandPool != 0) {
			vkDestroyCommandPool(device, commandPool, null);
		}

		if(renderPass != VK_NULL_HANDLE) vkDestroyRenderPass(device, renderPass, null);
		commandBufferRecording = false;

		output.destroy();

		memoryManager.destroy();

		if(device != null) vkDestroyDevice(device, null);

		fbCommandBuffer = null;
		memCommandBuffer = null;
		graphCommandBuffer = null;

		device = null;
		super.invalidate();
		resourceMutex.release();
	}

	@Override
	public void setShadowDepthOffset(float depth) {
		unifiedUniformBfrData.putFloat(0, depth);
		unifiedDataUpdated = true;
	}

	private void updateUnifiedStatic() {
		if(unifiedDataUpdated) {
			updateBufferAt(unifiedUniformBfr, 0, unifiedUniformBfrData);
			unifiedDataUpdated = false;
		}
	}

	protected AbstractVulkanBuffer unifiedUniformBfr = null;
	private final ByteBuffer unifiedUniformBfrData = BufferUtils.createByteBuffer(4);
	private boolean unifiedDataUpdated = false;

	@Override
	public TextureHandle generateTexture(ImageData image, String name) {
		return generateTextureInternal(image, 0L);
	}

	private IntBuffer fixRGBA8Buffer(IntBuffer orig) {
		IntBuffer bfr = BufferUtils.createIntBuffer(orig.remaining());
		while(orig.hasRemaining()) {
			int color = orig.get();
			bfr.put(((color<<24)&0xFF000000) | ((color >> 8)&0xFFFFFF));
		}
		bfr.rewind();
		return bfr;
	}

	private TextureHandle generateTextureInternal(ImageData image, long descSet) {
		int width = image.getWidth();
		int height = image.getHeight();

		if(!commandBufferRecording) return null;

		if(width == 0) width = 1;
		if(height == 0) height = 1;

		VulkanTextureHandle vkTexHandle = createTexture(width, height, descSet);
		vkTexHandle.getImage().changeLayout(memCommandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

		if(image.getReadData32() != null) updateTexture(vkTexHandle, 0, 0, image);
		return vkTexHandle;
	}

	@Override
	protected void drawMulti(MultiDrawHandle call) {
		if(!commandBufferRecording || call == null || call.drawCalls == null || call.sourceQuads == null) return;

		updateUnifiedStatic();

		AbstractVulkanBuffer vkDrawCalls = (AbstractVulkanBuffer) call.drawCalls;

		pipelineManager.bind(EVulkanPipelineType.UNIFIED_MULTI);

		AbstractVulkanBuffer vkQuads = (AbstractVulkanBuffer)call.sourceQuads.vertices;

		long verticesDescSet = multiDescriptorSets.computeIfAbsent(vkQuads, this::createMultiDescriptorSet);

		pipelineManager.bindDescSets(getTextureDescSet(call.sourceQuads.texture), verticesDescSet);

		pipelineManager.bindVertexBuffers(vkDrawCalls.getBufferIdVk());

		if (MAC_FAN_AS_LIST) {
			// drawMulti always renders 4-vertex quads as instances; fan->list rewrite needed.
			vkCmdBindIndexBuffer(graphCommandBuffer, getOrCreateQuadIndexBuffer(), 0, VK_INDEX_TYPE_UINT32);
			vkCmdDrawIndexed(graphCommandBuffer, 6, call.used, 0, 0, 0);
		} else {
			vkCmdDraw(graphCommandBuffer, 4, call.used, 0, 0);
		}

		((VulkanMultiBufferHandle)call.drawCalls).inc();
	}

	private final Map<AbstractVulkanBuffer, Long> multiDescriptorSets = new HashMap<>();

	private final VulkanMultiBufferHandle unifiedArrayBfr;
	private final ByteBuffer unifiedArrayStaging = BufferUtils.createByteBuffer(2*100*4*4);

	@Override
	protected void drawUnifiedArray(UnifiedDrawHandle call, int primitive, int vertexCount, float[] trans, float[] colors, int array_len) {
		if(!commandBufferRecording || call == null || call.vertices == null) return;

		updateUnifiedStatic();

		if(primitive != EPrimitiveType.Quad) throw new Error("not implemented primitive: " + primitive);

		FloatBuffer data = unifiedArrayStaging.asFloatBuffer();

		data.put(colors, 0, array_len*4);
		data.position(4*100);
		data.put(trans, 0, array_len*4);
		updateBufferAt(unifiedArrayBfr, 0, unifiedArrayStaging);

		pipelineManager.bind(EVulkanPipelineType.UNIFIED_ARRAY);

		long vb = ((AbstractVulkanBuffer)call.vertices).getBufferIdVk();
		pipelineManager.bindVertexBuffers(vb, vb, unifiedArrayBfr.getBufferIdVk());
		pipelineManager.bindDescSets(getTextureDescSet(call.texture));

		if (MAC_FAN_AS_LIST) {
			// vertexCount is 4 here (unified array always draws quads); promote to indexed
			// TRIANGLE_LIST so MoltenVK doesn't break the render pass for every fan draw.
			vkCmdBindIndexBuffer(graphCommandBuffer, getOrCreateQuadIndexBuffer(), 0, VK_INDEX_TYPE_UINT32);
			vkCmdDrawIndexed(graphCommandBuffer, 6, array_len, 0, call.offset, 0);
		} else {
			vkCmdDraw(graphCommandBuffer, vertexCount, array_len, call.offset, 0);
		}
		unifiedArrayBfr.inc();
	}

	private final Map<Integer, VulkanBufferHandle> lineIndexBfr = new HashMap<>();

	// On macOS we promote Quad pipelines to TRIANGLE_LIST (see VulkanUtils.createPipeline)
	// so we need a tiny shared index buffer that maps fan vertex order [0,1,2,3] to the two
	// triangles {0,1,2} and {0,2,3}. This buffer is lazily created on the first quad draw
	// because the memory manager isn't available until command-buffer recording starts.
	private VulkanBufferHandle quadIndexBfr = null;

	private long getOrCreateQuadIndexBuffer() {
		if (quadIndexBfr != null) return quadIndexBfr.getBufferIdVk();
		ByteBuffer indices = BufferUtils.createByteBuffer(6 * 4);
		IntBuffer data = indices.asIntBuffer();
		data.put(0, 0); data.put(1, 1); data.put(2, 2);
		data.put(3, 0); data.put(4, 2); data.put(5, 3);
		quadIndexBfr = memoryManager.createBuffer(indices.remaining(), EVulkanMemoryType.STATIC, EVulkanBufferUsage.INDEX_BUFFER);
		updateBufferAt(quadIndexBfr, 0, indices);
		return quadIndexBfr.getBufferIdVk();
	}

	private static final boolean MAC_FAN_AS_LIST = Platform.get() == Platform.MACOSX;

	@Override
	protected void drawUnified(UnifiedDrawHandle call, int primitive, int vertices, int mode, float x, float y, float z, float sx, float sy, AbstractColor color, float intensity) {
		if(!commandBufferRecording || call == null || call.vertices == null) return;

		updateUnifiedStatic();

		if(primitive == EPrimitiveType.Triangle || primitive == EPrimitiveType.Quad) {
			pipelineManager.bind(EVulkanPipelineType.UNIFIED_QUAD);
		} else {
			pipelineManager.bind(EVulkanPipelineType.UNIFIED_LINE);
		}

		pipelineManager.bindDescSets(getTextureDescSet(call.texture));

		long vb = ((AbstractVulkanBuffer)call.vertices).getBufferIdVk();
		pipelineManager.bindVertexBuffers(vb, vb);

		ByteBuffer unifiedPushConstants = pipelineManager.getPushConstantBfr();
		// 4 padding bytes

		unifiedPushConstants.putFloat(4, sx);
		unifiedPushConstants.putFloat(8, sy);
		unifiedPushConstants.putFloat(12, x);
		unifiedPushConstants.putFloat(16, y);
		unifiedPushConstants.putFloat(20, z);

		if(color != null) {
			unifiedPushConstants.putFloat(28, color.red);
			unifiedPushConstants.putFloat(32, color.green);
			unifiedPushConstants.putFloat(36, color.blue);
			unifiedPushConstants.putFloat(40, color.alpha);
		} else {
			unifiedPushConstants.putFloat(28, 1);
			unifiedPushConstants.putFloat(32, 1);
			unifiedPushConstants.putFloat(36, 1);
			unifiedPushConstants.putFloat(40, 1);
		}

		unifiedPushConstants.putFloat(44, intensity);
		unifiedPushConstants.putInt(48, mode);

		pipelineManager.pushConstants();

		if(primitive == EPrimitiveType.Triangle) {
			// Triangle path is always TRIANGLE_LIST on every platform (vertices already
			// supplied as 3 vertices per tri), so no fan->list rewrite is needed here.
			vkCmdDraw(graphCommandBuffer, vertices, 1, call.offset, 0);
		} else if(primitive == EPrimitiveType.Quad) {
			if (MAC_FAN_AS_LIST) {
				// Pipeline is TRIANGLE_LIST on macOS; emit 6 indices that fan-triangulate
				// the 4 quad corners (see getOrCreateQuadIndexBuffer). vkCmdDrawIndexed's
				// vertexOffset is added to every index, so we still get the per-handle
				// offset into the shared vertex buffer.
				vkCmdBindIndexBuffer(graphCommandBuffer, getOrCreateQuadIndexBuffer(), 0, VK_INDEX_TYPE_UINT32);
				vkCmdDrawIndexed(graphCommandBuffer, 6, 1, 0, call.offset, 0);
			} else {
				vkCmdDraw(graphCommandBuffer, 4, 1, call.offset, 0);
			}
		} else {
			VulkanBufferHandle indexBfr = lineIndexBfr.get(vertices);

			if(indexBfr == null) {
				ByteBuffer indices = BufferUtils.createByteBuffer((vertices)*2*4);
				IntBuffer data = indices.asIntBuffer();

				for(int i = 0; i != vertices; i++) {
					data.put(i*2, i);
					data.put(i*2+1, (i+1)%vertices);
				}

				indexBfr = memoryManager.createBuffer(indices.remaining(), EVulkanMemoryType.STATIC, EVulkanBufferUsage.INDEX_BUFFER);
				updateBufferAt(indexBfr, 0, indices);

				lineIndexBfr.put(vertices, indexBfr);
			}

			vkCmdBindIndexBuffer(graphCommandBuffer, indexBfr.getBufferIdVk(), 0, VK_INDEX_TYPE_UINT32);
			vkCmdDrawIndexed(graphCommandBuffer, (vertices+(primitive==EPrimitiveType.LineLoop?0:-1))*2, 1, 0, call.offset, 0);
		}
	}

	@Override
	public void drawBackground(BackgroundDrawHandle call) {
		if(!commandBufferRecording ||
				call == null ||
				call.texture == null ||
				call.vertices == null ||
				call.regions == null) {
			return;
		}

		AbstractVulkanBuffer vkShape = (AbstractVulkanBuffer) call.vertices;

		pipelineManager.bind(EVulkanPipelineType.BACKGROUND);

		if(backgroundDataUpdated) {
			updateBufferAt(backgroundUniformBfr, 0, backgroundUniformBfrData);
			backgroundDataUpdated = false;
		}

		pipelineManager.bindDescSets(getTextureDescSet(call.texture));

		pipelineManager.bindVertexBuffers(vkShape.getBufferIdVk());

		for(int i = 0; i < call.regionCount; i++) {
			int from = call.regions[i*2];
			int len = call.regions[i*2+1];

			vkCmdDraw(graphCommandBuffer, len, 1, from, 0);
		}
	}

	@Override
	public void setHeightMatrix(float[] matrix) {
		backgroundUniformBfrData.asFloatBuffer().put(matrix, 0, 16);
		backgroundDataUpdated = true;
	}

	protected final AbstractVulkanBuffer backgroundUniformBfr;
	private final ByteBuffer backgroundUniformBfrData = BufferUtils.createByteBuffer((4*4+2*4+1)*4);
	private boolean backgroundDataUpdated = false;


	protected int globalAttrIndex = 0;
	private final Matrix4f global = new Matrix4f();

	@Override
	public void setGlobalAttributes(float x, float y, float z, float sx, float sy, float sz) {
		if(!commandBufferRecording) return;

		if(globalAttrIndex == VulkanUtils.MAX_GLOBALTRANS_COUNT) throw new Error("Out of globalTrans slots: increase VulkanUtils.MAX_GLOBALTRANS_COUNT");
		globalAttrIndex++;

		finishFrame();

		global.identity();
		global.scale(sx, sy, sz);
		global.translate(x, y, z);
		global.get(4*4*4*(globalAttrIndex+1), globalUniformBufferData);

		if(pipelineManager.isPipelineBound()) pipelineManager.pushGlobalAttr();
	}

	@Override
	public void updateTexture(TextureHandle handle, List<int[]> diff, Buffer data) {
		if(!commandBufferRecording || handle == null) return;

		VulkanTextureHandle vkTexture = (VulkanTextureHandle)handle;
		if(!vkTexture.isValid()) return;

		Buffer fixedBfr;
		if(data instanceof IntBuffer) {
			fixedBfr = fixRGBA8Buffer((IntBuffer) data);
		} else {
			fixedBfr = data;
		}

		int stagingPos = prepareStagingData(fixedBfr);

		int count = diff.size();
		VkBufferImageCopy.Buffer regions = VkBufferImageCopy.create(count);
		for(int i = 0; i != count; i++) {
			VkBufferImageCopy imageCopy = regions.get(i);
			int[] original = diff.get(i);
			imageCopy.imageOffset().set(original[0], original[1], 0);
			imageCopy.imageExtent().set(original[2], original[3], 1);
			imageCopy.imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
			imageCopy.bufferOffset(stagingPos+original[4]).bufferRowLength(original[2]);
		}

		vkTexture.getImage().changeLayout(memCommandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
		vkCmdCopyBufferToImage(memCommandBuffer, stagingBuffers.get(stagingBufferIndex).getBufferIdVk(), vkTexture.getImage().getImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, regions);
		vkTexture.getImage().changeLayout(memCommandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
	}

	@Override
	public void updateTexture(TextureHandle textureIndex, int left, int bottom, ImageData image) {
		int width = image.getWidth();
		int height = image.getHeight();

		if(!commandBufferRecording || textureIndex == null || width == 0 || height == 0) return;

		VulkanTextureHandle vkTexture = (VulkanTextureHandle) textureIndex;
		if(!vkTexture.isValid()) return;
		int stagingPos = prepareStagingData(fixRGBA8Buffer(image.getReadData32()));

		VkBufferImageCopy.Buffer region = VkBufferImageCopy.create(1);
		region.get(0).imageOffset().set(left, bottom, 0);
		region.get(0).imageExtent().set(width, height, 1);
		region.get(0).imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
		region.get(0).bufferOffset(stagingPos).bufferRowLength(width);

		vkTexture.getImage().changeLayout(memCommandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
		vkCmdCopyBufferToImage(memCommandBuffer, stagingBuffers.get(stagingBufferIndex).getBufferIdVk(), vkTexture.getImage().getImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);
		vkTexture.getImage().changeLayout(memCommandBuffer, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
	}

	@Override
	public TextureHandle resizeTexture(TextureHandle textureIndex, ImageData image) {
		if(textureIndex == null) return null;

		((VulkanTextureHandle)textureIndex).setDestroy();
		if(!commandBufferRecording) return null;

		return generateTextureInternal(image, ((VulkanTextureHandle)textureIndex).descSet);
	}

	protected int usedStagingMemory = 0;
	protected int stagingBufferIndex = 0;
	protected final List<VulkanBufferHandle> stagingBuffers = new ArrayList<>();

	private int prepareStagingData(Buffer data) {
		ByteBuffer bdata = (data instanceof ByteBuffer)?(ByteBuffer)data : null;
		ShortBuffer sdata = (data instanceof ShortBuffer)?(ShortBuffer)data : null;
		IntBuffer idata = (data instanceof IntBuffer)?(IntBuffer) data : null;


		int size;
		if(bdata != null) size = bdata.remaining();
		else if(sdata != null) size = sdata.remaining()*2;
		else if(idata != null) size = idata.remaining()*4;
		else throw new Error("Not yet implemented Buffer variant: " + data.getClass().getName());

		do {
			if(stagingBuffers.size() == stagingBufferIndex) {
				int newSize = 1024*(1<<stagingBufferIndex); // aka 1kB * 2^index

				if(newSize < size) newSize = size; // don't create a too small buffer

				stagingBuffers.add(stagingBufferIndex, memoryManager.createBuffer(newSize, EVulkanMemoryType.STAGING, EVulkanBufferUsage.NONE));
			} else {
				if(usedStagingMemory+size > stagingBuffers.get(stagingBufferIndex).getSize()) {
					stagingBufferIndex++;
					usedStagingMemory = 0;
				} else {
					break;
				}
			}
		} while(true);

		VulkanBufferHandle currentStagingBuffer = stagingBuffers.get(stagingBufferIndex);

		ByteBuffer mapped = currentStagingBuffer.map(usedStagingMemory);

		if(bdata != null) mapped.put(bdata.asReadOnlyBuffer());
		if(sdata != null) mapped.asShortBuffer().put(sdata.asReadOnlyBuffer());
		if(idata != null) mapped.asIntBuffer().put(idata.asReadOnlyBuffer());

		currentStagingBuffer.unmap();

		int offset = usedStagingMemory;
		usedStagingMemory += size;
		// bufferOffset must be a multiple of 4
		usedStagingMemory -= -usedStagingMemory%4;

		return offset;
	}

	private final VkBufferCopy.Buffer update_buffer_region = VkBufferCopy.create(1);

	@Override
	public void updateBufferAt(BufferHandle handle, int pos, ByteBuffer data) {
		if(!commandBufferRecording || handle == null || data.remaining() == 0) return;

		AbstractVulkanBuffer vkBuffer = (AbstractVulkanBuffer) handle;

		if(vkBuffer.getType() == EVulkanMemoryType.STATIC) {

			if (data.remaining() >= 65536) {
				int writePos = prepareStagingData(data);
				update_buffer_region.get(0).set(writePos, pos, data.remaining());
				vkCmdCopyBuffer(memCommandBuffer, stagingBuffers.get(stagingBufferIndex).getBufferIdVk(), vkBuffer.getBufferIdVk(), update_buffer_region);
			} else {
				vkCmdUpdateBuffer(memCommandBuffer, vkBuffer.getBufferIdVk(), pos, data);
			}

			syncQueues(vkBuffer.getEvent(), vkBuffer.getBufferIdVk());
		} else {
			ByteBuffer mapped = vkBuffer.map();
			mapped.put(data.asReadOnlyBuffer());

			vkBuffer.unmap();
			vkBuffer.flushChanges(pos, data.remaining());
		}
	}

	@Override
	public void updateBufferAt(BufferHandle handle, List<Integer> pos, List<Integer> len, ByteBuffer data) {
		if(!commandBufferRecording || handle == null) return;

		AbstractVulkanBuffer vkBuffer = (AbstractVulkanBuffer)handle;

		int writePos = prepareStagingData(data);
		int count = pos.size();
		VkBufferCopy.Buffer update_buffer_regions = VkBufferCopy.create(count);
		for(int i = 0; i != count; i++) {
			int off = pos.get(i);
			update_buffer_regions.get(i).set(writePos+off, off, len.get(i));
		}

		vkCmdCopyBuffer(memCommandBuffer, stagingBuffers.get(stagingBufferIndex).getBufferIdVk(), vkBuffer.getBufferIdVk(), update_buffer_regions);
		syncQueues(vkBuffer.getEvent(), vkBuffer.getBufferIdVk());
	}

	private long firstTextureDescSet = 0;

	private VulkanTextureHandle createTexture(int width, int height, long descSet) {
		VulkanImage image = memoryManager.createImage(width, height, EVulkanImageType.COLOR_IMAGE_RGBA8);

		long textureDescSet = descSet;

		if(textureDescSet == 0) {
			textureDescSet = textureDescPool.createNewSet(textureDescLayout);
		}

		VulkanTextureHandle vkTexHandle = new VulkanTextureHandle(this, memoryManager, image, textureDescSet);

		if(descSet == 0) {
			vkTexHandle.tick();
		}

		if(firstTextureDescSet == 0) {
			firstTextureDescSet = textureDescSet;
		}

		textures.add(vkTexHandle);
		return vkTexHandle;
	}


	private final LongBuffer syncQueueBfr = BufferUtils.createLongBuffer(1);
	private final VkBufferMemoryBarrier.Buffer synQueueArea = VkBufferMemoryBarrier.create(1)
			.sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
			.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
			.size(VK_WHOLE_SIZE)
			.offset(0);

	private void syncQueues(long event, long buffer) {
		synQueueArea.srcAccessMask(VK_ACCESS_MEMORY_WRITE_BIT).dstAccessMask(VK_ACCESS_MEMORY_READ_BIT).buffer(buffer);

		// On macOS / MoltenVK, vkCmdWaitEvents inside a render pass turns into
		//   [MTLCommandBuffer encodeWaitForEvent:value:]
		// which Metal rejects with the assertion
		//   "encodeWaitForEvent:value: with uncommitted encoder"
		// because a render encoder is still open on graphCommandBuffer at the time of
		// the wait. We can't simply drop the sync though - without it the graphics
		// commands read from the uniform/vertex buffer before the transfer finishes
		// and the camera matrices / vertex data are garbage, which manifests as a
		// solid black screen. The fix is to emit a plain pipeline barrier at the END
		// of the transfer command buffer; Metal flushes the source command buffer
		// before starting the next one within a single vkQueueSubmit, and the barrier
		// makes the buffer write visible to subsequent reads. This is functionally
		// equivalent to the SetEvent/WaitEvents pair but lives inside one command
		// buffer, so MoltenVK encodes it as a normal MTLBlitCommandEncoder fence
		// instead of a cross-MTLCommandBuffer wait.
		if (Platform.get() == Platform.MACOSX) {
			vkCmdPipelineBarrier(memCommandBuffer,
					VK_PIPELINE_STAGE_TRANSFER_BIT,
					VK_PIPELINE_STAGE_VERTEX_INPUT_BIT|VK_PIPELINE_STAGE_VERTEX_SHADER_BIT|VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT,
					0,
					null,
					synQueueArea,
					null);
			return;
		}

		syncQueueBfr.put(0, event);
		vkCmdSetEvent(memCommandBuffer, event, VK_PIPELINE_STAGE_TRANSFER_BIT|VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT);
		vkCmdWaitEvents(graphCommandBuffer, syncQueueBfr, VK_PIPELINE_STAGE_TRANSFER_BIT|VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT|VK_PIPELINE_STAGE_ALL_GRAPHICS_BIT, null, synQueueArea, null);
	}

	@Override
	public BackgroundDrawHandle createBackgroundDrawCall(int vertices, TextureHandle texture) {
		AbstractVulkanBuffer vertexBfr = memoryManager.createBuffer(vertices*6*4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.VERTEX_BUFFER);
		return new BackgroundDrawHandle(this, -1, texture, vertexBfr);
	}

	@Override
	public UnifiedDrawHandle createUnifiedDrawCall(int vertices, String name, TextureHandle texture, TextureHandle texture2, float[] data) {
		BufferHandle vertexBuffer = memoryManager.createBuffer(vertices*(texture!=null?4:2)*4, EVulkanMemoryType.STATIC, EVulkanBufferUsage.VERTEX_UNIFORM_BUFFER);
		if (data != null) {
			try(MemoryStack stack = MemoryStack.stackPush()) {
				ByteBuffer dataBfr = stack.malloc(data.length*4);
				dataBfr.asFloatBuffer().put(data);
				updateBufferAt(vertexBuffer, 0, dataBfr);
			}
		}

		return new UnifiedDrawHandle(this, -1, 0, vertices, texture, texture2, vertexBuffer);
	}

	@Override
	protected MultiDrawHandle createMultiDrawCall(String name, ManagedHandle source) {
		VulkanMultiBufferHandle drawCallBuffer = memoryManager.createMultiBuffer(MultiDrawHandle.MAX_CACHE_ENTRIES*12*4, EVulkanMemoryType.DYNAMIC, EVulkanBufferUsage.VERTEX_BUFFER);
		drawCallBuffer.reset();
		return new MultiDrawHandle(this, managedHandles.size(), MultiDrawHandle.MAX_CACHE_ENTRIES, source, drawCallBuffer);
	}

	@Override
	public void clearDepthBuffer() {
		if(!commandBufferRecording) return;
		finishFrame();

		VkClearAttachment.Buffer clearAttachment = VkClearAttachment.create(1);
		clearAttachment.get(0).set(VK_IMAGE_ASPECT_DEPTH_BIT, 1, CLEAR_VALUES.get(1));
		VkClearRect.Buffer clearRect = VkClearRect.create(1).layerCount(1).baseArrayLayer(0);
		clearRect.rect().extent().set(fbWidth, fbHeight);

		vkCmdClearAttachments(graphCommandBuffer, clearAttachment, clearRect);
	}

	private VulkanImage depthImage = null;
	protected final AbstractVulkanBuffer globalUniformStagingBuffer;
	protected final AbstractVulkanBuffer globalUniformBuffer;
	private final ByteBuffer globalUniformBufferData;
	private final Matrix4f projMatrix = new Matrix4f();

	private int newWidth;
	private int newHeight;
	private boolean resizeScheduled = false;

	@Override
	public void resize(int width, int height) {
		newWidth = width;
		newHeight = height;
		resize();
	}


	public void resize() {
		resizeScheduled = true;
	}

	void regenerateRenderPass(MemoryStack stack, int newSurfaceFormat) {

		if(pipelineManager != null) {
			pipelineManager.destroy();
			pipelineManager = null;
		}
		if(renderPass != 0) {
			vkDestroyRenderPass(device, renderPass, null);
			renderPass = 0;
		}

		renderPass = VulkanUtils.createRenderPass(stack, device, newSurfaceFormat);
		renderPassBeginInfo.renderPass(renderPass);

		pipelineManager = new VulkanPipelineManager(stack, this, universalDescPool, renderPass, graphCommandBuffer);
		pipelineManager.installUniformBuffers(globalUniformBuffer, backgroundUniformBfr, unifiedUniformBfr);
	}

	private void doResize(int width, int height) {
		// Make sure no command buffer is still consuming the old swapchain images,
		// framebuffers or depth attachment before we destroy them. Previously this
		// invariant was provided by vkQueueWaitIdle at the end of each present;
		// with per-frame fences instead, the GPU may still be in flight when we
		// arrive here.
		vkDeviceWaitIdle(device);

		Dimension newSize = output.resize(new Dimension(width, height));
		if(newSize == null) return;
		fbWidth = newSize.width;
		fbHeight = newSize.height;

		if(depthImage != null) {
			depthImage.destroy();
		}
		depthImage = memoryManager.createImage(fbWidth, fbHeight, EVulkanImageType.DEPTH_IMAGE);

		output.createFramebuffers(depthImage);

		pipelineManager.resize(fbWidth, fbHeight);
		renderPassBeginInfo.renderArea().extent().width(fbWidth).height(fbHeight);

		projMatrix.identity();
		projMatrix.scale(1.0f, -1.0f, 1.0f);
		projMatrix.ortho(0,  width,0, height, -1, 1, true);
		projMatrix.get(0, globalUniformBufferData);
	}

	private boolean commandBufferRecording = false;

	private final VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.create().sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO).pClearValues(CLEAR_VALUES);
	private final static VkClearValue.Buffer CLEAR_VALUES = VkClearValue.create(2); // only zeros is equal to black
	static {
		CLEAR_VALUES.get(1).depthStencil().set(1, 0);
	}


	private static final VkCommandBufferBeginInfo commandBufferBeginInfo = VkCommandBufferBeginInfo.calloc().sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

	@Override
	public void startFrame() {
		endFrame();

		if(resizeScheduled) {
			doResize(newWidth, newHeight);
			resizeScheduled = false;
		}

		closeMutex.acquireUninterruptibly();
		resourceMutex.acquireUninterruptibly();
		closeMutex.release();

		try {
			super.startFrame();

			if(!output.startFrame()) {
				return;
			}

			// output.startFrame() has just waited on the per-frame fence, so the
			// previous frame's GPU work has completed. It is now safe to:
			//   1. drain queued resource destructions (textures, etc.) that were
			//      deferred because they may have been referenced by N-1.
			//   2. update descriptor sets via texture.tick(), since N-1's command
			//      buffer has finished consuming them.
			// Both of these would otherwise be undefined behaviour (a freed
			// Metal texture or a mutated descriptor set during command-buffer
			// execution shows up as VK_ERROR_OUT_OF_DEVICE_MEMORY /
			// kIOGPUCommandBufferCallbackErrorInvalidResource on MoltenVK).
			drainPendingDestroys();
			for (VulkanTextureHandle texture : textures) {
				texture.tick();
			}

			renderPassBeginInfo.framebuffer(output.getFramebuffer());

			if(vkBeginCommandBuffer(graphCommandBuffer, commandBufferBeginInfo) != VK_SUCCESS) return;
			if(vkBeginCommandBuffer(memCommandBuffer, commandBufferBeginInfo) != VK_SUCCESS) {
				vkEndCommandBuffer(graphCommandBuffer);
				return;
			}
			commandBufferRecording = true;
			// reset staging buffer
			usedStagingMemory = 0;
			stagingBufferIndex = 0;
			globalAttrIndex = 0;
			memoryManager.startFrame();

			vkCmdBeginRenderPass(graphCommandBuffer, renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);
			pipelineManager.clearLastPipeline();

			update_buffer_region.dstOffset(0).srcOffset(0).size(globalUniformBuffer.getSize());
			vkCmdCopyBuffer(memCommandBuffer, globalUniformStagingBuffer.getBufferIdVk(), globalUniformBuffer.getBufferIdVk(), update_buffer_region);
			syncQueues(globalUniformBuffer.getEvent(), globalUniformBuffer.getBufferIdVk());


			if(textDrawer == null) {
				textDrawer = new LWJGLTextDrawer(this, guiScale);
			}
		} finally {
			if(!commandBufferRecording) resourceMutex.release();
		}
	}

	private AbstractVulkanBuffer framebufferReadBack = null;
	private final VkBufferImageCopy.Buffer readBackRegion = VkBufferImageCopy.create(1);
	private int rbWidth = -1, rbHeight = -1;

	private boolean fbCBrecording = false;

	private static final VkImageSubresourceRange CLEAR_SUBRESOURCE = VkImageSubresourceRange.calloc().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1);

	public void clearFramebuffer() {
		if(!commandBufferRecording) return;

		if(!fbCBrecording) {
			if(vkBeginCommandBuffer(fbCommandBuffer, commandBufferBeginInfo)!=VK_SUCCESS) return;
			fbCBrecording = true;
		}

		VulkanImage image = output.getFramebufferImage();

		image.changeLayout(fbCommandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
		vkCmdClearColorImage(fbCommandBuffer, image.getImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, CLEAR_VALUES.get(0).color(), CLEAR_SUBRESOURCE);
		image.changeLayout(fbCommandBuffer, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);
	}

	public void readFramebuffer(IntBuffer pixels, int width, int height) {
		if(!commandBufferRecording) return;

		if(width > fbWidth) width = fbWidth;
		if(height > fbHeight) height = fbHeight;

		if(rbWidth != width || rbHeight != height) {
			if(framebufferReadBack != null) {
				framebufferReadBack.destroy();
			}

			rbWidth = width;
			rbHeight = height;
			framebufferReadBack = memoryManager.createBuffer(4*rbHeight*rbWidth, EVulkanMemoryType.READBACK, EVulkanBufferUsage.NONE);
		}

		readBackRegion.bufferOffset(0).bufferRowLength(width);
		readBackRegion.imageSubresource().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
		readBackRegion.imageOffset().set(0, 0, 0);
		readBackRegion.imageExtent().set(width, height, 1);

		if(vkBeginCommandBuffer(fbCommandBuffer, commandBufferBeginInfo) != VK_SUCCESS) {
			return;
		}
		fbCBrecording = true;

		VulkanImage image = output.getFramebufferImage();

		image.changeLayout(fbCommandBuffer, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
		vkCmdCopyImageToBuffer(fbCommandBuffer, image.getImage(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, framebufferReadBack.getBufferIdVk(), readBackRegion);
		image.changeLayout(fbCommandBuffer, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
		vkCmdPipelineBarrier(fbCommandBuffer, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, 0, null, null, null);
		vkCmdClearColorImage(fbCommandBuffer, image.getImage(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, CLEAR_VALUES.get(0).color(), CLEAR_SUBRESOURCE);
		image.changeLayout(fbCommandBuffer, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

		endFrame();

		IntBuffer mappedPixels = framebufferReadBack.map().asIntBuffer();
		int[] line = new int[width];
		for(int i = 0; i != height; i++) {
			mappedPixels.position(i*width);
			mappedPixels.get(line);
			pixels.position((height-i-1)*width);
			pixels.put(line);
		}
		pixels.rewind();
		framebufferReadBack.unmap();
	}

	public void endFrame() {
		try(MemoryStack stack = MemoryStack.stackPush()) {
			boolean cmdBfrSend = false;
			if(commandBufferRecording) {
				updateBufferAt(globalUniformStagingBuffer, 0, globalUniformBufferData);

				vkCmdEndRenderPass(graphCommandBuffer);
				vkEndCommandBuffer(graphCommandBuffer);
				vkEndCommandBuffer(memCommandBuffer);
				if(fbCBrecording) vkEndCommandBuffer(fbCommandBuffer);
				commandBufferRecording = false;

				VkSubmitInfo graphSubmitInfo = VkSubmitInfo.calloc(stack)
						.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
				if(fbCBrecording) {
					graphSubmitInfo.pCommandBuffers(stack.pointers(memCommandBuffer.address(), graphCommandBuffer.address(), fbCommandBuffer.address()));
					fbCBrecording = false;
				} else {
					graphSubmitInfo.pCommandBuffers(stack.pointers(memCommandBuffer.address(), graphCommandBuffer.address()));
				}

				output.configureDrawCommand(stack, graphSubmitInfo);

				long submitFence = output.acquireSubmitFence();
				int error = vkQueueSubmit(queueManager.getGraphicsQueue(), graphSubmitInfo, submitFence);
				if(error != VK_SUCCESS) {
					System.err.println("[VK-FRAME] vkQueueSubmit FAILED: " + error);
				} else {
					cmdBfrSend = true;
					if(submitFence != VK_NULL_HANDLE) {
						output.onSubmitFenceInFlight();
					}
				}
			}

			output.endFrame(cmdBfrSend);
		} finally {
			resourceMutex.release();
		}
	}


	private long getTextureDescSet(TextureHandle texture) {
		if(texture == null) {
			return firstTextureDescSet;
		} else {
			return ((VulkanTextureHandle) texture).descSet;
		}
	}


	private long createMultiDescriptorSet(AbstractVulkanBuffer multiBuffer) {
		long descSet = multiDescPool.createNewSet(multiDescLayout);

		VkDescriptorBufferInfo.Buffer install_uniform_buffer = VkDescriptorBufferInfo.create(1);
		install_uniform_buffer.get(0).set(multiBuffer.getBufferIdVk(), 0, VK_WHOLE_SIZE);


		VkWriteDescriptorSet.Buffer install_uniform_buffer_write = VkWriteDescriptorSet.create(1)
				.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
				.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
				.pBufferInfo(install_uniform_buffer)
				.descriptorCount(1)
				.dstArrayElement(0)
				.dstBinding(0)
				.dstSet(descSet);

		vkUpdateDescriptorSets(device, install_uniform_buffer_write, null);

		return descSet;
	}

	@Override
	public int getMaxManagedQuads() {
		return super.getMaxManagedQuads();
	}

	public VkDevice getDevice() {
		return device;
	}

	public int getGlobalAttrIndex() {
		return globalAttrIndex;
	}

	public VkInstance getInstance() {
		return instance;
	}

	public long getRenderPass() {
		return renderPass;
	}
}
