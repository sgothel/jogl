package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngjException;

/**
 * Pseudo chunk type, for chunks that were skipped on reading
 */
public class PngChunkSkipped extends PngChunk {

	public PngChunkSkipped(final String id, final ImageInfo info, final int clen) {
		super(id, info);
		this.length = clen;
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NONE;
	}

	@Override
	public ChunkRaw createRawChunk() {
		throw new PngjException("Non supported for a skipped chunk");
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		throw new PngjException("Non supported for a skipped chunk");
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		throw new PngjException("Non supported for a skipped chunk");
	}

	@Override
	public boolean allowsMultiple() {
		return true;
	}

}
