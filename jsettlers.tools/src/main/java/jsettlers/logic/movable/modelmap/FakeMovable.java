package jsettlers.logic.movable.modelmap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import jsettlers.common.buildings.IBuilding;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsBuildingWorker;
import jsettlers.common.movable.IGraphicsCargoShip;
import jsettlers.common.movable.IGraphicsFerry;
import jsettlers.common.movable.IGraphicsMovable;
import jsettlers.common.movable.IGraphicsThief;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.selectable.ESelectionType;

public class FakeMovable implements IGraphicsMovable, IGraphicsBuildingWorker, IGraphicsThief, IGraphicsFerry,
		IGraphicsCargoShip {

	private EMovableType movableType;
	private EMovableAction movableAction;
	private EMovingDirection direction;
	private EMaterialType materialType;
	private final Supplier<Long> currentTime;
	private final FakePlayer player;
	private final MovableModelMap map;
	private ShortPoint2D position;

	private final List<IGraphicsMovable> passengers = new ArrayList<>();

	public FakeMovable(Supplier<Long> currentTime, MovableModelMap map, ShortPoint2D position) {
		movableType = EMovableType.BEARER;
		movableAction = EMovableAction.NO_ACTION;
		direction = EMovingDirection.NORTH_WEST;
		materialType = EMaterialType.NO_MATERIAL;
		this.currentTime = currentTime;
		player = new FakePlayer();
		this.map = map;
		setPosition(position);
	}

	@Override
	public EMovableType getMovableType() {
		return movableType;
	}

	public void setMovableType(EMovableType movableType) {
		this.movableType = movableType;
	}

	@Override
	public EMovableAction getAction() {
		return movableAction;
	}

	public void setMovableAction(EMovableAction movableAction) {
		this.movableAction = movableAction;
	}

	public void setDirection(EMovingDirection direction) {
		this.direction = direction;
	}

	@Override
	public float getMoveProgress() {
		// (currentTime.get() % 1000000f)/1000000f
		return (float) (Math.sin((currentTime.get())/1000000f)+1)/2f;
	}

	@Override
	public EDirection getDirection() {
		EDirection realDirection = direction.getRealDirection();

		if (realDirection != null) return realDirection;

		return EDirection.VALUES[(int)((currentTime.get())/1000000f/Math.PI/4) % EDirection.VALUES.length];
	}

		@Override
	public EMaterialType getMaterial() {
		return materialType;
	}

	public void setMaterialType(EMaterialType materialType) {
		this.materialType = materialType;
	}

	@Override
	public float getHealth() {
		return 100;
	}

	@Override
	public boolean isAlive() {
		return true;
	}

	@Override
	public boolean isRightstep() {
		return false;
	}

	@Override
	public boolean hasEffect(EEffectType effect) {
		return false;
	}

	@Override
	public int getID() {
		return 0;
	}

	@Override
	public FakePlayer getPlayer() {
		return player;
	}

	@Override
	public ShortPoint2D getPosition() {
		return position;
	}

	public void setPosition(ShortPoint2D position) {
		if(this.position != null) {
			map.setMovableAt(this.position.x, this.position.y, null);
		}

		if(position != null) {
			map.setMovableAt(position.x, position.y, this);
		}

		this.position = position;
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
		return null;
	}

	@Override
	public boolean isWounded() {
		return false;
	}

	@Override
	public void setSoundPlayed() {

	}

	@Override
	public boolean isSoundPlayed() {
		return true;
	}

	@Override
	public IBuilding getGarrisonedBuilding() {
		return null;
	}

	@Override
	public List<? extends IGraphicsMovable> getPassengers() {
		return passengers;
	}

	@Override
	public boolean isUncoveredBy(byte teamId) {
		return true;
	}

	@Override
	public int getNumberOfCargoStacks() {
		return 0;
	}

	@Override
	public EMaterialType getCargoType(int stack) {
		return EMaterialType.STONE;
	}

	@Override
	public int getCargoCount(int stack) {
		return 0;
	}
}
