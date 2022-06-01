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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

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

import javax.swing.BoxLayout;
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeEvent;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.buildings.IBuildingOccupier;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.IPlayer;
import jsettlers.logic.movable.Movable;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXTaskPane;

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

	@Override
	public void run() {
		try {
			EBuildingType type = askType();
			BuildingVariant variant = askVariant(type);

			definition = new BuildingDefinition(variant);
			map = new BuildingtestMap(definition);
                        
			for (int x = 0; x < map.getWidth(); x++) {
				for (int y = 0; y < map.getHeight(); y++) {
					reloadColor(new ShortPoint2D(x, y));
				}
			}

			IMapInterfaceConnector connector = startMapWindow();
			connector.addListener(this);

			JPanel menu = generateMenu();

			window = new JFrame("Edit " + variant.toString());
			window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			window.add(menu);
			window.pack();
			window.setVisible(true);

			connector.fireAction(new Action(EActionType.TOGGLE_DEBUG));
		} catch (JSettlersLookAndFeelExecption e) {
			throw new RuntimeException(e);
		}
                
	}

	private JPanel generateMenu() {
		JPanel menu = new JPanel();
		menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
		menu.add(createToolChangeBar());
                
                menu.add(createBuildingStatus());

		JButton xmlButton = new JButton("show xml data");
		xmlButton.addActionListener(e -> showXML());
		menu.add(xmlButton);
		positionDisplayer = new JLabel();
		menu.add(positionDisplayer);
		return menu;
	}

	private IMapInterfaceConnector startMapWindow() throws JSettlersLookAndFeelExecption {
		return SwingManagedJSettlers.showJSettlers(new FakeMapGame(map));
	}

        private void occupyBulding() {
            BuildingVariant variant = definition.getBuilding();
            List<IBuildingOccupier> occupiers = null;
            IBuilding.IOccupied building = (IBuilding.IOccupied)map.getBuilding();
            occupiers = (List<IBuildingOccupier>)building.getOccupiers();
            for (OccupierPlace op: variant.getOccupierPlaces()) {
                if (op != null) {
                    switch (op.getSoldierClass()) {
                        case BOWMAN:
                            occupiers.add( new BuildingOccupier(op, new GraphicsMovable(EMovableType.BOWMAN_L3, IPlayer.DummyPlayer.getCached((byte)0))) );
                            break;
                        case INFANTRY:
                            occupiers.add( new BuildingOccupier(op, new GraphicsMovable(EMovableType.SWORDSMAN_L3, IPlayer.DummyPlayer.getCached((byte)0))) );
                            break;
                        default:
                            break;
                    }
                }
            }
        }

        private void evacuateBulding() {
            List<IBuildingOccupier> occupiers = null;
            IBuilding.IOccupied building = (IBuilding.IOccupied)map.getBuilding();
            occupiers = (List<IBuildingOccupier>)building.getOccupiers();
            occupiers.clear();
        }
        
        private void activateToolType(ToolType tt) {
            
            tool = tt;

            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getWidth(); y++) {
                    reloadColor(new ShortPoint2D(x, y));
                }
            }
        }

	private Component createToolChangeBar() {
            JPanel result = new JXTaskPane("Select tool...");
            
            for(ToolType tt: ToolType.values()) {
                JButton b = new JButton(tt.toString());
                b.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        activateToolType(tt);
                    }
                });
                result.add(b);
            }

            return result;
	}
        
        public Component createBuildingStatus() {
            JPanel result = new JXTaskPane("Building Status...");
            JCheckBox cb = new JCheckBox("Occupied");
            cb.addChangeListener(e -> {
                if (e.getSource() instanceof JCheckBox) {
                    JCheckBox cb2 = (JCheckBox)e.getSource();
                    if (cb2.isSelected()) {
                        occupyBulding();
                    } else {
                        evacuateBulding();
                    }
                }
            });
            result.add(cb);
            return result;
        }

	private EBuildingType askType() {
		EBuildingType[] buildingTypes = EBuildingType.values();
		Arrays.sort(buildingTypes, Comparator.comparing(EBuildingType::name));
		return (EBuildingType) JOptionPane.showInputDialog(null, "Select building type", "Building Type", JOptionPane.QUESTION_MESSAGE, null, buildingTypes, null);
	}

	private BuildingVariant askVariant(EBuildingType type) {
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

	private void showXML() {
		StringBuilder builder = new StringBuilder("");
		for (RelativePoint tile : definition.getBlocked()) {
			builder.append("\t<blocked dx=\"");
			builder.append(tile.getDx());
			builder.append("\" dy=\"");
			builder.append(tile.getDy());
			builder.append("\" />\n");
		}
		builder.append("\n");
		for (RelativePoint tile : definition.getJustProtected()) {
			builder.append("\t<blocked dx=\"");
			builder.append(tile.getDx());
			builder.append("\" dy=\"");
			builder.append(tile.getDy());
			builder.append("\" block=\"false\" />\n");
		}
		builder.append("\n");

		RelativePoint door = definition.getDoor();
		builder.append("\t<door dx=\"");
		builder.append(door.getDx());
		builder.append("\" dy=\"");
		builder.append(door.getDy());
		builder.append("\" />\n");
		builder.append("\n");

		for (ConstructionStack stack : definition.getConstructionStacks()) {
			builder.append("\t<constructionStack dx=\"");
			builder.append(stack.getDx());
			builder.append("\" dy=\"");
			builder.append(stack.getDy());
			builder.append("\" material=\"");
			builder.append(stack.getMaterialType().name());
			builder.append("\" buildrequired=\"");
			builder.append(stack.requiredForBuild());
			builder.append("\" />\n");
		}
		for (RelativeStack stack : definition.getRequestStacks()) {
			builder.append("\t<requestStack dx=\"");
			builder.append(stack.getDx());
			builder.append("\" dy=\"");
			builder.append(stack.getDy());
			builder.append("\" material=\"");
			builder.append(stack.getMaterialType().name());
			builder.append("\" />\n");
		}
		for (RelativeStack stack : definition.getOfferStacks()) {
			builder.append("\t<offerStack dx=\"");
			builder.append(stack.getDx());
			builder.append("\" dy=\"");
			builder.append(stack.getDy());
			builder.append("\" material=\"");
			builder.append(stack.getMaterialType().name());
			builder.append("\" />\n");
		}
		builder.append("\n");

		for (RelativeDirectionPoint bricklayer : definition.getBricklayers()) {
			builder.append("\t<bricklayer dx=\"");
			builder.append(bricklayer.getDx());
			builder.append("\" dy=\"");
			builder.append(bricklayer.getDy());
			builder.append("\" direction=\"");
			builder.append(bricklayer.getDirection());
			builder.append("\" />\n");
		}
		builder.append("\n");

		RelativePoint flag = definition.getFlag();
		builder.append("\t<flag dx=\"");
		builder.append(flag.getDx());
		builder.append("\" dy=\"");
		builder.append(flag.getDy());
		builder.append("\" />\n");
		builder.append("\n");

		for (RelativePoint mark : definition.getBuildmarks()) {
			builder.append("\t<buildmark dx=\"");
			builder.append(mark.getDx());
			builder.append("\" dy=\"");
			builder.append(mark.getDy());
			builder.append("\" />\n");
		}
		builder.append("\n");

		JDialog dialog = new JDialog(window, "xml");
		dialog.add(new JScrollPane(new JTextArea(builder.toString())));
		dialog.pack();
		dialog.setSize(700, 900);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);
	}
}
