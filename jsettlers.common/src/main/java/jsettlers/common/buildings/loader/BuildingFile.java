/*******************************************************************************
 * Copyright (c) 2015 - 2017
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
package jsettlers.common.buildings.loader;

import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.buildings.RelativeDirectionPoint;
import jsettlers.common.buildings.stacks.ConstructionStack;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.images.EImageLinkType;
import jsettlers.common.images.ImageLink;
import jsettlers.common.images.OriginalImageLink;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.ESoldierClass;
import jsettlers.common.position.RelativePoint;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a building's xml file.
 * 
 * @author michael
 */
public class BuildingFile {

	private static final String BUILDING_DTD = "building.dtd";

	private static final String TAG_BUILDING = "building";
	private static final String TAG_DOOR = "door";
	private static final String TAG_BLOCKED = "blocked";
	private static final String TAG_CONSTRUCTION_STACK = "constructionStack";
	private static final String TAG_REQUEST_STACK = "requestStack";
	private static final String TAG_OFFER_STACK = "offerStack";
	private static final String TAG_OCCUPYER = "occupyer";
	private static final String ATTR_DX = "dx";
	private static final String ATTR_DY = "dy";
	private static final String ATTR_MATERIAl = "material";
	private static final String ATTR_BUILDREQUIRED = "buildrequired";
	private static final String TAG_WORKCENTER = "workcenter";
	private static final String TAG_FLAG = "flag";
	private static final String TAG_BRICKLAYER = "bricklayer";
	private static final String ATTR_DIRECTION = "direction";
	private static final String TAG_BUILDMARK = "buildmark";
	private static final String TAG_PIG_FEED_POSITION = "pigFeedPosition";
	private static final String TAG_DONKEY_FEED_POSITION = "donkeyFeedPosition";
	private static final String TAG_SAWMILLER_WORK_POSITION = "sawmillerWorkPosition";
	private static final String TAG_OVEN_POSITION = "ovenPosition";
	private static final String TAG_ANIMAL_POSITION = "animalPosition";
	private static final String TAG_SMOKE_POSITION = "smokePosition";
	private static final String TAG_MINE = "mine";
	private static final String TAG_HEALSPOT = "healspot";
	private static final String TAG_MOLTEN_METAL = "moltenMetal";
	private static final String TAG_MELT_INPUT = "meltInput";
	private static final String TAG_MELT_OUTPUT = "meltOutput";
	private static final String TAG_ANVIL_POSITION = "anvilPosition";
	private static final String TAG_SMITH_DROP_POSITION = "smithDropPosition";
	private static final String TAG_IMAGE = "image";
	private static final String TAG_GROUNDTYE = "ground";
	private static final String TAG_FIRE = "fire";

	private final ArrayList<RelativePoint> blocked = new ArrayList<>();

	private final ArrayList<RelativePoint> protectedTiles = new ArrayList<>();

	private RelativePoint door = new RelativePoint(0, 0);
	private RelativePoint smokePosition = new RelativePoint(0, 0);
	private boolean smokeWithFire = false;

	private RelativePoint healSpot = new RelativePoint(0, 0);
	private RelativePoint pigFeedPosition = new RelativePoint(0, 0);
	private ArrayList<RelativePoint> animalPositions = new ArrayList<>();

	private RelativeDirectionPoint meltInput = new RelativeDirectionPoint(0, 0, EDirection.NORTH_EAST);
	private RelativeDirectionPoint meltOutput = new RelativeDirectionPoint(0, 0, EDirection.NORTH_EAST);
	private EMaterialType meltInputMaterial = EMaterialType.NO_MATERIAL;
	private EMaterialType meltOutputMaterial = EMaterialType.NO_MATERIAL;

	private RelativeDirectionPoint anvilPosition;
	private RelativePoint smithDropPosition;

	private RelativePoint moltenMetalPosition = new RelativePoint(0, 0);

