package jsettlers.ai.army;

import jsettlers.common.CommonConstants;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.movable.military.SoldierMovable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RegroupArmyModule extends ArmyModule {

	private static final float SOLDIER_THREAT_DISTANCE = CommonConstants.TOWER_RADIUS * 4;
	private static final float MAX_THREAT_OVER_COMMIT_FACTOR = 3;
	private static final float SOLDIER_FORCE_MOVE_DISTANCE = CommonConstants.TOWER_RADIUS;
	private static final float SOLDIER_MIN_MOVE_DISTANCE = CommonConstants.TOWER_RADIUS* 0.5F;
	private static final float MIN_THREAT_LEVEL = 1f;

	private final GroupMap<ShortPoint2D, Integer> groups = new GroupMap<>();
	private final Comparator<Map.Entry<ShortPoint2D, Float>> POI_COMPARATOR;

	public RegroupArmyModule(ArmyFramework parent) {
		super(parent);

		POI_COMPARATOR = Map.Entry.<ShortPoint2D, Float>comparingByValue().reversed();
	}

	@Override
	public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
		updateGroups(soldiersWithOrders);

		// send soldiers
		sendSoldiers(soldiersWithOrders);
	}

	@Override
	public void applyLightRules(Set<Integer> soldiersWithOrders) {
	}

	private void updateGroups(Set<Integer> soldiersWithOrders) {
		Map<ShortPoint2D, Float> pois = calculateThreatLevels(calculatePointsOfInterest());

		removeUnnecessaryGroups(pois.keySet());
		removeDeadSoldiers();

		// ignore otherwise assigned soldiers
		soldiersWithOrders.forEach(s -> groups.setMember(s, null));

		List<ShortPoint2D> unassignedSoldiers = getAvailableSoldiers(soldiersWithOrders);
		updateIdleGroups(pois, unassignedSoldiers);
	}

	private void removeDeadSoldiers() {
		groups.removeMemberIf(id -> MovableManager.getMovableByID(id) == null);
	}

	private void updateIdleGroups(Map<ShortPoint2D, Float> pois, List<ShortPoint2D> unassignedSoldiers) {
		List<Map.Entry<ShortPoint2D, Float>> sortedPOIs = pois.entrySet().stream().sorted(POI_COMPARATOR).collect(Collectors.toList());

		for (Map.Entry<ShortPoint2D, Float> poiData : sortedPOIs) {
			ShortPoint2D poi = poiData.getKey();
			float targetSize = poiData.getValue() * MAX_THREAT_OVER_COMMIT_FACTOR;

			updateGroupSize(targetSize, poi, unassignedSoldiers);
		}
	}

	private void updateGroupSize(float targetNumber, ShortPoint2D poi, List<ShortPoint2D> unassignedSoldiers) {
		Set<Integer> group = groups.getMembers(poi);

		float diffSize = group.size() - targetNumber;

		if(diffSize > 0) {
			Iterator<Integer> removeIter = group.iterator();
			removeFromGroup(diffSize, removeIter, unassignedSoldiers);
		} else {
			unassignedSoldiers.sort(Comparator.comparing(poi::getOnGridDistTo));
			addToGroup(-diffSize, poi, unassignedSoldiers);
		}
	}

	private void addToGroup(float amount, ShortPoint2D group, List<ShortPoint2D> unassignedSoldiers) {
		for (int i = 0; i < amount && !unassignedSoldiers.isEmpty(); i++) {
			ShortPoint2D newSoldier = unassignedSoldiers.remove(unassignedSoldiers.size() - 1);
			groups.setMember(getID(newSoldier), group);
		}
	}

	private void removeFromGroup(float limit, Iterator<Integer> groupMembers, List<ShortPoint2D> unassignedSoldiers) {
		for(int i = 0; i < limit && groupMembers.hasNext(); i++) {
			int soldier = groupMembers.next();
			groups.setMember(soldier, null);
			assert MovableManager.getMovableByID(soldier).isAlive();
			ShortPoint2D pos = MovableManager.getMovableByID(soldier).getPosition();
			// the movable might not actually be at its position for different reasons
			if(parent.movableGrid.getMovableAt(pos.x, pos.y) != null) {
				unassignedSoldiers.add(pos);
			}
		}
	}

	private List<ShortPoint2D> getAvailableSoldiers(Set<Integer> soldiersWithOrders) {
		List<ShortPoint2D> allSoldiers = parent.aiStatistics.getPositionsOfMovablesWithTypesForPlayer(parent.getPlayerId(), EMovableType.SOLDIERS);
		Stream.of(soldiersWithOrders.stream(), groups.listMembers().stream())
				.flatMap(id -> id)
				.map(MovableManager::getMovableByID)
				.map(ILogicMovable::getPosition)
				.forEach(allSoldiers::remove);

		return allSoldiers;
	}

	private void sendSoldiers(Set<Integer> soldiersWithOrders) {
		for(Map.Entry<ShortPoint2D, Set<Integer>> poiData : groups.listGroups().entrySet()) {
			ShortPoint2D poi = poiData.getKey();
			Set<Integer> soldiers = poiData.getValue();

			List<Integer> forceMove = new ArrayList<>();
			List<Integer> defaultMove = new ArrayList<>();
			for (Integer soldier : soldiers) {
				ILogicMovable mov = MovableManager.getMovableByID(soldier);
				float distance = mov.getPosition().getOnGridDistTo(poi);
				if(distance <= SOLDIER_MIN_MOVE_DISTANCE) {
					continue;
				}

				if(distance >= SOLDIER_FORCE_MOVE_DISTANCE) {
					forceMove.add(mov.getID());
				} else {
					defaultMove.add(mov.getID());
				}
			}

			parent.sendTroopsToById(forceMove, poi, soldiersWithOrders, EMoveToType.FORCED);
			parent.sendTroopsToById(defaultMove, poi, soldiersWithOrders, EMoveToType.DEFAULT);
		}
	}

	private int getID(ShortPoint2D soldier) {
		return parent.movableGrid.getMovableAt(soldier.x, soldier.y).getID();
	}

	private void removeUnnecessaryGroups(Set<ShortPoint2D> newGroups) {
		Set<ShortPoint2D> removeGroups = new HashSet<>(groups.listGroups().keySet());
		removeGroups.removeAll(newGroups);
		removeGroups.forEach(groups::removeGroup);
	}

	private List<ShortPoint2D> calculatePointsOfInterest() {
		List<ShortPoint2D> pois = new ArrayList<>(parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, parent.getPlayerId()));
		for(ShortPoint2D building : parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EnumSet.allOf(EBuildingType.class), parent.getPlayerId())) {
			if(pois.stream().mapToInt(poi -> poi.getOnGridDistTo(building)).min().orElse(0) >= CommonConstants.TOWER_RADIUS) {
				pois.add(building);
			}
		}

		for(IPlayer enemy : parent.aiStatistics.getAliveEnemiesOf(parent.getPlayer())) {
			Iterator<ShortPoint2D> poi = pois.iterator();
			while(poi.hasNext()) {
				ShortPoint2D next = poi.next();
				for(ShortPoint2D enemyBuilding : parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, enemy.getPlayerId())) {
					if(next.getOnGridDistTo(enemyBuilding) <= CommonConstants.TOWER_RADIUS*2) {
						poi.remove();
						break;
					}
				}
			}
		}
		return pois;
	}

	private Map<ShortPoint2D, Float> calculateThreatLevels(List<ShortPoint2D> pois) {
		Map<ShortPoint2D, Float> poiThreatLevels = new HashMap<>();
		for(ShortPoint2D poi : pois) {
			poiThreatLevels.put(poi, MIN_THREAT_LEVEL);
		}
		for(IPlayer enemy : parent.aiStatistics.getAliveEnemiesOf(parent.getPlayer())) {
			for (EMovableType soldierType : EMovableType.SOLDIERS) {
				for(ShortPoint2D soldier : parent.aiStatistics.getPositionsOfMovablesWithTypeForPlayer(enemy.getPlayerId(), soldierType)) {
					for(ShortPoint2D poi : pois) {
						float dst = poi.getOnGridDistTo(soldier)/ SOLDIER_THREAT_DISTANCE;
						if(dst < 1) {
							float threat = 1 - dst;
							poiThreatLevels.compute(poi, (key, val) -> val + threat);
						}
					}
				}
			}
		}

		return poiThreatLevels;
	}
}
