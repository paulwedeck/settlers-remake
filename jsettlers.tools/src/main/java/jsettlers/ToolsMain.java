package jsettlers;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import jsettlers.buildingcreator.editor.BuildingCreatorApp;
import jsettlers.graphics.debug.DatFileViewer;
import jsettlers.logic.movable.MovableModelWindow;

import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

public class ToolsMain {

	public static void main(String[] args) throws Exception{
                
                JFrame f = new JFrame("Select Tool...");
                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                f.setLayout(new GridBagLayout());
                
                int y = 0;
                for (Tool t: Tool.values()) {
                    JButton b = new JButton(t.name(), t.icon);
                    b.setHorizontalAlignment(SwingConstants.LEFT);
                    b.addActionListener(e -> {
                            f.setVisible(false);
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        t.mainFunc.run(args);
                                    } catch (Exception ex) {
                                        System.err.println("ERROR: Could not invoke tool "+t);
                                        ex.printStackTrace(System.err);
                                        f.setVisible(true);
                                    }
                                }
                            }).start();
                    });
                    f.add(b, new GridBagConstraints(0, y++, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
                }
                
                f.pack();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
	}


	private enum Tool {

		DAT_FILE_VIEWER(DatFileViewer::main, "/images/photo_library_FILL0_wght400_GRAD0_opsz48.png"),
		MOVABLE_MODEL_WINDOW(MovableModelWindow::main, "/images/groups_FILL0_wght400_GRAD0_opsz48.png"),
		BUILDING_CREATOR(BuildingCreatorApp::main, "/images/other_houses_FILL0_wght400_GRAD0_opsz48.png"),
		;


		public final MainFunc mainFunc;
                public final ImageIcon icon;

                /**
                 * Creates a new Tool enum.
                 * 
                 * @param mainFunc the main function to call for this tool
                 * @param relpath the classpath icon resource
                 */
		private Tool(MainFunc mainFunc, String relpath) {
			this.mainFunc = mainFunc;
                        this.icon = new ImageIcon(getClass().getResource(relpath));
		}
	};

	private interface MainFunc {
		public void run(String[] args) throws Exception;
	}
}
