package jsettlers.logic.buildings.workers;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.player.Player;

/**
 * This is a smith building (weapon- or toolsmith) with the ability to show igniting smoke.
 *
 * @author MarviMarv
 */
public final class SmithBuilding extends WorkerAnimationBuilding  {
    private static final long serialVersionUID = -304480934483060498L;

    //{DURATION_SMOKE}
    private static final int[] DURATIONS = {3000};

    public SmithBuilding(EBuildingType type, Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
        super(type, player, position, buildingsGrid);
    }

    @Override
    protected int[] getAnimationDurations() {
        return DURATIONS;
    }

    @Override
    protected int[] getAnimationTransitions() {
        return null;
    }
}