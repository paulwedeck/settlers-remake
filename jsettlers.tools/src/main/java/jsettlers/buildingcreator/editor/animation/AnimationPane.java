/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package jsettlers.buildingcreator.editor.animation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JComponent;
import javax.swing.Timer;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SettlerImage;
import jsettlers.graphics.image.SingleImage;
import jsettlers.graphics.image.sequence.Sequence;

/**
 *
 * @author hiran
 */
public class AnimationPane extends JComponent {
    
    private Sequence<? extends Image> sequence;
    private StepNameProvider stepNameProvider;
    
    private int step;
    private Timer timer;
    
    public AnimationPane() {
        timer = new Timer(100, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (sequence != null) {
                    step = (step + 1) % sequence.length();
                    repaint();
                }
            }
        });
    }
    
    public Sequence<? extends Image> getSequence() {
        return sequence;
    }

    public void setSequence(Sequence<? extends Image> seq, StepNameProvider stepNameProvider) {
        this.sequence = seq;
        this.stepNameProvider = stepNameProvider;
        
        if (sequence != null) {
            step = 0;
            timer.start();
        } else {
            timer.stop();
        }
        repaint();
    }
    
    public int getDelay() {
        return timer.getDelay();
    }
    
    public void setDelay(int millis) {
        timer.setDelay(millis);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        
        g2d.setColor(Color.green);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        if (sequence != null) {
            final String s = stepNameProvider.getStepName(step);
            Image image = sequence.getImageSafe(step, () -> s);
            
            if (image instanceof SettlerImage) {
                SettlerImage se = (SettlerImage)image;
                g2d.drawImage(se.convertToBufferedImage(), se.getOffsetX() + getWidth()/2, se.getOffsetY() + getHeight()/2, null);
                
                SingleImage shadow = se.getShadow();
                if (shadow != null) {
                    g2d.drawImage(shadow.convertToBufferedImage(), shadow.getOffsetX() + getWidth()/2, shadow.getOffsetY() + getHeight()/2, null);
                }
            }
        } else {
            g2d.setColor(Color.black);
            
            g2d.drawLine(0, 0, getWidth(), getHeight());
            g2d.drawLine(getHeight(), 0, getWidth(), 0);
        }
    }
    
}
