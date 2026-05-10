package go.graphics.swing.vulkan;

import go.graphics.swing.vulkan.memory.VulkanImage;
import java.awt.Dimension;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.function.BiFunction;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanSurfaceOutput extends AbstractVulkanOutput {

	private long surface;
	private long swapchain = VK_NULL_HANDLE;
	private int surfaceFormat;
	private int swapchainImageIndex = -1;
	private long waitSemaphore;
	private long signalSemaphore;

	// Fence signaled by every vkQueueSubmit that we wait on at the start of the
	// next frame. This is the deterministic per-frame sync point: relying on
	// vkQueueWaitIdle alone is unsound on MoltenVK because the Metal drawable
	// present is scheduled outside of the command-buffer being waited on, so the
	// next vkAcquireNextImageKHR can race with it and produce stale/black images.
	private long frameFence = VK_NULL_HANDLE;
	private boolean frameFenceInFlight = false;

	private VulkanImage[] swapchainImages;
	private long[] framebuffers;
	private final VkSwapchainCreateInfoKHR swapchainCreateInfo = VkSwapchainCreateInfoKHR.create();
	private final VkFramebufferCreateInfo framebufferCreateInfo = VkFramebufferCreateInfo.create();

	public VulkanSurfaceOutput(long surface) {
		this.surface = surface;
	}

	@Override
	BiFunction<VkQueueFamilyProperties, Integer, Boolean> getPresentQueueCond(VkPhysicalDevice physicalDevice) {
		return (queue, index) -> {
			int[] present = new int[1];
			if(surface == 0) return true;

			vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, index, surface, present);
			return present[0]==1;
		};
	}

	public void setSurface(long surface) {
		this.surface = surface;

		try(MemoryStack stack = MemoryStack.stackPush()) {
			VkSurfaceFormatKHR.Buffer allSurfaceFormats = VulkanUtils.listSurfaceFormats(stack, dc.getDevice().getPhysicalDevice(), surface);
			VkSurfaceFormatKHR surfaceFormat = VulkanUtils.findSurfaceFormat(allSurfaceFormats);
			int newSurfaceFormat = surfaceFormat.format();

			IntBuffer present = stack.callocInt(1);
			vkGetPhysicalDeviceSurfaceSupportKHR(dc.getDevice().getPhysicalDevice(), dc.queueManager.getPresentIndex(), surface, present);
			if(present.get(0) == 0) {
				System.err.println("[VULKAN] can't present anymore");
				return;
			}


			swapchainCreateInfo.surface(surface)
					.imageColorSpace(surfaceFormat.colorSpace())
					.imageFormat(surfaceFormat.format());

			if(dc.getRenderPass() == 0 || this.surfaceFormat != newSurfaceFormat) {
				dc.regenerateRenderPass(stack, newSurfaceFormat);
				framebufferCreateInfo.renderPass(dc.getRenderPass());
			}
			this.surfaceFormat = newSurfaceFormat;
		}

		dc.resize();
	}

	@Override
	void init(VulkanDrawContext dc) {
		super.init(dc);


		waitSemaphore = VulkanUtils.createSemaphore(dc.getDevice());
		signalSemaphore = VulkanUtils.createSemaphore(dc.getDevice());

		try(MemoryStack initStack = MemoryStack.stackPush()) {
			VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.calloc(initStack)
					.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
			LongBuffer fenceHandle = initStack.callocLong(1);
			if (vkCreateFence(dc.getDevice(), fenceInfo, null, fenceHandle) == VK_SUCCESS) {
				frameFence = fenceHandle.get(0);
			}
		}

		swapchainCreateInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
				.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
				.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT|VK_IMAGE_USAGE_TRANSFER_SRC_BIT|VK_IMAGE_USAGE_TRANSFER_DST_BIT)
				.presentMode(VK_PRESENT_MODE_FIFO_KHR) // must be supported by all drivers
				.imageArrayLayers(1)
				.clipped(false);

		if(dc.queueManager.hasUniversalQueue()) {
			swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
		} else {
			IntBuffer queueFamilies = BufferUtils.createIntBuffer(2);
			queueFamilies.put(0, dc.queueManager.getPresentIndex());
			queueFamilies.put(1, dc.queueManager.getGraphicsIndex());

			swapchainCreateInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
					.pQueueFamilyIndices(queueFamilies);
		}

		framebufferCreateInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
				.layers(1);

		setSurface(surface);
	}

	public void removeSurface() {
		destroyFramebuffers(-1);
		destroySwapchainViews(-1);
		vkDestroySwapchainKHR(dc.getDevice(), swapchain, null);
		vkDestroySurfaceKHR(dc.getInstance(), surface, null);

		surface = VK_NULL_HANDLE;
		swapchain = VK_NULL_HANDLE;
	}

	@Override
	void destroy() {
		super.destroy();

		if(frameFence != VK_NULL_HANDLE) {
			if(frameFenceInFlight) {
				vkWaitForFences(dc.getDevice(), frameFence, true, Long.MAX_VALUE);
				frameFenceInFlight = false;
			}
			vkDestroyFence(dc.getDevice(), frameFence, null);
			frameFence = VK_NULL_HANDLE;
		}

		if(waitSemaphore != 0) {
			vkDestroySemaphore(dc.getDevice(), waitSemaphore, null);
		}
		if(signalSemaphore != 0) {
			vkDestroySemaphore(dc.getDevice(), signalSemaphore, null);
		}

		if(swapchain != VK_NULL_HANDLE) {
			destroyFramebuffers(-1);
			destroySwapchainViews(-1);
			vkDestroySwapchainKHR(dc.getDevice(), swapchain, null);
		}
		swapchain = VK_NULL_HANDLE;
	}

	@Override
	Dimension resize(Dimension preferredSize) {
		destroyFramebuffers(-1);
		destroySwapchainViews(-1);

		VkSurfaceCapabilitiesKHR surfaceCapabilities = VkSurfaceCapabilitiesKHR.create();
		if (surface == 0 || vkGetPhysicalDeviceSurfaceCapabilitiesKHR(dc.getDevice().getPhysicalDevice(), surface, surfaceCapabilities) != VK_SUCCESS) {
			return null;
		}

		VkExtent2D minDim = surfaceCapabilities.minImageExtent();
		VkExtent2D maxDim = surfaceCapabilities.maxImageExtent();
		VkExtent2D curDim = surfaceCapabilities.currentExtent();

		// Prefer currentExtent when the surface dictates one (typical on Windows/Linux). On
		// macOS / MoltenVK, currentExtent reports 0xFFFFFFFF for "any" early on, in which case
		// we fall back to the caller's preferred size (the canvas size in pixels).
		int fbWidth = preferredSize.width;
		int fbHeight = preferredSize.height;
		// Vulkan signals "any" with 0xFFFFFFFF (== -1 as a Java signed int).
		if (curDim.width() != -1) {
			fbWidth = curDim.width();
		}
		if (curDim.height() != -1) {
			fbHeight = curDim.height();
		}

		fbWidth = Math.max(Math.min(fbWidth, maxDim.width()), minDim.width());
		fbHeight = Math.max(Math.min(fbHeight, maxDim.height()), minDim.height());

		int imageCount = surfaceCapabilities.minImageCount() + 1;
		if (surfaceCapabilities.maxImageCount() != 0)
			imageCount = Math.min(imageCount, surfaceCapabilities.maxImageCount());

		swapchainCreateInfo.preTransform(surfaceCapabilities.currentTransform())
				.minImageCount(imageCount)
				.oldSwapchain(swapchain)
				.imageExtent()
				.width(fbWidth)
				.height(fbHeight);

		LongBuffer swapchainBfr = BufferUtils.createLongBuffer(1);
		int createErr = vkCreateSwapchainKHR(dc.getDevice(), swapchainCreateInfo, null, swapchainBfr);
		boolean error = createErr != VK_SUCCESS;
		vkDestroySwapchainKHR(dc.getDevice(), swapchain, null);

		if (error) {
			System.err.println("[VK-SC] vkCreateSwapchainKHR failed: " + createErr);
			swapchain = VK_NULL_HANDLE;
			return null;
		}
		swapchain = swapchainBfr.get(0);

		framebufferCreateInfo.width(fbWidth)
				.height(fbHeight);

		return new Dimension(fbWidth, fbHeight);
	}

	private void destroySwapchainViews(int count) {
		if(swapchainImages == null) return;
		if(count == -1) count = swapchainImages.length;

		for(int i = 0; i != count; i++) {
			swapchainImages[i].destroy();
		}
		swapchainImages = null;
	}

	private void destroyFramebuffers(int count) {
		if(framebuffers == null) return;
		if(count == -1) count = framebuffers.length;

		for(int i = 0; i != count; i++) {
			vkDestroyFramebuffer(dc.getDevice(), framebuffers[i], null);
		}
		framebuffers = null;
	}

	@Override
	void createFramebuffers(VulkanImage depthImage) {
		long[] imageHandles = VulkanUtils.getSwapchainImages(dc.getDevice(), swapchain);
		if (imageHandles == null) {
			vkDestroySwapchainKHR(dc.getDevice(), swapchain, null);
			swapchain = VK_NULL_HANDLE;
			return;
		}

		swapchainImages = new VulkanImage[imageHandles.length];
		for (int i = 0; i != swapchainImages.length; i++) {

			try {
				swapchainImages[i] = new VulkanImage(dc, null, imageHandles[i], -1L, surfaceFormat, VK_IMAGE_ASPECT_COLOR_BIT);
			} catch(Throwable thrown) {
				thrown.printStackTrace();
				destroySwapchainViews(i);
				vkDestroySwapchainKHR(dc.getDevice(), swapchain, null);
				swapchain = VK_NULL_HANDLE;
				return;
			}
		}

		LongBuffer framebufferBfr = BufferUtils.createLongBuffer(1);
		LongBuffer attachments = BufferUtils.createLongBuffer(2);
		attachments.put(1, depthImage.getImageView());

		framebuffers = new long[swapchainImages.length];
		for (int i = 0; i != swapchainImages.length; i++) {
			attachments.put(0, swapchainImages[i].getImageView());
			framebufferCreateInfo.pAttachments(attachments);

			if (vkCreateFramebuffer(dc.getDevice(), framebufferCreateInfo, null, framebufferBfr) != VK_SUCCESS) {
				destroyFramebuffers(i);
				destroySwapchainViews(-1);
				vkDestroySwapchainKHR(dc.getDevice(), swapchain, null);
				swapchain = VK_NULL_HANDLE;
			}

			framebuffers[i] = framebufferBfr.get(0);
		}
	}

	@Override
	boolean startFrame() {
		if(swapchain == VK_NULL_HANDLE) {
			dc.resize();
			return false;
		}

		// Wait for the previous frame's GPU work to finish before re-using its
		// resources (including the swapchain image we are about to acquire and the
		// waitSemaphore). vkQueueWaitIdle is not enough on MoltenVK because the
		// Metal drawable presentation is scheduled outside the command buffer.
		if(frameFence != VK_NULL_HANDLE && frameFenceInFlight) {
			vkWaitForFences(dc.getDevice(), frameFence, true, Long.MAX_VALUE);
			vkResetFences(dc.getDevice(), frameFence);
			frameFenceInFlight = false;
		}

		IntBuffer swapchainImageIndexBfr = BufferUtils.createIntBuffer(1);
		int err = vkAcquireNextImageKHR(dc.getDevice(), swapchain, -1L, waitSemaphore, VK_NULL_HANDLE, swapchainImageIndexBfr);
		if(err == VK_ERROR_OUT_OF_DATE_KHR) {
			// The swapchain was already invalidated before we acquired anything, so
			// the semaphore was NOT signaled. Recreate and bail out of the frame.
			dc.resize();
			return false;
		}
		if(err != VK_SUBOPTIMAL_KHR && err != VK_SUCCESS) {
			System.err.println("[VK-FRAME] vkAcquireNextImageKHR failed: " + err);
			return false;
		}
		// VK_SUBOPTIMAL_KHR still produces a valid image and signals waitSemaphore;
		// schedule a resize for the *next* frame but render this one normally.
		if(err == VK_SUBOPTIMAL_KHR) {
			dc.resize();
		}

		swapchainImageIndex = swapchainImageIndexBfr.get(0);
		return true;
	}

	@Override
	void endFrame(boolean wait) {
		if (swapchainImageIndex == -1) {
			return;
		}

		try(MemoryStack stack = MemoryStack.stackPush()) {
			// If the caller didn't submit any work this frame (typically because
			// vkBeginCommandBuffer failed), waitSemaphore is still signaled from the
			// acquire and signalSemaphore is unsignaled. Both states are invalid for
			// the next iteration -- the next acquire would receive a semaphore that
			// is already signaled, which is UB and on MoltenVK reliably produces
			// stale/black framebuffers for several frames afterwards. Submit a
			// minimal "drain" batch that consumes waitSemaphore and signals
			// signalSemaphore so that semaphore states are restored to the same
			// invariants as a normal submitted frame.
			if (!wait) {
				VkSubmitInfo drain = VkSubmitInfo.calloc(stack)
						.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
						.pWaitSemaphores(stack.longs(waitSemaphore))
						.waitSemaphoreCount(1)
						.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
						.pSignalSemaphores(stack.longs(signalSemaphore));
				long submitFence = (frameFence != VK_NULL_HANDLE) ? frameFence : VK_NULL_HANDLE;
				int err = vkQueueSubmit(dc.queueManager.getGraphicsQueue(), drain, submitFence);
				if (err == VK_SUCCESS && submitFence != VK_NULL_HANDLE) {
					frameFenceInFlight = true;
				}
			}

			VkPresentInfoKHR presentInfo = VkPresentInfoKHR.calloc(stack)
					.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
					.pImageIndices(stack.ints(swapchainImageIndex))
					.swapchainCount(1)
					.pSwapchains(stack.longs(swapchain))
					.pWaitSemaphores(stack.longs(signalSemaphore));

			int presentErr = vkQueuePresentKHR(dc.queueManager.getPresentQueue(), presentInfo);
			if (presentErr != VK_SUCCESS && presentErr != VK_SUBOPTIMAL_KHR) {
				System.err.println("[VK-FRAME] vkQueuePresentKHR FAILED: " + presentErr);
			}
		}
		swapchainImageIndex = -1;
	}

	@Override
	VulkanImage getFramebufferImage() {
		return swapchainImages[swapchainImageIndex];
	}

	@Override
	long getFramebuffer() {
		return framebuffers[swapchainImageIndex];
	}

	@Override
	public boolean needsPresentQueue() {
		return true;
	}

	@Override
	void configureDrawCommand(MemoryStack stack, VkSubmitInfo graphSubmitInfo) {
		graphSubmitInfo.pWaitSemaphores(stack.longs(waitSemaphore))
					.pSignalSemaphores(stack.longs(signalSemaphore))
						.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
						.waitSemaphoreCount(1);
	}

	@Override
	long acquireSubmitFence() {
		return frameFence;
	}

	@Override
	void onSubmitFenceInFlight() {
		frameFenceInFlight = true;
	}
}
