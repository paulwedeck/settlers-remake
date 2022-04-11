package jsettlers.common.buildings;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import jsettlers.common.buildings.loader.BuildingFile;
import jsettlers.common.buildings.loader.MineElementWrapper;
import jsettlers.common.buildings.stacks.ConstructionStack;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.images.ImageLink;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.RelativePoint;

public class BuildingVariant {
	public final int ordinal;

	private final EBuildingType type;

	private final ECivilisation civilisation;

	private final EMovableType workerType;

	private final RelativePoint doorTile;

	private final RelativePoint[] blockedTiles;

	private final short workRadius;

	private final boolean mine;

	private final ConstructionStack[] constructionStacks;
	private final RelativeStack[] requestStacks;
	private final RelativeStack[] offerStacks;

	private final RelativePoint workCenter;

	private final RelativePoint flag;

	private final RelativeDirectionPoint[] bricklayers;

	private final byte numberOfConstructionMaterials;

	private final ImageLink guiImage;

	private final ImageLink[] images;

	private final ImageLink[] buildImages;

	private final RelativePoint[] protectedTiles;

	private final RelativePoint[] buildMarks;

	private final RelativePoint[] buildingAreaBorder;

	private final EnumSet<ELandscapeType> groundTypes;

	private final short viewDistance;

	private final OccupierPlace[] occupierPlaces;

	private final BuildingAreaBitSet buildingAreaBitSet;

	private final RelativePoint smokePosition;
	private final boolean smokeWithFire;

	private final RelativePoint healSpot;

	private final RelativePoint pigFeedPosition;

	private final RelativeDirectionPoint[] donkeyFeedPosition;

	private final RelativeDirectionPoint sawmillerWorkPosition;

	private final RelativeDirectionPoint ovenPosition;

	private final RelativePoint[] animalPositions;

	private final MineElementWrapper mineSettings;

	private final RelativePoint moltenMetalPosition;

	private final RelativeDirectionPoint meltInput;
	private final RelativeDirectionPoint meltOutput;

	private final EMaterialType meltInputMaterial;
	private final EMaterialType meltOutputMaterial;

	private final RelativeDirectionPoint anvilPosition;
	private final RelativePoint smithDropPosition;

	/**
	 * Constructs an enum object.
	 */
	BuildingVariant(EBuildingType type, ECivilisation civilisation, InputStream stream) {
		this.civilisation = civilisation;
		this.ordinal = type.ordinal;
		this.type = type;

		BuildingFile file = new BuildingFile(civilisation + "/" + type, stream);
		workerType = file.getWorkerType();
		doorTile = file.getDoor();
		blockedTiles = file.getBlockedTiles();
		protectedTiles = file.getProtectedTiles();

		constructionStacks = file.getConstructionRequiredStacks();
		requestStacks = file.getRequestStacks();
		offerStacks = file.getOfferStacks();

		workRadius = file.getWorkradius();
		workCenter = file.getWorkcenter();
		mine = file.isMine();
		flag = file.getFlag();
		bricklayers = file.getBricklayers();
		occupierPlaces = file.getOccupyerPlaces();
		guiImage = file.getGuiImage();

		images = file.getImages();
		buildImages = file.getBuildImages();

		buildMarks = file.getBuildmarks();
		groundTypes = EnumSet.copyOf(file.getGroundtypes());
		viewDistance = file.getViewdistance();

		smokePosition = file.getSmokePosition();
		smokeWithFire = file.isSmokeWithFire();

		healSpot = file.getHealSpot();

		pigFeedPosition = file.getPigFeedPosition();

		donkeyFeedPosition = file.getDonkeyFeedPositions();

		sawmillerWorkPosition = file.getSawmillerWorkPosition();

		ovenPosition = file.getOvenPosition();

		animalPositions = file.getAnimalPositions();

		meltInput = file.getMeltInput();
		meltOutput = file.getMeltOutput();

		meltInputMaterial = file.getMeltInputMaterial();
		meltOutputMaterial = file.getMeltOutputMaterial();

		moltenMetalPosition = file.getMoltenMetalPosition();

		mineSettings = file.getMineEntry();

		anvilPosition = file.getAnvilPosition();
		smithDropPosition = file.getSmithDropPosition();

		this.numberOfConstructionMaterials = calculateNumberOfConstructionMaterials();

		this.buildingAreaBitSet = new BuildingAreaBitSet(getBuildingArea());

		if (mine) {
			this.buildingAreaBitSet.setCenter((short) 1, (short) 1);
		}

		List<RelativePoint> buildingAreaBorder = new ArrayList<>();
		for(RelativePoint pt : protectedTiles) {
			for(EDirection dir : EDirection.VALUES) {
				int x = pt.getDx() + dir.gridDeltaX;
				int y = pt.getDy() + dir.gridDeltaY;

				boolean collision = false;
				for(RelativePoint potentialCollision : protectedTiles) {
					if(potentialCollision.getDy() == y && potentialCollision.getDx() == x) {
						collision = true;
						break;
					}
				}

				if(!collision) {
					buildingAreaBorder.add(new RelativePoint(x, y));
				}
			}
		}

		this.buildingAreaBorder = buildingAreaBorder.toArray(new RelativePoint[0]);
	}

