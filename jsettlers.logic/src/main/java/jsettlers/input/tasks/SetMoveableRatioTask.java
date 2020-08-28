package jsettlers.input.tasks;

import java.util.Objects;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

public class SetMoveableRatioTask extends SimpleGuiTask {

	private final ShortPoint2D position;
	private final EMovableType moveableType;
	private final float ratio;

	public SetMoveableRatioTask(byte playerId, ShortPoint2D position, EMovableType moveableType, float ratio) {
		super(EGuiAction.SET_MOVEABLE_RATIO, playerId);
		this.position = position;
		this.moveableType = moveableType;
		this.ratio = ratio;
	}

	public ShortPoint2D getPosition() {
		return position;
	}

	public EMovableType getMoveableType() {
		return moveableType;
	}

	public float getRatio() {
		return ratio;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(moveableType, position, ratio);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof SetMoveableRatioTask)) {
			return false;
		}
		SetMoveableRatioTask other = (SetMoveableRatioTask) obj;
		return moveableType == other.moveableType && Objects.equals(position, other.position) && Float.floatToIntBits(ratio) == Float.floatToIntBits(other.ratio);
	}

	@Override
	public String toString() {
		return "SetMoveableRatioTask [position=" + position + ", moveableType=" + moveableType + ", ratio=" + ratio + "]";
	}
}
