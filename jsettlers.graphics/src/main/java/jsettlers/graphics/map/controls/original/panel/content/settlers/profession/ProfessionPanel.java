package jsettlers.graphics.map.controls.original.panel.content.settlers.profession;

import java.util.List;

import go.graphics.text.EFontSize;
import jsettlers.common.action.Action;
import jsettlers.common.action.ChangeMovableSettingsAction;
import jsettlers.common.action.SetMovableLimitTypeAction;
import jsettlers.common.map.IGraphicsGrid;
import jsettlers.common.map.partition.IPartitionSettings;
import jsettlers.common.map.partition.IProfessionSettings;
import jsettlers.common.map.partition.ISingleProfessionLimit;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.graphics.action.ActionFireable;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.controls.original.panel.content.AbstractContentProvider;
import jsettlers.graphics.map.controls.original.panel.content.ESecondaryTabType;
import jsettlers.graphics.map.controls.original.panel.content.updaters.UiContentUpdater;
import jsettlers.graphics.map.controls.original.panel.content.updaters.UiLocationDependingContentUpdater;
import jsettlers.graphics.ui.Button;
import jsettlers.graphics.ui.CountArrows;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.Label.EHorizontalAlignment;
import jsettlers.graphics.ui.LabeledButton;
import jsettlers.graphics.ui.UIElement;
import jsettlers.graphics.ui.UIPanel;

public class ProfessionPanel extends AbstractContentProvider implements UiContentUpdater.IUiContentReceiver<IPartitionSettings> {

	private final ContentPanel panel;
	private final UiLocationDependingContentUpdater<IPartitionSettings> uiContentUpdater;

	public ProfessionPanel() {
		this.panel = new ContentPanel();
		this.uiContentUpdater = new UiLocationDependingContentUpdater<>(ProfessionPanel::currentDistributionSettingsProvider);
		this.uiContentUpdater.addListener(this);
	}

	@Override
	public UIPanel getPanel() {
		return panel;
	}

	@Override
	public ESecondaryTabType getTabs() {
		return ESecondaryTabType.SETTLERS;
	}

	@Override
	public void showMapPosition(ShortPoint2D position, IGraphicsGrid grid) {
		this.panel.setPosition(position);
		super.showMapPosition(position, grid);
		this.uiContentUpdater.updatePosition(grid, position);
	}

	@Override
	public void contentShowing(ActionFireable actionFireable) {
		super.contentShowing(actionFireable);
		this.uiContentUpdater.start();
	}
	
	@Override
	public void contentHiding(ActionFireable actionFireable, AbstractContentProvider nextContent) {
		super.contentHiding(actionFireable, nextContent);
		this.uiContentUpdater.stop();
	}

	@Override
	public void update(IPartitionSettings partitionSettings) {
		if (partitionSettings != null) {
			this.panel.setup(partitionSettings.getProfessionSettings());
		}
	}

	public void setPlayer(IInGamePlayer player) {
		uiContentUpdater.setPlayer(player);
	}

	private static IPartitionSettings currentDistributionSettingsProvider(IGraphicsGrid grid, ShortPoint2D position) {
		IPlayer player = grid.getPlayerAt(position.x, position.y);
		return (player != null && player.getPlayerId() >= 0) ? grid.getPartitionData(position.x, position.y).getPartitionSettings() : null;
	}

	public class ContentPanel extends Panel {
		private ShortPoint2D position;
		private final SettlerPanel diggerPanel;
		private final SettlerPanel carrierPanel;
		private final SettlerPanel builderPanel;

		public ContentPanel() {
			super(118f, 216f);
			this.position = new ShortPoint2D(0, 0);
			this.carrierPanel = new SettlerPanel(widthInPx, EMovableType.BEARER, true);
			this.diggerPanel = new SettlerPanel(widthInPx, EMovableType.DIGGER, false);
			this.builderPanel = new SettlerPanel(widthInPx, EMovableType.BRICKLAYER, false);

			add(Panel.box(new Label(Labels.getString("settler_profession_title"), EFontSize.NORMAL), widthInPx, 20f), 0f, 0f);
			add(carrierPanel, 0f, 20f);
			add(diggerPanel, 0f, 60f);
			add(builderPanel, 0f, 100f);
		}

		public void setup(IProfessionSettings settings) {
			carrierPanel.setValue(settings);
			diggerPanel.setValue(settings);
			builderPanel.setValue(settings);

			update();
		}

		public void setPosition(ShortPoint2D position) {
			this.position = position;
		}

		public class SettlerPanel extends Panel {
			private ISingleProfessionLimit data;
			private final Label descLabel;
			private final Button percentButton;
			private final Label targetLabel;
			private final Label currentLabel;
			private final EMovableType type;
			private final boolean min;

			public SettlerPanel(float width, EMovableType type, boolean min) {
				super(width, 30f);
				data = null;
				descLabel = new Label(Labels.getName(type, true), EFontSize.NORMAL, EHorizontalAlignment.LEFT);
				percentButton = new LabeledButton("%", null) {
					@Override
					public boolean isActive() {
						return data != null && data.isRelative();
					}

					@Override
					protected String getText() {
						if(isActive()) {
							return Labels.getString("settler_profession_relative");
						} else {
							return Labels.getString("settler_profession_absolute");
						}
					}

					@Override
					public Action getAction() {
						return new SetMovableLimitTypeAction(position, type, !isActive());
					}
				};
				targetLabel = new Label("...", EFontSize.NORMAL, EHorizontalAlignment.RIGHT);
				currentLabel = new Label("...", EFontSize.NORMAL, EHorizontalAlignment.LEFT);
				this.type = type;
				this.min = min;


				add(Panel.box(targetLabel, 20f, 20f), 10f, 5f);
				add(Panel.box(new CountArrows(
						() -> new ChangeMovableSettingsAction(type, true, 5, position),
						() -> new ChangeMovableSettingsAction(type, true, -5, position)
				), 12f, 18f), 30, 5f);
				add(Panel.box(descLabel, 30, 20f), 42f, 5f);
				add(Panel.box(percentButton, 40, 20), 77, 5);
				add(Panel.box(currentLabel, width, 20f), 10f, 25f);
			}

			@Override
			public void update() {
				super.update();

				if(data != null) {
					targetLabel.setText((min ? '>' : '<') + (data.isRelative() ? (int)(data.getTargetRatio()*100) + "%" : data.getTargetCount() + ""));

					currentLabel.setText(Labels.getString("settler_profession_currently", data.getCurrentCount(), data.getCurrentRatio()*100));
				}
			}

			public void setValue(IProfessionSettings settings) {
				data = settings.getSettings(type);
			}
		}
	}

	public static class Panel extends UIPanel {
		final float widthInPx;
		final float heightInPx;

		public Panel(float widthInPx, float heightInPx) {
			this.widthInPx = widthInPx;
			this.heightInPx = heightInPx;
		}

		public void update() {
			List<UIElement> children = getChildren();
			for (UIElement element : children) {
				if (element instanceof Panel) {
					Panel panel = (Panel) element;
					panel.update();
				}
			}
		}

		public void add(Panel panel, float x, float y) {
			addChild(panel, x(x), 1f - y(y) - y(panel.heightInPx), x(x) + x(panel.widthInPx), 1f - y(y));
		}

		public float x(float px) {
			return px / widthInPx;
		}

		public float y(float px) {
			return px / heightInPx;
		}

		public static Panel box(UIElement element, float width, float height) {
			Panel panel = new Panel(width, height);
			panel.addChild(element, 0f, 0f, 1f, 1f);
			return panel;
		}
	}
}
