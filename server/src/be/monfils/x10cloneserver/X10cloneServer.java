package be.monfils.x10cloneserver;

import be.monfils.x10clone.Scene;
import be.monfils.x10clone.constants.Constants;
import be.monfils.x10clone.dcpu.DCPUModel;
import be.monfils.x10clone.dcpu.HardwareTracker;
import be.monfils.x10clone.messages.*;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.ConnectionListener;
import com.jme3.network.HostedConnection;
import com.jme3.network.Network;
import com.jme3.network.Server;
import com.jme3.network.serializing.Serializer;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.JmeContext;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by nathan on 3/11/15.
 */
public class X10cloneServer extends SimpleApplication implements ConnectionListener {
	private Server myServer;
	private Thread scannerThread;

	public static HardwareTracker hardwareTracker = new HardwareTracker();
	private LinkedList<DCPUModel> dcpuModels = new LinkedList<>();
	private DCPUTickingThread dcpuTickingThread;

	private Spatial sceneModel;
	private Node dcpuScreens;

	private BulletAppState bulletAppState;
	private RigidBodyControl sceneBody;

	private Scene scene;

	public static void main(String args[]) {
		X10cloneServer app = new X10cloneServer();
		app.start(JmeContext.Type.Headless);
	}

	@Override
	public void simpleInitApp() {
		try {
			myServer = Network.createServer(Constants.GAME_NAME, Constants.GAME_VERSION, 47810, 47810);
			System.out.println("Running " + myServer.getGameName() + " server version #" + myServer.getVersion());

			Serializer.registerClass(MessageChangeUsername.class);
			Serializer.registerClass(MessageLoadScene.class);
			Serializer.registerClass(MessagePlayerLocation.class);
			Serializer.registerClass(MessageSpawnDCPU.class);
			Serializer.registerClass(MessageDCPUScreen.class);
			Serializer.registerClass(MessageUpdateVSSSound.class);
			Serializer.registerClass(MessageDCPUKeyCode.class);
			Serializer.registerClass(MessageResetDCPU.class);
			Serializer.registerClass(MessageSpawnPlayer.class);

			myServer.addMessageListener(new ServerListener(this), MessageChangeUsername.class);
			myServer.addMessageListener(new ServerListener(this), MessagePlayerLocation.class);
			myServer.addMessageListener(new ServerListener(this), MessageDCPUKeyCode.class);
			myServer.addMessageListener(new ServerListener(this), MessageResetDCPU.class);

			myServer.addConnectionListener(this);

			myServer.start();

			scannerThread = new ScannerThread();
			scannerThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			scene = Scene.loadJSON("util/assets/Scenes/TestScene.json");

			bulletAppState = new BulletAppState();
			stateManager.attach(bulletAppState);
			sceneModel = assetManager.loadModel(scene.getScene_file());
			CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
			sceneBody = new RigidBodyControl(sceneShape, 0);
			sceneModel.addControl(sceneBody);

			rootNode.attachChild(sceneModel);
			bulletAppState.getPhysicsSpace().add(sceneBody);

			dcpuScreens = new Node();
			rootNode.attachChild(dcpuScreens);

			dcpuModels.add(new DCPUModel(0, myServer, hardwareTracker, bulletAppState, assetManager, rootNode, new Vector3f(2, 1, -7), new Quaternion(), 1.0f, "util/assets/DCPU/stillalive.bin"));
			dcpuScreens.attachChild(dcpuModels.getLast().getScreen());
			dcpuModels.add(new DCPUModel(1, myServer, hardwareTracker, bulletAppState, assetManager, rootNode, new Vector3f(-5, 0.1f, 5), new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y), 1.0f, "util/assets/DCPU/BOLD.bin"));
			dcpuScreens.attachChild(dcpuModels.getLast().getScreen());

			dcpuTickingThread = new DCPUTickingThread(this, dcpuModels, assetManager);
			dcpuTickingThread.start();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}

	@Override
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);

		for(HostedConnection hostedConnection : myServer.getConnections()) {
			for(HostedConnection otherHostedConnection : myServer.getConnections()) {
				if(hostedConnection.getId() != otherHostedConnection.getId() && otherHostedConnection.getAttribute("node") != null) {
					MessagePlayerLocation messagePlayerLocation = new MessagePlayerLocation(otherHostedConnection.getId(), ((Node) otherHostedConnection.getAttribute("node")).getLocalTranslation(), ((Node) otherHostedConnection.getAttribute("node")).getWorldRotation());
					messagePlayerLocation.setReliable(false);
					hostedConnection.send(messagePlayerLocation);
				}
			}
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

		BetterCharacterControl player = new BetterCharacterControl(0.5f, 1.8f, 1);
		player.setJumpForce(new Vector3f(0.0f, 5.0f, 0.0f));
		hostedConnection.setAttribute("player", player);

		Node playerNode = new Node("Player");
		playerNode.setLocalTranslation(scene.getPlayer_position());
		rootNode.attachChild(playerNode);
		playerNode.addControl(player);
		hostedConnection.setAttribute("node", playerNode);

		//Tell client which level to load
		hostedConnection.send(new MessageLoadScene("util/assets/Scenes/TestScene.json"));
		for(DCPUModel dcpuModel : dcpuModels) {
			hostedConnection.send(new MessageSpawnDCPU(dcpuModel.getPosition(), dcpuModel.getRotation(), dcpuModel.getScale(), dcpuModel.getId()));
		}

		for(HostedConnection otherPlayer : myServer.getConnections()) {
			if(otherPlayer.getId() != hostedConnection.getId() && otherPlayer.getAttribute("node") != null) {
				hostedConnection.send(new MessageSpawnPlayer(otherPlayer.getId(), ((Node) otherPlayer.getAttribute("node")).getLocalTranslation(), false));
				otherPlayer.send(new MessageSpawnPlayer(hostedConnection.getId(), ((Node) hostedConnection.getAttribute("node")).getLocalTranslation(), false));
			}
		}
	}

	@Override
	public void connectionRemoved(Server server, HostedConnection hostedConnection) {
		for(HostedConnection otherPlayer : myServer.getConnections()) {
			if(otherPlayer.getId() != hostedConnection.getId()) {
				otherPlayer.send(new MessageSpawnPlayer(hostedConnection.getId(), new Vector3f(), true));
			}
		}

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
		super.destroy();
	}

	public Collection<HostedConnection> getClients() {
		return myServer.getConnections();
	}

	public LinkedList<DCPUModel> getDcpuModels() {
		return dcpuModels;
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

	@Override
	public void stop() {
		super.stop();
		for(DCPUModel dcpuModel : dcpuModels)
			dcpuModel.stop();
	}
}