	public static InputStream openBuildingFile(ECivilisation civilisation, EBuildingType type) {
		String suffix = civilisation + "/" + type + ".xml";
		suffix = suffix.toLowerCase(Locale.ENGLISH);
		return EBuildingType.class.getResourceAsStream("/jsettlers/common/buildings/" + suffix);
	}

	public static BuildingVariant create(EBuildingType type, ECivilisation civilisation) {
		InputStream stream = openBuildingFile(civilisation, type);
		if(stream == null) {
			return null;
		}

		return new BuildingVariant(type, civilisation, stream);
	}

	private byte calculateNumberOfConstructionMaterials() {
		byte sum = 0;
		for (ConstructionStack stack : getConstructionStacks()) {
			sum += stack.requiredForBuild();
		}
		return sum;
	}

	public EBuildingType getType() {
		return type;
	}

	public boolean isVariantOf(EBuildingType buildingType) {
		return type == buildingType;
	}

	public ECivilisation getCivilisation() {
		return civilisation;
	}

	public RelativePoint[] getBuildingArea() {
		return protectedTiles;
	}

	public RelativePoint[] getBuildingAreaBorder() {
		return buildingAreaBorder;
	}

	/**
	 * Gets the type of worker required for the building.
	 *
	 * @return The worker or <code>null</code> if no worker is required.
	 */
	public final EMovableType getWorkerType() {
		return workerType;
	}

	/**
	 * Gets the position of the door for this building.
	 *
	 * @return The door.
	 */
	public final RelativePoint getDoorTile() {
		return doorTile;
	}

	/**
	 * Gets a list of blocked positions.
	 *
	 * @return The list of blocked positions.
	 */
	public final RelativePoint[] getBlockedTiles() {
		return blockedTiles;
	}

	/**
	 * Gets the tiles that are protected by this building. On thse tiles, no other buildings may be build.
	 *
	 * @return The tiles as array.
	 */
	public final RelativePoint[] getProtectedTiles() {
		return protectedTiles;
	}

	/**
	 * Gets the images needed to display this building. They are rendered in the order provided.
	 *
	 * @return The images
	 */
	public final ImageLink[] getImages() {
		return images;
	}

	/**
	 * Gets the images needed to display this building while it si build. They are rendered in the order provided.
	 *
	 * @return The images
	 */
	public final ImageLink[] getBuildImages() {
		return buildImages;
	}

	/**
	 * Gets the gui image that is displayed in the building selection dialog.
	 *
	 * @return The image. It may be <code>null</code>
	 */
	public final ImageLink getGuiImage() {
		return guiImage;
	}

	/**
	 * Gets the working radius of the building. If it is 0, the building does not support a working radius.
	 *
	 * @return The radius.
	 */
	public final short getWorkRadius() {
		return workRadius;
	}

	/**
	 * Gets the default work center for the building type.
	 *
	 * @return The default work center position.
	 */
	public final RelativePoint getDefaultWorkcenter() {
		return workCenter;
	}

	/**
	 * Gets the position of the flag for this building. The flag type is determined by the building itself.
	 *
	 * @return The flag position.
	 */
	public final RelativePoint getFlag() {
		return flag;
	}

	/**
	 * Gets the positions where the bricklayers should stand to build the house.
	 *
	 * @return The positions.
	 * @see RelativeDirectionPoint
	 */
	public final RelativeDirectionPoint[] getBricklayers() {
		return bricklayers;
	}