	private ArrayList<RelativeDirectionPoint> donkeyFeedPositions = new ArrayList<>();
	private RelativeDirectionPoint sawmillerWorkPosition = new RelativeDirectionPoint(0, 0, EDirection.NORTH_EAST);
	private RelativeDirectionPoint ovenPosition = new RelativeDirectionPoint(0, 0, EDirection.NORTH_EAST);

	private EMovableType workerType;

	private ArrayList<ConstructionStack> constructionStacks = new ArrayList<>();
	private ArrayList<RelativeStack> requestStacks = new ArrayList<>();
	private ArrayList<RelativeStack> offerStacks = new ArrayList<>();

	private List<RelativeDirectionPoint> bricklayers = new ArrayList<>();

	private int workradius;
	private boolean mine;
	private MineElementWrapper mineEntry = new MineElementWrapper();
	private RelativePoint workCenter = new RelativePoint(0, 0);
	private RelativePoint flag = new RelativePoint(0, 0);
	private ArrayList<RelativePoint> buildmarks = new ArrayList<>();
	private ImageLink guiimage = new OriginalImageLink(EImageLinkType.GUI, 1, 0, 0);
	private ArrayList<ImageLink> images = new ArrayList<>();
	private ArrayList<ImageLink> buildImages = new ArrayList<>();
	private ArrayList<ELandscapeType> groundtypes = new ArrayList<>();
	private ArrayList<OccupierPlace> occupyerplaces = new ArrayList<>();
	private short viewdistance = 0;
	private final String buildingName;

	public BuildingFile(String name, InputStream stream) {
		this.buildingName = name;

		try {
			XMLReader xr = XMLReaderFactory.createXMLReader();
			xr.setContentHandler(new SaxHandler());
			xr.setEntityResolver((publicId, systemId) -> {
				if (systemId.contains(BUILDING_DTD)) {
					return new InputSource(EBuildingType.class.getResourceAsStream(BUILDING_DTD));
				} else {
					return null;
				}
			});

			xr.parse(new InputSource(stream));
		} catch (Exception e) {
			System.err.println("Error loading building file " + buildingName + ":" + e.getMessage());
			loadDefault();
		}
	}

	private class SaxHandler extends DefaultHandler {

