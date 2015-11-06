package be.monfils.x10clone.rendering;

import be.monfils.x10clone.SceneDescriptor;
import be.monfils.x10clone.dcpu.DCPUModel;
import be.monfils.x10clone.dcpu.HardwareTracker;
import be.monfils.x10clone.messages.MessageDCPUKeyCode;
import be.monfils.x10clone.messages.MessagePlayerLocation;
import be.monfils.x10clone.messages.MessageResetDCPU;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.audio.Listener;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.FlyByCamera;
import com.jme3.input.InputManager;
import com.jme3.input.MouseInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.*;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;

import java.util.LinkedList;
import java.util.Random;

/**
 * Created by nathan on 6/11/15.
 */
public class Scene {
	private Random random = new Random();

	private final Node rootNode;
	private final Listener listener;
	private final AssetManager assetManager;
	private final InputManager inputManager;
	private final AppSettings appSettings;

	private final SceneDescriptor sceneDescriptor;

	private final AppStateManager appStateManager;
	private BulletAppState bulletAppState;

	private LinkedList<DCPUModel> dcpuModels = new LinkedList<>();
	private Node dcpuScreens;
	private Node guiNode;
	private BitmapFont guiFont;
	private BitmapText hello_text;

	private Spatial sceneModel;
	private BetterCharacterControl player;
	private Node playerNode;
	private boolean focusedOnDCPU;
	private Spatial focusedDCPU;
	private Camera cam;
	private FlyByCamera flyCam;

	private AudioNode footSteps[];

	private Vector3f walkDirection = new Vector3f(), camDir = new Vector3f(), camLeft = new Vector3f();
	private boolean forwards, backwards, left, right;
	private int timeSinceLastStepSound = 0;

	private static HardwareTracker hardwareTracker = new HardwareTracker();

	private LinkedList<Spatial> players = new LinkedList<>();
	private Client myClient;

	private MouseInput mouseInput;

	private boolean loaded = false;

	public Scene(Node rootNode, Node guiNode, AssetManager assetManager, InputManager inputManager, AppSettings appSettings, SceneDescriptor sceneDescriptor, AppStateManager appStateManager, Listener listener, Client myClient, Camera cam, FlyByCamera flyCam, MouseInput mouseInput) {
		this.rootNode = rootNode;
		this.guiNode = guiNode;
		this.assetManager = assetManager;
		this.inputManager = inputManager;
		this.appSettings = appSettings;
		this.sceneDescriptor = sceneDescriptor;
		this.appStateManager = appStateManager;
		this.listener = listener;
		this.myClient = myClient;
		this.cam = cam;
		this.flyCam = flyCam;
		this.mouseInput = mouseInput;

		bulletAppState = new BulletAppState();

		player = new BetterCharacterControl(0.5f, 1.8f, 1);
		player.setJumpForce(new Vector3f(0.0f, 5.0f, 0.0f));

		playerNode = new Node("Player");
		playerNode.setLocalTranslation(0, 5, 0);
		rootNode.attachChild(playerNode);
		playerNode.addControl(player);

		footSteps = new AudioNode[10];
		for(int i = 0; i < 10; ++i) {
			footSteps[i] = new AudioNode(assetManager, "Sound/FootSteps/" + i + ".ogg", false);
			footSteps[i].setPositional(false);
			footSteps[i].setLooping(false);
		}
	}

	public void load() {
		rootNode.detachAllChildren();
		for(Light l : rootNode.getWorldLightList())
			rootNode.removeLight(l);
		dcpuModels.clear();

		String skybox = sceneDescriptor.getSkybox_file();
		rootNode.attachChild(SkyFactory.createSky(assetManager, assetManager.loadTexture("Textures/Sky/" + skybox + "/west.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/east.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/north.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/south.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/up.png"), assetManager.loadTexture("Textures/Sky/" + skybox + "/down.png")));

		bulletAppState = new BulletAppState();
		appStateManager.attach(bulletAppState);
		sceneModel = assetManager.loadModel(sceneDescriptor.getScene_file());
		CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
		RigidBodyControl sceneBody = new RigidBodyControl(sceneShape, 0);
		sceneModel.addControl(sceneBody);

		rootNode.attachChild(sceneModel);
		bulletAppState.getPhysicsSpace().add(sceneBody);
		bulletAppState.getPhysicsSpace().add(player);

		dcpuScreens = new Node();
		rootNode.attachChild(dcpuScreens);

		for(Light light: sceneDescriptor.getLights())
			rootNode.addLight(light);

		guiNode.detachAllChildren();
		guiFont = assetManager.loadFont("Interface/Fonts/Minecraft.fnt");
		hello_text = new BitmapText(guiFont);
		hello_text.setSize(guiFont.getCharSet().getRenderedSize());
		hello_text.setText("Welcome to 0x10clone !\nPress tab to focus a specific DCPU.");
		hello_text.setLocalTranslation(0, appSettings.getHeight(), 0);
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

		loaded = true;

		System.out.println("Scene loaded!");
	}

