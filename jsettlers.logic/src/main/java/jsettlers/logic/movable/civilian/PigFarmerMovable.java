package jsettlers.logic.movable.civilian;

import java.util.function.Predicate;

import jsettlers.algorithms.simplebehaviortree.IShortPoint2DSupplier;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class PigFarmerMovable extends BuildingWorkerMovable {

	private static final long serialVersionUID = 1L;

	public PigFarmerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.PIG_FARMER, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.PIG_FARMER, new Root<>(createPigFarmerBehaviour()));
	}

	private ShortPoint2D targetKillablePig;
	private ShortPoint2D targetFreePig;

	private static Node<PigFarmerMovable> createPigFarmerBehaviour() {
		return defaultWorkCycle(
				sequence(
						sleep(3000),
						waitFor(
							sequence(
								isAllowedToWork(),
								selector(
									sequence(
										outputStackNotFull(EMaterialType.PIG),
										condition(PigFarmerMovable::findKillablePig)
									),
									sequence(
										inputStackNotEmpty(EMaterialType.CROP),
										inputStackNotEmpty(EMaterialType.WATER),
										condition(PigFarmerMovable::findFreePigPlace)
									)
								)
							)
						),
						show(),
						setMaterialNode(EMaterialType.NO_MATERIAL),
						selector(
							sequence(
								outputStackNotFull(EMaterialType.PIG),
								condition(mov -> mov.targetKillablePig != null),
								ignoreFailure(
									sequence(
										goToPos(mov -> mov.targetKillablePig),
										setDirectionNode(EDirection.NORTH_EAST),
										take(mov -> EMaterialType.PIG, false),
										setPigAtTarget(mov -> mov.targetKillablePig, false),
										goToOutputStack(EMaterialType.PIG),
										setDirectionNode(EDirection.NORTH_WEST),
										dropProduced(mov -> EMaterialType.PIG)
									)
								)
							),
							sequence(
								inputStackNotEmpty(EMaterialType.CROP),
								inputStackNotEmpty(EMaterialType.WATER),
								condition(mov -> mov.targetFreePig != null),
								ignoreFailure(
									sequence(
										// take crops
										setDirectionNode(EDirection.SOUTH_EAST),
										goToInputStack(EMaterialType.CROP),
										setDirectionNode(EDirection.NORTH_WEST),
										take(mov -> EMaterialType.CROP, true),
										// and go home
										enterHome(),
										sleep(1000),

										// take water
										setMaterialNode(EMaterialType.NO_MATERIAL),
										setDirectionNode(EDirection.SOUTH_EAST),
										show(),
										goToInputStack(EMaterialType.WATER),
										setDirectionNode(EDirection.NORTH_EAST),
										take(mov -> EMaterialType.WATER, true),
										// and go home
										enterHome(),
										sleep(1000),

										// place new pig
										show(),
										setMaterialNode(EMaterialType.BASKET),
										setPigAtTarget(mov -> mov.targetFreePig, true),
										goToPos(mov -> mov.targetFreePig),
										setDirectionNode(EDirection.SOUTH_EAST),
										goToPos(PigFarmerMovable::getPigPlacePosition),
										setDirectionNode(EDirection.NORTH_EAST),
										playAction(EMovableAction.ACTION1, (short)2000)
									)
								)
							),
							alwaysSucceed()
						),
						enterHome()
				)
		);
	}

	private static <T extends PigFarmerMovable> Node<T> setPigAtTarget(IShortPoint2DSupplier<T> position, boolean place) {
		return action(mov -> {
			ShortPoint2D pos = position.apply(mov);
			mov.grid.placePigAt(pos, place);
			mov.building.addMapObjectCleanupPosition(pos, EMapObjectType.PIG);
		});
	}

	private ShortPoint2D getPigPlacePosition() {
		return building.getBuildingVariant().getPigFeedPosition().calculatePoint(building.getPosition());
	}

	private ShortPoint2D searchTargetPig(Predicate<ShortPoint2D> validFunction) {
		for(RelativePoint animalPosition : building.getBuildingVariant().getAnimalPositions()) {
			ShortPoint2D realPosition = animalPosition.calculatePoint(building.getPosition());

			if(!validFunction.test(realPosition)) continue;

			return realPosition;
		}

		return null;
	}

	private boolean findKillablePig() {
		targetKillablePig = searchTargetPig(grid::isPigAdult);

		return targetKillablePig != null;
	}


	private boolean findFreePigPlace() {
		targetFreePig = searchTargetPig(pos -> !grid.hasPigAt(pos));

		return targetFreePig != null;
	}
}
