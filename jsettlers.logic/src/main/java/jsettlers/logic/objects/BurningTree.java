package jsettlers.logic.objects;

import java.util.function.Consumer;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.coordinates.CoordinateStream;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.objects.IMapObjectsManagerGrid;
import jsettlers.logic.map.grid.objects.MapObjectsManager;

public class BurningTree extends ProgressingObject {
	private static final long serialVersionUID = 582760927711297568L;

	public static final int FIRE_TICK_INTERVAL = 2;
	public static final int FIRE_TICK_COUNT = 3;

	public static final int MUD_RADIUS = 3;
	public static final int FIRE_SPREAD_RADIUS = 6;
	public static final float FIRE_SPREAD_OUTER_CHANCE = 0.3f;
	public static final float FIRE_SPREAD_INNER_CHANCE = 0.6f;
	public static final float DELTA_CHANCE = FIRE_SPREAD_INNER_CHANCE - FIRE_SPREAD_OUTER_CHANCE;

	private final Consumer<ShortPoint2D> spreadFunc;

	public BurningTree(ShortPoint2D pos, Consumer<ShortPoint2D> spreadFunc) {
		super(pos);
		this.spreadFunc = spreadFunc;
	}

	private void spreadTo(int x, int y) {
		ShortPoint2D pos = new ShortPoint2D(x, y);
		float dist = pos.getOnGridDistTo(getX(), getY());
		if(MatchConstants.random().nextFloat() <= FIRE_SPREAD_OUTER_CHANCE + (dist/FIRE_SPREAD_RADIUS)*DELTA_CHANCE) {
			spreadFunc.accept(pos);
		}
	}

	@Override
	protected void handlePlacement(int x, int y, MapObjectsManager mapObjectsManager, IMapObjectsManagerGrid grid) {
		CoordinateStream area = MapCircle.stream(x, y, MUD_RADIUS);
		area.forEach((x1, y1) -> grid.setLandscape(x1, y1, ELandscapeType.MUDBORDEROUTER));
		area.forEach((x1, y1) -> grid.setLandscape(x1, y1, ELandscapeType.MUDBORDER));
		area.forEach((x1, y1) -> grid.setLandscape(x1, y1, ELandscapeType.MUD));
	}

	@Override
	protected void changeState() {
		MapCircle.stream(new ShortPoint2D(getX(), getY()), FIRE_SPREAD_RADIUS).forEach(this::spreadTo);
	}

	@Override
	public boolean cutOff() {
		return false;
	}

	@Override
	public boolean canBeCut() {
		return false;
	}

	@Override
	public EMapObjectType getObjectType() {
		return EMapObjectType.TREE_BURNING;
	}
}
