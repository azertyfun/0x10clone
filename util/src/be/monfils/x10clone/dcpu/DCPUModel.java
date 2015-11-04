package be.monfils.x10clone.dcpu;

import be.monfils.x10clone.messages.MessageUpdateVSSSound;
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
import com.jme3.network.Server;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

import java.io.IOException;
import java.nio.ByteBuffer;
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
	private HardwareTracker hardwareTracker;
	private final int id;

	private Vector3f position;
	private Quaternion rotation;
	private float scale;
	private Server server;

	public DCPUModel(int id, Server server, HardwareTracker hardwareTracker, BulletAppState appState, AssetManager assetManager, Node node, Vector3f position, Quaternion rotation, float scale, String file) {
		this.id = id;
		this.server = server;
		this.position = position;
		this.rotation = rotation;
		this.scale = scale;

		this.hardwareTracker = hardwareTracker;

		setupGeometry(appState, assetManager, node, position, rotation, scale);

		setupDCPU(file);

		screen.setUserData("Keyboard", keyboard.getID());
		screen.setUserData("DCPU", dcpu.getID());
		screen.setUserData("id", id);
	}

	public DCPUModel(int id, Server server, HardwareTracker hardwareTracker, BulletAppState appState, AssetManager assetManager, Node node, Vector3f position, Quaternion rotation, float scale, LEM1802 lem1802) {
		this.id = id;
		this.server = server;
		this.position = position;
		this.rotation = rotation;
		this.scale = scale;
		this.hardwareTracker = hardwareTracker;

		setupGeometry(appState, assetManager, node, position, rotation, scale);

		setupDCPU(lem1802);

		screen.setUserData("Keyboard", keyboard.getID());
		if(dcpu != null)
			screen.setUserData("DCPU", dcpu.getID());
		else
			screen.setUserData("DCPU", -1);
		screen.setUserData("id", id);
	}

	private void setupDCPU(String file) {
		dcpu = hardwareTracker.requestDCPU();
		lem1802 = hardwareTracker.requestLem();
		lem1802.connectTo(dcpu);
		lem1802.powerOn();
		clock = hardwareTracker.requestClock();
		clock.connectTo(dcpu);
		clock.powerOn();
		keyboard = hardwareTracker.requestKeyboard();
		keyboard.connectTo(dcpu);
		keyboard.powerOn();
		vss1224 = hardwareTracker.requestVss();
		vss1224.connectTo(dcpu);
		vss1224.powerOn();

		try {
			byte[] ram_b = Files.readAllBytes(Paths.get(file));
			char ram[] = new char[0x10000];
			for (int i = 0; i < 0x10000; ++i) {
				ram[i] = (char) (ram_b[i * 2] << 8);
				ram[i] |= (char) (ram_b[i * 2 + 1] & 0xFF);
			}
			dcpu.setRam(ram);
			dcpu.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void setupDCPU(LEM1802 lem1802) {
		dcpu = null;
		this.lem1802 = lem1802;
		if(dcpu != null)
			this.lem1802.connectTo(dcpu);
		this.lem1802.powerOn();
		clock = hardwareTracker.requestClock();
		if(dcpu != null)
			clock.connectTo(dcpu);
		clock.powerOn();
		keyboard = hardwareTracker.requestKeyboard();
		if(dcpu != null)
			keyboard.connectTo(dcpu);
		keyboard.powerOn();
		vss1224 = hardwareTracker.requestVss();
		if(dcpu != null)
			vss1224.connectTo(dcpu);
		vss1224.powerOn();
	}

	private void setupGeometry(BulletAppState appState, AssetManager assetManager, Node node, Vector3f position, Quaternion rotation, float scale) {
		Node mainNode = new Node();
		mainNode.setName("DCPU Node");

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

	public void setupSound(AssetManager assetManager) {
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
	}

	public void sound() {
		int frequencies[] = vss1224.getFrequencies();

		float pitch[][] = new float[2][4];
		float volume[][] = new float[2][4];

		if(frequencies[0] != lastFrequencies[0]) {
			sound(0, frequencies, pitch, volume);
		}

		if(frequencies[1] != lastFrequencies[1]) {
			sound(1, frequencies, pitch, volume);
		}

		if(server != null && (frequencies[0] != lastFrequencies[0] || frequencies[1] != lastFrequencies[1]))
			server.broadcast(new MessageUpdateVSSSound(id, pitch, volume));

		lastFrequencies[0] = frequencies[0];
		lastFrequencies[1] = frequencies[1];
	}

	private void sound(int channel, int[] frequencies, float[][] outputPitch, float[][] outputVolume) {
		/* for(AudioNode n : speakerNodes[channel])
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
		} */
		if(server != null) {
			int i = 0;

			if (frequencies[channel] <= 200)
				i = 0;
			else if (frequencies[channel] <= 400)
				i = 1;
			else if (frequencies[channel] <= 800)
				i = 2;
			else if (frequencies[channel] <= 1600)
				i = 3;

			if (frequencies[channel] != 0) {
				outputVolume[channel][i] = (channel == 0 ? 0.02f : 0.015f);
				if (frequencies[channel] <= 200) {
					outputPitch[channel][i] = ((float) frequencies[channel] / 100.0f);
				} else if (frequencies[0] <= 400) {
					outputPitch[channel][i] = ((float) frequencies[channel] / 200.0f);
				} else if (frequencies[0] <= 800) {
					outputPitch[channel][i] = ((float) frequencies[channel] / 400.0f);
				} else if (frequencies[0] <= 1600) {
					outputPitch[channel][i] = ((float) frequencies[channel] / 800.0f);
				}
			}
		}

	}

	public void updateSound(float pitch[][], float volume[][]) {
		for(int channel = 0; channel < 2; ++channel) {
			for(int i = 0; i < 4; ++i) {
				if(speakerNodes != null && speakerNodes[channel] != null && speakerNodes[channel][i] != null) { //The sound might not be initialized yet
					speakerNodes[channel][i].setPitch((pitch[channel][i] != 0 ? pitch[channel][i] : 1));
					speakerNodes[channel][i].setVolume(volume[channel][i]);
				}
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
		if(dcpu != null)
			dcpu.setStopped();
	}

	public Spatial getScreen() {
		return screen;
	}

	public int getId() {
		return id;
	}

	public Vector3f getPosition() {
		return position;
	}

	public Quaternion getRotation() {
		return rotation;
	}

	public float getScale() {
		return scale;
	}

	public char[] getVideoRam() {
		return lem1802.getVideoRam();
	}

	public char[] getFontRam() {
		return lem1802.getFontRam();
	}

	public char[] getPaletteRam() {
		return lem1802.getPaletteRam();
	}

	public void setVideoRam(char[] videoRam) {
		lem1802.setVideoRam(videoRam);
	}

	public void setFontRam(char[] fontRam) {
		lem1802.setFontRam(fontRam);
	}

	public void setPaletteRam(char[] paletteRam) {
		lem1802.setPaletteRam(paletteRam);
	}

	public void setUseGivenBuffers(boolean useGivenBuffers) {
		lem1802.setUseGivenBuffers(useGivenBuffers);
	}

	public GenericKeyboard getKeyboard() {
		return keyboard;
	}

	public DCPU getDcpu() {
		return dcpu;
	}
}
