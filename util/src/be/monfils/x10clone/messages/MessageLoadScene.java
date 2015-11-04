package be.monfils.x10clone.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 3/11/15.
 */

@Serializable
public class MessageLoadScene extends AbstractMessage {

	private String scene_json;

	public MessageLoadScene() {
		super(true);
	}

	public MessageLoadScene(String scene_json) {
		super(true);
		this.scene_json = scene_json;
	}

	public String getScene_json() {
		return scene_json;
	}
}
