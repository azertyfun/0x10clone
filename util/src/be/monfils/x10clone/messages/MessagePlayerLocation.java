package be.monfils.x10clone.messages;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 3/11/15.
 */

@Serializable
public class MessagePlayerLocation extends AbstractMessage {
	private int id;
	private Vector3f position;
	private Quaternion rotation;

	public MessagePlayerLocation() {
		super(true); //Server -> client must be reliable, but the client should set it to false
		position = new Vector3f();
		rotation = new Quaternion();
	}

	public MessagePlayerLocation(int id, Vector3f position, Quaternion rotation) {
		super(true); //Server -> client must be reliable, but the client should set it to false
		this.id = id;
		this.position = position;
		this.rotation = rotation;
	}

	public int getId() {
		return id;
	}

	public Vector3f getPosition() {
		return position;
	}

	public Quaternion getRotation() {
		return rotation;
	}
}
