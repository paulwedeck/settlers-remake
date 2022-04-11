package jsettlers.logic.movable.modelmap;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.movable.EMovableAction;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.player.ECivilisation;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.movable.MovableModelWindow;

public class MovableController extends JPanel implements Cloneable {

	private final MovableModelWindow parent;
	private int index;

	private final List<FakeMovable> movables;

	private final JComboBox<EMovableType> typeBox;
	private final JComboBox<EMovableAction> actionBox;
	private final JComboBox<EMovingDirection> directionBox;
	private final JComboBox<EMaterialType> materialBox;

	public MovableController(MovableModelWindow parent) {
		this.parent = parent;
		setLayout(new GridLayout(2, 1));
		setBorder(new LineBorder(Color.WHITE));

		movables  = new ArrayList<>();

		for(ECivilisation civilisation : ECivilisation.VALUES) {
			FakeMovable newMovable = parent.createSettler(null);
			newMovable.getPlayer().setCivilisation(civilisation);
			movables.add(newMovable);
		}

		clearIndex();

		JPanel topRow = new JPanel();
		topRow.setLayout(new BoxLayout(topRow, BoxLayout.X_AXIS));

		typeBox = new JComboBox<>(EMovableType.VALUES);
		typeBox.addActionListener(e -> updateSettlers());
		topRow.add(typeBox);

		actionBox = new JComboBox<>(EMovableAction.values());
		actionBox.addActionListener(e -> updateSettlers());
		topRow.add(actionBox);

		directionBox = new JComboBox<>(EMovingDirection.values());
		directionBox.addActionListener(e -> updateSettlers());
		topRow.add(directionBox);

		JPanel bottomRow = new JPanel();
		bottomRow.setLayout(new BoxLayout(bottomRow, BoxLayout.X_AXIS));

		materialBox = new JComboBox<>(EMaterialType.VALUES);
		materialBox.setSelectedItem(EMaterialType.NO_MATERIAL);
		materialBox.addActionListener(e -> updateSettlers());
		bottomRow.add(materialBox);

		JButton up = new JButton("up");
		up.addActionListener(e -> parent.moveUp(this.index));
		up.setMaximumSize(up.getPreferredSize());

		JButton duplicate = new JButton("duplicate");
		duplicate.addActionListener(e -> parent.duplicateController(this.index));
		duplicate.setMinimumSize(duplicate.getPreferredSize());

		JButton remove = new JButton("remove");
		remove.addActionListener(e -> parent.removeController(this.index));
		remove.setMaximumSize(remove.getPreferredSize());

		JButton down = new JButton("down");
		down.addActionListener(e -> parent.moveDown(this.index));
		down.setMaximumSize(down.getPreferredSize());

		bottomRow.add(up);
		bottomRow.add(duplicate);
		bottomRow.add(remove);
		bottomRow.add(down);

		add(topRow);
		add(bottomRow);

		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());

		updateSettlers();
	}

	private void updateSettlers() {
		for(FakeMovable movable : movables) {
			movable.setMovableAction((EMovableAction) actionBox.getSelectedItem());
			movable.setMovableType((EMovableType) typeBox.getSelectedItem());
			movable.setDirection((EMovingDirection) directionBox.getSelectedItem());
			movable.setMaterialType((EMaterialType) materialBox.getSelectedItem());
		}
	}

	public void clearIndex() {
		index = -1;

		for(FakeMovable movable : movables) {
			movable.setPosition(null);
		}
	}

	public void setIndex(int index) {
		this.index = index;

		for(FakeMovable movable : movables) {
			movable.setPosition(new ShortPoint2D(index*3 + 2, movable.getPlayer().getCivilisation().ordinal*5+3));
		}
	}

	@Override
	public MovableController clone() {
		MovableController clone = new MovableController(parent);
		clone.typeBox.setSelectedIndex(typeBox.getSelectedIndex());
		clone.actionBox.setSelectedIndex(actionBox.getSelectedIndex());
		clone.directionBox.setSelectedIndex(directionBox.getSelectedIndex());
		clone.materialBox.setSelectedIndex(materialBox.getSelectedIndex());
		return clone;
	}
}
