package be.monfils.x10clone.states;

import be.monfils.x10clone.SceneDescriptor;
import be.monfils.x10clone.dcpu.DCPUModel;
import be.monfils.x10clone.dcpu.HardwareTracker;
import be.monfils.x10clone.messages.MessageDCPUKeyCode;
import be.monfils.x10clone.messages.MessagePlayerLocation;
import be.monfils.x10clone.messages.MessageResetDCPU;
import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
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
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.network.Client;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.jme3.ui.Picture;
import com.jme3.util.SkyFactory;

import java.util.LinkedList;
import java.util.Random;

/**
 * Created by nathan on 10/11/15.
 */
public class StateInGame extends AbstractAppState {
	private Random random = new Random();

	private Node rootNode;

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

	private StateFPSModel stateFPSModel;

	public StateInGame(Node rootNode, Node guiNode, AssetManager assetManager, InputManager inputManager, AppSettings appSettings, SceneDescriptor sceneDescriptor, AppStateManager appStateManager, Listener listener, Client myClient, Camera cam, FlyByCamera flyCam, MouseInput mouseInput) {
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
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

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

		Picture crosshair = new Picture("Crosshair");
		crosshair.setImage(assetManager, "Interface/icons/crosshair.png", true);
		crosshair.setWidth(16);
		crosshair.setHeight(16);
		crosshair.setPosition(cam.getWidth() / 2 - 8, cam.getHeight() / 2 - 8);
		guiNode.attachChild(crosshair);

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

		boolean found = false;
		for(Light l : sceneDescriptor.getLights()) {
			if(l instanceof DirectionalLight) {
				stateManager.attach(stateFPSModel = new StateFPSModel(((DirectionalLight) l).getDirection()));
				found = true;
				break;
			}
		}
		if(!found)
			stateManager.attach(stateFPSModel = new StateFPSModel(new Vector3f(0.2f, -0.2f, 1.5f)));


		loaded = true;

		System.out.println("Scene loaded!");
	}

	@Override
	public void update(float tpf) {
		super.update(tpf);

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

		stateFPSModel.updateLocation(cam.getLocation());
		stateFPSModel.updateDirection(cam.getDirection());
	}

	@Override
	public void render(RenderManager rm) {
		super.render(rm);

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

	public void input(Controls input, boolean pressed, float tpf) {
		switch (input) {
			case FOCUS_DCPU:
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
			case RESET_DCPU:
				if(!pressed) {
					if (focusedDCPU.getUserData("id") == null)
						return;

					int id = focusedDCPU.getUserData("id");
					myClient.send(new MessageResetDCPU(id));
				}
				break;
			case TOGGLE_FLY_CAM:
				if(!focusedOnDCPU) {
					if (!pressed) //on release
						flyCam.setEnabled(!flyCam.isEnabled());

					if (flyCam.isEnabled())
						mouseInput.setCursorVisible(false);
					else
						mouseInput.setCursorVisible(true);
				}
				break;
			case JUMP:
				if(!focusedOnDCPU)
					player.jump();
				break;
			case FORWARDS:
				if(!focusedOnDCPU)
					forwards = pressed;
				break;
			case BACKWARDS:
				if(!focusedOnDCPU)
					backwards = pressed;
				break;
			case LEFT:
				if(!focusedOnDCPU)
					left = pressed;
				break;
			case RIGHT:
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

	public enum Controls {
		FOCUS_DCPU,
		RESET_DCPU,
		TOGGLE_FLY_CAM,
		JUMP,
		FORWARDS,
		BACKWARDS,
		LEFT,
		RIGHT;
	}
}
