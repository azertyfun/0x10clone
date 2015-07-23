package be.monfils.x10clone.rendering;

import be.monfils.x10clone.dcpu.*;
import com.jme3.app.SimpleApplication;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.AnalogListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import com.jme3.util.TangentBinormalGenerator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by nathan on 18/07/15.
 */
public class X10clone extends SimpleApplication {

	private Spatial suzanne, screen;
	private Geometry planetGeom;
	private DirectionalLight sun;
	private AmbientLight al;
	private BitmapText hello_text;
	public static HardwareTracker hardwareTracker = new HardwareTracker();
	private DCPUManager dcpuManager;

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


		screen = new Geometry("Screen", new Quad(2.56f, 1.92f));
		Material screen_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		Texture screen_tex = assetManager.loadTexture("Textures/lem1802/boot_transparent.png");
		screen_tex.setMagFilter(Texture.MagFilter.Nearest);
		screen_mat.setTexture("ColorMap", screen_tex);
		screen_mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		screen.setQueueBucket(RenderQueue.Bucket.Transparent);
		screen.setMaterial(screen_mat);
		rootNode.attachChild(screen);

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

		try {
			byte[] testRam_b = Files.readAllBytes(Paths.get("assets/DCPU/palettetest.bin"));
			char testRam[] = new char[0x10000];
			for(int i = 0; i < 0x10000; ++i) {
				testRam[i] = (char) (testRam_b[i * 2] << 8);
				testRam[i] |= (char) (testRam_b[i * 2 + 1] & 0xFF);
			}
			dcpuManager = new DCPUManager(hardwareTracker.requestDCPU(), testRam, 1, 1, 1);
			dcpuManager.startDCPU();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void initKeys() {
		inputManager.addMapping("Forwards", new KeyTrigger(KeyInput.KEY_UP));
		inputManager.addMapping("Backwards", new KeyTrigger(KeyInput.KEY_DOWN));
		inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_LEFT));
		inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_RIGHT));
		inputManager.addMapping("ToggleFlyCam", new KeyTrigger(KeyInput.KEY_RETURN), new KeyTrigger(KeyInput.KEY_NUMPADENTER));

		inputManager.addListener(analogListener, "Forwards", "Backwards", "Left", "Right");
		inputManager.addListener(actionListener, "ToggleFlyCam");
	}

	private AnalogListener analogListener = new AnalogListener() {
		@Override
		public void onAnalog(String name, float value, float tpf) {
			if(name.equals("Forwards")) {
				suzanne.move(tpf, 0, 0);
			} else if(name.equals("Backwards")) {
				suzanne.move(-tpf, 0, 0);
			} else if(name.equals("Left")) {
				suzanne.move(0, 0, tpf);
			} else if(name.equals("Right")) {
				suzanne.move(0, 0, -tpf);

			}
		}
	};

	private ActionListener actionListener = new ActionListener() {
		@Override
		public void onAction(String name, boolean pressed, float tpf) {
			if(!pressed) //on release
				flyCam.setEnabled(!flyCam.isEnabled());
			if(flyCam.isEnabled()) {
				mouseInput.setCursorVisible(false);
			}
		}
	};

	@Override
	public void simpleUpdate(float tpf) {
		dcpuManager.tick(); //TODO : Move that elsewhere with a consistent refresh rate
		hello_text.setText(dcpuManager.dump());
	}

	@Override
	public void simpleRender(RenderManager rm) {
		super.simpleRender(rm);
		Texture tex = dcpuManager.getLems().getFirst().render(); //TODO : Do something nice with that
		Material screenmat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		screenmat.setTexture("ColorMap", tex);
		screenmat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		screen.setMaterial(screenmat);
	}

	@Override
	public void stop() {
		super.stop();
		dcpuManager.stopDCPU();
	}
}
