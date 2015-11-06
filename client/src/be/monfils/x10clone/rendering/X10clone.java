package be.monfils.x10clone.rendering;

import be.monfils.x10clone.SceneDescriptor;
import be.monfils.x10clone.constants.Constants;
import be.monfils.x10clone.dcpu.DCPUModel;
import be.monfils.x10clone.messages.*;
import be.monfils.x10clone.networking.ClientListener;
import com.jme3.app.SimpleApplication;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Network;
import com.jme3.network.serializing.Serializer;
import com.jme3.renderer.RenderManager;
import com.jme3.system.AppSettings;

import java.io.IOException;
import java.net.ConnectException;
import java.util.LinkedList;
import java.util.concurrent.Callable;

/**
 * Created by nathan on 18/07/15.
 */
public class X10clone extends SimpleApplication implements ClientStateListener {

	private Client myClient;

	private boolean connected;
	private String username;

	private Scene scene;

	public static void main(String args[]) {
		AppSettings settings = new AppSettings(true);
		settings.setTitle("0x10clone");
		settings.setVSync(true);
		settings.setWidth(1024);
		settings.setHeight(768);
		X10clone app = new X10clone();
		app.setShowSettings(false);
		app.setSettings(settings);
		app.start();
	}

	@Override
	public void simpleInitApp() {
		setPauseOnLostFocus(false);

		cam.setFrustumPerspective(90, (float) settings.getWidth() / (float) settings.getHeight(), 0.05f, 10000f);

		initKeys();

		try {
			System.out.print("Connecting to the server... ");
			try {
				myClient = Network.connectToServer(Constants.GAME_NAME, Constants.GAME_VERSION, "localhost", 47810, 47810);
			} catch(ConnectException e) {
				System.err.println("Could not connect : " + e.getLocalizedMessage());
				stop();
				destroy();
				System.exit(-1);
			}

			myClient.addClientStateListener(this);

			Serializer.registerClass(MessageChangeUsername.class);
			Serializer.registerClass(MessageLoadScene.class);
			Serializer.registerClass(MessagePlayerLocation.class);
			Serializer.registerClass(MessageSpawnDCPU.class);
			Serializer.registerClass(MessageDCPUScreen.class);
			Serializer.registerClass(MessageUpdateVSSSound.class);
			Serializer.registerClass(MessageDCPUKeyCode.class);
			Serializer.registerClass(MessageResetDCPU.class);
			Serializer.registerClass(MessageSpawnPlayer.class);

			myClient.addMessageListener(new ClientListener(this), MessageChangeUsername.class);
			myClient.addMessageListener(new ClientListener(this), MessageLoadScene.class);
			myClient.addMessageListener(new ClientListener(this), MessagePlayerLocation.class);
			myClient.addMessageListener(new ClientListener(this), MessageSpawnDCPU.class);
			myClient.addMessageListener(new ClientListener(this), MessageDCPUScreen.class);
			myClient.addMessageListener(new ClientListener(this), MessageUpdateVSSSound.class);
			myClient.addMessageListener(new ClientListener(this), MessageSpawnPlayer.class);

			System.out.print("Connected!\nStarting client... ");

			myClient.start();

			MessageChangeUsername messageChangeUsername = new MessageChangeUsername("MyPseudo", false);
			myClient.send(messageChangeUsername);

			System.out.println("Client started!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadScene(SceneDescriptor sceneDescriptor) {
		this.enqueue(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				scene = new Scene(rootNode, guiNode, assetManager, inputManager, settings, sceneDescriptor, stateManager, listener, myClient, cam, flyCam, mouseInput);
				scene.load();

				return null;
			}
		});
	}

	public void addDCPU(Vector3f position, Quaternion rotation, float scale, int id) {
		scene.addDCPU(position, rotation, scale, id);
	}

	private void initKeys() {
		inputManager.addMapping("Forwards", new KeyTrigger(KeyInput.KEY_W));
		inputManager.addMapping("Backwards", new KeyTrigger(KeyInput.KEY_S));
		inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
		inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
		inputManager.addMapping("ToggleFlyCam", new KeyTrigger(KeyInput.KEY_RETURN), new KeyTrigger(KeyInput.KEY_NUMPADENTER));
		inputManager.addMapping("focusDCPU", new KeyTrigger(KeyInput.KEY_TAB));
		inputManager.addMapping("resetDCPU", new KeyTrigger(KeyInput.KEY_END));

		inputManager.addListener(actionListener, "Forwards", "Backwards", "Left", "Right");
		inputManager.addListener(actionListener, "ToggleFlyCam", "focusDCPU", "resetDCPU", "Jump");
		flyCam.unregisterInput();
	}

	private ActionListener actionListener = new ActionListener() {
		@Override
		public void onAction(String name, boolean pressed, float tpf) {
			if(name.equals("focusDCPU"))
				scene.input(Scene.Controls.FOCUS_DCPU, pressed, tpf);
			else if(name.equals("resetDCPU"))
				scene.input(Scene.Controls.RESET_DCPU, pressed, tpf);
			else if(name.equals("ToggleFlyCam"))
				scene.input(Scene.Controls.TOGGLE_FLY_CAM, pressed, tpf);
			else if(name.equals("Jump"))
				scene.input(Scene.Controls.JUMP, pressed, tpf);
			else if(name.equals("Forwards"))
				scene.input(Scene.Controls.FORWARDS, pressed, tpf);
			else if(name.equals("Backwards"))
				scene.input(Scene.Controls.BACKWARDS, pressed, tpf);
			else if(name.equals("Left"))
				scene.input(Scene.Controls.LEFT, pressed, tpf);
			else if(name.equals("Right"))
				scene.input(Scene.Controls.RIGHT, pressed, tpf);
		}
	};

	@Override
	public void simpleUpdate(float tpf) {
		if(scene != null && scene.isLoaded())
			scene.update(tpf);
	}

	@Override
	public void simpleRender(RenderManager rm) {
		super.simpleRender(rm);
		if(scene != null && scene.isLoaded())
			scene.render();
	}

	@Override
	public void stop() {
		super.stop();
	}

	@Override
	public void destroy() {
		if(myClient != null && myClient.isConnected())
			myClient.close();
		super.destroy();
	}

	public void setUsername(String username) {
		System.out.println("Set username to " + username);
		this.username = username;
	}

	@Override
	public void clientConnected(Client client) {
		System.out.println("Connected to the server!");
	}

	@Override
	public void clientDisconnected(Client client, DisconnectInfo disconnectInfo) {
		System.out.println("Disconnected from the server (reason: " + ((disconnectInfo != null && disconnectInfo.reason != null) ? disconnectInfo.reason : "none given") + ").");
		stop();
		System.exit(0);
	}

	public void setPlayerPosition(int id, Vector3f position, Quaternion rotation) {
		enqueue(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				scene.setPlayerPosition(id, position, rotation);
				return null;
			}
		});
	}

	public boolean isLoadingScene() {
		return scene == null || !scene.isLoaded();
	}

	public LinkedList<DCPUModel> getDcpuModels() {
		return scene.getDcpuModels();
	}

	public void addPlayer(int id, Vector3f position) {
		scene.addPlayer(id, position);
	}

	public void removePlayer(int id) {
		scene.removePlayer(id);
	}
}
