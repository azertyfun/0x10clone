package be.monfils.x10clone.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.Message;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 4/11/15.
 */

@Serializable
public class MessageResetDCPU extends AbstractMessage {
	private int id;

	public MessageResetDCPU() {
		this.id = 0;
	}

	public MessageResetDCPU(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}
}
