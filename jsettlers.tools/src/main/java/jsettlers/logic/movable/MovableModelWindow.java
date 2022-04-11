package jsettlers.logic.movable;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import jsettlers.TestToolUtils;
import jsettlers.common.CommonConstants;
import jsettlers.common.menu.IMapInterfaceConnector;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.graphics.map.draw.settlerimages.SettlerImageMap;
import jsettlers.logic.movable.modelmap.FakeMovable;
import jsettlers.logic.movable.modelmap.MovableController;
import jsettlers.logic.movable.modelmap.MovableModelMap;
import jsettlers.logic.movable.modelmap.SimpleStoppableClock;
import jsettlers.main.swing.lookandfeel.JSettlersLookAndFeelExecption;

public class MovableModelWindow {

	public static final short TILE_SIZE = 32;

	private final MovableModelMap map;
	private final IMapInterfaceConnector inputHandler;

	private final SimpleStoppableClock clock = new SimpleStoppableClock();

	private final List<MovableController> controllers = new ArrayList<>();
	private JPanel controllerPanel;
	private JFrame controlFrame;

	private MovableModelWindow() throws JSettlersLookAndFeelExecption, IOException {
		map = new MovableModelMap(TILE_SIZE, TILE_SIZE);

		inputHandler = TestToolUtils.openTestWindow(map);
		inputHandler.addListener(action -> {
			switch(action.getActionType()) {
				case SPEED_TOGGLE_PAUSE:
					clock.toggleStopped();
					break;
				case SPEED_SET_PAUSE:
					clock.setStopped(true);
					break;
				case SPEED_UNSET_PAUSE:
					clock.setStopped(false);
					break;
				case SPEED_FASTER:
					clock.mulSpeed(1.2f);
					break;
				case SPEED_SLOWER:
					clock.mulSpeed(1/1.2f);
					break;
			}
		});

		SwingUtilities.invokeLater(this::createControlFrame);
	}

	private void createControlFrame() {
		controlFrame = new JFrame(getClass().getSimpleName());
		controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);


		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());

		JButton addButton = new JButton("Add settlers");
		addButton.setMinimumSize(addButton.getPreferredSize());
		addButton.addActionListener(e -> addController());

		JButton reloadButton = new JButton("reload movables");
		reloadButton.setMinimumSize(reloadButton.getPreferredSize());
		reloadButton.addActionListener(e -> SettlerImageMap.reloadFromDirectory());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 2));
		buttonPanel.add(addButton);
		buttonPanel.add(reloadButton);

		mainPanel.add(buttonPanel, BorderLayout.PAGE_START);

		JPanel wrapPanel = new JPanel();
		controllerPanel = new JPanel();
		controllerPanel.setLayout(new GridBagLayout());
		wrapPanel.add(controllerPanel);
		JScrollPane mainScrollPanel = new JScrollPane(wrapPanel);
		mainScrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		mainPanel.add(mainScrollPanel, BorderLayout.CENTER);

		controlFrame.add(mainPanel);

		controlFrame.setVisible(true);
		controlFrame.setSize(500, 500);
	}

	public FakeMovable createSettler(ShortPoint2D position) {
		return new FakeMovable(clock::getCurrentTime, map, position);
	}

	public void addController() {
		controllers.add(new MovableController(this));

		updateExternal(controllers.size()-1);
	}

	public void duplicateController(int index) {
		controllers.add(index+1, controllers.get(index).clone());

		updateExternal(index);
	}

	private void updateExternal(int start) {
		int size = controllers.size();

		for(int i = start; i < size; i++) {
			MovableController controller = controllers.get(i);

			controllerPanel.remove(controller);
			controller.clearIndex();
		}

		for(int i = start; i < size; i++) {
			MovableController controller = controllers.get(i);
			controllerPanel.add(controller, createGBC(i));
			controller.setIndex(i);
		}

		redraw();
	}

	public void removeController(int index) {
		MovableController old = controllers.remove(index);
		controllerPanel.remove(old);
		old.clearIndex();

		updateExternal(index);
	}

	public void moveUp(int controller) {
		if(controller == 0) return;

		swap(controller, controller-1);
	}

	public void moveDown(int controller) {
		if(controller == controllers.size()-1) return;

		swap(controller, controller+1);
	}

	private void swap(int a, int b) {
		MovableController aHandle = controllers.get(a);
		MovableController bHandle = controllers.get(b);

		controllers.set(b, aHandle);
		controllers.set(a, bHandle);

		controllerPanel.remove(aHandle);
		controllerPanel.remove(bHandle);

		aHandle.clearIndex();
		bHandle.clearIndex();

		controllerPanel.add(aHandle, createGBC(b));
		controllerPanel.add(bHandle, createGBC(a));

		aHandle.setIndex(b);
		bHandle.setIndex(a);

		redraw();
	}

	private void redraw() {
		controlFrame.validate();
	}


	private GridBagConstraints createGBC(int index) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = index;

		gbc.gridwidth = 1;
		gbc.gridheight = 1;

		gbc.fill = GridBagConstraints.HORIZONTAL;

		return gbc;
	}

	public static void main(String[] args) throws JSettlersLookAndFeelExecption, IOException {
		new MovableModelWindow();
		CommonConstants.MUTABLE_MOVABLES_TXT = true;
		SettlerImageMap.reloadFromDirectory();
	}
}
