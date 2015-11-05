package be.monfils.x10cloneserver;

import be.monfils.x10clone.dcpu.DCPUModel;
import be.monfils.x10clone.messages.*;
import com.jme3.network.HostedConnection;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;
import com.jme3.scene.Node;

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
		} else if(message instanceof MessagePlayerLocation) {
			MessagePlayerLocation messagePlayerLocation = (MessagePlayerLocation) message;

			for(HostedConnection hostedConnection : server.getClients()) {
				if(hostedConnection.getId() == source.getId()) {
					Object node_ = hostedConnection.getAttribute("node");
					if(node_ != null) {
						Node node = (Node) node_;
						node.setLocalTranslation(messagePlayerLocation.getPosition());
						node.setLocalRotation(messagePlayerLocation.getRotation());
					}
					break;
				}
			}
		} else if(message instanceof MessageDCPUKeyCode) {
			MessageDCPUKeyCode messageDCPUKeyCode = (MessageDCPUKeyCode) message;

			for(DCPUModel model : server.getDcpuModels()) {
				if(model.getId() == messageDCPUKeyCode.getId()) {
					if(messageDCPUKeyCode.isChar())
						model.getKeyboard().pressedKey((char) messageDCPUKeyCode.getKey());
					else
						model.getKeyboard().pressedKeyCode(messageDCPUKeyCode.getKey());
				}
			}
		} else if(message instanceof MessageResetDCPU) {
			MessageResetDCPU messageResetDCPU = (MessageResetDCPU) message;

			for(DCPUModel model : server.getDcpuModels()) {
				if(model.getId() == messageResetDCPU.getId()) {
					model.getDcpu().reset();
				}
			}
		}
	}
}
