package jsettlers.logic.movable.interfaces;

import jsettlers.logic.buildings.military.occupying.IOccupyableBuilding;
import jsettlers.shared.ShortPoint2D;

public interface ISoldierMovable extends IAttackableHumanMovable {

	boolean moveToTower(IOccupyableBuilding building);

	void leaveTower(ShortPoint2D newPosition);

	/**
	 * This method is called when this movable has to defend it's building at the given position.
	 *
	 */
	void defendTowerAt();
}
