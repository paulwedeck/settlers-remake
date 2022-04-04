package jsettlers.logic.movable.other;

import java.util.ArrayList;
import java.util.List;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IFerryMovable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class FerryMovable extends AttackableMovable implements IFerryMovable {

	private static final int MAX_NUMBER_OF_PASSENGERS = 7;
	private static final long serialVersionUID = -8381283159755498644L;

	private final List<IAttackableHumanMovable> passengers = new ArrayList<>(MAX_NUMBER_OF_PASSENGERS);

	private ShortPoint2D currentTarget = null;
	private ShortPoint2D nextTarget = null;

	public FerryMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.FERRY, position, player, movable);

	}

	static {
		MovableManager.registerBehaviour(EMovableType.FERRY, new Root<>(createFerryBehaviour()));
	}

	private static Node<FerryMovable> createFerryBehaviour() {
		return guardSelector(
				guard(mov -> mov.nextTarget != null,
					action(mov -> {
						mov.currentTarget = mov.nextTarget;
						mov.nextTarget = null;
					})
				),
				guard(mov -> mov.currentTarget != null,
					resetAfter(mov -> mov.currentTarget = null,
						goToPos(mov -> mov.currentTarget)
					)
				),
				doingNothingGuard()
		);
	}

	@Override
	public List<? extends IAttackableHumanMovable> getPassengers() {
		return passengers;
	}

	@Override
	public boolean addPassenger(IAttackableHumanMovable movable) {
		if (passengers.size() == MAX_NUMBER_OF_PASSENGERS) return false;

		passengers.add(movable);
		return true;
	}

	@Override
	public void unloadFerry() {
		if (passengers.isEmpty()) {
			return;
		}

		ShortPoint2D position = getPosition();

		HexGridArea.stream(position.x, position.y, 2, Constants.MAX_FERRY_UNLOADING_RADIUS)
				.filterBounds(grid.getWidth(), grid.getHeight())
				.filter((x, y) -> !grid.isWater(x, y))
				.iterate((x, y) -> {
					IAttackableHumanMovable passenger = passengers.get(passengers.size() - 1);

					if (grid.isValidPosition(passenger, x, y) && grid.isFreePosition(x,y)) {
						passenger.leaveFerryAt(new ShortPoint2D(x, y));
						passengers.remove(passengers.size() - 1);
					}

					return !passengers.isEmpty();
				});
	}

	@Override
	protected void killMovable() {
		super.killMovable();

		passengers.forEach(ILogicMovable::kill);
	}

	@Override
	public void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType) {
		nextTarget = targetPosition;
	}
}
