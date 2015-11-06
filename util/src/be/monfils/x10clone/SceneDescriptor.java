package be.monfils.x10clone;

import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;

/**
 * Created by nathan on 3/11/15.
 */
public class SceneDescriptor {

	private Vector3f player_position;
	private LinkedList<Light> lights;
	private String scene_file, skybox_file;

	public Vector3f getPlayer_position() {
		return player_position;
	}

	public LinkedList<Light> getLights() {
		return lights;
	}

	public String getScene_file() {
		return scene_file;
	}

	public String getSkybox_file() {
		return skybox_file;
	}

	public static SceneDescriptor loadJSON(String path) throws IOException {
		SceneDescriptor s = new SceneDescriptor();

		String file = new String(Files.readAllBytes(Paths.get(path)));
		JSONObject scene_json = new JSONObject(file);
		JSONObject scene_files_json = scene_json.getJSONObject("files");
		s.scene_file = scene_files_json.getString("scene");
		s.skybox_file = scene_files_json.getString("skybox");
		JSONArray player_position_json = scene_json.getJSONArray("player_position");
		s.player_position = new Vector3f((float) player_position_json.getDouble(0), (float) player_position_json.getDouble(1), (float) player_position_json.getDouble(2));

		s.lights = new LinkedList<>();

		JSONArray lighting_json = scene_json.getJSONArray("lighting");
		for(int i = 0; i < lighting_json.length(); ++i) {
			JSONObject light = lighting_json.getJSONObject(i);
			String light_type = light.getString("type");
			JSONArray light_color_json = light.getJSONArray("color");
			ColorRGBA color = new ColorRGBA((float) light_color_json.getDouble(0), (float) light_color_json.getDouble(1), (float) light_color_json.getDouble(2), (float) light_color_json.getDouble(3));
			float color_multiplier = (float) light.getDouble("color_multiplier");
			if(light_type.equalsIgnoreCase("PointLight")) {
				JSONArray light_position_json = light.getJSONArray("position");
				Vector3f position = new Vector3f((float) light_position_json.getDouble(0), (float) light_position_json.getDouble(1), (float) light_position_json.getDouble(2));

				PointLight pl = new PointLight();
				pl.setColor(color.mult(color_multiplier));
				pl.setPosition(position);
				s.lights.add(pl);
			} else if(light_type.equalsIgnoreCase("AmbientLight")) {
				AmbientLight al = new AmbientLight();
				al.setColor(color.mult(color_multiplier));
				s.lights.add(al);
			} else if(light_type.equalsIgnoreCase("DirectionalLight")) {
				JSONArray light_direction_json = light.getJSONArray("direction");
				Vector3f direction = new Vector3f((float) light_direction_json.getDouble(0), (float) light_direction_json.getDouble(1), (float) light_direction_json.getDouble(2));

				DirectionalLight dl = new DirectionalLight();
				dl.setColor(color.mult(color_multiplier));
				dl.setDirection(direction);
				s.lights.add(dl);
			}
		}

		return s;
	}
}
