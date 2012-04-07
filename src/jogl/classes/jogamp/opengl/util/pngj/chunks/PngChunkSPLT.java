package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjException;


public class PngChunkSPLT extends PngChunk {
	// http://www.w3.org/TR/PNG/#11sPLT

	private String palName;
	private int sampledepth; // 8/16
	private int[] palette; // 5 elements per entry

	public PngChunkSPLT(ImageInfo info) {
		super(ChunkHelper.sPLT, info);
	}

	@Override
	public boolean allowsMultiple() {
		return true; // allows multiple, but pallete name should be different
	}

	@Override
	public boolean mustGoBeforeIDAT() {
		return true;
	}

	@Override
	public ChunkRaw createChunk() {
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ba.write(palName.getBytes(PngHelper.charsetLatin1));
			ba.write(0); // separator
			ba.write((byte) sampledepth);
			int nentries = getNentries();
			for (int n = 0; n < nentries; n++) {
				for (int i = 0; i < 4; i++) {
					if (sampledepth == 8)
						PngHelper.writeByte(ba, (byte) palette[n * 5 + i]);
					else
						PngHelper.writeInt2(ba, palette[n * 5 + i]);
				}
				PngHelper.writeInt2(ba, palette[n * 5 + 4]);
			}
			byte[] b = ba.toByteArray();
			ChunkRaw chunk = createEmptyChunk(b.length, false);
			chunk.data = b;
			return chunk;
		} catch (IOException e) {
			throw new PngjException(e);
		}
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		int t = -1;
		for (int i = 0; i < c.data.length; i++) { // look for first zero
			if (c.data[i] == 0) {
				t = i;
				break;
			}
		}
		if (t <= 0 || t > c.data.length - 2)
			throw new PngjException("bad sPLT chunk: no separator found");
		palName = new String(c.data, 0, t, PngHelper.charsetLatin1);
		sampledepth = PngHelper.readInt1fromByte(c.data, t + 1);
		t += 2;
		int nentries = (c.data.length - t) / (sampledepth == 8 ? 6 : 10);
		palette = new int[nentries * 5];
		int r, g, b, a, f, ne;
		ne = 0;
		for (int i = 0; i < nentries; i++) {
			if (sampledepth == 8) {
				r = PngHelper.readInt1fromByte(c.data, t++);
				g = PngHelper.readInt1fromByte(c.data, t++);
				b = PngHelper.readInt1fromByte(c.data, t++);
				a = PngHelper.readInt1fromByte(c.data, t++);
			} else {
				r = PngHelper.readInt2fromBytes(c.data, t);
				t += 2;
				g = PngHelper.readInt2fromBytes(c.data, t);
				t += 2;
				b = PngHelper.readInt2fromBytes(c.data, t);
				t += 2;
				a = PngHelper.readInt2fromBytes(c.data, t);
				t += 2;
			}
			f = PngHelper.readInt2fromBytes(c.data, t);
			t += 2;
			palette[ne++] = r;
			palette[ne++] = g;
			palette[ne++] = b;
			palette[ne++] = a;
			palette[ne++] = f;
		}
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkSPLT otherx = (PngChunkSPLT) other;
		palName = otherx.palName;
		sampledepth = otherx.sampledepth;
		palette = new int[otherx.palette.length];
		System.arraycopy(otherx.palette, 0, palette, 0, palette.length);
	}

	public int getNentries() {
		return palette.length / 5;
	}

	public String getPalName() {
		return palName;
	}

	public void setPalName(String palName) {
		this.palName = palName;
	}

	public int getSampledepth() {
		return sampledepth;
	}

	public void setSampledepth(int sampledepth) {
		this.sampledepth = sampledepth;
	}

	public int[] getPalette() {
		return palette;
	}

	public void setPalette(int[] palette) {
		this.palette = palette;
	}

}
