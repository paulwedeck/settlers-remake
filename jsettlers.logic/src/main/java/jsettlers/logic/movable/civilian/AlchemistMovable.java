package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class AlchemistMovable extends BuildingWorkerMovable {

	private static final int MAX_MATERIALS = 5;
	private static final float NO_MATERIAL_CHANCE = 3/5.f;
	private static final long serialVersionUID = 8460201983939801550L;

	private final List<EMaterialType> materials = new LinkedList<>();

	public AlchemistMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.ALCHEMIST, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.ALCHEMIST, new Root<>(createAlchemistBehaviour()));
	}

	private static Node<AlchemistMovable> createAlchemistBehaviour() {
		return defaultWorkCycle(
				sequence(
						sleep(3000),
						waitFor(
								sequence(
										isAllowedToWork(),
										inputStackNotEmpty(EMaterialType.GEMS),
										inputStackNotEmpty(EMaterialType.SULFUR)
								)
						),
						show(),
						ignoreFailure(
								sequence(
										setMaterialNode(EMaterialType.NO_MATERIAL),
										dropIntoOven(EMaterialType.SULFUR, EDirection.SOUTH_WEST),
										dropIntoOven(EMaterialType.GEMS, EDirection.SOUTH_WEST),
										enterHome(),
										sleep(3000),
										show(),
										setMaterialNode(EMaterialType.CHEMICALS),
										goToPos(AlchemistMovable::getOvenPosition),
										setDirectionNode(AlchemistMovable::getOvenDirection),
										playAction(EMovableAction.ACTION1, (short)1000),

										action(AlchemistMovable::generateRandomMaterials),
										setMaterialNode(EMaterialType.NO_MATERIAL),
										crouchDown(setMaterial(AlchemistMovable::getAverageMaterial)),
										repeat(mov -> !mov.materials.isEmpty(),
											sequence(
												selector(
													outputStackNotFull(AlchemistMovable::getOutputMaterial),
													sequence(
														enterHome(),
														waitFor(outputStackNotFull(AlchemistMovable::getOutputMaterial)),
														show()
													)
												),
												goToOutputStack(AlchemistMovable::getOutputMaterial),
												crouchDown(
													sequence(
														action(AlchemistMovable::dropOutputMaterial),
														setMaterial(AlchemistMovable::getAverageMaterial)
													)
												)
											)
										)
								)
						),
						enterHome()
				)
		);
	}

	private EMaterialType[] getPossibleMaterials() {
		return Arrays.stream(building.getBuildingVariant().getOfferStacks())
				.map(RelativeStack::getMaterialType)
				.toArray(EMaterialType[]::new);
	}

	private EMaterialType getOutputMaterial() {
		if(materials.isEmpty()) return EMaterialType.NO_MATERIAL;

		return materials.get(materials.size() - 1);
	}

	private void dropOutputMaterial() {
		EMaterialType mat = getOutputMaterial();

		while(getOutputMaterial() == mat) {
			if(!grid.dropMaterial(position, mat, true, false)) return;
			produce(mat);
			materials.remove(materials.size() - 1);
		}
	}

	private void generateRandomMaterials() {
		EMaterialType[] possibleMaterials = getPossibleMaterials();

		for(int i = 0; i < MAX_MATERIALS; i++) {
			if(MatchConstants.random().nextFloat() > NO_MATERIAL_CHANCE) {
				materials.add(possibleMaterials[MatchConstants.random().nextInt(possibleMaterials.length)]);
			}
		}

		materials.sort(Comparator.comparing(EMaterialType::ordinal));
	}

	private EMaterialType getAverageMaterial() {
		if(materials.contains(EMaterialType.IRON) && materials.contains(EMaterialType.GOLD)) {
			return EMaterialType.METALS;
		}

		return getOutputMaterial();
	}


	private ShortPoint2D getOvenPosition() {
		return building.getBuildingVariant().getOvenPosition().calculatePoint(building.getPosition());
	}

	private EDirection getOvenDirection() {
		return building.getBuildingVariant().getOvenPosition().getDirection();
	}
}
