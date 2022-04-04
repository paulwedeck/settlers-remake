package jsettlers.logic.movable.interfaces;

import jsettlers.logic.player.Player;
import jsettlers.shared.ShortPoint2D;

public interface IAttackableHumanMovable extends IAttackableMovable {

	void moveToFerry(IFerryMovable ferry, ShortPoint2D entrancePosition);

	void leaveFerryAt(ShortPoint2D position);


	boolean pingWounded(IHealerMovable healer);

	boolean needsTreatment();

	boolean isGoingToTreatment();

	void heal();

	/**
	 * This function will create a new movable.
	 * The original movable will be reported as DEAD
	 *
	 * @param player
	 * 		The new owner of this movable
	 */
	void defectTo(Player player);
}
