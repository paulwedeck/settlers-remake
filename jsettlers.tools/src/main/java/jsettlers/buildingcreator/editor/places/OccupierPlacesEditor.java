/*
 */
package jsettlers.buildingcreator.editor.places;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Arrays;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicBorders;
import jsettlers.buildingcreator.editor.map.PseudoBuilding;
import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.buildings.OccupierPlace;

/**
 * Allows editing a list of places with create/update/delete function.
 * 
 * @author hiran
 */
public class OccupierPlacesEditor extends JPanel {
    
    private JList<OccupierPlace> list;
    private DefaultListModel<OccupierPlace> listModel;
    private PseudoBuilding building;
    
    public OccupierPlacesEditor(PseudoBuilding building) {
        setLayout(new BorderLayout());
        
        // toolbar to add/remove places
        JPanel toolbar = new JPanel(new FlowLayout());
        toolbar.add(new JButton("Add..."));
        toolbar.add(new JButton("Remove"));
        add(toolbar, BorderLayout.NORTH);
        
        list = new JList<>();
        list.setCellRenderer(new ListCellRenderer<OccupierPlace>() {

            private OccupierPlaceEditor renderer;
            private Border selectedBorder;
            private Border normalBorder;
            
            @Override
            public Component getListCellRendererComponent(JList<? extends OccupierPlace> list, OccupierPlace value, int index, boolean isSelected, boolean cellHasFocus) {
                if (renderer == null) {
                    renderer = new OccupierPlaceEditor();
                    renderer.setEnabled(false);
                    
                    selectedBorder = new LineBorder(list.getSelectionBackground(), 3);
                    normalBorder = new LineBorder(list.getBackground(), 3);
                }

                if (cellHasFocus) {
                    renderer.setEnabled(true);
                    renderer.grabFocus();
                } else {
                    renderer.setEnabled(false);
                }
                
                if (isSelected) {
                    renderer.setBorder(selectedBorder);
                    renderer.setBackground(list.getSelectionBackground());
                    renderer.setForeground(list.getSelectionForeground());
                } else {
                    renderer.setBorder(normalBorder);
                    renderer.setBackground(list.getBackground());
                    renderer.setForeground(list.getForeground());
                }
                
                renderer.setData(value);
                
                return renderer;
            }
        });
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent lse) {
                if (!lse.getValueIsAdjusting()) {
                    System.out.println("valuechanged "+lse);
                    OccupierPlace selected = list.getSelectedValue();
                    building.selectOccupierPlace(selected);
                }
            }
        });
        add(new JScrollPane(list), BorderLayout.CENTER);
        
        this.building = building;
        listModel = new DefaultListModel<OccupierPlace>();
        listModel.addAll(Arrays.asList(building.getBuildingVariant().getOccupierPlaces()));
        list.setModel(listModel);
    }
    
}
