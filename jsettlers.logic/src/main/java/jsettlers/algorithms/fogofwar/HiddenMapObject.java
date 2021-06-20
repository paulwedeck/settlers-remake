package jsettlers.algorithms.fogofwar;

import java.io.Serializable;

import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.mapobject.IMapObject;
import jsettlers.common.mapobject.IStackMapObject;
import jsettlers.common.material.EMaterialType;
import jsettlers.logic.map.grid.objects.AbstractHexMapObject;

public class HiddenMapObject implements IMapObject, Serializable {

	private static final long serialVersionUID = 1L;

	private final float progress;
	private final EMapObjectType type;

	private IMapObject nextObject;

	public HiddenMapObject(AbstractHexMapObject original) {
		this.progress = original.getStateProgress();
		this.type = original.getObjectType();
	}

	@Override
	public EMapObjectType getObjectType() {
		return type;
	}

	@Override
	public float getStateProgress() {
		return progress;
	}

	@Override
	public IMapObject getNextObject() {
		return nextObject;
	}

	public void setNextObject(IMapObject nextObject) {
		this.nextObject = nextObject;
	}

	@Override
	public IMapObject getMapObject(EMapObjectType type) {
		if(this.type.equals(type)) return this;

		return nextObject.getMapObject(type);
	}

	static class HiddenStackMapObject extends HiddenMapObject implements IStackMapObject {

		private static final long serialVersionUID = 1L;

		private final EMaterialType type;
		private final byte size;

		public HiddenStackMapObject(AbstractHexMapObject original) {
			super(original);
			IStackMapObject originalStack = (IStackMapObject) original;
			this.type = originalStack.getMaterialType();
			this.size = originalStack.getSize();
		}

		@Override
		public EMaterialType getMaterialType() {
			return type;
		}

		@Override
		public byte getSize() {
			return size;
		}
	}
}
