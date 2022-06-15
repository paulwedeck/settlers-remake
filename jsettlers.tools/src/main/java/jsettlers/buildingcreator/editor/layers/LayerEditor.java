/*
 */
package jsettlers.buildingcreator.editor.layers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import jsettlers.common.buildings.BuildingVariant;
import jsettlers.common.images.ImageLink;
import jsettlers.graphics.image.Image;
import jsettlers.graphics.image.SettlerImage;
import jsettlers.graphics.map.draw.ImageProvider;

/**
 * Shows a list of layers and the selected ones as a preview.
 * Also a crosshair marks up the origin.
 * 
 * @author hiran
 */
public class LayerEditor extends JPanel {
    
    private class PointEditor extends AbstractCellEditor implements TableCellEditor, TableCellRenderer, ChangeListener {
        private JPanel panel;
        private JSpinner spX;
        private JSpinner spY;
        private Point data;
        private int editRow;
        private int editColumn;
        private boolean configuring;
        
        public PointEditor() {
            panel = new JPanel();
            panel.setLayout(new FlowLayout());
            panel.setOpaque(true);
            
            panel.add(new JLabel("X:"));
            spX = new JSpinner(new SpinnerNumberModel(0, -300, 300, 1));
            spX.getModel().addChangeListener(this);
            panel.add(spX);
            panel.add(new JLabel("Y:"));
            spY = new JSpinner(new SpinnerNumberModel(0, -300, 300, 1));
            spY.getModel().addChangeListener(this);
            panel.add(spY);
        }

        public Object getCellEditorValue() {
            return new Point(Integer.parseInt(String.valueOf(spX.getValue())), Integer.parseInt(String.valueOf(spY.getValue())));
        }

        public Component getTableCellEditorComponent(JTable jtable, Object value, boolean isSelected, int rowIndex, int columnIndex) {
            System.out.println("getTableCellEditorComponent(..., " + value + ")");
            
            configuring = true;
            data = (Point)value;
            spX.setValue(data.x);
            spY.setValue(data.y);
            editRow = rowIndex;
            editColumn = columnIndex;
            configuring = false;
            
            jtable.setRowHeight(rowIndex, Math.max(jtable.getRowHeight(rowIndex), panel.getPreferredSize().height));
            if (isSelected) {
                panel.setBackground(jtable.getSelectionBackground());
                panel.setForeground(jtable.getSelectionForeground());
            } else {
                panel.setBackground(jtable.getBackground());
                panel.setForeground(jtable.getForeground());
            }
            
            return panel;
        }

        public Component getTableCellRendererComponent(JTable jtable, Object value, boolean isSelected, boolean isFocused, int rowIndex, int columnIndex) {
            //System.out.println("getTableCellRenderComponent(..., " + value + ")");
            configuring = true;
            if (value == null) {
                spX.setValue(0);
                spY.setValue(0);
                spX.setEnabled(false);
                spY.setEnabled(false);
            } else {
                Point p = (Point)value;
                spX.setValue(p.x);
                spY.setValue(p.y);
                spX.setEnabled(true);
                spY.setEnabled(true);
            }
            configuring = false;
            
            jtable.setRowHeight(rowIndex, Math.max(jtable.getRowHeight(rowIndex), panel.getPreferredSize().height));
            if (isSelected) {
                panel.setBackground(jtable.getSelectionBackground());
                panel.setForeground(jtable.getSelectionForeground());
            } else {
                panel.setBackground(jtable.getBackground());
                panel.setForeground(jtable.getForeground());
            }
            return panel;
        }

        @Override
        public void stateChanged(ChangeEvent ce) {
            if (!configuring && data != null) {
                data.x = Integer.parseInt(String.valueOf(spX.getValue()));
                data.y = Integer.parseInt(String.valueOf(spY.getValue()));
                model.fireTableCellUpdated(editRow, editColumn);
            }
        }
        
    }
    
    /** Similar to an ImageLink, this class combines image and offset data. */
    private class ImageData {
        boolean show;
        String imageName;
        private Point offset;
        String purpose;
        Image image;
        
        public ImageData(ImageLink il, String purpose) {
            this(il);
            this.purpose = purpose;
        }
        
        public ImageData(ImageLink il) {
            show = true;
            imageName = String.format("%s_%s_%s_%s", il.getFile(), il.getType(), il.getSequence(), il.getImageIndex());
            this.image = ImageProvider.getInstance().getImage(il);
        }

