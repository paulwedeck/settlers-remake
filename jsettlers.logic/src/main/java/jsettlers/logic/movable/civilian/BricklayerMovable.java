package jsettlers.logic.movable.civilian;

import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.map.grid.partition.manager.manageables.IManageableBricklayer;
import jsettlers.logic.map.grid.partition.manager.manageables.interfaces.IConstructableBuilding;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class BricklayerMovable extends CivilianMovable implements IManageableBricklayer {

	private static final long serialVersionUID = 1L;

	private static final float BRICKLAYER_ACTION_DURATION = 1f;

	private IConstructableBuilding constructionSite = null;
	private boolean registered = false;
	private ShortPoint2D targetPosition;
	private EDirection lookDirection;

	public BricklayerMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.BRICKLAYER, position, player, movable);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.BRICKLAYER, new Root<>(createBricklayerBehaviour()));
	}

	private static Node<BricklayerMovable> createBricklayerBehaviour() {
		return guardSelector(
				fleeIfNecessary(),
				guard(mov -> mov.constructionSite != null && mov.constructionSite.isBricklayerRequestActive(),
					sequence(
						selector(
							goToPos(mov -> mov.targetPosition),
							sequence(
								action(BricklayerMovable::abortJob),
								alwaysFail()
							)
						),
						action(mov -> {
							mov.lookInDirection(mov.lookDirection);
						}),
						repeat(mov -> true,
							sequence(
								condition(mov -> mov.constructionSite.tryToTakeMaterial()),
								playAction(EMovableAction.ACTION1, (short)(BRICKLAYER_ACTION_DURATION*1000))
							)
						)
					)
				),
				guard(mov -> !mov.registered,
					action(mov -> {
						mov.constructionSite = null;
						mov.registered = true;
						mov.grid.addJobless(mov);
					})
				),
				doingNothingGuard()
		);
	}

	@Override
	public boolean setBricklayerJob(IConstructableBuilding constructionSite, ShortPoint2D bricklayerTargetPos, EDirection direction) {
		if(this.constructionSite == null) {
			this.constructionSite = constructionSite;
			this.targetPosition = bricklayerTargetPos;
			this.lookDirection = direction;
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

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		abortJob();

		grid.removeJobless(this);
	}

	@Override
	protected void abortJob() {
		if(constructionSite != null) constructionSite.bricklayerRequestFailed(targetPosition, lookDirection);
	}
}
