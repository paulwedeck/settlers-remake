package jsettlers.logic.movable.cargo;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.ITradeBuilding;
import jsettlers.logic.trading.ITrader;
import jsettlers.logic.trading.TransportationRequest;
import jsettlers.logic.trading.TradeManager;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.other.AttackableMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public abstract class CargoMovable extends AttackableMovable implements ITrader {

	private static final long serialVersionUID = 1L;

	protected TransportationRequest request;
	protected ShortPoint2D gotoPosition;

	public CargoMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);

		getTradeManager().registerTrader(this);
	}

	static {
		Root<CargoMovable> cargoBehaviour = new Root<>(createCargoBehaviour());
		MovableManager.registerBehaviour(EMovableType.CARGO_SHIP, cargoBehaviour);
		MovableManager.registerBehaviour(EMovableType.DONKEY, cargoBehaviour);
	}

	private static Node<CargoMovable> createCargoBehaviour() {
		return guardSelector(
			guard(mov -> mov.request != null && mov.request.isActive(),
				resetAfter(mov -> {
						mov.request.finishTask();
						mov.request = null;
					},
					sequence(
						goToPos(mov -> mov.request.getStart()),
						condition(mov -> mov.loadUp(mov.request.getBuilding())),
						action(mov -> mov.request.receivedGoods()),
						ignoreFailure(repeat(mov -> mov.request.hasNextWaypoint(),
							sequence(
								condition(mov -> {
									ShortPoint2D nextPosition = mov.request.nextWaypoint();
									return mov.preSearchPath(true, nextPosition.x, nextPosition.y, mov.getWaypointSearchRadius(), ESearchType.VALID_FREE_POSITION);
								}),
								followPresearchedPath(mov -> !mov.lostCargo)
							)
						)),
						action(CargoMovable::dropMaterialIfPossible)
					)
				)
			),
			guard(mov -> mov.gotoPosition != null,
				resetAfter(mov -> mov.gotoPosition = null,
					goToPos(mov -> mov.gotoPosition)
				)
			),
			doingNothingGuard()
		);
	}

	@Override
	public void moveGoods(TransportationRequest transportRequest) {
		this.gotoPosition = null;
		this.request = transportRequest;
	}

	@Override
	public void goToTradeBuilding(ShortPoint2D position) {
		this.gotoPosition = position;
	}

	@Override
	public boolean canReachPosition(ShortPoint2D target) {
		return grid.isReachable(position, target, isShip());
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		getTradeManager().removeTrader(this, request);

		dropMaterialIfPossible();
	}

	protected boolean lostCargo = false;

	/**
	 *
	 * @return
	 * true if the tradeBuilding had material, the unit is loaded and should proceed to the target destination
	 */
	protected abstract boolean loadUp(ITradeBuilding tradeBuilding);

	protected abstract void dropMaterialIfPossible();

	protected abstract TradeManager getTradeManager();

	protected abstract short getWaypointSearchRadius();
}
