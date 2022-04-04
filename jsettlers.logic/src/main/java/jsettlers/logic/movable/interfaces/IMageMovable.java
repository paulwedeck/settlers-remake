package jsettlers.logic.movable.interfaces;

import jsettlers.common.movable.ESpellType;
import jsettlers.common.position.ShortPoint2D;

public interface IMageMovable extends IAttackableHumanMovable {

	void moveToCast(ShortPoint2D at, ESpellType spell);
}
