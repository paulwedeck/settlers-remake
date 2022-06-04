/*
 */
package jsettlers.buildingcreator.editor.places;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.movable.ESoldierClass;

/**
 * A UI bean that allows editing an OccupierPlace. Well, sort of since
 * currently the OccupierPlace has no write mechanism.
 * 
 * @author hiran
 */
public class OccupierPlaceEditor extends JPanel {
    private final JComboBox<ESoldierClass> cbSoldierClass;
    private final JSpinner spOffsetX;
    private final JSpinner spOffsetY;
    
    public OccupierPlaceEditor() {
        setLayout(new GridBagLayout());
        add(new JLabel("Soldier Class"), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        
        cbSoldierClass = new JComboBox<>(ESoldierClass.values());
        add(cbSoldierClass, new GridBagConstraints(1, 0, 2, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        add(new JLabel("Offset X/Y"), new GridBagConstraints(0, 1, 1, 1, 0, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        spOffsetX = new JSpinner();
        add(spOffsetX, new GridBagConstraints(1, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        spOffsetY = new JSpinner();
        add(spOffsetY, new GridBagConstraints(2, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
    }
    
    public void setData(OccupierPlace data) {
        cbSoldierClass.setSelectedItem(data.getSoldierClass());
        spOffsetX.setValue(data.getOffsetX());
        spOffsetY.setValue(data.getOffsetY());
    }
}
