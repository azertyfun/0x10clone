package be.monfils.x10clone.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 3/11/15.
 */

@Serializable
public class MessagePlayerLocation extends AbstractMessage {
	Vector3f position;

	public MessagePlayerLocation() {
		super(true); //Server -> client must be reliable, but the client should set it to false
		position = new Vector3f();
	}

	public MessagePlayerLocation(Vector3f position) {
		super(true); //Server -> client must be reliable, but the client should set it to false
		this.position = position;
	}

	public Vector3f getPosition() {
		return position;
	}
}
