package jsettlers.logic.movable.military;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import java8.util.Lists;
import java8.util.function.Function;
import jsettlers.algorithms.terraform.LandscapeEditor;
import jsettlers.common.action.EMoveToType;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.map.shapes.MapCircle;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.menu.messages.SimpleMessage;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESpellType;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.utils.coordinates.CoordinateStream;
import jsettlers.common.utils.mutables.MutableInt;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.constants.MatchConstants;
import jsettlers.logic.movable.interfaces.IAttackableHumanMovable;
import jsettlers.logic.movable.interfaces.IBowmanMovable;
import jsettlers.logic.movable.interfaces.ILogicMovable;
import jsettlers.logic.movable.other.AttackableHumanMovable;
import jsettlers.logic.movable.Movable;
import jsettlers.logic.movable.interfaces.AbstractMovableGrid;
import jsettlers.logic.movable.interfaces.IMageMovable;
import jsettlers.logic.player.Player;

import static jsettlers.common.landscape.ELandscapeType.DESERT;
import static jsettlers.common.landscape.ELandscapeType.DESERT_TYPES;
import static jsettlers.common.landscape.ELandscapeType.FLATTENED;
import static jsettlers.common.landscape.ELandscapeType.FLATTENED_DESERT;
import static jsettlers.common.landscape.ELandscapeType.FLATTENED_DESERTS;
import static jsettlers.common.landscape.ELandscapeType.GRASS;
import static jsettlers.common.landscape.ELandscapeType.MOOR_TYPES;
import static jsettlers.common.landscape.ELandscapeType.MOUNTAIN;
import static jsettlers.common.landscape.ELandscapeType.SNOW_TYPES;

public class MageMovable extends AttackableHumanMovable implements IMageMovable {

	private transient final LandscapeEditor terraformHandle = new LandscapeEditor(pt -> grid.getLandscapeTypeAt(pt.x, pt.y), (pt, type) -> grid.setLandscape(pt.x, pt.y, type));

	private ShortPoint2D spellLocation = null;
	private ESpellType spell = null;

	public MageMovable(AbstractMovableGrid grid, ShortPoint2D position, Player player, Movable movable) {
		super(grid, EMovableType.MAGE, position, player, movable);
	}

	@Override
	public void moveToCast(ShortPoint2D at, ESpellType spell) {
		super.moveTo(at, EMoveToType.DEFAULT);
		this.spell = spell;
		this.spellLocation = at;
	}


	/**
	 * Tests if this movable can receive moveTo requests and if so, directs it to go to the given position.
	 *
	 * @param targetPosition
	 * 		Desired position the movable should move to
	 */
	@Override
	public void moveTo(ShortPoint2D targetPosition, EMoveToType moveToType) {
		super.moveTo(targetPosition, moveToType);
		this.spell = null;
		this.spellLocation = null;
	}

	private CoordinateStream spellRegion() {
		return spellRegion(Constants.SPELL_EFFECT_RADIUS);
	}

	private CoordinateStream spellRegion(int radius) {
		return spellRegion(spellLocation, radius);
	}

	private CoordinateStream spellRegion(ShortPoint2D at, int radius) {
		return new MapCircle(at, radius).stream()
				.filterBounds(grid.getWidth(), grid.getHeight());
	}

	private CoordinateStream sort(CoordinateStream stream) {

		List<ShortPoint2D> points = stream.toList();
		Lists.sort(points, (pt1, pt2) -> pt1.getOnGridDistTo(position)-pt2.getOnGridDistTo(position));
		return CoordinateStream.fromList(points);
	}

	private int teamId(int x, int y) {
		IPlayer player = grid.getPlayerAt(new ShortPoint2D(x, y));
		return player != null ? player.getTeamId() : -1;
	}

	private int teamId(ILogicMovable movable) {
		return movable.getPlayer().getTeamId();
	}

	private int teamId() {
		return player.getTeamId();
	}

