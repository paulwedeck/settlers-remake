/*******************************************************************************
 * Copyright (c) 2015 - 2017
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
package jsettlers.ai.construction;

import java.util.ArrayList;
import java.util.List;

import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.position.ShortPoint2D;

import static jsettlers.common.buildings.EBuildingType.FISHER;

/**
 * Algorithm: find all possible construction points within the borders of the player - calculates a score based on the amount of fish
 * 
 * @author codingberlin
 */
public class FisherConstructionPositionFinder extends ConstructionPositionFinder {

	private final BuildingVariant fisher;

	protected FisherConstructionPositionFinder(Factory factory) {
		super(factory);

		fisher = FISHER.getVariant(civilisation);
	}

	@Override
	public ShortPoint2D findBestConstructionPosition() {
		List<ScoredConstructionPosition> scoredConstructionPositions = new ArrayList<>();

		int fishDistance = fisher.getWorkRadius();
		for (ShortPoint2D point : aiStatistics.getLandForPlayer(playerId)) {
			if (aiStatistics.wasFishNearByAtGameStart(point, civilisation) && constructionMap.canConstructAt(point.x, point.y, FISHER, playerId)
					&& !aiStatistics.blocksWorkingAreaOfOtherBuilding(point.x, point.y, playerId, fisher)) {
				ShortPoint2D fishPosition = aiStatistics.getNearestFishPointForPlayer(point, playerId, fishDistance);
				if (fishPosition != null) {
					fishDistance = point.getOnGridDistTo(fishPosition);
					scoredConstructionPositions.add(new ScoredConstructionPosition(point, fishDistance));
				}
			}
		}

		return ScoredConstructionPosition.detectPositionWithLowestScore(scoredConstructionPositions);
	}
}
