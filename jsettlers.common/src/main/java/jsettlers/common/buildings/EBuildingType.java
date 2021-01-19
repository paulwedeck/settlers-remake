/*******************************************************************************
 * Copyright (c) 2015, 2016
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
package jsettlers.common.buildings;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import jsettlers.common.buildings.jobs.IBuildingJob;
import jsettlers.common.buildings.loader.BuildingFile;
import jsettlers.common.buildings.stacks.ConstructionStack;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.images.ImageLink;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.RelativePoint;

import static jsettlers.common.player.ECivilisation.REPLACE_ME;

/**
 * This interface defines the main building type.
 * 
 * @author Michael Zangl
 * @author Andreas Eberle
 */
public enum EBuildingType {
	STONECUTTER,
	FORESTER,
	LUMBERJACK,
	SAWMILL,

	COALMINE,
	IRONMINE,
	GOLDMINE,
	GOLDMELT,
	IRONMELT,
	TOOLSMITH,
	WEAPONSMITH,

	FARM,
	PIG_FARM,
	/**
	 * Needs to implement {@link IBuilding.IMill}
	 */
	MILL,
	WATERWORKS,
	SLAUGHTERHOUSE,
	BAKER,
	FISHER,
	WINEGROWER,
	CHARCOAL_BURNER,
	DONKEY_FARM,

	SMALL_LIVINGHOUSE,
	MEDIUM_LIVINGHOUSE,
	BIG_LIVINGHOUSE,

	LOOKOUT_TOWER,
	TOWER,
	BIG_TOWER,
	CASTLE,
	HOSPITAL,
	BARRACK,

	DOCKYARD,
	HARBOR,
	STOCK,

	TEMPLE,
	BIG_TEMPLE,

	MARKET_PLACE,

	SULFURMINE,
	GEMSMINE,

	CATAPULT_HALL, //TODO: needs specific implementation, also Ballistenwerkstatt, Kanonenwerkstatt
	BREWERY,
	GUNPOWDER_MAKER,
	DISTILLERY,
	RICE_FARM,
	BEEKEEPERS_HUT,
	MEADMAKERS_HUT,
	ALCHEMISTS_DEN;

	/**
	 * A copy of {@link #values()}. Do not modify this array. This is intended for quicker access to this value.
	 */
	public static final EBuildingType[] VALUES = EBuildingType.values();

	/**
	 * The number of buildings in the {@link #VALUES} array.
	 */
	public static final int NUMBER_OF_BUILDINGS = VALUES.length;
	public static final EnumSet<EBuildingType> MILITARY_BUILDINGS = EnumSet.of(TOWER, BIG_TOWER, CASTLE);

	/**
	 * The ordinal of this type. Yields more performance than using {@link #ordinal()}
	 */
	public final int ordinal;

	private final Map<ECivilisation, BuildingVariant> buildingVariants = new EnumMap<>(ECivilisation.class);

	/**
	 * Constructs an enum object.
	 */
	EBuildingType() {
		this.ordinal = ordinal();

		for(ECivilisation civilisation : ECivilisation.VALUES) {
			try {
				buildingVariants.put(civilisation, new BuildingVariant(this, civilisation));
			} catch (FileNotFoundException ex) {
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	public BuildingVariant getVariant(ECivilisation civilisation) {
		return buildingVariants.get(civilisation);
	}

	public BuildingVariant[] getVariants() {
		return buildingVariants.values().toArray(new BuildingVariant[0]);
	}

	/**
	 * Gets the tiles that are protected by this building. On thse tiles, no other buildings may be build.
	 * 
	 * @return The tiles as array.
	 */
	public final RelativePoint[] getProtectedTiles() {
		return buildingVariants.get(REPLACE_ME).getProtectedTiles();
	}

	/**
	 * Gets the ground types this building can be placed on.
	 * 
	 * @return The ground types.
	 */
	public final Set<ELandscapeType> getGroundTypes() {
		return buildingVariants.get(REPLACE_ME).getGroundTypes();
	}

	/**
	 * Checks if this building is a mine.
	 * 
	 * @return <code>true</code> iff this building is a mine.
	 */
	public boolean isMine() {
		return buildingVariants.get(REPLACE_ME).isMine();
	}

	public boolean needsFlattenedGround() {
		return buildingVariants.get(REPLACE_ME).needsFlattenedGround();
	}

	/**
	 * Checks if this building is a military building.
	 * 
	 * @return <code>true</code> iff this is a military building.
	 */
	public boolean isMilitaryBuilding() {
		return MILITARY_BUILDINGS.contains(this);
	}
}
