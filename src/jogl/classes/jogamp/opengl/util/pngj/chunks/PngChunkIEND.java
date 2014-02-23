package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;

/**
 * IEND chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11IEND
 */
public class PngChunkIEND extends PngChunkSingle {
	public final static String ID = ChunkHelper.IEND;

	// http://www.w3.org/TR/PNG/#11IEND
	// this is a dummy placeholder
	public PngChunkIEND(ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NA;
	}

	@Override
	public ChunkRaw createRawChunk() {
		ChunkRaw c = new ChunkRaw(0, ChunkHelper.b_IEND, false);
		return c;
	}

	@Override
	public void parseFromRaw(ChunkRaw c) {
		// this is not used
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
	}
}