		@Override
		public void startElement(String uri, String localName, String tagName, Attributes attributes) throws SAXException {
			if (TAG_BUILDING.equals(tagName)) {
				readAttributes(attributes);
			} else if (TAG_DOOR.equals(tagName)) {
				door = readRelativeTile(attributes);
			} else if (TAG_WORKCENTER.equals(tagName)) {
				workCenter = readRelativeTile(attributes);
			} else if (TAG_FLAG.equals(tagName)) {
				flag = readRelativeTile(attributes);
			} else if (TAG_BLOCKED.equals(tagName)) {
				RelativePoint point = readRelativeTile(attributes);
				// block should only be false or true. true is the default value but android sometimes defaults to null.
				if (!"false".equals(attributes.getValue("block"))) {
					blocked.add(point);
				}
				protectedTiles.add(point);
			} else if (TAG_CONSTRUCTION_STACK.equals(tagName)) {
				readConstructionStack(attributes);
			} else if (TAG_REQUEST_STACK.equals(tagName)) {
				readAndAddRelativeStack(attributes, requestStacks);
			} else if (TAG_OFFER_STACK.equals(tagName)) {
				readAndAddRelativeStack(attributes, offerStacks);
			} else if (TAG_BRICKLAYER.equals(tagName)) {
				bricklayers.add(readRelativeDirectionPoint(attributes));
			} else if (TAG_IMAGE.equals(tagName)) {
				readImageLink(attributes);
			} else if (TAG_BUILDMARK.equals(tagName)) {
				buildmarks.add(readRelativeTile(attributes));
			} else if(TAG_PIG_FEED_POSITION.equals(tagName)) {
				pigFeedPosition = readRelativeTile(attributes);
			} else if(TAG_DONKEY_FEED_POSITION.equals(tagName)) {
				donkeyFeedPositions.add(readRelativeDirectionPoint(attributes));
			} else if(TAG_SAWMILLER_WORK_POSITION.equals(tagName)) {
				sawmillerWorkPosition = readRelativeDirectionPoint(attributes);
			} else if(TAG_OVEN_POSITION.equals(tagName)) {
				ovenPosition = readRelativeDirectionPoint(attributes);
			} else if(TAG_ANIMAL_POSITION.equals(tagName)) {
				animalPositions.add(readRelativeTile(attributes));
			} else if(TAG_SMOKE_POSITION.equals(tagName)) {
				smokePosition = readRelativeTile(attributes);
				smokeWithFire = Boolean.parseBoolean(attributes.getValue(TAG_FIRE));
			} else if(TAG_MINE.equals(tagName)) {
				mineEntry = new MineElementWrapper(attributes);
			} else if(TAG_HEALSPOT.equals(tagName)) {
				healSpot = readRelativeTile(attributes);
			} else if(TAG_MOLTEN_METAL.equals(tagName)) {
				moltenMetalPosition = readRelativeTile(attributes);
			} else if(TAG_MELT_INPUT.equals(tagName)) {
				meltInput = readRelativeDirectionPoint(attributes);
				meltInputMaterial = readMaterial(attributes);
			} else if(TAG_MELT_OUTPUT.equals(tagName)) {
				meltOutput = readRelativeDirectionPoint(attributes);
				meltOutputMaterial = readMaterial(attributes);
			} else if (TAG_ANVIL_POSITION.equals(tagName)) {
				anvilPosition = readRelativeDirectionPoint(attributes);
			} else if (TAG_SMITH_DROP_POSITION.equals(tagName)) {
				smithDropPosition = readRelativeTile(attributes);
			} else if (TAG_GROUNDTYE.equals(tagName)) {
				groundtypes.add(ELandscapeType.valueOf(attributes.getValue("groundtype")));
			} else if (TAG_OCCUPYER.equals(tagName)) {
				addOccupyer(attributes);
			}
		}
	}

	private void loadDefault() {
		blocked.add(new RelativePoint(0, 0));
		protectedTiles.add(new RelativePoint(0, 0));
		System.err.println("Building file defect: " + buildingName);
	}

	private void addOccupyer(Attributes attributes) {
		try {
			int x = parseOptionalInteger(attributes.getValue("offsetX"));
			int y = parseOptionalInteger(attributes.getValue("offsetY"));
			ESoldierClass type = ESoldierClass.valueOf(attributes.getValue("type"));
			RelativePoint position = new RelativePoint(Short.parseShort(attributes.getValue("soldierX")), Short.parseShort(attributes
					.getValue("soldierY")));
			OccupierPlace place = new OccupierPlace(x, y, type, position, "true".equals(attributes.getValue("looksRight")));
			occupyerplaces.add(place);
		} catch (NumberFormatException e) {
			System.err.println("Warning: illegal number " + "for occupyer x/y attribute, in definiton for " + buildingName);
		} catch (IllegalArgumentException e) {
			System.err.println("Illegal occupyer position name in " + buildingName);
		}
	}

	/**
	 * If value != null, the value is parsed. Otherwise, 0 is returned.
	 * 
	 * @param value
	 * @return
	 * @throws NumberFormatException
	 */
	private int parseOptionalInteger(String value) throws NumberFormatException {
		if (value != null) {
			return Integer.parseInt(value);
		} else {
			return 0;
		}
	}

	private void readImageLink(Attributes attributes) {
		try {
			ImageLink imageLink = getImageFromAttributes(attributes);
			String forState = attributes.getValue("for");
			if ("GUI".equals(forState)) {
				guiimage = imageLink;
			} else if ("BUILD".equals(forState)) {
				buildImages.add(imageLink);
			} else {
				images.add(imageLink);
			}
		} catch (NumberFormatException e) {
			System.err.println("Warning: illegal number " + "for image link attribute, in definiton for " + buildingName);
		} catch (IllegalArgumentException e) {
			System.err.println("Illegal image link name in " + buildingName);
		}
	}

