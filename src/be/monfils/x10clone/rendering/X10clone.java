package be.monfils.x10clone.rendering;

import be.monfils.x10clone.dcpu.DCPU;
import be.monfils.x10clone.dcpu.DCPUTickingThread;
import be.monfils.x10clone.dcpu.GenericKeyboard;
import be.monfils.x10clone.dcpu.HardwareTracker;
import com.jme3.app.SimpleApplication;
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
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;

import java.util.LinkedList;

/**
 * Created by nathan on 18/07/15.
 */
public class X10clone extends SimpleApplication {

	private Spatial sceneModel;
	private Geometry planetGeom;
	private DirectionalLight sun;
	private AmbientLight al;
	private PointLight pl;
	private BitmapText hello_text;
	public static HardwareTracker hardwareTracker = new HardwareTracker();
	private LinkedList<DCPUModel> dcpus = new LinkedList<>();
	private DCPUTickingThread dcpuTickingThread;
	private Node dcpuScreens;
	private boolean focusedOnDCPU;
	private Spatial focusedDCPU;

	private BulletAppState bulletAppState;
	private RigidBodyControl sceneBody;
	private BetterCharacterControl player;
	private Node playerNode;
	private Vector3f walkDirection = new Vector3f(), camDir = new Vector3f(), camLeft = new Vector3f();
	private boolean forwards, backwards, left, right;

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
		rootNode.attachChild(SkyFactory.createSky(assetManager, assetManager.loadTexture("Textures/Sky/Stars/west.png"), assetManager.loadTexture("Textures/Sky/Stars/east.png"), assetManager.loadTexture("Textures/Sky/Stars/north.png"), assetManager.loadTexture("Textures/Sky/Stars/south.png"), assetManager.loadTexture("Textures/Sky/Stars/up.png"), assetManager.loadTexture("Textures/Sky/Stars/down.png")));

		cam.setFrustumPerspective(90, (float) settings.getWidth() / (float) settings.getHeight(), 0.05f, 10000f);

		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);
		sceneModel = assetManager.loadModel("Scenes/TestScene.j3o");
		CollisionShape sceneShape = CollisionShapeFactory.createMeshShape(sceneModel);
		sceneBody = new RigidBodyControl(sceneShape, 0);
		sceneModel.addControl(sceneBody);

		player = new BetterCharacterControl(0.5f, 1.8f, 1);
		player.setJumpForce(new Vector3f(0.0f, 5.0f, 0.0f));

		playerNode = new Node("Player");
		playerNode.setLocalTranslation(0, 5, 0);
		rootNode.attachChild(playerNode);
		playerNode.addControl(player);

		rootNode.attachChild(sceneModel);
		bulletAppState.getPhysicsSpace().add(sceneBody);
		bulletAppState.getPhysicsSpace().add(player);

		setupMeshes();

		dcpuTickingThread = new DCPUTickingThread(dcpus, assetManager);
		dcpuTickingThread.start();

		/* sun = new DirectionalLight();
		sun.setDirection(new Vector3f(5f, -5f, -8f));
		rootNode.addLight(sun); */

		pl = new PointLight();
		pl.setColor(new ColorRGBA(1f, 0.9f, 0.9f, 0.8f));
		pl.setPosition(new Vector3f(-5f, 5f, -5f));
		rootNode.addLight(pl);

		al = new AmbientLight();
		al.setColor(ColorRGBA.White.mult(1.3f));
		rootNode.addLight(al);

		guiNode.detachAllChildren();
		guiFont = assetManager.loadFont("Interface/Fonts/Minecraft.fnt");
		hello_text = new BitmapText(guiFont);
		hello_text.setSize(guiFont.getCharSet().getRenderedSize());
		hello_text.setText("Welcome to 0x10clone !\nPress tab to focus a specific DCPU.");
		hello_text.setLocalTranslation(0, settings.getHeight(), 0);
		guiNode.attachChild(hello_text);

		initKeys();

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
					GenericKeyboard k = hardwareTracker.getKeyboard(focusedDCPU.getUserData("Keyboard"));
					if(k != null) {
						if(keyInputEvent.getKeyChar() >= 0x20 && keyInputEvent.getKeyChar() < 0x7F)
							k.pressedKey(keyInputEvent.getKeyChar());
						else
							k.pressedKeyCode(keyInputEvent.getKeyCode());
					}
				}
			}
		});
	}

	private void setupMeshes() {
		Sphere planetMesh = new Sphere(32, 32, 64);
		planetGeom = new Geometry("Planet", planetMesh);
		planetMesh.setTextureMode(Sphere.TextureMode.Projected);
		TangentBinormalGenerator.generate(planetMesh);
		Material planetMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
		planetMat.setTexture("DiffuseMap", assetManager.loadTexture("Textures/Planet/planet_colormap.png"));
		planetMat.setTexture("NormalMap", assetManager.loadTexture("Textures/Planet/planet_normalmap.png"));
		planetMat.setBoolean("UseMaterialColors", true);
		planetMat.setColor("Diffuse", ColorRGBA.White);
		planetMat.setColor("Specular", ColorRGBA.DarkGray);
		planetMat.setFloat("Shininess", 16f);
		planetGeom.setMaterial(planetMat);
		planetGeom.setLocalTranslation(1000, 30, -1000);
		rootNode.attachChild(planetGeom);

		dcpuScreens = new Node();
		rootNode.attachChild(dcpuScreens);

		dcpus.add(new DCPUModel(bulletAppState, assetManager, rootNode, new Vector3f(2, 1, -7), new Quaternion(), 1.0f, "assets/DCPU/FrOSt.bin"));
		dcpuScreens.attachChild(dcpus.getLast().getScreen());
		dcpus.add(new DCPUModel(bulletAppState, assetManager, rootNode, new Vector3f(-5, 0.1f, 5), new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y), 1.0f, "assets/DCPU/BOLD.bin"));
		dcpuScreens.attachChild(dcpus.getLast().getScreen());
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
						focusedOnDCPU = true;
						focusedDCPU = closest.getGeometry();
						flyCam.setEnabled(false);
						mouseInput.setCursorVisible(true);
					}
				} else {
					flyCam.setEnabled(true);
					mouseInput.setCursorVisible(false);
					focusedOnDCPU = false;
				}
			} else if(name.equals("resetDCPU") && focusedOnDCPU && !pressed) {
				DCPU dcpu = hardwareTracker.getDCPU(focusedDCPU.getUserData("DCPU"));
				if(dcpu != null) {
					dcpu.reset();
				}
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
	}

	@Override
	public void simpleRender(RenderManager rm) {
		super.simpleRender(rm);
	}

	@Override
	public void stop() {
		super.stop();
		dcpuTickingThread.setStopped();
		for(DCPUModel dcpu : dcpus)
			dcpu.stop();
	}
}
