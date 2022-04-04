package jsettlers.input.tasks;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

public class SetMovableLimitTypeTask extends SimpleGuiTask {

	private ShortPoint2D position;
	private EMovableType movableType;
	private boolean relative;

	public SetMovableLimitTypeTask() {

	}

	public SetMovableLimitTypeTask(byte playerId, ShortPoint2D position, EMovableType movableType, boolean relative) {
		super(EGuiAction.SET_MOVABLE_LIMIT_TYPE, playerId);
		this.position = position;
		this.movableType = movableType;
		this.relative = relative;
	}

	public ShortPoint2D getPosition() {
		return position;
	}

	public EMovableType getMovableType() {
		return movableType;
	}

	public boolean isRelative() {
		return relative;
	}

	@Override
	protected void serializeTask(DataOutputStream dos) throws IOException {
		super.serializeTask(dos);
		SimpleGuiTask.serializePosition(dos, position);
		dos.writeInt(movableType.ordinal());
		dos.writeBoolean(relative);
	}

	@Override
	protected void deserializeTask(DataInputStream dis) throws IOException {
		super.deserializeTask(dis);
		position = SimpleGuiTask.deserializePosition(dis);
		movableType = EMovableType.VALUES[dis.readInt()];
		relative = dis.readBoolean();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof SetMovableLimitTypeTask)) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		SetMovableLimitTypeTask that = (SetMovableLimitTypeTask) o;
		return relative == that.relative && Objects.equals(position, that.position) &&
				movableType == that.movableType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), position, movableType, relative);
	}
}
