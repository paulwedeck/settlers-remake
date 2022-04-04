package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableDigger;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IDiggerRequester;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class DiggerMovable extends CivilianMovable implements IManageableDigger {

	private static final long serialVersionUID = 1L;

	private IDiggerRequester requester = null;
	private boolean registered = false;
	private ShortPoint2D targetPosition;

	public DiggerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.DIGGER, position, player, movable);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.DIGGER, new Root<>(createDiggerBehaviour()));
	}

	private static Node<DiggerMovable> createDiggerBehaviour() {
		return guardSelector(
				fleeIfNecessary(),
				guard(mov -> mov.requester != null && mov.requester.isDiggerRequestActive(),
					selector(
						repeat(DiggerMovable::flattenPositionsRemaining,
							sequence(
								condition(DiggerMovable::findDiggablePosition),
								resetAfter(
									mov -> mov.grid.setMarked(mov.targetPosition, false),
									sequence(
										action(mov -> {mov.grid.setMarked(mov.targetPosition, true);}),
										selector(
											condition(mov -> mov.position.equals(mov.targetPosition)),
											goToPos(mov -> mov.targetPosition)
										),
										playAction(EMovableAction.ACTION1, (short)1000),
										action(DiggerMovable::executeDigg)
									)
								)
							)
						),
						action(DiggerMovable::abortJob)
					)
				),
				guard(mov -> !mov.registered,
					action(mov -> {
						mov.requester = null;
						mov.registered = true;
						mov.grid.addJobless(mov);
					})
				),
				doingNothingGuard()
		);
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		if(requester != null) {
			abortJob();
		} else {
			grid.removeJobless(this);
		}
	}

	@Override
	public boolean setDiggerJob(IDiggerRequester requester) {
		if (this.requester == null) {
			this.requester = requester;
			registered = false;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void convertToBearer() {
		grid.dropMaterial(position, getMovableType().getTool(), true, true);
		createMovable(EMovableType.BEARER, player, position, grid, this);
	}

	private void executeDigg() {
		grid.changeHeightTowards(position.x, position.y, requester.getAverageHeight());
	}

	private boolean flattenPositionsRemaining() {
		for (RelativePoint relativePosition : requester.getBuildingVariant().getProtectedTiles()) {
			if (needsToBeWorkedOn(relativePosition.calculatePoint(requester.getPosition()))) {
				return true;
			}
		}
		return false;
	}

	private boolean findDiggablePosition() {
		RelativePoint[] blockedTiles = requester.getBuildingVariant().getProtectedTiles();
		ShortPoint2D buildingPos = requester.getPosition();
		int offset = MatchConstants.random().nextInt(blockedTiles.length);

		for (int i = 0; i < blockedTiles.length; i++) {
			ShortPoint2D pos = blockedTiles[(i + offset) % blockedTiles.length].calculatePoint(buildingPos);
			if (!grid.isMarked(pos) && needsToBeWorkedOn(pos)) {
				targetPosition = pos;
				return true;
			}
		}
		return false;
	}

	private boolean needsToBeWorkedOn(ShortPoint2D pos) {
		return needsToChangeHeight(pos) || isNotFlattened(pos);
	}

	private boolean isNotFlattened(ShortPoint2D pos) {
		// some places can't be flattened
		if(!grid.canChangeLandscapeTo(pos.x, pos.y, ELandscapeType.FLATTENED) &&
				!grid.canChangeLandscapeTo(pos.x, pos.y, ELandscapeType.FLATTENED_DESERT)) return false;


		ELandscapeType currentLandscape = grid.getLandscapeTypeAt(pos.x, pos.y);

		return currentLandscape != ELandscapeType.FLATTENED && currentLandscape != ELandscapeType.FLATTENED_DESERT;
	}

	private boolean needsToChangeHeight(ShortPoint2D pos) {
		return grid.getHeightAt(pos) != requester.getAverageHeight();
	}

	@Override
	protected void abortJob() {
		if(requester != null) {
			requester.diggerRequestFailed();
			requester = null;
		}
	}
}
