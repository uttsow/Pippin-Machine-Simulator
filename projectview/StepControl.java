package projectview;

import javax.swing.Timer;

public class StepControl {
	private static final int TICK = 500;
	private boolean autoStepOn = false;
	private Timer timer;
	private Pippin_Run mediator;
	
	public StepControl(Pippin_Run aMediator) {
		mediator = aMediator;
	}
	boolean isAutoStepOn() {
		return autoStepOn;
	}
	void setAutoStepOn(boolean arg) {
		autoStepOn = arg;
	}
	void toggleAutoStep() {
		autoStepOn = !autoStepOn;
	}
	void setPeriod(int period) {
		timer.setDelay(period);
	}
	void start() {
		timer = new Timer(TICK, e -> {if(autoStepOn) mediator.step();});
		timer.start();
	}
	
}
