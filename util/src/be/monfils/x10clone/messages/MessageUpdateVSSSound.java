package be.monfils.x10clone.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 4/11/15.
 */

@Serializable
public class MessageUpdateVSSSound extends AbstractMessage {
	float pitch[][];
	float volume[][];
	private int id;

	public MessageUpdateVSSSound() {
		super(true);

		id = 0;
		pitch = new float[2][4];
		volume = new float[2][4];
	}

	public MessageUpdateVSSSound(int id, float[][] pitch, float[][] volume) {
		super(true);

		this.id = id;
		this.pitch = pitch;
		this.volume = volume;
	}

	public float[][] getPitch() {
		return pitch;
	}

	public float[][] getVolume() {
		return volume;
	}

	public int getId() {
		return id;
	}
}
