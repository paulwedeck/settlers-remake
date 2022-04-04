package jsettlers.common.action;

import java.util.Objects;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

public class ChangeMovableSettingsAction extends PointAction {

	private final EMovableType movableType;
	private final boolean relative;
	private final int amount;

	public ChangeMovableSettingsAction(EMovableType movableType, boolean relative, int amount, ShortPoint2D position) {
		super(EActionType.CHANGE_MOVABLE_SETTINGS, position);
		this.movableType = movableType;
		this.relative = relative;
		this.amount = amount;
	}

	public EMovableType getMovableType() {
		return movableType;
	}

	public boolean isRelative() {
		return relative;
	}

	public int getAmount() {
		return amount;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ChangeMovableSettingsAction that = (ChangeMovableSettingsAction) o;
		return relative == that.relative && amount == that.amount && movableType == that.movableType && getPosition().equals(that.getPosition());
	}

	@Override
	public int hashCode() {
		return Objects.hash(movableType, relative, amount, getPosition());
	}

	@Override
	public String toString() {
		return "ChangeMovableSettingsAction{" +
				"movableType=" + movableType +
				", relative=" + relative +
				", amount=" + amount +
				", position=" + getPosition() +
				'}';
	}
}
