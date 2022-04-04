package jsettlers.logic.movable.military;

import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.player.Player;

public class InfantryMovable extends SoldierMovable {
	private static final float INFANTRY_ATTACK_DURATION = 1;
	private static final long serialVersionUID = -7469857575626629826L;

	public InfantryMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);
	}

	@Override
	protected boolean isEnemyAttackable() {
		if(!isEnemyValid()) {
			return false;
		}

		int distance = position.getOnGridDistTo(enemy.getPosition());

		return distance <= getMaxAttackDistance();
	}

	private short getMaxAttackDistance() {
		if(defending) return Constants.TOWER_DEFEND_ATTACK_RADIUS;

		if(getMovableType().isPikeman() && !enemy.isTower()) return Constants.PIKEMAN_ATTACK_RADIUS;

		return Constants.DEFAULT_ATTACK_RADIUS;
	}

	@Override
	protected short getAttackDuration() {
		short duration = (short)(INFANTRY_ATTACK_DURATION*1000);
		if(hasEffect(EEffectType.MOTIVATE_SWORDSMAN))  duration *= EEffectType.MOTIVATE_SWORDSMAN_ANIMATION_FACTOR;

		return duration;
	}

	@Override
	protected short getMinSearchDistance() {
		return 0;
	}

	@Override
	protected short getMaxSearchDistance() {
		if(defending) return getMaxAttackDistance();

		return Constants.SOLDIER_SEARCH_RADIUS;
	}

	@Override
	protected void hitEnemy() {
		enemy.receiveHit(getMovableType().getStrength() * getCombatStrength(), position, player);
		// decrease the enemy's health
	}
}
