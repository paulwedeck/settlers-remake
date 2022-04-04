package jsettlers.common.action;

import jsettlers.common.movable.ESpellType;
import jsettlers.common.position.ShortPoint2D;

public class CastSpellAction extends PointAction {

	private ESpellType spell;

	public CastSpellAction(ESpellType spell, ShortPoint2D position) {
		super(EActionType.CAST_SPELL, position);
		this.spell = spell;
	}

	public ESpellType getSpell() {
		return spell;
	}
}
