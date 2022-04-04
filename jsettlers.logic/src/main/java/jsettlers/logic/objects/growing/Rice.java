/*******************************************************************************
 * Copyright (c) 2015
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
package jsettlers.logic.objects.growing;

import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.objects.IMapObjectsManagerGrid;
import jsettlers.logic.map.grid.objects.MapObjectsManager;

/**
 * This is a Riceplant on the map.
 * 
 * @author MarviMarv
 * 
 */
public final class Rice extends GrowingObject {
	private static final long serialVersionUID = 8086261556083213266L;

	public static final float GROWTH_DURATION = 11 * 60;
	public static final float DECOMPOSE_DURATION = 3 * 60;
	public static final float REMOVE_DURATION = 2 * 60;

	/**
	 * Creates a new Rice.
	 *
	 * @param grid
	 */
	public Rice(ShortPoint2D pos) {
		super(pos, EMapObjectType.RICE_GROWING);
	}

	@Override
	protected float getGrowthDuration() {
		return GROWTH_DURATION;
	}

	@Override
	protected float getDecomposeDuration() {
		return DECOMPOSE_DURATION;
	}

	@Override
	protected EMapObjectType getDeadState() {
		return EMapObjectType.RICE_DEAD;
	}

	@Override
	protected EMapObjectType getAdultState() {
		return EMapObjectType.RICE_HARVESTABLE;
	}

	@Override
	protected void handlePlacement(int x, int y, MapObjectsManager mapObjectsManager, IMapObjectsManagerGrid grid) {
		super.handlePlacement(x, y, mapObjectsManager, grid);
	}

	@Override
	protected void handleRemove(int x, int y, MapObjectsManager mapObjectsManager, IMapObjectsManagerGrid grid) {
		super.handleRemove(x, y, mapObjectsManager, grid);
	}
}