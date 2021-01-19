/*******************************************************************************
 * Copyright (c) 2021
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
package jsettlers.logic.buildings.workers;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.player.Player;

import java.util.Arrays;

/**
 * This is a worker building with one or more animations, you cann add transition durations in order to draw all animations in a specific time order
 * 
 * @author MarviMarv
 */
public abstract class WorkerAnimationBuilding extends WorkerBuilding implements IBuilding.IWorkAnimation {
	private static final long serialVersionUID = 1210188934302286757L;

	private int[] animationStartTime;
	private boolean[] isAnimationRequested;

	protected WorkerAnimationBuilding(EBuildingType type, Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
		super(type, player, position, buildingsGrid);

		animationStartTime = new int[getAnimationCount()];
		isAnimationRequested = new boolean[getAnimationCount()];

		for (int i = 0; i < getAnimationCount(); i++) {
			isAnimationRequested[i] = false;
			animationStartTime[i] = 0;
		}
	}

	protected abstract int[] getAnimationDurations();
	protected abstract int[] getAnimationTransitions();


	public int getAnimationCount() {
		if (getAnimationDurations() != null) {
			return getAnimationDurations().length;
		} else {
			return 0;
		}
	}

	private int getTransitionCount() {
		if (getAnimationTransitions() != null) {
			return getAnimationTransitions().length;
		} else {
			return 0;
		}
	}

	public boolean isAnimationRequested(int index) {
		if (index >= 0 && index < getAnimationCount()) {
			return isAnimationRequested[index];
		}

		return false;
	}

	public float getAnimationProgress(int index) {
		if (index >= 0 && index < getAnimationCount()) {
			if (isAnimationRequested[index]) {
				float progress = (MatchConstants.clock().getTime() - animationStartTime[index]) / (float)getAnimationDurations()[index];

				//check transition for possible next animation
				if (!isAnimationRequested(index + 1) && index < getTransitionCount()) {
					float nextAnimationQuotient = (float)getAnimationTransitions()[index] / getAnimationDurations()[index];

					if (progress >= nextAnimationQuotient) {
						requestAnimation(index + 1);
					}
				}

				if (progress < 1f) {
					return progress;
				} else {
					isAnimationRequested[index] = false;
					return 1f;
				}
			}
		}

		return 0f;
	}

	/**
	 * Invokes the animation for the building
	 *
	 * @return the duration of the animation which is requested by index
	 */
	public int requestAnimation(int index) {
		if (index >= 0 && index < getAnimationCount()) {
			if (!isAnimationRequested[index]) {
				isAnimationRequested[index] = true;
				animationStartTime[index] = MatchConstants.clock().getTime();
				return getAnimationDurations()[index];
			}
		}

		return 0;
	}

	/**
	 * Requests all animations
	 * @return entire duration of all animations
	 */
	public int requestAnimation() {
		requestAnimation(0);
		return getEntireDuration();
	}

	public int getEntireDuration() {
		if (getAnimationDurations() != null) {
			int transitionSum = 0;
			for (int i = 0; i < getTransitionCount(); i++) {
				transitionSum += getAnimationDurations()[i] - getAnimationTransitions()[i];
			}

			return Arrays.stream(getAnimationDurations()).sum() - transitionSum;
		}
		return 0;
	}
}
