package jsettlers.graphics.map.draw;

/**
 * gfx file constants
 * arrays contain civilisation specific data in following order {roman, egyptian, asian, amazon}
 * FILE_* = gfx file index
 * SEQ_* = gfx sequence index
 * XY_OFFSET_* = offset relatively drawn to specific gfx position
 *
 * @author MarviMarv
 *
 */
public final class GfxConstants {

    private GfxConstants() {
    }

    /**
     * File references
     */
    public static final int FILE_OBJECT = 1;
    public static final int FILE_OBJECT_ANIMAL = 6;
    public static final int[] FILE_WORKER_BEARER = {10, 20, 30, 40};
    public static final int[] FILE_WORKER_PROFESSION = {11, 21, 31, 41};
    public static final int[] FILE_MILITARY = {12, 22, 32, 42};
    public static final int[] FILE_BUILDING = {13, 23, 33, 43};
    public static final int[] FILE_BUILDING_GUI = {14, 24, 34, 44};

    /**
     * Buildings
     */
    public static final int[] SEQ_MILL_ROTATION = {15, 22, 21, 20};
    //public static final int DURATION_MILL_ROTATION = 5000; //this is the original duration of the mill rotation, currently set by the building xml

    public static final int COUNT_MELTER_IMAGES = 25;
    public static final int[] SEQ_MELTER_GOLD = {36, 36, 38, 39};
    public static final int[] SEQ_MELTER_IRON = {37, 37, 39, 40};
    public static final float Z_MELT_RESULT = 0f;

    public static final int[] COUNT_SMOKE_IMAGES = {36, 29, 29, 36};
    public static final int[] SEQ_SMOKE = {42, 42, 44, 45};
    public static final float Z_SMOKE = 0.9f;

    public static final int COUNT_SMOKESMITH_IMAGES = 40;
    public static final int[] SEQ_SMOKESMITH = {43, 44, 46, 46};

    //use the BuildingCreatorApp in order to find the tile where you want to draw relatively to the building's position
    //TODO: the tile system is a huge problem for placing animations correctly, is there a way to draw meticulous?
    public static final int[][] XY_OFFSET_SMOKE_IRONMELT = {{-7, -9},{-5, -6},{-6, -8},{-6, -9}};
    public static final int[][] XY_OFFSET_METAL_IRONMELT = {{0, 0},{0, 0},{0, 0},{0, 0}};
    public static final int[][] XY_OFFSET_SMOKE_GOLDMELT = {{-7, -10},{-5, -6},{-5, -5},{-7, -10}};
    public static final int[][] XY_OFFSET_METAL_GOLDMELT = {{0, 0},{0, 0},{0, 0},{0, 0}};

    public static final int[][] XY_OFFSET_SMOKE_BAKER = {{-3, -5},{1, -2},{-2, -3},{-1, -7}};

    public static final int[] XY_OFFSET_SMOKE_CHARCOALBURNER = {-4, -6};

    public static final int[][] XY_OFFSET_SMOKESMITH_WEAPONSMITH = {{-3, -2},{-3, -2},{-3, -2},{-3, -2}};
    public static final int[][] XY_OFFSET_SMOKESMITH_TOOLSMITH = {{-3, -2},{-2, -1},{-2, -1},{-3, -2}};

    /**
     * Objects
     */
    //Corn
    public static final int SEQ_CORN = 23;
    public static final int COUNT_CORN_GROW_STEPS = 6; //Corn is already harvestable beginning from image 6
    public static final int INDEX_CORN_POST_GROWTH = 7;
    public static final int INDEX_CORN_DEAD_STEP = 8;

    //Wine
    public static final int SEQ_WINE = 25;
    public static final int COUNT_WINE_GROW_STEPS = 3;
    public static final int INDEX_WINE_DEAD_STEP = 0;

    //Rice
    public static final int SEQ_RICE = 24;
    public static final int COUNT_RICE_GROW_STEPS = 4;
    public static final int INDEX_RICE_DEAD_STEP = 5;

    //Hive
    public static final int SEQ_HIVE_EMPTY = 8;
    public static final int SEQ_HIVE_LAST = 14;
    public static final int[] SEQ_HIVE_GROW = {9, 10, 11, 12, 13, 14};
    public static final int SOUND_BEES_INDEX = 117; //TODO: add sound in MapObjectDrawer

    //Temple Mana Bowl
    public static final int[] SEQ_TEMPLE_MANA_BOWL = {46, 47, 49, 49};
    public static final int COUNT_TEMPLE_MANA_BOWL_IMAGES = 9;
}
