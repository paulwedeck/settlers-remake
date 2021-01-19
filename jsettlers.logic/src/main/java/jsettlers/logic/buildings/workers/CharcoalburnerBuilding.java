package jsettlers.logic.buildings.workers;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.player.Player;

/**
 * This is a charcoal burner building with the ability to show smoke.
 *
 * @author MarviMarv
 */
public final class CharcoalburnerBuilding extends WorkerAnimationBuilding  {
    private static final long serialVersionUID = -2652624466987173515L;

    //{DURATION_SMOKE}
    private static final int[] DURATIONS = {40000};
    //TODO: create transition for smoke start, running, end in order to have fluent animation
    private static final int[] TRANSITIONS = {};

    public CharcoalburnerBuilding(EBuildingType type, Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
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