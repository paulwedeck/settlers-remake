package jsettlers.graphics.map.controls.original.panel.content.settlers.profession;

import java.text.MessageFormat;
import java.util.List;

import go.graphics.text.EFontSize;
import jsettlers.common.action.Action;
import jsettlers.common.action.SetMoveableRatioAction;
import jsettlers.common.images.EImageLinkType;
import jsettlers.common.images.OriginalImageLink;
import jsettlers.common.map.IGraphicsGrid;
import jsettlers.common.map.partition.IPartitionSettings;
import jsettlers.common.map.partition.IProfessionSettings;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.graphics.action.ActionFireable;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.controls.original.panel.content.AbstractContentProvider;
import jsettlers.graphics.map.controls.original.panel.content.ESecondaryTabType;
import jsettlers.graphics.map.controls.original.panel.content.updaters.UiContentUpdater;
import jsettlers.graphics.map.controls.original.panel.content.updaters.UiLocationDependingContentUpdater;
import jsettlers.graphics.ui.Button;
import jsettlers.graphics.ui.Label;
import jsettlers.graphics.ui.Label.EHorizontalAlignment;
import jsettlers.graphics.ui.UIElement;
import jsettlers.graphics.ui.UIPanel;

public class ProfessionPanel extends AbstractContentProvider implements UiContentUpdater.IUiContentReceiver<IPartitionSettings> {

	private final ContentPanel panel;
	private final UiLocationDependingContentUpdater<IPartitionSettings> uiContentUpdater;
	private long lastChangeTimestamp;

	public ProfessionPanel() {
		this.panel = new ContentPanel();
		this.uiContentUpdater = new UiLocationDependingContentUpdater<>(ProfessionPanel::currentDistributionSettingsProvider);
		this.uiContentUpdater.addListener(this);
		this.lastChangeTimestamp = 0;
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
	public void update(IPartitionSettings partitionSettings) {
		// Use delay to prevent race condition between direct ui update and settings update
		if (System.currentTimeMillis() - lastChangeTimestamp > 5000) {
			IProfessionSettings professionSettings = partitionSettings.getProfessionSettings();
			this.panel.setup(professionSettings);
		}
	}

	private static IPartitionSettings currentDistributionSettingsProvider(IGraphicsGrid grid, ShortPoint2D position) {
		return grid.getPlayerIdAt(position.x, position.y) >= 0 ? grid.getPartitionData(position.x, position.y).getPartitionSettings() : null;
	}

	public enum EProfessionType {
		CARRIER("Carriers", true, EMovableType.BEARER),
		DIGGER("Diggers", false, EMovableType.DIGGER),
		BUILDER("Builders", false, EMovableType.BRICKLAYER);

		public final String label;
		public final boolean min;
		public final EMovableType moveableType;

		private EProfessionType(String label, boolean min, EMovableType movableType) {
			this.label = label;
			this.min = min;
			this.moveableType = movableType;
		}
	}

	public class ContentPanel extends Panel {
		private ShortPoint2D position;
		private final SettlerPanel diggerPanel;
		private final SettlerPanel carrierPanel;
		private final SettlerPanel builderPanel;

		public ContentPanel() {
			super(118f, 216f);
			this.position = new ShortPoint2D(0, 0);
			this.carrierPanel = new SettlerPanel(widthInPx, EProfessionType.CARRIER);
			this.diggerPanel = new SettlerPanel(widthInPx, EProfessionType.DIGGER);
			this.builderPanel = new SettlerPanel(widthInPx, EProfessionType.BUILDER);

			add(Panel.box(new Label(Labels.getString("Settlers"), EFontSize.NORMAL), widthInPx, 20f), 0f, 0f);
			add(carrierPanel, 0f, 20f);
			add(diggerPanel, 0f, 50f);
			add(builderPanel, 0f, 80f);
		}

		public void setup(IProfessionSettings settings) {
			this.carrierPanel.setRatio(settings.getMinBearerRatio());
			this.carrierPanel.setCurrentRatio(settings.getCurrentBearerRatio());

			this.diggerPanel.setRatio(settings.getMaxDiggerRatio());
			this.diggerPanel.setCurrentRatio(settings.getCurrentDiggerRatio());

			this.builderPanel.setRatio(settings.getMaxBricklayerRatio());
			this.builderPanel.setCurrentRatio(settings.getCurrentBricklayerRatio());

			update();
		}

		public float getTotalRatio() {
			return carrierPanel.ratio + diggerPanel.ratio + builderPanel.ratio;
		}

		public void setPosition(ShortPoint2D position) {
			this.position = position;
		}

		public class SettlerPanel extends Panel {
			private float ratio;
			private float currentRatio;
			private final Label label;
			private final EProfessionType type;

			public SettlerPanel(float width, EProfessionType type) {
				super(width, 30f);
				this.ratio = 0f;
				this.currentRatio = 0f;
				this.label = new Label("...", EFontSize.NORMAL, EHorizontalAlignment.LEFT);
				this.type = type;

				add(new UpDownArrows(type), 10f, 5f);
				add(Panel.box(label, width, 20f), 28f, 5f);
			}

			@Override
			public void update() {
				super.update();
				this.label.setText(MessageFormat.format("{0} {1} {2} ({3})", (this.type.min ? '>' : '<'), formatPercentage(ratio), this.type.label, formatPercentage(currentRatio)));
			}

			private String formatPercentage(float value) {
				return (int)(value * 100f) + "%";
			}

			public void setRatio(float ratio) {
				this.ratio = ratio;
			}

			public void setCurrentRatio(float currentRatio) {
				this.currentRatio = currentRatio;
			}

			public class UpDownArrows extends Panel {
				public UpDownArrows(EProfessionType type) {
					super(12f, 18f);
					setBackground(new OriginalImageLink(EImageLinkType.GUI, 3, 231, 0));
					addChild(new Button(null) {
						@Override
						public Action getAction() {
							if (getTotalRatio() <= 1.0f - 0.05f) {
								setRatio(Math.min(1f, ratio + 0.05f));
								lastChangeTimestamp = System.currentTimeMillis();
								SettlerPanel.this.update();
								return new SetMoveableRatioAction(type.moveableType, position, ratio);
							} else {
								return null;
							}
						}
					}, 0f, 0.5f, 1f, 1f);
					addChild(new Button(null) {
						@Override
						public Action getAction() {
							if (getTotalRatio() >= 0.05f) {
								setRatio(Math.max(0f, ratio - 0.05f));
								lastChangeTimestamp = System.currentTimeMillis();
								SettlerPanel.this.update();
								return new SetMoveableRatioAction(type.moveableType, position, ratio);
							} else {
								return null;
							}
						}
					}, 0f, 0f, 1f, 0.5f);
				}
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
