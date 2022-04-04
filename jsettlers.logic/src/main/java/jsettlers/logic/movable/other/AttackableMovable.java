package jsettlers.logic.movable.other;

import jsettlers.algorithms.simplebehaviortree.Root;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IAttackable;
import jsettlers.logic.movable.interfaces.IAttackableMovable;
import jsettlers.logic.player.Player;

public class AttackableMovable extends Movable implements IAttackableMovable {

	private static final long serialVersionUID = -7194469828472910405L;
	protected boolean attackable;

	public AttackableMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);

		attackable = movableType.attackable;
	}

	@Override
	public void receiveHit(float hitStrength, ShortPoint2D attackerPos, IPlayer attackingPlayer) {
		if(hasEffect(EEffectType.SHIELDED)) hitStrength *= EEffectType.SHIELDED_DAMAGE_FACTOR;

		this.health -= hitStrength;
		if (health <= 0) {
			this.kill();
		}

		player.showMessage(SimpleMessage.attacked(attackingPlayer.getPlayerId(), attackerPos));
	}

	@Override
	public final boolean isAttackable() {
		return attackable;
	}

	@Override
	public boolean isTower() {
		return false;
	}


	/**
	 * This method may only be called if this movable shall be informed about a movable that's in it's search radius.
	 *
	 * @param other
	 * 		The other movable.
	 */
	@Override
	public void informAboutAttackable(IAttackable other) {
	}

}
