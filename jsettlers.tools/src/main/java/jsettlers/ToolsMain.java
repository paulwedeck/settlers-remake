package jsettlers;

import jsettlers.buildingcreator.editor.BuildingCreatorApp;
import jsettlers.graphics.debug.DatFileViewer;
import jsettlers.logic.movable.MovableModelWindow;

import javax.swing.JOptionPane;

public class ToolsMain {

	public static void main(String[] args) throws Exception{
		Tool start = (Tool) JOptionPane.showInputDialog(null, "Select Tool", "Tool selection", JOptionPane.QUESTION_MESSAGE, null, Tool.values(), null);
		start.mainFunc.run(args);
	}


	private enum Tool {

		DAT_FILE_VIEWER(DatFileViewer::main),
		MOVABLE_MODEL_WINDOW(MovableModelWindow::main),
		BUILDING_CREATOR(BuildingCreatorApp::main),
		;


		public final MainFunc mainFunc;

		private Tool(MainFunc mainFunc) {
			this.mainFunc = mainFunc;
		}
	};

	private interface MainFunc {
		public void run(String[] args) throws Exception;
	}
}
