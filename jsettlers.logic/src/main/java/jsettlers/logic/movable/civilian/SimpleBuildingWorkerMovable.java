package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.workers.DockyardBuilding;
import jsettlers.logic.buildings.workers.MillBuilding;
import jsettlers.logic.buildings.workers.SlaughterhouseBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class SimpleBuildingWorkerMovable extends BuildingWorkerMovable {

	private static final long serialVersionUID = 6311951868814253433L;

	public SimpleBuildingWorkerMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.FORESTER, new Root<>(createForesterBehaviour()));
		MovableManager.registerBehaviour(EMovableType.LUMBERJACK, new Root<>(createLumberjackBehaviour()));
		MovableManager.registerBehaviour(EMovableType.WATERWORKER, new Root<>(createWaterworkerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.FISHERMAN, new Root<>(createFishermanBehaviour()));
		MovableManager.registerBehaviour(EMovableType.STONECUTTER, new Root<>(createStonecutterBehaviour()));
		MovableManager.registerBehaviour(EMovableType.WINEGROWER, new Root<>(createWinegrowerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.FARMER, new Root<>(createFarmerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.DOCKWORKER, new Root<>(createDockworkerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.MILLER, new Root<>(createMillerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.SLAUGHTERER, new Root<>(createSlaughtererBehaviour()));
		MovableManager.registerBehaviour(EMovableType.CHARCOAL_BURNER, new Root<>(createCharcoalBurnerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.BREWER, new Root<>(createBrewerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.RICE_FARMER, new Root<>(createRiceFarmerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.BEEKEEPER, new Root<>(createBeekeeperBehaviour()));
		MovableManager.registerBehaviour(EMovableType.DISTILLER, new Root<>(createDistillerBehaviour()));
		MovableManager.registerBehaviour(EMovableType.MEAD_BREWER, new Root<>(createMeadBrewerBehaviour()));
	}

	private static Node<SimpleBuildingWorkerMovable> createForesterBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(4000),
					setMaterialNode(EMaterialType.TREE),
					waitFor(
						sequence(
							isAllowedToWork(),
							preSearchPath(false, ESearchType.PLANTABLE_TREE)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							followPresearchedPath(),
							setDirectionNode(mov -> EDirection.NORTH_WEST),
							playAction(EMovableAction.ACTION1, (short)3000),
							setMaterialNode(EMaterialType.NO_MATERIAL),
							executeSearch(ESearchType.PLANTABLE_TREE)
						)
					),
					enterHome()
				)
		);
	}

	private static final short LUMBERJACK_ACTION1_DURATION = (short)1000;

	private static Node<SimpleBuildingWorkerMovable> createLumberjackBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(EMaterialType.TRUNK),
							preSearchPath(true, ESearchType.CUTTABLE_TREE)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							markDuring(mov -> mov.path.getTargetPosition(),
								sequence(
									followPresearchedPath(),
									selector(
										sequence(
											goInDirectionWaitFree(EDirection.EAST),
											setDirectionNode(mov -> EDirection.NORTH_WEST),
											repeatLoop(6, playAction(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION)),
											goInDirectionWaitFree(EDirection.WEST)
										),
										sequence(
											setDirectionNode(mov -> EDirection.NORTH_WEST),
											repeatLoop(6, playAction(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION))
										)
									)
								)
							),
							executeSearch(ESearchType.CUTTABLE_TREE),
							ignoreFailure(
								sequence(
									goInDirectionWaitFree(EDirection.SOUTH_EAST),
									goInDirectionWaitFree(EDirection.NORTH_EAST),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									repeatLoop(3, playAction(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION)),
									goInDirectionWaitFree(EDirection.NORTH_EAST),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									repeatLoop(3, playAction(EMovableAction.ACTION1, LUMBERJACK_ACTION1_DURATION)),
									goInDirectionWaitFree(EDirection.SOUTH_WEST),
									setDirectionNode(mov -> EDirection.NORTH_WEST),
									take(mov -> EMaterialType.TRUNK, false)
								)
							),
							setMaterialNode(EMaterialType.TRUNK),
							goToOutputStack(EMaterialType.TRUNK),
							dropProduced(mov -> EMaterialType.TRUNK)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createWaterworkerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(EMaterialType.WATER),
							preSearchPath(false, ESearchType.RIVER)
						)
					),
					setMaterialNode(EMaterialType.EMPTY_BUCKET),
					show(),
					ignoreFailure(
						sequence(
							followPresearchedPath(),
							lookAtSearched(ESearchType.RIVER),
							playAction(EMovableAction.ACTION1, (short)1000),
							setMaterialNode(EMaterialType.WATER),
							goToOutputStack(EMaterialType.WATER),
							dropProduced(mov -> EMaterialType.WATER)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createFishermanBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(EMaterialType.FISH),
							preSearchPath(false, ESearchType.FISHABLE)
						)
					),
					setMaterialNode(EMaterialType.NO_MATERIAL),
					show(),
					ignoreFailure(
						sequence(
							followPresearchedPath(),
							lookAtSearched(ESearchType.FISHABLE),
							playAction(EMovableAction.ACTION1, (short)1500),
							selector(
								//try taking fish
								condition(mov -> mov.grid.tryTakingResource(mov.getDirection().getNextHexPoint(mov.position), EResourceType.FISH)),
								sequence(// fishing failed
									playAction(EMovableAction.ACTION2, (short)2000),
									alwaysFail()
								)
							),
							// fishing succeeded
							playAction(EMovableAction.ACTION3, (short)1000),
							setMaterialNode(EMaterialType.FISH),
							goToOutputStack(EMaterialType.FISH),
							setDirectionNode(mov -> EDirection.SOUTH_WEST),
							dropProduced(mov -> EMaterialType.FISH)
						)
					),
					enterHome()
				)
		);
	}

	private static final short STONECUTTER_ACTION1_DURATION = (short)750;
	private static final int STONECUTTER_CUT_STONE_ITERATIONS = 6;

	private static Node<SimpleBuildingWorkerMovable> createStonecutterBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							outputStackNotFull(EMaterialType.STONE),
							preSearchPath(true, ESearchType.CUTTABLE_STONE)
						)
					),
					setMaterialNode(EMaterialType.NO_MATERIAL),
					show(),
					ignoreFailure(
						sequence(
							markDuring(mov -> mov.path.getTargetPosition(),
								sequence(
									followPresearchedPath(),
									setDirectionNode(mov -> EDirection.SOUTH_WEST),
									repeatLoop(STONECUTTER_CUT_STONE_ITERATIONS, playAction(EMovableAction.ACTION1, STONECUTTER_ACTION1_DURATION))
								)
							),
							executeSearch(ESearchType.CUTTABLE_STONE),
							take(mov -> EMaterialType.STONE, false),
							goToOutputStack(EMaterialType.STONE),
							dropProduced(mov -> EMaterialType.STONE)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createWinegrowerBehaviour() {
		return defaultWorkCycle(
				ignoreFailure(
					sequence(
						waitFor(isAllowedToWork()),
						selector(
							sequence(
								outputStackNotFull(EMaterialType.WINE),
								preSearchPath(true, ESearchType.HARVESTABLE_WINE),
								ignoreFailure(
									sequence(
										setMaterialNode(EMaterialType.BASKET),
										show(),
										markDuring(mov -> mov.path.getTargetPosition(),
											sequence(
												followPresearchedPath(),
												setDirectionNode(mov -> EDirection.NORTH_WEST),
												take(mov -> EMaterialType.BASKET, false)
											)
										),
										executeSearch(ESearchType.HARVESTABLE_WINE),
										enterHome(),
										sleep(3000),
										setMaterialNode(EMaterialType.WINE),
										show(),
										goToOutputStack(EMaterialType.WINE),
										setDirectionNode(mov -> EDirection.NORTH_EAST),
										dropProduced(mov -> EMaterialType.WINE)
									)
								)
							),
							sequence(
								preSearchPathNoWarning(true, ESearchType.PLANTABLE_WINE),
								ignoreFailure(
									sequence(
										setMaterialNode(EMaterialType.PLANT),
										show(),
										markDuring(mov -> mov.path.getTargetPosition(),
											sequence(
												followPresearchedPath(),
												setDirectionNode(mov -> EDirection.SOUTH_WEST),
												playAction(EMovableAction.ACTION1, (short)4000)
											)
										),
										executeSearch(ESearchType.PLANTABLE_WINE),
										setMaterialNode(EMaterialType.NO_MATERIAL)
									)
								)
							),
							sequence(
								sleep(1000),
								alwaysFail()
							)
						),
						enterHome(),
						sleep(8000)
					)
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createFarmerBehaviour() {
		return defaultWorkCycle(
				ignoreFailure(
					sequence(
						waitFor(isAllowedToWork()),
						selector(
							sequence(
								outputStackNotFull(EMaterialType.CROP),
								preSearchPath(true, ESearchType.CUTTABLE_CORN),
								ignoreFailure(
									sequence(
										show(),
										setMaterialNode(EMaterialType.SCYTHE),
										markDuring(mov -> mov.path.getTargetPosition(),
											sequence(
												followPresearchedPath(),

												setDirectionNode(mov -> EDirection.SOUTH_WEST),
												playAction(EMovableAction.ACTION1, (short)1000),
												setDirectionNode(mov -> EDirection.NORTH_WEST),
												playAction(EMovableAction.ACTION1, (short)1000),
												setDirectionNode(mov -> EDirection.NORTH_EAST),
												playAction(EMovableAction.ACTION1, (short)1000),
												setDirectionNode(mov -> EDirection.SOUTH_EAST),
												playAction(EMovableAction.ACTION1, (short)1000)
											)
										),

										executeSearch(ESearchType.CUTTABLE_CORN),
										setDirectionNode(mov -> EDirection.NORTH_EAST),
										setMaterialNode(EMaterialType.NO_MATERIAL),
										take(mov -> EMaterialType.CROP, false),
										goToOutputStack(EMaterialType.CROP),
										setDirectionNode(mov -> EDirection.NORTH_EAST),
										dropProduced(mov -> EMaterialType.CROP)
									)
								)
							),
							sequence(
								preSearchPathNoWarning(true, ESearchType.PLANTABLE_CORN),
								ignoreFailure(
									sequence(
										show(),
										setMaterialNode(EMaterialType.PLANT),
										markDuring(mov -> mov.path.getTargetPosition(),
											sequence(
												followPresearchedPath(),

												setDirectionNode(mov -> EDirection.NORTH_EAST),
												playAction(EMovableAction.ACTION2, (short)1400),
												setDirectionNode(mov -> EDirection.SOUTH_EAST),
												playAction(EMovableAction.ACTION2, (short)1400),
												setDirectionNode(mov -> EDirection.SOUTH_WEST),
												playAction(EMovableAction.ACTION2, (short)1400),
												setDirectionNode(mov -> EDirection.NORTH_WEST),
												playAction(EMovableAction.ACTION2, (short)1400)
											)
										),

										executeSearch(ESearchType.PLANTABLE_CORN),
										setMaterialNode(EMaterialType.NO_MATERIAL)
									)
								)
							),
							sequence(
								sleep(1000),
								alwaysFail()
							)
						),
						enterHome(),
						sleep(8000)
					)
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createDockworkerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							condition(mov -> ((DockyardBuilding)mov.building).getOrderedShipType()!= null),
							selector(
								inputStackNotEmpty(EMaterialType.PLANK),
								inputStackNotEmpty(EMaterialType.IRON)
							)
						)
					),
					show(),
					selector(
						sequence(
							inputStackNotEmpty(EMaterialType.PLANK),
							goToInputStack(EMaterialType.PLANK),
							take(mov -> EMaterialType.PLANK, true)
						),
						sequence(
							inputStackNotEmpty(EMaterialType.IRON),
							goToInputStack(EMaterialType.IRON),
							take(mov -> EMaterialType.IRON, true)
						)
					),
					goToPos(mov -> ((DockyardBuilding)mov.building).getDock().getPosition()),
					setMaterialNode(EMaterialType.NO_MATERIAL),
					repeatLoop(6, sequence(
						playAction(EMovableAction.ACTION1, (short)750),
						action(mov -> ((DockyardBuilding)mov.building).buildShipAction())
					)),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createMillerBehaviour() {
		return busyWorkCycle(SimpleBuildingWorkerMovable::millerPreconditions,
				sequence(
					goToInputStack(EMaterialType.CROP),
					selector(
						sequence(
							condition(mov -> mov.getDirection().equals(EDirection.NORTH_WEST) ||
									MatchConstants.random().nextBoolean()),
							setDirectionNode(EDirection.NORTH_WEST)
						),
						setDirectionNode(EDirection.NORTH_EAST)
					),
					take(mov -> EMaterialType.CROP, true),
					enterHome(),
					sleep(1000),
					action(mov -> ((MillBuilding) mov.building).setRotating(true)),
					sleep(5000),
					action(mov -> ((MillBuilding) mov.building).setRotating(false)),
					sleep(1000),
					setMaterialNode(EMaterialType.FLOUR),
					show(),
					goToOutputStack(EMaterialType.FLOUR),
					selector(
						sequence(
							condition(mov -> mov.getDirection().equals(EDirection.NORTH_WEST) ||
									MatchConstants.random().nextBoolean()),
							setDirectionNode(EDirection.NORTH_WEST)
						),
						setDirectionNode(EDirection.NORTH_EAST)
					),
					dropProduced(mov -> EMaterialType.FLOUR)
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> millerPreconditions() {
		return sequence(
				isAllowedToWork(),
				inputStackNotEmpty(EMaterialType.CROP),
				outputStackNotFull(EMaterialType.FLOUR)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createSlaughtererBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(4000),
					waitFor(
						sequence(
							isAllowedToWork(),
							inputStackNotEmpty(EMaterialType.PIG),
							outputStackNotFull(EMaterialType.MEAT)
						)
					),
					show(),
					goToInputStack(EMaterialType.PIG),
					setDirectionNode(EDirection.NORTH_EAST),
					take(mov -> EMaterialType.PIG, true),
					enterHome(),
					sleep(1000),
					action(mov -> ((SlaughterhouseBuilding) mov.building).requestSound()),
					sleep(4700),
					setMaterialNode(EMaterialType.MEAT),
					show(),
					goToOutputStack(EMaterialType.MEAT),
					setDirectionNode(EDirection.NORTH_WEST),
					dropProduced(mov -> EMaterialType.MEAT),
					enterHome()
				)
		);
	}


	private static Node<SimpleBuildingWorkerMovable> createCharcoalBurnerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(1000),
					ignoreFailure(
						sequence(
							waitFor(
								sequence(
									isAllowedToWork(),
									outputStackNotFull(EMaterialType.COAL)
								)
							),
							repeatLoop(4,
								sequence(
									waitFor(
										sequence(
											isAllowedToWork(),
											inputStackNotEmpty(EMaterialType.PLANK)
										)
									),
									setMaterialNode(EMaterialType.NO_MATERIAL),
									show(),
									goToInputStack(EMaterialType.PLANK),
									setDirectionNode(EDirection.EAST),
									take(mov -> EMaterialType.PLANK, true),
									enterHome(),
									sleep(500)
								)
							),
							sleep(500),
							setSmoke((short) 5500),
							sleep(5000),
							playAction(EMovableAction.ACTION1, (short)500),
							setSmoke((short) 0),
							setMaterialNode(EMaterialType.COAL),
							waitFor(
								sequence(
									isAllowedToWork(),
									outputStackNotFull(EMaterialType.COAL)
								)
							),
							show(),
							goToOutputStack(EMaterialType.COAL),
							setDirectionNode(EDirection.SOUTH_WEST),
							dropProduced(mov -> EMaterialType.COAL)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createBrewerBehaviour() {
		return defaultWorkCycle(
				sequence(
						sleep(1000),
						waitFor(
								sequence(
										isAllowedToWork(),
										inputStackNotEmpty(EMaterialType.CROP),
										inputStackNotEmpty(EMaterialType.WATER),
										outputStackNotFull(EMaterialType.KEG)
								)
						),
						show(),
						ignoreFailure(
								sequence(
										dropIntoOven(EMaterialType.WATER, EDirection.NORTH_EAST),
										dropIntoOven(EMaterialType.CROP, EDirection.NORTH_EAST),
										enterHome(),
										sleep(5000),
										setMaterialNode(EMaterialType.KEG),
										show(),
										goToOutputStack(EMaterialType.KEG),
										setDirectionNode(EDirection.NORTH_WEST),
										dropProduced(mov -> EMaterialType.KEG)
								)
						),
						enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createRiceFarmerBehaviour() {
		return defaultWorkCycle(
				ignoreFailure(
					sequence(
						waitFor(isAllowedToWork()),
						selector(
							sequence(
								outputStackNotFull(EMaterialType.RICE),
								preSearchPath(true, ESearchType.HARVESTABLE_RICE),
								ignoreFailure(
									sequence(
										condition(mov -> true),
										setMaterialNode(EMaterialType.NO_MATERIAL),
										show(),
										markDuring(mov -> mov.path.getTargetPosition(),
											sequence(
												followPresearchedPath(),
												setMaterialNode(EMaterialType.RICE),
												playAction(EMovableAction.ACTION1, (short)3000)
											)
										),
										executeSearch(ESearchType.HARVESTABLE_RICE),
										goToOutputStack(EMaterialType.RICE),
										dropProduced(mov -> EMaterialType.RICE)
									)
								)
							),
							sequence(
								preSearchPathNoWarning(true, ESearchType.PLANTABLE_RICE),
								ignoreFailure(
									sequence(
										setMaterialNode(EMaterialType.PLANT),
										show(),
										markDuring(mov -> mov.path.getTargetPosition(),
											sequence(
												followPresearchedPath(),
												setDirectionNode(mov -> EDirection.NORTH_WEST),
												playAction(EMovableAction.ACTION1, (short)3000)
											)
										),
										executeSearch(ESearchType.PLANTABLE_RICE),
										setMaterialNode(EMaterialType.NO_MATERIAL)
									)
								)
							),
							sequence(
								sleep(1000),
								alwaysFail()
							)
						),
						enterHome(),
						sleep(8000)
					)
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createBeekeeperBehaviour() {
		return defaultWorkCycle(
				ignoreFailure(
					sequence(
						waitFor(isAllowedToWork()),
						selector(
							sequence(
								outputStackNotFull(EMaterialType.HONEY),
								preSearchPath(true, ESearchType.HARVESTABLE_HIVE),
								ignoreFailure(
									sequence(
										show(),
										setMaterialNode(EMaterialType.EMPTY_BUCKET),
										markDuring(mov -> mov.path.getTargetPosition(),
											sequence(
												followPresearchedPath(),

												playAction(EMovableAction.ACTION1, (short) 1400),
												setMaterialNode(EMaterialType.HONEY)
											)
										),

										executeSearch(ESearchType.HARVESTABLE_HIVE),
										goToOutputStack(EMaterialType.HONEY),
										dropProduced(mov -> EMaterialType.HONEY)
									)
								)
							),
							sequence(
								preSearchPathNoWarning(true, ESearchType.PLANTABLE_HIVE),
								ignoreFailure(
									sequence(
										show(),
										setMaterialNode(EMaterialType.PLANT),
										markDuring(mov -> mov.path.getTargetPosition(),
											sequence(
												followPresearchedPath(),

												playAction(EMovableAction.ACTION1, (short)1400)
											)
										),

										executeSearch(ESearchType.PLANTABLE_HIVE),
										setMaterialNode(EMaterialType.NO_MATERIAL)
									)
								)
							),
							sequence(
								sleep(1000),
								alwaysFail()
							)
						),
						enterHome(),
						sleep(8000)
					)
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createDistillerBehaviour() {
		return defaultWorkCycle(
				sequence(
					sleep(3000),
					waitFor(
						sequence(
							isAllowedToWork(),
							inputStackNotEmpty(EMaterialType.COAL),
							inputStackNotEmpty(EMaterialType.RICE),
							outputStackNotFull(EMaterialType.LIQUOR)
						)
					),
					show(),
					ignoreFailure(
						sequence(
							setMaterialNode(EMaterialType.NO_MATERIAL),
							dropIntoOven(EMaterialType.COAL, EDirection.NORTH_WEST),
							dropIntoOven(EMaterialType.RICE, EDirection.NORTH_WEST),
							enterHome(),
							sleep(5000),
							show(),
							setMaterialNode(EMaterialType.LIQUOR),
							goToOutputStack(EMaterialType.LIQUOR),
							setDirectionNode(EDirection.NORTH_EAST),
							dropProduced(mov -> EMaterialType.LIQUOR)
						)
					),
					enterHome()
				)
		);
	}

	private static Node<SimpleBuildingWorkerMovable> createMeadBrewerBehaviour() {
		return defaultWorkCycle(
			sequence(
				sleep(3000),
				waitFor(
					sequence(
						isAllowedToWork(),
						inputStackNotEmpty(EMaterialType.WATER),
						inputStackNotEmpty(EMaterialType.HONEY),
						outputStackNotFull(EMaterialType.MEAD)
					)
				),
				show(),
				ignoreFailure(
					sequence(
						dropIntoOven(EMaterialType.WATER, EDirection.NORTH_WEST),
						dropIntoOven(EMaterialType.HONEY, EDirection.NORTH_EAST),
						enterHome(),
						sleep(3000),
						setMaterialNode(EMaterialType.MEAD),
						show(),
						goToOutputStack(EMaterialType.MEAD),
						dropProduced(mov -> EMaterialType.MEAD)
					)
				),
				enterHome()
			)
		);
	}
}
