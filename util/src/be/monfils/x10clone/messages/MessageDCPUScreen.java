package be.monfils.x10clone.messages;

import com.jme3.network.AbstractMessage;
import com.jme3.network.serializing.Serializable;

/**
 * Created by nathan on 4/11/15.
 */

@Serializable
public class MessageDCPUScreen extends AbstractMessage {
	private int id;
	private char borderColor;
	private char[] fontRam;
	private char[] paletteRam;
	char[] videoRam;

	public MessageDCPUScreen() {
		super(false);

		videoRam = new char[384];
		fontRam = new char[256];
		paletteRam = new char[16];
		id = 0;
	}

	public MessageDCPUScreen(int id, char borderColor, char[] videoRam, char[] paletteRam, char[] fontRam) {
		super(false); //TODO : Check if it works with a lot of network problems

		this.id = id;
		this.borderColor = borderColor;
		this.videoRam = videoRam;
		this.paletteRam = paletteRam;
		this.fontRam = fontRam;
	}

	public int getId() {
		return id;
	}

	public char getBorderColor() {
		return borderColor;
	}

	public char[] getFontRam() {
		return fontRam;
	}

	public char[] getPaletteRam() {
		return paletteRam;
	}

	public char[] getVideoRam() {
		return videoRam;
	}
}
