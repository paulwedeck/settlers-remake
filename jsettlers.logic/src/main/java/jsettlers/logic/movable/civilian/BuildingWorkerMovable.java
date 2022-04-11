package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.IEMaterialTypeSupplier;
import jsettlers.algorithms.simplebehaviortree.IShortSupplier;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.nodes.Guard;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.EPriority;
import jsettlers.common.material.ESearchType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableWorker;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IWorkerRequestBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBuildingWorkerMovable;
import jsettlers.logic.player.Player;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Supplier;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BuildingWorkerMovable extends CivilianMovable implements IBuildingWorkerMovable, IManageableWorker {

	private static final long serialVersionUID = 1679128621183098990L;

	protected IWorkerRequestBuilding building;
	private boolean registered = false;
	protected int searchFailedCtr = 0;

	public BuildingWorkerMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace);
	}

	public static void resetProductionFile() {
		if(!MatchConstants.ENABLE_PRODUCTION_LOG) return;

		if(OUT != null) {
			OUT.close();
		}

		try {
			OUT = new PrintStream(new FileOutputStream("produced" + System.currentTimeMillis() + ".log"));
		} catch (IOException e) {
			throw new Error(e);
		}
	}

	private static PrintStream OUT;

	protected void produce(EMaterialType material) {
		if(material == EMaterialType.GOLD) {
			getPlayer().getEndgameStatistic().incrementAmountOfProducedGold();
		}

		if(MatchConstants.ENABLE_PRODUCTION_LOG) {
			OUT.println(MatchConstants.clock().getTime() + "," + material);
			OUT.flush();
		}
	}

	protected static <T extends BuildingWorkerMovable> Node<T> dropProduced(IEMaterialTypeSupplier<T> material) {
		return sequence(
				action(mov -> mov.produce(material.apply(mov))),
				drop(material, true)
		);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> lookAtSearched(ESearchType searchType) {
		return setDirectionNode(mov -> mov.grid.getDirectionOfSearched(mov.getPosition(), searchType));
	}

	protected static <T extends BuildingWorkerMovable> Node<T> executeSearch(ESearchType searchType) {
		return BehaviorTreeHelper.condition(mov -> mov.grid.executeSearchType(mov, mov.position, searchType));
	}

	protected ShortPoint2D getOutputStackPosition(EMaterialType outputMaterial) {
		for(RelativeStack stack : building.getBuildingVariant().getOfferStacks()) {
			if(stack.getMaterialType() == outputMaterial) {
				return stack.calculatePoint(building.getPosition());
			}
		}

		throw new AssertionError("stack for " + outputMaterial + " not found in " + building.getBuildingVariant());
	}

	protected ShortPoint2D getInputStackPosition(EMaterialType inputMaterial) {
		for(RelativeStack stack : building.getBuildingVariant().getRequestStacks()) {
			if(stack.getMaterialType() == inputMaterial) {
				return stack.calculatePoint(building.getPosition());
			}
		}

		throw new AssertionError("stack for " + inputMaterial + " not found in " + building.getBuildingVariant());
	}

	protected static <T extends BuildingWorkerMovable> Node<T> goToOutputStack(EMaterialType outputMaterial) {
		return goToOutputStack(mov -> outputMaterial);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> goToOutputStack(IEMaterialTypeSupplier<T> outputMaterial) {
		return goToPos(mov -> mov.getOutputStackPosition(outputMaterial.apply(mov)));
	}

	protected static <T extends BuildingWorkerMovable> Node<T> goToInputStack(EMaterialType outputMaterial) {
		return goToInputStack(mov -> outputMaterial);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> goToInputStack(IEMaterialTypeSupplier<T> outputMaterial) {
		return goToPos(mov -> mov.getInputStackPosition(outputMaterial.apply(mov)));
	}

	protected static <T extends BuildingWorkerMovable> Node<T> outputStackNotFull(EMaterialType outputMaterial) {
		return outputStackNotFull(mov -> outputMaterial);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> outputStackNotFull(IEMaterialTypeSupplier<T> outputMaterial) {
		return condition(mov -> {
			EMaterialType mat = outputMaterial.apply(mov);
			return mov.grid.canPushMaterial(mov.getOutputStackPosition(mat));
		});
	}

	protected static <T extends BuildingWorkerMovable> Node<T> inputStackNotEmpty(EMaterialType inputMaterial) {
		return inputStackNotEmpty(mov -> inputMaterial);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> inputStackNotEmpty(IEMaterialTypeSupplier<T> inputMaterial) {
		return condition(mov -> {
			EMaterialType realInputMaterial = inputMaterial.apply(mov);
			return mov.grid.canTakeMaterial(mov.getInputStackPosition(realInputMaterial), realInputMaterial);
		});
	}

	protected static <T extends BuildingWorkerMovable> Node<T> isAllowedToWork() {
		return condition(mov -> mov.building.getPriority() != EPriority.STOPPED);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> enterHome() {
		return sequence(
				// I am not sure if the repeat structure is actually necessary but it won't hurt
				repeat(mov -> !mov.building.getDoor().equals(mov.getPosition()),
						selector(
								goToPos(mov -> mov.building.getDoor()),
								sleep(1000)
						)
				),
				hide()
		);
	}

	protected static <T extends BuildingWorkerMovable> Guard<T> handleBuildingDestroyedGuard() {
		return guard(mov -> mov.building != null && mov.building.isDestroyed(),
				action(BuildingWorkerMovable::buildingDestroyed)
		);
	}

	protected static <T extends BuildingWorkerMovable> Guard<T> registerMovableGuard() {
		return guard(mov -> !((BuildingWorkerMovable)mov).registered,
				action(mov -> {
					mov.dropCurrentMaterial();
					mov.abortJob();
					((BuildingWorkerMovable)mov).registered = true;
					mov.grid.addJobless(mov);
				})
		);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> defaultWorkCycle(Node<T> doWork) {
		return defaultFramework(
			guard(mov -> mov.building != null,
				sequence(
					enterHome(),
					repeat(mov -> true, doWork)
				)
			)
		);
	}

	/**
	 * Use this work cycle to make the movable automatically enter its home when the preconditions are no longer met.
	 */
	protected static <T extends BuildingWorkerMovable> Node<T> busyWorkCycle(Supplier<Node<T>> preconditions, Node<T> doWork) {
		return defaultWorkCycle(
				sequence(
					waitFor(preconditions.get()),
					show(),
					ignoreFailure(
						repeat(mov -> true,
							sequence(
								preconditions.get(),
								doWork
							)
						)
					),
					enterHome()
				)
		);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> defaultFramework(Guard<T> doWork) {
		return guardSelector(
				fleeIfNecessary(),
				handleBuildingDestroyedGuard(),
				doWork,
				registerMovableGuard(),
				doingNothingGuard()
		);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> setSmoke(short duration) {
		return setSmoke(mov -> duration);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> setSmoke(IShortSupplier<T> duration) {
		return action(mov -> {
			ShortPoint2D pos = mov.building.getBuildingVariant()
					.getSmokePosition()
					.calculatePoint(mov.building.getPosition());

			EMapObjectType type = mov.building.getBuildingVariant().isSmokeWithFire() ? EMapObjectType.SMOKE_WITH_FIRE : EMapObjectType.SMOKE;
			mov.grid.placeSmoke(pos, type, duration.apply(mov));
			mov.building.addMapObjectCleanupPosition(pos, type);
		});
	}

	protected static <T extends BuildingWorkerMovable> Node<T> dropIntoOven(EMaterialType material, EDirection takeDirection) {
		return sequence(
				goToInputStack(material),
				setDirectionNode(takeDirection),
				take(mov -> material, true),

				goToPos(mov -> mov.building.getBuildingVariant().getOvenPosition().calculatePoint(mov.building.getPosition())),
				setDirectionNode(mov -> mov.building.getBuildingVariant().getOvenPosition().getDirection()),
				crouchDown(setMaterialNode(EMaterialType.NO_MATERIAL))
		);
	}

	@Override
	protected boolean isBusy() {
		return super.isBusy() || !registered;
	}

	@Override
	public IBuilding getGarrisonedBuilding() {
		return building;
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		dropCurrentMaterial();

		if(building != null) {
			building.leaveBuilding(this);
		} else {
			grid.removeJobless(this);
		}
	}

	/*
	 * @param dijkstra
	 * 		if true, dijkstra algorithm is used<br>
	 * 		if false, in area finder is used.
	 */
	protected static <T extends BuildingWorkerMovable> Node<T> preSearchPathNoWarning(boolean dijkstra, ESearchType searchType) {
		return BehaviorTreeHelper.condition(mov -> {
			ShortPoint2D workAreaCenter = mov.building.getWorkAreaCenter();

			return mov.preSearchPath(dijkstra, workAreaCenter.x, workAreaCenter.y, mov.building.getBuildingVariant().getWorkRadius(),
					searchType);
		});
	}

	protected static <T extends BuildingWorkerMovable> Node<T> preSearchPath(boolean dijkstra, ESearchType searchType) {
		return selector(
				sequence(
					preSearchPathNoWarning(dijkstra, searchType),
					action(mov -> {
						mov.searchFailedCtr = 0;
						mov.building.setCannotWork(false);
					})
				),
				sequence(
					action(mov -> {
						mov.searchFailedCtr++;

						if (mov.searchFailedCtr > 10) {
							mov.building.setCannotWork(true);
							mov.player.showMessage(SimpleMessage.cannotFindWork(mov.building));
						}
					}),
					alwaysFail()
				)
		);
	}

	protected boolean tryTakingResource() {
		if(building.getBuildingVariant().isVariantOf(EBuildingType.FISHER)) {
			EDirection fishDirection = getDirection();
			return grid.tryTakingResource(fishDirection.getNextHexPoint(position), EResourceType.FISH);
		} else if(building.getBuildingVariant().isMine()) {
			return building.tryTakingResource();
		} else {
			return false;
		}
	}

	@Override
	public void setWorkerJob(IWorkerRequestBuilding building) {
		this.building = building;
		building.occupyBuilding(this);
		this.registered = false;
	}

	@Override
	protected void abortJob() {
		if(building != null) {
			building.leaveBuilding(this);
			building = null;
		}

		dropCurrentMaterial();
	}

	@Override
	public void buildingDestroyed() {
		setVisible(true);

		dropCurrentMaterial();

		building = null;
	}

	protected void dropCurrentMaterial() {
		EMaterialType material = getMaterial();
		if (material.isDroppable()) {
			grid.dropMaterial(position, material, true, false);
		}
		super.setMaterial(EMaterialType.NO_MATERIAL);
	}
}
