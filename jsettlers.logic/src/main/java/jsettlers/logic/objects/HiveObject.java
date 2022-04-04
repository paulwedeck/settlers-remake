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
package jsettlers.logic.objects;

import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.objects.IMapObjectsManagerGrid;
import jsettlers.logic.map.grid.objects.MapObjectsManager;


/**
 * This is a hive on a map.
 *
 * TODO: orginial hives are dependent on the amount of trees around them
 * TODO: beekeeper has a lead time of 8 minutes before he harvests at all, he also has an interval which limits how often he harvests hive
 * 
 * @author MarviMarv
 * 
 */
public final class HiveObject extends ProgressingSoundableObject {
	private static final long serialVersionUID = -8416528218446090558L;

	private static final int[] EMPTY_DURATION_BOUNDS = {90, 180};
	private static final int[] GROWTH_DURATION_BOUNDS = {110, 180};
	//private static final int[] BEE_ANIMATION_BOUNDS = {0, 5};

	private EMapObjectType state;

	//private int beeAnimation;
	private int emptyDuration;
	private int growingDuration;

	/**
	 * Creates a new Hive.
	 *
	 * @param grid
	 */
	public HiveObject(ShortPoint2D pos) {
		super(pos);
		initNextCycle();
	}

	private void initNextCycle() {
		//beeAnimation = generateRandomBeeAnimation();
		emptyDuration = getRandomEmptyDuration();
		growingDuration = getRandomGrowthDuration();
		state = EMapObjectType.HIVE_EMPTY;
		super.setDuration(emptyDuration);
	}

	@Override
	protected void changeState() {
		if (state == EMapObjectType.HIVE_EMPTY) {
			state = EMapObjectType.HIVE_GROWING;
			super.setDuration(growingDuration);
		} else  if (state == EMapObjectType.HIVE_GROWING) {
			state = EMapObjectType.HIVE_HARVESTABLE;
		}
		//harvestable change is done in cutOff method
	}

	@Override
	public boolean cutOff() {
		if (state == EMapObjectType.HIVE_HARVESTABLE) {
			initNextCycle();
			return true;
		}

		return false;
	}

	@Override
	public boolean canBeCut() {
		return true;
	}

	@Override
	protected void handlePlacement(int x, int y, MapObjectsManager mapObjectsManager, IMapObjectsManagerGrid grid) {
		super.handlePlacement(x, y, mapObjectsManager, grid);
	}

	@Override
	public EMapObjectType getObjectType() {
		return state;
	}

	private int getRandomEmptyDuration() {
		return MatchConstants.random().nextInt(EMPTY_DURATION_BOUNDS[0], EMPTY_DURATION_BOUNDS[1]);
	}

	private int getRandomGrowthDuration() {
		return MatchConstants.random().nextInt(GROWTH_DURATION_BOUNDS[0], GROWTH_DURATION_BOUNDS[1]);
	}

	public int getEmptyDuration() {
		return emptyDuration;
	}

	public int getGrowingDuration() {
		return growingDuration;
	}

	/*
	private int generateRandomBeeAnimation() {
		return MatchConstants.random().nextInt(BEE_ANIMATION_BOUNDS[0], BEE_ANIMATION_BOUNDS[1]);
	}

	public int getBeeAnimation() {
		return beeAnimation;
	}
	*/
}