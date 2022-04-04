/*
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
 */
package jsettlers.common.action;

import jsettlers.common.map.shapes.MapRectangle;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.action.Action;
import jsettlers.common.action.EActionType;

/**
 * This action is fired whenever the visible map area has been changed by the user.
 * 
 * @author Michael Zangl
 * @see EActionType#SCREEN_CHANGE
 */
public class ScreenChangeAction extends Action {

	private final MapRectangle screenArea;
	private final ShortPoint2D centerPosition;

	/**
	 * Creates a new screen change action.
	 * 
	 * @param screenArea
	 *            the area
	 */
	public ScreenChangeAction(MapRectangle screenArea, ShortPoint2D centerPosition) {
		super(EActionType.SCREEN_CHANGE);
		this.screenArea = screenArea;
		this.centerPosition = centerPosition;
	}

	/**
	 * Gets the new area of the screen.
	 * 
	 * @return The screen area.
	 */
	public MapRectangle getScreenArea() {
		return screenArea;
	}

	/**
	 * Gets the center position of the new screen.
	 *
	 * @return The center psotion.
	 */
	public ShortPoint2D getCenterPosition() {
		return centerPosition;
	}
}
