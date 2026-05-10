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

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;

import javax.swing.SwingUtilities;

import go.graphics.swing.ContextContainer;

public abstract class ContextCreator<T extends Component> implements ComponentListener{

	public ContextCreator(ContextContainer ac, boolean debug) {
		parent = ac;
		this.debug = debug;
	}

	protected int width = 1, height = 1;
	protected int new_width = 1, new_height = 1;
	protected boolean change_res = true;
	protected final Object wnd_lock = new Object();
	protected boolean first_draw = true;
	protected int fpsLimit = 0;
	protected T canvas;
	protected ContextContainer parent;
	protected boolean debug;


	public abstract void stop();
	public abstract void initSpecific();

	public void repaint() {
		canvas.repaint();
	}

	public void requestFocus() {
		canvas.requestFocus();
	}


	protected void error(String message) throws ContextException {
		parent.fatal(message);
		throw new ContextException();
	}

	public void init() {
		initSpecific();

		parent.addCanvas(canvas);

		canvas.addComponentListener(this);
	}

	@Override
	public void componentResized(ComponentEvent componentEvent) {
		// Don't gate on window focus: on macOS, layout passes during a frame transition
		// (e.g. lobby -> in-game) often happen with the window unfocused, and dropping
		// the size update leaves the swapchain stuck at 1x1.
		java.awt.Window window = SwingUtilities.windowForComponent(canvas);
		if(window == null) return;

		synchronized (wnd_lock) {
			AffineTransform scaleInfo = canvas.getGraphicsConfiguration().getDefaultTransform();

			double scaleX = 1;
			double scaleY = 1;
			if(scaleInfo != null) {
				scaleX = scaleInfo.getScaleX();
				scaleY = scaleInfo.getScaleX();
			}
			new_width = (int) (canvas.getWidth()*scaleX);
			new_height = (int) (canvas.getHeight()*scaleY);
			change_res = true;

			if(new_width == 0) new_width = 1;
			if(new_height == 0) new_height = 1;
		}
	}

	@Override
	public void componentHidden(ComponentEvent componentEvent) {}

	@Override
	public void componentMoved(ComponentEvent componentEvent) {}

	@Override
	public void componentShown(ComponentEvent componentEvent) {}

	public void updateFPSLimit(int fpsLimit) {
		this.fpsLimit = fpsLimit;
	}
}
