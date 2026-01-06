package jsettlers.common.action;

import jsettlers.common.movable.ESpellType;
import jsettlers.common.sound.ESoundType;

public class AskCastSpellAction extends Action {

	private ESpellType spell;

	public AskCastSpellAction(ESpellType spell) {
		super(EActionType.ASK_CAST_SPELL, ESoundType.UI_INCREASE);
		this.spell = spell;
	}

	public ESpellType getSpell() {
		return spell;
	}
}
