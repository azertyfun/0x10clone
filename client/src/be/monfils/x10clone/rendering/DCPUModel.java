package be.monfils.x10clone.rendering;

import be.monfils.x10clone.dcpu.*;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
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
	private AudioNode speakerNodes[][];
	private DCPU dcpu;
	private LEM1802 lem1802;
	private GenericClock clock;
	private GenericKeyboard keyboard;
	private VSS1224 vss1224;
	private PointLight screenLight;
	private int lastFrequencies[] =  new int[] {0, 0};

	public DCPUModel(BulletAppState appState, AssetManager assetManager, Node node, Vector3f position, Quaternion rotation, float scale, String file) {
		setupGeometry(appState, assetManager, node, position, rotation, scale);
		setupDCPU(file);

		screen.setUserData("Keyboard", keyboard.getID());
		screen.setUserData("DCPU", dcpu.getID());
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
		vss1224 = X10clone.hardwareTracker.requestVss();
		vss1224.connectTo(dcpu);
		vss1224.powerOn();

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
		Texture screen_tex = assetManager.loadTexture("Textures/lem1802/boot.png");
		screen_tex.setMagFilter(Texture.MagFilter.Nearest);
		screen_mat.setTexture("ColorMap", screen_tex);
		screen_mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		screen.setQueueBucket(RenderQueue.Bucket.Transparent);
		screen.setMaterial(screen_mat);
		mainNode.attachChild(screen);

		Spatial crt = assetManager.loadModel("Models/CRT/CRT.j3o");
		mainNode.attachChild(crt);

		Spatial kb = assetManager.loadModel("Models/Keyboard/Keyboard.j3o");
		mainNode.attachChild(kb);

		mainNode.scale(scale);
		screen.scale(0.46f);
		crt.scale(0.5f);
		kb.scale(0.4f);

		speakerNodes = new AudioNode[2][4];
		for(int channel = 0; channel < 2; ++channel) {
			for (int i = 0; i < 4; i++) {
				speakerNodes[channel][i] = new AudioNode(assetManager, "Sound/Beeps/beep_" + channel + "_" + ((int) Math.pow(2, i)) + "00hz.wav", false);
				speakerNodes[channel][i].setLocalTranslation(position);
				speakerNodes[channel][i].setLooping(true);
				speakerNodes[channel][i].setPositional(true);
				speakerNodes[channel][i].setVolume(0);
				speakerNodes[channel][i].play();
			}
		}

		screenLight = new PointLight();
		screenLight.setPosition(position);
		screenLight.setRadius(4f);
		node.addLight(screenLight);

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

	public void sound() {
		int frequencies[] = vss1224.getFrequencies();

		if(frequencies[0] != lastFrequencies[0]) {
			sound(0, frequencies);
		}

		if(frequencies[1] != lastFrequencies[1]) {
			sound(1, frequencies);
		}

		lastFrequencies[0] = frequencies[0];
		lastFrequencies[1] = frequencies[1];
	}

	private void sound(int channel, int[] frequencies) {
		for(AudioNode n : speakerNodes[channel])
			n.setVolume(0);

		int i = 0;

		if(frequencies[channel] <= 200)
			i = 0;
		else if(frequencies[channel] <= 400)
			i = 1;
		else if(frequencies[channel] <= 800)
			i = 2;
		else if(frequencies[channel] <= 1600)
			i = 3;

		if(frequencies[channel] != 0) {
			speakerNodes[channel][i].setVolume(channel == 0 ? 0.02f : 0.015f);
			if(frequencies[channel] <= 200) {
				speakerNodes[channel][i].setPitch((float) frequencies[channel] / 100.0f);
			} else if(frequencies[0] <= 400) {
				speakerNodes[channel][i].setPitch((float) frequencies[channel] / 200.0f);
			} else if(frequencies[0] <= 800) {
				speakerNodes[channel][i].setPitch((float) frequencies[channel] / 400.0f);
			} else if(frequencies[0] <= 1600) {
				speakerNodes[channel][i].setPitch((float) frequencies[channel] / 800.0f);
			}
		}
	}

	public void render(AssetManager assetManager) {
		lem1802.render();
		Texture tex = lem1802.getTexture();
		int[] avgColor = lem1802.getAverageColor();

		Material screenmat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		screenmat.setTexture("ColorMap", tex);
		screenmat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		screen.setMaterial(screenmat);

		screenLight.setColor(new ColorRGBA((float) avgColor[0] / 255.0f, (float) avgColor[1] / 255.0f, (float) avgColor[2] / 255.0f, 255));
	}

	public void stop() {
		dcpu.setStopped();
	}

	public Spatial getScreen() {
		return screen;
	}
}
