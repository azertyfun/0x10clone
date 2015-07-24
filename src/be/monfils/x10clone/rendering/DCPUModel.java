package be.monfils.x10clone.rendering;

import be.monfils.x10clone.dcpu.*;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by nathan on 23/07/15.
 */
public class DCPUModel {

	private Spatial screen;
	//private DCPUManager dcpuManager;
	private Material mat;
	private DCPU dcpu;
	private LEM1802 lem1802;
	private GenericClock clock;
	private GenericKeyboard keyboard;

	public DCPUModel(BulletAppState appState, AssetManager assetManager, Node node, Vector3f position, Quaternion rotation, float scale, String file) {
		setupGeometry(appState, assetManager, node, position, rotation, scale);
		setupDCPU(file);

		screen.setUserData("Keyboard", keyboard.getID());
	}

	private void setupDCPU(String file) {
		dcpu = X10clone.hardwareTracker.requestDCPU();
		lem1802 = X10clone.hardwareTracker.requestLem();
		lem1802.connectTo(dcpu);
		lem1802.powerOn();
		clock = X10clone.hardwareTracker.requestClock();
		clock.connectTo(dcpu);
		clock.powerOn();
		keyboard = X10clone.hardwareTracker.requestKeyboard();
		keyboard.connectTo(dcpu);
		keyboard.powerOn();

		try {
			byte[] ram_b = Files.readAllBytes(Paths.get(file));
			char ram[] = new char[0x10000];
			for(int i = 0; i < 0x10000; ++i) {
				ram[i] = (char) (ram_b[i * 2] << 8);
				ram[i] |= (char) (ram_b[i * 2 + 1] & 0xFF);
			}

			dcpu.setRam(ram);
			dcpu.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setupGeometry(BulletAppState appState, AssetManager assetManager, Node node, Vector3f position, Quaternion rotation, float scale) {
		Node mainNode = new Node();

		screen = new Geometry("Screen", new Quad(2.56f, 1.92f));
		Material screen_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		Texture screen_tex = assetManager.loadTexture("Textures/lem1802/boot_transparent.png");
		screen_tex.setMagFilter(Texture.MagFilter.Nearest);
		screen_mat.setTexture("ColorMap", screen_tex);
		screen_mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		screen.setQueueBucket(RenderQueue.Bucket.Transparent);
		screen.setMaterial(screen_mat);
		mainNode.attachChild(screen);

		Spatial crt = assetManager.loadModel("Models/CRT/CRT.obj");
		mainNode.attachChild(crt);

		Spatial kb = assetManager.loadModel("Models/Keyboard/Keyboard.obj");
		mainNode.attachChild(kb);

		mainNode.scale(scale);
		screen.scale(0.46f);
		crt.scale(0.5f);
		kb.scale(0.4f);

		crt.setLocalRotation(rotation);
		kb.setLocalRotation(rotation);
		kb.rotate(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_Y));
		screen.setLocalRotation(rotation);

		crt.setLocalTranslation(position);
		kb.setLocalTranslation(rotation.multLocal(new Vector3f(0, 0, 0.5f)));
		kb.move(position);
		screen.setLocalTranslation(rotation.multLocal(new Vector3f(-0.596f, 0.0559998f, 0.05f)));
		screen.move(position);

		node.attachChild(mainNode);

		CollisionShape crtShape = CollisionShapeFactory.createMeshShape(crt);
		RigidBodyControl crtBody = new RigidBodyControl(crtShape, 0);
		crtBody.setPhysicsLocation(position);
		crtBody.setPhysicsRotation(rotation);
		appState.getPhysicsSpace().add(crtBody);
	}

	public void tick() {
		lem1802.tick60hz();
		clock.tick60hz();
		keyboard.tick60hz();
	}

	public void render(AssetManager assetManager) {
		Texture tex = lem1802.render();
		Material screenmat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		screenmat.setTexture("ColorMap", tex);
		screenmat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		screen.setMaterial(screenmat);
	}

	public void stop() {
		dcpu.setStopped();
	}

	public Spatial getScreen() {
		return screen;
	}
}
