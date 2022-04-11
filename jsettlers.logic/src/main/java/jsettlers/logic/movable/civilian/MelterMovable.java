package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.buildings.EBuildingType;
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

public class MelterMovable extends BuildingWorkerMovable {

	private static final long serialVersionUID = 1L;

	public MelterMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.MELTER, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.MELTER, new Root<>(createMelterBehaviour()));
	}

	private static Node<MelterMovable> createMelterBehaviour() {
		return busyWorkCycle(MelterMovable::melterWorkPreconditions,
				sequence(
					goToInputStack(MelterMovable::getInputMaterial),
					setDirectionNode(EDirection.NORTH_WEST),
					take(MelterMovable::getInputMaterial, true),

					dropIntoMeltingPot(),

					goToInputStack(EMaterialType.COAL),
					setDirectionNode(EDirection.NORTH_WEST),
					take(mov -> EMaterialType.COAL, true),

					dropIntoMeltingPot(),

					goToPos(MelterMovable::getOutputPosition),
					setDirectionNode(MelterMovable::getOutputDirection),
					setSmoke(MelterMovable::getMeltingTime),
					playAction(EMovableAction.ACTION1, MelterMovable::getMeltingTime),
					setSmoke((short) 0),
					take(MelterMovable::getProducedMaterial, false),

					goToOutputStack(MelterMovable::getProducedMaterial),
					setDirectionNode(EDirection.NORTH_EAST),
					dropProduced(MelterMovable::getProducedMaterial)
				)
		);
	}

	private static Node<MelterMovable> dropIntoMeltingPot() {
		return sequence(
				goToPos(MelterMovable::getInputPoint),
				setDirectionNode(MelterMovable::getInputDirection),

				crouchDown(setMaterialNode(EMaterialType.NO_MATERIAL))
		);
	}

	private static Node<MelterMovable> melterWorkPreconditions() {
		return sequence(
			isAllowedToWork(),
			inputStackNotEmpty(EMaterialType.COAL),
			inputStackNotEmpty(MelterMovable::getInputMaterial),
			outputStackNotFull(MelterMovable::getProducedMaterial)
		);
	}

	private EMaterialType getProducedMaterial() {
		return building.getBuildingVariant().getMeltOutputMaterial();
	}

	private EMaterialType getInputMaterial() {
		return building.getBuildingVariant().getMeltInputMaterial();
	}

	private ShortPoint2D getInputPoint() {
		return building.getBuildingVariant().getMeltInput().calculatePoint(building.getPosition());
	}

	private ShortPoint2D getOutputPosition() {
		return building.getBuildingVariant().getMeltOutput().calculatePoint(building.getPosition());
	}

	private EDirection getOutputDirection() {
		return building.getBuildingVariant().getMeltOutput().getDirection();
	}

	private EDirection getInputDirection() {
		return building.getBuildingVariant().getMeltInput().getDirection();
	}

	private short getMeltingTime() {
		return building.getBuildingVariant().getType() == EBuildingType.IRONMELT ? (short)5000 : (short)3500;
	}
}
