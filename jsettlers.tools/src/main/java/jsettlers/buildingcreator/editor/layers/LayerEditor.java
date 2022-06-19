/*
 */
package jsettlers.buildingcreator.editor.layers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.JCheckBox;
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
    
    private class ImageDataTableModel extends AbstractTableModel {
        
        private String[] columnNames = new String[]{"Show", "Name", "Offset"};
        private Class[] columnClasses = new Class[]{Boolean.class, String.class, Point.class};
        
        List<ImageData> images;
        
        public ImageDataTableModel() {
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

        public ImageDataTableModel(BuildingVariant building) {
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
        
        private ImageDataTableModel model;
        private boolean drawShadowAxis;
        private boolean drawAllAxis;
        private boolean drawCenter;
        private boolean drawSelection;

        public ImagePanel() {
            setPreferredSize(new Dimension(400, 400));
        }

        public ImageDataTableModel getModel() {
            return model;
        }

        public void setModel(ImageDataTableModel model) {
            if (this.model != null) {
                model.removeTableModelListener(this);
            }
            this.model = model;
            model.addTableModelListener(this);
        }

        public boolean isDrawShadowAxis() {
            return drawShadowAxis;
        }

        public void setDrawShadowAxis(boolean drawShadowAxis) {
            this.drawShadowAxis = drawShadowAxis;
            repaint();
        }

        public boolean isDrawAllAxis() {
            return drawAllAxis;
        }

        public void setDrawAllAxis(boolean drawAllAxis) {
            this.drawAllAxis = drawAllAxis;
            repaint();
        }

        public boolean isDrawCenter() {
            return drawCenter;
        }

        public void setDrawCenter(boolean drawCenter) {
            this.drawCenter = drawCenter;
            repaint();
        }

        public boolean isDrawSelection() {
            return drawSelection;
        }

        public void setDrawSelection(boolean drawSelection) {
            this.drawSelection = drawSelection;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D)g;
            g2d.setColor(Color.green);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            
            Point center = new Point(getWidth()/2, getHeight()/2);

            if (drawAllAxis) {
                // We have an isometric view, hence the axes are tilted 30 degrees
                double rad = 30.0/180.0 * Math.PI;
                double sin = Math.sin(rad);
                int offset = (int)(sin * getWidth()/2.0);
                g2d.setColor(Color.gray);
                g2d.drawLine(0, center.y + offset, getWidth(), center.y - offset);
                g2d.drawLine(0, center.y - offset, getWidth(), center.y + offset);
            }
            if (drawShadowAxis) {
                // target point is where the shadow should be. It deviates from the
                // axes since the buldings have a shadow in the front right
                // however the mountains do not?

                // It is calculated
                // as 20° up-right from the center point
                double rad = 20.0/180.0 * Math.PI;
                double sin = Math.sin(rad);
                int offset = (int)(sin * getWidth()/2.0);

                g2d.setColor(Color.pink);
                g2d.drawLine(center.x, center.y, getWidth(), center.y - offset);
            }
            
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
                
                
                if (drawSelection) {
                    int rowIndex = table.getSelectedRow();
                    if (rowIndex != -1) {
                        ImageData row = model.getRow(rowIndex);
                        if (row.image != null) {
                            if (row.image instanceof jsettlers.graphics.image.SingleImage) {
                                jsettlers.graphics.image.SingleImage si = (jsettlers.graphics.image.SingleImage)row.image;
                                BufferedImage bi = si.convertToBufferedImage();

                                g2d.setColor(Color.white);
                                g2d.drawRect(center.x + row.offset.x -1, center.y+ row.offset.y - 1, bi.getWidth() + 2, bi.getHeight() + 2);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }

            if (drawCenter) {
                g2d.setColor(Color.white);
                g2d.drawLine(center.x-5, center.y, center.x+5, center.y);
                g2d.drawLine(center.x, center.y-5, center.x, center.y+5);
            }
        }

        public void tableChanged(TableModelEvent tme) {
            repaint();
        }

    }
    
    private BuildingVariant building;
    private JTable table;
    private ImagePanel imagePanel;
    private ImageDataTableModel model;
    private Point dragStart;
    private Point layerPosition;
    
    public LayerEditor() {
        setLayout(new GridBagLayout());
        table = new JTable();
        table.setDefaultRenderer(Point.class, new PointEditor());
        table.setDefaultEditor(Point.class, new PointEditor());
        table.setPreferredScrollableViewportSize(new Dimension(500, 500));

        imagePanel = new ImagePanel();
        imagePanel.setDrawCenter(true);
        imagePanel.setDrawShadowAxis(true);
        imagePanel.addMouseListener(new MouseAdapter() {
            
            /** Determines whether a layer can be dragged.
             * Is one selected and visible?
             */
            private boolean canDragLayer() {
                int rowIndex = table.getSelectedRow();
                if (rowIndex == -1) {
                    // nothing selected
                    return false;
                } else {
                    ImageData row = (ImageData)model.getRow(rowIndex);
                    return row.show;
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (dragStart != null) {
                    imagePanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                } else if (canDragLayer()) {
                    // we can draw, show a finger cursor
                    imagePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    imagePanel.setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (canDragLayer()) {
                    imagePanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                    dragStart = e.getPoint();
                    
                    int rowIndex = table.getSelectedRow();
                    ImageData row = model.getRow(rowIndex);
                    layerPosition = row.getOffset();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStart = null;
                if (canDragLayer()) {
                    // we can draw, show a finger cursor
                    imagePanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    imagePanel.setCursor(Cursor.getDefaultCursor());
                }
            }
        });
        imagePanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    Point dragDistance = new Point(e.getPoint().x - dragStart.x, e.getPoint().y - dragStart.y);
                    
                    Point newOffset = new Point(layerPosition.x + dragDistance.x, layerPosition.y + dragDistance.y);
                    
                    int rowIndex = table.getSelectedRow();
                    ImageData row = model.getRow(rowIndex);
                    row.setOffset(newOffset);
                    
                    repaint();
                }
            }
        });
        table.getSelectionModel().addListSelectionListener((lse) -> {
            imagePanel.repaint();
        });
        
        JCheckBox cbShowCenter = new JCheckBox("Center");
        cbShowCenter.setSelected(imagePanel.isDrawCenter());
        cbShowCenter.addChangeListener((ce) -> {
            imagePanel.setDrawCenter(cbShowCenter.isSelected());
        });
        JCheckBox cbShowAxes = new JCheckBox("Axes");
        cbShowAxes.setSelected(imagePanel.isDrawAllAxis());
        cbShowAxes.addChangeListener((ce) -> {
            imagePanel.setDrawAllAxis(cbShowAxes.isSelected());
        });
        JCheckBox cbShowShadowAxis = new JCheckBox("ShadowAxis");
        cbShowShadowAxis.setSelected(imagePanel.isDrawShadowAxis());
        cbShowShadowAxis.addChangeListener((ce) -> {
            imagePanel.setDrawShadowAxis(cbShowShadowAxis.isSelected());
        });
        JCheckBox cbShowSelection = new JCheckBox("Selection");
        cbShowSelection.addChangeListener((ce) -> {
            imagePanel.setDrawSelection(cbShowSelection.isSelected());
        });
        
        add(new JScrollPane(table), 
                new GridBagConstraints(0, 0, 1, 2, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, new Insets(0,0,0,0), 0, 0));
        add(imagePanel, 
                new GridBagConstraints(1, 1, 4, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0,0,0,0), 0, 0));
        add(cbShowCenter, 
                new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2,4,2,4), 0, 0));
        add(cbShowAxes, 
                new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2,4,2,4), 0, 0));
        add(cbShowShadowAxis, 
                new GridBagConstraints(3, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2,4,2,4), 0, 0));
        add(cbShowSelection, 
                new GridBagConstraints(4, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(2,4,2,4), 0, 0));
        
    }
    
    public void setBuilding(BuildingVariant building) {
        this.building = building;
        model = new ImageDataTableModel(building);
        table.setModel(model);
        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(50);
        tcm.getColumn(0).setMaxWidth(50);
        tcm.getColumn(1).setPreferredWidth(220);
        tcm.getColumn(1).setMaxWidth(220);
        imagePanel.setModel(model);
    }
}
