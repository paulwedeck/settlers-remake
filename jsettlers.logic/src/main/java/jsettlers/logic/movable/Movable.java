/*******************************************************************************
 * Copyright (c) 2015 - 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.logic.movable;

import jsettlers.algorithms.fogofwar.FoWTask;
import jsettlers.algorithms.path.Path;
import jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper;
import jsettlers.algorithms.simplebehaviortree.IBooleanConditionFunction;
import jsettlers.algorithms.simplebehaviortree.IEDirectionSupplier;
import jsettlers.algorithms.simplebehaviortree.IEMaterialTypeSupplier;
import jsettlers.algorithms.simplebehaviortree.IShortPoint2DSupplier;
import jsettlers.algorithms.simplebehaviortree.IShortSupplier;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.NodeStatus;
import jsettlers.algorithms.simplebehaviortree.Tick;
import jsettlers.algorithms.simplebehaviortree.nodes.Guard;
import jsettlers.common.CommonConstants;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.selectable.ESelectionType;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.cargo.CargoShipMovable;
import jsettlers.logic.movable.cargo.DonkeyMovable;
import jsettlers.logic.movable.civilian.AlchemistMovable;
import jsettlers.logic.movable.civilian.BakerMovable;
import jsettlers.logic.movable.civilian.BearerMovable;
import jsettlers.logic.movable.civilian.BricklayerMovable;
import jsettlers.logic.movable.civilian.DiggerMovable;
import jsettlers.logic.movable.civilian.DonkeyFarmerMovable;
import jsettlers.logic.movable.civilian.HealerMovable;
import jsettlers.logic.movable.civilian.MelterMovable;
import jsettlers.logic.movable.civilian.MinerMovable;
import jsettlers.logic.movable.civilian.PigFarmerMovable;
import jsettlers.logic.movable.civilian.SawMillerMovable;
import jsettlers.logic.movable.civilian.SimpleBuildingWorkerMovable;
import jsettlers.logic.movable.civilian.SmithMovable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.movable.military.BowmanMovable;
import jsettlers.logic.movable.military.InfantryMovable;
import jsettlers.logic.movable.military.MageMovable;
import jsettlers.logic.movable.other.FerryMovable;
import jsettlers.logic.movable.specialist.GeologistMovable;
import jsettlers.logic.movable.specialist.PioneerMovable;
import jsettlers.logic.movable.specialist.ThiefMovable;
import jsettlers.logic.player.Player;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

/**
 * Central Movable class of JSettlers.
 *
 * @author Andreas Eberle
 */
public abstract class Movable implements ILogicMovable, FoWTask {
	private static final long serialVersionUID = -705947810059935866L;

	private static final int SHIP_PUSH_DISTANCE = 10;
	private static final int BEHAVIOUR_RETRY_COUNT = 3;

	protected final AbstractMovableGrid grid;
	private final     int                 id;
	protected final   Player              player;

	private EMovableState state = EMovableState.ACTIVE;

	private final EMovableType    movableType;

	private EMaterialType  materialType  = EMaterialType.NO_MATERIAL;
	private EMovableAction movableAction = EMovableAction.NO_ACTION;
	private EDirection     direction;
	private final Map<EEffectType, Integer> effectEnd = new EnumMap<>(EEffectType.class);

	private int   animationStartTime;
	private short animationDuration;

	public transient ShortPoint2D oldFowPosition = null;
	protected ShortPoint2D position;

	protected Path path;

	protected float         health;
	private boolean       visible           = true;
	private ShortPoint2D pushedFrom;

	private boolean isRightstep = false;
	protected int     flockDelay  = 700;

	private transient boolean selected    = false;
	private transient boolean soundPlayed = false;

	protected boolean playerControlled;

	private boolean leavePosition = false;

	private transient Tick<? extends Movable> tick;

	protected Movable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		this.grid = grid;
		this.position = position;
		this.player = player;
		this.movableType = movableType;

		this.tick = new Tick<>(this, MovableManager.getBehaviourFor(movableType));

		if(replace != null) {
			this.health = replace.getHealth()/replace.getMovableType().getHealth()*movableType.getHealth();
			this.direction = replace.getDirection();
			if(movableType == replace.movableType) {
				this.materialType = replace.materialType;
			}
		} else {
			this.health = movableType.getHealth();
			this.direction = EDirection.VALUES[MatchConstants.random().nextInt(EDirection.NUMBER_OF_DIRECTIONS)];
		}

