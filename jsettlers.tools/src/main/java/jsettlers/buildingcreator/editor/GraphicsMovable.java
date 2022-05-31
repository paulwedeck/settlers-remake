/*
 */
package jsettlers.buildingcreator.editor;

import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EEffectType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IGraphicsMovable;
import jsettlers.common.player.IPlayer;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.selectable.ESelectionType;

/**
 *
 * @author hiran
 */
public class GraphicsMovable implements IGraphicsMovable {
    
    private IPlayer player;
    private EMovableType movableType;
    
    public GraphicsMovable(EMovableType movableType, IPlayer player) {
        this.player = player;
        this.movableType = movableType;
    }

    @Override
    public EMovableType getMovableType() {
        return EMovableType.BOWMAN_L1;
    }

    @Override
    public EMovableAction getAction() {
        return EMovableAction.NO_ACTION;
    }

    @Override
    public EDirection getDirection() {
        return EDirection.SOUTH_WEST;
    }

    @Override
    public float getMoveProgress() {
        return 0f;
    }

    @Override
    public EMaterialType getMaterial() {
        return EMaterialType.NO_MATERIAL;
    }

    @Override
    public float getHealth() {
        return 100f;
    }

    @Override
    public boolean isAlive() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isRightstep() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean hasEffect(EEffectType eet) {
        return false;
    }

    @Override
    public boolean isSelected() {
        return true;
    }

    @Override
    public void setSelected(boolean bln) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public ESelectionType getSelectionType() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isWounded() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public IPlayer getPlayer() {
        return player;
    }

    @Override
    public ShortPoint2D getPosition() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void setSoundPlayed() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public boolean isSoundPlayed() {
        return false;
    }

    @Override
    public int getID() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getName()).append("(");
        sb.append("movableType=").append(movableType);
        sb.append(", player=").append(player);
        sb.append(")");
        return sb.toString();
    }
}
