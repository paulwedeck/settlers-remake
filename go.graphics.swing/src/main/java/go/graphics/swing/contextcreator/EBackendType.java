/*******************************************************************************
 * Copyright (c) 2018
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
package go.graphics.swing.contextcreator;

import org.lwjgl.system.Platform;
import org.lwjgl.system.linux.X11;
import org.lwjgl.system.windows.GDI32;
import org.lwjgl.vulkan.VK;

import java.util.function.BiFunction;
import java.util.function.Supplier;

import go.graphics.swing.ContextContainer;


public enum EBackendType implements Comparable<EBackendType> {

	DEFAULT(null, "default", null, null, null),

	GLX(GLXContextCreator::new, "glx", null, Platform.LINUX, X11::getLibrary),
	EGL(EGLContextCreator::new, "egl", null, null, org.lwjgl.egl.EGL::getFunctionProvider),
	WGL(WGLContextCreator::new, "wgl", null, Platform.WINDOWS, GDI32::getLibrary),
	JOGL(JOGLContextCreator::new, "jogl", Platform.MACOSX, null, null),
	// Vulkan-via-MoltenVK is the macOS default. It attaches a CAMetalLayer to the
	// AWT Canvas's NSView, which is the only surface mode that composites correctly
	// inside Swing's layer tree on Apple Silicon / macOS 14+.
	VULKAN(VulkanContextCreator::new, "vulkan", null, Platform.MACOSX, VK::getFunctionProvider),

	GLFW(GLFWContextCreator::new, "glfw", null, null, org.lwjgl.glfw.GLFW::getLibrary),
	GLFW_VULKAN(GLFWVulkanContextCreator::new, "glfw-vulkan", null, null, VK::getFunctionProvider),
	LWJGLX_GL(LWJGLXContextCreator::new, "lwjglx-gl", null, null, null),
	LWJGLX_VK(VkLWJGLXContextCreator::new, "lwjglx-vk", null, null, VK::getFunctionProvider),

	VULKAN_OFFSCREEN(OffscreenVulkanContextCreator::new, "vulkan-offscreen", null, null, VK::getFunctionProvider),
	;

	EBackendType(BiFunction<ContextContainer, Boolean, ContextCreator<?>> creator, String cc_name, Platform platform, Platform default_for, Supplier<?> probe_function) {
		this.creator = creator;
		this.cc_name = cc_name;
		this.platform = platform;
		this.default_for = default_for;
		this.probe_function = probe_function;
	}

	public final BiFunction<ContextContainer, Boolean, ContextCreator<?>> creator;
	public final Platform platform, default_for;
	public final String cc_name;
	private final Supplier<?> probe_function;

	@Override
	public String toString() {
		return cc_name;
	}

	public boolean available(Platform platform) {
		if(probe_function != null) {
			try {
				probe_function.get();
			} catch (Throwable thrown) {
				return false;
			}
		} else if(this.platform != null){
			return this.platform == platform;
		}

		return true;
	}

	public ContextCreator<?> createContext(ContextContainer container, boolean debug) {
		return creator.apply(container, debug);
	}
}
