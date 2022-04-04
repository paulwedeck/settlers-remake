package jsettlers.common.movable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jsettlers.common.images.ImageLink;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.utils.coordinates.ICoordinatePredicate;

public enum ESpellType {
	ROMAN_EYE(10, false, ECivilisation.ROMAN, "original_14_GUI_246"),
	IRRIGATE(10, true, ECivilisation.ROMAN, "original_14_GUI_249"),
	GREEN_THUMB(15, true, ECivilisation.ROMAN, "original_14_GUI_252"),
	DEFEATISM(15, true, ECivilisation.ROMAN, "original_14_GUI_255"),

	DESERTIFICATION(10, true, ECivilisation.EGYPTIAN, "original_24_GUI_240"),
	DRAIN_MOOR(10, true, ECivilisation.EGYPTIAN, "original_24_GUI_243"),
	CONVERT_FOOD(15, true, ECivilisation.EGYPTIAN, "original_24_GUI_246"),
	BURN_FOREST(15, true, ECivilisation.EGYPTIAN, "original_24_GUI_249"),

	MELT_SNOW(10, true, ECivilisation.ASIAN, "original_34_GUI_255"),
	SUMMON_STONE(10, true, ECivilisation.ASIAN, "original_34_GUI_258"),
	SUMMON_FISH(15, true, ECivilisation.ASIAN, "original_34_GUI_261"),

	AMAZON_EYE(10, false, ECivilisation.AMAZON, "original_44_GUI_249"),
	SUMMON_FOREST(10, true, ECivilisation.AMAZON, "original_44_GUI_252"),
	FREEZE_FOES(15, true, ECivilisation.AMAZON, "original_44_GUI_255"),
	SEND_GOODS(15, false, ECivilisation.AMAZON, "original_44_GUI_258"),

	// common spell
	GIFTS(20, true, null, "original_14_GUI_258"),

	GILDING(20, true, ECivilisation.ROMAN, "original_14_GUI_261"),
	CURSE_MOUNTAIN(25, true, ECivilisation.ROMAN, "original_14_GUI_264"),
	DEFECT(40, true, ECivilisation.ROMAN, "original_14_GUI_267"),

	INCREASE_MORALE(20, true, ECivilisation.EGYPTIAN, "original_24_GUI_255"),
	SEND_FOES(25, false, ECivilisation.EGYPTIAN, "original_24_GUI_258"),
	CURSE_BOWMAN(40, true, ECivilisation.EGYPTIAN, "original_24_GUI_261"),

	MELT_STONE(15, true, ECivilisation.ASIAN, "original_34_GUI_267"),
	SHIELD(20, true, ECivilisation.ASIAN, "original_34_GUI_270"),
	MOTIVATE_SWORDSMAN(25, true, ECivilisation.ASIAN, "original_34_GUI_273"),
	CALL_HELP(40, false, ECivilisation.ASIAN, "original_34_GUI_276"),

	REMOVE_GOLD(20, true, ECivilisation.AMAZON, "original_44_GUI_264"),
	CALL_GOODS(25, false, ECivilisation.AMAZON, "original_44_GUI_267"),
	DESTROY_ARROWS(40, true, ECivilisation.AMAZON, "original_44_GUI_270");

	private short manna;
	private ImageLink imageLink;
	private boolean forcePresence;

	private ECivilisation civ;

	ESpellType(int manna, boolean forcePresence, ECivilisation civ, String imageLink) {
		this.civ = civ;
		this.manna = (short) manna;
		this.forcePresence = forcePresence;
		this.imageLink = ImageLink.fromName(imageLink, 0);
	}

	public boolean availableForCiv(ECivilisation civilisation) {
		return civ == null || civ == civilisation;
	}

	public ImageLink getImageLink() {
		return imageLink;
	}

	public short getBaseCost() {
		return manna;
	}

	public short getMannaCost(int count) {
		return (short)(manna*(count/10f+1));
	}

	public boolean forcePresence() {
		return forcePresence;
	}

	public static final int SEND_GOODS_MAX = 40;
	public static final int CALL_GOODS_MAX = 40;
	public static final int REMOVE_GOLD_MAX_GOLD = 40;
	public static final int GILDING_MAX_IRON = 40;
	public static final int CONVERT_FOOD_MAX_FISH = 40;
	public static final int CONVERT_IRON_MAX_STONE = 40;

	public static final int DEFEATISM_MAX_SOLDIERS = 20;
	public static final int INCREASE_MORALE_MAX_SOLDIERS = 20;
	public static final int SEND_FOES_MAX_SOLDIERS = 20;
	public static final int CALL_HELP_MAX_SOLDIERS = 20;
	public static final int CURSE_BOWMAN_MAX_BOWMAN = 20;
	public static final int DESTROY_ARROWS_MAX_BOWMAN = 20;
	public static final int SHIELD_MAX_SOLDIERS = 20;
	public static final int FREEZE_FOES_MAX_SOLDIERS = 20;

	public static final int GIFTS_MAX_STACKS = 5;
	public static final int GIFTS_RADIUS = 3;

	public static final int SUMMON_FISH_RADIUS = 5;
	public static final float SUMMON_FISH_RESOURCE_ADD = 0.5f;

	public static final int CURSE_MOUNTAIN_RADIUS = 5;
	public static final float CURSE_MOUNTAIN_RESOURCE_MOD = 0.5f;


	public static final long DEFECT_MAX_ENEMIES = 10;
	public static final long GREEN_THUMB_MAX_SETTLERS = 1;

	public static final int IRRIGATE_RADIUS = 7;
	public static final int DESERTIFICATION_RADIUS = 7;
	public static final int DRAIN_MOOR_RADIUS = 7;
	public static final int MELT_SNOW_RADIUS = 7;

	// don't place stone in a blob that nobody can pass
	public static final int SUMMON_STONE_OFFSET = 10;

	public static final int SUMMON_STONE_RADIUS = 10;
	public static final int SUMMON_FOREST_RADIUS = 7;

	public static final short ROMAN_EYE_RADIUS = 10;
	public static final float ROMAN_EYE_TIME = 6;

	public static final short AMAZON_EYE_RADIUS = (short)-1; // -1 will show whole map
	public static final float AMAZON_EYE_TIME = 6;

	public static final int BURN_FOREST_MAX_TREE_COUNT = 5;

	private static final ESpellType[][] spellsForCivilisation = new ESpellType[ECivilisation.VALUES.length][];

	static {
		List<ESpellType> civSpells = new ArrayList<>();
		for(ECivilisation civilisation : ECivilisation.VALUES) {
			for(ESpellType spell : ESpellType.values()) {
				if(spell.availableForCiv(civilisation)) civSpells.add(spell);
			}

			spellsForCivilisation[civilisation.ordinal] = civSpells.toArray(new ESpellType[0]);
			civSpells.clear();
		}
	}

	public static ESpellType[] getSpellsForCivilisation(ECivilisation civilisation) {
		return spellsForCivilisation[civilisation.ordinal];
	}
}