	public void update(float tpf) {
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

		Quaternion rotation = new Quaternion();
		rotation.lookAt(camDir.setY(0), new Vector3f(0, 1, 0));
		MessagePlayerLocation messagePlayerLocation = new MessagePlayerLocation(myClient.getId(), playerNode.getLocalTranslation(), rotation);
		messagePlayerLocation.setReliable(false);
		myClient.send(messagePlayerLocation);
	}

	public void render() {
		for(DCPUModel m : dcpuModels)
			m.render(assetManager);
	}

	public void addDCPU(Vector3f position, Quaternion rotation, float scale, int id) {
		dcpuModels.add(new DCPUModel(id, null, hardwareTracker, bulletAppState, assetManager, rootNode, position, rotation, scale, hardwareTracker.requestLem()));
		dcpuScreens.attachChild(dcpuModels.getLast().getScreen());
	}

	public void addPlayer(int id, Vector3f position) {
		Spatial spatial = assetManager.loadModel("Models/Character/Character.j3o");
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		spatial.setMaterial(mat);
		spatial.setUserData("id", id);
		spatial.setLocalTranslation(position);
		players.add(spatial);
		rootNode.attachChild(spatial);
	}

	public void removePlayer(int id) {
		for(Spatial s : players) {
			if((int) s.getUserData("id") == id) {
				rootNode.detachChild(s);
				players.remove(s);
			}
		}
	}

	public void setPlayerPosition(int id, Vector3f position, Quaternion rotation) {
		if(id == myClient.getId()) {
			playerNode.setLocalTranslation(position);
			playerNode.setLocalRotation(rotation);
		}
		else {
			for(Spatial player : players) {
				if(player.getUserData("id") != null && (int) player.getUserData("id") == id) {
					player.setLocalTranslation(position);
					player.setLocalRotation(rotation);
					break;
				}
			}
		}
	}

	public void input(int input, boolean pressed, float tpf) {
		switch (input) {
			case Controls.FOCUS_DCPU:
				if (!focusedOnDCPU && !pressed) {
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
				} else if(!pressed) {
					flyCam.setEnabled(true);
					mouseInput.setCursorVisible(false);
					focusedOnDCPU = false;
				}
				break;
			case Controls.RESET_DCPU:
				if(!pressed) {
					if (focusedDCPU.getUserData("id") == null)
						return;

					int id = focusedDCPU.getUserData("id");
					myClient.send(new MessageResetDCPU(id));
				}
				break;
			case Controls.TOGGLE_FLY_CAM:
				if(!focusedOnDCPU) {
					if (!pressed) //on release
						flyCam.setEnabled(!flyCam.isEnabled());

					if (flyCam.isEnabled())
						mouseInput.setCursorVisible(false);
					else
						mouseInput.setCursorVisible(true);
				}
				break;
			case Controls.JUMP:
				if(!focusedOnDCPU)
					player.jump();
				break;
			case Controls.FORWARDS:
				if(!focusedOnDCPU)
					forwards = pressed;
				break;
			case Controls.BACKWARDS:
				if(!focusedOnDCPU)
					backwards = pressed;
				break;
			case Controls.LEFT:
				if(!focusedOnDCPU)
					left = pressed;
				break;
			case Controls.RIGHT:
				if(!focusedOnDCPU)
					right = pressed;
				break;
		}
	}

	public LinkedList<DCPUModel> getDcpuModels() {
		return dcpuModels;
	}

	public boolean isLoaded() {
		return loaded;
	}

	public class Controls {
		public static final int FOCUS_DCPU = 0;
		public static final int RESET_DCPU = 1;
		public static final int TOGGLE_FLY_CAM = 2;
		public static final int JUMP = 3;
		public static final int FORWARDS = 4;
		public static final int BACKWARDS = 5;
		public static final int LEFT = 6;
		public static final int RIGHT = 7;
	}
}
