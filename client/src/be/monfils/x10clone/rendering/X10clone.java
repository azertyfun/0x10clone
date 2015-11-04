package be.monfils.x10clone.rendering;

import be.monfils.x10clone.Scene;
import be.monfils.x10clone.constants.Constants;
import be.monfils.x10clone.dcpu.*;
import be.monfils.x10clone.messages.*;
import be.monfils.x10clone.networking.ClientListener;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.*;
import com.jme3.light.Light;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.network.ClientStateListener;
import com.jme3.network.Network;
import com.jme3.network.serializing.Serializer;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by nathan on 18/07/15.
 */
public class X10clone extends SimpleApplication implements ClientStateListener {

	private Client myClient;

	private Random random = new Random();

	public static HardwareTracker hardwareTracker = new HardwareTracker();
	private LinkedList<DCPUModel> dcpuModels = new LinkedList<>();

	private Spatial sceneModel;
	private BitmapText hello_text;
	private Node dcpuScreens;
	private boolean focusedOnDCPU;
	private Spatial focusedDCPU;
	private AudioNode footSteps[];

	private BulletAppState bulletAppState;
	private RigidBodyControl sceneBody;
	private BetterCharacterControl player;
	private Node playerNode;
	private Vector3f walkDirection = new Vector3f(), camDir = new Vector3f(), camLeft = new Vector3f();
	private boolean forwards, backwards, left, right;
	private int timeSinceLastStepSound = 0;

	private boolean connected;
	private String username;
	private boolean loadingScene = true;

	private ScheduledThreadPoolExecutor executor;

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
		cam.setFrustumPerspective(90, (float) settings.getWidth() / (float) settings.getHeight(), 0.05f, 10000f);

		initAudio();
		initKeys();

		player = new BetterCharacterControl(0.5f, 1.8f, 1);
		player.setJumpForce(new Vector3f(0.0f, 5.0f, 0.0f));

		playerNode = new Node("Player");
		playerNode.setLocalTranslation(0, 5, 0);
		rootNode.attachChild(playerNode);
		playerNode.addControl(player);

		executor = new ScheduledThreadPoolExecutor(4);

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

			myClient.addMessageListener(new ClientListener(this), MessageChangeUsername.class);
			myClient.addMessageListener(new ClientListener(this), MessageLoadScene.class);
			myClient.addMessageListener(new ClientListener(this), MessagePlayerLocation.class);
			myClient.addMessageListener(new ClientListener(this), MessageSpawnDCPU.class);
			myClient.addMessageListener(new ClientListener(this), MessageDCPUScreen.class);
			myClient.addMessageListener(new ClientListener(this), MessageUpdateVSSSound.class);

			System.out.print("Connected!\nStarting client... ");

			myClient.start();

			MessageChangeUsername messageChangeUsername = new MessageChangeUsername("MyPseudo", false);
			myClient.send(messageChangeUsername);