	/**
	 * Gets the positions of the build marks (sticks) for this building.
	 *
	 * @return The positions of the marks.
	 */
	public final RelativePoint[] getBuildMarks() {
		return buildMarks;
	}

	/**
	 * Gets the ground types this building can be placed on.
	 *
	 * @return The ground types.
	 */
	public final Set<ELandscapeType> getGroundTypes() {
		return groundTypes;
	}

	/**
	 * Gets the distance the FOW should be set to visible around this building.
	 *
	 * @return The view distance.
	 */
	public final short getViewDistance() {
		return viewDistance;
	}

	/**
	 * Gets the places where occupiers can be in this building.
	 *
	 * @return The places.
	 * @see OccupierPlace
	 */
	public final OccupierPlace[] getOccupierPlaces() {
		return occupierPlaces;
	}

	/**
	 * Gets the area for this building.
	 *
	 * @return The building area.
	 */
	public final BuildingAreaBitSet getBuildingAreaBitSet() {
		return buildingAreaBitSet;
	}

	/**
	 * Gets the materials required to build this building and where to place them.
	 *
	 * @return The array of material stacks.
	 */
	public ConstructionStack[] getConstructionStacks() {
		return constructionStacks;
	}

	/**
	 * Get the amount of material required to build this house. Usually the number of stone + planks.
	 *
	 * @return The number of materials required to construct the building.
	 */
	public final byte getNumberOfConstructionMaterials() {
		return numberOfConstructionMaterials;
	}

	/**
	 * Gets the request stacks required to operate this building.
	 *
	 * @return The request stacks.
	 */
	public RelativeStack[] getRequestStacks() {
		return requestStacks;
	}

	/**
	 * Gets the positions where the building should offer materials.
	 *
	 * @return The offer positions.
	 */
	public RelativeStack[] getOfferStacks() {
		return offerStacks;
	}

	/**
	 * Checks if this building is a mine.
	 *
	 * @return <code>true</code> iff this building is a mine.
	 */
	public boolean isMine() {
		return mine;
	}

	public boolean needsFlattenedGround() {
		return !mine;
	}

	public RelativePoint getSmokePosition() {
		return smokePosition;
	}

	public boolean isSmokeWithFire() {
		return smokeWithFire;
	}

	/**
	 * Returns the position a movable should be healed at.<br>
	 * Only usable for hospitals.
	 *
	 * @return the heal spot.
	 */
	public RelativePoint getHealSpot() {
		return healSpot;
	}

	public RelativePoint getPigFeedPosition() {
		return pigFeedPosition;
	}

	public RelativeDirectionPoint[] getDonkeyFeedPosition() {
		return donkeyFeedPosition;
	}

	public RelativeDirectionPoint getSawmillerWorkPosition() {
		return sawmillerWorkPosition;
	}

	public RelativeDirectionPoint getOvenPosition() {
		return ovenPosition;
	}

	public RelativePoint[] getAnimalPositions() {
		return animalPositions;
	}

	public MineElementWrapper getMineSettings() {
		return mineSettings;
	}

	public RelativeDirectionPoint getAnvilPosition() {
		return anvilPosition;
	}

	public RelativePoint getSmithDropPosition() {
		return smithDropPosition;
	}

	public RelativePoint getMoltenMetalPosition() {
		return moltenMetalPosition;
	}

	public RelativeDirectionPoint getMeltInput() {
		return meltInput;
	}

	public EMaterialType getMeltInputMaterial() {
		return meltInputMaterial;
	}

	public RelativeDirectionPoint getMeltOutput() {
		return meltOutput;
	}

	public EMaterialType getMeltOutputMaterial() {
		return meltOutputMaterial;
	}

	public Set<ELandscapeType> getRequiredGroundTypeAt(int relativeX, int relativeY) {
		if (relativeX == 0 && relativeY == 0 && mine) { // if it is a mine and we are in the center
			return ELandscapeType.MOUNTAIN_TYPES;
		} else {
			return groundTypes;
		}
	}

	@Override
	public String toString() {
		return "BuildingVariant{type=" + type + ", civ=" + civilisation + "}";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BuildingVariant that = (BuildingVariant) o;
		return type == that.type &&
				civilisation == that.civilisation;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, civilisation);
	}
}
