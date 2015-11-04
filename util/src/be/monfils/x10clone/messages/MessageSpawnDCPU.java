package be.monfils.x10clone.messages;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 4/11/15.
 */

@Serializable
public class MessageSpawnDCPU extends AbstractMessage {

	private Vector3f position;
	private Quaternion rotation;
	private float scale;
	private int id;

	public MessageSpawnDCPU() {
		super(true);

		this.position = new Vector3f(0, 0, 0);
		this.rotation = new Quaternion();
		id = 0;
	}

	public MessageSpawnDCPU(Vector3f position, Quaternion rotation, float scale, int id) {
		super(true);

		this.position = position;
		this.rotation = rotation;
		this.scale = scale;
		this.id = id;
	}

	public Vector3f getPosition() {
		return position;
	}

	public Quaternion getRotation() {
		return rotation;
	}

	public float getScale() {
		return scale;
	}

	public int getId() {
		return id;
	}
}
