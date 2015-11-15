package be.monfils.x10clone.messages;

import com.jme3.math.Vector3f;
import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 15/11/15.
 */

@Serializable
public class MessageShootBullet extends AbstractMessage {

	private Vector3f direction;

	public MessageShootBullet() {
		super(true);
	}

	public MessageShootBullet(Vector3f direction) {
		super(true);

		this.direction = direction;
	}

	public Vector3f getDirection() {
		return direction;
	}
}
