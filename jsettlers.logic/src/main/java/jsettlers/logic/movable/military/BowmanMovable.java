package jsettlers.logic.movable.military;

import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IBowmanMovable;
import jsettlers.logic.player.Player;

public class BowmanMovable extends SoldierMovable implements IBowmanMovable {
	private static final float BOWMAN_ATTACK_DURATION = 1.2f;
	private static final long serialVersionUID = 6513070596651626711L;

	public BowmanMovable(AbstractMovableGrid grid, EMovableType movableType, ShortPoint2D position, Player player, Movable movable) {
		super(grid, movableType, position, player, movable);
	}

	@Override
	public void convertToPioneer() {
		createMovable(EMovableType.PIONEER, player, position, grid, this);
	}

	@Override
	protected boolean isEnemyAttackable() {
		if (!isEnemyValid()) {
			return false;
		}

		int distance = position.getOnGridDistTo(enemy.getPosition());

		if (isInTower) {
			return Constants.BOWMAN_MIN_ATTACK_DISTANCE <= distance && distance <= Constants.BOWMAN_IN_TOWER_ATTACK_RADIUS;
		} else {
			return Constants.BOWMAN_MIN_ATTACK_DISTANCE <= distance && distance <= Constants.BOWMAN_ATTACK_RADIUS;
		}
	}

	@Override
	protected short getAttackDuration() {
		return (short)(BOWMAN_ATTACK_DURATION*1000);
	}

	@Override
	protected short getMinSearchDistance() {
		return Constants.BOWMAN_MIN_ATTACK_DISTANCE;
	}

	@Override
	protected short getMaxSearchDistance() {
		if(defending) return Constants.BOWMAN_ATTACK_RADIUS;

		return isInTower ? Constants.BOWMAN_IN_TOWER_ATTACK_RADIUS : Constants.SOLDIER_SEARCH_RADIUS;
	}

	@Override
	protected void startAttack() {
		if(!hasEffect(EEffectType.NO_ARROWS)) {
			grid.addArrowObject(position, player, getMovableType().getStrength() * getCombatStrength(), enemy.getPosition()
			);
		}
	}
}
