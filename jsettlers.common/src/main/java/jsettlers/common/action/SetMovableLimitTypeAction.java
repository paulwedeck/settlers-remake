package jsettlers.common.action;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

import java.util.Objects;

public class SetMovableLimitTypeAction extends PointAction {

	private final EMovableType movableType;
	private final boolean relative;

	public SetMovableLimitTypeAction(ShortPoint2D position, EMovableType movableType, boolean relative) {
		super(EActionType.SET_MOVABLE_LIMIT_TYPE, position);

		this.movableType = movableType;
		this.relative = relative;
	}

	public EMovableType getMovableType() {
		return movableType;
	}

	public boolean isRelative() {
		return relative;
	}

	@Override
	public String toString() {
		return "ChangeMovableLimitTypeAction{" +
				"movableType= " + movableType +
				"relative=" + relative +
				"position=" + getPosition() +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SetMovableLimitTypeAction)) {
			return false;
		}
		SetMovableLimitTypeAction that = (SetMovableLimitTypeAction) o;
		return movableType.equals(that.movableType) && relative == that.relative && that.getPosition().equals(getPosition());
	}

	@Override
	public int hashCode() {
		return Objects.hash(movableType, relative, getPosition());
	}
}
