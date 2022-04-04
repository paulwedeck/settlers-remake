package jsettlers.ai.army;

import jsettlers.ai.highlevel.AiPositions;
import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.action.SetMaterialProductionAction;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESoldierType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.input.tasks.MoveToGuiTask;
import jsettlers.input.tasks.SetMaterialProductionGuiTask;
import jsettlers.input.tasks.UpgradeSoldiersGuiTask;
import jsettlers.logic.map.grid.movable.MovableGrid;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.player.Player;
import jsettlers.network.client.interfaces.ITaskScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Stream;

public class ArmyFramework {
	final AiStatistics aiStatistics;
	private final Player player;
	final MovableGrid movableGrid;
	final ITaskScheduler taskScheduler;

	protected final List<ArmyModule> modules = new ArrayList<>();

	ArmyFramework(AiStatistics aiStatistics, Player player, MovableGrid movableGrid, ITaskScheduler taskScheduler) {
		this.aiStatistics = aiStatistics;
		this.player = player;
		this.movableGrid = movableGrid;
		this.taskScheduler = taskScheduler;
	}

	void addModule(ArmyModule module) {
		modules.add(module);
	}

	public <T extends ArmyModule> Stream<T> findModules(Class<T> modClazz) {
		return modules.stream().filter(modClazz::isInstance).map(modClazz::cast);
	}

	boolean existsAliveEnemy() {
		return !aiStatistics.getAliveEnemiesOf(player).isEmpty();
	}


	void setNumberOfFutureProducedMaterial(byte playerId, EMaterialType materialType, int numberToProduce) {
		if (aiStatistics.getMaterialProduction(playerId).getAbsoluteProductionRequest(materialType) != numberToProduce) {
			taskScheduler.scheduleTask(new SetMaterialProductionGuiTask(playerId, aiStatistics.getPositionOfPartition(playerId), materialType,
					SetMaterialProductionAction.EMaterialProductionType.SET_PRODUCTION, numberToProduce));
		}
	}

	void setRatioOfMaterial(byte playerId, EMaterialType materialType, float ratio) {
		if (aiStatistics.getMaterialProduction(playerId).getUserConfiguredRelativeRequestValue(materialType) != ratio) {
			taskScheduler.scheduleTask(new SetMaterialProductionGuiTask(playerId, aiStatistics.getPositionOfPartition(playerId), materialType,
					SetMaterialProductionAction.EMaterialProductionType.SET_RATIO, ratio));
		}
	}


	void sendTroopsTo(List<ShortPoint2D> attackerPositions, ShortPoint2D target, Set<Integer> soldiersWithOrders, EMoveToType moveToType) {
		List<Integer> attackerIds = new Vector<>(attackerPositions.size());
		for (ShortPoint2D attackerPosition : attackerPositions) {
			ILogicMovable movable = movableGrid.getMovableAt(attackerPosition.x, attackerPosition.y);
			if(movable == null) {
				System.err.printf("AI ERROR: Attacker at %d:%d does not exist!\n", attackerPosition.x, attackerPosition.y);
				continue;
			}
			attackerIds.add(movable.getID());
		}

		sendTroopsToById(attackerIds, target, soldiersWithOrders, moveToType);
	}

	void sendTroopsToById(Collection<Integer> attackerIds, ShortPoint2D target, Set<Integer> soldiersWithOrders, EMoveToType moveToType) {
		List<Integer> ids = new ArrayList<>(attackerIds);
		if(soldiersWithOrders != null) {
			ids.removeAll(soldiersWithOrders);
			soldiersWithOrders.addAll(attackerIds);
		}

		taskScheduler.scheduleTask(new MoveToGuiTask(player.playerId, target, ids, moveToType));
	}


	IPlayer getWeakestEnemy() {
		IPlayer weakestEnemyPlayer = null;
		int minAmountOfEnemyId = Integer.MAX_VALUE;

		for (IPlayer enemyPlayer : aiStatistics.getAliveEnemiesOf(player)) {
			int amountOfEnemyTroops = aiStatistics.getCountOfMovablesOfPlayer(enemyPlayer, EMovableType.SOLDIERS);
			if (amountOfEnemyTroops < minAmountOfEnemyId) {
				minAmountOfEnemyId = amountOfEnemyTroops;
				weakestEnemyPlayer = enemyPlayer;
			}
		}

		return weakestEnemyPlayer;
	}

	boolean canUpgradeSoldiers(ESoldierType type) {
		return player.getMannaInformation().isUpgradePossible(type);
	}

	void upgradeSoldiers(ESoldierType type) {
		assert canUpgradeSoldiers(type);

		taskScheduler.scheduleTask(new UpgradeSoldiersGuiTask(player.playerId, type));
	}

	public Player getPlayer() {
		return player;
	}

	public byte getPlayerId() {
		return player.getPlayerId();
	}

	AiPositions getEnemiesInTown() {
		return aiStatistics.getEnemiesInTownOf(player.getPlayerId());
	}
}
