package jsettlers.ai.army;

import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IPlayer;
import jsettlers.shared.ShortPoint2D;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class SimpleStrategyModule extends ArmyModule {

	private static final byte MIN_ATTACKER_COUNT = 20;
	private static final float MAX_WOUNDED_RATIO_FOR_ATTACK = 0.5f;
	private static final EBuildingType[] MIN_BUILDING_REQUIREMENTS_FOR_ATTACK = { EBuildingType.COALMINE, EBuildingType.IRONMINE, EBuildingType.IRONMELT, EBuildingType.WEAPONSMITH,
			EBuildingType.BARRACK };
	private final float attackerCountFactor;

	private static final float[] ATTACKER_COUNT_FACTOR_BY_PLAYER_TYPE = { 1.1F, 1F, 0.9F, 0.8F, 0F };

	public SimpleStrategyModule(ArmyFramework parent) {
		super(parent);

		this.attackerCountFactor = ATTACKER_COUNT_FACTOR_BY_PLAYER_TYPE[parent.getPlayer().getPlayerType().ordinal()];
	}

	@Override
	public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
		SoldierPositions soldierPositions = calculateSituation(parent.getPlayerId());
		if (parent.getEnemiesInTown().size() > 0) {
			defend(soldierPositions, soldiersWithOrders);
		} else if (parent.existsAliveEnemy()) {
			IPlayer weakestEnemy = parent.getWeakestEnemy();
			SoldierPositions enemySoldierPositions = calculateSituation(weakestEnemy.getPlayerId());
			boolean infantryWouldDie = wouldInfantryDie(enemySoldierPositions);
			int woundedSoldiersCount = parent.findModules(HealSoldiersModule.class).findAny().map(HealSoldiersModule::getWoundedSoldiersCount).orElse(0);
			if (woundedSoldiersCount/(float)soldierPositions.getSoldiersCount() <= MAX_WOUNDED_RATIO_FOR_ATTACK &&
					attackIsPossible(soldierPositions, enemySoldierPositions, infantryWouldDie)) {
				attack(soldierPositions, infantryWouldDie, soldiersWithOrders);
			}
		}
	}


	private boolean wouldInfantryDie(SoldierPositions enemySoldierPositions) {
		return enemySoldierPositions.bowmenPositions.size() > SoldierProductionModule.BOWMEN_COUNT_OF_KILLING_INFANTRY;
	}


	private boolean attackIsPossible(SoldierPositions soldierPositions, SoldierPositions enemySoldierPositions, boolean infantryWouldDie) {
		for (EBuildingType requiredType : MIN_BUILDING_REQUIREMENTS_FOR_ATTACK) {
			if (parent.aiStatistics.getNumberOfBuildingTypeForPlayer(requiredType, parent.getPlayerId()) < 1) {
				return false;
			}
		}

		float combatStrength = parent.getPlayer().getCombatStrengthInformation().getCombatStrength(false);
		float effectiveAttackerCount;
		if (infantryWouldDie) {
			effectiveAttackerCount = soldierPositions.bowmenPositions.size() * combatStrength;
		} else {
			effectiveAttackerCount = soldierPositions.getSoldiersCount() * combatStrength;
		}
		return effectiveAttackerCount >= MIN_ATTACKER_COUNT && effectiveAttackerCount * attackerCountFactor > enemySoldierPositions.getSoldiersCount();

	}

	private void defend(SoldierPositions soldierPositions, Set<Integer> soldiersWithOrders) {
		List<ShortPoint2D> allMyTroops = new Vector<>();
		allMyTroops.addAll(soldierPositions.bowmenPositions);
		allMyTroops.addAll(soldierPositions.pikemenPositions);
		allMyTroops.addAll(soldierPositions.swordsmenPositions);
		parent.sendTroopsTo(allMyTroops, parent.getEnemiesInTown().iterator().next(), soldiersWithOrders, EMoveToType.DEFAULT);
	}

	private void attack(SoldierPositions soldierPositions, boolean infantryWouldDie, Set<Integer> soldiersWithOrders) {
		IPlayer weakestEnemy = parent.getWeakestEnemy();
		ShortPoint2D targetDoor = getTargetEnemyDoorToAttack(weakestEnemy);
		if(targetDoor == null) return;

		if (infantryWouldDie) {
			parent.sendTroopsTo(soldierPositions.bowmenPositions, targetDoor, soldiersWithOrders, EMoveToType.DEFAULT);
		} else {
			List<ShortPoint2D> soldiers = new ArrayList<>(soldierPositions.bowmenPositions.size() + soldierPositions.pikemenPositions.size() + soldierPositions.swordsmenPositions.size());
			soldiers.addAll(soldierPositions.bowmenPositions);
			soldiers.addAll(soldierPositions.pikemenPositions);
			soldiers.addAll(soldierPositions.swordsmenPositions);
			parent.sendTroopsTo(soldiers, targetDoor, soldiersWithOrders, EMoveToType.DEFAULT);
		}
	}

	private ShortPoint2D getTargetEnemyDoorToAttack(IPlayer enemyToAttack) {
		List<ShortPoint2D> myMilitaryBuildings = parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, parent.getPlayerId());
		ShortPoint2D myBaseAveragePoint = AiStatistics.calculateAveragePointFromList(myMilitaryBuildings);
		List<ShortPoint2D> enemyMilitaryBuildings = parent.aiStatistics.getBuildingPositionsOfTypesForPlayer(EBuildingType.MILITARY_BUILDINGS, enemyToAttack.getPlayerId());
		// ignore unfinished buildings
		enemyMilitaryBuildings.removeIf(shortPoint2D -> !parent.aiStatistics.getBuildingAt(shortPoint2D).isConstructionFinished());

		ShortPoint2D nearestEnemyBuildingPosition = AiStatistics.detectNearestPointFromList(myBaseAveragePoint, enemyMilitaryBuildings);
		if(nearestEnemyBuildingPosition == null) return null;

		return parent.aiStatistics.getBuildingAt(nearestEnemyBuildingPosition).getDoor();
	}

	private void calculateSituation(List<ShortPoint2D> soldierPositions, byte playerId, Set<EMovableType> soldierTypes) {
		for(EMovableType soldierType : soldierTypes) {
			soldierPositions.addAll(parent.aiStatistics.getPositionsOfMovablesWithTypeForPlayer(playerId, soldierType));
		}
	}

	private SoldierPositions calculateSituation(byte playerId) {
		SoldierPositions soldierPositions = new SoldierPositions();
		calculateSituation(soldierPositions.swordsmenPositions, playerId, EMovableType.SWORDSMEN);
		calculateSituation(soldierPositions.bowmenPositions, playerId, EMovableType.BOWMEN);
		calculateSituation(soldierPositions.pikemenPositions, playerId, EMovableType.PIKEMEN);
		return soldierPositions;
	}

	private static class SoldierPositions {
		private final List<ShortPoint2D> swordsmenPositions = new Vector<>();
		private final List<ShortPoint2D> bowmenPositions = new Vector<>();
		private final List<ShortPoint2D> pikemenPositions = new Vector<>();

		int getSoldiersCount() {
			return swordsmenPositions.size() + bowmenPositions.size() + pikemenPositions.size();
		}
	}

	@Override
	public void applyLightRules(Set<Integer> soldiersWithOrders) {

	}
}
