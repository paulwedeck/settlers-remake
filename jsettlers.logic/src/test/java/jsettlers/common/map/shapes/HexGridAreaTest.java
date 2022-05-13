/*******************************************************************************
 * Copyright (c) 2015 - 2017
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.common.map.shapes;

import java.util.Optional;
import jsettlers.common.map.shapes.HexGridArea.HexGridAreaIterator;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.mutables.MutableInt;
import jsettlers.logic.utils.DebugImagesHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.BitSet;

import static org.junit.Assert.*;

public class HexGridAreaTest {

	@BeforeClass
	public static void setup() {
		// DebugImagesHelper.DEBUG_IMAGES_ENABLED = true;
		DebugImagesHelper.setupDebugging();
	}

	@Test
	public void testSinglePoint() {
		HexGridArea area = new HexGridArea(10, 10, 0, 0);
		HexGridAreaIterator iter = area.iterator();

		assertTrue(iter.hasNext());
		assertEquals(new ShortPoint2D(10, 10), iter.next());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testCircleRadius1() {
		ShortPoint2D center = new ShortPoint2D(10, 10);
		int startRadius = 1;
		int maxRadius = 1;
		int expectedCount = 6;

		assertPositions(center, startRadius, maxRadius, expectedCount);
	}

	@Test
	public void testCircleRadius1To2() {
		ShortPoint2D center = new ShortPoint2D(10, 10);
		int startRadius = 1;
		int maxRadius = 2;
		int expectedCount = 6 + 12;

		assertPositions(center, startRadius, maxRadius, expectedCount);
	}

	@Test
	public void testCircleRadius0To2() {
		ShortPoint2D center = new ShortPoint2D(10, 10);
		int startRadius = 0;
		int maxRadius = 2;
		int expectedCount = 1 + 6 + 12;

		assertPositions(center, startRadius, maxRadius, expectedCount);
	}

	@Test
	public void testCircleRadius4To6() {
		ShortPoint2D center = new ShortPoint2D(10, 10);
		int startRadius = 4;
		int maxRadius = 6;
		int expectedCount = 4 * 6 + 5 * 6 + 6 * 6;

		assertPositions(center, startRadius, maxRadius, expectedCount);
	}

	private void assertPositions(ShortPoint2D center, int startRadius, int maxRadius, int expectedCount) {
		HexGridArea area = new HexGridArea(center.x, center.y, startRadius, maxRadius);

		int count = 0;
		for (ShortPoint2D pos : area) {
			count++;

			int onGridDist = center.getOnGridDistTo(pos);
			if (!(startRadius <= onGridDist && onGridDist <= maxRadius)) {
				fail("onGridDist: " + onGridDist + "   not in the expected range of [" + startRadius + "|" + maxRadius + "]   pos: " + pos);
			}
		}

		assertEquals(expectedCount, count);
	}

	@Test
	public void testIterateSinglePoint() {
		MutableInt counter = new MutableInt(0);
		HexGridArea.stream(10, 10, 0, 0).forEach((x, y) -> {
			counter.value++;
			assertEquals(new ShortPoint2D(10, 10), new ShortPoint2D(x, y));
		});

		assertEquals(1, counter.value);
	}

	@Test
	public void testIterateForResultStopsAfterResult() {
		int expectedVisits = 5;
		Object expectedResultObject = new Object();
		MutableInt counter = new MutableInt(0);

		Optional<Object> actualResultObject = HexGridArea.stream(10, 10, 3, 10).iterateForResult((x, y) -> {
			counter.value++;
			if (counter.value == expectedVisits) {
				return Optional.of(expectedResultObject);
			} else {
				return Optional.empty();
			}
		});

		assertEquals(expectedVisits, counter.value);
		assertTrue(actualResultObject.isPresent());
		assertSame(expectedResultObject, actualResultObject.get());
	}

	@Test
	public void testIterateStopsAfterFalse() {
		int expectedVisits = 5;
		MutableInt counter = new MutableInt(0);

		boolean wasNotStopped = HexGridArea.stream(10, 10, 3, 10).iterate((x, y) -> {
			counter.value++;
			return counter.value != expectedVisits;
		});

		assertEquals(expectedVisits, counter.value);
		assertFalse(wasNotStopped);
	}

	@Test
	public void testIterateCircleRadius1() {
		ShortPoint2D center = new ShortPoint2D(10, 10);
		int startRadius = 1;
		int maxRadius = 1;
		int expectedCount = 6;

		assertPositionsIterate(center, startRadius, maxRadius, expectedCount);
	}

	@Test
	public void testIterateCircleRadius1To2() {
		ShortPoint2D center = new ShortPoint2D(10, 10);
		int startRadius = 1;
		int maxRadius = 2;
		int expectedCount = 6 + 12;

		assertPositionsIterate(center, startRadius, maxRadius, expectedCount);
	}

	@Test
	public void testIterateRadius0To2() {
		ShortPoint2D center = new ShortPoint2D(10, 10);
		int startRadius = 0;
		int maxRadius = 2;
		int expectedCount = 1 + 6 + 12;

		assertPositionsIterate(center, startRadius, maxRadius, expectedCount);
	}

	@Test
	public void testIterateRadius4To6() {
		ShortPoint2D center = new ShortPoint2D(10, 10);
		int startRadius = 4;
		int maxRadius = 6;
		int expectedCount = 4 * 6 + 5 * 6 + 6 * 6;

		assertPositionsIterate(center, startRadius, maxRadius, expectedCount);
	}

	private void assertPositionsIterate(ShortPoint2D center, int startRadius, int maxRadius, int expectedCount) {
		int width = center.x + maxRadius + 1;
		int height = center.y + maxRadius + 1;
		BitSet positions = new BitSet(width * height);

		MutableInt counter = new MutableInt(0);

		HexGridArea.stream(center.x, center.y, startRadius, maxRadius)
				.forEach((x, y) -> {
					int onGridDist = center.getOnGridDistTo(new ShortPoint2D(x, y));
					if (!(startRadius <= onGridDist && onGridDist <= maxRadius)) {
						fail("onGridDist: " + onGridDist + "   not in the expected range of [" + startRadius + "|" + maxRadius + "]   pos: ("
								+ x + "|" + y
								+ ")");
					}
					positions.set(x + y * width);
					DebugImagesHelper.writeDebugImageBoolean("count-" + counter.value, width, height,
							(imageX, imageY) -> positions.get(imageX + imageY * width));
					counter.value++;
				});

		assertEquals(expectedCount, counter.value);
	}
}
