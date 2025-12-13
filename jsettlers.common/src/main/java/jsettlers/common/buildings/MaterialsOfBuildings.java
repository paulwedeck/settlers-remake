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
package jsettlers.common.buildings;

import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.player.ECivilisation;

/**
 * This class calculates static data gained from configuration files. The class supplies the information what buildings can request a given
 * {@link EMaterialType}.
 * 
 * @author Andreas Eberle
 * 
 */
public final class MaterialsOfBuildings {
	private static final Map<ECivilisation, Map<EMaterialType, EBuildingType[]>> buildingsRequestingMaterial = new EnumMap<>(ECivilisation.class);

	static {
		for(ECivilisation civilisation : ECivilisation.VALUES) {
			Map<EMaterialType, List<EBuildingType>> buildingsForMaterials = new EnumMap<>(EMaterialType.class);

			for(EMaterialType material : EMaterialType.VALUES) {
				buildingsForMaterials.put(material, new LinkedList<>());
			}

			for (EBuildingType buildingType : EBuildingType.VALUES) {
				BuildingVariant building = buildingType.getVariant(civilisation);
				if(building == null) continue;

				for (RelativeStack requestStack : building.getRequestStacks()) {
					buildingsForMaterials.get(requestStack.getMaterialType()).add(buildingType);
				}
			}

			Map<EMaterialType, EBuildingType[]> asArray = new EnumMap<>(EMaterialType.class);

			for(EMaterialType material : EMaterialType.VALUES) {
                List<EBuildingType> list = buildingsForMaterials.get(material);

                // If material is plank or stone, add building site
                if (material == EMaterialType.PLANK || material == EMaterialType.STONE) {
                    list.add(EBuildingType.BUILDING_SITE);
                }

                asArray.put(material, list.toArray(new EBuildingType[0]));
			}

			buildingsRequestingMaterial.put(civilisation, asArray);
		}
	}

	/**
	 * Gets an array of {@link EBuildingType}s that can request the given {@link EMaterialType}.
	 * <p />
	 * NOTE: The array MUST NOT be changed! For the sake of speed, no copy is created by this method!
	 * 
	 * @param material
	 *            {@link EMaterialType} to be checked.
	 * @return Returns an array of {@link EBuildingType}s that can request the given {@link EMaterialType}.
	 */
	public static EBuildingType[] getBuildingTypesRequestingMaterial(EMaterialType material, ECivilisation civilisation) {
		return buildingsRequestingMaterial.get(civilisation).get(material);
	}

	private MaterialsOfBuildings() {
	}
}