			System.out.println("Client started!");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void loadScene(Scene scene) {
		this.enqueue(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				rootNode.detachAllChildren();
				for(Light l : rootNode.getWorldLightList())
					rootNode.removeLight(l);
				dcpuModels.clear();

				String skybox = scene.getSkybox_file();
				rootNode.attachChild(SkyFactory.createSky(assetManager, assetManager.loadTexture("Textures/Sky/" + skybox + "/west.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/east.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/north.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/south.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/up.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/down.png")));

				bulletAppState = new BulletAppState();
				stateManager.attach(bulletAppState);
				sceneModel = assetManager.loadModel(scene.getScene_file());
				CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
				sceneBody = new RigidBodyControl(sceneShape, 0);
				sceneModel.addControl(sceneBody);

				rootNode.attachChild(sceneModel);
				bulletAppState.getPhysicsSpace().add(sceneBody);
				bulletAppState.getPhysicsSpace().add(player);

				dcpuScreens = new Node();
				rootNode.attachChild(dcpuScreens);

				for(Light light: scene.getLights())
						rootNode.addLight(light);

				guiNode.detachAllChildren();
				guiFont = assetManager.loadFont("Interface/Fonts/Minecraft.fnt");
				hello_text = new BitmapText(guiFont);
				hello_text.setSize(guiFont.getCharSet().getRenderedSize());
				hello_text.setText("Welcome to 0x10clone !\nPress tab to focus a specific DCPU.");
				hello_text.setLocalTranslation(0, settings.getHeight(), 0);
				guiNode.attachChild(hello_text);

				inputManager.addRawInputListener(new RawInputListener() {
					@Override
					public void beginInput() {}
					@Override
					public void endInput() {}
					@Override
					public void onJoyAxisEvent(JoyAxisEvent joyAxisEvent) {}
					@Override
					public void onJoyButtonEvent(JoyButtonEvent joyButtonEvent) {}
					@Override
					public void onMouseMotionEvent(MouseMotionEvent mouseMotionEvent) {}
					@Override
					public void onMouseButtonEvent(MouseButtonEvent mouseButtonEvent) {}
					@Override
					public void onTouchEvent(TouchEvent touchEvent) {}

					@Override
					public void onKeyEvent(KeyInputEvent keyInputEvent) {
						if(focusedOnDCPU && focusedDCPU != null && !keyInputEvent.isReleased()) { //For an unknown reason, if isRealeased() == true, then getKeyChar() doesn't return a valid character...
							if(focusedDCPU.getUserData("id") == null)
								return;

							int id = focusedDCPU.getUserData("id");

							myClient.send(new MessageDCPUKeyCode(id, keyInputEvent.getKeyChar() >= 0x20 && keyInputEvent.getKeyChar() < 0x7F, (keyInputEvent.getKeyChar() >= 0x20 && keyInputEvent.getKeyChar() < 0x7F) ? keyInputEvent.getKeyChar() : keyInputEvent.getKeyCode()));
						}
					}
				});

				rootNode.attachChild(playerNode);

				loadingScene = false;
				System.out.println("Scene loaded!");

				return null;
			}
		});
	}

	public void addDCPU(Vector3f position, Quaternion rotation, float scale, int id) {
		dcpuModels.add(new DCPUModel(id, null, hardwareTracker, bulletAppState, assetManager, rootNode, position, rotation, scale, hardwareTracker.requestLem()));
		dcpuScreens.attachChild(dcpuModels.getLast().getScreen());
	}

	private void initAudio() {
		footSteps = new AudioNode[10];
		for(int i = 0; i < 10; ++i) {
			footSteps[i] = new AudioNode(assetManager, "Sound/FootSteps/" + i + ".ogg", false);
			footSteps[i].setPositional(false);
			footSteps[i].setLooping(false);
		}
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
			if(name.equals("focusDCPU") && !pressed) {
				if (!focusedOnDCPU) {
					CollisionResults results = new CollisionResults();
					Ray ray = new Ray(cam.getLocation(), cam.getDirection());
					dcpuScreens.collideWith(ray, results);
					if (results.size() > 0) {
						CollisionResult closest = results.getClosestCollision();
						if(closest.getDistance() <= 5) {
							focusedOnDCPU = true;
							focusedDCPU = closest.getGeometry();
							flyCam.setEnabled(false);
							mouseInput.setCursorVisible(true);
						}
					}
				} else {
					flyCam.setEnabled(true);
					mouseInput.setCursorVisible(false);
					focusedOnDCPU = false;
				}
			} else if(name.equals("resetDCPU") && focusedOnDCPU && !pressed) {
				if(focusedDCPU.getUserData("id") == null)
					return;

				int id = focusedDCPU.getUserData("id");
				myClient.send(new MessageResetDCPU(id));
			} else if(name.equals("ToggleFlyCam") && !focusedOnDCPU) {
				if (!pressed) //on release
					flyCam.setEnabled(!flyCam.isEnabled());

				if (flyCam.isEnabled())
					mouseInput.setCursorVisible(false);
				else
					mouseInput.setCursorVisible(true);
			} else if(name.equals("Jump") && !focusedOnDCPU && pressed) {
				player.jump();
			} else if(name.equals("Forwards") && !focusedOnDCPU) {
				forwards = pressed;
			} else if(name.equals("Backwards") && !focusedOnDCPU) {
				backwards = pressed;
			} else if(name.equals("Left") && !focusedOnDCPU) {
				left = pressed;
			} else if(name.equals("Right") && !focusedOnDCPU) {
				right = pressed;
			}
		}
	};

	@Override
	public void simpleUpdate(float tpf) {
		camDir.set(cam.getDirection()).multLocal(6f);
		camLeft.set(cam.getLeft()).multLocal(4f);
		walkDirection.set(0, 0, 0);
		if(left)
			walkDirection.addLocal(camLeft);
		if(right)
			walkDirection.addLocal(camLeft.negate());
		if(forwards)
			walkDirection.addLocal(camDir);
		if(backwards)
			walkDirection.addLocal(camDir.negate());
		player.setWalkDirection(walkDirection.multLocal(1, 0, 1));
		cam.setLocation(playerNode.getLocalTranslation().addLocal(0, 1.5f, 0));

		listener.setLocation(cam.getLocation());
		listener.setRotation(cam.getRotation());

		timeSinceLastStepSound += (tpf * 1000);
		if(walkDirection.length() > 0.1f && player.isOnGround() && timeSinceLastStepSound >= 300) {
			int i = random.nextInt(10);
			footSteps[i].setLocalTranslation(playerNode.getLocalTranslation());
			footSteps[i].playInstance();
			timeSinceLastStepSound = 0;
		}

		for(DCPUModel m : dcpuModels) {
			m.sound();
		}

		MessagePlayerLocation messagePlayerLocation = new MessagePlayerLocation(playerNode.getLocalTranslation());
		messagePlayerLocation.setReliable(false);
		myClient.send(messagePlayerLocation);
	}

	@Override
	public void simpleRender(RenderManager rm) {
		super.simpleRender(rm);
		for(DCPUModel m : dcpuModels)
			m.render(assetManager);
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
		executor.shutdown();
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

	public void setPlayerPosition(Vector3f playerPosition) {
		enqueue(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				playerNode.setLocalTranslation(playerPosition);
				return null;
			}
		});
	}

	public boolean isLoadingScene() {
		return loadingScene;
	}

	public void setLoadingScene(boolean loadingScene) {
		this.loadingScene = loadingScene;
	}

	public LinkedList<DCPUModel> getDcpuModels() {
		return dcpuModels;
	}
}
