/*******************************************************************************
 * Copyright (c) 2015 - 2018
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.graphics.map.draw;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

import go.graphics.GLDrawContext;
import jsettlers.common.CommonConstants;
import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.buildings.IBuilding.IOccupied;
import jsettlers.common.buildings.IBuildingOccupier;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.images.EImageLinkType;
import jsettlers.common.images.ImageLink;
import jsettlers.common.images.OriginalImageLink;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.mapobject.IArrowMapObject;
import jsettlers.common.mapobject.IAttackableTowerMapObject;
import jsettlers.common.mapobject.IMannaBowlObject;
import jsettlers.common.mapobject.IMapObject;
import jsettlers.common.mapobject.ISpecializedMapObject;
import jsettlers.common.mapobject.IStackMapObject;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESoldierClass;
import jsettlers.common.movable.IGraphicsBuildingWorker;
import jsettlers.common.movable.IGraphicsCargoShip;
import jsettlers.common.movable.IGraphicsFerry;
import jsettlers.common.movable.IGraphicsMovable;
import jsettlers.common.movable.IGraphicsThief;
import jsettlers.common.movable.IShipInConstruction;
import jsettlers.common.player.IInGamePlayer;
import jsettlers.common.player.IPlayer;
import jsettlers.common.player.IPlayerable;
import jsettlers.common.sound.ISoundable;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SettlerImage;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.sequence.Sequence;
import jsettlers.graphics.localization.Labels;
import jsettlers.graphics.map.MapDrawContext;
import jsettlers.graphics.map.draw.settlerimages.SettlerImageMap;
import jsettlers.graphics.map.geometry.MapCoordinateConverter;
import jsettlers.graphics.sound.SoundManager;
import jsettlers.shared.Color;
import jsettlers.shared.ShortPoint2D;

/**
 * This class handles drawing of objects on the map.
 *
 * @author michael
 * @author MarviMarv
 */
public class MapObjectDrawer {

	private static final int[] PASSENGER_POSITION_TO_FRONT = {
		2,
		-2,
		-2,
		1,
		1,
		-1,
		-1};
	private static final int[] PASSENGER_POSITION_TO_RIGHT = {
		0,
		1,
		-1,
		-1,
		1,
		1,
		-1};
	private static final int   maxNumberOfPassengers       = PASSENGER_POSITION_TO_FRONT.length;
	private static final int   PASSENGER_DECK_HEIGHT       = 10;
	private static final int[] CARGO_POSITION_TO_FRONT     = {
		0,
		3,
		-2};
	private static final int[] CARGO_POSITION_TO_RIGHT     = {
		0,
		0,
		0};
	private static final int   maxNumberOfStacks           = CARGO_POSITION_TO_FRONT.length;
	private static final int   CARGO_DECK_HEIGHT           = 18;

	private static final int SOUND_MILL               = 42;
	private static final int SOUND_SLAUGHTERHOUSE     = 14;
	private static final int SOUND_BUILDING_DESTROYED = 93;
	private static final int SOUND_SETTLER_KILLED     = 35;
	private static final int SOUND_FALLING_TREE       = 36;


	private static final int OBJECTS_FILE   = 1;
	private static final int BUILDINGS_FILE = 13;

	private static final int   TREE_TYPES              = 7;
	private static final int[] TREE_SEQUENCES          = new int[]{
		1,
		2,
		4,
		7,
		8,
		16,
		17};
	private static final int[] TREE_CHANGING_SEQUENCES = new int[]{
		3,
		3,
		6,
		9,
		9,
		18,
		18};
	private static final float TREE_CUT_1              = 0.03F;
	private static final float TREE_CUT_2              = 0.06F;
	private static final float TREE_CUT_3              = 0.09F;
	private static final float TREE_TAKEN              = 0.1F;

	/**
	 * First images in tree cutting sequence.
	 */
	private static final int TREE_FALL_IMAGES = 4;

	/**
	 * Tree falling speed. bigger => faster.
	 */
	private static final float TREE_FALLING_SPEED = 1 / 0.001f;
	private static final int   TREE_ROT_IMAGES    = 4;
	private static final int   TREE_SMALL         = 12;
	private static final int   TREE_MEDIUM        = 11;
	private static final int   SMALL_GROWING_TREE = 22;

	private static final int CORN            = 23;
	private static final int CORN_GROW_STEPS = 7;
	private static final int CORN_DEAD_STEP  = 8;

	private static final int WINE            = 25;
	private static final int WINE_GROW_STEPS = 3;
	private static final int WINE_DEAD_STEP  = 0;

	private static final int WINE_BOWL_SEQUENCE = 46;
	private static final int WINE_BOWL_IMAGES   = 9;

	private static final int RICE			 = 24;
	private static final int RICE_GROW_STEPS = 5;
	private static final int RICE_DEAD_STEP  = 0;

	public static final int HIVE_EMPTY = 8;
	public static final int HIVE_LAST = 14;
	public static final int[] HIVE_GROW = {9, 10, 11, 12, 13, 14};


	private static final int WAVES = 26;

	private static final int FILE_BORDER_POST = 13;

	private static final int STONE = 31;

	private static final int SELECT_MARK_SEQUENCE = 11;
	private static final int SELECT_MARK_FILE     = 4;

	private static final int PIG_SEQ      = 0;
	private static final int ANIMALS_FILE = 6;
	private static final int FISH_SEQ     = 7;

	private static final int MOVE_TO_MARKER_SEQUENCE = 0;
	private static final int MARKER_FILE             = 3;

	private static final float CONSTRUCTION_MARK_Z         = 0.92f;
	private static final float PLACEMENT_BUILDING_Z        = 0.91f;
	private static final float MOVABLE_SELECTION_MARKER_Z  = 0.9f;
	private static final float BUILDING_SELECTION_MARKER_Z = 0.9f;
	private static final float FLAG_ROOF_Z                 = 0.89f;
	private static final float SMOKE_Z                     = 0.9f;
	private static final float BACKGROUND_Z                = -0.1f;
	private final float z_per_y;
	private final float shadow_offset;
	private final float construction_offset;
	private final float molten_metal_offset;
	private final float tower_front_offset;

	private static final int SMOKE_HEIGHT = 30;

	private static final int FLAG_FILE = 13;
	private final SoundManager   sound;
	private final MapDrawContext context;
	private byte[][] visibleGrid = null;

	/**
	 * An animation counter, used for trees and other waving/animated things.
	 */
	private int             animationStep = 0;
	/**
	 * The image provider that supplies us with the images we need.
	 */
	private ImageProvider   imageProvider;
	private SettlerImageMap imageMap;
	private float           betweenTilesY;
	private Image playerBorderObjectImage;
	private IInGamePlayer localPlayer;

	/**
	 * Creates a new {@link MapObjectDrawer}.
	 * @param context
	 * 		The context to use for computing the positions.
	 * @param sound
	 * @param localPlayer
	 */
	public MapObjectDrawer(MapDrawContext context, SoundManager sound, IInGamePlayer localPlayer) {
		this.localPlayer = localPlayer;
		this.context = context;
		this.sound = sound;

		z_per_y = 1f/(context.getMap().getHeight()*100);
		shadow_offset = 20 * z_per_y;
		construction_offset = z_per_y;
		molten_metal_offset = z_per_y;
		tower_front_offset = z_per_y / 2;
	}

