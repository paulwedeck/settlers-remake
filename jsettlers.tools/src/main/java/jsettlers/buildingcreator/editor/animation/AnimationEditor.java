/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jsettlers.buildingcreator.editor.animation;

import java.awt.Dimension;
import javax.swing.JPanel;
import jsettlers.common.buildings.BuildingVariant;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.sequence.Sequence;
import jsettlers.graphics.map.draw.ImageProvider;

/**
 *
 * @author hiran
 */
public class AnimationEditor extends JPanel {
    
    private BuildingVariant building;
    private AnimationPane animationPane;
    
    public AnimationEditor() {
        animationPane = new AnimationPane();
        Dimension size = new Dimension(400,400);
        animationPane.setMinimumSize(size);
        animationPane.setPreferredSize(size);
        add(animationPane);
    }

    public BuildingVariant getBuilding() {
        return building;
    }

    public void setBuilding(BuildingVariant building) {
        this.building = building;
        
        Sequence<? extends Image> seq = ImageProvider.getInstance().getSettlerSequence(
            ImageProvider.getInstance().getGFXBuildingFileIndex(building.getCivilisation()), 
            ImageProvider.getInstance().getMillRotationIndex(building.getCivilisation())
        );
        animationPane.setSequence(seq, new StepNameProvider() {
            @Override
            public String getStepName(int i) {
                return "mill-"+i;
            }
        });
    }
}
