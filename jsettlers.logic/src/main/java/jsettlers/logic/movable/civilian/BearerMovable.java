package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.IIntegerSupplier;
import jsettlers.algorithms.simplebehaviortree.IShortSupplier;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableBearer;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IBarrack;
import jsettlers.logic.map.grid.partition.manager.materials.interfaces.IMaterialOffer;
import jsettlers.logic.map.grid.partition.manager.materials.interfaces.IMaterialRequest;
import jsettlers.logic.map.grid.partition.manager.materials.offers.EOfferPriority;
import jsettlers.logic.map.grid.partition.manager.objects.WorkerCreationRequest;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBearerMovable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.player.Player;
import jsettlers.shared.ShortPoint2D;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BearerMovable extends CivilianMovable implements IBearerMovable, IManageableBearer {

	private static final long serialVersionUID = 1L;

	private IMaterialOffer   offer;
	private IMaterialRequest request;
	private EMaterialType    materialType;

	private IBarrack              barrack;
	private IManageableBearer.IWorkerRequester workerRequester;
	private WorkerCreationRequest workerCreationRequest;

	private boolean registered = false;

	private boolean hasBed;

	public BearerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable replace) {
		super(grid, EMovableType.BEARER, position, player, replace);

		if(replace == null) {
			player.getBedInformation().addNewBearer();
			hasBed = true;
		} else {
			hasBed = false;
		}
	}

	static {
		MovableManager.registerBehaviour(EMovableType.BEARER, new Root<>(createBearerBehaviour()));
	}

	@Override
	public ILogicMovable convertTo(EMovableType newMovableType) {
		return createMovable(newMovableType, player, position, grid, this);
	}

	private static Node<BearerMovable> createBearerBehaviour() {
		return guardSelector(
				fleeIfNecessary(),
				guard(BearerMovable::isHomeless,
					selector(
						idleAction(),
						doRandomly(3/4f,
							selector(
								sequence(
									condition(mov -> mov.player.getCivilisation() != ECivilisation.AMAZON),
									playAction(EMovableAction.HOMELESS1, (short) 1500),
									ignoreFailure(repeatLoop(randomIntFromRange(2, 6),
										selector(
											doRandomly(1/8f, setDirectionNode(mov -> EDirection.VALUES[MatchConstants.random().nextInt(0, EDirection.NUMBER_OF_DIRECTIONS-1)])),
											doRandomly(7/8f, playAction(EMovableAction.HOMELESS_IDLE, randomShortFromRange(1000, 3000))),
											doRandomly(1/2f, playAction(EMovableAction.HOMELESS2, randomShortFromRange(1300, 1700))),
											playAction(EMovableAction.HOMELESS3, randomShortFromRange(1300, 1600))
										)
									)),
									playAction(EMovableAction.HOMELESS4, randomShortFromRange(1100, 1600))
								),
								sequence(
									playAction(EMovableAction.HOMELESS1, (short) 2000),
									playAction(EMovableAction.HOMELESS_IDLE, randomShortFromRange(1000, 3000)),
									playAction(EMovableAction.HOMELESS2, (short) 2000)
								)
							)
						),
						sleep(3000)
					)
				),
				guard(mov -> mov.barrack != null,
					selector(
						sequence(
							goToPos(mov -> mov.barrack.getDoor()),
							condition(mov -> {
								EMovableType soldierType = mov.barrack.popWeaponForBearer();
								if(soldierType == null) return false;

								ShortPoint2D targetPosition = mov.barrack.getSoldierTargetPosition();
								mov.barrack = null;


								mov.player.getEndgameStatistic().incrementAmountOfProducedSoldiers();
								ILogicMovable convertedMovable = mov.convertTo(soldierType);

								// TODO change
								convertedMovable.moveTo(targetPosition, EMoveToType.DEFAULT);
								return true;
							})
						),
						action(BearerMovable::abortJob)
					)
				),
				guard(mov -> mov.workerCreationRequest != null,
					selector(
						sequence(
							selector(
								condition(mov -> mov.offer == null),
								handleOffer()
							),
							action(mov -> {
								EMovableType newMovable = mov.workerCreationRequest.requestedMovableType();
								mov.workerRequester.workerCreationRequestFulfilled(mov.workerCreationRequest);
								mov.workerCreationRequest = null;
								mov.setMaterial(EMaterialType.NO_MATERIAL);
								mov.convertTo(newMovable);
							})
						),
						action(BearerMovable::abortJob)
					)
				),
				guard(mov -> mov.request != null,
					resetAfter(BearerMovable::forceDropMaterial,
						selector(
							sequence(
								handleOffer(),
								goToPos(mov -> mov.request.getPosition(), mov -> mov.request.isActive()),
								crouchDown(
									action(mov -> {
										EMaterialType takeDropMaterial = mov.getMaterial();

										mov.request.deliveryFulfilled();
										mov.request = null;

										mov.setMaterial(EMaterialType.NO_MATERIAL);
										mov.grid.dropMaterial(mov.position, takeDropMaterial, false, false);
									})
								)
							),
							action(BearerMovable::abortJob)
						)
					)
				),
				guard(mov -> !mov.registered,
					action(mov -> {
						mov.offer = null;
						mov.request = null;
						mov.barrack = null;
						mov.materialType = null;
						mov.workerRequester = null;
						mov.workerCreationRequest = null;
						mov.registered = true;
						mov.grid.addJobless(mov);
					})
				),
				doingNothingGuard()
		);
	}

	private static <T extends BearerMovable> IShortSupplier<T> randomShortFromRange(int min, int max) {
		return (mov) -> (short)MatchConstants.random().nextInt(min, max);
	}
	private static <T extends BearerMovable> IIntegerSupplier<T> randomIntFromRange(int min, int max) {
		return (mov) -> MatchConstants.random().nextInt(min, max);
	}

	private boolean isHomeless() {
		if(hasBed) {
			if(!player.getBedInformation().testIfBedStillExists()) {
				hasBed = false;
				abortJob();
			}
		}

		if(!hasBed) {
			if(player.getBedInformation().tryReservingBed()) {
				hasBed = true;
			}
		}

		return !hasBed;

	}

	private static Node<BearerMovable> handleOffer() {
		return sequence(
				goToPos(mov -> mov.offer.getPosition(), mov -> {
					EOfferPriority minimumAcceptedPriority = mov.request != null ? mov.request.getMinimumAcceptedOfferPriority() : EOfferPriority.LOWEST;
					return mov.offer.isStillValid(minimumAcceptedPriority);
				}),
				take(mov -> mov.materialType, true),
				action(mov -> {
					mov.offer.offerTaken();
					mov.offer = null;
				})
		);
	}

	@Override
	protected void abortJob() {
		forceDropMaterial();

		if(offer != null) offer.distributionAborted();

		if(workerCreationRequest != null) {
			workerRequester.workerCreationRequestFailed(workerCreationRequest);
			workerRequester = null;
			workerCreationRequest = null;
		}

		if(request != null) {
			request.deliveryAborted();
			request = null;
		}

		if(barrack != null) {
			barrack.bearerRequestFailed();
			barrack = null;
		}
	}

	@Override
	public void deliver(EMaterialType materialType, IMaterialOffer offer, IMaterialRequest request) {
		if(!registered) return;

		this.offer = offer;
		this.request = request;
		this.materialType = materialType;
		registered = false;

		offer.distributionAccepted();
		request.deliveryAccepted();
	}

	@Override
	public boolean becomeWorker(IManageableBearer.IWorkerRequester requester, WorkerCreationRequest workerCreationRequest, IMaterialOffer offer) {
		if(!registered) return false;

		this.workerRequester = requester;
		this.workerCreationRequest = workerCreationRequest;
		this.offer = offer;
		if(offer != null) {
			this.materialType = workerCreationRequest.requestedMovableType().getTool();
			offer.distributionAccepted();
		}
		registered = false;
		return true;
	}

	@Override
	public boolean becomeSoldier(IBarrack barrack) {
		if(!registered) return false;

		this.barrack = barrack;
		registered = false;
		return true;
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		abortJob();

		if(hasBed) {
			player.getBedInformation().removeBearer();
		}

		grid.removeJobless(this);
	}

	private void forceDropMaterial() {
		EMaterialType carriedMaterial = setMaterial(EMaterialType.NO_MATERIAL);

		if (carriedMaterial != EMaterialType.NO_MATERIAL) {
			grid.dropMaterial(position, carriedMaterial, true, false);
		}
	}
}
