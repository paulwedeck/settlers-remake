/*
 */
package jsettlers.buildingcreator.editor.layers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.images.ImageLink;
import jsettlers.graphics.map.draw.ImageProvider;

/**
 * Shows a list of layers and the selected ones as a preview.
 * Also a crosshair marks up the origin.
 * 
 * @author hiran
 */
public class LayerEditor extends JPanel {
    
    private enum ERenderState {
        HIDDEN,
        //SEMITRANSPARENT,
        SOLID
    }
    
    private class RenderStatePanel extends JLabel implements TableCellRenderer {

        public Component getTableCellRendererComponent(JTable jtable, Object o, boolean bln, boolean bln1, int i, int i1) {
            setText(String.valueOf(o));
            
            return this;
        }
    
    }
    
    class RenderStateEditor extends AbstractCellEditor implements TableCellEditor {

        JComboBox<ERenderState> cb = new JComboBox<ERenderState>(ERenderState.values());

        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int rowIndex, int vColIndex) {
            
          //((JTextField) component).setText((String) value);

            return cb;
        }

        public Object getCellEditorValue() {
            return cb.getSelectedItem();
        }
    }    
    
    /** Similar to an ImageLink, this class combines image and offset data. */
    private class ImageData {
        ERenderState state;
        String imageName;
        int offsetX;
        int offsetY;
        String purpose;
        ImageLink imageLink;
        
        public ImageData(ImageLink il, String purpose) {
            this(il);
            this.purpose = purpose;
        }

        
        public ImageData(ImageLink il) {
            state = ERenderState.SOLID;
            imageName = String.format("%s_%s_%s_%s", il.getFile(), il.getType(), il.getSequence(), il.getImageIndex());
            offsetX = 0;
            offsetY = 0;
            jsettlers.graphics.map.draw.ImageProvider.getInstance().getImage(il);
            this.imageLink = il;
        }
    }
    
    private class ImageLinkTableModel extends AbstractTableModel {
        
        private String[] columnNames = new String[]{"RenderState", "Name"};
        private Class[] columnClasses = new Class[]{ERenderState.class, String.class};
        
        List<ImageData> images;
        
        public ImageLinkTableModel() {
            images = new ArrayList<>();
        }

        public ImageLinkTableModel(BuildingVariant building) {
            images = new ArrayList<>();
            for (ImageLink il: building.getBuildImages()) {
                images.add(new ImageData(il, "under construction"));
            }
            for (ImageLink il: building.getImages()) {
                images.add(new ImageData(il, "built"));
            }
        }

        public int getRowCount() {
            return images.size();
        }

        public int getColumnCount() {
            return columnNames.length;
        }
        
        public ImageData getRow(int rowIndex) {
            return images.get(rowIndex);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return columnClasses[columnIndex];
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            ImageData row = images.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return row.state;
                case 1:
                    return row.imageName;
                default:
                    return "n/a";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            System.out.println("setValueAt("+aValue+", ...)");
            if (columnIndex!=0)
                throw new UnsupportedOperationException("not yet implemented");
            
            ImageData row = images.get(rowIndex);
            row.state = (ERenderState)aValue;
            
            fireTableCellUpdated(rowIndex, columnIndex);
        }
        
        
    }
    
    private class ImagePanel extends JPanel implements TableModelListener {
        
        private ImageLinkTableModel model;

        public ImagePanel() {
            setPreferredSize(new Dimension(400, 400));
        }

        public ImageLinkTableModel getModel() {
            return model;
        }

        public void setModel(ImageLinkTableModel model) {
            if (this.model != null) {
                model.removeTableModelListener(this);
            }
            this.model = model;
            model.addTableModelListener(this);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setColor(Color.green);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            Point center = new Point(getWidth()/2, getHeight()/2);
            
            for(int rowIndex = 0; rowIndex<model.getRowCount(); rowIndex++) {
                ImageData row = model.getRow(rowIndex);
                
                if (row.state == ERenderState.HIDDEN) {
                    System.out.println("skipped "+row.imageName);
                    continue;
                }

                jsettlers.graphics.image.Image image = ImageProvider.getInstance().getImage(row.imageLink);
                if (image != null) {
                    if (image instanceof jsettlers.graphics.image.SingleImage) {
                        jsettlers.graphics.image.SingleImage si = (jsettlers.graphics.image.SingleImage)image;
                        BufferedImage bi = si.convertToBufferedImage();
                        System.out.println("drawing "+image.getClass().getName()+": "+row.imageName);
                        g2d.drawImage(bi, center.x + si.getOffsetX(), center.y+si.getOffsetY(), null);
                        System.out.println("painted "+row.imageName);
                    }
                }

            }
            
            g2d.setColor(Color.white);
            g2d.drawLine(center.x-5, center.y, center.x+5, center.y);
            g2d.drawLine(center.x, center.y-5, center.x, center.y+5);
        }

        public void tableChanged(TableModelEvent tme) {
            repaint();
        }

    }
    
    private BuildingVariant building;
    private JTable table;
    private ImagePanel imagePanel;
    private ImageLinkTableModel model;
    
    public LayerEditor() {
        setLayout(new BorderLayout());
        table = new JTable();
        table.setDefaultRenderer(ERenderState.class, new RenderStatePanel());
        table.setDefaultEditor(ERenderState.class, new RenderStateEditor());
        add(new JScrollPane(table), BorderLayout.WEST);
        imagePanel = new ImagePanel();
        add(imagePanel, BorderLayout.CENTER);
    }
    
    public void setBuilding(BuildingVariant building) {
        this.building = building;
        model = new ImageLinkTableModel(building);
        table.setModel(model);
        imagePanel.setModel(model);
    }
}
