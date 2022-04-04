package jsettlers.input.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

import jsettlers.common.movable.ESpellType;
import jsettlers.common.position.ShortPoint2D;

public class CastSpellGuiTask extends MovableGuiTask {

	private ESpellType spell;
	private ShortPoint2D at;

	public CastSpellGuiTask() {
	}

	public CastSpellGuiTask(byte playerId, ShortPoint2D at, int position, ESpellType spell) {
		super(EGuiAction.CAST_SPELL, playerId, Arrays.asList(position));

		this.spell = spell;
		this.at = at;
	}

	public ESpellType getSpell() {
		return spell;
	}

	public ShortPoint2D getAt() {
		return at;
	}

	@Override
	protected void serializeTask(DataOutputStream dos) throws IOException {
		super.serializeTask(dos);
		SimpleGuiTask.serializePosition(dos, at);
		dos.writeByte(spell.ordinal());
	}

	@Override
	protected void deserializeTask(DataInputStream dis) throws IOException {
		super.deserializeTask(dis);

		at = SimpleGuiTask.deserializePosition(dis);
		spell = ESpellType.values()[dis.readByte()];
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (at != null ? at.hashCode() : 0);
		result = 31 * result + (spell != null ? spell.ordinal() : 0);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof CastSpellGuiTask) {
			CastSpellGuiTask obj2 = (CastSpellGuiTask) obj;
			return obj2.spell == spell && (obj2.at == null || obj2.at.equals(at)) && super.equals(obj);
		}

		return false;
	}
}
