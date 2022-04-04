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
package jsettlers.algorithms.construction;

import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.map.shapes.IMapArea;
import jsettlers.common.map.shapes.MapRectangle;
import jsettlers.common.position.RelativePoint;

/**
 * Algorithm to calculate the construction marks for the user.
 *
 * @author Andreas Eberle
 *
 */
public final class NewConstructionMarksAlgorithm {
	private final AbstractConstructionMarkableMap map;
	private final byte playerId;

	private MapRectangle lastArea = null;

	public NewConstructionMarksAlgorithm(AbstractConstructionMarkableMap map, byte player) {
		this.map = map;
		this.playerId = player;
	}

	public void calculateConstructMarks(final MapRectangle mapArea, BuildingVariant buildingVariant) {
		if (lastArea != null) {
			removeConstructionMarks(lastArea, mapArea);
		}

		boolean binaryConstructionMarkValues = !buildingVariant.needsFlattenedGround();
		RelativePoint[] buildingArea = buildingVariant.getBuildingArea();

		final short height = mapArea.getHeight();
		final short width = mapArea.getWidth();

		for(short line = 0; line < height; line++) {
			short y = (short) mapArea.getLineY(line);

			short minX = (short) mapArea.getLineStartX(line);

			for(short tile = 0; tile < width; tile++) {
				short x = (short) (minX + tile);

				if(map.canConstructAt(x, y, buildingVariant.getType(), playerId)) {
					map.setConstructMarking(x, y, true, binaryConstructionMarkValues, buildingArea);
				} else {
					map.setConstructMarking(x, y, false, false, null);
				}

			}
		}

		// set the lastArea variable for the next run
		lastArea = mapArea;
	}

	/**
	 * Removes all construction marks on the screen.
	 */
	public void removeConstructionMarks() {
		if (lastArea != null) {
			lastArea.stream()
					.filterBounds(map.getWidth(), map.getHeight())
					.forEach((x, y) -> map.setConstructMarking(x, y, false, false, null));
			lastArea = null;
		}
	}

	/**
	 * Removes all construction marks in the given area.
	 * 
	 * @param area
	 *            The area to remove the marks
	 * @param notIn
	 *            The area of marks that should be skipped.
	 */
	private void removeConstructionMarks(IMapArea area, IMapArea notIn) {
		area.stream()
				.filterBounds(map.getWidth(), map.getHeight())
				.filter((x, y) -> !notIn.contains(x, y))
				.forEach((x, y) -> map.setConstructMarking(x, y, false, false, null));
	}
}