	public static ImageLink getImageFromAttributes(Attributes attributes) {
		ImageLink imageLink;
		if (attributes.getIndex("name") < 0) {
			imageLink = getOriginalImageLink(attributes);
		} else {
			String name = attributes.getValue("name");
			int image = Integer.parseInt(attributes.getValue("image"));
			imageLink = ImageLink.fromName(name, image);
		}
		return imageLink;
	}

	private static OriginalImageLink getOriginalImageLink(Attributes attributes) {
		int file = Integer.parseInt(attributes.getValue("file"));
		int sequence = Integer.parseInt(attributes.getValue("sequence"));
		String imageStr = attributes.getValue("image");
		int image = imageStr != null ? Integer.parseInt(imageStr) : 0;
		EImageLinkType type = EImageLinkType.valueOf(attributes.getValue("type"));
		return new OriginalImageLink(type, file, sequence, image);
	}

	private RelativeDirectionPoint readRelativeDirectionPoint(Attributes attributes) {
		try {
			int dx = Integer.parseInt(attributes.getValue(ATTR_DX));
			int dy = Integer.parseInt(attributes.getValue(ATTR_DY));
			EDirection direction = EDirection.valueOf(attributes.getValue(ATTR_DIRECTION));

			return new RelativeDirectionPoint(dx, dy, direction);

		} catch (NumberFormatException e) {
			System.err.println("Warning: illegal number for stack attribute, in definiton for " + buildingName);
		} catch (IllegalArgumentException e) {
			System.err.println("Illegal direction name in " + buildingName);
		}
		return new RelativeDirectionPoint(0, 0, EDirection.NORTH_EAST);
	}

	private RelativePoint readRelativeTile(Attributes attributes) {
		try {
			int dx = Integer.parseInt(attributes.getValue(ATTR_DX));
			int dy = Integer.parseInt(attributes.getValue(ATTR_DY));

			return new RelativePoint(dx, dy);

		} catch (NumberFormatException e) {
			System.err.println("Warning: illegal number " + "for relative tile attribute, in definiton for " + buildingName);
			return new RelativePoint(0, 0);
		}
	}

	private EMaterialType readMaterial(Attributes attributes) {
		return EMaterialType.valueOf(attributes.getValue(ATTR_MATERIAl));
	}

	private void readAndAddRelativeStack(Attributes attributes, ArrayList<RelativeStack> stacks) {
		try {
			int dx = Integer.parseInt(attributes.getValue(ATTR_DX));
			int dy = Integer.parseInt(attributes.getValue(ATTR_DY));
			EMaterialType type = readMaterial(attributes);

			stacks.add(new RelativeStack(dx, dy, type));
		} catch (NumberFormatException e) {
			System.err.println("Warning: illegal number " + "for stack attribute, in definiton for " + buildingName);
		} catch (IllegalArgumentException e) {
			System.err.println("Illegal material name in " + buildingName);
		}
	}

	private void readConstructionStack(Attributes attributes) {
		try {
			int dx = Integer.parseInt(attributes.getValue(ATTR_DX));
			int dy = Integer.parseInt(attributes.getValue(ATTR_DY));
			EMaterialType type = readMaterial(attributes);
			short requiredForBuild = Short.parseShort(attributes.getValue(ATTR_BUILDREQUIRED));

			if (requiredForBuild <= 0) {
				throw new NumberFormatException("RequiredForBuild attribute needs to be an integer > 0");
			}

			constructionStacks.add(new ConstructionStack(dx, dy, type, requiredForBuild));
		} catch (NumberFormatException e) {
			System.err.println("Warning: illegal number " + "for stack attribute, in definiton for " + buildingName);
		} catch (IllegalArgumentException e) {
			System.err.println("Illegal material name in " + buildingName);
		}
	}