		playerControlled = movableType.playerControllable;

		this.id = MovableManager.requestId(this, replace);
	}

	/**
	 * Tests if this movable can receive moveTo requests and if so, directs it to go to the given position.
	 *
	 * @param targetPosition
	 * 		Desired position the movable should move to
	 */
	@Override
	public void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType) {
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();

		EMovableType type = (EMovableType) ois.readObject();
		tick = Tick.deserialize(ois, this, MovableManager.getBehaviourFor(type));
	}

	private void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();

		oos.writeObject(movableType);
		tick.serialize(oos);
	}

	public void leavePosition() {
		if (isBusy()) {
			return;
		}

		leavePosition = true;
	}

	protected static <T extends Movable> Node<T> setDirectionNode(EDirection direction) {
		return action(mov -> mov.setDirection(direction));
	}

	protected static <T extends Movable> Node<T> setDirectionNode(IEDirectionSupplier<T> direction) {
		return action(mov -> mov.setDirection(direction.apply(mov)));
	}

	protected static <T extends Movable> Node<T> setMaterialNode(EMaterialType material) {
		return action(mov -> mov.setMaterial(material));
	}

	protected static <T extends Movable> Node<T> setMaterial(IEMaterialTypeSupplier<T> material) {
		return action(mov -> mov.setMaterial(material.apply(mov)));
	}

	protected static <T extends Movable> Node<T> hide() {
		return action(mov -> mov.setVisible(false));
	}

	protected static <T extends Movable> Node<T> show() {
		return action(mov -> mov.setVisible(true));
	}

	protected static <T extends Movable> Node<T> crouchDown(Node<T> child) {
		return sequence(
				playAction(EMovableAction.BEND_DOWN, Constants.MOVABLE_BEND_DURATION),
				child,
				playAction(EMovableAction.RAISE_UP, Constants.MOVABLE_BEND_DURATION)
		);
	}

	protected static <T extends Movable> Node<T> drop(IEMaterialTypeSupplier<T> materialType, boolean offerMaterial) {
		return crouchDown(
				action(mov -> {
					EMaterialType takeDropMaterial = materialType.apply(mov);

					if (takeDropMaterial == null || !takeDropMaterial.isDroppable()) return;

					mov.setMaterial(EMaterialType.NO_MATERIAL);
					mov.grid.dropMaterial(mov.position, takeDropMaterial, offerMaterial, false);
				})
		);
	}

	protected static <T extends Movable> Node<T> take(IEMaterialTypeSupplier<T> materialType, boolean fromMap) {
		return sequence(
				condition(mov -> !fromMap || mov.grid.canTakeMaterial(mov.position, materialType.apply(mov))),
				crouchDown(
					action(mov -> {
						EMaterialType material = materialType.apply(mov);
						mov.grid.takeMaterial(mov.position, material);
						mov.setMaterial(material);
					})
				)
		);
	}

	protected static <T extends Movable> Node<T> goInDirectionIfAllowedAndFreeNode(IEDirectionSupplier<T> direction) {
		return sequence(
				condition(mov -> {
					ShortPoint2D targetPosition = direction.apply(mov).getNextHexPoint(mov.getPosition());

					if ((mov.grid.isValidPosition(mov, targetPosition.x, targetPosition.y) && mov.grid.hasNoMovableAt(targetPosition.x, targetPosition.y))) {
						mov.path = new Path(targetPosition);
						return true;
					}
					return false;
				}),
				followPath(mov -> true)
		);
	}

	protected static <T extends Movable> Node<T> goInDirectionIfFree(IEDirectionSupplier<T> direction) {
		return sequence(
				condition(mov -> {
					EDirection realDirection = direction.apply(mov);

					ShortPoint2D targetPosition = realDirection.getNextHexPoint(mov.position);
					if(mov.grid.isFreePosition(targetPosition.x, targetPosition.y)) {
						mov.setDirection(realDirection);
						return true;
					} else {
						return false;
					}
				}),
				goSingleStep(mov -> mov.getDirection().getNextHexPoint(mov.position))
		);
	}

	protected static <T extends Movable> Node<T> goInDirectionWaitFree(EDirection direction) {
		return sequence(
				action(mov -> {
					mov.setDirection(direction);
					mov.path = new Path(direction.getNextHexPoint(mov.getPosition()));
				}),
				followPath(mov -> true)
		);
	}

	private ShortPoint2D markedTarget = null;

	protected static <T extends Movable> Node<T> markDuring(IShortPoint2DSupplier<T> markPosition, Node<T> child) {
		return resetAfter(mov -> {
					Movable mmov = mov;
					mmov.grid.setMarked(mmov.markedTarget, false);
					mmov.markedTarget = null;
				},
				sequence(
						action(mov -> {
							Movable mmov = mov;
							mmov.markedTarget = markPosition.apply(mov);
							mmov.grid.setMarked(mmov.markedTarget, true);
						}),
						child
				)
		);
	}

	protected static <T extends Movable> Node<T> followPresearchedPathMarkTarget(IBooleanConditionFunction<T> pathStep) {
		return markDuring(mov -> mov.path.getTargetPosition(), followPresearchedPath(pathStep));
	}

	protected static <T extends Movable> Node<T> followPresearchedPath() {
		return followPresearchedPath(mov -> true);
	}

	protected static <T extends Movable> Node<T> followPresearchedPath(IBooleanConditionFunction<T> pathStep) {
		return sequence(
				action(mov -> {
					assert mov.path != null : "path must be non-null to be able to followPresearchedPath()!";
				}),
				followPath(pathStep)

		);
	}

	/**
	 *
	 * @param duration
	 * 			duration in milliseconds
	 */
	protected static <T extends Movable> Node<T> playAction(EMovableAction action, short duration) {
		return playAction(action, mov -> duration);
	}

	/**
	 *
	 * @param duration
	 * 			duration in milliseconds
	 */
	protected static <T extends Movable> Node<T> playAction(EMovableAction action, IShortSupplier<T> duration) {
		return resetAfter(mov -> ((Movable)mov).movableAction = EMovableAction.NO_ACTION,
				sequence(
					action(mov -> {
						Movable realMov = mov;

						realMov.playAnimation(action, duration.apply(mov));

						realMov.soundPlayed = false;
					}),
					sleep(mov -> (int)((Movable)mov).animationDuration)
			)
		);
	}
	protected static <T extends Movable> Node<T> goToPos(IShortPoint2DSupplier<T> target) {
		return goToPos(target, mov -> true);
	}

	protected static <T extends Movable> Node<T> goToPos(IShortPoint2DSupplier<T> target, IBooleanConditionFunction<T> pathStep) {
		return sequence(
				condition(mov -> {
					mov.path = mov.grid.calculatePathTo(mov, target.apply(mov));
					return mov.path != null;
				}),
				followPath(pathStep)
		);
	}


	protected void findWayAroundObstacle() {
		if (!path.hasOverNextStep()) { // if path has no position left
			return;
		}

		EDirection direction = EDirection.getApproxDirection(position, path.getOverNextPos());

		EDirection rightDir = direction.getNeighbor(-1);
		EDirection leftDir = direction.getNeighbor(1);

		ShortPoint2D straightPos = direction.getNextHexPoint(position);
		ShortPoint2D twoStraightPos = direction.getNextHexPoint(position, 2);

		ShortPoint2D rightPos = rightDir.getNextHexPoint(position);
		ShortPoint2D rightStraightPos = direction.getNextHexPoint(rightPos);
		ShortPoint2D straightRightPos = rightDir.getNextHexPoint(straightPos);

		ShortPoint2D leftPos = leftDir.getNextHexPoint(position);
		ShortPoint2D leftStraightPos = direction.getNextHexPoint(leftPos);
		ShortPoint2D straightLeftPos = leftDir.getNextHexPoint(straightPos);

		ShortPoint2D overNextPos = path.getOverNextPos();

		LinkedList<ShortPoint2D[]> possiblePaths = new LinkedList<>();

		if (twoStraightPos.equals(overNextPos)) {
			if (grid.isValidPosition(this, rightPos.x, rightPos.y) && grid.isValidPosition(this, rightStraightPos.x, rightStraightPos.y)) {
				possiblePaths.add(new ShortPoint2D[]{
						rightPos,
						rightStraightPos});
			} else if (grid.isValidPosition(this, leftPos.x, leftPos.y) && grid.isValidPosition(this, leftStraightPos.x, leftStraightPos.y)) {
				possiblePaths.add(new ShortPoint2D[]{
						leftPos,
						leftStraightPos});
			} else {
				// TODO @Andreas Eberle maybe calculate a new path
			}
		}

		if (rightStraightPos.equals(overNextPos) && grid.isValidPosition(this, rightPos.x, rightPos.y)) {
			possiblePaths.add(new ShortPoint2D[]{rightPos});
		}
		if (leftStraightPos.equals(overNextPos) && grid.isValidPosition(this, leftPos.x, leftPos.y)) {
			possiblePaths.add(new ShortPoint2D[]{leftPos});
		}

		if ((straightRightPos.equals(overNextPos) || straightLeftPos.equals(overNextPos))
				&& grid.isValidPosition(this, straightPos.x, straightPos.y) && grid.hasNoMovableAt(straightPos.x, straightPos.y)) {
			possiblePaths.add(new ShortPoint2D[]{straightPos});

		} else {
			// TODO @Andreas Eberle maybe calculate a new path
		}

		// try to find a way without a movable or with a pushable movable.
		for (ShortPoint2D[] pathPrefix : possiblePaths) { // check if any of the paths is free of movables
			ShortPoint2D firstPosition = pathPrefix[0];
			ILogicMovable movable = grid.getMovableAt(firstPosition.x, firstPosition.y);
			if (movable == null) {
				path.goToNextStep();
				path = new Path(path, pathPrefix);
			}
		}
	}

	@Override
	public int timerEvent() {
		if (!isAlive()) {
			return -1;
		}

		NodeStatus status = NodeStatus.SUCCESS;

		// continue behaviour if the previous run was successful
		for(int i = 0; i < BEHAVIOUR_RETRY_COUNT && status == NodeStatus.SUCCESS && state == EMovableState.ACTIVE; i++) {
			tick.root.setInvocationDelay(0);
			status = tick.tick();
		}

		leavePosition = false;

		int delay = tick.root.getInvocationDelay();
		if(delay < Constants.MOVABLE_INTERRUPT_PERIOD) {
			return Constants.MOVABLE_INTERRUPT_PERIOD;
		}

		return delay;
	}

	private void pushShips() {
		if(!isShip()) return;

		HexGridArea.stream(position.x, position.y, 1, SHIP_PUSH_DISTANCE)
				   .filterBounds(grid.getWidth(), grid.getHeight())
				   .forEach((x, y) -> {
					   ILogicMovable blockingMovable = grid.getMovableAt(x, y);
					   if (blockingMovable != null && blockingMovable.isShip()) {
						   //blockingMovable.push(this);
					   }
				   });
	}

	@Override
	public ShortPoint2D getPosition() {
		return this.position;
	}

	@Override
	public int getViewDistance() {
		return movableType.getViewDistance();
	}

	@Override
	public boolean continueFoW() {
		return state != EMovableState.DEAD;
	}

	@Override
	public ShortPoint2D getFoWPosition() {
		return position;
	}

	@Override
	public ShortPoint2D getOldFoWPosition() {
		return oldFowPosition;
	}

	@Override
	public void setOldFoWPosition(ShortPoint2D position) {
		oldFowPosition = position;
	}

	private static <T extends Movable> Node<T> goSingleStep(IShortPoint2DSupplier<T> target) {
		return sequence(
			action(mov -> {
				Movable realMov = mov;
				ShortPoint2D targetPosition = target.apply(mov);

				mov.setDirection(EDirection.getApproxDirection(mov.position, targetPosition));
				mov.grid.leavePosition(mov.position, mov);
				mov.grid.enterPosition(targetPosition, mov, false);
				realMov.position = targetPosition;
				realMov.isRightstep = !realMov.isRightstep;

			}),
			playAction(EMovableAction.WALKING, mov -> mov.getMovableType().getStepDurationMs())
		);
	}

	protected static <T extends Movable> Guard<T> doingNothingGuard() {
		return guard(mov -> true, doingNothingAction());
	}

	protected static <T extends Movable> Node<T> doingNothingAction() {
		return selector(
				idleAction(),
				sequence(
					action(mov -> {
						int turnDirection = MatchConstants.random().nextInt(-8, 8);
						if (Math.abs(turnDirection) <= 1) {
							mov.lookInDirection(mov.getDirection().getNeighbor(turnDirection));
						}
					}),
					sleep(mov -> mov.flockDelay)
				)
		);
	}

	protected static <T extends Movable> Node<T> idleAction() {
		return selector(
				sequence(
					condition(mov -> ((Movable) mov).pushedFrom != null),
					selector(
						// we should either move out of his way
						sequence(
							condition(Movable::setRandomFreeDirection),
							goInDirectionIfAllowedAndFreeNode(Movable::getDirection)
						),
						// or switch positions
						sequence(
							goSingleStep(mov -> ((Movable)mov).pushedFrom)
						)
					)
				),
				sequence(
					condition(mov -> ((Movable) mov).leavePosition),
					selector(
						goInDirectionIfAllowedAndFreeNode(mov -> EDirection.VALUES[0]),
						goInDirectionIfAllowedAndFreeNode(mov -> EDirection.VALUES[1]),
						goInDirectionIfAllowedAndFreeNode(mov -> EDirection.VALUES[2]),
						goInDirectionIfAllowedAndFreeNode(mov -> EDirection.VALUES[3]),
						goInDirectionIfAllowedAndFreeNode(mov -> EDirection.VALUES[4]),
						goInDirectionIfAllowedAndFreeNode(mov -> EDirection.VALUES[5])
					)
				),
				sequence(
					condition(Movable::isShip),
					action(Movable::pushShips),
					sleep(mov -> mov.flockDelay)
				),
				sequence(
					condition(mov -> mov.grid.isBlockedOrProtected(mov.position.x, mov.position.y)),
					selector(
						sequence(
							condition(mov -> mov.preSearchPath(true, mov.position.x, mov.position.y, (short) 50, ESearchType.NON_BLOCKED_OR_PROTECTED)),
							followPresearchedPath()
						),
						// just "succeed" after dying
						action(Movable::kill)
					)
				),
				// flock to decentralize
				sequence(
					condition(mov -> {
						ShortPoint2D decentVector = mov.grid.calcDecentralizeVector(mov.position.x, mov.position.y);

						EDirection randomDirection = mov.getDirection().getNeighbor(MatchConstants.random().nextInt(-2, 2));
						int dx = randomDirection.gridDeltaX + decentVector.x;
						int dy = randomDirection.gridDeltaY + decentVector.y;

						if (ShortPoint2D.getOnGridDist(dx, dy) >= 2) {
							mov.flockDelay = Math.max(mov.flockDelay - 100, 500);
							mov.flockDirection = EDirection.getApproxDirection(0, 0, dx, dy);
							return true;
						} else {
							mov.flockDelay = Math.min(mov.flockDelay + 100, 1000);
							return false;
						}
					}),
					selector(
						goInDirectionIfAllowedAndFreeNode(mov -> mov.flockDirection),
						sequence(
							condition(Movable::setRandomFreeDirection),
							goInDirectionIfAllowedAndFreeNode(Movable::getDirection)
						)
					)
				)
		);
	}

	private boolean setRandomFreeDirection() {
		int start = MatchConstants.random().nextInt(8);

		for(int i = 0; i < EDirection.NUMBER_OF_DIRECTIONS; i++) {
			EDirection dir = EDirection.VALUES[(start + i) % EDirection.NUMBER_OF_DIRECTIONS];

			ShortPoint2D neighbor = dir.getNextHexPoint(position);

			if(grid.isFreePosition(neighbor.x, neighbor.y)) {
				setDirection(dir);
				return true;
			}
		}

		return false;
	}

	protected EDirection flockDirection;

	/**
	 * A call to this method indicates this movable that it shall leave it's position to free the position for another movable.
	 *
	 * @param pushingMovable
	 * 		The movable pushing at this movable. This should be the movable that want's to get the position!
	 */
	@Override
	public void push(ILogicMovable pushingMovable) {
		if(!(pushingMovable instanceof Movable)) return;

		Movable realPushing = (Movable) pushingMovable;

		// other movable can actually push us
		if(id <= realPushing.id) return;

		// ask behaviour how to react
		pushedFrom = pushingMovable.getPosition();
		tick.tick();
		pushedFrom = null;
	}

	/**
	 * Sets the material this movable is carrying to the given one.
	 *
	 * @param materialType
	 * The material type to be set
	 * @return {@link EMaterialType} that has been set before.
	 */
	public final EMaterialType setMaterial(EMaterialType materialType) {
		assert materialType != null : "MaterialType may not be null";
		EMaterialType former = this.materialType;
		this.materialType = materialType;
		return former;
	}

	private void playAnimation(EMovableAction movableAction, short duration) {
		this.animationStartTime = MatchConstants.clock().getTime();
		this.animationDuration = duration;
		this.movableAction = movableAction;
	}

	/**
	 * Lets this movable look in the given direction.
	 *
	 * @param direction
	 * The direction to look.
	 */
	public final void lookInDirection(EDirection direction) {
		this.direction = direction;
	}

	@Override
	public final void setPosition(ShortPoint2D position) {
		if (visible) {
			grid.leavePosition(this.position, this);
			grid.enterPosition(position, this, true);
		}

		this.position = position;
	}

	public final void setVisible(boolean visible) {
		if (this.visible != visible) {
			if (this.visible) { // is visible and gets invisible
				grid.leavePosition(position, this);
			} else {
				grid.enterPosition(position, this, true);
			}
		}  // nothing to change


		this.visible = visible;
	}

	/**
	 * @param dijkstra
	 * 		if true, dijkstra algorithm is used<br>
	 * 		if false, in area finder is used.
	 * @param centerX the x coordinate of the center of the search area
	 * @param centerY the y coordinate of the center of the search area
	 * @param radius the radius of the search area
	 * @param searchType the type of search to conduct
	 * @return true if a path has been found.
	 */
	public final boolean preSearchPath(boolean dijkstra, short centerX, short centerY, short radius, ESearchType searchType) {
		if (dijkstra) {
			this.path = grid.searchDijkstra(this, centerX, centerY, radius, searchType);
		} else {
			this.path = grid.searchInArea(this, centerX, centerY, radius, searchType);
		}

		return path != null;
	}

	protected boolean isBusy() {
		return state != EMovableState.ACTIVE;
	}

	public boolean isOnOwnGround() {
		return grid.getPlayerAt(position) == player;
	}

	private static <T extends Movable> Node<T> followPath(IBooleanConditionFunction<T> pathStep) {
		return resetAfter(mov -> mov.path = null,
				sequence(
					condition(mov -> mov.path != null),

					repeat(mov -> mov.path.hasNextStep(),
						guard(pathStep,
							sequence(
								action2(Movable::setupNextStep),
								goSingleStep(mov -> mov.path.getNextPos()),
								action(mov -> mov.path.goToNextStep()),
								action(Movable::pushShips)
							)
						)
					)
			)
		);
	}

	private NodeStatus canGoNextStep() {
		boolean valid = grid.isValidNextPathPosition(this, path.getNextPos(), path.getTargetPosition());
		if(!valid) {
			path = grid.calculatePathTo(this, path.getTargetPosition());

			valid = (path != null);
		}

		if(!valid) return NodeStatus.FAILURE;

		for(int i = 0; i < 4; i++) {
			ILogicMovable blockingMovable = grid.getMovableAt(path.nextX(), path.nextY());

			if(blockingMovable == null) break;

			switch (i) {
				case 0:
					findWayAroundObstacle();
					break;
				case 1:
					int remainingSteps = path.getRemainingSteps();

					if(remainingSteps > CommonConstants.MOVABLE_PATH_REPAIR_DISTANCE) {
						ShortPoint2D prefixTarget = path.getNextPos(CommonConstants.MOVABLE_PATH_REPAIR_DISTANCE);
						Path newPrefix = grid.calculatePathTo(this, prefixTarget);

						if (newPrefix != null) {
							path.goToNextStep(CommonConstants.MOVABLE_PATH_REPAIR_DISTANCE);
							path = new Path(path, newPrefix);
						}
					} else {
						path = grid.calculatePathTo(this, path.getTargetPosition());
						if(path == null) return NodeStatus.FAILURE;
					}
					break;
				case 2:
					blockingMovable.push(this);
					if(!isAlive()) return NodeStatus.RUNNING;
					break;
				case 3:
					return NodeStatus.RUNNING;
			}
		}

		return NodeStatus.SUCCESS;
	}

	private NodeStatus setupNextStep() {
		NodeStatus pathStatus = canGoNextStep();

		if(pathStatus != NodeStatus.RUNNING) {
			return pathStatus;
		}

		if(pushedFrom != null) {
			if(pushedFrom.equals(path.getNextPos())) return NodeStatus.SUCCESS;

			if(!setRandomFreeDirection()) {
				setDirection(EDirection.getApproxDirection(position, pushedFrom));
			}

			// swap positions and then continue to target
			if(pushedFrom.equals(path.getTargetPosition())) {
				path = new Path(pushedFrom);
			} else {
				path = new Path(grid.calculatePathTo(this, path.getTargetPosition(), pushedFrom), pushedFrom);
			}
			return NodeStatus.SUCCESS;
		} else {
			if(isAlive()) {
				setDirection(EDirection.getApproxDirection(position, path.getNextPos()));
			}
			return NodeStatus.RUNNING;
		}
	}

	/**
	 * Sets the state to the given one and resets the movable to a clean start of this state.
	 *
	 * @param newState the new state of the movable
	 */
	protected void setState(EMovableState newState) {
		this.state = newState;
	}

	/**
	 * kills this movable.
	 */
	@Override
	public final void kill() {
		if(!isAlive()) return;

		decoupleMovable();

		spawnGhost();

		killMovable();
	}

	private void spawnGhost() {
		ShortPoint2D ghostPosition = getGhostPosition();
		if(ghostPosition != null) {
			grid.addSelfDeletingMapObject(position, EMapObjectType.GHOST, Constants.GHOST_PLAY_DURATION, player);
		}
	}

	protected ShortPoint2D getGhostPosition() {
		// position of the movable on a ferry is the position it loaded into the ferry => not correct => don't show ghost
		return isOnFerry()? null : position;
	}

	protected void decoupleMovable() {
		if (!isAlive()) {
			return; // this movable already died.
		}

		grid.leavePosition(this.position, this);

		MovableManager.remove(this);
	}

	protected void killMovable() {
		this.health = -200;
		this.state = EMovableState.DEAD;
		this.selected = false;
		position = null;
	}

	public boolean isOnFerry() {
		return state == EMovableState.ON_FERRY;
	}

	/**
	 * Gets the player object of this movable.
	 *
	 * @return The player object of this movable.
	 */
	public final Player getPlayer() {
		return player;
	}

	@Override
	public final boolean isSelected() {
		return selected;
	}

	@Override
	public final boolean isWounded() {
		return health < movableType.health;
	}

	@Override
	public final void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
	}

	@Override
	public final ESelectionType getSelectionType() {
		return movableType.selectionType;
	}

	@Override
	public final void setSoundPlayed() {
		this.soundPlayed = true;
	}

	@Override
	public final boolean isSoundPlayed() {
		return soundPlayed;
	}

	@Override
	public final EMovableType getMovableType() {
		return movableType;
	}

	@Override
	public final EMovableAction getAction() {
		return movableAction;
	}

	@Override
	public final EDirection getDirection() {
		return direction;
	}

	@Override
	public final float getMoveProgress() {
		return ((float) (MatchConstants.clock().getTime() - animationStartTime)) / animationDuration;
	}

	@Override
	public final EMaterialType getMaterial() {
		return materialType;
	}

	@Override
	public final float getHealth() {
		return health;
	}

	@Override
	public final boolean isAlive() {
		return state != EMovableState.DEAD;
	}

	@Override
	public final boolean isRightstep() {
		return isRightstep;
	}

	@Override
	public final boolean needsPlayersGround() {
		return movableType.needsPlayersGround();
	}

	@Override
	public final void debug() {
		System.out.println("debug: " + this);
	}

	@Override
	public final int getID() {
		return id;
	}

	@Override
	public String toString() {
		return "Movable: " + id + " position: " + position + " player: " + player.playerId + " movableType: " + movableType
			+ " direction: " + direction + " material: " + materialType;
	}

	protected enum EMovableState {
		ACTIVE,
		ON_FERRY,

		DEAD,

		/**
		 * This state may only be used for debugging reasons!
		 */
		DEBUG_STATE
	}

	public boolean isShip() {
		return movableType.isShip();
	}

	public final void setDirection(EDirection direction) {
		this.direction = direction;
	}

	public void addEffect(EEffectType effect) {
		effectEnd.put(effect, effect.getTime()*1000 + MatchConstants.clock().getTime());
	}

	@Override
	public boolean hasEffect(EEffectType effect) {
		return getEffectTime(effect) > 0;
	}

	protected int getEffectTime(EEffectType effect) {
		if(!effectEnd.containsKey(effect)) return -MatchConstants.clock().getTime();
		return effectEnd.get(effect) - MatchConstants.clock().getTime();
	}

	protected static <T extends Movable> Guard<T> handleFrozenEffect() {
		return guard(mov -> mov.hasEffect(EEffectType.FROZEN),
				BehaviorTreeHelper.sleep(mov -> mov.getEffectTime(EEffectType.FROZEN))
		);
	}


	public static Movable createMovable(EMovableType movableType, Player player, ShortPoint2D position, AbstractMovableGrid grid) {
		return createMovable(movableType, player, position, grid, null);
	}

	protected static Movable createMovable(EMovableType movableType, Player player, ShortPoint2D position, AbstractMovableGrid grid, Movable replaceMovable) {
		if(replaceMovable != null) replaceMovable.decoupleMovable();

		Movable movable = chooseMovableClass(movableType, player, position, grid, replaceMovable);

		MovableManager.add(movable);
		grid.enterPosition(position, movable, true);

		if(replaceMovable != null) replaceMovable.killMovable();
		return movable;
	}

	private static Movable chooseMovableClass(EMovableType movableType, Player player, ShortPoint2D position, AbstractMovableGrid grid, Movable movable) {
		switch (movableType) {
			case SWORDSMAN_L1:
			case SWORDSMAN_L2:
			case SWORDSMAN_L3:
			case PIKEMAN_L1:
			case PIKEMAN_L2:
			case PIKEMAN_L3:
				return new InfantryMovable(grid, movableType, position, player, movable);

			case BOWMAN_L1:
			case BOWMAN_L2:
			case BOWMAN_L3:
				return new BowmanMovable(grid, movableType, position, player, movable);

			case MAGE:
				return new MageMovable(grid, position, player, movable);


			case BEARER:
				return new BearerMovable(grid, position, player, movable);
			case DIGGER:
				return new DiggerMovable(grid, position, player, movable);
			case BRICKLAYER:
				return new BricklayerMovable(grid, position, player, movable);


			case THIEF:
				return new ThiefMovable(grid, position, player, movable);
			case PIONEER:
				return new PioneerMovable(grid, position, player, movable);
			case GEOLOGIST:
				return new GeologistMovable(grid, position, player, movable);

			case CARGO_SHIP:
				return new CargoShipMovable(grid, position, player, movable);
			case FERRY:
				return new FerryMovable(grid, position, player, movable);

			case WHITEFLAGGED_DONKEY:
			case DONKEY:
				return new DonkeyMovable(grid, movableType, position, player, movable);

			case FISHERMAN:
			case STONECUTTER:
			case WATERWORKER:
			case LUMBERJACK:
			case FORESTER:
			case WINEGROWER:
			case FARMER:
			case DOCKWORKER:
			case MILLER:
			case SLAUGHTERER:
			case CHARCOAL_BURNER:
			case BREWER:
			case RICE_FARMER:
			case BEEKEEPER:
			case DISTILLER:
			case MEAD_BREWER:
				return new SimpleBuildingWorkerMovable(grid, movableType, position, player, movable);


			case ALCHEMIST:
				return new AlchemistMovable(grid, position, player, movable);

			case SMITH:
				return new SmithMovable(grid, position, player, movable);

			case MELTER:
				return new MelterMovable(grid, position, player, movable);

			case MINER:
				return new MinerMovable(grid, position, player, movable);

			case BAKER:
				return new BakerMovable(grid, position, player, movable);

			case SAWMILLER:
				return new SawMillerMovable(grid, position, player, movable);

			case DONKEY_FARMER:
				return new DonkeyFarmerMovable(grid, position, player, movable);

			case PIG_FARMER:
				return new PigFarmerMovable(grid, position, player, movable);

			case HEALER:
				return new HealerMovable(grid, position, player, movable);

			default:
				throw new AssertionError("movable type " + movableType + " is not mapped to a movable class!");
		}
	}
}
