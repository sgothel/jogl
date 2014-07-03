package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * sPLT chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11sPLT
 */
public class PngChunkSPLT extends PngChunkMultiple {
	public final static String ID = ChunkHelper.sPLT;

	// http://www.w3.org/TR/PNG/#11sPLT

	private String palName;
	private int sampledepth; // 8/16
	private int[] palette; // 5 elements per entry

	public PngChunkSPLT(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.BEFORE_IDAT;
	}

	@Override
	public ChunkRaw createRawChunk() {
		try {
			final ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ba.write(palName.getBytes(PngHelperInternal.charsetLatin1));
			ba.write(0); // separator
			ba.write((byte) sampledepth);
			final int nentries = getNentries();
			for (int n = 0; n < nentries; n++) {
				for (int i = 0; i < 4; i++) {
					if (sampledepth == 8)
						PngHelperInternal.writeByte(ba, (byte) palette[n * 5 + i]);
					else
						PngHelperInternal.writeInt2(ba, palette[n * 5 + i]);
				}
				PngHelperInternal.writeInt2(ba, palette[n * 5 + 4]);
			}
			final byte[] b = ba.toByteArray();
			final ChunkRaw chunk = createEmptyChunk(b.length, false);
			chunk.data = b;
			return chunk;
		} catch (final IOException e) {
			throw new PngjException(e);
		}
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		int t = -1;
		for (int i = 0; i < c.data.length; i++) { // look for first zero
			if (c.data[i] == 0) {
				t = i;
				break;
			}
		}
		if (t <= 0 || t > c.data.length - 2)
			throw new PngjException("bad sPLT chunk: no separator found");
		palName = new String(c.data, 0, t, PngHelperInternal.charsetLatin1);
		sampledepth = PngHelperInternal.readInt1fromByte(c.data, t + 1);
		t += 2;
		final int nentries = (c.data.length - t) / (sampledepth == 8 ? 6 : 10);
		palette = new int[nentries * 5];
		int r, g, b, a, f, ne;
		ne = 0;
		for (int i = 0; i < nentries; i++) {
			if (sampledepth == 8) {
				r = PngHelperInternal.readInt1fromByte(c.data, t++);
				g = PngHelperInternal.readInt1fromByte(c.data, t++);
				b = PngHelperInternal.readInt1fromByte(c.data, t++);
				a = PngHelperInternal.readInt1fromByte(c.data, t++);
			} else {
				r = PngHelperInternal.readInt2fromBytes(c.data, t);
				t += 2;
				g = PngHelperInternal.readInt2fromBytes(c.data, t);
				t += 2;
				b = PngHelperInternal.readInt2fromBytes(c.data, t);
				t += 2;
				a = PngHelperInternal.readInt2fromBytes(c.data, t);
				t += 2;
			}
			f = PngHelperInternal.readInt2fromBytes(c.data, t);
			t += 2;
			palette[ne++] = r;
			palette[ne++] = g;
			palette[ne++] = b;
			palette[ne++] = a;
			palette[ne++] = f;
		}
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkSPLT otherx = (PngChunkSPLT) other;
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

	public void setPalName(final String palName) {
		this.palName = palName;
	}

	public int getSampledepth() {
		return sampledepth;
	}

	public void setSampledepth(final int sampledepth) {
		this.sampledepth = sampledepth;
	}

	public int[] getPalette() {
		return palette;
	}

	public void setPalette(final int[] palette) {
		this.palette = palette;
	}

}
