package jsettlers.logic.movable.other;

import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IFerryMovable;
import jsettlers.logic.movable.interfaces.IHealerMovable;
import jsettlers.logic.player.Player;

public class AttackableHumanMovable extends AttackableMovable implements IAttackableHumanMovable {

	private static final long serialVersionUID = 6890695823402563L;
	protected EMoveToType nextMoveToType;
	protected ShortPoint2D nextTarget = null;
	protected boolean goingToHealer = false;

	// the following data only for ship passengers
	protected IFerryMovable ferryToEnter = null;

	public AttackableHumanMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);
	}

	@Override
	public void leaveFerryAt(ShortPoint2D position) {
		this.position = position;
		setState(Movable.EMovableState.ACTIVE);

		grid.enterPosition(position, this, true);
	}

	@Override
	public void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType) {
		if(!playerControlled) return;

		nextTarget = targetPosition;
		nextMoveToType = moveToType;
		goingToHealer = false;
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		if(!playerControlled) return;

		nextTarget = position;
		nextMoveToType = stop? EMoveToType.FORCED : EMoveToType.DEFAULT;
		goingToHealer = false;
	}

	@Override
	public void moveToFerry(IFerryMovable ferry, ShortPoint2D entrancePosition) {
		if(!playerControlled) return;

		ferryToEnter = ferry;
		nextTarget = entrancePosition;
		nextMoveToType = EMoveToType.FORCED;
		goingToHealer = false;
	}

	@Override
	public void heal() {
		health = getMovableType().getHealth();
	}

	@Override
	public boolean isGoingToTreatment() {
		return goingToHealer;
	}

	@Override
	public boolean needsTreatment() {
		if(health == getMovableType().getHealth()) return false;
		if(!playerControlled) return false;
		return true;
	}

	@Override
	public boolean pingWounded(IHealerMovable healer) {
		if(!needsTreatment() || isGoingToTreatment()) return false;

		nextTarget = healer.getHealSpot();
		nextMoveToType = EMoveToType.FORCED;
		goingToHealer = true;
		return true;
	}

	@Override
	public void defectTo(Player player) {
		Movable.createMovable(getMovableType(), player, position, grid, this);
	}

	@Override
	public ShortPoint2D getFoWPosition() {
		if(isOnFerry()) return null;

		return position;
	}

	protected void enterFerry() {
		if(ferryToEnter == null) return;

		int distanceToFerry = position.getOnGridDistTo(ferryToEnter.getPosition());
		if(distanceToFerry <= Constants.MAX_FERRY_ENTRANCE_DISTANCE) {
			if (ferryToEnter.addPassenger(this)) {
				grid.leavePosition(position, this);
				setState(EMovableState.ON_FERRY);
			}
		}
		ferryToEnter = null;
	}
}
