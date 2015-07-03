package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * sRGB chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11sRGB
 */
public class PngChunkSRGB extends PngChunkSingle {
	public final static String ID = ChunkHelper.sRGB;

	// http://www.w3.org/TR/PNG/#11sRGB

	public static final int RENDER_INTENT_Perceptual = 0;
	public static final int RENDER_INTENT_Relative_colorimetric = 1;
	public static final int RENDER_INTENT_Saturation = 2;
	public static final int RENDER_INTENT_Absolute_colorimetric = 3;

	private int intent;

	public PngChunkSRGB(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.BEFORE_PLTE_AND_IDAT;
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		if (c.len != 1)
			throw new PngjException("bad chunk length " + c);
		intent = PngHelperInternal.readInt1fromByte(c.data, 0);
	}

	@Override
	public ChunkRaw createRawChunk() {
		ChunkRaw c = null;
		c = createEmptyChunk(1, true);
		c.data[0] = (byte) intent;
		return c;
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkSRGB otherx = (PngChunkSRGB) other;
		intent = otherx.intent;
	}

	public int getIntent() {
		return intent;
	}

	public void setIntent(final int intent) {
		this.intent = intent;
	}
}
