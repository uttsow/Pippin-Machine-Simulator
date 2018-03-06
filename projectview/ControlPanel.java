package projectview;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class ControlPanel implements Observer {
	private Pippin_Run mediator;
	private JButton stepButton = new JButton("Step");
	private JButton clearButton = new JButton("Clear");
	private JButton runButton = new JButton("Run/Pause");
	private JButton reloadButton = new JButton("Reload");
	
	public ControlPanel(Pippin_Run mediator) {
		this.mediator = mediator;
		mediator.addObserver((Observer) this);
	}
	public JComponent createControlDisplay() {
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 0));
		
		stepButton.setBackground(Color.WHITE);
		stepButton.addActionListener(e -> mediator.step());
		panel.add(stepButton);
		
		clearButton.setBackground(Color.WHITE);
		clearButton.addActionListener(e -> mediator.clear());
		panel.add(clearButton);
		
		runButton.setBackground(Color.WHITE);
		runButton.addActionListener(e -> mediator.toggleAutoStep());
		panel.add(runButton);
		
		reloadButton.setBackground(Color.WHITE);
		reloadButton.addActionListener(e -> mediator.reload());
		panel.add(reloadButton);
		
		JSlider slider = new JSlider(5,1000);
		slider.addChangeListener(e -> mediator.setPeriod(slider.getValue()));
		panel.add(slider);
		return panel;
	}
	
	@Override
	public void update(Observable arg0, Object arg1) {
		runButton.setEnabled(mediator.getCurrentState().getRunPauseActive());
		stepButton.setEnabled(mediator.getCurrentState().getStepActive());
		clearButton.setEnabled(mediator.getCurrentState().getClearActive());
		reloadButton.setEnabled(mediator.getCurrentState().getReloadActive());		
	}
}
