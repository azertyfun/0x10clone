package be.monfils.x10clone.networking;

import be.monfils.x10clone.messages.MessageChangeUsername;
import be.monfils.x10clone.rendering.X10clone;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;

/**
 * Created by nathan on 3/11/15.
 */
public class ClientListener implements MessageListener<Client> {

	private final X10clone client;

	public ClientListener(X10clone client) {
		this.client = client;
	}

	@Override
	public void messageReceived(Client client, Message message) {
		if(message instanceof MessageChangeUsername) {
			MessageChangeUsername messageChangeUsername = (MessageChangeUsername) message;
			if(messageChangeUsername.isAccepted()) {
				this.client.setUsername(messageChangeUsername.getUsername());
			} else {
				System.out.println("Username change denied. Maybe there is someone else using this username?");
			}
		}
	}
}
