package jsettlers.logic.movable.specialist;

import java.util.BitSet;

import jsettlers.algorithms.path.Path;
import jsettlers.algorithms.simplebehaviortree.Node;
import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsThief;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

import static jsettlers.algorithms.simplebehaviortree.BehaviorTreeHelper.*;

public class ThiefMovable extends AttackableHumanMovable implements IGraphicsThief {

	private static final long serialVersionUID = 1;

	private final BitSet uncoveredBy = new BitSet();

	private static final float ACTION1_DURATION = 1f;

	private ShortPoint2D currentTarget = null;
	private ShortPoint2D goToTarget = null;

	private ShortPoint2D returnPos = null;

	public ThiefMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.THIEF, position, player, movable);
	}

	static {
		MovableManager.registerBehaviour(EMovableType.THIEF, new Root<>(createThiefBehaviour()));
	}

	public static Node<ThiefMovable> createThiefBehaviour() {
		return guardSelector(
				handleFrozenEffect(),
				guard(mov -> mov.nextTarget != null,
					action(mov -> {
						mov.goToTarget = null;
						mov.currentTarget = null;
						mov.returnPos = null;

						// steal something
						if(mov.nextMoveToType.isWorkOnDestination() && mov.getMaterial() == EMaterialType.NO_MATERIAL) {
							Path dijkstraPath = mov.grid.searchDijkstra(mov, mov.nextTarget.x, mov.nextTarget.y, (short) 30, ESearchType.FOREIGN_MATERIAL);
							if (dijkstraPath != null) {
								mov.currentTarget = dijkstraPath.getTargetPosition();
								mov.returnPos = mov.position;
							}
						}
						if(mov.currentTarget == null) {
							// or just go there
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
				guard(mov -> (mov.getMaterial() != EMaterialType.NO_MATERIAL && mov.isOnOwnGround()),
					action(ThiefMovable::dropMaterialIfPossible)
				),
				guard(mov -> mov.currentTarget != null,
					sequence(
						selector(
							condition(mov -> mov.position.equals(mov.currentTarget)),
							goToPos(mov -> mov.currentTarget, mov -> mov.getMaterial() == EMaterialType.NO_MATERIAL || !mov.isOnOwnGround())
						),
						selector(
							condition(mov -> mov.getMaterial() != EMaterialType.NO_MATERIAL),
							sequence(
								stealMaterial(),
								action(mov -> {
									mov.goToTarget = mov.returnPos;
								})
							)
						)
					)
				),
				doingNothingGuard()
		);
	}

	private void dropMaterialIfPossible() {
		EMaterialType stolenMaterial = setMaterial(EMaterialType.NO_MATERIAL);
		grid.dropMaterial(position, stolenMaterial, true, true);
	}

	protected static Node<ThiefMovable> stealMaterial() {
		return sequence(
				condition(mov -> mov.grid.fitsSearchType(mov, mov.currentTarget.x, mov.currentTarget.y, ESearchType.FOREIGN_MATERIAL)),

				playAction(EMovableAction.ACTION1, (short)(ACTION1_DURATION*1000)),

				condition(mov -> {
					EMaterialType stolenMaterial = mov.grid.takeMaterial(mov.currentTarget);
					mov.setMaterial(stolenMaterial);
					return stolenMaterial != EMaterialType.NO_MATERIAL;
				})
		);
	}


	@Override
	public boolean isUncoveredBy(byte teamId) {
		return uncoveredBy.get(teamId);
	}

	@Override
	public void receiveHit(float hitStrength, ShortPoint2D attackerPos, IPlayer attackingPlayer) {
		super.receiveHit(hitStrength, attackerPos, attackingPlayer);

		uncoveredBy.set(attackingPlayer.getTeamId());
	}

	@Override
	public void heal() {
		super.heal();
		uncoveredBy.clear();
	}

	@Override
	protected void decoupleMovable() {
		super.decoupleMovable();

		dropMaterialIfPossible();
	}
}
