package be.monfils.x10clone.dcpu;

/**
 * Created by nathan on 22/07/15.
 */
public class HardwareTracker {
	private int nDCPUs, nLems, nClocks, nKeyboards;

	public HardwareTracker() {

	}

	public DCPU requestDCPU() {
		return new DCPU("dcpu_" + (++nDCPUs));
	}

	public LEM1802 requestLem() {
		return new LEM1802("lem1802_" + (++nLems));
	}

	public GenericClock requestClock() {
		return new GenericClock("clock_" + (++nClocks));
	}

	public GenericKeyboard requestKeyboard() {
		return new GenericKeyboard("keyboard_" + (++nKeyboards));
	}

}
