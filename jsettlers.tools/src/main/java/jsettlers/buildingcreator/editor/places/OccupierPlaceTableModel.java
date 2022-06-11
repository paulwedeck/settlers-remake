/*
 */
package jsettlers.buildingcreator.editor.places;

import javax.swing.table.AbstractTableModel;
import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.buildings.OccupierPlace;

/**
 *
 * @author hiran
 */
public class OccupierPlaceTableModel extends AbstractTableModel {
    
    private BuildingVariant building;
    
    public OccupierPlaceTableModel(BuildingVariant building) {
        this.building = building;
    }

    @Override
    public int getRowCount() {
        return building.getOccupierPlaces().length;
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return OccupierPlace.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return getRow(rowIndex);
    }
    
    public OccupierPlace getRow(int rowIndex) {
        return building.getOccupierPlaces()[rowIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }
}
