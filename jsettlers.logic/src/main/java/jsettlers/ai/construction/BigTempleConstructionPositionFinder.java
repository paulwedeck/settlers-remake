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
import jsettlers.shared.ShortPoint2D;

/**
 * @author codingberlin
 */
public class BigTempleConstructionPositionFinder extends NearDiggersConstructionPositionFinder {

	public BigTempleConstructionPositionFinder(Factory factory) {
		super(factory, EBuildingType.BIG_TEMPLE);
	}
	@Override
	public ShortPoint2D findBestConstructionPosition() {
		if (aiStatistics.getTotalNumberOfBuildingTypeForPlayer(EBuildingType.TEMPLE, playerId) < 1) {
			return null; // do not construct big temple - you don't need it before small temples produce the remaining 2 mana for first level2
		} else {
			return super.findBestConstructionPosition();
		}
	}
}
