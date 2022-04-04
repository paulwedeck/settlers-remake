package jsettlers.ai.army;

import jsettlers.common.action.EMoveToType;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.MovableManager;
import jsettlers.logic.movable.interfaces.ILogicMovable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HealSoldiersModule extends ArmyModule {

	private static final float[] HEALING_PROP_FACTOR = new float[] {0f, .3f, .7f, 1f, 1f};

	public HealSoldiersModule(ArmyFramework parent) {
		super(parent);
		this.healPropFactor = HEALING_PROP_FACTOR[parent.getPlayer().getPlayerType().ordinal()];
		hospitalWorkRadius = EBuildingType.HOSPITAL.getVariant(parent.getPlayer().getCivilisation()).getWorkRadius();

		// setup woundedSoldiers
		healTroops(null, false);
	}

	@Override
	public void applyHeavyRules(Set<Integer> soldiersWithOrders) {
		soldiersWithOrders.addAll(woundedSoldiers);
	}

	@Override
	public void applyLightRules(Set<Integer> soldiersWithOrders) {
		healTroops(soldiersWithOrders, true);
	}


	private static final float SOLDIERS_MIN_HEALTH = 0.6f;
	private static final float HEALING_DISTANCE_WEIGHT = -1;
	private static final float HEALING_USAGE_WEIGHT = -10;

	private final float healPropFactor;
	private final Set<Integer> woundedSoldiers = new HashSet<>();
	private final Map<ILogicMovable, ShortPoint2D> assignedPatients = new HashMap<>();
	private final Map<ShortPoint2D, Integer> usedHospitalCapacity = new HashMap<>();

	private final int hospitalWorkRadius;


	private void healTroops(Set<Integer> soldiersWithOrders, boolean commit) {
		woundedSoldiers.clear();
		if(healPropFactor == 0) return;

		// this list is at most a couple of seconds old
		Set<ShortPoint2D> hospitals = parent.aiStatistics.getActiveHospitalsForPlayer(parent.getPlayerId());
		if(hospitals.isEmpty()) return;

		Iterator<Map.Entry<ILogicMovable, ShortPoint2D>> woundedIter = assignedPatients.entrySet().iterator();

		// remove dead and healed movables from the patients list
		// regenerate the usedHospitalCapacity
		// unassign patients that are going to destroyed hospitals

		usedHospitalCapacity.clear();
		hospitals.forEach(pt -> usedHospitalCapacity.put(pt, 0));

		while(woundedIter.hasNext()) {
			Map.Entry<ILogicMovable, ShortPoint2D> next = woundedIter.next();
			ShortPoint2D hospital = next.getValue();
			if(!hospitals.contains(hospital)) {
				woundedIter.remove();
				continue;
			}

			ILogicMovable movable = next.getKey();

			if(!isWounded(movable) || !movable.isAlive()) {
				woundedIter.remove();
				continue;
			}

			increaseHospitalUse(hospital);
		}

		// assign newly wounded soldiers to hospitals

		Map<ShortPoint2D, List<Integer>> newOrders = new HashMap<>();
		hospitals.forEach(hospital -> newOrders.put(hospital, new ArrayList<>()));

		MovableManager.getAllMovables().stream()
				.filter(this::isWounded)
				.filter(mov -> mov.getPlayer().equals(parent.getPlayer()))
				.filter(mov -> EMovableType.PLAYER_CONTROLLED_HUMAN_MOVABLE_TYPES
						.contains(mov.getMovableType()))
				// only wounded movables that we actually can heal should be considered
				.forEach(mov -> {
					ShortPoint2D assignedHospital = assignedPatients.get(mov);

					// not all wounded soldiers should be send
					if(assignedHospital != null || randomHealChance()) {
						if (assignedHospital == null) {
							assignedHospital = getBestHospital(mov, hospitals);
							increaseHospitalUse(assignedHospital);
							assignedPatients.put(mov, assignedHospital);
						}

						woundedSoldiers.add(mov.getID());

						if(mov.getPosition().getOnGridDistTo(assignedHospital) >= hospitalWorkRadius) {
							newOrders.get(assignedHospital).add(mov.getID());
						}
					}
				});

		if(commit) {
			newOrders.forEach((key, value) -> parent.sendTroopsToById(value, key, soldiersWithOrders, EMoveToType.FORCED));
		}
	}

	private boolean randomHealChance() {
		if(healPropFactor == 1) return true;

		return MatchConstants.aiRandom().nextFloat() >= 1-healPropFactor;
	}

	private boolean isWounded(ILogicMovable mov) {
		return mov.getHealth() <= SOLDIERS_MIN_HEALTH * mov.getMovableType().getHealth();
	}

	private void increaseHospitalUse(ShortPoint2D hospital) {
		usedHospitalCapacity.compute(hospital, (pos, oldValue) -> oldValue+1);
	}

	private ShortPoint2D getBestHospital(ILogicMovable movable, Set<ShortPoint2D> hospitals) {
		float maxScore = Float.NEGATIVE_INFINITY;
		ShortPoint2D bestHospital = null;

		for (ShortPoint2D hospital : hospitals) {
			float localScore = getHospitalScore(hospital, movable.getPosition());

			if (localScore > maxScore) {
				maxScore = localScore;
				bestHospital = hospital;
			}
		}

		return bestHospital;
	}

	private float getHospitalScore(ShortPoint2D hospital, ShortPoint2D from) {
		int distance = hospital.getOnGridDistTo(from);
		int usage = usedHospitalCapacity.get(hospital);

		float score = 0;

		score += distance * HEALING_DISTANCE_WEIGHT;
		score += usage * HEALING_USAGE_WEIGHT;

		return score;
	}

	public int getWoundedSoldiersCount() {
		return woundedSoldiers.size();
	}
}
