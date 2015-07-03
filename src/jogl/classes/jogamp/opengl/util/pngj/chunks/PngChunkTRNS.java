package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * tRNS chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11tRNS
 * <p>
 * this chunk structure depends on the image type
 */
public class PngChunkTRNS extends PngChunkSingle {
	public final static String ID = ChunkHelper.tRNS;

	// http://www.w3.org/TR/PNG/#11tRNS

	// only one of these is meaningful, depending on the image type
	private int gray;
	private int red, green, blue;
	private int[] paletteAlpha = new int[] {};

	public PngChunkTRNS(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.AFTER_PLTE_BEFORE_IDAT;
	}

	@Override
	public ChunkRaw createRawChunk() {
		ChunkRaw c = null;
		if (imgInfo.greyscale) {
			c = createEmptyChunk(2, true);
			PngHelperInternal.writeInt2tobytes(gray, c.data, 0);
		} else if (imgInfo.indexed) {
			c = createEmptyChunk(paletteAlpha.length, true);
			for (int n = 0; n < c.len; n++) {
				c.data[n] = (byte) paletteAlpha[n];
			}
		} else {
			c = createEmptyChunk(6, true);
			PngHelperInternal.writeInt2tobytes(red, c.data, 0);
			PngHelperInternal.writeInt2tobytes(green, c.data, 0);
			PngHelperInternal.writeInt2tobytes(blue, c.data, 0);
		}
		return c;
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		if (imgInfo.greyscale) {
			gray = PngHelperInternal.readInt2fromBytes(c.data, 0);
		} else if (imgInfo.indexed) {
			final int nentries = c.data.length;
			paletteAlpha = new int[nentries];
			for (int n = 0; n < nentries; n++) {
				paletteAlpha[n] = c.data[n] & 0xff;
			}
		} else {
			red = PngHelperInternal.readInt2fromBytes(c.data, 0);
			green = PngHelperInternal.readInt2fromBytes(c.data, 2);
			blue = PngHelperInternal.readInt2fromBytes(c.data, 4);
		}
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkTRNS otherx = (PngChunkTRNS) other;
		gray = otherx.gray;
		red = otherx.red;
		green = otherx.green;
		blue = otherx.blue;
		if (otherx.paletteAlpha != null) {
			paletteAlpha = new int[otherx.paletteAlpha.length];
			System.arraycopy(otherx.paletteAlpha, 0, paletteAlpha, 0, paletteAlpha.length);
		}
	}

	/**
	 * Set rgb values
	 *
	 */
	public void setRGB(final int r, final int g, final int b) {
		if (imgInfo.greyscale || imgInfo.indexed)
			throw new PngjException("only rgb or rgba images support this");
		red = r;
		green = g;
		blue = b;
	}

	public int[] getRGB() {
		if (imgInfo.greyscale || imgInfo.indexed)
			throw new PngjException("only rgb or rgba images support this");
		return new int[] { red, green, blue };
	}

	public void setGray(final int g) {
		if (!imgInfo.greyscale)
			throw new PngjException("only grayscale images support this");
		gray = g;
	}

	public int getGray() {
		if (!imgInfo.greyscale)
			throw new PngjException("only grayscale images support this");
		return gray;
	}

	/**
	 * WARNING: non deep copy
	 */
	public void setPalletteAlpha(final int[] palAlpha) {
		if (!imgInfo.indexed)
			throw new PngjException("only indexed images support this");
		paletteAlpha = palAlpha;
	}

	/**
	 * to use when only one pallete index is set as totally transparent
	 */
	public void setIndexEntryAsTransparent(final int palAlphaIndex) {
		if (!imgInfo.indexed)
			throw new PngjException("only indexed images support this");
		paletteAlpha = new int[] { palAlphaIndex + 1 };
		for (int i = 0; i < palAlphaIndex; i++)
			paletteAlpha[i] = 255;
		paletteAlpha[palAlphaIndex] = 0;
	}

	/**
	 * WARNING: non deep copy
	 */
	public int[] getPalletteAlpha() {
		return paletteAlpha;
	}

}
