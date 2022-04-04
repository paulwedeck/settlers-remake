package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.nodes.Guard;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.ICivilianMovable;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public abstract class CivilianMovable extends Movable implements ICivilianMovable {

	private static final long serialVersionUID = -8052643329227231124L;

	private int searchesCounter = 0;
	protected boolean turnNextTime;

	private ShortPoint2D lastCheckedPosition = null;
	private byte pathStepCheckedCounter;

	protected CivilianMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable replace) {
		super(grid, movableType, position, player, replace);
	}

	protected static <T extends CivilianMovable> Guard<T> fleeIfNecessary() {
		return guard(CivilianMovable::checkPlayerOfPosition,
				resetAfter(mov -> {
					((CivilianMovable)mov).searchesCounter = 0;
				},
					sequence(
						action(CivilianMovable::abortJob),
						repeat(mov -> ((CivilianMovable)mov).searchesCounter <= 120,
							sequence(
								action(mov -> {((CivilianMovable)mov).searchesCounter++;}),
								ignoreFailure(
									selector(
										// move to nearest own ground
										sequence(
											condition(mov -> mov.preSearchPath(true, mov.position.x, mov.position.y, Constants.MOVABLE_FLEEING_DIJKSTRA_RADIUS, ESearchType.VALID_FREE_POSITION)
													|| mov.preSearchPath(false, mov.position.x, mov.position.y, Constants.MOVABLE_FLEEING_MAX_RADIUS, ESearchType.VALID_FREE_POSITION)),

											action(mov -> {((CivilianMovable)mov).lastCheckedPosition = null;}),
											followPresearchedPath(CivilianMovable::checkEvacuationPath)
										),
										// or move in random directions
										sequence(
											action(mov -> {
												EDirection currentDirection = mov.getDirection();
												if (mov.turnNextTime || MatchConstants.random().nextFloat() < 0.10) {
													mov.turnNextTime = false;
													mov.lookInDirection(currentDirection.getNeighbor(MatchConstants.random().nextInt(-1, 1)));
												}
											}),
											selector(
												sequence(
													goInDirectionIfFree(Movable::getDirection),
													action(mov -> {
														mov.turnNextTime = MatchConstants.random().nextInt(7) == 0;
													})
												),
												action(mov -> {mov.turnNextTime = true;})
											)
										)
									)
								)
							)
						),
						action(Movable::kill)
					)
				)
		);
	}

	protected abstract void abortJob();

	private boolean checkEvacuationPath() {
		if (position.equals(lastCheckedPosition)) {
			pathStepCheckedCounter++;
			searchesCounter++;
		} else {
			pathStepCheckedCounter = 0;
			lastCheckedPosition = position;
		}

		return !grid.isValidPosition(this, position.x, position.y) && pathStepCheckedCounter < 5;
	}

	protected boolean checkPlayerOfPosition() {
		// civilians are only allowed on their players ground => abort current task and flee to nearest own ground
		return grid.getPlayerAt(position) != player;
	}
}
