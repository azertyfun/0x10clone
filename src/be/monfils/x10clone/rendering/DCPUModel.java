package be.monfils.x10clone.rendering;

import be.monfils.x10clone.dcpu.DCPUManager;
import be.monfils.x10clone.dcpu.GenericKeyboard;
import be.monfils.x10clone.dcpu.HardwareTracker;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
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
	private DCPUManager dcpuManager;

	public DCPUModel(AssetManager assetManager, Node node, Vector3f position, Quaternion rotation, String file) {
		screen = new Geometry("Screen", new Quad(2.56f, 1.92f));
		Material screen_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		Texture screen_tex = assetManager.loadTexture("Textures/lem1802/boot_transparent.png");
		screen_tex.setMagFilter(Texture.MagFilter.Nearest);
		screen_mat.setTexture("ColorMap", screen_tex);
		screen_mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		screen.setQueueBucket(RenderQueue.Bucket.Transparent);
		screen.setMaterial(screen_mat);
		node.attachChild(screen);
		screen.setLocalTranslation(position);
		screen.setLocalRotation(rotation);

		try {
			byte[] testRam_b = Files.readAllBytes(Paths.get(file));
			char testRam[] = new char[0x10000];
			for(int i = 0; i < 0x10000; ++i) {
				testRam[i] = (char) (testRam_b[i * 2] << 8);
				testRam[i] |= (char) (testRam_b[i * 2 + 1] & 0xFF);
			}
			dcpuManager = new DCPUManager(X10clone.hardwareTracker.requestDCPU(), testRam, 1, 1, 1);
			dcpuManager.startDCPU();
		} catch (IOException e) {
			e.printStackTrace();
		}

		screen.setUserData("Keyboard", dcpuManager.getKeyboards().getFirst().getID());
	}

	public void tick() {
		dcpuManager.tick();
	}

	public void render(AssetManager assetManager) {
		Texture tex = dcpuManager.getLems().getFirst().render();
		Material screenmat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		screenmat.setTexture("ColorMap", tex);
		screenmat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
		screen.setMaterial(screenmat);
	}

	public void stop() {
		dcpuManager.stopDCPU();
	}

	public Spatial getScreen() {
		return screen;
	}
}
