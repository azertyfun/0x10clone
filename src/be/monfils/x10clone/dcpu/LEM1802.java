package be.monfils.x10clone.dcpu;

import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by nathan on 22/07/15.
 */
public class LEM1802 extends DCPUHardware {

	public static final int TYPE = 0x7349f615, REVISION = 0x1802, MANUFACTURER = 0x1c6c8b36;
	public static final int WIDTH_CHARS = 32;
	public static final int HEIGHT_CHARS = 12;
	public static final int WIDTH_PIXELS = 128;
	public static final int HEIGHT_PIXELS = 96;
	private static final int START_DURATION = 60;
	private static final int BORDER_WIDTH = 4;
	private int lightColor;
	private int[] palette = new int[16];
	private char[] font = new char[256];
	private int[] pixels = new int[12289];
	private final static int[] bootImage_raw = new int[12288];
	private final static int[][][] bootImage = new int[128][96][3];
	static {
		try {
			ImageIO.read(new File("assets/Textures/lem1802/boot.png")).getRGB(0, 0, 128, 96, bootImage_raw, 0, 128);
			int pos = 0;
			for(int y = 0; y < 96; ++y) {
				for(int x = 0; x < 128; ++x) {
					bootImage[x][y][0] = bootImage_raw[pos & 0xFF0000];
					bootImage[x][y][1] = bootImage_raw[pos & 0xFF00];
					bootImage[x][y][2] = bootImage_raw[pos & 0xFF];

					pos++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected boolean blinkOn;
	protected int blinkDelay;

	public static final char defaultFont[] = new char[] {
		0x000f, 0x0808, 0x080f, 0x0808, 0x08f8, 0x0808, 0x00ff, 0x0808, 0x0808, 0x0808, 0x08ff, 0x0808, 0x00ff, 0x1414,
		0xff00, 0xff08, 0x1f10, 0x1714, 0xfc04, 0xf414, 0x1710, 0x1714, 0xf404, 0xf414, 0xff00, 0xf714, 0x1414, 0x1414,
		0xf700, 0xf714, 0x1417, 0x1414, 0x0f08, 0x0f08, 0x14f4, 0x1414, 0xf808, 0xf808, 0x0f08, 0x0f08, 0x001f, 0x1414,
		0x00fc, 0x1414, 0xf808, 0xf808, 0xff08, 0xff08, 0x14ff, 0x1414, 0x080f, 0x0000, 0x00f8, 0x0808, 0xffff, 0xffff,
		0xf0f0, 0xf0f0, 0xffff, 0x0000, 0x0000, 0xffff, 0x0f0f, 0x0f0f, 0x0000, 0x0000, 0x005f, 0x0000, 0x0300, 0x0300,
		0x3e14, 0x3e00, 0x266b, 0x3200, 0x611c, 0x4300, 0x3629, 0x7650, 0x0002, 0x0100, 0x1c22, 0x4100, 0x4122, 0x1c00,
		0x2a1c, 0x2a00, 0x083e, 0x0800, 0x4020, 0x0000, 0x0808, 0x0800, 0x0040, 0x0000, 0x601c, 0x0300, 0x3e41, 0x3e00,
		0x427f, 0x4000, 0x6259, 0x4600, 0x2249, 0x3600, 0x0f08, 0x7f00, 0x2745, 0x3900, 0x3e49, 0x3200, 0x6119, 0x0700,
		0x3649, 0x3600, 0x2649, 0x3e00, 0x0024, 0x0000, 0x4024, 0x0000, 0x0814, 0x2241, 0x1414, 0x1400, 0x4122, 0x1408,
		0x0259, 0x0600, 0x3e59, 0x5e00, 0x7e09, 0x7e00, 0x7f49, 0x3600, 0x3e41, 0x2200, 0x7f41, 0x3e00, 0x7f49, 0x4100,
		0x7f09, 0x0100, 0x3e49, 0x3a00, 0x7f08, 0x7f00, 0x417f, 0x4100, 0x2040, 0x3f00, 0x7f0c, 0x7300, 0x7f40, 0x4000,
		0x7f06, 0x7f00, 0x7f01, 0x7e00, 0x3e41, 0x3e00, 0x7f09, 0x0600, 0x3e41, 0xbe00, 0x7f09, 0x7600, 0x2649, 0x3200,
		0x017f, 0x0100, 0x7f40, 0x7f00, 0x1f60, 0x1f00, 0x7f30, 0x7f00, 0x7708, 0x7700, 0x0778, 0x0700, 0x7149, 0x4700,
		0x007f, 0x4100, 0x031c, 0x6000, 0x0041, 0x7f00, 0x0201, 0x0200, 0x8080, 0x8000, 0x0001, 0x0200, 0x2454, 0x7800,
		0x7f44, 0x3800, 0x3844, 0x2800, 0x3844, 0x7f00, 0x3854, 0x5800, 0x087e, 0x0900, 0x4854, 0x3c00, 0x7f04, 0x7800,
		0x447d, 0x4000, 0x2040, 0x3d00, 0x7f10, 0x6c00, 0x417f, 0x4000, 0x7c18, 0x7c00, 0x7c04, 0x7800, 0x3844, 0x3800,
		0x7c14, 0x0800, 0x0814, 0x7c00, 0x7c04, 0x0800, 0x4854, 0x2400, 0x043e, 0x4400, 0x3c40, 0x7c00, 0x1c60, 0x1c00,
		0x7c30, 0x7c00, 0x6c10, 0x6c00, 0x4c50, 0x3c00, 0x6454, 0x4c00, 0x0836, 0x4100, 0x0077, 0x0000, 0x4136, 0x0800,
		0x0201, 0x0201, 0x704c, 0x7000
	};
	public static final char defaultPalette[] = new char[] {
		0x000, 0x00a, 0x0a0, 0x0aa, 0xa00, 0xa0a, 0xa50, 0xaaa, 0x555, 0x55f, 0x5f5, 0x5ff, 0xf55, 0xf5f, 0xff5, 0xfff
	};

	private int screenMemMap, fontMemMap, paletteMemMap, startDelay;
	private char borderColor[] = new char[3];

	public LEM1802(String id) {
		super(TYPE, REVISION, MANUFACTURER);
		this.id = id;
	}

	public Texture render() {
		ByteBuffer data = ByteBuffer.allocateDirect((128 + 2 * BORDER_WIDTH) * (96 + 2 * BORDER_WIDTH) * 4);

		if(screenMemMap != 0 && startDelay == 0) {
			/*
			 * This ram to texture algorithm is heavily inspired from mappum's in his javascript emulator. Check it out there : https://github.com/mappum/DCPU-16/blob/master/lib/LEM1802.js
			 */
			char colorBuffer2D[][][] = new char[128 + 2 * BORDER_WIDTH][96 + 2 * BORDER_WIDTH][3];
			char buffer[] = new char[(128 + 2 * BORDER_WIDTH) * (96 + 2 * BORDER_WIDTH) * 4];
			int pos = 0;
			for(int y = 0; y < 12; ++y) {
				for(int x = 0; x < 32; ++x) {
					if(dcpu.ram[(screenMemMap + pos) & 0xFFFF] != 0) {
						char fgCol = (char) ((dcpu.ram[(screenMemMap + pos) & 0xFFFF] & 0xF000) >> 12);
						char bgCol = (char) ((dcpu.ram[(screenMemMap + pos) & 0xFFFF] & 0xF00) >> 8);
						boolean blink = ((dcpu.ram[(screenMemMap + pos & 0xFFFF)] & 0x80) >> 7) == 1; //TODO : Check if that actually works
						char character = (char) (dcpu.ram[(screenMemMap + pos) & 0xFFFF] & 0x7F);
						char fontChar[] = new char[] {font[character * 2], font[character * 2 + 1]};

						if(!blink || !blinkOn) {
							for(int i = 0; i < 4; ++i) {
								int word = fontChar[((i >= 2) ? 1 : 0) * 1]; //java, plz, why can't you cast boolean to int ?
								int hword = (word >> (((i % 2) == 0 ? 1 : 0) * 8)) & 0xFF; //plz java plz
								for(int j = 0; j < 8; ++j) {
									int pixel = (hword >> j) & 1;
									int px = BORDER_WIDTH + x * 4 + i;
									int py = BORDER_WIDTH + y * 8 + j;
									if(pixel == 1) {
										colorBuffer2D[px][py][0] = (char) (((palette[fgCol] & 0xF00) >> 4) | 0xF);
										colorBuffer2D[px][py][1] = (char) ((palette[fgCol] & 0xF0) | 0xF);
										colorBuffer2D[px][py][2] = (char) (((palette[fgCol] & 0xF) << 4) | 0xF);
									} else {
										colorBuffer2D[px][py][0] = (char) (((palette[bgCol] & 0xF00) >> 4) | 0xF);
										colorBuffer2D[px][py][1] = (char) ((palette[bgCol] & 0xF0) | 0xF);
										colorBuffer2D[px][py][2] = (char) (((palette[bgCol] & 0xF) << 4) | 0xF);
									}
								}
							}
						}
					}
					pos++;
				}
			}
			pos = 0;
			for(int y = 95 + 2 * BORDER_WIDTH; y >=0 ; --y) {
				for(int x = 0; x < 128 + 2 * BORDER_WIDTH; ++x) {
					if(y < BORDER_WIDTH || (y < 96 + 2 * BORDER_WIDTH && y >= 96 + BORDER_WIDTH)) {
						buffer[pos * 4] = borderColor[0];
						buffer[pos * 4 + 1] = borderColor[1];
						buffer[pos * 4 + 2] = borderColor[2];
						buffer[pos * 4 + 3] = 128;
					} else if(x < BORDER_WIDTH || (x < 128 + 2 * BORDER_WIDTH && x >= 128 + BORDER_WIDTH)) {
						buffer[pos * 4] = borderColor[0];
						buffer[pos * 4 + 1] = borderColor[1];
						buffer[pos * 4 + 2] = borderColor[2];
						buffer[pos * 4 + 3] = 128;
					} else {
						buffer[pos * 4] = colorBuffer2D[x][y][0];
						buffer[pos * 4 + 1] = colorBuffer2D[x][y][1];
						buffer[pos * 4 + 2] = colorBuffer2D[x][y][2];
						buffer[pos * 4 + 3] = 128;
					}
					pos++;
				}
			}
			byte[] buffer_b = new byte[buffer.length];
			for(int i = 0; i < buffer.length; ++i) {
				buffer_b[i] = (byte) buffer[i];
			}
			data.put(buffer_b);
		} else {
			char buffer[] = new char[(128 + 2 * BORDER_WIDTH) * (96 + 2 * BORDER_WIDTH) * 4];
			int pos = 0;
			for(int y = 0; y < 96 + 2 * BORDER_WIDTH; ++y) {
				for(int x = 0; x < 128 + 2 * BORDER_WIDTH; ++x) {
					if(y < BORDER_WIDTH || (y < 96 + 2 * BORDER_WIDTH && y >= 96 + BORDER_WIDTH)) {
						buffer[pos * 4] = 0;
						buffer[pos * 4 + 1] = 0;
						buffer[pos * 4 + 2] = 0;
						buffer[pos * 4 + 3] = 128;
					} else if(x < BORDER_WIDTH || (x < 128 + 2 * BORDER_WIDTH && x >= 128 +BORDER_WIDTH)) {
						buffer[pos * 4] = 0;
						buffer[pos * 4 + 1] = 0;
						buffer[pos * 4 + 2] = 0;
						buffer[pos * 4 + 3] = 128;
					} else {
						buffer[pos * 4] = (char) bootImage[x - BORDER_WIDTH][y - BORDER_WIDTH][0];
						buffer[pos * 4 + 1] = (char) bootImage[x - BORDER_WIDTH][y - BORDER_WIDTH][1];
						buffer[pos * 4 + 2] = (char) bootImage[x - BORDER_WIDTH][y - BORDER_WIDTH][2];
						buffer[pos * 4 + 3] = 128;
					}
					pos++;
				}
			}
		}

		Texture tex = new Texture2D();
		tex.setImage(new Image(Image.Format.RGBA8, WIDTH_PIXELS + 2 * BORDER_WIDTH, HEIGHT_PIXELS + 2 * BORDER_WIDTH, data));
		tex.setMagFilter(Texture.MagFilter.Nearest);
		return tex;
	}

	public void interrupt() {
		int a = dcpu.registers[0];
		int offset = 0;
		switch(a) {
			case 0: //MEM_MAP_SCREEN
				if(screenMemMap == 0 && dcpu.registers[1] != 0) {
					startDelay = START_DURATION;
				}
				screenMemMap = dcpu.registers[1];
				break;
			case 1: //MEM_MAP_FONT
				fontMemMap = dcpu.registers[1];
				break;
			case 2: //MEM_MAP_PALETTE
				paletteMemMap = dcpu.registers[1];
				loadPalette(dcpu.ram, paletteMemMap);
				break;
			case 3: //SET_BORDER_COLOR
				int col = dcpu.registers[1] & 0xF;
				borderColor[0] = (char) (((palette[col] & 0xF00) >> 4) | 0xF);
				borderColor[1] = (char) ((palette[col] & 0xF0) | 0xF);
				borderColor[2] = (char) (((palette[col] & 0xF) << 4) | 0xF);
				break;
			case 4: //MEM_DUMP_FONT
				offset = dcpu.registers[1];
				for(int i = 0; i < font.length; ++i) {
					dcpu.ram[offset + i & 0xFFFF] = defaultFont[i];
				}
				dcpu.cycles += 256;
				break;
			case 5: //MEM_DUMP_PALETTE
				offset = dcpu.registers[1];
				for(int i = 0; i < 16; ++i) {
					dcpu.ram[offset + i & 0xFFFF] = defaultPalette[i];
				}
				dcpu.cycles += 16;
		}
	}

	protected void loadPalette(char[] ram, int offset) {
		for(int i = 0; i < 16; ++i) {
			char ch = ram[(offset + i)];
			int b = (ch >> '\000' & 0xF) * 17;
			int g = (ch >> '\004' & 0xF) * 17;
			int r = (ch >> '\b' & 0xF) * 17;
			palette[i] = (0xFF000000 | r << 16 | g << 8 | b);
		}
	}

	@Override
	public void tick60hz() {
		if(startDelay > 0)
			startDelay -= 1;
		if(blinkDelay > 0)
			blinkDelay -= 1;
		if(blinkDelay == 0) {
			blinkOn = !blinkOn;
			blinkDelay = 30;
		}
	}

	@Override
	public void powerOff() {
		lightColor = 0;
		screenMemMap = 0;
		fontMemMap = 0;
		borderColor = new char[3];
		startDelay = 0;
		resetPalette();
		resetFont();
		resetPixels();
	}

	@Override
	public void powerOn() {
		resetPalette();
		resetFont();
		resetPixels();
	}

	private void resetFont() {
		for(int i = 0; i < 256; ++i)
			font[i] = defaultFont[i];
	}

	private void resetPalette() {
		for(int i =  0 ; i < 16; ++i) {
			palette[i] = defaultPalette[i];
		}
	}

	private void resetPixels() {
		for(int i = 0; i < pixels.length; ++i) {
			pixels[i] = 0;
		}
	}

}
