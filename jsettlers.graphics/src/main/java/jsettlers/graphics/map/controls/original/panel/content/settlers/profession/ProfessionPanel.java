package jsettlers.graphics.map.controls.original.panel.content.settlers.profession;

import java.util.function.BiFunction;

import go.graphics.text.EFontSize;
import jsettlers.common.map.IGraphicsGrid;
import jsettlers.common.map.partition.IPartitionSettings;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.graphics.map.controls.original.panel.content.AbstractContentProvider;
import jsettlers.graphics.map.controls.original.panel.content.BarFill;
import jsettlers.graphics.map.controls.original.panel.content.ESecondaryTabType;
import jsettlers.graphics.map.controls.original.panel.content.updaters.UiContentUpdater;
import jsettlers.graphics.map.controls.original.panel.content.updaters.UiLocationDependingContentUpdater;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.UIPanel;
import jsettlers.graphics.ui.layout.ProfessionsLayout;

public class ProfessionPanel extends AbstractContentProvider implements UiContentUpdater.IUiContentReceiver<IPartitionSettings> {

	private UIPanel panel;
	private final UiLocationDependingContentUpdater<IPartitionSettings> uiContentUpdater = new UiLocationDependingContentUpdater<>(this::currentDistributionSettingsProvider);

	public ProfessionPanel() {
		panel = new ProfessionsLayout()._root;
		uiContentUpdater.addListener(this);
	}

	@Override
	public UIPanel getPanel() {
		return panel;
	}

	private IPartitionSettings currentDistributionSettingsProvider(IGraphicsGrid grid, ShortPoint2D position) {
		if (grid.getPlayerIdAt(position.x, position.y) >= 0) {
			return grid.getPartitionData(position.x, position.y).getPartitionSettings();
		}

		return null;
	}

	@Override
	public ESecondaryTabType getTabs() {
		return ESecondaryTabType.SETTLERS;
	}

	@Override
	public void update(IPartitionSettings data) {

	}

	public enum ConfigurableRatioType {
		BEARER,
		DIGGER,
		BRICKLAYER
	}

	public static class RatioPanel extends UIPanel {

		private BarFill barFill;

		public RatioPanel(ConfigurableRatioType type) {
			barFill = new BarFill(type.name() + "-barfill");
			addChild(barFill, 0, 0, 1, 1);
		}
	}
}
