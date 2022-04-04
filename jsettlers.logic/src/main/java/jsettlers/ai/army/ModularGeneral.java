package jsettlers.ai.army;

import jsettlers.ai.highlevel.AiStatistics;
import jsettlers.logic.map.grid.movable.MovableGrid;
import jsettlers.logic.player.Player;
import jsettlers.network.client.interfaces.ITaskScheduler;

import java.util.Set;
import java.util.function.Consumer;

public class ModularGeneral extends ArmyFramework implements ArmyGeneral {

	public ModularGeneral(AiStatistics aiStatistics, Player player, MovableGrid movableGrid, ITaskScheduler taskScheduler) {
		super(aiStatistics, player, movableGrid, taskScheduler);
	}

	@Override
	public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
		for (ArmyModule mod : modules) {
			mod.applyHeavyRules(soldiersWithOrders);
		}
	}

	@Override
	public void applyLightRules(Set<Integer> soldiersWithOrders) {
		for (ArmyModule mod : modules) {
			mod.applyLightRules(soldiersWithOrders);
		}
	}

	public static ArmyGeneral createDefaultGeneral(AiStatistics aiStatistics, Player player, MovableGrid movableGrid, ITaskScheduler taskScheduler) {
		return createGeneral(aiStatistics, player, movableGrid, taskScheduler,
			MountTowerModule::new,
			SoldierProductionModule::new,
			UpgradeSoldiersModule::new,
			HealSoldiersModule::new,
			SimpleDefenseStrategy::new,
			SimpleAttackStrategy::new,
			RegroupArmyModule::new);
	}

	@SafeVarargs
	public static ArmyGeneral createGeneral(AiStatistics aiStatistics, Player player, MovableGrid movableGrid, ITaskScheduler taskScheduler, Consumer<ArmyFramework>... modules) {
		ModularGeneral general = new ModularGeneral(aiStatistics, player, movableGrid, taskScheduler);
		for (Consumer<ArmyFramework> module : modules) {
			module.accept(general);
		}
		return general;
	}
}