	/**
	 * Read from a building tag
	 * 
	 * @param attributes
	 */
	private void readAttributes(Attributes attributes) {
		String workerName = attributes.getValue("worker");
		if (workerName == null || workerName.isEmpty()) {
			this.workerType = null;
		} else {
			try {
				this.workerType = EMovableType.valueOf(workerName);
			} catch (IllegalArgumentException e) {
				System.err.println("Illegal worker name: " + workerName);
				this.workerType = EMovableType.BEARER;
			}
		}
		String workradius = attributes.getValue("workradius");
		if (workradius != null && workradius.matches("\\d+")) {
			this.workradius = Integer.parseInt(workradius);
		}
		String viewdistance = attributes.getValue("viewdistance");
		if (viewdistance != null && viewdistance.matches("\\d+")) {
			this.viewdistance = Short.parseShort(viewdistance);
		}

		this.mine = Boolean.valueOf(attributes.getValue("mine"));
	}

	public RelativePoint getDoor() {
		return door;
	}

	public EMovableType getWorkerType() {
		return workerType;
	}

	public RelativePoint[] getProtectedTiles() {
		return protectedTiles.toArray(new RelativePoint[protectedTiles.size()]);
	}

	public RelativePoint[] getBlockedTiles() {
		return blocked.toArray(new RelativePoint[blocked.size()]);
	}

	public ConstructionStack[] getConstructionRequiredStacks() {
		return constructionStacks.toArray(new ConstructionStack[constructionStacks.size()]);
	}

	public RelativeStack[] getRequestStacks() {
		return requestStacks.toArray(new RelativeStack[requestStacks.size()]);
	}

	public RelativeStack[] getOfferStacks() {
		return offerStacks.toArray(new RelativeStack[offerStacks.size()]);
	}

	public RelativeDirectionPoint[] getBricklayers() {
		return bricklayers.toArray(new RelativeDirectionPoint[bricklayers.size()]);
	}

	public short getWorkradius() {
		return (short) workradius;
	}

	public boolean isMine() {
		return mine;
	}

	public RelativePoint getWorkcenter() {
		return workCenter;
	}

	public RelativePoint getFlag() {
		return flag;
	}

	public ImageLink[] getImages() {
		return images.toArray(new ImageLink[images.size()]);
	}

	public ImageLink[] getBuildImages() {
		return buildImages.toArray(new ImageLink[buildImages.size()]);
	}

	public ImageLink getGuiImage() {
		return guiimage;
	}

	public RelativePoint[] getBuildmarks() {
		return buildmarks.toArray(new RelativePoint[buildmarks.size()]);
	}

	public RelativePoint getSmokePosition() {
		return smokePosition;
	}

	public boolean isSmokeWithFire() {
		return smokeWithFire;
	}

	public MineElementWrapper getMineEntry() {
		return mineEntry;
	}

	public RelativePoint getHealSpot() {
		return healSpot;
	}

	public RelativePoint getPigFeedPosition() {
		return pigFeedPosition;
	}

	public RelativeDirectionPoint[] getDonkeyFeedPositions() {
		return donkeyFeedPositions.toArray(new RelativeDirectionPoint[0]);
	}

	public RelativeDirectionPoint getSawmillerWorkPosition() {
		return sawmillerWorkPosition;
	}

	public RelativeDirectionPoint getOvenPosition() {
		return ovenPosition;
	}

	public RelativePoint[] getAnimalPositions() {
		return animalPositions.toArray(new RelativePoint[0]);
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

	public RelativeDirectionPoint getAnvilPosition() {
		return anvilPosition;
	}

	public RelativePoint getSmithDropPosition() {
		return smithDropPosition;
	}

	public List<ELandscapeType> getGroundtypes() {
		return groundtypes;
	}

	public short getViewdistance() {
		return viewdistance;
	}

	public OccupierPlace[] getOccupyerPlaces() {
		return occupyerplaces.toArray(new OccupierPlace[occupyerplaces.size()]);
	}
}
