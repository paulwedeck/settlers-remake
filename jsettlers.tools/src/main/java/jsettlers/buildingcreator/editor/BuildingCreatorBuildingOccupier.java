/*
 */
package jsettlers.buildingcreator.editor;

import jsettlers.common.buildings.IBuildingOccupier;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.movable.IGraphicsMovable;

/**
 * This is a simple implementation of the BuildingOccupier, just good enough
 * for the Building Creator to display occupiers.
 * 
 * @author hiran
 */
public class BuildingCreatorBuildingOccupier implements IBuildingOccupier{
    
    private IGraphicsMovable movable;
    private OccupierPlace occupierPlace;
    
    public BuildingCreatorBuildingOccupier(OccupierPlace occupierPlace, IGraphicsMovable movable) {
        this.movable = movable;
        this.occupierPlace = occupierPlace;
    }

    @Override
    public IGraphicsMovable getMovable() {
        return movable;
    }

    @Override
    public OccupierPlace getPlace() {
        return occupierPlace;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName()).append("(");
        sb.append("movable=").append(movable);
        sb.append(", occupierPlace=").append(occupierPlace);
        sb.append(")");
        return sb.toString();
    }
   
}
