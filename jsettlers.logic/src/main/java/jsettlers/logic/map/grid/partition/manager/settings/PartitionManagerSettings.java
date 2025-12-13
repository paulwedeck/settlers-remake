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
package jsettlers.logic.map.grid.partition.manager.settings;

import java.io.Serializable;
import java.util.HashMap;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.map.partition.IPartitionSettings;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.buildings.stack.multi.StockSettings;
import jsettlers.logic.map.grid.partition.manager.PartitionManager;

/**
 * This class bundles all settings for the {@link PartitionManager}.
 * 
 * @author Andreas Eberle
 * 
 */
public final class PartitionManagerSettings implements IPartitionSettings, Serializable {
	private static final long serialVersionUID = -6269898822727665606L;

    private static final HashMap<ECivilisation, HashMap<EMaterialType, HashMap<EBuildingType, Float>>> DEFAULT_MATERIAL_DISTRIBUTION = new HashMap<ECivilisation, HashMap<EMaterialType, HashMap<EBuildingType, Float>>>() {
        {
            put(ECivilisation.ROMAN, new HashMap<EMaterialType, HashMap<EBuildingType, Float>>() {
                {
                    put(EMaterialType.COAL, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.IRONMELT, 0.32f);
                        put(EBuildingType.GOLDMELT, 0.32f);
                        put(EBuildingType.WEAPONSMITH, 0.24f);
                        put(EBuildingType.TOOLSMITH, 0.12f);
                    }});
                    put(EMaterialType.IRON, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.WEAPONSMITH, 0.33f);
                        put(EBuildingType.TOOLSMITH, 0.33f);
                        put(EBuildingType.DOCKYARD, 0.16f);
                        // TODO: Add Catapult Hall
                    }});
                    put(EMaterialType.PLANK, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.BUILDING_SITE, 0.45f);
                        put(EBuildingType.DOCKYARD, 0.22f);
                        put(EBuildingType.CHARCOAL_BURNER, 0.11f);
                        // TODO: Add Catapult Hall
                    }});
                    put(EMaterialType.CROP, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.MILL, 0.4f);
                        put(EBuildingType.PIG_FARM, 0.4f);
                        put(EBuildingType.DONKEY_FARM, 0.2f);
                    }});
                    put(EMaterialType.WATER, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.BAKER, 0.4f);
                        put(EBuildingType.PIG_FARM, 0.4f);
                        put(EBuildingType.DONKEY_FARM, 0.2f);
                    }});
                    put(EMaterialType.BREAD, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.COALMINE, 1.0f);
                        put(EBuildingType.IRONMINE, 0.0f);
                        put(EBuildingType.GOLDMINE, 0.0f);
                        put(EBuildingType.GEMSMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                    put(EMaterialType.MEAT, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.IRONMINE, 1.0f);
                        put(EBuildingType.COALMINE, 0.0f);
                        put(EBuildingType.GOLDMINE, 0.0f);
                        put(EBuildingType.GEMSMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                    put(EMaterialType.FISH, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.GOLDMINE, 1.0f);
                        put(EBuildingType.GEMSMINE, 1.0f);
                        put(EBuildingType.COALMINE, 0.0f);
                        put(EBuildingType.IRONMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                }
            });
            put(ECivilisation.EGYPTIAN, new HashMap<EMaterialType, HashMap<EBuildingType, Float>>() {
                {
                    put(EMaterialType.COAL, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.IRONMELT, 0.32f);
                        put(EBuildingType.GOLDMELT, 0.32f);
                        put(EBuildingType.WEAPONSMITH, 0.24f);
                        put(EBuildingType.TOOLSMITH, 0.12f);
                    }});
                    put(EMaterialType.IRON, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.WEAPONSMITH, 0.33f);
                        put(EBuildingType.TOOLSMITH, 0.33f);
                        put(EBuildingType.DOCKYARD, 0.16f);
                        // TODO: Add Ballista Hall
                    }});
                    put(EMaterialType.PLANK, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.BUILDING_SITE, 0.45f);
                        put(EBuildingType.DOCKYARD, 0.22f);
                        put(EBuildingType.CHARCOAL_BURNER, 0.11f);
                        // TODO: Add Ballista Hall
                    }});
                    put(EMaterialType.TRUNK, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.SAWMILL, 0.8f);
                        // TODO: Add Ballista Hall
                    }});
                    put(EMaterialType.CROP, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.MILL, 0.33f);
                        put(EBuildingType.PIG_FARM, 0.33f);
                        put(EBuildingType.DONKEY_FARM, 0.16f);
                        put(EBuildingType.BREWERY, 0.18f);
                    }});
                    put(EMaterialType.WATER, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.BAKER, 0.33f);
                        put(EBuildingType.PIG_FARM, 0.33f);
                        put(EBuildingType.DONKEY_FARM, 0.16f);
                        put(EBuildingType.BREWERY, 0.18f);
                    }});
                    put(EMaterialType.BREAD, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.COALMINE, 1.0f);
                        put(EBuildingType.IRONMINE, 0.0f);
                        put(EBuildingType.GOLDMINE, 0.0f);
                        put(EBuildingType.GEMSMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                    put(EMaterialType.MEAT, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.IRONMINE, 1.0f);
                        put(EBuildingType.COALMINE, 0.0f);
                        put(EBuildingType.GOLDMINE, 0.0f);
                        put(EBuildingType.GEMSMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                    put(EMaterialType.FISH, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.GOLDMINE, 1.0f);
                        put(EBuildingType.GEMSMINE, 1.0f);
                        put(EBuildingType.COALMINE, 0.0f);
                        put(EBuildingType.IRONMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                }
            });
            put(ECivilisation.ASIAN, new HashMap<EMaterialType, HashMap<EBuildingType, Float>>() {
                {
                    put(EMaterialType.COAL, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.IRONMELT, 0.25f);
                        put(EBuildingType.GOLDMELT, 0.25f);
                        put(EBuildingType.WEAPONSMITH, 0.19f);
                        put(EBuildingType.TOOLSMITH, 0.09f);
                        put(EBuildingType.DISTILLERY, 0.16f);
                    }});
                    put(EMaterialType.IRON, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.WEAPONSMITH, 0.33f);
                        put(EBuildingType.TOOLSMITH, 0.33f);
                        put(EBuildingType.DOCKYARD, 0.16f);
                        // TODO: Add Cannon Hall
                    }});
                    put(EMaterialType.PLANK, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.BUILDING_SITE, 0.50f);
                        put(EBuildingType.DOCKYARD, 0.25f);
                        // TODO: Add Cannon Hall
                    }});
                    put(EMaterialType.RICE, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.SULFURMINE, 0.5f);
                        put(EBuildingType.DISTILLERY, 0.5f);
                    }});
                    put(EMaterialType.CROP, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.MILL, 0.4f);
                        put(EBuildingType.PIG_FARM, 0.4f);
                        put(EBuildingType.DONKEY_FARM, 0.2f);
                    }});
                    put(EMaterialType.WATER, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.BAKER, 0.4f);
                        put(EBuildingType.PIG_FARM, 0.4f);
                        put(EBuildingType.DONKEY_FARM, 0.2f);
                    }});
                    put(EMaterialType.BREAD, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.COALMINE, 1.0f);
                        put(EBuildingType.IRONMINE, 0.0f);
                        put(EBuildingType.GOLDMINE, 0.0f);
                        put(EBuildingType.GEMSMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                    put(EMaterialType.MEAT, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.IRONMINE, 1.0f);
                        put(EBuildingType.COALMINE, 0.0f);
                        put(EBuildingType.GOLDMINE, 0.0f);
                        put(EBuildingType.GEMSMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                    put(EMaterialType.FISH, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.GOLDMINE, 1.0f);
                        put(EBuildingType.GEMSMINE, 1.0f);
                        put(EBuildingType.COALMINE, 0.0f);
                        put(EBuildingType.IRONMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                }
            });
            put(ECivilisation.AMAZON, new HashMap<EMaterialType, HashMap<EBuildingType, Float>>() {
                {
                    put(EMaterialType.COAL, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.IRONMELT, 0.32f);
                        put(EBuildingType.GOLDMELT, 0.32f);
                        put(EBuildingType.WEAPONSMITH, 0.24f);
                        put(EBuildingType.TOOLSMITH, 0.12f);
                    }});
                    put(EMaterialType.IRON, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.WEAPONSMITH, 0.33f);
                        put(EBuildingType.TOOLSMITH, 0.33f);
                        put(EBuildingType.DOCKYARD, 0.16f);
                        // TODO: Add Gong Hall
                    }});
                    put(EMaterialType.PLANK, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.BUILDING_SITE, 0.50f);
                        put(EBuildingType.DOCKYARD, 0.25f);
                        // TODO: Add Gong Hall
                    }});
                    put(EMaterialType.HONEY, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.MEAD_BREWERY, 0.5f);
                        put(EBuildingType.SULFURMINE, 0.5f);
                    }});
                    put(EMaterialType.CROP, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.MILL, 0.4f);
                        put(EBuildingType.PIG_FARM, 0.4f);
                        put(EBuildingType.DONKEY_FARM, 0.2f);
                    }});
                    put(EMaterialType.WATER, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.PIG_FARM, 0.33f);
                        put(EBuildingType.DONKEY_FARM, 0.16f);
                        put(EBuildingType.MEAD_BREWERY, 0.18f);
                    }});
                    put(EMaterialType.BREAD, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.COALMINE, 1.0f);
                        put(EBuildingType.IRONMINE, 0.0f);
                        put(EBuildingType.GOLDMINE, 0.0f);
                        put(EBuildingType.GEMSMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                    put(EMaterialType.MEAT, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.IRONMINE, 1.0f);
                        put(EBuildingType.COALMINE, 0.0f);
                        put(EBuildingType.GOLDMINE, 0.0f);
                        put(EBuildingType.GEMSMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                    put(EMaterialType.FISH, new HashMap<EBuildingType, Float>() {{
                        put(EBuildingType.GOLDMINE, 0.67f);
                        put(EBuildingType.GEMSMINE, 0.33f);
                        put(EBuildingType.COALMINE, 0.0f);
                        put(EBuildingType.IRONMINE, 0.0f);
                        put(EBuildingType.SULFURMINE, 0.0f);
                    }});
                }
            });
        }
    };

	private static final MaterialDistributionSettings[][] INITIAL_MATERIAL_DISTRIBUTION_SETTINGS = new MaterialDistributionSettings[ECivilisation.VALUES.length][EMaterialType.NUMBER_OF_MATERIALS];
	private static final boolean[] INITIAL_STOCK_SETTINGS = new boolean[EMaterialType.NUMBER_OF_DROPPABLE_MATERIALS];
	private static final boolean[] INITIAL_STOCK_SETTINGS_EGYPTIAN = new boolean[EMaterialType.NUMBER_OF_DROPPABLE_MATERIALS];

	static {
		for(ECivilisation civilisation : ECivilisation.VALUES) {
            var civilisationMap = DEFAULT_MATERIAL_DISTRIBUTION.containsKey(civilisation) ? DEFAULT_MATERIAL_DISTRIBUTION.get(civilisation) : DEFAULT_MATERIAL_DISTRIBUTION.get(ECivilisation.ROMAN);

			for(EMaterialType materialType : EMaterialType.VALUES) {
                MaterialDistributionSettings distributionSettings = new MaterialDistributionSettings(materialType, civilisation);

                if(civilisationMap.containsKey(materialType)) {
                    var materialMap = civilisationMap.get(materialType);

                    EBuildingType[] requestingBuildingTypes = distributionSettings.getBuildingTypes();

                    for (EBuildingType buildingType : requestingBuildingTypes) {
                        if (materialMap.containsKey(buildingType))
                            distributionSettings.setUserConfiguredDistributionValue(buildingType, materialMap.get(buildingType));
                    }
                }

                INITIAL_MATERIAL_DISTRIBUTION_SETTINGS[civilisation.ordinal][materialType.ordinal] = distributionSettings;
			}
		}

		// GOLD is active by default
		INITIAL_STOCK_SETTINGS[EMaterialType.GOLD.ordinal] = true;
		INITIAL_STOCK_SETTINGS_EGYPTIAN[EMaterialType.GOLD.ordinal] = true;
		// GEMS are active by default but only for egyptians
		INITIAL_STOCK_SETTINGS_EGYPTIAN[EMaterialType.GEMS.ordinal] = true;
	}

	private final EMaterialType[] materialTypeForPriorities;
	private final MaterialDistributionSettings[] settingsOfMaterials;
	private final MaterialProductionSettings materialProductionSettings;
	private final StockSettings stockSettings;
	private final ProfessionSettings professionSettings;

	public PartitionManagerSettings(ECivilisation civilisation) {
		materialTypeForPriorities = new EMaterialType[EMaterialType.NUMBER_OF_DROPPABLE_MATERIALS];
		System.arraycopy(EMaterialType.DROPPABLE_MATERIALS, 0, materialTypeForPriorities, 0, EMaterialType.NUMBER_OF_DROPPABLE_MATERIALS);

		if(civilisation == null) civilisation = ECivilisation.ROMAN;

		settingsOfMaterials = INITIAL_MATERIAL_DISTRIBUTION_SETTINGS[civilisation.ordinal];
		materialProductionSettings = new MaterialProductionSettings();
		stockSettings = new StockSettings(civilisation == ECivilisation.EGYPTIAN ? INITIAL_STOCK_SETTINGS_EGYPTIAN : INITIAL_STOCK_SETTINGS);
		professionSettings = new ProfessionSettings();
	}

	@Override
	public EMaterialType getMaterialTypeForPriority(int priorityIdx) {
		return materialTypeForPriorities[priorityIdx];
	}

	@Override
	public MaterialDistributionSettings getDistributionSettings(final EMaterialType materialType) {
		return settingsOfMaterials[materialType.ordinal];
	}

	/**
	 * Sets the setting for the priorities of the droppable {@link EMaterialType}s.
	 * 
	 * @param materialTypeForPriority
	 *            An array of all droppable {@link EMaterialType}s. The first element has the highest priority, the last one has the lowest.
	 */
	public void setMaterialPriorities(EMaterialType[] materialTypeForPriority) {
		assert this.materialTypeForPriorities.length == materialTypeForPriority.length;

		for (int i = 0; i < materialTypeForPriority.length; i++) {
			this.materialTypeForPriorities[i] = materialTypeForPriority[i];
		}
	}

	@Override
	public MaterialProductionSettings getMaterialProductionSettings() {
		return materialProductionSettings;
	}

	public void setAcceptedStockMaterial(EMaterialType materialType, boolean accepted) {
		stockSettings.setAccepted(materialType, accepted);
	}

	@Override
	public StockSettings getStockSettings() {
		return stockSettings;
	}

	public void setMaterialDistributionSettings(EMaterialType materialType, EBuildingType buildingType, float ratio) {
		getDistributionSettings(materialType).setUserConfiguredDistributionValue(buildingType, ratio);
	}

	@Override
	public ProfessionSettings getProfessionSettings() {
		return professionSettings;
	}
}
