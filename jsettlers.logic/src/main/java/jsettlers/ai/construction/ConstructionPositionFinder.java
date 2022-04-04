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
package jsettlers.ai.construction;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.algorithms.construction.AbstractConstructionMarkableMap;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.MainGrid;

import static jsettlers.common.buildings.EBuildingType.*;
import static jsettlers.common.landscape.EResourceType.COAL;
import static jsettlers.common.landscape.EResourceType.GEMSTONE;
import static jsettlers.common.landscape.EResourceType.GOLDORE;
import static jsettlers.common.landscape.EResourceType.IRONORE;

/**
 * This is the low level AI. It is called by the high level AI which decides what to build. The purpose of this low level KI is, to determine WHERE to
 * build.
 *
 * @author codingberlin
 */
public abstract class ConstructionPositionFinder {

	protected final AiStatistics aiStatistics;
	protected final AbstractConstructionMarkableMap constructionMap;
	protected final byte playerId;
	protected final ECivilisation civilisation;

	protected ConstructionPositionFinder(Factory factory) {
		aiStatistics = factory.aiStatistics;
		constructionMap = factory.constructionMap;
		playerId = factory.playerId;
		civilisation = factory.civilisation;
	}

	public abstract ShortPoint2D findBestConstructionPosition();

	public static class Factory {

		final ECivilisation civilisation;
		final AiStatistics aiStatistics;
		final AbstractConstructionMarkableMap constructionMap;
		final byte playerId;

		public Factory(ECivilisation civilisation, AiStatistics aiStatistics, AbstractConstructionMarkableMap constructionMap, byte playerId) {
			this.civilisation = civilisation;
			this.aiStatistics = aiStatistics;
			this.constructionMap = constructionMap;
			this.playerId = playerId;
		}

		private Map<EBuildingType, ConstructionPositionFinder> constructionPositionFinders = new EnumMap<>(EBuildingType.class);

		public final ConstructionPositionFinder getBestConstructionPositionFinderFor(EBuildingType type) {
			ConstructionPositionFinder cached = constructionPositionFinders.get(type);
			if(cached == null) {
				constructionPositionFinders.put(type, cached = newConstructionPositionFinderFor(type));
			}

			return cached;
		}

		private ConstructionPositionFinder newConstructionPositionFinderFor(EBuildingType type) {
			switch (type) {
			case STONECUTTER:
				return new StoneCutterConstructionPositionFinder(this);
			case LUMBERJACK:
				return new LumberJackConstructionPositionFinder(this);
			case FORESTER:
				return new ForesterConstructionPositionFinder(this);
			case SAWMILL:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, LUMBERJACK);
			case TOWER:
			case BIG_TOWER:
			case CASTLE:
				return new MilitaryConstructionPositionFinder(this, type);
			case FARM:
				return new FarmConstructionPositionFinder(this);
			case WINEGROWER:
				return new WinegrowerConstructionPositionFinder(this);
			case COALMINE:
				return new MineConstructionPositionFinder(this, type, COAL);
			case IRONMINE:
				return new MineConstructionPositionFinder(this, type, IRONORE);
			case GOLDMINE:
				return new MineConstructionPositionFinder(this, type, GOLDORE);
			case GEMSMINE:
				return new MineConstructionPositionFinder(this, type, GEMSTONE);
			case WATERWORKS:
				return new WaterWorksConstructionPositionFinder(this);
			case IRONMELT:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, IRONMINE);
			case WEAPONSMITH:
			case TOOLSMITH:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, IRONMELT);
			case BARRACK:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, WEAPONSMITH);
			case MILL:
			case PIG_FARM:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, FARM);
			case BAKER:
			case BREWERY:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, MILL);
			case SLAUGHTERHOUSE:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, PIG_FARM);
			case TEMPLE:
				return new TempleConstructionPositionFinder(this);
			case BIG_TEMPLE:
				return new BigTempleConstructionPositionFinder(this);
			case GOLDMELT:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, GOLDMINE);
			case FISHER:
				return new FisherConstructionPositionFinder(this);
			case STOCK:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, GOLDMELT);
			case DISTILLERY:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, RICE_FARM);
			case RICE_FARM:
				return new RiceFarmConstructionPositionFinder(this);
			case BEEKEEPING:
				return new PlantingBuildingConstructionPositionFinder(this, type) {
					@Override
					protected boolean isMyPlantPlantable(MainGrid mainGrid, ShortPoint2D position) {
						return mainGrid.isHivePlantable(position);
					}
				};
			case MEAD_BREWERY:
				return new NearRequiredBuildingConstructionPositionFinder(this, type, BEEKEEPING);
			default:
				return new NearDiggersConstructionPositionFinder(this, type);
			}
		}

		public final ConstructionPositionFinder getBorderDefenceConstructionPosition(List<ShortPoint2D> threatenedBorder) {
			return new BorderDefenceConstructionPositionFinder(this, threatenedBorder);
		}

	}
}
