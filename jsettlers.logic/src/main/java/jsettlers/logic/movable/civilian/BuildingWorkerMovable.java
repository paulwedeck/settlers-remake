package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.IBooleanConditionFunction;
import jsettlers.algorithms.simplebehaviortree.IEMaterialTypeSupplier;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.algorithms.simplebehaviortree.nodes.Guard;
import jsettlers.algorithms.simplebehaviortree.nodes.Repeat;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.landscape.EResourceType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.EPriority;
import jsettlers.common.material.ESearchType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableWorker;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IWorkerRequestBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBuildingWorkerMovable;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BuildingWorkerMovable extends CivilianMovable implements IBuildingWorkerMovable, IManageableWorker {

	protected IWorkerRequestBuilding building;
	private boolean registered = false;
	protected int searchFailedCtr = 0;

	public BuildingWorkerMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace, Root<? extends BuildingWorkerMovable> behaviour) {
		super(grid, movableType, position, player, replace, behaviour);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> dropProduced(IEMaterialTypeSupplier<T> material) {
		return sequence(
				BehaviorTreeHelper.action(mov -> {
					if(material.apply(mov) == EMaterialType.GOLD) {
						mov.getPlayer().getEndgameStatistic().incrementAmountOfProducedGold();
					}
				}),
				drop(material, mov -> true)
		);
	}
	protected static <T extends BuildingWorkerMovable> Node<T> lookAtSearched(ESearchType searchType) {
		return setDirectionNode(mov -> ((BuildingWorkerMovable)mov).grid.getDirectionOfSearched(mov.getPosition(), searchType));
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

	protected static <T extends BuildingWorkerMovable> Node<T> goToOutputStack(EMaterialType outputMaterial, IBooleanConditionFunction<T> pathStep) {
		return goToPos(mov -> mov.getOutputStackPosition(outputMaterial), pathStep);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> outputStackNotFull(EMaterialType outputMaterial) {
		return condition(mov -> mov.grid.canPushMaterial(mov.getOutputStackPosition(outputMaterial)));
	}

	protected static <T extends BuildingWorkerMovable> Node<T> isAllowedToWork() {
		return condition(mov -> mov.building.getPriority() != EPriority.STOPPED);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> enterHome() {
		return sequence(
				// I am not sure if the repeat structure is actually necessary but it won't hurt
				repeat(mov -> !((BuildingWorkerMovable)mov).building.getDoor().equals(((BuildingWorkerMovable)mov).getPosition()),
						selector(
								goToPos(mov -> ((BuildingWorkerMovable)mov).building.getDoor(), BuildingWorkerMovable::tmpPathStep), // TODO
								sleep(1000)
						)
				),
				hide()
		);
	};

	protected static <T extends BuildingWorkerMovable> Guard<T> handleBuildingDestroyedGuard() {
		return guard(mov -> mov.building != null && mov.building.isDestroyed(),
				BehaviorTreeHelper.action(BuildingWorkerMovable::buildingDestroyed)
		);
	}

	protected static <T extends BuildingWorkerMovable> Guard<T> registerMovableGuard() {
		return guard(mov -> !((BuildingWorkerMovable)mov).registered,
				BehaviorTreeHelper.action(mov -> {
					mov.dropCurrentMaterial();
					mov.abortJob();
					((BuildingWorkerMovable)mov).registered = true;
					mov.pathStep = null;
					mov.grid.addJobless(mov);
				})
		);
	}

	protected static <T extends BuildingWorkerMovable> Node<T> defaultWorkCycle(Node<T> doWork) {
		return defaultFramework(
				guard(mov -> ((BuildingWorkerMovable)mov).building != null,
						sequence(
								enterHome(),
								repeat(mov -> true, doWork)
						)
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

	@Override
	protected boolean isBusy() {
		return super.isBusy() || !registered;
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

	/*protected void dropAction(EMaterialType materialType) {
		if (materialType == EMaterialType.GOLD) {
			player.getEndgameStatistic().incrementAmountOfProducedGold();
		}
	}*/

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
						BehaviorTreeHelper.action(mov -> {
							mov.searchFailedCtr = 0;
							mov.building.setCannotWork(false);
						})
				),
				sequence(
						BehaviorTreeHelper.action(mov -> {
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

	protected boolean tmpPathStep() {
		return building != null;
	}
}