	@Override
	protected void action() {
		if(!willCastSpell()) return;

		int sound = -1;
		int animation = -1;
		float duration = 2;
		List<ShortPoint2D> effectLocations = new ArrayList<>();

		if(player.getMannaInformation().useSpell(spell)) {
			switch(spell) {
				case SEND_GOODS:
					transferStacks(position, spellLocation, false, ESpellType.SEND_GOODS_MAX);
					break;
				case CALL_GOODS:
					transferStacks(spellLocation, position, true, ESpellType.CALL_GOODS_MAX);
					break;
				case REMOVE_GOLD:
					convertMaterial(EMaterialType.GOLD, EMaterialType.STONE, ESpellType.REMOVE_GOLD_MAX_GOLD, effectLocations);

					sound = 95;
					animation = 121;
					break;
				case GILDING:
					convertMaterial(EMaterialType.IRON, EMaterialType.GOLD, ESpellType.GILDING_MAX_IRON, effectLocations);

					sound = 95;
					animation = 121;
					break;
				case CONVERT_FOOD:
					convertMaterial(EMaterialType.FISH, EMaterialType.MEAT, ESpellType.CONVERT_FOOD_MAX_FISH, effectLocations);

					sound = 95;
					animation = 121;
					break;
				case MELT_STONE:
					convertMaterial(EMaterialType.STONE, EMaterialType.IRON, ESpellType.CONVERT_IRON_MAX_STONE, effectLocations);

					sound = 95;
					animation = 121;
					break;
				case GREEN_THUMB:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive())
							.filter(lm -> lm.getMovableType() == EMovableType.FARMER ||
									lm.getMovableType() == EMovableType.WINEGROWER ||
									lm.getMovableType() == EMovableType.FORESTER
							).limit(ESpellType.GREEN_THUMB_MAX_SETTLERS)
							.forEach(lm -> lm.addEffect(EEffectType.GREEN_THUMB));
					effectLocations.add(spellLocation);
					break;
				case DEFEATISM:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.getMovableType().isSoldier())
							.filter(lm -> teamId(lm) != teamId(this))
							.limit(ESpellType.DEFEATISM_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.DEFEATISM));
					effectLocations.add(spellLocation);
					animation = 116;
					break;
				case INCREASE_MORALE:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.getMovableType().isSoldier())
							.filter(lm -> teamId(lm) == teamId())
							.limit(ESpellType.INCREASE_MORALE_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.INCREASED_MORALE));
					effectLocations.add(spellLocation);
					animation = 115;
					break;
				case MOTIVATE_SWORDSMAN:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.getMovableType().isSwordsman())
							.filter(lm -> teamId(lm) == teamId())
							.limit(ESpellType.INCREASE_MORALE_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.MOTIVATE_SWORDSMAN));
					effectLocations.add(spellLocation);
					animation = 115;
					break;
				case SHIELD:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive())
							.filter(lm -> EMovableType.PLAYER_CONTROLLED_HUMAN_MOVABLE_TYPES.contains(lm.getMovableType()))
							.filter(lm -> teamId(lm) == teamId())
							.limit(ESpellType.SHIELD_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.SHIELDED));
					effectLocations.add(spellLocation);
					animation = 116;
					break;
				case DESTROY_ARROWS:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive()&&lm.getMovableType().isBowman())
							.filter(lm -> teamId(lm) != teamId())
							.limit(ESpellType.DESTROY_ARROWS_MAX_BOWMAN)
							.forEach(movable -> movable.addEffect(EEffectType.NO_ARROWS));
					effectLocations.add(spellLocation);
					animation = 116;
					break;
				case FREEZE_FOES:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive())
							.filter(lm -> EMovableType.PLAYER_CONTROLLED_HUMAN_MOVABLE_TYPES.contains(lm.getMovableType()))
							.filter(lm -> teamId(lm) != teamId())
							.limit(ESpellType.FREEZE_FOES_MAX_SOLDIERS)
							.forEach(movable -> movable.addEffect(EEffectType.FROZEN));
					effectLocations.add(spellLocation);
					animation = 116;
					break;
				case SEND_FOES:
					Queue<ILogicMovable> sendEnemies = new ArrayDeque<>(ESpellType.SEND_FOES_MAX_SOLDIERS);
					sort(spellRegion(position, Constants.SPELL_EFFECT_RADIUS))
							.map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive())
							.filter(lm -> teamId(lm) != teamId())
							.filter(lm -> EMovableType.PLAYER_CONTROLLED_HUMAN_MOVABLE_TYPES.contains(lm.getMovableType()))
							.limit(ESpellType.SEND_FOES_MAX_SOLDIERS)
							.forEach(sendEnemies::add);


					sort(spellRegion()).filter((x, y) -> !grid.isBlockedOrProtected(x, y))
							.forEach((x, y) -> {
								ILogicMovable send = sendEnemies.poll();
								if(send != null) {
									ShortPoint2D movPos = send.getPosition();
									send.setPosition(new ShortPoint2D(x, y));
									effectLocations.add(movPos);
								}
							});
					animation = 121;
					break;
				case CALL_HELP:
					Queue<ILogicMovable> callDefenders = new ArrayDeque<>(ESpellType.CALL_HELP_MAX_SOLDIERS);
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive())
							.filter(lm -> teamId(lm) == teamId())
							.filter(lm -> EMovableType.PLAYER_CONTROLLED_HUMAN_MOVABLE_TYPES.contains(lm.getMovableType()))
							.limit(ESpellType.CALL_HELP_MAX_SOLDIERS)
							.forEach(callDefenders::add);


					sort(spellRegion(position, Constants.SPELL_EFFECT_RADIUS))
							.filter((x, y) -> !grid.isBlockedOrProtected(x, y))
							.forEach((x, y) -> {
								ILogicMovable send = callDefenders.poll();
								if(send != null) {
									ShortPoint2D movPos = send.getPosition();
									send.setPosition(new ShortPoint2D(x, y));
									effectLocations.add(movPos);
								}
							});
					animation = 121;
				case CURSE_BOWMAN:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm!=null&&lm.isAlive()&&lm instanceof IBowmanMovable)
							.map(lm -> (IBowmanMovable)lm)
							.filter(lm -> teamId(lm) != teamId())
							.limit(ESpellType.CURSE_BOWMAN_MAX_BOWMAN)
							.forEach(IBowmanMovable::convertToPioneer);
					break;
				case GIFTS:
					spellRegion(ESpellType.GIFTS_RADIUS).filter((x, y) -> !grid.isBlockedOrProtected(x, y))
							.filter((x, y) -> teamId(x, y) == -1 || teamId(x, y) == teamId())
							.limit(MatchConstants.random().nextInt(ESpellType.GIFTS_MAX_STACKS+1))
							.forEach((x, y) -> {
								ShortPoint2D at = new ShortPoint2D(x, y);
								//TODO only give useful stuff
								EMaterialType type = EMaterialType.values()[MatchConstants.random().nextInt(EMaterialType.values().length)];
								int size = MatchConstants.random().nextInt(9);
								for(int i = 0; i != size; i++) grid.dropMaterial(at, type, true, false);
								effectLocations.add(at);
							});
					duration = 1;
					sound = 78;
					animation = 114;
					break;
				case CURSE_MOUNTAIN:
					spellRegion(ESpellType.CURSE_MOUNTAIN_RADIUS)
							.filter((x, y) -> teamId(x, y) == -1 || teamId(x, y) != teamId())
							.forEach((x, y) -> grid.tryCursingLocation(new ShortPoint2D(x, y)));
					effectLocations.add(spellLocation);
					sound = 100;
					animation = 120;
					break;
				case SUMMON_FISH:
					spellRegion(ESpellType.SUMMON_FISH_RADIUS)
							.filter((x, y) -> grid.getLandscapeTypeAt(x, y) == ELandscapeType.WATER1)
							.forEach((x, y) -> grid.trySummonFish(new ShortPoint2D(x, y)));
					effectLocations.add(spellLocation);
					break;
				case DEFECT:
					sort(spellRegion()).map(grid::getMovableAt)
							.filter(lm -> lm instanceof IAttackableHumanMovable)
							.map(lm -> (IAttackableHumanMovable)lm)
							.filter(ILogicMovable::isAlive)
							.filter(lm -> teamId(lm) != teamId())
							.limit(ESpellType.DEFECT_MAX_ENEMIES)
							.forEach(lm -> {
								lm.defectTo(player);
								effectLocations.add(lm.getPosition());
							});
					sound = 95;
					animation = 119;
					break;
				case IRRIGATE:
					convertLandscape(ESpellType.IRRIGATE_RADIUS, DESERT_TYPES::contains, GRASS, FLATTENED_DESERTS::contains, FLATTENED);

					effectLocations.add(spellLocation);
					animation = 125;
					break;
				case DESERTIFICATION:
					convertLandscape(ESpellType.DESERTIFICATION_RADIUS, ELandscapeType::isGrass, DESERT, (type) -> type == FLATTENED, FLATTENED_DESERT);

					effectLocations.add(spellLocation);
					animation = 127;
					break;
				case DRAIN_MOOR:
					convertLandscape(ESpellType.DRAIN_MOOR_RADIUS, MOOR_TYPES::contains, GRASS, null, null);

					effectLocations.add(spellLocation);
					animation = 127;
					break;
				case MELT_SNOW:
					convertLandscape(ESpellType.MELT_SNOW_RADIUS, SNOW_TYPES::contains, MOUNTAIN, null, null);

					effectLocations.add(spellLocation);
					animation = 127;
					break;
				case AMAZON_EYE:
					grid.addEyeMapObject(position, ESpellType.AMAZON_EYE_RADIUS, ESpellType.AMAZON_EYE_TIME, player);
					effectLocations.add(position);
					sound = 80;
					animation = 126;
					break;
				case ROMAN_EYE:
					grid.addEyeMapObject(spellLocation, ESpellType.ROMAN_EYE_RADIUS, ESpellType.ROMAN_EYE_TIME, player);
					effectLocations.add(spellLocation);
					sound = 80;
					animation = 126;
					break;
				case BURN_FOREST:
					MutableInt remainingTrees = new MutableInt(ESpellType.BURN_FOREST_MAX_TREE_COUNT);
					sort(spellRegion()).forEach((x, y) -> {
						if(remainingTrees.value > 0 && grid.executeSearchType(this, new ShortPoint2D(x, y), ESearchType.BURNABLE_TREE)) {
							remainingTrees.value--;
						}
					});
					break;
				case SUMMON_STONE:
					spellRegion(ESpellType.SUMMON_STONE_RADIUS)
							.getEvery(ESpellType.SUMMON_STONE_OFFSET)
							.forEach((x, y) -> {
								grid.executeSearchType(this, new ShortPoint2D(x, y), ESearchType.SUMMON_STONE);
							});
					break;
				case SUMMON_FOREST:
					spellRegion(ESpellType.SUMMON_FOREST_RADIUS).forEach((x, y) -> {
						grid.executeSearchType(this, new ShortPoint2D(x, y), ESearchType.PLANTABLE_TREE);
					});
					break;
			}

			if(animation != -1) {
				for (ShortPoint2D point : effectLocations) {
					grid.addSelfDeletingMapObject(point, sound, animation, duration, player);
				}
			}

		} else {
			player.showMessage(SimpleMessage.castFailed(spellLocation, "spell_failed"));
		}

		playAction(EMovableAction.ACTION1, 1);
		spellLocation = null;
		spell = null;
	}

	private void transferStacks(ShortPoint2D from, ShortPoint2D to, boolean checkFromOwner, int maxGoods) {
		CoordinateStream stream = sort(spellRegion(from, Constants.SPELL_EFFECT_RADIUS));
		if(checkFromOwner) stream = stream.filter((x, y) -> player.hasSameTeam(grid.getPlayerAt(new ShortPoint2D(x, y))));

		MutableInt remaining = new MutableInt(maxGoods);
		stream.forEach((x, y) -> {
			EMaterialType took;
			while(remaining.value > 0 && (took = grid.takeMaterial(new ShortPoint2D(x, y))) != null) {
				grid.dropMaterial(to, took, true, true);
				remaining.value--;
			}
		});
	}

	private void convertLandscape(int radius, Function<ELandscapeType, Boolean> fromCond, ELandscapeType to, Function<ELandscapeType, Boolean> fromFlatCond, ELandscapeType toFlat) {
		List<ShortPoint2D> flattenedRegion = null;

		CoordinateStream affectedRegion = spellRegion(radius)
				.filter((x, y) -> fromCond.apply(grid.getLandscapeTypeAt(x, y)));

		if(fromFlatCond != null) {
			flattenedRegion = affectedRegion.filter((x, y) -> fromFlatCond.apply(grid.getLandscapeTypeAt(x, y))).toList();
		}

		terraformHandle.fill(to, affectedRegion.toList());

		if(fromFlatCond != null) {
			terraformHandle.fill(toFlat, flattenedRegion);
		}
	}

	private void convertMaterial(EMaterialType from, EMaterialType to, int limit, List<ShortPoint2D> effects) {
		final MutableInt materialCount = new MutableInt(0);
		sort(spellRegion()).forEach((x, y) -> {
			ShortPoint2D pos = new ShortPoint2D(x, y);
			boolean tookSomething = false;

			while(grid.takeMaterial(pos, from) && materialCount.value <= limit) {
				materialCount.value++;
				tookSomething = true;
			}
			if(tookSomething) effects.add(pos);
		});

		sort(spellRegion()).forEach((x, y) -> {
			ShortPoint2D pos = new ShortPoint2D(x, y);

			while(materialCount.value > 0 && grid.dropMaterial(pos, to, true, false)) {
				materialCount.value--;
			}
		});
	}

	private boolean willCastSpell() {
		return spell != null && (!spell.forcePresence() || position.getOnGridDistTo(spellLocation) <= Constants.MAGE_CAST_DISTANCE);
	}

	@Override
	protected boolean checkPathStepPreconditions(ShortPoint2D pathTarget, int step, EMoveToType moveToType) {
		return !willCastSpell();
	}

	@Override
	public void stopOrStartWorking(boolean stop) {
		if(stop) {
			moveTo(position, EMoveToType.FORCED);
		}
	}
}