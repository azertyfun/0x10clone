package be.monfils.x10clone.dcpu;

/**
 * Created by nathan on 21/07/15.
 */
public abstract class DCPUHardware implements Identifiable {

	private final int type, revision, manufacturer;
	public DCPU dcpu;
	protected String id;

	protected DCPUHardware(int type, int revision, int manufacturer) {
		this.type = type;
		this.revision = revision;
		this.manufacturer = manufacturer;
	}

	public DCPUHardware connectTo(DCPU dcpu) {
		this.dcpu = dcpu;
		dcpu.addHardware(this);
		return this;
	}

	public DCPUHardware disconnect() {
		dcpu.removeHardware(this);
		dcpu = null;
		return this;
	}

	public void query() {
		this.dcpu.registers[0] = (char)(this.type & 0xFFFF);
		this.dcpu.registers[1] = (char)(this.type >> 16 & 0xFFFF);
		this.dcpu.registers[2] = (char)(this.revision & 0xFFFF);
		this.dcpu.registers[3] = (char)(this.manufacturer & 0xFFFF);
		this.dcpu.registers[4] = (char)(this.manufacturer >> 16 & 0xFFFF);
	}

	public void interrupt() {}
	public void tick60hz() {}
	public void powerOff() {}
	public void powerOn() {}
	public void onDestroy() {}

	public boolean isConnected() {
		return dcpu != null;
	}

	@Override
	public String getID() {
		return id;
	}

	@Override
	public void setID(String id) {
		this.id = id;
	}
}
