package be.monfils.x10clone.rendering;

import be.monfils.x10clone.dcpu.*;
import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.event.*;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;
import org.lwjgl.input.Keyboard;

import java.util.LinkedList;

/**
 * Created by nathan on 18/07/15.
 */
public class X10clone extends SimpleApplication {

	private Spatial suzanne;
	private Geometry planetGeom;
	private DirectionalLight sun;
	private AmbientLight al;
	private BitmapText hello_text;
	public static HardwareTracker hardwareTracker = new HardwareTracker();
	private LinkedList<DCPUModel> dcpus = new LinkedList<>();
	private Node dcpuScreens;
	private boolean focusedOnDCPU;
	private Spatial focusedDCPU;

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

		cam.setFrustumFar(10000f);

		suzanne = assetManager.loadModel("Models/Suzanne/Suzanne.obj");
		suzanne.setLocalTranslation(new Vector3f(3, 0, -15));
		rootNode.attachChild(suzanne);

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

		dcpus.add(new DCPUModel(assetManager, rootNode, new Vector3f(2, 0, 0), new Quaternion(), "assets/DCPU/palettetest.bin"));
		dcpuScreens.attachChild(dcpus.getLast().getScreen());
		dcpus.add(new DCPUModel(assetManager, rootNode, new Vector3f(-5, 0, 5), new Quaternion().fromAngleAxis(3.14159f / 2.0f, Vector3f.UNIT_Y), "assets/DCPU/FrOSt.bin"));
		dcpuScreens.attachChild(dcpus.getLast().getScreen());

		sun = new DirectionalLight();
		sun.setDirection(new Vector3f(-0.1f, -0.7f, -1.0f));
		rootNode.addLight(sun);

		al = new AmbientLight();
		al.setColor(ColorRGBA.White.mult(1.3f));
		rootNode.addLight(al);

		guiNode.detachAllChildren();
		guiFont = assetManager.loadFont("Interface/Fonts/Minecraft.fnt");
		hello_text = new BitmapText(guiFont);
		hello_text.setSize(guiFont.getCharSet().getRenderedSize());
		hello_text.setText("Welcome to 0x10clone !");
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

	private void initKeys() {
		inputManager.addMapping("Forwards", new KeyTrigger(KeyInput.KEY_UP));
		inputManager.addMapping("Backwards", new KeyTrigger(KeyInput.KEY_DOWN));
		inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
		inputManager.addMapping("ToggleFlyCam", new KeyTrigger(KeyInput.KEY_RETURN), new KeyTrigger(KeyInput.KEY_NUMPADENTER));
		inputManager.addMapping("focusDCPU", new KeyTrigger(KeyInput.KEY_TAB));

		inputManager.addListener(analogListener, "Forwards", "Backwards", "Left", "Right");
		inputManager.addListener(actionListener, "ToggleFlyCam", "focusDCPU");
	}

	private AnalogListener analogListener = new AnalogListener() {
		@Override
		public void onAnalog(String name, float value, float tpf) {
			if(!focusedOnDCPU) {
				if (name.equals("Forwards")) {
					suzanne.move(tpf, 0, 0);
				} else if (name.equals("Backwards")) {
					suzanne.move(-tpf, 0, 0);
				} else if (name.equals("Left")) {
					suzanne.move(0, 0, tpf);
				} else if (name.equals("Right")) {
					suzanne.move(0, 0, -tpf);

				}
			}
		}
	};

	private ActionListener actionListener = new ActionListener() {
		@Override
		public void onAction(String name, boolean pressed, float tpf) {
			if(name.equals("ToggleFlyCam") && !focusedOnDCPU) {
				if (!pressed) //on release
					flyCam.setEnabled(!flyCam.isEnabled());
				if (flyCam.isEnabled()) {
					mouseInput.setCursorVisible(false);
				}
			} else if(name.equals("focusDCPU") && !pressed) {
				if(!focusedOnDCPU) {
					CollisionResults results = new CollisionResults();
					Ray ray = new Ray(cam.getLocation(), cam.getDirection());
					dcpuScreens.collideWith(ray, results);
					if (results.size() > 0) {
						CollisionResult closest = results.getClosestCollision();
						focusedOnDCPU = true;
						flyCam.setEnabled(false);
						focusedDCPU = closest.getGeometry();
					}
				} else {
					flyCam.setEnabled(true);
					mouseInput.setCursorVisible(false);
					focusedOnDCPU = false;
				}
			}
		}
	};

	@Override
	public void simpleUpdate(float tpf) {
		for(DCPUModel dcpu : dcpus) dcpu.tick(); //TODO : Move that elsewhere with a consistent refresh rate
		//hello_text.setText(dcpuManager.dump());
	}

	@Override
	public void simpleRender(RenderManager rm) {
		super.simpleRender(rm);
		for(DCPUModel dcpu : dcpus) {
			dcpu.render(assetManager);
		}
	}

	@Override
	public void stop() {
		super.stop();
		for(DCPUModel dcpu : dcpus)
		dcpu.stop();
	}
}
