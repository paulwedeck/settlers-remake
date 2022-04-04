package jsettlers.logic.movable.specialist;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IPioneerMovable;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class PioneerMovable extends AttackableHumanMovable implements IPioneerMovable {

	private static final long serialVersionUID = 1L;

	private static final float ACTION1_DURATION = 1.2f;

	private ShortPoint2D currentTarget = null;
	private ShortPoint2D goToTarget = null;

	private EDirection workDirection = null;

	public PioneerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.PIONEER, position, player, movable);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.PIONEER, new Root<>(createPioneerBehaviour()));
	}

	private static Node<PioneerMovable> createPioneerBehaviour() {
		return guardSelector(
				handleFrozenEffect(),
				guard(mov -> mov.nextTarget != null,
					action(mov -> {
						mov.currentTarget = null;
						mov.goToTarget = null;

						if(mov.nextMoveToType.isWorkOnDestination()) {
							mov.currentTarget = mov.nextTarget;
						} else {
							mov.goToTarget = mov.nextTarget;
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
				guard(mov -> mov.currentTarget != null,
					sequence(
						selector(
							condition(mov -> mov.position.equals(mov.currentTarget)),
							goToPos(mov -> mov.currentTarget)
						),
						ignoreFailure(repeat(mov -> true,
							sequence(
								findWorkablePosition(),
								ignoreFailure(workOnPosition())
							)
						)),
						action(mov -> {
							mov.currentTarget = null;
						})
					)
				),
				doingNothingGuard()
		);
	}

	@Override
	public boolean convertToBearer() {
		if(!player.equals(grid.getPlayerAt(position))) return false;

		createMovable(EMovableType.BEARER, player, position, grid, this);

		return true;
	}

	protected static Node<PioneerMovable> findWorkablePosition() {
		return selector(
				sequence(
					condition(mov -> {
						mov.workDirection = null;
						double[] bestNeighbourDistance = new double[] { Double.MAX_VALUE }; // distance from start point

						ShortPoint2D position = mov.getPosition();
						HexGridArea.stream(position.x, position.y, 1, 6)
								.filter((x, y) -> mov.grid.isValidPosition(mov, x, y) && mov.canWorkOnPos(x, y))
								.forEach((x, y) -> {
									double distance = ShortPoint2D.getOnGridDist(x - mov.currentTarget.x, y - mov.currentTarget.y);
									if (distance < bestNeighbourDistance[0]) {
										bestNeighbourDistance[0] = distance;
										mov.workDirection = EDirection.getApproxDirection(position.x, position.y, x, y);
									}
								});
						return mov.workDirection != null;
					}),
					goInDirectionIfAllowedAndFreeNode(mov -> mov.workDirection)
				),
				sequence(
					condition(mov -> mov.preSearchPath(true, mov.position.x, mov.position.y, (short) 30, ESearchType.UNENFORCED_FOREIGN_GROUND)),
					followPresearchedPath()
				)
		);
	}

	private boolean canWorkOnPos(int x, int y) {
		return grid.fitsSearchType(this, x, y, ESearchType.UNENFORCED_FOREIGN_GROUND);
	}

	protected static Node<PioneerMovable> workOnPosition() {
		return sequence(
				condition(mov -> mov.grid.fitsSearchType(mov, mov.position.x, mov.position.y, ESearchType.UNENFORCED_FOREIGN_GROUND)),

				playAction(EMovableAction.ACTION1, (short)(ACTION1_DURATION*1000)),

				action(mov -> {
					mov.grid.changePlayerAt(mov.position, mov.getPlayer());
				})
		);
	}
}
