package jsettlers.common.action;

import java.util.Objects;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

public class SetMoveableRatioAction extends PointAction {

	private final EMovableType moveableType;
	private final float ratio;

	public SetMoveableRatioAction(EMovableType moveableType, ShortPoint2D position, float ratio) {
		super(EActionType.SET_MOVEABLE_RATIO, position);
		this.moveableType = moveableType;
		this.ratio = ratio;
	}

	public EMovableType getMoveableType() {
		return moveableType;
	}

	public float getRatio() {
		return ratio;
	}

	@Override
	public int hashCode() {
		return Objects.hash(moveableType, ratio);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SetMoveableRatioAction)) {
			return false;
		}
		SetMoveableRatioAction other = (SetMoveableRatioAction) obj;
		return moveableType == other.moveableType && Float.floatToIntBits(ratio) == Float.floatToIntBits(other.ratio);
	}

	@Override
	public String toString() {
		return "SetMoveableRatioAction [moveableType=" + moveableType + ", ratio=" + ratio + "]";
	}
}
