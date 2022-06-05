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
package jsettlers.buildingcreator.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import jsettlers.buildingcreator.editor.map.BuildingtestMap;
import jsettlers.buildingcreator.editor.map.PseudoTile;
import jsettlers.common.Color;
import jsettlers.common.action.Action;
import jsettlers.common.action.EActionType;
import jsettlers.common.action.IAction;
import jsettlers.common.action.PointAction;
import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.RelativeDirectionPoint;
import jsettlers.common.buildings.stacks.ConstructionStack;
import jsettlers.common.buildings.stacks.RelativeStack;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.menu.FakeMapGame;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.menu.IMapInterfaceListener;
import jsettlers.common.movable.EDirection;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.main.swing.SwingManagedJSettlers;
import jsettlers.main.swing.lookandfeel.JSettlersLookAndFeelExecption;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import javax.swing.JCheckBox;
import javax.swing.border.TitledBorder;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import jsettlers.buildingcreator.editor.map.PseudoBuilding;
import jsettlers.buildingcreator.editor.places.OccupierPlacesEditor;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.images.ImageLink;
import jsettlers.common.landscape.ELandscapeType;
import jsettlers.common.movable.ESoldierClass;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This is the main building creator class.
 * 
 * @author michael
 */
public class BuildingCreatorApp implements IMapInterfaceListener, Runnable {
	private BuildingDefinition definition;
	private BuildingtestMap map;

	private ToolType tool = ToolType.SET_BLOCKED;
	private JLabel positionDisplayer;
	private JFrame window;
        private IMapInterfaceConnector mapInterfaceConnector;

        private Timer timer;

	@Override
	public void run() {
		try {
			EBuildingType type = askType();
                        if (type == null) {
                            // user pressed Cancel
                            return;
                        }
			BuildingVariant variant = askVariant(type);
                        if (variant == null) {
                            // user pressed Cancel
                            return;
                        }

			definition = new BuildingDefinition(variant);
			map = new BuildingtestMap(definition);

                        reloadMapColor();

			mapInterfaceConnector = startMapWindow();
			mapInterfaceConnector.addListener(this);

			JPanel menu = generateMenu();

			window = new JFrame("Edit " + variant);
			window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                        window.setLayout(new BorderLayout());
			window.add(new JScrollPane(menu), BorderLayout.CENTER);
			window.pack();
			window.setVisible(true);

			mapInterfaceConnector.fireAction(new Action(EActionType.TOGGLE_DEBUG));
                        
		} catch (JSettlersLookAndFeelExecption e) {
			throw new RuntimeException(e);
		}

	}
        
	private JPanel generateMenu() {
		JPanel menu = new JPanel();
		menu.setLayout(new GridBagLayout());
                GridBagConstraints gbc = null;

                gbc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
		menu.add(createToolChangeBar(), gbc);

                // do we have a military building?
                switch (definition.getBuilding().getType()) {
                    case BIG_TOWER:
                    case CASTLE:
                    case LOOKOUT_TOWER:
                    case TOWER:
                        gbc = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
                        menu.add(createMilitaryMenu(), gbc);
                        break;
                    default:
                        break;
                }

		JButton xmlButton = new JButton("show xml data");
		xmlButton.addActionListener(event -> {
                    try {
                        showXML();
                    } catch (Exception e) {
                        e.printStackTrace(System.err);
                        JOptionPane.showMessageDialog(null, "Could not generate XML");
                    }
                });
                gbc = new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
		menu.add(xmlButton, gbc);
		positionDisplayer = new JLabel();
		menu.add(positionDisplayer);
		return menu;
	}

	private IMapInterfaceConnector startMapWindow() throws JSettlersLookAndFeelExecption {
		return SwingManagedJSettlers.showJSettlers(new FakeMapGame(map));
	}

        /**
         * Activates the given tool and reloads the map colors.
         *
         * @param tt the tool to activate
         */
        private void activateToolType(ToolType tt) {
            tool = tt;
            reloadMapColor();
        }

