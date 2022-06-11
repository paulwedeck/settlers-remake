/*
 */
package jsettlers.buildingcreator.editor.places;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import jsettlers.buildingcreator.editor.map.PseudoBuilding;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.movable.ESoldierClass;
import jsettlers.common.position.RelativePoint;

/**
 * Allows editing a list of places with create/update/delete function.
 * 
 * @author hiran
 */
public class OccupierPlacesEditor extends JPanel {
    
    private final JTable table;
    private OccupierPlaceTableModel tableModel;
    private final PseudoBuilding building;
    
    public OccupierPlacesEditor(PseudoBuilding building) {
        setLayout(new BorderLayout());

        // need to initialize list early so we can address it in action listeners
        table = new JTable();
        table.setTableHeader(null);
        table.setRowHeight((int)new OccupierPlaceEditor().getPreferredSize().getHeight());
        
        // toolbar to add/remove places
        JPanel toolbar = new JPanel(new FlowLayout());
        JButton btAdd = new JButton("Add...");
        btAdd.addActionListener((ae) -> {
            OccupierPlace op = new OccupierPlace(0, 0, ESoldierClass.INFANTRY, new RelativePoint(0, 0), true);
            OccupierPlaceEditor ope = new OccupierPlaceEditor();
            ope.setData(op);
            if (JOptionPane.showOptionDialog(this, ope, "Add new position", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, null, null) == JOptionPane.OK_OPTION) {
                building.getBuildingVariant().addOccupierPlace(op);
                updateModel();
                
                // ensure we have a soldier on the new position
                building.evacuate();
                building.occupy();
            }
        });
        toolbar.add(btAdd);
        JButton btRemove = new JButton("Remove");
        btRemove.addActionListener((ae) -> {
            int rowIndex = table.getSelectedRow();
            if (rowIndex == -1) {
                JOptionPane.showMessageDialog(this, "Please select the position to delete first.");
                return;
            }
            OccupierPlace op = tableModel.getRow(rowIndex);
            building.getBuildingVariant().removeOccupierPlace(op);
            updateModel();

            // ensure we have a soldier on the new position
            building.evacuate();
            building.occupy();
        });
        toolbar.add(btRemove);
        add(toolbar, BorderLayout.NORTH);
        
        table.setDefaultRenderer(OccupierPlace.class, new TableCellRenderer() {
            private OccupierPlaceEditor ope = new OccupierPlaceEditor();
            
            @Override
            public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected, boolean isFocused, int rowIndex, int columnIndex) {
                //ope.setData((OccupierPlace)value);
                
                ope.setOpaque(true);
                if (isSelected) {
                    ope.setForeground(jtable.getSelectionForeground());
                    ope.setBackground(jtable.getSelectionBackground());
                } else {
                    ope.setForeground(jtable.getForeground());
                    ope.setBackground(jtable.getBackground());
                }
                return ope;
            }
        });
        table.setDefaultEditor(OccupierPlace.class, new TableCellEditor() {
            private OccupierPlaceEditor ope = new OccupierPlaceEditor();
            private List<CellEditorListener> listeners = new ArrayList<>();

            @Override
            public Component getTableCellEditorComponent(JTable jtable, Object value, boolean isSelected, int rowIndex, int columnIndex) {
                ope.setData((OccupierPlace)value);
                ope.setOpaque(true);
                if (isSelected) {
                    ope.setForeground(jtable.getSelectionForeground());
                    ope.setBackground(jtable.getSelectionBackground());
                } else {
                    ope.setForeground(jtable.getForeground());
                    ope.setBackground(jtable.getBackground());
                }
                return ope;
            }

            @Override
            public Object getCellEditorValue() {
                throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
            }

            @Override
            public boolean isCellEditable(EventObject eo) {
                return true;
            }

            @Override
            public boolean shouldSelectCell(EventObject eo) {
                return true;
            }

            @Override
            public boolean stopCellEditing() {
                // we modified the unerlying objects directly, so no cleanup
                // required
                
                return true;
            }

            @Override
            public void cancelCellEditing() {
                // we modified the unerlying objects directly, so no cleanup
                // required
            }

            @Override
            public void addCellEditorListener(CellEditorListener cl) {
                if (!listeners.contains(cl)) {
                    listeners.add(cl);
                }
            }

            @Override
            public void removeCellEditorListener(CellEditorListener cl) {
                listeners.remove(cl);
            }
        });
        table.getSelectionModel().addListSelectionListener(lse -> {
            if (!lse.getValueIsAdjusting()) {
                int rowIndex = table.getSelectedRow();
                if (rowIndex == -1) {
                    building.selectOccupierPlace(null);
                } else {
                    OccupierPlace selected = tableModel.getRow(rowIndex);
                    building.selectOccupierPlace(selected);
                }
            }
        });
        JScrollPane sp = new JScrollPane(table);
        add(sp, BorderLayout.CENTER);
        
        this.building = building;
        updateModel();
    }
    
    private void updateModel() {
        tableModel = new OccupierPlaceTableModel(building.getBuildingVariant());
        table.setModel(tableModel);
    }
}
