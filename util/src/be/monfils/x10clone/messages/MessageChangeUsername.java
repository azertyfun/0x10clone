package be.monfils.x10clone.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 3/11/15.
 */

@Serializable
public class MessageChangeUsername extends AbstractMessage {
	private String username;
	private boolean accepted; //Sent by server to client

	public MessageChangeUsername() {

	}

	public MessageChangeUsername(String username, boolean accepted) {
		this.username = username;
		this.accepted = accepted;
	}

	public String getUsername() {
		return username;
	}

	public boolean isAccepted() {
		return accepted;
	}
}
