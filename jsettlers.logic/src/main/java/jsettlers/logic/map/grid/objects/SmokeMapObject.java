package jsettlers.logic.map.grid.objects;

import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.objects.ProgressingObject;

public class SmokeMapObject extends ProgressingObject {

	private static final long serialVersionUID = -2653551420373458964L;
	private final EMapObjectType type;

	public SmokeMapObject(ShortPoint2D pos, EMapObjectType type, short duration) {
		super(pos);
		this.type = type;
		setDuration(duration/1000f);
	}

	@Override
	public EMapObjectType getObjectType() {
		return type;
	}

	@Override
	protected void changeState() {
	}

	@Override
	public boolean cutOff() {
		return false;
	}

	@Override
	public boolean canBeCut() {
		return false;
	}
}
