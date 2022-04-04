package jsettlers.ai.highlevel;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IMaterialProductionSettings;
import jsettlers.common.map.partition.IPartitionData;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

class PlayerStatistic {
	ShortPoint2D referencePosition;
	final int[] totalBuildingsNumbers = new int[EBuildingType.NUMBER_OF_BUILDINGS];
	final int[] buildingsNumbers = new int[EBuildingType.NUMBER_OF_BUILDINGS];
	final Map<EBuildingType, List<ShortPoint2D>> buildingPositions = new HashMap<>();
	final Map<EBuildingType, List<ShortPoint2D>> buildingWorkAreas = new HashMap<>();
	final Set<ShortPoint2D> activeHospitals = new HashSet<>();
	short partitionIdToBuildOn;
	IPartitionData materials;
	final AiPositions landToBuildOn = new AiPositions();
	final AiPositions borderIngestibleByPioneers = new AiPositions();
	final AiPositions otherPartitionBorder = new AiPositions();
	final Map<EMovableType, List<ShortPoint2D>> movablePositions = new HashMap<>();
	final List<ShortPoint2D> joblessBearerPositions = new ArrayList<>();
	final AiPositions stones = new AiPositions();
	final AiPositions stonesNearBy = new AiPositions();
	final AiPositions trees = new AiPositions();
	final AiPositions rivers = new AiPositions();
	final AiPositions enemyTroopsInTown = new AiPositions();
	List<ShortPoint2D> threatenedBorder;
	int numberOfNotFinishedBuildings;
	int numberOfTotalBuildings;
	int numberOfNotOccupiedMilitaryBuildings;
	int wineCount;
	IMaterialProductionSettings materialProduction;

	final AiPartitionResources partitionResources = new AiPartitionResources();

	PlayerStatistic() {
		clearIntegers();
	}

	public void clearAll() {
		materials = null;
		buildingPositions.clear();
		buildingWorkAreas.clear();
		enemyTroopsInTown.clear();
		stones.clear();
		stonesNearBy.clear();
		trees.clear();
		rivers.clear();
		landToBuildOn.clear();
		borderIngestibleByPioneers.clear();
		otherPartitionBorder.clear();
		movablePositions.clear();
		joblessBearerPositions.clear();
		activeHospitals.clear();
		threatenedBorder = null;
		partitionResources.clear();
		clearIntegers();
	}

	private void clearIntegers() {
		Arrays.fill(totalBuildingsNumbers, 0);
		Arrays.fill(buildingsNumbers, 0);
		numberOfNotFinishedBuildings = 0;
		numberOfTotalBuildings = 0;
		numberOfNotOccupiedMilitaryBuildings = 0;
		wineCount = 0;
		partitionIdToBuildOn = Short.MIN_VALUE;
	}
}
