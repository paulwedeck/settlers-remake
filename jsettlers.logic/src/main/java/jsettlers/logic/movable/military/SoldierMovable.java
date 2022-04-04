package jsettlers.logic.movable.military;

import jsettlers.algorithms.path.Path;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.ISoldierMovable;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public abstract class SoldierMovable extends AttackableHumanMovable implements ISoldierMovable {

	private static final long serialVersionUID = 667104393129440108L;

	protected IAttackable enemy;

	private IOccupyableBuilding building;
	protected boolean isInTower;
	private ShortPoint2D inTowerAttackPosition;
	protected boolean defending;

	private ShortPoint2D currentTarget = null;
	private ShortPoint2D goToTarget = null;

	private int patrolStep = -1;
	private ShortPoint2D[] patrolPoints = null;

	private boolean enemyNearby;
	private IAttackable toCloseEnemy;
	private ShortPoint2D startPoint;


	public SoldierMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);

		enemyNearby = true; // might not actually be true
	}

	static {
		Root<SoldierMovable> behaviour = new Root<>(createSoldierBehaviour());

		for(EMovableType type : EMovableType.SOLDIERS) {
			MovableManager.registerBehaviour(type, behaviour);
		}
	}

	private static Node<SoldierMovable> createSoldierBehaviour() {
		return guardSelector(
				handleFrozenEffect(),
				// go to tower
				guard(mov -> mov.building != null && !mov.isInTower,
					resetAfter(mov -> {
						if(!mov.isInTower && mov.building != null) {
							mov.notifyTowerThatRequestFailed();
							mov.building = null;
						}
					},
						sequence(
							selector(
								condition(mov -> mov.building.getDoor().equals(mov.position)),
								goToPos(mov -> mov.building.getDoor(), mov -> mov.building.getPlayer() == mov.player && !mov.building.isDestroyed())
							),
							hide(),
							action(mov -> {
								OccupierPlace place = mov.building.addSoldier(mov);
								mov.setPosition(place.getPosition().calculatePoint(mov.building.getPosition()));

								if (mov.isBowman()) mov.inTowerAttackPosition = mov.building.getTowerBowmanSearchPosition(place);
								mov.isInTower = true;
							})
						)
					)
				),
				guard(mov -> mov.nextTarget != null,
					action(mov -> {
						mov.abortGoTo();

						switch(mov.nextMoveToType) {
							default:
							case DEFAULT:
								mov.currentTarget = mov.nextTarget;
								break;
							case FORCED:
								mov.goToTarget = mov.nextTarget;
								break;
							case PATROL:
								mov.patrolPoints = new ShortPoint2D[] {mov.position, mov.nextTarget};
								mov.patrolStep = 0;
								break;
						}

						mov.nextTarget = null;
					})
				),
				guard(mov -> mov.goToTarget != null,
					sequence(
						ignoreFailure(goToPos(mov -> mov.goToTarget)),
						action(mov -> {
							mov.enterFerry();
							mov.goToTarget = null;
						})
					)
				),
				// attack enemy at the door of the tower
				guard(mov -> mov.defending,
					selector(
						sequence(
							findEnemy(),
							ignoreFailure(attackEnemy())
						),
						findTooCloseEnemy(),
						action(mov -> {
							mov.building.towerDefended(mov);
							mov.defending = false;
						})
					)
				),
				// attack enemies from the top of the tower
				guard(mov -> mov.isBowman() && mov.isInTower && mov.enemyNearby,
					sequence(
						selector(
							findEnemy(),
							sequence(
								sleep(100),
								alwaysFail()
							)
						),
						attackEnemy()
					)
				),
				// attack enemy
				guard(mov -> mov.enemyNearby && !mov.isInTower,
					selector(
						sequence(
							// handle potential enemy
							findEnemy(),
							ignoreFailure(
								selector(
									// attack him
									attackEnemy(),

									condition(mov -> !mov.enemy.isAlive()), // enemy might die even if the attack fails

									// or roughly chase enemy
									goInDirectionIfAllowedAndFreeNode(mov -> EDirection.getApproxDirection(mov.position, mov.enemy.getPosition())),
									// or go to his position
									resetAfter(mov -> {
										mov.startPoint = null;
									},
										sequence(
											action(mov -> {
												mov.startPoint = mov.position;
											}),
											goToPos(mov -> mov.enemy.getPosition(), mov -> {
												// hit him
												if(mov.isEnemyAttackable()) return false;
												// update behaviour (adjust target)
												if(mov.startPoint.getOnGridDistTo(mov.position) > 2) return false;
												return true;
											})
										)
									)
								)
							)
						),
						sequence(
							// handle nearby enemies (bowman only)
							findTooCloseEnemy(),
							// run in opposite direction
							ignoreFailure(goInDirectionIfAllowedAndFreeNode(mov -> EDirection.getApproxDirection(mov.toCloseEnemy.getPosition(), mov.position)))
						),
						sequence(
							// no enemy in sight
							action(mov -> {
								mov.enemyNearby = false;
							})
						)
					)
				),
				guard(mov -> mov.currentTarget != null,
					sequence(
						ignoreFailure(goToPos(mov -> mov.currentTarget)),
						action(mov -> {
							mov.currentTarget = null;
						})
					)
				),
				guard(mov -> mov.patrolStep != -1,
					sequence(
						ignoreFailure(goToPos(mov -> mov.patrolPoints[mov.patrolStep])),
						action(mov -> {
							mov.patrolStep = (mov.patrolStep+1) % mov.patrolPoints.length;
						})
					)
				),
				guard(mov -> !mov.isInTower,
					doingNothingAction()
				)
		);
	}

	private static Node<SoldierMovable> findTooCloseEnemy() {
		return sequence(
				condition(mov -> mov.getMinSearchDistance() > 0),
				condition(mov -> {
					mov.toCloseEnemy = mov.grid.getEnemyInSearchArea(
							mov.getAttackPosition(), mov, (short) 0, mov.getMinSearchDistance(), !mov.defending);
					return mov.toCloseEnemy != null;
				})
		);
	}

	private static Node<SoldierMovable> findEnemy() {
		return condition(mov -> {
			mov.enemy = mov.grid.getEnemyInSearchArea(mov.getAttackPosition(), mov, mov.getMinSearchDistance(), mov.getMaxSearchDistance(), !mov.defending);
			return mov.enemy != null;
		});
	}

	private static Node<SoldierMovable> attackEnemy() {
		return sequence(
				condition(SoldierMovable::isEnemyValid),
				condition(SoldierMovable::isEnemyAttackable),
				action(mov -> {mov.setDirection(EDirection.getApproxDirection(mov.position, mov.enemy.getPosition()));}),
				action(SoldierMovable::startAttack),
				playAction(EMovableAction.ACTION1, SoldierMovable::getAttackDuration),
				condition(SoldierMovable::isEnemyValid),
				action(SoldierMovable::hitEnemy)

		);
	}

	protected boolean isEnemyValid() {
		return enemy != null && enemy.isAlive();
	}

	@Override
	protected boolean isBusy() {
		return super.isBusy() || isInTower;
	}

	protected abstract short getAttackDuration();

	protected void startAttack() {

	}

	private void abortGoTo() {
		currentTarget = null;
		goToTarget = null;
		patrolStep = -1;
		patrolPoints = null;
	}

	private void notifyTowerThatRequestFailed() {
		if(building.getPlayer() != player) return; // only notify, if the tower still belongs to this player

		building.requestFailed(this);
		building = null;
		playerControlled = true;
	}

	protected ShortPoint2D getAttackPosition() {
		return isInTower && !defending && isBowman() ? inTowerAttackPosition : position;
	}

	private boolean isBowman() {
		return getMovableType().isBowman();
	}

	protected void hitEnemy() {
	}

	protected abstract boolean isEnemyAttackable();

	protected abstract short getMinSearchDistance();

	protected abstract short getMaxSearchDistance();

	@Override
	public boolean moveToTower(IOccupyableBuilding building) {
		if(this.building != null || hasEffect(EEffectType.FROZEN)) return false;

		this.building = building;
		playerControlled = false;

		abortGoTo(); // this prevents that the soldiers goes to the last target after he leaves the tower.
		nextTarget = null;
		return true;
	}

	public void leaveTower(ShortPoint2D newPosition) {
		if (isInTower) {
			setPosition(newPosition);
			setVisible(true);
			setSelected(false);

			isInTower = false;
			defending = false;

		}

		building = null;
		playerControlled = true;
	}

	@Override
	public void receiveHit(float hitStrength, ShortPoint2D attackerPos, IPlayer attackingPlayer) {
		super.receiveHit(hitStrength, attackerPos, attackingPlayer);
		enemyNearby = true;
	}

	@Override
	public void informAboutAttackable(IAttackable other) {
		enemyNearby = true;
	}

	@Override
	public void defendTowerAt() {
		setPosition(building.getDoor());
		defending = true;
	}

	@Override
	public void findWayAroundObstacle() {
		if (building == null && startPoint != null) {
			EDirection direction = EDirection.getDirection(position, path.getNextPos());

			EDirection rightDir = direction.getNeighbor(-1);
			ShortPoint2D rightPos = rightDir.getNextHexPoint(position);
			EDirection leftDir = direction.getNeighbor(1);
			ShortPoint2D leftPos = leftDir.getNextHexPoint(position);

			ShortPoint2D freePosition = getRandomFreePosition(rightPos, leftPos);

			if (freePosition != null) {
				path = new Path(freePosition);

			} else {
				EDirection twoRightDir = direction.getNeighbor(-2);
				ShortPoint2D twoRightPos = twoRightDir.getNextHexPoint(position);
				EDirection twoLeftDir = direction.getNeighbor(2);
				ShortPoint2D twoLeftPos = twoLeftDir.getNextHexPoint(position);

				freePosition = getRandomFreePosition(twoRightPos, twoLeftPos);

				if (freePosition != null) {
					path = new Path(freePosition);
				}
			}
		} else {
			super.findWayAroundObstacle();
		}
	}

	private ShortPoint2D getRandomFreePosition(ShortPoint2D pos1, ShortPoint2D pos2) {
		boolean pos1Free = grid.isFreePosition(pos1.x, pos1.y);
		boolean pos2Free = grid.isFreePosition(pos2.x, pos2.y);

		if (pos1Free && pos2Free) {
			return MatchConstants.random().nextBoolean() ? pos1 : pos2;
		} else if (pos1Free) {
			return pos1;
		} else if (pos2Free) {
			return pos2;
		} else {
			return null;
		}
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		if (building != null) {
			if (isInTower) {
				building.removeSoldier(this);
			} else {
				notifyTowerThatRequestFailed();
			}
		}
	}

	protected float getCombatStrength() {
		boolean alliedGround = player.hasSameTeam(grid.getPlayerAt(position));

		float strengthMod = 1;
		if(alliedGround && hasEffect(EEffectType.DEFEATISM)) strengthMod *= EEffectType.DEFEATISM_DAMAGE_FACTOR;
		if(!alliedGround && hasEffect(EEffectType.INCREASED_MORALE)) strengthMod *= EEffectType.INCREASED_MORALE_DAMAGE_FACTOR;
		if(hasEffect(EEffectType.MOTIVATE_SWORDSMAN)) strengthMod *= EEffectType.MOTIVATE_SWORDSMAN_DAMAGE_FACTOR;

		return player.getCombatStrengthInformation().getCombatStrength(isOnOwnGround()) * strengthMod;
	}

	public ShortPoint2D getCurrentTarget() {
		return path != null ? path.getTargetPosition() : null;
	}
}
