package jsettlers.logic.movable.interfaces;

import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;

public interface ISoldierMovable extends IAttackableHumanMovable {

	boolean moveToTower(IOccupyableBuilding building);

	void leaveTower(ShortPoint2D newPosition);

	/**
	 * This method is called when this movable has to defend it's building at the given position.
	 *
	 */
	void defendTowerAt();
}