        public ImageData(Image i, String purpose) {
            this(i);
            this.purpose = purpose;
        }
        
        public ImageData(Image i) {
            show = true;
            imageName = "n/a";
            this.image = i;
        }
        
        public void setOffset(Point offset) {
            this.offset = offset;
        }
        
        public Point getOffset() {
            if (offset==null) {
                if (image != null) {
                    if (image instanceof jsettlers.graphics.image.SingleImage) {
                        jsettlers.graphics.image.SingleImage si = (jsettlers.graphics.image.SingleImage)image;
                        offset = new Point(si.getOffsetX(), si.getOffsetY());
                    }
                }
            }
            
            return offset;
        }
    }
    
    private class ImageLinkTableModel extends AbstractTableModel {
        
        private String[] columnNames = new String[]{"Show", "Name", "Offset"};
        private Class[] columnClasses = new Class[]{Boolean.class, String.class, Point.class};
        
        List<ImageData> images;
        
        public ImageLinkTableModel() {
            images = new ArrayList<>();
        }
        
        private void addImage(ImageLink il, String purpose) {
            ImageData imageData = new ImageData(il, purpose);
            images.add(imageData);

            // search for shadow
            if (imageData.image instanceof SettlerImage) {
                SettlerImage simage = (SettlerImage)imageData.image;
                Image shadow = simage.getShadow();
                if (shadow != null) {
                    ImageData shadowData = new ImageData(il, "shadow");
                    shadowData.image = shadow;
                    shadowData.imageName = shadowData.imageName + "_shadow";
                    images.add(shadowData);
                }
            }
        }

        public ImageLinkTableModel(BuildingVariant building) {
            images = new ArrayList<>();
            for (ImageLink il: building.getBuildImages()) {
                addImage(il, "under construction");
            }
            for (ImageLink il: building.getImages()) {
                addImage(il, "built");
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
                    return row.show;
                case 1:
                    return row.imageName;
                case 2:
                    return row.getOffset();
                default:
                    return "n/a";
            }
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 0 || columnIndex == 2;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            ImageData row = images.get(rowIndex);
            switch(columnIndex) {
                case 0:
                    row.show = (Boolean)aValue;
                    fireTableCellUpdated(rowIndex, columnIndex);
                    break;
                case 2:
                    row.offset = (Point)aValue;
                    fireTableCellUpdated(rowIndex, columnIndex);
                    break;
                default:
                    throw new UnsupportedOperationException("not yet implemented");
            }
            
        }

        @Override
        public String getColumnName(int column) {
            return columnNames[column];
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
            
            // target point is where the shadow should be. It is calculated
            // as 30° up-right from the center point
            double rad = 30.0/180.0 * Math.PI;
            double sin = Math.sin(rad);
            Point target = new Point(getWidth(), (int)(center.getY() - sin * getWidth()/2.0) );
            
            g2d.setColor(Color.RED);
            g2d.drawLine(center.x, center.y, target.x, target.y);
            
            try {
                for(int rowIndex = 0; rowIndex<model.getRowCount(); rowIndex++) {
                    ImageData row = model.getRow(rowIndex);

                    if (!row.show) {
                        continue;
                    }

                    if (row.image != null) {
                        if (row.image instanceof jsettlers.graphics.image.SingleImage) {
                            jsettlers.graphics.image.SingleImage si = (jsettlers.graphics.image.SingleImage)row.image;

                            if (row.offset== null) {
                                row.offset = new Point(si.getOffsetX(), si.getOffsetY());
                            }

                            BufferedImage bi = si.convertToBufferedImage();
                            g2d.drawImage(bi, center.x + row.offset.x, center.y+ row.offset.y, null);
                        }
                    }

                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
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
        table.setDefaultRenderer(Point.class, new PointEditor());
        table.setDefaultEditor(Point.class, new PointEditor());
        table.setPreferredScrollableViewportSize(new Dimension(500, 500));
        add(new JScrollPane(table), BorderLayout.WEST);
        imagePanel = new ImagePanel();
        add(imagePanel, BorderLayout.CENTER);
    }
    
    public void setBuilding(BuildingVariant building) {
        this.building = building;
        model = new ImageLinkTableModel(building);
        table.setModel(model);
        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(50);
        tcm.getColumn(0).setMaxWidth(50);
        tcm.getColumn(1).setPreferredWidth(220);
        tcm.getColumn(1).setMaxWidth(220);
        imagePanel.setModel(model);
    }
}
