package be.monfils.x10cloneserver;

import be.monfils.X10clone.constants.Constants;
import be.monfils.x10clone.messages.MessageChangeUsername;
import com.jme3.app.SimpleApplication;
import com.jme3.network.*;
import com.jme3.network.serializing.Serializer;
import com.jme3.system.JmeContext;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by nathan on 3/11/15.
 */
public class X10cloneServer extends SimpleApplication implements ConnectionListener {
	private Server myServer;
	private Thread scannerThread;

	public static void main(String args[]) {
		X10cloneServer app = new X10cloneServer();
		app.start(JmeContext.Type.Headless);
	}

	@Override
	public void simpleInitApp() {
		try {
			myServer = Network.createServer(Constants.GAME_NAME, Constants.GAME_VERSION, 47810, 47810);
			System.out.println("Running " + myServer.getGameName() + " server version #" + myServer.getVersion());
			myServer.addMessageListener(new ServerListener(this), MessageChangeUsername.class);
			myServer.addConnectionListener(this);

			Serializer.registerClass(MessageChangeUsername.class);

			myServer.start();

			scannerThread = new ScannerThread();
			scannerThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void connectionAdded(Server server, HostedConnection hostedConnection) {
		System.out.println("Connection from " + hostedConnection.getAddress() + " (id " + hostedConnection.getId() + ").");
		hostedConnection.setAttribute("ip_address", hostedConnection.getAddress());

		//Set unique username
		if(!setUsername(hostedConnection, "player_" + hostedConnection.getId())) {
			do {

			} while(!setUsername(hostedConnection, "player_" + ThreadLocalRandom.current().nextInt(10000, 100000)));
		}
	}

	@Override
	public void connectionRemoved(Server server, HostedConnection hostedConnection) {
		System.out.println("Disconnection from " + hostedConnection.getAttribute("ip_address") + " (id " + hostedConnection.getId() + ").");
	}

	public boolean setUsername(HostedConnection hostedConnection, String username) {
		for(HostedConnection con : myServer.getConnections()) {
			if(con.getAttribute("username") != null && ((String) (con.getAttribute("username"))).equalsIgnoreCase(username)) {
				hostedConnection.send(new MessageChangeUsername(username, false));
				return false;
			}
		}

		hostedConnection.setAttribute("username", username);
		hostedConnection.send(new MessageChangeUsername(username, true));
		return true;
	}

	public void saveAndExit() {
		stop();
		destroy();
		System.exit(0);
	}

	@Override
	public void destroy() {
		myServer.close();
		super.destroy();
	}

	private class ScannerThread extends Thread {

		public void run() {
			while(true) {
				Scanner sc = new Scanner(System.in);
				String s = sc.nextLine();
				String command[] = s.split(" ");
				if(command[0].equalsIgnoreCase("stop")) {
					saveAndExit();
				} else if (command[0].equalsIgnoreCase("list")) {
					System.out.println("Currently connected players:");
					for (HostedConnection conn : myServer.getConnections()) {
						System.out.print(conn.getAttribute("username") + "; ");
					}
					System.out.println();
				} else if(command[0].equalsIgnoreCase("kick")) {
					if(command.length == 2 && command[1] != "") {
						boolean playerFound = false;
						for (HostedConnection conn : myServer.getConnections()) {
							if(conn.getAttribute("username") != null && ((String) conn.getAttribute("username")).equalsIgnoreCase(command[1]))
								conn.close("Kicked from the server");
								playerFound = true;
								break;
						}

						if(!playerFound)
							System.out.println("Player '" + command[1] + "' not found.");
						else
							System.out.println("Player '" + command[1] + "' kicked!");
					} else
						System.out.println("Command usage: kick [player]");
				} else {
					System.err.println("Unknown command " + command[0]);
				}
			}
		}
	}
}
