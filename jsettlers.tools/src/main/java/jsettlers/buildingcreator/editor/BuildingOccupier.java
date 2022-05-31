/*
 */
package jsettlers.buildingcreator.editor;

import jsettlers.common.buildings.IBuildingOccupier;
import jsettlers.common.buildings.OccupierPlace;
import jsettlers.common.movable.IGraphicsMovable;

/**
 *
 * @author hiran
 */
public class BuildingOccupier implements IBuildingOccupier{
    
    private IGraphicsMovable movable;
    private OccupierPlace occupierPlace;
    
    public BuildingOccupier(OccupierPlace occupierPlace, IGraphicsMovable movable) {
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
