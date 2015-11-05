package be.monfils.x10clone.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 5/11/15.
 */

@Serializable
public class MessageSpawnPlayer extends AbstractMessage {
	private int id;
	private Vector3f position;
	private boolean isRemoval;

	public MessageSpawnPlayer() {
		super(true);
	}

	public MessageSpawnPlayer(int id, Vector3f position, boolean isRemoval) {
		super(true);

		this.id = id;
		this.position = position;
		this.isRemoval = isRemoval;
	}

	public int getId() {
		return id;
	}

	public Vector3f getPosition() {
		return position;
	}

	public boolean isRemoval() {
		return isRemoval;
	}
}
