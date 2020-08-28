package jsettlers.graphics.map.controls.original.panel.content.settlers.profession;

import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.Supplier;

import go.graphics.text.EFontSize;
import jsettlers.common.action.Action;
import jsettlers.common.action.SetMoveableRatioAction;
import jsettlers.common.images.EImageLinkType;
import jsettlers.common.images.OriginalImageLink;
import jsettlers.common.map.IGraphicsGrid;
import jsettlers.common.map.partition.IPartitionSettings;
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

	private final ContentUIPanel panel;
	private final UiLocationDependingContentUpdater<IPartitionSettings> uiContentUpdater;
	private final Ref<ShortPoint2D> positionRef;
	private long lastChangeTimestamp;

	public ProfessionPanel() {
		this.positionRef = Ref.create(new ShortPoint2D(0, 0));
		this.panel = new ContentUIPanel();
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
		this.positionRef.set(position);
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
		if (System.currentTimeMillis() - lastChangeTimestamp > 5000) {
			this.panel.setup(partitionSettings.getMinBearerRatio(), partitionSettings.getMaxDiggerRatio(), partitionSettings.getMaxBricklayerRatio());
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

	public class ProfessionRatioModel {
		private float carrierRatio;
		private float diggerRatio;
		private float builderRatio;

		public ProfessionRatioModel() {
			this(0f, 0f, 0f);
		}

		public ProfessionRatioModel(float carrierRatio, float diggerRatio, float builderRatio) {
			this.carrierRatio = carrierRatio;
			this.diggerRatio = diggerRatio;
			this.builderRatio = builderRatio;
		}

		public float totalRatio() {
			return carrierRatio + diggerRatio + builderRatio;
		}

		public Ref<Float> carrierRatioRef() {
			return new Ref<>(() -> carrierRatio, ratio -> carrierRatio = ratio);
		}

		public Ref<Float> diggerRatioRef() {
			return new Ref<>(() -> diggerRatio, ratio -> diggerRatio = ratio);
		}

		public Ref<Float> builderRatioRef() {
			return new Ref<>(() -> builderRatio, ratio -> builderRatio = ratio);
		}
	}

	public class ContentUIPanel extends Panel {
		private final SettlerUIPanel diggerPanel;
		private final SettlerUIPanel carrierPanel;
		private final SettlerUIPanel builderPanel;
		private final ProfessionRatioModel model;

		public ContentUIPanel() {
			super(118f, 216f);
			this.model = new ProfessionRatioModel();
			this.carrierPanel = new SettlerUIPanel(widthInPx, EProfessionType.CARRIER, positionRef, model.carrierRatioRef());
			this.diggerPanel = new SettlerUIPanel(widthInPx, EProfessionType.DIGGER, positionRef, model.diggerRatioRef());
			this.builderPanel = new SettlerUIPanel(widthInPx, EProfessionType.BUILDER, positionRef, model.builderRatioRef());
			add(Panel.box(new Label(Labels.getString("Settlers"), EFontSize.NORMAL), widthInPx, 20f), 0f, 0f);
			add(carrierPanel, 0f, 20f);
			add(diggerPanel, 0f, 50f);
			add(builderPanel, 0f, 80f);
		}

		public void setup(float carrierRatio, float diggerRatio, float builderRatio) {
			this.carrierPanel.setup(carrierRatio);
			this.diggerPanel.setup(diggerRatio);
			this.builderPanel.setup(builderRatio);
		}

		public class SettlerUIPanel extends Panel {
			private final Label label;
			private final EProfessionType type;
			private final Ref<Float> ratioRef;

			public SettlerUIPanel(float width, EProfessionType type, Ref<ShortPoint2D> positionRef, Ref<Float> ratioRef) {
				super(width, 30f);
				this.ratioRef = ratioRef;
				this.label = new Label("...", EFontSize.NORMAL, EHorizontalAlignment.LEFT);
				this.type = type;
				add(new UpDownArrows(type, ratioRef, positionRef), 10f, 5f);
				add(Panel.box(label, width, 20f), 28f, 5f);
			}

			public void setup(float ratio) {
				this.ratioRef.set(ratio);
				this.label.setText((this.type.min ? '>' : '<') + " " + (new DecimalFormat("#").format(Math.round(ratioRef.get() * 100f))) + "% " + this.type.label);
			}

			public class UpDownArrows extends Panel {
				public UpDownArrows(EProfessionType type, Ref<Float> ratioRef, Ref<ShortPoint2D> positionRef) {
					super(12f, 18f);
					setBackground(new OriginalImageLink(EImageLinkType.GUI, 3, 231, 0));
					addChild(new Button(null) {
						@Override
						public Action getAction() {
							if (model.totalRatio() <= 1.0f - 0.05f) {
								setup(Math.min(1f, ratioRef.get() + 0.05f));
								lastChangeTimestamp = System.currentTimeMillis();
								return new SetMoveableRatioAction(type.moveableType, positionRef.get(), ratioRef.get());
							} else {
								return null;
							}
						}
					}, 0f, 0.5f, 1f, 1f);
					addChild(new Button(null) {
						@Override
						public Action getAction() {
							if (model.totalRatio() >= 0.05f) {
								setup(Math.max(0f, ratioRef.get() - 0.05f));
								lastChangeTimestamp = System.currentTimeMillis();
								return new SetMoveableRatioAction(type.moveableType, positionRef.get(), ratioRef.get());
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

	public static class Ref<T> {
		public final Supplier<T> get;
		public final Consumer<T> set;

		public Ref(Supplier<T> get, Consumer<T> set) {
			this.get = get;
			this.set = set;
		}

		public T get() {
			return get.get();
		}

		public void set(T t) {
			set.accept(t);
		}

		public static <T> Ref<T> create(T t) {
			final Object[] array = new Object[1];
			array[0] = t;
			return new Ref<T>(() -> (T) array[0], v -> array[0] = v);
		}
	}
}
