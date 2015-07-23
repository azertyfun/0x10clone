package be.monfils.x10clone.dcpu;

import be.monfils.x10clone.rendering.X10clone;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.export.Savable;
import com.jme3.input.KeyInput;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nathan on 22/07/15.
 */
public class GenericKeyboard extends DCPUHardware {

	public static final int TYPE = 0x30cf7406, REVISION = 1, MANUFACTURER = 0;

	protected char[] buffer = new char[64];
	protected int buffer_pointer = -1, interruptMessage = 0;

	protected GenericKeyboard(String id) {
		super(TYPE, REVISION, MANUFACTURER);
		this.id = id;
	}

	public void interrupt() {
		int a = dcpu.registers[0];
		switch(a) {
			case 0:
				buffer_pointer = -1;
				for(int i = 0; i < buffer.length; ++i) {
					buffer[i] = 0;
				}
				break;
			case 1:
				if(buffer_pointer == -1)
					dcpu.registers[2] = 0;
				else {
					dcpu.registers[2] = buffer[buffer_pointer & 0x3F];
					buffer[buffer_pointer-- & 0x3F] = 0;
				}
				break;
			case 2:
				dcpu.registers[2] = 0; //TODO
				break;
			case 3:
				interruptMessage = dcpu.registers[1];
				break;
		}
	}

	public void pressedKey(char keyChar) {
		buffer[++buffer_pointer] = keyChar;
	}
	public void pressedKeyCode(int keyCode) {
		switch (keyCode) {
			case KeyInput.KEY_BACK:
				buffer[++buffer_pointer] = 0x10;
				break;
			case KeyInput.KEY_RETURN:
			case KeyInput.KEY_NUMPADENTER:
				buffer[++buffer_pointer] = 0x11;
				break;
			case KeyInput.KEY_INSERT:
				buffer[++buffer_pointer] = 0x12;
				break;
			case KeyInput.KEY_DELETE:
				buffer[++buffer_pointer] = 0x13;
				break;
			case KeyInput.KEY_UP:
				buffer[++buffer_pointer] = 0x80;
				break;
			case KeyInput.KEY_DOWN:
				buffer[++buffer_pointer] = 0x81;
				break;
			case KeyInput.KEY_LEFT:
				buffer[++buffer_pointer] = 0x82;
				break;
			case KeyInput.KEY_RIGHT:
				buffer[++buffer_pointer] = 0x83;
				break;
			case KeyInput.KEY_LSHIFT:
			case KeyInput.KEY_RSHIFT:
				buffer[++buffer_pointer] = 0x90;
				break;
			case KeyInput.KEY_LCONTROL:
			case KeyInput.KEY_RCONTROL:
				buffer[++buffer_pointer] = 0x91;
		}
	}
}
