package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * gAMA chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11gAMA
 */
public class PngChunkGAMA extends PngChunkSingle {
	public final static String ID = ChunkHelper.gAMA;

	// http://www.w3.org/TR/PNG/#11gAMA
	private double gamma;

	public PngChunkGAMA(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.BEFORE_PLTE_AND_IDAT;
	}

	@Override
	public ChunkRaw createRawChunk() {
		final ChunkRaw c = createEmptyChunk(4, true);
		final int g = (int) (gamma * 100000 + 0.5);
		PngHelperInternal.writeInt4tobytes(g, c.data, 0);
		return c;
	}

	@Override
	public void parseFromRaw(final ChunkRaw chunk) {
		if (chunk.len != 4)
			throw new PngjException("bad chunk " + chunk);
		final int g = PngHelperInternal.readInt4fromBytes(chunk.data, 0);
		gamma = (g) / 100000.0;
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		gamma = ((PngChunkGAMA) other).gamma;
	}

	public double getGamma() {
		return gamma;
	}

	public void setGamma(final double gamma) {
		this.gamma = gamma;
	}

}
