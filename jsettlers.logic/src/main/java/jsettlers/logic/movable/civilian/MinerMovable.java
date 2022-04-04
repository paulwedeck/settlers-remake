package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.MineBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class MinerMovable extends BuildingWorkerMovable {

	private static final long serialVersionUID = -4875647544045930908L;

	public MinerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.MINER, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.MINER, new Root<>(createMinerBehaviour()));
	}
	private static Node<MinerMovable> createMinerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(MinerMovable::getMiningInterval),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(MinerMovable::getOutputMaterial),
							// must be last
							condition(mov -> mov.building.tryTakingFood())
						)
					),
					ignoreFailure(
						selector(
							sequence(
								condition(mov -> mov.building.tryTakingResource()),
								setMaterial(MinerMovable::getOutputMaterial),
								show(),
								goToOutputStack(MinerMovable::getOutputMaterial),
								setDirectionNode(MinerMovable::getDropDirection),
								dropProduced(MinerMovable::getOutputMaterial)
							),
							sequence(
								setMaterialNode(EMaterialType.BASKET),
								show(),
								goToOutputStack(MinerMovable::getOutputMaterial),
								setDirectionNode(MinerMovable::getDropDirection),
								playAction(EMovableAction.ACTION1, (short)3000)
							)
						)
					),
					enterHome()
				)
		);
	}

	private int getMiningInterval() {
		return (int) (building.getBuildingVariant().getMineSettings().getMiningInterval()*1000);
	}

	private EDirection getDropDirection() {
		return building.getBuildingVariant().getMineSettings().getDropDirection();
	}

	private EMaterialType getOutputMaterial() {
		return building.getBuildingVariant().getOfferStacks()[0].getMaterialType();
	}
}