	private Component createToolChangeBar() {
            //JPanel result = new JXTaskPane("Select tool...");
            JPanel result = new JPanel();
            result.setBorder(new TitledBorder("Tool"));
            result.setLayout(new GridBagLayout());

            GridBagConstraints gbc;
            int y = 0;
            for(ToolType tt: ToolType.values()) {
                JButton b = new JButton(tt.toString());
                b.addActionListener(ae -> activateToolType(tt));
                gbc = new GridBagConstraints(0, y++, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
                result.add(b, gbc);
            }

            return result;
	}

        public Component createMilitaryMenu() {
            JPanel result = new JPanel();
            result.setBorder(new TitledBorder("Military"));
            result.setLayout(new GridBagLayout());

            GridBagConstraints gbc = null;

            JCheckBox cb = new JCheckBox("populated");
            cb.addChangeListener(e -> {
                if (e.getSource() instanceof JCheckBox) {
                    JCheckBox cb2 = (JCheckBox)e.getSource();
                    PseudoBuilding building = (PseudoBuilding)map.getBuilding();
                    if (cb2.isSelected()) {
                        building.occupy();
                    } else {
                        building.evacuate();
                    }
                }
            });
            gbc = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
            result.add(cb, gbc);

            JButton btEditPlaces = new JButton("Edit places...");
            btEditPlaces.addActionListener(e -> {
                showPlacesEditor();
            });
            gbc = new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
            result.add(btEditPlaces, gbc);

            return result;
        }

        /**
         * Asks the user which building type to edit.
         * 
         * @return the building type, or null if the user cancelled
         */
	private EBuildingType askType() {
		EBuildingType[] buildingTypes = EBuildingType.values();
		Arrays.sort(buildingTypes, Comparator.comparing(EBuildingType::name));
		return (EBuildingType) JOptionPane.showInputDialog(null, "Select building type", "Building Type", JOptionPane.QUESTION_MESSAGE, null, buildingTypes, null);
	}

        /**
         * Asks the user which building type variant to edit.
         * 
         * @return the variant, or null if the user cancelled
         */
	private BuildingVariant askVariant(EBuildingType type) {
                if (type == null) {
                    throw new IllegalArgumentException("type must not be null");
                }
		BuildingVariant[] variants = type.getVariants();
		ECivilisation[] civs = Arrays.stream(variants).map(BuildingVariant::getCivilisation).toArray(ECivilisation[]::new);
		return type.getVariant((ECivilisation)JOptionPane.showInputDialog(null, "Select building variant", "Building Variant", JOptionPane.QUESTION_MESSAGE, null, civs, null));
	}

	public static void main(String[] args) throws InvocationTargetException, InterruptedException, IOException {
		SwingManagedJSettlers.setupResources(true, args);
		SwingUtilities.invokeAndWait(new BuildingCreatorApp());
	}

	@Override
	public void action(final IAction action) {
		SwingUtilities.invokeLater(() -> doAction(action));
	}

	private void doAction(IAction action) {
		if (action instanceof PointAction) {
			PointAction sAction = (PointAction) action;
			ShortPoint2D pos = sAction.getPosition();
			RelativePoint relative = absoluteToRelative(pos);

			positionDisplayer.setText("x = " + (pos.x - BuildingtestMap.OFFSET) + ", y = " + (pos.y - BuildingtestMap.OFFSET));

			switch (tool) {

			case SET_BLOCKED:
				toogleUsedTile(relative);
				break;
			case SET_DOOR:
				setDoor(relative);
				break;
			case ADD_CONSTRUCTION_STACK:
				addConstructionStack(relative);
				break;
			case ADD_REQUEST_STACK:
			case ADD_OFFER_STACK:
				addStack(relative, tool == ToolType.ADD_REQUEST_STACK);
				break;
			case REMOVE_STACK:
				removeStack(relative);
				break;
			case SET_FLAG:
				setFlag(relative);
				break;
			case SET_BUILDMARK:
				definition.toggleBuildmarkStatus(relative);
				break;
			case BRICKLAYER_NE:
				definition.toggleBrickayer(relative, EDirection.NORTH_EAST);
				break;
			case BRICKLAYER_NW:
				definition.toggleBrickayer(relative, EDirection.NORTH_WEST);
				break;
			}

			reloadColor(pos);
		}
	}

	private void removeStack(RelativePoint relative) {
		definition.removeStack(relative);
	}

	private void addStack(RelativePoint relative, boolean requestStack) {
		EMaterialType material = requestMaterialType(tool);

		if (material != null) {
			if (requestStack) {
				definition.setRequestStack(relative, material);
			} else {
				definition.setOfferStack(relative, material);
			}
		}
	}

	private void addConstructionStack(RelativePoint relative) {
		EMaterialType material = requestMaterialType(tool);

		Integer buildrequired = (Integer) JOptionPane.showInputDialog(null, "Select building type", "Building Type", JOptionPane.QUESTION_MESSAGE,
				null, new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8 }, tool);

		if (material != null && buildrequired != null) {
			definition.setConstructionStack(relative, material, buildrequired);
		}
	}

