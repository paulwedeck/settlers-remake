package jsettlers.logic.buildings.workers;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.player.Player;

/**
 * This is a baker building with the ability to show smoke.
 *
 * @author MarviMarv
 */
public final class BakerBuilding extends WorkerAnimationBuilding  {
    private static final long serialVersionUID = 5293189298460881825L;

    //{DURATION_SMOKE}
    private static final int[] DURATIONS = {7000};

    public BakerBuilding(EBuildingType type, Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
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