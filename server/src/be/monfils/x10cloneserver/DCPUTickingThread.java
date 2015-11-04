package be.monfils.x10cloneserver;

import be.monfils.x10clone.dcpu.DCPUModel;
import be.monfils.x10clone.messages.MessageDCPUScreen;
import com.jme3.asset.AssetManager;
import com.jme3.network.HostedConnection;
import com.jme3.network.kernel.KernelException;

import java.util.LinkedList;

/**
 * Created by nathan on 24/07/15.
 */
public class DCPUTickingThread extends Thread {
	private final X10cloneServer server;
	private AssetManager assetManager;
	private LinkedList<DCPUModel> dcpuModels = new LinkedList<>();
	private boolean stopped;

	public DCPUTickingThread(X10cloneServer server, LinkedList<DCPUModel> dcpuModels, AssetManager assetManager) {
		this.dcpuModels = dcpuModels;
		this.assetManager = assetManager;
		this.server = server;
	}

	public void setStopped() {
		stopped = true;
	}

	@Override
	public void run() {
		long expectedTime = (long) ((1.0f/60.0f) * 1000);
		while(!stopped) {
			long start = System.currentTimeMillis();
			synchronized (dcpuModels) {
				for (DCPUModel dcpuModel : dcpuModels) {
					dcpuModel.tick();
					dcpuModel.render(assetManager);
					for(HostedConnection client : server.getClients()) {
						try {
							client.send(new MessageDCPUScreen(dcpuModel.getId(), dcpuModel.getVideoRam(), dcpuModel.getPaletteRam(), dcpuModel.getFontRam()));
						} catch(KernelException e) {
							System.err.println("Could not send screen info to client because UDP endpoint is disconnected.\nError message: " + e.getLocalizedMessage());
						}
					}
					dcpuModel.sound();
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

	public boolean isStopped() {
		return stopped;
	}
}