	private EMaterialType requestMaterialType(ToolType tool) {
		EMaterialType[] materialTypes = EMaterialType.values();
		Arrays.sort(materialTypes, Comparator.comparing(EMaterialType::name));

		return (EMaterialType) JOptionPane.showInputDialog(null, "Select Material Type", "Material Type",
				JOptionPane.QUESTION_MESSAGE, null, materialTypes, tool);
	}

	private void setDoor(RelativePoint tile) {
		RelativePoint oldDoor = definition.getDoor();
		ShortPoint2D oldPos = relativeToAbsolute(oldDoor);
		reloadColor(oldPos);

		definition.setDoor(tile);
	}

	private void setFlag(RelativePoint tile) {
		RelativePoint oldFlag = definition.getFlag();
		ShortPoint2D oldPos = relativeToAbsolute(oldFlag);
		reloadColor(oldPos);

		definition.setFlag(tile);
	}

	private ShortPoint2D relativeToAbsolute(RelativePoint oldDoor) {
		return new ShortPoint2D(oldDoor.getDx() + BuildingtestMap.OFFSET, oldDoor.getDy() + BuildingtestMap.OFFSET);
	}

	private RelativePoint absoluteToRelative(ShortPoint2D pos) {
		return new RelativePoint(pos.x - BuildingtestMap.OFFSET, pos.y - BuildingtestMap.OFFSET);
	}

	private void toogleUsedTile(RelativePoint relative) {
		if (definition.getBlockedStatus(relative)) {
			definition.setBlockedStatus(relative, false, false);
		} else if (definition.getProtectedStatus(relative)) {
			definition.setBlockedStatus(relative, true, true);
		} else {
			definition.setBlockedStatus(relative, true, false);
		}
	}

