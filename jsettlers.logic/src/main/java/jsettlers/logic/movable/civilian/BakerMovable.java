package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BakerMovable extends BuildingWorkerMovable {

	private static final long serialVersionUID = 323577854037859030L;

	public BakerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.BAKER, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.BAKER, new Root<>(createBakerBehaviour()));
	}

	private static Node<BakerMovable> createBakerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(1000),
					waitFor(
						sequence(
							isAllowedToWork(),
							inputStackNotEmpty(EMaterialType.FLOUR),
							inputStackNotEmpty(EMaterialType.WATER),
							outputStackNotFull(EMaterialType.BREAD)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							setMaterialNode(EMaterialType.NO_MATERIAL),
							goToInputStack(EMaterialType.FLOUR),
							setDirectionNode(EDirection.NORTH_WEST),
							take(mov -> EMaterialType.FLOUR, true),
							enterHome(),

							sleep(1000),

							setMaterialNode(EMaterialType.NO_MATERIAL),
							show(),
							goToInputStack(EMaterialType.WATER),
							setDirectionNode(EDirection.NORTH_EAST),
							take(mov -> EMaterialType.WATER, true),
							enterHome(),

							sleep(3000),

							setMaterialNode(EMaterialType.WHITE_BREAD),
							show(),
							goToPos(BakerMovable::getOvenPosition),
							setDirectionNode(BakerMovable::getOvenDirection),
							playAction(EMovableAction.ACTION1, (short)1000),
							setMaterialNode(EMaterialType.BLADE),
							setSmoke((short) 4000),
							sleep(4000),
							setMaterialNode(EMaterialType.BREAD),
							setSmoke((short) 0),
							playAction(EMovableAction.ACTION1, (short)1000),

							goToOutputStack(EMaterialType.BREAD),
							setDirectionNode(EDirection.NORTH_WEST),
							dropProduced(mov -> EMaterialType.BREAD),
							setMaterialNode(EMaterialType.BLADE)
						)
					),
					enterHome()
				)
		);
	}

	private ShortPoint2D getOvenPosition() {
		return building.getBuildingVariant().getOvenPosition().calculatePoint(building.getPosition());
	}

	private EDirection getOvenDirection() {
		return building.getBuildingVariant().getOvenPosition().getDirection();
	}
}