	public void setVisibleGrid(byte[][] visibleGrid) {
		this.visibleGrid = visibleGrid;
	}

	/**
	 * Draws a map object at a given position.
	 *
	 * @param x
	 * 		THe position to draw the object.
	 * @param y
	 * 		THe position to draw the object.
	 * @param object
	 * 		The object (tree, ...) to draw.
	 */
	public void drawMapObject(int x, int y, IMapObject object) {
		byte fogStatus = visibleGrid != null ? visibleGrid[x][y] : CommonConstants.FOG_OF_WAR_VISIBLE;
		if (fogStatus == 0) {
			return; // break
		}
		float color = getColor(fogStatus);

		drawObject(x, y, object, color);

		if (object.getNextObject() != null) {
			drawMapObject(x, y, object.getNextObject());
		}
	}

	public void drawDock(int x, int y, IMapObject object) {
		byte fogStatus = context.getVisibleStatus(x, y);
		if (fogStatus == 0) {
			return;
		}
		float color = getColor(fogStatus);
		Image image = imageProvider.getImage(new OriginalImageLink(EImageLinkType.SETTLER, 1, 112, 0));
		draw(image, x, y,  BACKGROUND_Z, getColor(object), color);
	}

	private void drawShipInConstruction(int x, int y, IShipInConstruction ship) {
		EMovableType shipType = ship.getObjectType() == EMapObjectType.FERRY ? EMovableType.FERRY : EMovableType.CARGO_SHIP;
		float shade = getColor(visibleGrid != null ? visibleGrid[x][y] : CommonConstants.FOG_OF_WAR_VISIBLE);
		float state = ship.getStateProgress();
		Image image = imageMap.getImageForSettler(ship.getPlayer().getCivilisation(), shipType, EMovableAction.NO_ACTION, EMaterialType.TREE, ship.getDirection(), 0);
		drawWithConstructionMask(x, y, state, image, shade);
	}

