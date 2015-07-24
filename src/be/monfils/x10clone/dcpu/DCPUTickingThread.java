package be.monfils.x10clone.dcpu;

import be.monfils.x10clone.rendering.DCPUModel;
import be.monfils.x10clone.rendering.X10clone;
import com.jme3.asset.AssetManager;

import java.util.LinkedList;

/**
 * Created by nathan on 24/07/15.
 */
public class DCPUTickingThread extends Thread {
	private AssetManager assetManager;
	private LinkedList<DCPUModel> dcpus = new LinkedList<>();
	private boolean stopped;

	public DCPUTickingThread(LinkedList<DCPUModel> dcpus, AssetManager assetManager) {
		this.dcpus = dcpus;
		this.assetManager = assetManager;
	}

	public void setStopped() {
		stopped = true;
	}

	@Override
	public void run() {
		long expectedTime = (long) ((1.0f/60.0f) * 1000);
		while(!stopped) {
			long start = System.currentTimeMillis();
			synchronized (dcpus) {
				for (DCPUModel dcpu : dcpus) {
					dcpu.tick();
					dcpu.render(assetManager);
				}
			}
			long end = System.currentTimeMillis();

			if(expectedTime - (end - start) > 0) {
				try {
					Thread.sleep(expectedTime - (end - start));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
