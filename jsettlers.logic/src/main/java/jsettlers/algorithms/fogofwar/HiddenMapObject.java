package jsettlers.algorithms.fogofwar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.buildings.IBuildingMaterial;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.mapobject.IArrowMapObject;
import jsettlers.common.mapobject.IMannaBowlObject;
import jsettlers.common.mapobject.IMapObject;
import jsettlers.common.mapobject.IStackMapObject;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.EPriority;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.IShipInConstruction;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.player.IPlayer;
import jsettlers.common.player.IPlayerable;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.selectable.ESelectionType;
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

		if(nextObject == null) return null;

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

	static class HiddenArrowMapObject extends HiddenMapObject implements IArrowMapObject {

		private static final long serialVersionUID = 1L;

		private final short sourceX;
		private final short sourceY;

		private final short targetX;
		private final short targetY;

		private final EDirection direction;

		HiddenArrowMapObject(AbstractHexMapObject original) {
			super(original);

			IArrowMapObject originalArrow = (IArrowMapObject) original;

			sourceX = originalArrow.getSourceX();
			sourceY = originalArrow.getSourceY();

			targetX = originalArrow.getTargetX();
			targetY = originalArrow.getTargetY();

			direction = originalArrow.getDirection();
		}

		@Override
		public short getSourceX() {
			return sourceX;
		}

		@Override
		public short getSourceY() {
			return sourceY;
		}

		@Override
		public short getTargetX() {
			return targetX;
		}

		@Override
		public short getTargetY() {
			return targetY;
		}

		@Override
		public EDirection getDirection() {
			return direction;
		}
	}

	public static class HiddenPlayerableMapObject extends HiddenMapObject implements IPlayerable {

		private static final long serialVersionUID = -7144155005050874615L;
		private final IPlayer player;

		public HiddenPlayerableMapObject(AbstractHexMapObject original) {
			super(original);

			player = ((IPlayerable) original).getPlayer();
		}

		@Override
		public IPlayer getPlayer() {
			return player;
		}
	}

	public static class HiddenBuilding extends HiddenPlayerableMapObject implements IBuilding, IBuilding.IMill, IBuilding.ISoundRequestable {

		private static final long serialVersionUID = 1L;

		private final EBuildingType type;
		private final ECivilisation civilisation;
		private final boolean occupied;
		private final ShortPoint2D position;

		public HiddenBuilding(AbstractHexMapObject original) {
			super(original);

			IBuilding originalBuilding = (IBuilding) original;
			type = originalBuilding.getBuildingVariant().getType();
			civilisation = originalBuilding.getBuildingVariant().getCivilisation();
			occupied = originalBuilding.isOccupied();
			position = originalBuilding.getPosition();
		}

		@Override
		public BuildingVariant getBuildingVariant() {
			return type.getVariant(civilisation);
		}

		@Override
		public EPriority getPriority() {
			return EPriority.DEFAULT;
		}

		@Override
		public EPriority[] getSupportedPriorities() {
			return new EPriority[0];
		}

		@Override
		public boolean isOccupied() {
			return occupied;
		}

		@Override
		public List<IBuildingMaterial> getMaterials() {
			return new ArrayList<>();
		}

		@Override
		public boolean cannotWork() {
			return false;
		}

		@Override
		public boolean isSoundRequested() {
			return false;
		}

		@Override
		public void requestSound() {

		}

		@Override
		public ShortPoint2D getPosition() {
			return position;
		}

		@Override
		public boolean isSelected() {
			return false;
		}

		@Override
		public void setSelected(boolean selected) {
		}

		@Override
		public ESelectionType getSelectionType() {
			return ESelectionType.BUILDING;
		}

		@Override
		public boolean isWounded() {
			return false;
		}

		@Override
		public boolean isRotating() {
			return false;
		}

		@Override
		public void setSoundPlayed() {

		}

		@Override
		public boolean isSoundPlayed() {
			return false;
		}
	}

	public static class HiddenMannaBowlObject extends HiddenMapObject implements IMannaBowlObject {

		private static final long serialVersionUID = 8521417926939943008L;
		private final ECivilisation civilisation;

		public HiddenMannaBowlObject(AbstractHexMapObject original) {
			super(original);

			this.civilisation = ((IMannaBowlObject) original).getCivilisation();
		}

		@Override
		public ECivilisation getCivilisation() {
			return civilisation;
		}
	}

	public static class HiddenShipInConstructionObject extends HiddenPlayerableMapObject implements IShipInConstruction {

		private static final long serialVersionUID = 4530464167210213660L;
		private final EDirection direction;

		public HiddenShipInConstructionObject(AbstractHexMapObject original) {
			super(original);

			IShipInConstruction shipInConstruction = (IShipInConstruction) original;

			this.direction = shipInConstruction.getDirection();
		}

		@Override
		public EDirection getDirection() {
			return direction;
		}
	}
}
