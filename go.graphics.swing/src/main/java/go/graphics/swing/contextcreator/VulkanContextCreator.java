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
package go.graphics.swing.contextcreator;

import go.graphics.swing.vulkan.VulkanSurfaceOutput;
import org.lwjgl.PointerBuffer;
import org.lwjgl.awt.MacOSX;
import org.lwjgl.system.JNI;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Platform;
import org.lwjgl.system.SharedLibrary;
import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.LibFFI;
import org.lwjgl.system.macosx.MacOSXLibrary;
import org.lwjgl.system.macosx.ObjCRuntime;
import org.lwjgl.system.windows.WinBase;
import org.lwjgl.vulkan.EXTMetalSurface;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkMetalSurfaceCreateInfoEXT;
import org.lwjgl.vulkan.VkWin32SurfaceCreateInfoKHR;
import org.lwjgl.vulkan.VkXlibSurfaceCreateInfoKHR;

import java.awt.Rectangle;
import java.nio.DoubleBuffer;
import java.nio.LongBuffer;
import java.util.List;

import go.graphics.swing.ContextContainer;
import go.graphics.swing.vulkan.VulkanUtils;

import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.EXTMetalSurface.VK_STRUCTURE_TYPE_METAL_SURFACE_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.KHRWin32Surface.*;
import static org.lwjgl.vulkan.KHRXlibSurface.*;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanContextCreator extends JAWTContextCreator {
	public VulkanContextCreator(ContextContainer container, boolean debug) {
		super(container, debug);
	}

	private VulkanSurfaceOutput output;
	private VkInstance instance = null;
	private long debugCallback;

	// Hold the Metal/QuartzCore framework handles so they stay loaded for the lifetime of
	// this creator. Once a SharedLibrary is GC'd LWJGL dlclose()s it, and CAMetalLayer /
	// CALayer class lookups inside libMoltenVK then resolve to a half-loaded class object
	// and SIGABRT with "Attempt to use unknown class".
	private static SharedLibrary metalFramework;
	private static SharedLibrary quartzCoreFramework;

	// Strong references to the Obj-C objects we hand to MoltenVK. These are *not* under
	// ARC since we go through objc_msgSend directly, so AppKit's autorelease pool will
	// dealloc them on the next main-thread tick if we don't retain them ourselves. When
	// MoltenVK then dispatches a swapchain reconfigure on the main thread it does
	// [layer class] on the saved CAMetalLayer pointer, and Apple's runtime aborts with
	// "Attempt to use unknown class <addr>" because the instance has been freed.
	private static long retainedMetalDevice;
	private static long retainedInterLayer;
	private static long retainedMetalLayer;

	@Override
	protected void onNewConnection() throws ContextException {
		boolean wrapCtx = false;

		try(MemoryStack stack = MemoryStack.stackPush()) {

			if(instance == null) {
				List<String> extensions = VulkanUtils.defaultExtensionArray(stack, debug);
				if (Platform.get() == Platform.LINUX)
					extensions.add(VK_KHR_XLIB_SURFACE_EXTENSION_NAME);
				if (Platform.get() == Platform.WINDOWS)
					extensions.add(VK_KHR_WIN32_SURFACE_EXTENSION_NAME);

				instance = VulkanUtils.createInstance(stack, extensions, debug);
				// Always install the debug-report callback on macOS so MoltenVK errors don't
				// vanish silently. It's free if there are no errors and indispensable for
				// diagnosing a black canvas with no log output.
				boolean wantDebug = debug || Platform.get() == Platform.MACOSX;
				debugCallback = wantDebug ? VulkanUtils.setupDebugging(instance) : 0;
				wrapCtx = true;
			}

			LongBuffer surfacePtr = stack.mallocLong(1);
			if (Platform.get() == Platform.WINDOWS) {
				if (!instance.getCapabilities().VK_KHR_win32_surface)
					error("VK_KHR_win32_surface is missing.");

				VkWin32SurfaceCreateInfoKHR surfaceCreateInfo = VkWin32SurfaceCreateInfoKHR.calloc(stack)
						.sType(VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR)
						.hwnd(windowConnection)
						.hinstance(WinBase.GetModuleHandle(null, (String) null));

				if (vkCreateWin32SurfaceKHR(instance, surfaceCreateInfo, null, surfacePtr) != VK_SUCCESS) {
					error("Could not create a surface via VK_KHR_win32_surface.");
				}
			} else if (Platform.get() == Platform.LINUX) {
				if (!instance.getCapabilities().VK_KHR_xlib_surface)
					error("VK_KHR_xlib_surface is missing.");

				VkXlibSurfaceCreateInfoKHR surfaceCreateInfo = VkXlibSurfaceCreateInfoKHR.calloc(stack)
						.sType(VK_STRUCTURE_TYPE_XLIB_SURFACE_CREATE_INFO_KHR)
						.dpy(windowConnection)
						.window(windowDrawable);

				if (vkCreateXlibSurfaceKHR(instance, surfaceCreateInfo, null, surfacePtr) != VK_SUCCESS) {
					error("Could not create a surface via VK_KHR_xlib_surface.");
				}
			} else if (Platform.get() == Platform.MACOSX) {
				if (!instance.getCapabilities().VK_EXT_metal_surface)
					error("VK_EXT_metal_surface is missing (MoltenVK not available?).");

				// We must not use AWTVK.create here because it reopens a fresh JAWT lock
				// and the caller (JAWTContextCreator.paint) already holds it. We also
				// deliberately do not allocate an MTKView - MoltenVK only needs a
				// CAMetalLayer, and AWTVK's MTKView wrapper was the source of historic
				// "Attempt to use unknown class" aborts because nothing retained the view
				// across AppKit's main-thread autorelease drains.
				Rectangle bounds = canvas.getBounds();
				long metalLayer = createMetalLayer(windowConnection, bounds.x, bounds.y, bounds.width, bounds.height);

				// CRITICAL: pLayer is `const CAMetalLayer*` - the field directly stores the
				// layer pointer, not a pointer-to-pointer. MemoryStack.pointers(metalLayer)
				// would allocate a stack slot, write metalLayer into it, and return a buffer
				// whose address0() is the SLOT, so npLayer() would set pCreateInfo.pLayer
				// to the *slot address*. That works while the stack frame is alive, but
				// MoltenVK keeps the saved pointer for later use (vkCreateSwapchainKHR,
				// present, ...); once we leave the try-with-resources the slot is recycled
				// and MoltenVK is left holding a pointer into reused stack memory, producing
				// the long "Attempt to use unknown class <addr>" abort whenever the runtime
				// later does [savedPtr class].
				//
				// PointerBuffer.create(metalLayer, 1) gives us a buffer whose address0() is
				// metalLayer itself, so npLayer() writes the actual layer pointer into the
				// pCreateInfo field, matching the spec and lwjgl3-awt's
				// PlatformMacOSXVKCanvas.
				VkMetalSurfaceCreateInfoEXT surfaceCreateInfoEXT = VkMetalSurfaceCreateInfoEXT.calloc(stack)
						.sType(VK_STRUCTURE_TYPE_METAL_SURFACE_CREATE_INFO_EXT)
						.pLayer(PointerBuffer.create(metalLayer, 1));

				int err = EXTMetalSurface.vkCreateMetalSurfaceEXT(instance, surfaceCreateInfoEXT, null, surfacePtr);
				if(err != VK_SUCCESS) {
					error("vkCreateMetalSurfaceEXT failed: " + err);
				}
			}
			long surface = surfacePtr.get(0);
			output = new VulkanSurfaceOutput(surface);

			if(wrapCtx) {
				parent.wrapNewVkContext(instance, output);
			} else {
				output.setSurface(surface);
			}
		}
	}

	@Override
	protected void onNewDrawable() throws ContextException {
		// same event
		// onNewDrawable();
	}

	@Override
	public void stop() {
		if(debugCallback != 0) vkDestroyDebugReportCallbackEXT(instance, debugCallback, null);
		vkDestroyInstance(instance, null);
	}

	@Override
	protected void swapBuffers() throws ContextException {
		parent.swapBuffersVk();
	}

	@Override
	public void makeCurrent(boolean draw) {
		// OpenGL business
	}

	/**
	 * Allocates a {@code CAMetalLayer} directly (no {@code MTKView}, no {@code MetalKit})
	 * and parents it under the {@code JAWTSurfaceLayers*} that JAWT handed us via
	 * {@code surfaceinfo.platformInfo()}. The returned pointer is what we hand to
	 * {@code vkCreateMetalSurfaceEXT}.
	 *
	 * <p>Why no MTKView: lwjgl3-awt's {@code AWTVK.create} (and earlier mirrors of it)
	 * built an MTKView, then handed MoltenVK <em>the MTKView's owned layer</em>. The
	 * MTKView itself wasn't retained outside the local Java scope, so AppKit's
	 * main-thread autorelease pool would eventually dealloc it together with its
	 * layer; the next time MoltenVK touched the saved layer pointer (during swapchain
	 * reconfigure) Apple's runtime aborted with
	 * {@code "objc[*]: Attempt to use unknown class <addr>"} where {@code <addr>} was
	 * exactly the pointer returned from {@code [mtkView layer]}. MoltenVK does not
	 * need MTKView for anything; it only needs a {@code CAMetalLayer}.
	 *
	 * <p>Equivalent Objective-C:
	 * <pre>
	 *   id&lt;MTLDevice&gt; dev = MTLCreateSystemDefaultDevice();
	 *   CAMetalLayer *metalLayer = [[CAMetalLayer alloc] init];
	 *   [metalLayer setDevice:dev];
	 *   [metalLayer setPixelFormat:80];        // MTLPixelFormatBGRA8Unorm
	 *   [metalLayer setFramebufferOnly:YES];
	 *   [metalLayer setOpaque:YES];
	 *   [metalLayer setAutoresizingMask:18];   // width+height sizable
	 *   [metalLayer setFrame:CGRectMake(x,y,w,h)];
	 *
	 *   CALayer *interLayer = [[CALayer alloc] init];
	 *   [interLayer setFrame:CGRectMake(x,y,w,h)];
	 *   [interLayer addSublayer:metalLayer];
	 *
	 *   [platformInfo performSelectorOnMainThread:@selector(setLayer:)
	 *                                  withObject:interLayer waitUntilDone:YES];
	 *   return metalLayer;
	 * </pre>
	 *
	 * <p>Every Obj-C object we create here is stashed in a static field via
	 * {@code -retain} so AppKit's autorelease pool can't dealloc it out from under
	 * MoltenVK; without that retain MoltenVK's first {@code [layer class]} on the
	 * saved pointer aborts the runtime the moment it hits the main thread.
	 */
	private static long createMetalLayer(long surfaceLayers, int x, int y, int width, int height) {
		// Force-load Metal & QuartzCore. Cache the handles in static fields so LWJGL's
		// reference tracker doesn't dlclose() the frameworks between paints, which would
		// unregister CAMetalLayer / CALayer and trip "Attempt to use unknown class".
		if (metalFramework == null) {
			metalFramework = MacOSXLibrary.create("/System/Library/Frameworks/Metal.framework");
		}
		if (quartzCoreFramework == null) {
			// CAMetalLayer is declared in QuartzCore on macOS, even though it bridges Metal.
			// Without an explicit dlopen, its class isn't registered with the Obj-C runtime
			// in headless / Swing-only contexts and objc_getClass("CAMetalLayer") returns 0.
			quartzCoreFramework = MacOSXLibrary.create("/System/Library/Frameworks/QuartzCore.framework");
		}
		long mtlCreateDeviceFn = metalFramework.getFunctionAddress("MTLCreateSystemDefaultDevice");
		long objc_msgSend = ObjCRuntime.getLibrary().getFunctionAddress("objc_msgSend");

		long device = JNI.invokeP(mtlCreateDeviceFn);
		if (device == 0L) {
			throw new IllegalStateException("MTLCreateSystemDefaultDevice() returned NULL");
		}
		// Retain into a static so the device outlives any autorelease pool drain.
		retainedMetalDevice = JNI.invokePPP(device, ObjCRuntime.sel_getUid("retain"), objc_msgSend);

		long caMetalLayerClass = ObjCRuntime.objc_getClass("CAMetalLayer");
		if (caMetalLayerClass == 0L) {
			throw new IllegalStateException("objc_getClass(\"CAMetalLayer\") returned 0; QuartzCore not loaded?");
		}
		long caLayerClass = ObjCRuntime.objc_getClass("CALayer");
		if (caLayerClass == 0L) {
			throw new IllegalStateException("objc_getClass(\"CALayer\") returned 0");
		}

		// CAMetalLayer *metalLayer = [[CAMetalLayer alloc] init];
		long metalLayerAlloc = JNI.invokePPP(caMetalLayerClass, ObjCRuntime.sel_getUid("alloc"), objc_msgSend);
		if (metalLayerAlloc == 0L) {
			throw new IllegalStateException("[CAMetalLayer alloc] returned nil");
		}
		long metalLayer = JNI.invokePPP(metalLayerAlloc, ObjCRuntime.sel_getUid("init"), objc_msgSend);
		if (metalLayer == 0L) {
			throw new IllegalStateException("[CAMetalLayer init] returned nil");
		}
		// init returns the same pointer +1 retained; retain again into a static so the
		// next main-thread autorelease drain doesn't dealloc the layer MoltenVK is using.
		retainedMetalLayer = JNI.invokePPP(metalLayer, ObjCRuntime.sel_getUid("retain"), objc_msgSend);

		// [metalLayer setDevice:dev];
		JNI.invokePPPV(metalLayer, ObjCRuntime.sel_getUid("setDevice:"), device, objc_msgSend);
		// [metalLayer setPixelFormat:MTLPixelFormatBGRA8Unorm]; // 80
		JNI.invokePPPV(metalLayer, ObjCRuntime.sel_getUid("setPixelFormat:"), 80L, objc_msgSend);
		// [metalLayer setFramebufferOnly:YES];
		JNI.invokePPPV(metalLayer, ObjCRuntime.sel_getUid("setFramebufferOnly:"), 1L, objc_msgSend);
		// [metalLayer setOpaque:YES];
		JNI.invokePPPV(metalLayer, ObjCRuntime.sel_getUid("setOpaque:"), 1L, objc_msgSend);
		// [metalLayer setAutoresizingMask:(kCALayerWidthSizable | kCALayerHeightSizable)] // 18
		JNI.invokePPPV(metalLayer, ObjCRuntime.sel_getUid("setAutoresizingMask:"), 18L, objc_msgSend);
		// [metalLayer setFrame:CGRectMake(x,y,w,h)];
		invokeSetFrame(metalLayer, ObjCRuntime.sel_getUid("setFrame:"), x, y, width, height, objc_msgSend);

		// CALayer *interLayer = [[CALayer alloc] init];   (NOT +layer, which is autoreleased)
		long interAlloc = JNI.invokePPP(caLayerClass, ObjCRuntime.sel_getUid("alloc"), objc_msgSend);
		if (interAlloc == 0L) {
			throw new IllegalStateException("[CALayer alloc] returned nil");
		}
		long interLayer = JNI.invokePPP(interAlloc, ObjCRuntime.sel_getUid("init"), objc_msgSend);
		if (interLayer == 0L) {
			throw new IllegalStateException("[CALayer init] returned nil");
		}
		retainedInterLayer = interLayer; // +1 from alloc/init, no extra retain needed

		invokeSetFrame(interLayer, ObjCRuntime.sel_getUid("setFrame:"), x, y, width, height, objc_msgSend);
		JNI.invokePPPV(interLayer, ObjCRuntime.sel_getUid("addSublayer:"), metalLayer, objc_msgSend);

		// AppKit insists layer-tree mutation goes through the main thread. Skipping the
		// hop produced "Attempt to use unknown class" the moment MoltenVK ran a swapchain
		// reconfigure on the main thread.
		JNI.invokePPPPV(
				surfaceLayers,
				ObjCRuntime.sel_getUid("performSelectorOnMainThread:withObject:waitUntilDone:"),
				ObjCRuntime.sel_getUid("setLayer:"),
				interLayer,
				ObjCRuntime.YES,
				objc_msgSend);

		// Force a CoreAnimation transaction commit so the layer assignment is visible to
		// the main thread (and to MoltenVK) before we return.
		MacOSX.caFlush();

		return metalLayer;
	}

	/**
	 * Invokes {@code -[layer setFrame:CGRectMake(x,y,w,h)]} via libffi. CGRect's four
	 * doubles are passed by value, which {@code objc_msgSend} doesn't expose through
	 * LWJGL's pre-generated JNI wrappers (those only cover pointer/long combinations).
	 */
	private static void invokeSetFrame(long self, long sel,
	                                   double x, double y, double width, double height,
	                                   long objc_msgSend) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			PointerBuffer argumentTypes = stack.pointers(
					LibFFI.ffi_type_pointer,
					LibFFI.ffi_type_pointer,
					LibFFI.ffi_type_double,
					LibFFI.ffi_type_double,
					LibFFI.ffi_type_double,
					LibFFI.ffi_type_double);

			FFICIF cif = FFICIF.malloc(stack);
			int rc = LibFFI.ffi_prep_cif(cif, LibFFI.FFI_DEFAULT_ABI, LibFFI.ffi_type_void, argumentTypes);
			if (rc != LibFFI.FFI_OK) throw new IllegalStateException("ffi_prep_cif failed: " + rc);

			DoubleBuffer rect = stack.doubles(x, y, width, height);
			PointerBuffer pointers = stack.pointers(self, sel);

			PointerBuffer arguments = stack.mallocPointer(6)
					.put(0, MemoryUtil.memAddress(pointers, 0))
					.put(1, MemoryUtil.memAddress(pointers, 1))
					.put(2, MemoryUtil.memAddress(rect, 0))
					.put(3, MemoryUtil.memAddress(rect, 1))
					.put(4, MemoryUtil.memAddress(rect, 2))
					.put(5, MemoryUtil.memAddress(rect, 3));

			LibFFI.ffi_call(cif, objc_msgSend, null, arguments);
		}
	}
}