        /**
         * Reloads the color for the whole map.
         */
        private void reloadMapColor() {
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getWidth(); y++) {
                    reloadColor(new ShortPoint2D(x, y));
                }
            }
        }

        /**
         * Reloads the color for one map tile.
         *
         * @param pos the position to reload
         */
	private void reloadColor(ShortPoint2D pos) {
		PseudoTile tile = map.getTile(pos);
		ArrayList<Color> colors = new ArrayList<>();

		RelativePoint relative = absoluteToRelative(pos);
		if (definition.getBlockedStatus(relative)) {
			colors.add(new Color(0xff0343df));
		} else if (definition.getProtectedStatus(relative)) {
			colors.add(new Color(0xff75bbfd));
		}

		switch (tool) {
		case SET_BUILDMARK:
			if (definition.getBuildmarkStatus(relative)) {
				colors.add(new Color(0xfff97306));
			}
			break;

		case SET_DOOR:
			if (definition.getDoor().equals(relative)) {
				colors.add(new Color(0xfff97306));
			}
			break;

		case SET_FLAG:
			if (definition.getFlag().equals(relative)) {
				colors.add(new Color(0xfff97306));
			}
			break;

		case ADD_CONSTRUCTION_STACK:
			checkAddConstructionStack(tile, colors, relative);
			break;

		case ADD_REQUEST_STACK:
			checkAddRequestStack(tile, colors, relative);
			break;

		case ADD_OFFER_STACK:
			checkAddOfferStack(tile, colors, relative);
			break;

		case REMOVE_STACK:
			checkAddConstructionStack(tile, colors, relative);
			checkAddRequestStack(tile, colors, relative);
			checkAddOfferStack(tile, colors, relative);
			break;

		case BRICKLAYER_NE:
		case BRICKLAYER_NW:
			if (definition.getBricklayerStatus(relative)) {
				colors.add(new Color(0xfff97306));
			}
			break;

		default:
			break;
		}

		if (!colors.isEmpty()) {
			tile.setDebugColor(mixColors(colors));
		} else {
			tile.setDebugColor(0);
		}
	}

	private void checkAddConstructionStack(PseudoTile tile, ArrayList<Color> colors, RelativePoint relative) {
		RelativeStack stack = getStackAt(relative, definition.getConstructionStacks());
		if (stack != null) {
			colors.add(new Color(0xfff97386));
		}
		tile.setStack(new MapStack(stack));
	}

	private void checkAddRequestStack(PseudoTile tile, ArrayList<Color> colors, RelativePoint relative) {
		RelativeStack stack = getStackAt(relative, definition.getRequestStacks());
		if (stack != null) {
			colors.add(new Color(0xfff973F6));
		}
		tile.setStack(new MapStack(stack));
	}

	private void checkAddOfferStack(PseudoTile tile, ArrayList<Color> colors, RelativePoint relative) {
		RelativeStack stack = getStackAt(relative, definition.getOfferStacks());
		if (stack != null) {
			colors.add(new Color(0xfff97306));
		}
		tile.setStack(new MapStack(stack));
	}

	private static RelativeStack getStackAt(RelativePoint position, List<? extends RelativeStack> stacks) {
		int indexOf = stacks.indexOf(position);
		if (indexOf >= 0) {
			return stacks.get(indexOf);
		} else {
			return null;
		}
	}

        /**
         * Adds all colors and returns the sum.
         * The sum is built by averaging out all the red/green/blue channels.
         *
         * @param colors the colors to mix
         * @return the resulting color
         */
	private static int mixColors(ArrayList<Color> colors) {
            float bluesum = 0;
            float redsum = 0;
            float greensum = 0;
            for (Color color : colors) {
                bluesum += color.getBlue();
                redsum += color.getRed();
                greensum += color.getGreen();
            }
            return Color.getARGB(redsum / colors.size(), greensum / colors.size(), bluesum / colors.size(), 1);
	}

        /**
         * Constructs the DOM structure that gets serialized to a String. This
         * String is then displayed in a window.
         * 
         * @throws ParserConfigurationException something went wrong
         * @throws TransformerException something went wrong
         */
	private void showXML() throws ParserConfigurationException, TransformerException, IOException {
            
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = db.newDocument();
            Element eBuilding = doc.createElement("building");
            if (definition.getBuilding().getWorkerType() != null) {
                eBuilding.setAttribute("worker", String.valueOf(definition.getBuilding().getWorkerType()));
            } else {
                eBuilding.setAttribute("worker", "");
            }
            eBuilding.setAttribute("viewdistance", "40");
            
            eBuilding.appendChild(doc.createComment("Ground Types"));
            for (ELandscapeType ground :definition.getBuilding().getGroundTypes()) {
                Element eGround = doc.createElement("ground");
                eGround.setAttribute("groundtype", String.valueOf(ground));
                eBuilding.appendChild(eGround);
            }
            
            eBuilding.appendChild(doc.createComment("blocked tiles"));
            for (RelativePoint tile : definition.getBlocked()) {
                Element eBlocked = doc.createElement("blocked");
                eBlocked.setAttribute("dx", String.valueOf(tile.getDx()));
                eBlocked.setAttribute("dy", String.valueOf(tile.getDy()));
                eBuilding.appendChild(eBlocked);
            }

            eBuilding.appendChild(doc.createComment("protected tiles"));
            for (RelativePoint tile : definition.getJustProtected()) {
                Element eProtected = doc.createElement("blocked");
                eProtected.setAttribute("dx", String.valueOf(tile.getDx()));
                eProtected.setAttribute("dy", String.valueOf(tile.getDy()));
                eProtected.setAttribute("block", "false");
                eBuilding.appendChild(eProtected);
            }

            eBuilding.appendChild(doc.createComment("door"));
            {
                RelativePoint door = definition.getDoor();
                Element eDoor = doc.createElement("door");
                eDoor.setAttribute("dx", String.valueOf(door.getDx()));
                eDoor.setAttribute("dy", String.valueOf(door.getDy()));
                eBuilding.appendChild(eDoor);
            }

            eBuilding.appendChild(doc.createComment("construction stacks"));
            for (ConstructionStack stack : definition.getConstructionStacks()) {
                Element eStack = doc.createElement("constructionStack");
                eStack.setAttribute("dx", String.valueOf(stack.getDx()));
                eStack.setAttribute("dy", String.valueOf(stack.getDy()));
                eStack.setAttribute("material", stack.getMaterialType().name());
                eStack.setAttribute("buildrequired", String.valueOf(stack.requiredForBuild()));
                eBuilding.appendChild(eStack);
            }

            eBuilding.appendChild(doc.createComment("request stacks"));
            for (RelativeStack stack : definition.getRequestStacks()) {
                Element eStack = doc.createElement("requestStack");
                eStack.setAttribute("dx", String.valueOf(stack.getDx()));
                eStack.setAttribute("dy", String.valueOf(stack.getDy()));
                eStack.setAttribute("material", stack.getMaterialType().name());
                eBuilding.appendChild(eStack);
            }

            eBuilding.appendChild(doc.createComment("offer stack"));
            for (RelativeStack stack : definition.getOfferStacks()) {
                Element eStack = doc.createElement("offerStack");
                eStack.setAttribute("dx", String.valueOf(stack.getDx()));
                eStack.setAttribute("dy", String.valueOf(stack.getDy()));
                eStack.setAttribute("material", stack.getMaterialType().name());
                eBuilding.appendChild(eStack);
            }

            eBuilding.appendChild(doc.createComment("brick layer"));
            for (RelativeDirectionPoint bricklayer : definition.getBricklayers()) {
                Element eBrickLayer = doc.createElement("bricklayer");
                eBrickLayer.setAttribute("dx", String.valueOf(bricklayer.getDx()));
                eBrickLayer.setAttribute("dy", String.valueOf(bricklayer.getDy()));
                eBrickLayer.setAttribute("direction", String.valueOf(bricklayer.getDirection()));
                eBuilding.appendChild(eBrickLayer);
            }
            
            eBuilding.appendChild(doc.createComment("flag"));
            {
		RelativePoint flag = definition.getFlag();
                Element eFlag = doc.createElement("flag");
                eFlag.setAttribute("dx", String.valueOf(flag.getDx()));
                eFlag.setAttribute("dy", String.valueOf(flag.getDy()));
                eBuilding.appendChild(eFlag);
            }

            eBuilding.appendChild(doc.createComment("build marks"));
            for (RelativePoint mark : definition.getBuildmarks()) {
                Element eBuildMark = doc.createElement("buildmark");
                eBuildMark.setAttribute("dx", String.valueOf(mark.getDx()));
                eBuildMark.setAttribute("dy", String.valueOf(mark.getDy()));
                eBuilding.appendChild(eBuildMark);
            }
            
            eBuilding.appendChild(doc.createComment("smoke position"));
            {
                RelativePoint smoke = definition.getBuilding().getSmokePosition();
                Element eSmoke = doc.createElement("smokePosition");
                eSmoke.setAttribute("dx", String.valueOf(smoke.getDx()));
                eSmoke.setAttribute("dy", String.valueOf(smoke.getDy()));
                eBuilding.appendChild(eSmoke);
            }
            
            eBuilding.appendChild(doc.createComment("oven position"));
            {
                RelativePoint oven = definition.getBuilding().getOvenPosition();
                Element eOven = doc.createElement("ovenPosition");
                eOven.setAttribute("dx", String.valueOf(oven.getDx()));
                eOven.setAttribute("dy", String.valueOf(oven.getDy()));
                eBuilding.appendChild(eOven);
            }
            
            eBuilding.appendChild(doc.createComment("image data"));
            {
                ImageLink image = definition.getBuilding().getGuiImage();
                Element eImage = doc.createElement("image");
                eImage.setAttribute("file", String.valueOf(image.getFile()));
                eImage.setAttribute("type", String.valueOf(image.getType()));
                eImage.setAttribute("sequence", String.valueOf(image.getSequence()));
                eImage.setAttribute("image", String.valueOf(image.getImageIndex()));
                eImage.setAttribute("for", "GUI");
                eBuilding.appendChild(eImage);
            }
            for (ImageLink image: definition.getBuilding().getImages()) {
                Element eImage = doc.createElement("image");
                eImage.setAttribute("file", String.valueOf(image.getFile()));
                eImage.setAttribute("type", String.valueOf(image.getType()));
                eImage.setAttribute("sequence", String.valueOf(image.getSequence()));
                eImage.setAttribute("image", String.valueOf(image.getImageIndex()));
                eImage.setAttribute("for", "FINAL");
                eBuilding.appendChild(eImage);
            }
            for (ImageLink image: definition.getBuilding().getBuildImages()) {
                Element eImage = doc.createElement("image");
                eImage.setAttribute("file", String.valueOf(image.getFile()));
                eImage.setAttribute("type", String.valueOf(image.getType()));
                eImage.setAttribute("sequence", String.valueOf(image.getSequence()));
                eImage.setAttribute("image", String.valueOf(image.getImageIndex()));
                eImage.setAttribute("for", "BUILD");
                eBuilding.appendChild(eImage);
            }
            
            eBuilding.appendChild(doc.createComment("occupier places"));
            for (OccupierPlace op: definition.getBuilding().getOccupierPlaces()) {
                Element eOccupierPlace = doc.createElement("occupyer");
                eOccupierPlace.setAttribute("type", String.valueOf(op.getSoldierClass()));
                eOccupierPlace.setAttribute("offsetX", String.valueOf(op.getOffsetX()));
                eOccupierPlace.setAttribute("offsetY", String.valueOf(op.getOffsetY()));
                eOccupierPlace.setAttribute("soldierX", String.valueOf(op.getPosition().getDx()));
                eOccupierPlace.setAttribute("soldierY", String.valueOf(op.getPosition().getDy()));
                if (ESoldierClass.INFANTRY == op.getSoldierClass()) {
                    eOccupierPlace.setAttribute("looksRight", String.valueOf(op.looksRight()));
                }
                eBuilding.appendChild(eOccupierPlace);
            }
            
            doc.appendChild(eBuilding);

            // Transform the DOM to XML string
            StringWriter sw = new StringWriter();
            URL url = getClass().getResource("/building.xslt");
            Transformer t = TransformerFactory.newInstance().newTransformer(new StreamSource(url.openStream()));
            t.transform(new DOMSource(doc), new StreamResult(sw));

            JDialog dialog = new JDialog(window, "xml");
            JTextArea jta = new JTextArea(sw.toString());
            jta.setWrapStyleWord(true);
            jta.setLineWrap(true);
            dialog.add(new JScrollPane(jta));
            dialog.pack();
            dialog.setSize(700, 900);
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
	}

        private void showPlacesEditor() {
            JDialog placesEditor = new JDialog(window, "Places Editor");

            PseudoBuilding building = (PseudoBuilding)map.getBuilding();
            placesEditor.add(new OccupierPlacesEditor(building));

            placesEditor.pack();
            placesEditor.setLocationRelativeTo(window);
            placesEditor.setVisible(true);
        }
}