	private void drawShip(IGraphicsMovable ship, int x, int y) {
		byte fogOfWarVisibleStatus = visibleGrid != null ? visibleGrid[x][y] : CommonConstants.FOG_OF_WAR_VISIBLE;
		if (fogOfWarVisibleStatus == 0) {
			return;
		}

		float height = context.getMap().getVisibleHeightAt(x, y);
		EDirection direction = ship.getDirection();
		EMovableType shipType = ship.getMovableType();
		float shade = getColor(fogOfWarVisibleStatus);

		IGraphicsCargoShip cargoShip = (shipType == EMovableType.CARGO_SHIP) ? (IGraphicsCargoShip)ship : null;
		IGraphicsFerry ferryShip = (shipType == EMovableType.FERRY) ? (IGraphicsFerry) ship : null;

		GLDrawContext glDrawContext = context.getGl();
		MapCoordinateConverter mapCoordinateConverter = context.getConverter();

		// get drawing position
		Color color = MapDrawContext.getPlayerColor(ship.getPlayer().getPlayerId());
		float viewX = 0;
		float viewY = 0;
		if (ship.getAction() == EMovableAction.WALKING) {
			viewX += betweenTilesX(x, y, direction.getInverseDirection(), 1-ship.getMoveProgress());
			viewY += betweenTilesY;
		} else {
			viewX += mapCoordinateConverter.getViewX(x, y, height);
			viewY += mapCoordinateConverter.getViewY(x, y, height);
		}
		// draw ship body
		drawShipLink(ship, EMaterialType.IRON, glDrawContext, viewX, viewY, y, color, shade);
		// prepare freight drawing
		List<? extends IGraphicsMovable> passengerList = ferryShip!=null?ferryShip.getPassengers(): null;

		float baseViewX = mapCoordinateConverter.getViewX(x, y, height);
		float baseViewY = mapCoordinateConverter.getViewY(x, y, height);
		float xShiftForward = mapCoordinateConverter.getViewX(x + direction.gridDeltaX, y + direction.gridDeltaY, height) - baseViewX;
		float yShiftForward = mapCoordinateConverter.getViewY(x + direction.gridDeltaX, y + direction.gridDeltaY, height) - baseViewY;
		int xRight = x + direction.rotateRight(1).gridDeltaX + direction.rotateRight(2).gridDeltaX;
		int yRight = y + direction.rotateRight(1).gridDeltaY + direction.rotateRight(2).gridDeltaY;

		float xShiftRight = (mapCoordinateConverter.getViewX(xRight, yRight, height) - baseViewX) / 2;
		float yShiftRight = (mapCoordinateConverter.getViewY(xRight, yRight, height) - baseViewY) / 2;
		ArrayList<FloatIntObject> freightY = new ArrayList<>();
		int numberOfFreight;
		// get freight positions
		if (shipType == EMovableType.FERRY) {
			numberOfFreight = passengerList.size();
			if (numberOfFreight > maxNumberOfPassengers) {
				numberOfFreight = maxNumberOfPassengers;
			}
			for (int i = 0; i < numberOfFreight; i++) {
				freightY.add(new FloatIntObject(PASSENGER_POSITION_TO_FRONT[i] * yShiftForward
					+ PASSENGER_POSITION_TO_RIGHT[i] * yShiftRight, i));
			}
		} else {
			numberOfFreight = cargoShip.getNumberOfCargoStacks();
			if (numberOfFreight > maxNumberOfStacks) {
				numberOfFreight = maxNumberOfStacks;
			}
			for (int i = 0; i < numberOfFreight; i++) {
				freightY.add(new FloatIntObject(CARGO_POSITION_TO_FRONT[i] * yShiftForward
					+ CARGO_POSITION_TO_RIGHT[i] * yShiftRight, i));
			}
		}
		// sort freight by view y
		if (freightY.size() > 0) {
			Collections.sort(freightY, (o1, o2) -> Float.compare(o2.getFloat(), o1.getFloat()));
		}

		ShortPoint2D shipPosition = ship.getPosition();

		if (shipType == EMovableType.FERRY) {
			// draw passengers behind the sail
			for (int i = 0; i < numberOfFreight; i++) {
				int j = freightY.get(i).getInt();
				float yShift = freightY.get(i).getFloat();
				if (yShift >= 0) {
					float xShift = PASSENGER_POSITION_TO_FRONT[j] * xShiftForward + PASSENGER_POSITION_TO_RIGHT[j] * xShiftRight;
					IGraphicsMovable passenger = passengerList.get(j);
					Image image = this.imageMap.getImageForSettler(passenger.getPlayer().getCivilisation(), passenger.getMovableType(), EMovableAction.NO_ACTION,
						EMaterialType.NO_MATERIAL, getPassengerDirection(direction, shipPosition, i), 0
					);
					image.drawAt(glDrawContext, viewX + xShift, viewY + yShift + PASSENGER_DECK_HEIGHT, getZ(0, y), color, shade);
				}
			}
		} else {
			// draw stacks behind the sail
			for (int i = 0; i < numberOfFreight; i++) {
				int j = freightY.get(i).getInt();
				float yShift = freightY.get(i).getFloat();
				if (yShift >= 0) {
					float xShift = CARGO_POSITION_TO_FRONT[j] * xShiftForward + CARGO_POSITION_TO_RIGHT[j] * xShiftRight;
					EMaterialType material = cargoShip.getCargoType(j);
					int count = cargoShip.getCargoCount(j);
					if (material != null && count > 0) {
						Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, material.getStackIndex());
						Image image = seq.getImageSafe(count - 1, () -> Labels.getName(material, false));
						image.drawAt(glDrawContext, viewX + xShift, viewY + yShift + CARGO_DECK_HEIGHT, getZ(0, y), color, shade);
					}
				}
			}
		}
		// draw sail
		drawShipLink(ship, EMaterialType.TRUNK, glDrawContext, viewX, viewY, y, color, shade);
		if (shipType == EMovableType.FERRY) {
			// draw passengers in front of the sail
			for (int i = 0; i < numberOfFreight; i++) {
				int j = freightY.get(i).getInt();
				float yShift = freightY.get(i).getFloat();
				if (yShift < 0) {
					float xShift = PASSENGER_POSITION_TO_FRONT[j] * xShiftForward + PASSENGER_POSITION_TO_RIGHT[j] * xShiftRight;
					IGraphicsMovable passenger = passengerList.get(j);
					Image image = this.imageMap.getImageForSettler(passenger.getPlayer().getCivilisation(), passenger.getMovableType(), EMovableAction.NO_ACTION,
						EMaterialType.NO_MATERIAL, getPassengerDirection(direction, shipPosition, i), 0
					);
					image.drawAt(glDrawContext, viewX + xShift, viewY + yShift + PASSENGER_DECK_HEIGHT, getZ(0, y), color, shade);
				}
			}
		} else if(shipType == EMovableType.CARGO_SHIP) {
			// draw stacks in front of the sail
			for (int i = 0; i < numberOfFreight; i++) {
				int j = freightY.get(i).getInt();
				float yShift = freightY.get(i).getFloat();
				if (yShift < 0) {
					float xShift = CARGO_POSITION_TO_FRONT[j] * xShiftForward + CARGO_POSITION_TO_RIGHT[j] * xShiftRight;
					EMaterialType material = cargoShip.getCargoType(j);
					int count = cargoShip.getCargoCount(j);
					if (material != null && count > 0) {
						Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, material.getStackIndex());
						Image image = seq.getImageSafe(count - 1, () -> Labels.getName(material, false));
						image.drawAt(glDrawContext, viewX + xShift, viewY + yShift + CARGO_DECK_HEIGHT, getZ(0, y), color, shade);
					}
				}
			}
		}
		// draw ship front
		drawShipLink(ship, EMaterialType.PLANK, glDrawContext, viewX, viewY, y, color, shade);
		if (ship.isSelected()) {
			drawSettlerMark(viewX, viewY, ship);
		}
	}

	private EDirection getPassengerDirection(EDirection shipDirection, ShortPoint2D shipPosition, int seatIndex) { // make ferry passengers look around
		int x = shipPosition.x;
		int y = shipPosition.y;
		int slowerAnimationStep = animationStep / 32;
		return shipDirection.getNeighbor(((x + seatIndex + slowerAnimationStep) / 8 + (y + seatIndex + slowerAnimationStep) / 11 + seatIndex) % 3 - 1);
	}

	private void drawShipLink(IGraphicsMovable ship, EMaterialType fakeMat, GLDrawContext gl, float viewX, float viewY, float y, Color color, float shade) {
		Image image = imageMap.getImageForSettler(ship.getPlayer().getCivilisation(), ship.getMovableType(), ship.getAction(), fakeMat, ship.getDirection(), 0);
		image.drawAt(gl, viewX, viewY, getZ(0, y), color, shade);
	}

	private void drawObject(int x, int y, IMapObject object, float color) {
		EMapObjectType type = object.getObjectType();
		float progress = object.getStateProgress();

		switch (type) {
			case ARROW:
				drawArrow(context, (IArrowMapObject) object, color);
				break;
			case TREE_ADULT:
				drawTree(x, y, color);
				break;
			case TREE_BURNING:
				drawBurningTree(x, y, color);
				break;

			case TREE_DEAD:
				playSound(object, SOUND_FALLING_TREE, x, y);
				drawFallingTree(x, y, progress, color);
				break;

			case TREE_GROWING:
				drawGrowingTree(x, y, progress, color);
				break;

			case CORN_GROWING:
				drawGrowingCorn(x, y, object, color);
				break;
			case CORN_ADULT:
				drawCorn(x, y, color);
				break;
			case CORN_DEAD:
				drawDeadCorn(x, y, color);
				break;

			case HIVE_EMPTY:
				drawEmptyHive(x, y, color);
				break;
			case HIVE_GROWING:
				drawGrowingHive(x, y, color);
				break;
			case HIVE_HARVESTABLE:
				drawHarvestableHive(x, y, color);
				break;

			case WINE_GROWING:
				drawGrowingWine(x, y, object, color);
				break;
			case WINE_HARVESTABLE:
				drawHarvestableWine(x, y, color);
				break;
			case WINE_DEAD:
				drawDeadWine(x, y, color);
				break;

			case MANNA_BOWL:
				drawMannaBowl(x, y, (IMannaBowlObject) object, color);
				break;

			case RICE_GROWING:
				drawGrowingRice(x, y, object, color);
				break;
			case RICE_HARVESTABLE:
				drawHarvestableRice(x, y, color);
				break;
			case RICE_DEAD:
				drawDeadRice(x, y, color);
				break;

			case WAVES:
				drawWaves(x, y, color);
				break;

			case STONE:
				drawStones(x, y, (int) object.getStateProgress(), color);
				break;

			case CUT_OFF_STONE:
				drawStones(x, y, 0, color);
				break;

			case GHOST:
				drawPlayerableByProgress(x, y, object, color, imageProvider.getSettlerSequence(DEAD_SETTLER_FILE, DEAD_SETTLER_INDEX));
				playSound(object, SOUND_SETTLER_KILLED, x, y);
				break;

			case SPELL_EFFECT:
				ISpecializedMapObject smo = (ISpecializedMapObject) object;
				drawPlayerableByProgress(x, y, object, color, imageProvider.getSettlerSequence(1, smo.getAnimation()));
				playSound(object, smo.getSound(), x, y);
				break;

			case BUILDING_DECONSTRUCTION_SMOKE:
				drawByProgress(x, y, 0, 13, 38, object.getStateProgress(), color);
				playSound(object, SOUND_BUILDING_DESTROYED, x, y);
				break;

			case FOUND_COAL:
				drawByProgress(x, y, 0, OBJECTS_FILE, 94, object.getStateProgress(), color);
				break;

			case FOUND_GEMSTONE:
				drawByProgress(x, y, 0, OBJECTS_FILE, 95, object.getStateProgress(), color);
				break;

			case FOUND_GOLD:
				drawByProgress(x, y, 0, OBJECTS_FILE, 96, object.getStateProgress(), color);
				break;

			case FOUND_IRON:
				drawByProgress(x, y, 0, OBJECTS_FILE, 97, object.getStateProgress(), color);
				break;

			case FOUND_BRIMSTONE:
				drawByProgress(x, y, 0, OBJECTS_FILE, 98, object.getStateProgress(), color);
				break;

			case FOUND_NOTHING:
				drawByProgress(x, y, 0, OBJECTS_FILE, 99, object.getStateProgress(), color);
				break;

			case BUILDINGSITE_SIGN:
				drawByProgress(x, y, 0, OBJECTS_FILE, 93, object.getStateProgress(), color);
				break;

			case BUILDINGSITE_POST:
				drawByProgress(x, y, 0, OBJECTS_FILE, 92, object.getStateProgress(), color);
				break;

			case WORKAREA_MARK:
				drawByProgress(x, y, 0, OBJECTS_FILE, 91, object.getStateProgress(), color);
				break;

			case FLAG_DOOR:
				drawPlayerableWaving(x, y, 0, 63, object, color, "door");
				break;

			case CONSTRUCTION_MARK:
				drawConstructionMark(x, y, object, color);
				break;

			case FLAG_ROOF:
				drawRoofFlag(x, y, object, color);
				break;

			case BUILDING:
				drawBuilding(x, y, (IBuilding) object, color);
				break;

			case PLACEMENT_BUILDING:
				drawPlacementBuilding(x, y, object, color);
				break;

			case STACK_OBJECT:
				drawStack(x, y, (IStackMapObject) object, color);
				break;

			case SMOKE:
			case SMOKE_WITH_FIRE:
				drawByProgressWithHeight(x, y, SMOKE_HEIGHT, object, color);
				break;

			case PLANT_DECORATION:
				drawPlantDecoration(x, y, color);
				break;

			case DESERT_DECORATION:
				drawDesertDecoration(x, y, color);
				break;

			case SWAMP_DECORATION:
				drawSwampDecoration(x, y, color);
				break;

			case PIG:
				drawPig(x, y, color);
				break;

			case DONKEY:
				drawDonkey(x, y, object, color);
				break;
			case FISH_DECORATION:
				drawDecorativeFish(x, y, color);
				break;

			case ATTACKABLE_TOWER:
				drawAttackableTower(x, y, object);
				break;

			case FERRY:
			case CARGO_SHIP:
				drawShipInConstruction(x, y, (IShipInConstruction) object);

			default:
				break;
		}
	}

	private void drawConstructionMark(int x, int y, IMapObject object, float color) {
		drawByProgress(x, y, CONSTRUCTION_MARK_Z, 4, 6, object.getStateProgress(), color);
	}

	private void drawRoofFlag(int x, int y, IMapObject object, float color) {
		drawPlayerableWaving(x, y, FLAG_ROOF_Z, 64, object, color, "roof");
	}

	private void drawPlacementBuilding(int x, int y, IMapObject object, float color) {
		ImageLink[] images = ((IBuilding) object).getBuildingVariant().getImages();
		Image image;
		for (ImageLink image1 : images) {
			image = imageProvider.getImage(image1);
			drawOnlyImage(image, x, y, PLACEMENT_BUILDING_Z, null, color);
		}
	}

	private void drawPlantDecoration(int x, int y, float color) {
		int step = (x * 13 + y * 233) % 8;
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(1, 27);
		draw(seq.getImageSafe(step, () -> "plant"), x, y, 0, color);
	}

	private void drawDesertDecoration(int x, int y, float color) {
		int step = (x * 13 + y * 233) % 5 + 10;
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(1, 27);
		draw(seq.getImageSafe(step, () -> "desert-decoration"), x, y, 0, color);
	}

	private void drawSwampDecoration(int x, int y, float color) {
		int step = (x * 13 + y * 233) % 6 + 27;
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(1, 27);
		draw(seq.getImageSafe(step, () -> "swamp-decoration"), x, y, 0, color);
	}

	private void drawPig(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(ANIMALS_FILE, PIG_SEQ);

		if (seq.length() > 0) {
			int i = getAnimationStep(x, y) / 2;
			int step = i % seq.length();
			draw(seq.getImageSafe(step, () -> "pig"), x, y, 0, color);
		}
	}

	private void drawDonkey(int x, int y, IMapObject object, float color) {
		int i = (getAnimationStep(x, y) / 20) % 6;
		Image image = imageProvider.getImage(new OriginalImageLink(EImageLinkType.SETTLER, 6, 17, 72 + i));
		draw(image, x, y, 0, getColor(object), color);
	}

	private void drawDecorativeFish(int x, int y, float color) {
		int step = getAnimationStep(x, y);
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(ANIMALS_FILE, FISH_SEQ);
		int substep = step % 1024;
		if (substep < 15) {
			int subseq = (step / 1024) % 4;
			draw(seq.getImageSafe(subseq * 15 + substep, () -> "fish-decoration"), x, y, 0, color);
		}
	}

	private void drawAttackableTower(int x, int y, IMapObject object) {
		IGraphicsMovable movable = ((IAttackableTowerMapObject) object).getMovable();
		if (movable != null) {
			drawMovableAt(movable, x, y);
			playMovableSound(movable);
		}
	}

	private GLDrawContext lastDC = null;

	/**
	 * Draws any type of movable.
	 *
	 * @param movable
	 * 		The movable.
	 */
	public void draw(IGraphicsMovable movable) {
		final ShortPoint2D pos = movable.getPosition();
		if (movable.getMovableType().isShip()) {
			drawShip(movable, pos.x, pos.y);
		} else {
			drawMovableAt(movable, pos.x, pos.y);
		}

		playMovableSound(movable);
	}

	private void playMovableSound(IGraphicsMovable movable) {
		if (movable.isSoundPlayed()) {
			return;
		}
		int soundNumber = -1;
		float delay = movable.getMoveProgress();
		switch (movable.getAction()) {
			case ACTION1:
				switch (movable.getMovableType()) {
					case LUMBERJACK:
						if (delay > .8) {
							soundNumber = 0;
						}
						break;
					case BRICKLAYER:
						if (delay > .7) {
							soundNumber = 1;
						}
						break;
					case DIGGER:
						if (delay > .6) {
							soundNumber = 2;
						}
						break;
					case STONECUTTER:
						if (delay > .8) {
							soundNumber = 3;
						}
						break;
					case SAWMILLER:
						if (delay > .2) {
							soundNumber = 5;
						}
						break;
					case SMITH:
						if (delay > .7) {
							soundNumber = 6;
						}
						break;
					case FARMER:
						if (delay > .8) {
							soundNumber = 9;
						}
						break;
					case FISHERMAN:
						if (delay > .8) {
							soundNumber = 16;
						}
						break;
					case DOCKWORKER:
						if (delay > .8) {
							soundNumber = 20;
						}
						break;
					case HEALER:
						if (delay > .8) {
							soundNumber = 21;
						}
						break;
					case GEOLOGIST: // TODO: should also check grid.getResourceAmountAt(x, y)
						if (sound.random.nextInt(256) == 0) {
							soundNumber = 24;
						}
						break;
					case SWORDSMAN_L1:
					case SWORDSMAN_L2:
					case SWORDSMAN_L3:
						if (delay > .8) {
							soundNumber = 30;
						}
						break;
					case BOWMAN_L1:
					case BOWMAN_L2:
					case BOWMAN_L3:
						if (delay > .4) {
							soundNumber = 33;
						}
						break;
					case PIKEMAN_L1:
					case PIKEMAN_L2:
					case PIKEMAN_L3:
						soundNumber = 34;
						break;
					case MELTER:
						soundNumber = 38;
						break;
					case PIG_FARMER:
						if (delay > .4) {
							soundNumber = 39;
						}
						break;
					case DONKEY_FARMER:
						if (delay > .4) {
							soundNumber = 40;
						}
						break;
					case CHARCOAL_BURNER:
						if (delay > .8) {
							soundNumber = 45;
						}
						break;
				}
				break;
			case ACTION2:
				switch (movable.getMovableType()) {
					case FARMER:
						if (delay > .8) {
							soundNumber = 12;
						}
						break;
					case FISHERMAN:
						if (delay > .5) {
							soundNumber = 15;
						}
						break;
					case LUMBERJACK:
						if (delay > .8) {
							soundNumber = 36;
						}
						break;
				}
			case ACTION3:
				switch (movable.getMovableType()) {
					case FISHERMAN:
						if (delay > .95) {
							soundNumber = 17;
						}
						break;
				}
				break;

		}
		if (soundNumber >= 0) {
			sound.playSound(soundNumber, 1, movable.getPosition());
			movable.setSoundPlayed();
		}
	}

	private void drawMovableAt(IGraphicsMovable movable, int x, int y) {
		byte fogStatus = visibleGrid != null ? visibleGrid[x][y] : CommonConstants.FOG_OF_WAR_VISIBLE;
		if (fogStatus <= CommonConstants.FOG_OF_WAR_EXPLORED) {
			return; // break
		}
		boolean isUndercover = false;
		if(!CommonConstants.CONTROL_ALL && localPlayer != null &&
				movable.getMovableType() == EMovableType.THIEF &&
				movable.getPlayer().getTeamId() != localPlayer.getTeamId() &&
				!((IGraphicsThief)movable).isUncoveredBy(localPlayer.getTeamId())) {
			isUndercover = true;
		}

		final float moveProgress = movable.getMoveProgress();

		IPlayer movablePlayer;
		if (!isUndercover || localPlayer == null) {
			movablePlayer = movable.getPlayer();
		} else {
			IPlayer localPlayer = context.getMap().getPlayerAt(x, y);
			if(localPlayer != null) {
				movablePlayer = localPlayer;
			} else {
				movablePlayer = this.localPlayer;
			}
		}
		Color color = MapDrawContext.getPlayerColor(movablePlayer.getPlayerId());
		float shade = MapObjectDrawer.getColor(fogStatus);
		Image image;
		float viewX;
		float viewY;
		int height = context.getHeight(x, y);
		EMovableType movableType = movable.getMovableType();

		// melter action
		if (movableType == EMovableType.MELTER && movable.getAction() == EMovableAction.ACTION1) {
			int number = (int) (moveProgress * 36);

			IBuilding building = ((IGraphicsBuildingWorker)movable).getGarrisonedBuilding();

			if(building != null) {
				ShortPoint2D position = building.getBuildingVariant().getMoltenMetalPosition().calculatePoint(building.getPosition());
				int metalHeight = context.getHeight(position.x, position.y);

				viewX = context.getConverter().getViewX(position.x, position.y, metalHeight);
				viewY = context.getConverter().getViewY(position.x, position.y, metalHeight);

				int ironIndex;
				int goldIndex;
				switch (movable.getPlayer().getCivilisation()) {
					case AMAZON:
						ironIndex = 40;
						goldIndex = 39;
						break;
					case ASIAN:
						ironIndex = 39;
						goldIndex = 38;
						break;
					default:
						ironIndex = 37;
						goldIndex = 36;
						break;
				}
				int metal = goldIndex;
				if(building.getBuildingVariant().isVariantOf(EBuildingType.IRONMELT)) {
					metal = ironIndex;
				}

				ImageLink link = new OriginalImageLink(EImageLinkType.SETTLER, movable.getPlayer().getCivilisation().getFileIndex()*10 + 3, metal, number > 24 ? 24 : number);
				image = imageProvider.getImage(link);
				image.drawAt(context.getGl(), viewX, viewY, getZ(molten_metal_offset, position.y), color, shade);
			}
		}

		if (movable.getAction() == EMovableAction.WALKING) {
			viewX = betweenTilesX(x, y, movable.getDirection().getInverseDirection(), 1-moveProgress);
			viewY = betweenTilesY;
		} else {
			viewX = context.getConverter().getViewX(x, y, height);
			viewY = context.getConverter().getViewY(x, y, height);
		}

		image = this.imageMap.getImageForSettler(movable, moveProgress, isUndercover?movablePlayer.getCivilisation():null);
		image.drawAt(context.getGl(), viewX, viewY, getZ(0, y), color, shade);

		drawSettlerMark(viewX, viewY, movable);
	}

	private float betweenTilesX(int startX, int startY, EDirection direction, float progress) {
		float theight = context.getHeight(startX, startY);
		float dheight = context.getHeight(startX+direction.gridDeltaX, startY+direction.gridDeltaY);
		MapCoordinateConverter converter = context.getConverter();
		float x = converter.getViewX(startX+progress*direction.gridDeltaX, startY+progress*direction.gridDeltaY, theight+progress*(dheight-theight));
		betweenTilesY = converter.getViewY(startX+progress*direction.gridDeltaX, startY+progress*direction.gridDeltaY, theight+progress*(dheight-theight));
		return x;
	}

	private void drawSettlerMark(float viewX, float viewY, IGraphicsMovable movable) {
		if(movable.isSelected()) {
			Image image = ImageProvider.getInstance().getSettlerSequence(4, 7).getImageSafe(0, () -> "settler-selection-indicator");
			image.drawAt(context.getGl(), viewX, viewY + 20, MOVABLE_SELECTION_MARKER_Z, Color.BLACK, 1);

			float healthPercentage = (movable.getHealth() / movable.getMovableType().getHealth());

			Sequence<? extends Image> sequence = ImageProvider.getInstance().getSettlerSequence(4, 6);
			int healthId = Math.min((int) ((1 - healthPercentage) * sequence.length()), sequence.length() - 1);
			Image healthImage = sequence.getImageSafe(healthId, () -> "settler-health-indicator");
			healthImage.drawAt(context.getGl(), viewX, viewY + 38, MOVABLE_SELECTION_MARKER_Z, Color.BLACK, 1);
		}

		int i = 1;
		for(EEffectType effect : EEffectType.values()) {
			if(movable.hasEffect(effect)) {
				float x = viewX + (i%3)*10;

				// line should wrap every 3 elements
				@SuppressWarnings("IntegerDivisionInFloatingPointContext")
				float y = viewY - (i/3)*10;

				ImageProvider.getInstance().getImage(effect.getImageLink()).drawAt(context.getGl(), x, y+20, MOVABLE_SELECTION_MARKER_Z, Color.BLACK, 1);
				i++;
			}
		}
	}

	private void playSound(IMapObject object, int soundId, int x, int y) {
		if(soundId == -1) return;

		if (object instanceof IBuilding.ISoundRequestable) {
			sound.playSound(soundId, 1, x, y);
		} else if (object instanceof ISoundable) {
			ISoundable soundable = (ISoundable) object;
			if (!soundable.isSoundPlayed()) {
				sound.playSound(soundId, 1, x, y);
				soundable.setSoundPlayed();
			}
		}
	}

	private void drawArrow(MapDrawContext context, IArrowMapObject object,
						   float color) {
		int sequence = 0;
		switch (object.getDirection()) {
			case SOUTH_WEST:
				sequence = 100;
				break;

			case WEST:
				sequence = 101;
				break;

			case NORTH_WEST:
				sequence = 102;
				break;

			case NORTH_EAST:
				sequence = 103;
				break;

			case EAST:
				sequence = 104;
				break;

			case SOUTH_EAST:
				sequence = 104;
				break;
		}

		float progress = object.getStateProgress();
		int index = Math.round(progress * 2);


		boolean onGround = progress >= 1;

		int startX = object.getSourceX();
		int startY = object.getSourceY();
		int destinationX = object.getTargetX();
		int destinationY = object.getTargetY();
		float theight = this.context.getHeight(startX, startY);
		float dheight = this.context.getHeight(destinationX, destinationY);

		float x = startX+progress*(destinationX-startX);
		float y = startY+progress*(destinationY-startY);
		float h = theight+progress*(dheight-theight);


		MapCoordinateConverter converter = this.context.getConverter();
		float viewX = converter.getViewX(x, y, h);
		float viewY = converter.getViewY(x, y, h);

		Image image = this.imageProvider.getSettlerSequence(OBJECTS_FILE, sequence).getImageSafe(index, () -> "arrow-" + object.getDirection() + "-" + index);
		image.drawAt(context.getGl(), viewX, viewY + 20 * progress * (1 - progress) + 20, getZ(onGround?BACKGROUND_Z:0, y), null, color);
	}

	private void drawStones(int x, int y, int availableStones, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, STONE);
		int stones = seq.length() - availableStones - 1;
		draw(seq.getImageSafe(stones, () -> "stone" + availableStones), x, y, 0, color);
	}

	private void drawWaves(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, WAVES);
		int len = seq.length();
		int step = (animationStep / 2 + x / 2 + y / 2) % len;
		if (step < len) {
			draw(seq.getImageSafe(step, () -> "wave"), x, y, BACKGROUND_Z, color); // waves must not be drawn on top of other things than water
		}
	}

	//Corn
	private void drawGrowingCorn(int x, int y, IMapObject object, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, CORN);
		int step = (int) (object.getStateProgress() * CORN_GROW_STEPS);
		draw(seq.getImageSafe(step, () -> "growing-corn"), x, y, 0, color);
	}

	private void drawCorn(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, CORN);
		draw(seq.getImageSafe(CORN_GROW_STEPS, () -> "grown-corn"), x, y, 0, color);
	}

	private void drawDeadCorn(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, CORN);
		draw(seq.getImageSafe(CORN_DEAD_STEP, () -> "dead-corn"), x, y, 0, color);
	}

	//Wine
	private void drawGrowingWine(int x, int y, IMapObject object, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, WINE);
		int step = (int) (object.getStateProgress() * WINE_GROW_STEPS);
		draw(seq.getImageSafe(step, () -> "growing-wine"), x, y, 0, color);
	}

	private void drawHarvestableWine(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, WINE);
		draw(seq.getImageSafe(WINE_GROW_STEPS, () -> "grown-wine"), x, y, 0, color);
	}

	private void drawDeadWine(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, WINE);
		draw(seq.getImageSafe(WINE_DEAD_STEP, () -> "dead-wine"), x, y, 0, color);
	}

	//Rice
	private void drawGrowingRice(int x, int y, IMapObject object, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, RICE);
		int step = (int) (object.getStateProgress() * RICE_GROW_STEPS);
		draw(seq.getImageSafe(step, () -> "growing-rice"), x, y, 0, color);
	}

	private void drawHarvestableRice(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, RICE);
		draw(seq.getImageSafe(RICE_GROW_STEPS, () -> "grown-rice"), x, y, 0, color);
	}

	private void drawDeadRice(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, RICE);
		draw(seq.getImageSafe(RICE_DEAD_STEP, () -> "dead-rice"), x, y, 0, color);
	}

	private void drawMannaBowl(int x, int y, IMannaBowlObject object, float color) {

		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(
				imageProvider.getGFXBuildingFileIndex(object.getCivilisation()),
				imageProvider.getMannaBowlSequence(object.getCivilisation())
		);

		int step = (int) (object.getStateProgress() * (WINE_BOWL_IMAGES - 1));
		draw(seq.getImageSafe(step, () -> "wine-bowl"), x, y, 0, color);
	}

	//Hive
	private void drawEmptyHive(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(ANIMALS_FILE, HIVE_EMPTY);
		draw(seq.getImageSafe(0, () -> "empty-hive"), x, y, 0, color);
	}

	private void drawGrowingHive(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(ANIMALS_FILE, HIVE_GROW[0]);
		int step = getAnimationStep(x, y) % seq.length();
		draw(seq.getImageSafe(step, () -> "growing-hive"), x, y, 0, color);
	}

	private void drawHarvestableHive(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(ANIMALS_FILE, HIVE_LAST);
		int step = getAnimationStep(x, y) % seq.length();
		draw(seq.getImageSafe(step, () -> "grown-hive"), x, y, 0, color);
	}

	//Tree
	private void drawGrowingTree(int x, int y, float progress, float color) {
		Image image;
		if (progress < 0.33) {
			Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, SMALL_GROWING_TREE);
			image = seq.getImageSafe(0, () -> "growing-tree-step1");
		} else {
			int treeType = getTreeType(x, y);
			Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, TREE_CHANGING_SEQUENCES[treeType]);
			if (progress < 0.66) {
				image = seq.getImageSafe(TREE_SMALL, () -> "growing-tree-step2");
			} else {
				image = seq.getImageSafe(TREE_MEDIUM, () -> "growing-tree-step3");
			}
		}
		draw(image, x, y, 0, color);
	}

	private void drawFallingTree(int x, int y, float progress, float color) {
		int treeType = getTreeType(x, y);
		int imageStep;

		if (progress < TREE_CUT_1) {
			imageStep = (int) (progress * TREE_FALLING_SPEED);
			if (imageStep >= TREE_FALL_IMAGES) {
				imageStep = TREE_FALL_IMAGES - 1;
			}
		} else if (progress < TREE_CUT_2) {
			// cut image 1
			imageStep = TREE_FALL_IMAGES;
		} else if (progress < TREE_CUT_3) {
			// cut image 2
			imageStep = TREE_FALL_IMAGES + 1;
		} else if (progress < TREE_TAKEN) {
			// cut image 3
			imageStep = TREE_FALL_IMAGES + 2;
		} else {
			int relativeStep = (int) ((progress - TREE_TAKEN) / (1 - TREE_TAKEN) * TREE_ROT_IMAGES);
			imageStep = relativeStep + TREE_FALL_IMAGES + 3;
		}

		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, TREE_CHANGING_SEQUENCES[treeType]);
		draw(seq.getImageSafe(imageStep, () -> "dying-tree"), x, y, 0, color);
	}

	private void drawTree(int x, int y, float color) {
		int treeType = getTreeType(x, y);
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, TREE_SEQUENCES[treeType]);

		int step = getAnimationStep(x, y) % seq.length();
		draw(seq.getImageSafe(step, () -> "grown-tree"), x, y, 0, color);
	}

	private void drawBurningTree(int x, int y, float color) {
		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, 124);
		int step = (getAnimationStep(x, y)*5) % seq.length();

		draw(seq.getImageSafe(step, () -> "burning-tree"), x, y, 0, color);
	}

	/**
	 * Draws a player border at a given position.
	 *
	 * @param x
	 * 		X position
	 * @param y
	 * 		Y position
	 * @param player
	 * 		The player.
	 */
	public void drawPlayerBorderObject(int x, int y, IPlayer player) {
		byte fogStatus = visibleGrid != null ? visibleGrid[x][y] : CommonConstants.FOG_OF_WAR_VISIBLE;
		if (fogStatus <= CommonConstants.FOG_OF_WAR_EXPLORED) {
			return; // break
		}
		Color color = MapDrawContext.getPlayerColor(player.getPlayerId());

		draw(playerBorderObjectImage, x, y, BACKGROUND_Z, color);
	}

	private static int getTreeType(int x, int y) {
		return (x + x / 5 + y / 3 + y + y / 7) % TREE_TYPES;
	}

	private int getAnimationStep(int x, int y) {
		return 0xfffffff & (this.animationStep + x * 167 + y * 1223);
	}

	/**
	 * Increases the animation step for trees and other stuff.
	 */
	public void nextFrame() {
		this.animationStep = ((int) System.currentTimeMillis() / 100) & 0x7fffffff;

		imageMap = SettlerImageMap.getInstance();
		imageProvider = ImageProvider.getInstance();

		playerBorderObjectImage = imageProvider.getSettlerSequence(FILE_BORDER_POST, 65).getImageSafe(0, () -> "border-indicator");

		if(context.getGl() != lastDC) {
			lastDC = context.getGl();

			context.getGl().setShadowDepthOffset(shadow_offset);
			SettlerImage.shadow_offset = shadow_offset;

		}
	}

	/**
	 * Draws a stack
	 *
	 * @param x
	 * 		The x coordinate of the building
	 * @param y
	 * 		The y coordinate of the building
	 * @param object
	 * 		The stack to draw.
	 * @param color
	 * 		Color to be drawn
	 */
	private void drawStack(int x, int y, IStackMapObject object, float color) {
		byte elements = object.getSize();
		if (elements > 0) {
			drawStackAtScreen(x, y, object.getMaterialType(), elements, color);
		}
	}

	/**
	 * Draws the stack directly to the screen.
	 *
	 * @param x
	 * 		The x coordinate of the building
	 * @param y
	 * 		The y coordinate of the building
	 * @param material
	 * 		The material the stack should have.
	 * @param count
	 * 		The number of elements on the stack
	 */
	private void drawStackAtScreen(int x, int y, EMaterialType material, int count, float color) {
		int stackIndex = material.getStackIndex();

		Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(OBJECTS_FILE, stackIndex);
		draw(seq.getImageSafe(count - 1, () -> Labels.getName(material, count != 1) + "@" + count), x, y, 0, color);
	}

	/**
	 * Gets the gray color for a given fog.
	 *
	 * @param fogStatus
	 * 		The fog of war value
	 * @return Fog of war transparency color value
	 */
	private static float getColor(int fogStatus) {
		return (float) fogStatus / CommonConstants.FOG_OF_WAR_VISIBLE;
	}

	/**
	 * Draws a given buildng to the context.
	 *
	 * @param x
	 * 		The x coordinate of the building
	 * @param y
	 * 		The y coordinate of the building
	 * @param building
	 * 		The building to draw
	 * @param color
	 * 		Gray color shade
	 */
	private void drawBuilding(int x, int y, IBuilding building, float color) {
		BuildingVariant variant = building.getBuildingVariant();

		float state = building.getStateProgress();

		if (state >= 0.99) {
			if (variant.isVariantOf(EBuildingType.SLAUGHTERHOUSE) && building instanceof IBuilding.ISoundRequestable && ((IBuilding.ISoundRequestable) building).isSoundRequested()) {
				playSound(building, SOUND_SLAUGHTERHOUSE, x, y);
			}

			if (variant.isVariantOf(EBuildingType.MILL) && building instanceof IBuilding.IMill && ((IBuilding.IMill) building).isRotating()) {
				Sequence<? extends Image> seq = this.imageProvider.getSettlerSequence(this.imageProvider.getGFXBuildingFileIndex(variant.getCivilisation()), this.imageProvider.getMillRotationIndex(variant.getCivilisation()));

				if (seq.length() > 0) {
					int i = getAnimationStep(x, y);
					int step = i % seq.length();
					drawOnlyImage(seq.getImageSafe(step, () -> "mill-" + step), x, y, 0, MapDrawContext.getPlayerColor(building.getPlayer().getPlayerId()), color);
					ImageLink[] images = variant.getImages();
					if (images.length > 0) {
						Image image = imageProvider.getImage(images[0]);
						drawOnlyShadow(image, x, y);
					}
				}
				playSound(building, SOUND_MILL, x, y);

			} else if(variant.isVariantOf(EBuildingType.STOCK)) {
				float[] zvalues = new float[] {-4*z_per_y, -2*z_per_y, 2*z_per_y, 3*z_per_y, 2*z_per_y, -2*z_per_y};
				ImageLink[] images = variant.getImages();
				for (int i = 0; i != 6; i++) {
					draw(imageProvider.getImage(images[i]), x, y, zvalues[i], color);
				}
			} else {
				ImageLink[] images = variant.getImages();
				if (images.length > 0) {
					Image image = imageProvider.getImage(images[0]);
					draw(image, x, y, building.getBuildingVariant().isVariantOf(EBuildingType.MARKET_PLACE) ? BACKGROUND_Z : 0, null, color);
				}

				byte fow = visibleGrid != null ? visibleGrid[x][y] : CommonConstants.FOG_OF_WAR_VISIBLE;

				if (building instanceof IOccupied && fow > CommonConstants.FOG_OF_WAR_EXPLORED) {
					drawOccupiers(x, y, (IOccupied) building, color);
				}

				for (int i = 1; i < images.length; i++) {
					Image image = imageProvider.getImage(images[i]);
					draw(image, x, y, tower_front_offset, color);
				}
			}
		} else if (state >= .01f) {
			drawBuildingConstruction(x, y, color, variant, state);
		}

		if (building.isSelected()) {
			drawBuildingSelectMarker(x, y);
		}
	}

	private void drawBuildingConstruction(int x, int y, float color, BuildingVariant variant, float state) {
		boolean hasTwoConstructionPhases = variant.getBuildImages().length > 0;

		boolean isInBuildPhase = hasTwoConstructionPhases && state < .5f;

		if (!isInBuildPhase && hasTwoConstructionPhases) {
			// draw the base build image
			for (ImageLink link : variant.getBuildImages()) {
				Image image = imageProvider.getImage(link);
				draw(image, x, y, 0, color);
			}
		}

		ImageLink[] constructionImages = isInBuildPhase ? variant.getBuildImages() : variant.getImages();

		float maskState = hasTwoConstructionPhases ? (state * 2) % 1 : state;
		for (ImageLink link : constructionImages) {
			Image image = imageProvider.getImage(link);
			drawWithConstructionMask(x, y, maskState, image, color);
		}
	}

	/**
	 * Draws the occupiers of a building
	 *
	 * @param x
	 * 		The x coordinate of the building
	 * @param y
	 * 		The y coordinate of the building
	 * @param building
	 * 		The occupyed building
	 * @param baseColor
	 * 		The base color (gray shade).
	 */
	private void drawOccupiers(int x, int y, IOccupied building, float baseColor) {
		// this can cause a ConcurrentModificationException when
		// a soldier enters the tower!
		try {
			int height = context.getHeight(x, y);
			float towerX = context.getConverter().getViewX(x, y, height);
			float towerY = context.getConverter().getViewY(x, y, height);
			GLDrawContext gl = context.getGl();

			for (IBuildingOccupier occupier : building.getOccupiers()) {
				OccupierPlace place = occupier.getPlace();

				IGraphicsMovable movable = occupier.getMovable();
				Color color = MapDrawContext.getPlayerColor(movable.getPlayer().getPlayerId());

				Image image;
				switch (place.getSoldierClass()) {
					case INFANTRY:
						ImageLink imageLink = ImageLinkMap.get(movable.getPlayer().getCivilisation(), place.looksRight() ? ECommonLinkType.GARRISON_RIGHT:ECommonLinkType.GARRISON_LEFT, movable.getMovableType());
						image = imageProvider.getImage(imageLink);
						if(image instanceof SettlerImage) ((SettlerImage)image).setShadow(null);
						break;
					case BOWMAN:
					default:
						image = this.imageMap.getImageForSettler(movable, movable.getMoveProgress(), null);
						break;
				}
				float viewX = towerX + place.getOffsetX();
				float viewY = towerY + place.getOffsetY();
				image.drawAt(gl, viewX, viewY, getZ(0, y), color, baseColor);

				if (place.getSoldierClass() == ESoldierClass.BOWMAN) {
					playMovableSound(movable);
					drawSettlerMark(viewX, viewY, movable);
				}
			}
		} catch (ConcurrentModificationException e) {
			// happens sometime, just ignore it.
		}
	}

	private void drawBuildingSelectMarker(int x, int y) {
		Image image = imageProvider.getSettlerSequence(SELECT_MARK_FILE, SELECT_MARK_SEQUENCE).getImageSafe(0, () -> "building-selection-indicator");
		draw(image, x, y, BUILDING_SELECTION_MARKER_Z, Color.BLACK);
	}

	private void drawWithConstructionMask(int x, int y, float maskState, Image unsafeImage, float color) {
		if (!(unsafeImage instanceof SingleImage)) {
			return; // should not happen
		}
		int height = context.getHeight(x, y);
		float viewX = context.getConverter().getViewX(x, y, height);
		float viewY = context.getConverter().getViewY(x, y, height);

		SingleImage image = (SingleImage) unsafeImage;
		// number of tiles in x direction, can be adjusted for performance
		int tiles = 10;

		float topLineBottom = 1 - maskState;
		float topLineTop = Math.max(0, topLineBottom - .1f);

		image.drawOnlyImageWithProgressAt(context.getGl(), viewX, viewY, getZ(construction_offset, y), 1, 1, 0, topLineBottom, color, false);

		for (int i = 0; i < tiles; i++) {
			image.drawOnlyImageWithProgressAt(context.getGl(), viewX, viewY, getZ(construction_offset, y), i/(float)tiles, topLineBottom, (i+1)/(float)tiles, topLineTop, color, true);
		}
	}

	// TODO shadow is wrong
	private static final int DEAD_SETTLER_FILE = 12;
	private static final int DEAD_SETTLER_INDEX = 27;

	private void drawPlayerableByProgress(int x, int y, IMapObject object, float baseColor, Sequence<? extends Image> seq) {
		int index = Math.min((int) (object.getStateProgress() * seq.length()), seq.length() - 1);
		Color color = getColor(object);
		draw(seq.getImage(index, () -> "dead-settler"), x, y, 0, color, baseColor);
	}

	private Color getColor(IMapObject object) {
		Color color = null;
		if (object instanceof IPlayerable) {
			color = MapDrawContext.getPlayerColor(((IPlayerable) object).getPlayer().getPlayerId());
		}
		return color;
	}

	private void drawPlayerableWaving(int x, int y, float z, int sequenceIndex, IMapObject object, float baseColor, String at) {
		Sequence<? extends Image> sequence = this.imageProvider.getSettlerSequence(FLAG_FILE, sequenceIndex);
		int index = animationStep % sequence.length();
		Color color = getColor(object);
		draw(sequence.getImageSafe(index, () -> "flag-" + at), x, y, z, color, baseColor);
	}

	private void drawByProgress(int x, int y, float z, int file, int sequenceIndex, float progress, float color) {
		Sequence<? extends Image> sequence = this.imageProvider.getSettlerSequence(file, sequenceIndex);
		int index = Math.min((int) (progress * sequence.length()), sequence.length() - 1);
		draw(sequence.getImageSafe(index, null), x, y, z, color);
	}

	private static final int SMOKE_FILE = 13;
	private static final int SMOKE_INDEX = 42;
	private static final int SMOKE_WITH_FIRE_INDEX = 43;

	private void drawByProgressWithHeight(int x, int y, int height, IMapObject object, float color) {
		int sequenceIndex;
		if(object.getObjectType() == EMapObjectType.SMOKE_WITH_FIRE) {
			sequenceIndex = SMOKE_WITH_FIRE_INDEX;
		} else {
			sequenceIndex = SMOKE_INDEX;
		}

		Sequence<? extends Image> sequence = this.imageProvider.getSettlerSequence(SMOKE_FILE, sequenceIndex);
		int index = Math.min((int) (object.getStateProgress() * sequence.length()), sequence.length() - 1);
		drawWithHeight(sequence.getImageSafe(index, null), x, y, height, color);
	}

	private void draw(Image image, int x, int y, float z, Color color) {
		draw(image, x, y, z, color, 1);
	}

	private void draw(Image image, int x, int y, float z, Color color, float fowDim) {
		int height = context.getHeight(x, y);
		float viewX = context.getConverter().getViewX(x, y, height);
		float viewY = context.getConverter().getViewY(x, y, height);

		image.drawAt(context.getGl(), viewX, viewY, getZ(z, y), color, fowDim);
	}

	private void draw(Image image, int x, int y, float z, float fowDim) {
		draw(image, x, y, z, null, fowDim);
	}

	private void drawOnlyImage(Image image, int x, int y, float z, Color torsoColor, float color) {
		int height = context.getHeight(x, y);
		float viewX = context.getConverter().getViewX(x, y, height);
		float viewY = context.getConverter().getViewY(x, y, height);
		image.drawOnlyImageAt(context.getGl(), viewX, viewY, getZ(z, y), torsoColor, color);
	}

	private void drawOnlyShadow(Image image, int x, int y) {
		int height = context.getHeight(x, y);
		float viewX = context.getConverter().getViewX(x, y, height);
		float viewY = context.getConverter().getViewY(x, y, height);
		image.drawOnlyShadowAt(context.getGl(), viewX, viewY, getZ(0, y));
	}

	private void drawWithHeight(Image image, int x, int y, int height, float color) {
		int baseHeight = context.getHeight(x, y);
		float viewX = context.getConverter().getViewX(x, y, baseHeight + height);
		float viewY = context.getConverter().getViewY(x, y, baseHeight + height);

		image.drawAt(context.getGl(), viewX, viewY, getZ(0, y), null, color);
	}

	public void drawMoveToMarker(ShortPoint2D moveToMarker, float progress) {
		drawByProgress(moveToMarker.x, moveToMarker.y, 0, MARKER_FILE, MOVE_TO_MARKER_SEQUENCE, progress, 1);
	}

	public void drawGotoMarker(ShortPoint2D gotoMarker, Image image) {
		draw(image, gotoMarker.x, gotoMarker.y, FLAG_ROOF_Z,null, 1);
	}

	private float getZ(float offset, float y) {
		return y*z_per_y+offset;
	}
}
