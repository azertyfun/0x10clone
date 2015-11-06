package be.monfils.x10clone.networking;

import be.monfils.x10clone.SceneDescriptor;
import be.monfils.x10clone.dcpu.DCPUModel;
import be.monfils.x10clone.messages.*;
import be.monfils.x10clone.rendering.X10clone;
import com.jme3.network.Client;
import com.jme3.network.Message;
import com.jme3.network.MessageListener;

import java.io.IOException;
import java.util.concurrent.Callable;

/**
 * Created by nathan on 3/11/15.
 */
public class ClientListener implements MessageListener<Client> {

	private final X10clone client;

	public ClientListener(X10clone client) {
		this.client = client;
	}

	@Override
	public void messageReceived(Client client, Message message) {
		if(message instanceof MessageChangeUsername) {
			MessageChangeUsername messageChangeUsername = (MessageChangeUsername) message;
			if(messageChangeUsername.isAccepted()) {
				this.client.setUsername(messageChangeUsername.getUsername());
			} else {
				System.out.println("Username change denied. Maybe there is someone else using this username?");
			}
		} else if(message instanceof MessageLoadScene) {
			MessageLoadScene messageLoadScene = (MessageLoadScene) message;
			try {
				this.client.loadScene(SceneDescriptor.loadJSON(messageLoadScene.getScene_json()));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			System.out.println("Loading scene...");
		} else if(message instanceof MessagePlayerLocation) {
			while(this.client.isLoadingScene()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			MessagePlayerLocation messagePlayerLocation = (MessagePlayerLocation) message;
			this.client.setPlayerPosition(messagePlayerLocation.getId(), messagePlayerLocation.getPosition(), messagePlayerLocation.getRotation());
		} else if(message instanceof MessageSpawnDCPU) {
			while(this.client.isLoadingScene()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			MessageSpawnDCPU messageSpawnDCPU = (MessageSpawnDCPU) message;
			this.client.addDCPU(messageSpawnDCPU.getPosition(), messageSpawnDCPU.getRotation(), messageSpawnDCPU.getScale(), messageSpawnDCPU.getId());

			for(DCPUModel dcpuModel : this.client.getDcpuModels()) {
				if(dcpuModel.getId() == messageSpawnDCPU.getId()) {
					X10clone _client = this.client;
					_client.enqueue(new Callable<Object>() {
						@Override
						public Object call() throws Exception {
							dcpuModel.setupSound(_client.getAssetManager());
							return null;
						}
					});
					break;
				}
			}
		} else if(message instanceof MessageDCPUScreen) {
			while(this.client.isLoadingScene()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			MessageDCPUScreen messageDCPUScreen = (MessageDCPUScreen) message;
			for(DCPUModel dcpuModel : this.client.getDcpuModels()) {
				if(dcpuModel.getId() == messageDCPUScreen.getId()) {
					dcpuModel.setVideoRam(messageDCPUScreen.getVideoRam());
					dcpuModel.setFontRam(messageDCPUScreen.getFontRam());
					dcpuModel.setPaletteRam(messageDCPUScreen.getPaletteRam());
					dcpuModel.setBorderColor(messageDCPUScreen.getBorderColor());
					dcpuModel.setUseGivenBuffers(true);
					break;
				}
			}
		} else if(message instanceof  MessageUpdateVSSSound) {
			while(this.client.isLoadingScene()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			MessageUpdateVSSSound messageUpdateVSSSound = (MessageUpdateVSSSound) message;
			for(DCPUModel dcpuModel : this.client.getDcpuModels()) {
				if(dcpuModel.getId() == messageUpdateVSSSound.getId()) {
					X10clone _client = this.client;
					_client.enqueue(new Callable<Object>() {
						@Override
						public Object call() throws Exception {
							dcpuModel.updateSound(messageUpdateVSSSound.getPitch(), messageUpdateVSSSound.getVolume());
							return null;
						}
					});
					break;
				}
			}
		} else if(message instanceof MessageSpawnPlayer) {
			while(this.client.isLoadingScene()) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			MessageSpawnPlayer messageSpawnPlayer = (MessageSpawnPlayer) message;
			if(messageSpawnPlayer.isRemoval())
				this.client.removePlayer(messageSpawnPlayer.getId());
			else
				this.client.addPlayer(messageSpawnPlayer.getId(), messageSpawnPlayer.getPosition());
		}
	}
}
