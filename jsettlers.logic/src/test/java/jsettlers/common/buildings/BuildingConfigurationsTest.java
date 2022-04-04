/*
 * Copyright (c) 2016 - 2017
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
package jsettlers.common.buildings;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jsettlers.common.buildings.stacks.ConstructionStack;
import jsettlers.common.movable.EDirection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.RelativePoint;

@RunWith(Parameterized.class)
public class BuildingConfigurationsTest {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> buildingTypes() {
		List<Object[]> result = new ArrayList<>();
		for (EBuildingType buildingType : EBuildingType.VALUES) {
			for(ECivilisation civilisation : ECivilisation.VALUES) {
				BuildingVariant building = buildingType.getVariant(civilisation);
				if(building != null) {
					result.add(new Object[]{building});
				}
			}
		}
		return result;
	}

	private final BuildingVariant building;

	public BuildingConfigurationsTest(BuildingVariant building) {
		this.building = building;
	}

	@Test
	public void testDoorIsNotBlockedButProtected() {
		assumeFalse(building.isVariantOf(EBuildingType.TEMPLE)); // temple uses door location for the wine bowl
		assumeFalse(building.isVariantOf(EBuildingType.MARKET_PLACE)); // market place does not use the door

		assertFalse(isBlocked(building.getDoorTile()));
		assertTrue(isProtected(building.getDoorTile()));
	}

	@Test
	public void isMineValid() {
		assumeTrue(building.isMine());
		// mine may only have one offer stack
		assertEquals(building.getOfferStacks().length, 1);
		assertTrue(building.getMineSettings().getFoodOrder().length > 0);
	}

	@Test
	public void testStacksAreNotBlockedButProtected() {
		for (RelativeStack stack : building.getConstructionStacks()) {
			assertFalse(building + "", isBlocked(stack));
			assertTrue(building + "", isProtected(stack));
		}
		for (RelativeStack stack : building.getRequestStacks()) {
			assertFalse(building + "", isBlocked(stack));
			assertTrue(building + "", isProtected(stack));
		}
		for (RelativeStack stack : building.getOfferStacks()) {
			assertFalse(building + "", isBlocked(stack));
			assertTrue(building + "", isProtected(stack));
		}
	}

	@Test
	public void testConstructionStacksHaveValidSize() {
		for(ConstructionStack stack : building.getConstructionStacks()) {
			assertTrue(stack + " has " + stack.requiredForBuild() + " elements but can only have 8", stack.requiredForBuild() <= 8);
			assertTrue(stack + " must have at least one element", stack.requiredForBuild() > 0);
		}
	}

	@Test
	public void testBricklayerPositionsAreNotBlockedButProtected() {
		for(RelativePoint bricklayerPosition : building.getBricklayers()) {
			assertTrue(building + ": " + bricklayerPosition + " is not protected!", isProtected(bricklayerPosition));
			assumeFalse(building + ": " + bricklayerPosition + " is blocked!", isBlocked(bricklayerPosition));
		}
	}

	@Test
	public void testBuildingsAreSurroundedByProtectedPositions() {
		for(RelativePoint pos : building.getBlockedTiles()) {
			for(EDirection dir : new EDirection[] {EDirection.NORTH_EAST, EDirection.SOUTH_EAST, EDirection.NORTH_WEST, EDirection.SOUTH_WEST, EDirection.EAST, EDirection.WEST}) {
				RelativePoint neighbour = new RelativePoint(pos.getDx() + dir.getGridDeltaX(), pos.getDy() + dir.getGridDeltaY());

				assertTrue(pos + " has a non protected neighbour!", isProtected(neighbour));
			}
		}
	}

	private boolean isBlocked(RelativePoint position) {
		return contains(building.getBlockedTiles(), position);
	}

	private boolean isProtected(RelativePoint position) {
		return contains(building.getProtectedTiles(), position);
	}

	private static boolean contains(RelativePoint[] positions, RelativePoint positionToCheck) {
		for (RelativePoint current : positions) {
			if (current.equals(positionToCheck)) {
				return true;
			}
		}
		return false;
	}
}
