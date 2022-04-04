/*******************************************************************************
 * Copyright (c) 2016
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

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.position.ShortPoint2D;

/**
 * @author codingberlin
 */
public class TempleConstructionPositionFinder extends NearRequiredBuildingConstructionPositionFinder {

	public static final int WINE_PER_TEMPLE = 15;

	public TempleConstructionPositionFinder(Factory factory) {
		super(factory, EBuildingType.TEMPLE, factory.civilisation.getMannaBuilding());
	}

	@Override
	public ShortPoint2D findBestConstructionPosition() {
		switch (civilisation) {
			case EGYPTIAN:
			case ASIAN:
			case AMAZON:
				return super.findBestConstructionPosition();
			default:
				int availableWine = aiStatistics.getTotalWineCountForPlayer(playerId);
				int usedWine = aiStatistics.getTotalNumberOfBuildingTypeForPlayer(EBuildingType.TEMPLE, playerId) *
						WINE_PER_TEMPLE;
				if (availableWine - usedWine >= WINE_PER_TEMPLE) {
					return super.findBestConstructionPosition();
				} else {
					// reject construction of temple - the wine is not grown yet
					return null;
				}
		}
	}

}
