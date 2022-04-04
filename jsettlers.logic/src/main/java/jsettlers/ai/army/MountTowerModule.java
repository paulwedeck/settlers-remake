package jsettlers.ai.army;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.input.tasks.ChangeTowerSoldiersGuiTask;
import jsettlers.logic.buildings.military.occupying.OccupyingBuilding;
import jsettlers.logic.movable.interfaces.ILogicMovable;

import java.util.List;
import java.util.Set;

import static jsettlers.logic.constants.Constants.TOWER_SEARCH_SOLDIERS_RADIUS;

public class MountTowerModule extends ArmyModule {

	public MountTowerModule(ArmyFramework parent) {
		super(parent);
	}

	private void ensureAllTowersFullyMounted() {
		parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, parent.getPlayerId()).stream()
				.map(parent.aiStatistics::getBuildingAt)
				.filter(building -> building instanceof OccupyingBuilding)
				.map(building -> (OccupyingBuilding) building)
				.filter(building -> !building.isSetToBeFullyOccupied())
				.forEach(building -> parent.taskScheduler.scheduleTask(new ChangeTowerSoldiersGuiTask(parent.getPlayerId(), building.getPosition(), ChangeTowerSoldiersGuiTask.EChangeTowerSoldierTaskType.FULL, null)));
	}

	private void occupyMilitaryBuildings(Set<Integer> soldiersWithOrders) {
		for (ShortPoint2D militaryBuildingPosition : parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, parent.getPlayerId())) {
			OccupyingBuilding militaryBuilding = (OccupyingBuilding) parent.aiStatistics.getBuildingAt(militaryBuildingPosition);
			if (!militaryBuilding.isOccupied()) {
				ShortPoint2D door = militaryBuilding.getDoor();
				ILogicMovable soldier = parent.aiStatistics.getNearestSwordsmanOf(door, parent.getPlayerId());
				if (soldier != null && militaryBuilding.getPosition().getOnGridDistTo(soldier.getPosition()) > TOWER_SEARCH_SOLDIERS_RADIUS) {
					parent.sendTroopsToById(List.of(soldier.getID()), door, soldiersWithOrders, EMoveToType.FORCED);
				}
			}
		}
	}

	@Override
	public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
		ensureAllTowersFullyMounted();
		occupyMilitaryBuildings(soldiersWithOrders);
	}

	@Override
	public void applyLightRules(Set<Integer> soldiersWithOrders) {

	}
}
