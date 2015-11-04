package be.monfils.x10clone.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.Message;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 4/11/15.
 */

@Serializable
public class MessageDCPUKeyCode extends AbstractMessage {
	private boolean isChar;
	private int key;
	private int id;

	public MessageDCPUKeyCode() {
		super(true);

		isChar = false;
		key = 0;
		id = 0;
	}

	public MessageDCPUKeyCode(int id, boolean isChar, int key) {
		super(true);

		this.id = id;
		this.isChar = isChar;
		this.key = key;
	}

	public boolean isChar() {
		return isChar;
	}

	public int getKey() {
		return key;
	}

	public int getId() {
		return id;
	}
}
