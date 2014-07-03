package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * bKGD Chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11bKGD
 * <p>
 * this chunk structure depends on the image type
 */
public class PngChunkBKGD extends PngChunkSingle {
	public final static String ID = ChunkHelper.bKGD;
	// only one of these is meaningful
	private int gray;
	private int red, green, blue;
	private int paletteIndex;

	public PngChunkBKGD(final ImageInfo info) {
		super(ChunkHelper.bKGD, info);
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
			c = createEmptyChunk(1, true);
			c.data[0] = (byte) paletteIndex;
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
			paletteIndex = c.data[0] & 0xff;
		} else {
			red = PngHelperInternal.readInt2fromBytes(c.data, 0);
			green = PngHelperInternal.readInt2fromBytes(c.data, 2);
			blue = PngHelperInternal.readInt2fromBytes(c.data, 4);
		}
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkBKGD otherx = (PngChunkBKGD) other;
		gray = otherx.gray;
		red = otherx.red;
		green = otherx.red;
		blue = otherx.red;
		paletteIndex = otherx.paletteIndex;
	}

	/**
	 * Set gray value (0-255 if bitdept=8)
	 *
	 * @param gray
	 */
	public void setGray(final int gray) {
		if (!imgInfo.greyscale)
			throw new PngjException("only gray images support this");
		this.gray = gray;
	}

	public int getGray() {
		if (!imgInfo.greyscale)
			throw new PngjException("only gray images support this");
		return gray;
	}

	/**
	 * Set pallette index
	 *
	 */
	public void setPaletteIndex(final int i) {
		if (!imgInfo.indexed)
			throw new PngjException("only indexed (pallete) images support this");
		this.paletteIndex = i;
	}

	public int getPaletteIndex() {
		if (!imgInfo.indexed)
			throw new PngjException("only indexed (pallete) images support this");
		return paletteIndex;
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

}
