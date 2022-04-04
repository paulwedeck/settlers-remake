package jsettlers.input.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;

import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;

public class ChangeMovableSettingsTask extends SimpleGuiTask {

	private ShortPoint2D position;
	private EMovableType movableType;
	private boolean relative;
	private int amount;

	public ChangeMovableSettingsTask() {
	}

	public ChangeMovableSettingsTask(byte playerId, ShortPoint2D position, boolean relative, int amount, EMovableType movableType) {
		super(EGuiAction.CHANGE_MOVABLE_SETTINGS, playerId);
		this.position = position;
		this.movableType = movableType;
		this.relative = relative;
		this.amount = amount;
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

	public int getAmount() {
		return amount;
	}

	@Override
	protected void serializeTask(DataOutputStream dos) throws IOException {
		super.serializeTask(dos);
		SimpleGuiTask.serializePosition(dos, position);
		dos.writeInt(movableType.ordinal());
		dos.writeBoolean(relative);
		dos.writeInt(amount);
	}

	@Override
	protected void deserializeTask(DataInputStream dis) throws IOException {
		super.deserializeTask(dis);
		position = SimpleGuiTask.deserializePosition(dis);
		movableType = EMovableType.VALUES[dis.readInt()];
		relative = dis.readBoolean();
		amount = dis.readInt();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		if (!super.equals(o)) {
			return false;
		}
		ChangeMovableSettingsTask that = (ChangeMovableSettingsTask) o;
		return relative == that.relative && amount == that.amount && position.equals(that.position) &&
				movableType == that.movableType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), position, movableType, relative, amount);
	}

	@Override
	public String toString() {
		return "ChangeMovableSettingsTask{" +
				"position=" + position +
				", movableType=" + movableType +
				", relative=" + relative +
				", amount=" + amount +
				'}';
	}
}
