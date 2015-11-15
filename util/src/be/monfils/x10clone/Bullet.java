package be.monfils.x10clone;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;

/**
 * Created by nathan on 15/11/15.
 */
public class Bullet extends AbstractAppState implements PhysicsCollisionListener {
	private Node node;
	private Vector3f location, direction;
	private Quaternion rotation;
	private ColorRGBA color;

	private BulletAppState bulletAppState;
	private RigidBodyControl rbc;
	private PointLight light;
	private Spatial bullet;

	public Bullet(Node node, Vector3f location, Vector3f direction, Quaternion rotation, ColorRGBA color, BulletAppState bulletAppState) {
		this.node = node;
		this.location = location;
		this.direction = direction;
		this.rotation = rotation;
		this.bulletAppState = bulletAppState;
		this.color = color;
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		bullet = new Geometry("bullet", new Cylinder(5, 5, 0.01f, 0.5f, true));
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", new ColorRGBA(0, 1, 0, 1));
		bullet.setMaterial(mat);
		node.attachChild(bullet);

		bullet.setLocalTranslation(location.add(direction));
		rbc = new RigidBodyControl(CollisionShapeFactory.createBoxShape(bullet));
		rbc.setFriction(0);
		rbc.setLinearVelocity(direction.mult(20));
		bullet.addControl(rbc);
		bulletAppState.getPhysicsSpace().add(rbc);
		bulletAppState.getPhysicsSpace().addCollisionListener(this);

		rbc.setPhysicsRotation(rotation);
		rbc.setGravity(new Vector3f(0, 0, 0));

		light = new PointLight();
		light.setColor(color);
		light.setPosition(location);
		light.setRadius(3f);
		node.addLight(light);
	}

	@Override
	public void update(float tpf) {
		light.setPosition(rbc.getPhysicsLocation());
	}

	@Override
	public void collision(PhysicsCollisionEvent physicsCollisionEvent) {
		if(physicsCollisionEvent.getNodeA().getName().equals("bullet")) {
			Spatial bullet = physicsCollisionEvent.getNodeA();
			if(physicsCollisionEvent.getNodeB() != null && physicsCollisionEvent.getNodeB().getName() != null && physicsCollisionEvent.getNodeB().getName().equals("player")) {

			}
			node.detachChild(bullet);
			node.removeLight(light);
		} else if(physicsCollisionEvent.getNodeB().getName().equals("bullet")) {
			Spatial bullet = physicsCollisionEvent.getNodeB();
			if(physicsCollisionEvent.getNodeA() != null && physicsCollisionEvent.getNodeA().getName() != null && physicsCollisionEvent.getNodeA().getName().equals("player")) {

			}
			node.detachChild(bullet);
			node.removeLight(light);
		}
	}
}
