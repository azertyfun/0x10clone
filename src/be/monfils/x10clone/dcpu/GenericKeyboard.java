package be.monfils.x10clone.dcpu;

/**
 * Created by nathan on 22/07/15.
 */
public class GenericKeyboard extends DCPUHardware {

	public static final int TYPE = 0x30cf7406, REVISION = 1, MANUFACTURER = 0;

	protected GenericKeyboard(String id) {
		super(TYPE, REVISION, MANUFACTURER);
		this.id = id;
	}
}
