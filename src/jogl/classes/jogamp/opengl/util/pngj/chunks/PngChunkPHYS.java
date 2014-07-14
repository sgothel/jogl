package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;
/**
 * pHYs chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11pHYs
 */
public class PngChunkPHYS extends PngChunkSingle {
	public final static String ID = ChunkHelper.pHYs;

	// http://www.w3.org/TR/PNG/#11pHYs
	private long pixelsxUnitX;
	private long pixelsxUnitY;
	private int units; // 0: unknown 1:metre

	public PngChunkPHYS(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.BEFORE_IDAT;
	}

	@Override
	public ChunkRaw createRawChunk() {
		final ChunkRaw c = createEmptyChunk(9, true);
		PngHelperInternal.writeInt4tobytes((int) pixelsxUnitX, c.data, 0);
		PngHelperInternal.writeInt4tobytes((int) pixelsxUnitY, c.data, 4);
		c.data[8] = (byte) units;
		return c;
	}

	@Override
	public void parseFromRaw(final ChunkRaw chunk) {
		if (chunk.len != 9)
			throw new PngjException("bad chunk length " + chunk);
		pixelsxUnitX = PngHelperInternal.readInt4fromBytes(chunk.data, 0);
		if (pixelsxUnitX < 0)
			pixelsxUnitX += 0x100000000L;
		pixelsxUnitY = PngHelperInternal.readInt4fromBytes(chunk.data, 4);
		if (pixelsxUnitY < 0)
			pixelsxUnitY += 0x100000000L;
		units = PngHelperInternal.readInt1fromByte(chunk.data, 8);
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkPHYS otherx = (PngChunkPHYS) other;
		this.pixelsxUnitX = otherx.pixelsxUnitX;
		this.pixelsxUnitY = otherx.pixelsxUnitY;
		this.units = otherx.units;
	}

	public long getPixelsxUnitX() {
		return pixelsxUnitX;
	}

	public void setPixelsxUnitX(final long pixelsxUnitX) {
		this.pixelsxUnitX = pixelsxUnitX;
	}

	public long getPixelsxUnitY() {
		return pixelsxUnitY;
	}

	public void setPixelsxUnitY(final long pixelsxUnitY) {
		this.pixelsxUnitY = pixelsxUnitY;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(final int units) {
		this.units = units;
	}

	// special getters / setters

	/**
	 * returns -1 if the physicial unit is unknown, or X-Y are not equal
	 */
	public double getAsDpi() {
		if (units != 1 || pixelsxUnitX != pixelsxUnitY)
			return -1;
		return (pixelsxUnitX) * 0.0254;
	}

	/**
	 * returns -1 if the physicial unit is unknown
	 */
	public double[] getAsDpi2() {
		if (units != 1)
			return new double[] { -1, -1 };
		return new double[] { (pixelsxUnitX) * 0.0254, (pixelsxUnitY) * 0.0254 };
	}

	public void setAsDpi(final double dpi) {
		units = 1;
		pixelsxUnitX = (long) (dpi / 0.0254 + 0.5);
		pixelsxUnitY = pixelsxUnitX;
	}

	public void setAsDpi2(final double dpix, final double dpiy) {
		units = 1;
		pixelsxUnitX = (long) (dpix / 0.0254 + 0.5);
		pixelsxUnitY = (long) (dpiy / 0.0254 + 0.5);
	}

}
