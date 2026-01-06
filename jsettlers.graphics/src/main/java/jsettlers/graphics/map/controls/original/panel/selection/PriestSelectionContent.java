package jsettlers.graphics.map.controls.original.panel.selection;

import java.util.Locale;

import go.graphics.GLDrawContext;
import go.graphics.text.EFontSize;
import jsettlers.common.action.Action;
import jsettlers.common.action.AskCastSpellAction;
import jsettlers.common.action.EActionType;
import jsettlers.common.images.ImageLink;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.player.IMannaInformation;
import jsettlers.common.player.IPlayer;
import jsettlers.common.selectable.ISelectionSet;
import jsettlers.common.sound.ESoundType;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.ui.Button;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIPanel;

public class PriestSelectionContent extends AbstractSelectionContent {

	private final IMannaInformation mannaInformation;
	private final ISelectionSet selection;
	private final Label availableManna;
	private final String availableMannaString;

	public PriestSelectionContent(ISelectionSet selection) {
		super();
		this.selection = selection;
		spell_cost = Labels.getString("spell_cost");

		availableManna = new Label("", EFontSize.NORMAL);
		availableMannaString = Labels.getString("manna_available");

		panel.addChild(availableManna, 0, .95f, 1, 1f);

		//SoldierSelectionContent.addRowsToPanel(panel, selection, new EMovableType[] {EMovableType.MAGE});

		IPlayer selectionPlayer = selection.get(0).getPlayer();
		if(selectionPlayer instanceof IInGamePlayer) {
			mannaInformation = ((IInGamePlayer) selectionPlayer).getMannaInformation();

		} else {
			mannaInformation = null;
		}

		int i = 0;
		for(ESpellType spell : ESpellType.values()) {
			if(!spell.availableForCiv(selectionPlayer.getCivilisation())) continue;

			float top = .95f-i*.1f;
			panel.addChild(new SpellContent(spell), .1f, top-.1f, 1, top);

			i++;
		}

		UIPanel kill = new LabeledButton(Labels.getString("kill"), new Action(EActionType.DESTROY, ESoundType.UI_DECREASE_MAX));
		UIPanel stop = new LabeledButton(Labels.getString("stop"), new Action(EActionType.STOP_WORKING, ESoundType.MOVE_ACTION));

		panel.addChild(kill, .1f, 0.025f, .5f, .125f);
		panel.addChild(stop, .5f, 0.025f, .9f, .125f);
	}

	private final UIPanel panel = new UIPanel() {
		@Override
		public void drawAt(GLDrawContext gl) {
			availableManna.setText(String.format(Locale.ENGLISH, availableMannaString, mannaInformation.getAmountOfManna()));
			super.drawAt(gl);
		}
	};

	@Override
	public UIPanel getPanel() {
		return panel;
	}

	private final String spell_cost;

	private class SpellContent extends UIPanel {

		private final Action askCastSpellAction;
		private final ESpellType spell;
		private final Button spell_icon;
		private final Label spell_label;
		private final String spell_name;

		public SpellContent(ESpellType spell) {
			this.spell = spell;
			spell_icon = new Button(spell.getImageLink(), Labels.getString("spell_desc_" + spell));
			askCastSpellAction = new AskCastSpellAction(spell);
			spell_name = Labels.getName(spell);
			spell_label = new Label("", EFontSize.SMALL);
			addChild(spell_icon, 0, 0, 3/9f, 1);
			addChild(spell_label, 3.5f/9, 0f, .9f, 1);

		}

		@Override
		public void drawAt(GLDrawContext gl) {
			short cost = mannaInformation.getSpellCost(spell);

			spell_label.setText(spell_name + "\n" + String.format(Locale.ENGLISH, spell_cost, cost));
			if(mannaInformation.canUseSpell(spell)) {
				spell_icon.setIntensity(1);
				spell_label.setIntensity(1);
				spell_icon.setAction(askCastSpellAction);
			} else {
				spell_icon.setIntensity(.5f);
				spell_label.setIntensity(.5f);
				spell_icon.setAction(null);
			}
			super.drawAt(gl);
		}
	}
}
