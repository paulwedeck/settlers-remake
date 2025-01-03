/*******************************************************************************
 * Copyright (c) 2015 - 2016
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
package jsettlers.mapcreator.mapvalidator.tasks.error;

import jsettlers.common.position.ShortPoint2D;
import jsettlers.mapcreator.mapvalidator.tasks.AbstractValidationTask;

/**
 * Validate all players start position
 * 
 * @author Andreas Butti
 */
public class ValidatePlayerStartPosition extends AbstractValidationTask {

	/**
	 * Constructor
	 */
	public ValidatePlayerStartPosition() {
	}

	@Override
	public void doTest() {
		addHeader("playerstart.header", null /* no autofix possible */);

		for (int player = 0; player < data.getPlayerCount(); player++) {
			ShortPoint2D point = data.getStartPoint(player);

			boolean invalid;
			if(data.hasStartBuildings()) {
				invalid = players[point.x][point.y] != player;
			} else {
				// a bit rough but will do for now
				invalid = data.getLandscape(point.x, point.y).isBlocking;
			}

			if(invalid) {
				addErrorMessage("playerstart.text", point, player);
			}

			// set a visible start point on the map
			borders[point.x][point.y] = true;

			// even if this startpoint is invalid, display the point in the right player color
			players[point.x][point.y] = (byte) player;

		}
	}

}
