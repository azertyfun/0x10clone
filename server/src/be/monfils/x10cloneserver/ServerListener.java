package be.monfils.x10cloneserver;

import be.monfils.x10clone.messages.MessageChangeUsername;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;

/**
 * Created by nathan on 3/11/15.
 */
public class ServerListener implements MessageListener<HostedConnection> {

	private final X10cloneServer server;

	public ServerListener(X10cloneServer server) {
		this.server = server;
	}

	@Override
	public void messageReceived(HostedConnection source, Message message) {
		if(message instanceof MessageChangeUsername) {
			MessageChangeUsername messageChangeUsername = (MessageChangeUsername) message;
			System.out.print("Client requested username change to " + messageChangeUsername.getUsername() + "... ");
			if(server.setUsername(source, messageChangeUsername.getUsername())) {
				System.out.println("Accepted!");
			} else {
				System.out.println("Denied!");
				source.send(new MessageChangeUsername(messageChangeUsername.getUsername(), false));
			}
		}
	}
}
