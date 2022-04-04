package jsettlers.logic.movable.specialist;

import jsettlers.algorithms.path.Path;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.MutablePoint2D;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.mutables.MutableDouble;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class GeologistMovable extends AttackableHumanMovable {

	private static final long serialVersionUID = 1L;

	private static final float ACTION1_DURATION = 1.4f;
	private static final float ACTION2_DURATION = 1.5f;

	private ShortPoint2D currentTarget = null;
	private ShortPoint2D goToTarget = null;
	private ShortPoint2D centerPos = null;

	public GeologistMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.GEOLOGIST, position, player, movable);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.GEOLOGIST, new Root<>(createGeologistBehaviour()));
	}

	public static Node<GeologistMovable> createGeologistBehaviour() {
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
						goToPos(mov -> mov.goToTarget),
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
						action(mov -> {mov.centerPos = mov.currentTarget;}),
						ignoreFailure(repeat(mov -> true,
							sequence(
								findWorkablePosition(),
								resetAfter(mov -> mov.grid.setMarked(mov.currentTarget, false),

									sequence(
										action(mov -> {mov.grid.setMarked(mov.currentTarget, true);}),
										goToPos(mov -> mov.currentTarget),
										ignoreFailure(workOnPosition())
									)
								)
							)
						)),
						action(mov -> {
							mov.currentTarget = null;
							mov.centerPos = null;
						})
					)
				),
				doingNothingGuard()
		);
	}

	protected static Node<GeologistMovable> findWorkablePosition() {
		return selector(
				condition(mov -> {
					MutablePoint2D bestNeighbourPos = new MutablePoint2D(-1, -1);
					MutableDouble bestNeighbourDistance = new MutableDouble(Double.MAX_VALUE); // distance from start point

					HexGridArea.streamBorder(mov.currentTarget, 2)
							.filterBounds(mov.grid.getWidth(), mov.grid.getHeight())
							.filter((x, y) -> mov.grid.fitsSearchType(mov, x, y, ESearchType.RESOURCE_SIGNABLE))
							.forEach((x, y) -> {
								double distance = ShortPoint2D.getOnGridDist(x - mov.centerPos.x, y - mov.centerPos.y);
								if (distance < bestNeighbourDistance.value) {
									bestNeighbourDistance.value = distance;
									bestNeighbourPos.x = x;
									bestNeighbourPos.y = y;
								}
							});

					if(bestNeighbourDistance.value != Double.MAX_VALUE) {
						mov.currentTarget = bestNeighbourPos.createShortPoint2D();
						return true;
					}
					return false;
				}),
				condition(mov -> {
					Path dijkstraPath = mov.grid.searchDijkstra(mov, mov.currentTarget.x, mov.currentTarget.y, (short) 30, ESearchType.RESOURCE_SIGNABLE);
					if (dijkstraPath != null) {
						mov.currentTarget = dijkstraPath.getTargetPosition();
						return true;
					}
					return false;
				})
		);
	}

	protected static Node<GeologistMovable> workOnPosition() {
		return sequence(
				condition(mov -> {
					mov.grid.setMarked(mov.currentTarget, false);
					boolean success = mov.grid.fitsSearchType(mov, mov.currentTarget.x, mov.currentTarget.y, ESearchType.RESOURCE_SIGNABLE);
					mov.grid.setMarked(mov.currentTarget, true);

					return success;
				}),

				playAction(EMovableAction.ACTION1, (short)(ACTION1_DURATION*1000)),
				playAction(EMovableAction.ACTION2, (short)(ACTION2_DURATION*1000)),

				condition(mov -> {
					mov.grid.setMarked(mov.currentTarget, false);
					boolean success = mov.grid.executeSearchType(mov, mov.currentTarget, ESearchType.RESOURCE_SIGNABLE);
					mov.grid.setMarked(mov.currentTarget, true);

					return success;
				})
		);
	}
}
