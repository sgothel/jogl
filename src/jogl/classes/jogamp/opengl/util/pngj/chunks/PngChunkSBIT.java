package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * sBIT chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11sBIT
 * <p>
 * this chunk structure depends on the image type
 */
public class PngChunkSBIT extends PngChunkSingle {
	public final static String ID = ChunkHelper.sBIT;
	// http://www.w3.org/TR/PNG/#11sBIT

	// significant bits
	private int graysb, alphasb;
	private int redsb, greensb, bluesb;

	public PngChunkSBIT(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.BEFORE_PLTE_AND_IDAT;
	}

	private int getLen() {
		int len = imgInfo.greyscale ? 1 : 3;
		if (imgInfo.alpha)
			len += 1;
		return len;
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		if (c.len != getLen())
			throw new PngjException("bad chunk length " + c);
		if (imgInfo.greyscale) {
			graysb = PngHelperInternal.readInt1fromByte(c.data, 0);
			if (imgInfo.alpha)
				alphasb = PngHelperInternal.readInt1fromByte(c.data, 1);
		} else {
			redsb = PngHelperInternal.readInt1fromByte(c.data, 0);
			greensb = PngHelperInternal.readInt1fromByte(c.data, 1);
			bluesb = PngHelperInternal.readInt1fromByte(c.data, 2);
			if (imgInfo.alpha)
				alphasb = PngHelperInternal.readInt1fromByte(c.data, 3);
		}
	}

	@Override
	public ChunkRaw createRawChunk() {
		ChunkRaw c = null;
		c = createEmptyChunk(getLen(), true);
		if (imgInfo.greyscale) {
			c.data[0] = (byte) graysb;
			if (imgInfo.alpha)
				c.data[1] = (byte) alphasb;
		} else {
			c.data[0] = (byte) redsb;
			c.data[1] = (byte) greensb;
			c.data[2] = (byte) bluesb;
			if (imgInfo.alpha)
				c.data[3] = (byte) alphasb;
		}
		return c;
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkSBIT otherx = (PngChunkSBIT) other;
		graysb = otherx.graysb;
		redsb = otherx.redsb;
		greensb = otherx.greensb;
		bluesb = otherx.bluesb;
		alphasb = otherx.alphasb;
	}

	public void setGraysb(final int gray) {
		if (!imgInfo.greyscale)
			throw new PngjException("only greyscale images support this");
		graysb = gray;
	}

	public int getGraysb() {
		if (!imgInfo.greyscale)
			throw new PngjException("only greyscale images support this");
		return graysb;
	}

	public void setAlphasb(final int a) {
		if (!imgInfo.alpha)
			throw new PngjException("only images with alpha support this");
		alphasb = a;
	}

	public int getAlphasb() {
		if (!imgInfo.alpha)
			throw new PngjException("only images with alpha support this");
		return alphasb;
	}

	/**
	 * Set rgb values
	 *
	 */
	public void setRGB(final int r, final int g, final int b) {
		if (imgInfo.greyscale || imgInfo.indexed)
			throw new PngjException("only rgb or rgba images support this");
		redsb = r;
		greensb = g;
		bluesb = b;
	}

	public int[] getRGB() {
		if (imgInfo.greyscale || imgInfo.indexed)
			throw new PngjException("only rgb or rgba images support this");
		return new int[] { redsb, greensb, bluesb };
	}
}
