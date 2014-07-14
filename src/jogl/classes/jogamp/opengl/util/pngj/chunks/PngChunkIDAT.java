package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;

/**
 * IDAT chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11IDAT
 * <p>
 * This is dummy placeholder - we write/read this chunk (actually several) by
 * special code.
 */
public class PngChunkIDAT extends PngChunkMultiple {
	public final static String ID = ChunkHelper.IDAT;

	// http://www.w3.org/TR/PNG/#11IDAT
	public PngChunkIDAT(final ImageInfo i, final int len, final long offset) {
		super(ID, i);
		this.length = len;
		this.offset = offset;
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NA;
	}

	@Override
	public ChunkRaw createRawChunk() {// does nothing
		return null;
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) { // does nothing
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
	}
}
