package be.monfils.x10clone.dcpu;

/**
 * Created by nathan on 25/07/15.
 */
public class VSS1224 extends DCPUHardware {

	public static final int TYPE=0x02060001, REVISION=0x1224, MANUFACTURER=0x1c6c8b36;

	protected int frequencies[] = new int[] {0, 0};

	public VSS1224(String id) {
		super(TYPE, REVISION, MANUFACTURER);
		this.id = id;
	}

	@Override
	public void interrupt() {
		int a = dcpu.registers[0];
		int b = dcpu.registers[1];
		switch(a) {
			case 0:
				if(b < 50 && b != 0)
					b = 50;
				else if(b > 1600)
					b = 1600;
				frequencies[0] = b;
				break;
			case 1:
				if(b < 50 && b != 0)
					b = 50;
				else if(b > 1600)
					b = 1600;
				frequencies[1] = b;
				break;
		}
	}

	public int[] getFrequencies() {
		return frequencies;
	}

	@Override
	public void tick60hz() {

	}

	@Override
	public void powerOff() {
		frequencies[0] = 0;
		frequencies[1] = 0;
	}

	@Override
	public void powerOn() {
		frequencies[0] = 0;
		frequencies[1] = 0;
	}
}
