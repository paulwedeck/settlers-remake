package jsettlers.logic.movable.interfaces;

import jsettlers.common.position.ShortPoint2D;

public interface IHealerMovable extends IBuildingWorkerMovable {

	/**
	 * Returns the position this movable will heal other movables at.<br>
	 *
	 * If this healer is outside of an building this function will return null.
	 * @return the heal spot or null
	 */
	ShortPoint2D getHealSpot();
}
