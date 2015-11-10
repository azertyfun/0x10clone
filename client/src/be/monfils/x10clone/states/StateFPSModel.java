package be.monfils.x10clone.states;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

/**
 * Created by nathan on 10/11/15.
 */
public class StateFPSModel extends AbstractAppState {
	private Node rootNode;
	private Camera camera;
	private ViewPort viewPort;
	private Spatial hand;

	private final Vector3f sunDirection;
	private DirectionalLight sun;
	private AmbientLight ambientLight;

	private Vector3f oldLocation = new Vector3f();
	private Vector3f location = new Vector3f();

	private float offsetTime = 0;

	public StateFPSModel(Vector3f sunDirection) {
		this.sunDirection = sunDirection;
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		rootNode = new Node("FPS Model root node");
		rootNode.setCullHint(Spatial.CullHint.Never);

		Camera originalCam = app.getCamera();
		camera = new Camera(originalCam.getWidth(), originalCam.getHeight());
		camera.setFrustumPerspective(70, (float) camera.getWidth() / (float) camera.getHeight(), 0.001f, 5f);
		camera.lookAtDirection(new Vector3f(0, 0, -1), new Vector3f(0, 1, 0));

		viewPort = app.getRenderManager().createMainView("FPS Model viewport", camera);
		viewPort.setEnabled(true);
		viewPort.setClearFlags(false, true, false);
		viewPort.attachScene(rootNode);

		hand = app.getAssetManager().loadModel("Models/Hand/Hand.j3o");
		hand.setMaterial(new Material(app.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md"));
		hand.setLocalTranslation(0.06f, -0.06f, -0.1f);
		hand.setLocalScale(0.4f);
		hand.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.PI / 4f, new Vector3f(1, 0, 0)));
		rootNode.attachChild(hand);

		sun = new DirectionalLight();
		sun.setDirection(sunDirection);
		rootNode.addLight(sun);

		ambientLight = new AmbientLight();
		ambientLight.setColor(ColorRGBA.White.mult(1.3f));
		rootNode.addLight(ambientLight);

		rootNode.updateLogicalState(1);
		rootNode.updateGeometricState();
	}

	@Override
	public void update(float tpf) {
		float distance = oldLocation.distance(location);
		if(distance > tpf / 100f) { //if we moved
			offsetTime += tpf * distance * 50;
			hand.setLocalTranslation(0.06f, -0.06f + FastMath.sin(offsetTime * 2f) / 100f, -0.1f);
		}

		rootNode.updateLogicalState(tpf);
	}

	@Override
	public void render(RenderManager rm) {
		rootNode.updateGeometricState();
	}

	public void updateLocation(Vector3f location) {
		this.oldLocation = this.location.clone();
		this.location = location.clone();
	}

	public void updateDirection(Vector3f direction) {
		if(isInitialized())
			sun.setDirection(direction.mult(sunDirection).normalize());
	}

	public Node getRootNode() {
		return rootNode;
	}

	public Camera getCamera() {
		return camera;
	}
}
