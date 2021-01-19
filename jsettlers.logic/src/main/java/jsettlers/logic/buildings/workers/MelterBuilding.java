package jsettlers.logic.buildings.workers;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.player.Player;

/**
 * This is a melter building (iron or gold) with the ability to melt and smoke.
 * 
 * @author MarviMarv
 */
public final class MelterBuilding extends WorkerAnimationBuilding  {
	private static final long serialVersionUID = -939588556733030645L;

	//{DURATION_SMOKE, DURATION_MELT_PROGRESS, DURATION_MELT_RESULT}
	private static final int[] DURATIONS = {3500, 1500, 6000};
	//{DURATION_SMOKE -> DURATION_MELT_PROGRESS, DURATION_MELT_PROGRESS -> DURATION_MELT_RESULT}
	private static final int[] TRANSITIONS = {2000, 1500};

	public MelterBuilding(EBuildingType type, Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
		super(type, player, position, buildingsGrid);
	}

	@Override
	protected int[] getAnimationDurations() {
		return DURATIONS;
	}

	@Override
	protected int[] getAnimationTransitions() {
		return TRANSITIONS;
	}
}
