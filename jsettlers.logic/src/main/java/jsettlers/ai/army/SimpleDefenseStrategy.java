package jsettlers.ai.army;

import java.util.Set;

public class SimpleDefenseStrategy extends SimpleStrategy {

	public SimpleDefenseStrategy(ArmyFramework parent) {
		super(parent);
	}

	@Override
	public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
		if (parent.getEnemiesInTown().size() > 0) {
			SoldierPositions soldierPositions = new SoldierPositions(parent.getPlayerId(), soldiersWithOrders);
			defend(soldierPositions, soldiersWithOrders);
		}
	}

	@Override
	public void applyLightRules(Set<Integer> soldiersWithOrders) {

	}
}
