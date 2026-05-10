package go.graphics.swing.vulkan;

import go.graphics.swing.vulkan.memory.VulkanImage;
import java.awt.Dimension;
import java.util.function.BiFunction;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSubmitInfo;

public abstract class AbstractVulkanOutput {

	protected VulkanDrawContext dc;

	abstract BiFunction<VkQueueFamilyProperties, Integer, Boolean> getPresentQueueCond(VkPhysicalDevice physicalDevice);

	void init(VulkanDrawContext dc) {
		this.dc = dc;
	}

	void destroy() {
	}

	abstract Dimension resize(Dimension preferredSize);

	abstract void createFramebuffers(VulkanImage depthImage);

	abstract boolean startFrame();

	abstract void endFrame(boolean wait);

	abstract VulkanImage getFramebufferImage();

	abstract long getFramebuffer();

	public abstract boolean needsPresentQueue();

	abstract void configureDrawCommand(MemoryStack stack, VkSubmitInfo graphSubmitInfo);

	/**
	 * Returns the fence the next graphics submit should signal, or VK_NULL_HANDLE
	 * if the output doesn't need a per-frame fence. The output is responsible for
	 * waiting on / resetting that fence at the start of the next frame.
	 */
	long acquireSubmitFence() {
		return 0L; // VK_NULL_HANDLE
	}

	/**
	 * Notifies the output that the last vkQueueSubmit successfully consumed the
	 * fence returned by {@link #acquireSubmitFence()} and is therefore now in
	 * flight on the GPU.
	 */
	void onSubmitFenceInFlight() {
	}
}
