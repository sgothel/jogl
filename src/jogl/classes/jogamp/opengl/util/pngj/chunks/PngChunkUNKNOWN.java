package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;

/**
 * Placeholder for UNKNOWN (custom or not) chunks.
 * <p>
 * For PngReader, a chunk is unknown if it's not registered in the chunk factory
 */
public class PngChunkUNKNOWN extends PngChunkMultiple { // unkown, custom or not

	private byte[] data;

	public PngChunkUNKNOWN(final String id, final ImageInfo info) {
		super(id, info);
	}

	private PngChunkUNKNOWN(final PngChunkUNKNOWN c, final ImageInfo info) {
		super(c.id, info);
		System.arraycopy(c.data, 0, data, 0, c.data.length);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NONE;
	}

	@Override
	public ChunkRaw createRawChunk() {
		final ChunkRaw p = createEmptyChunk(data.length, false);
		p.data = this.data;
		return p;
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		data = c.data;
	}

	/* does not copy! */
	public byte[] getData() {
		return data;
	}

	/* does not copy! */
	public void setData(final byte[] data) {
		this.data = data;
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		// THIS SHOULD NOT BE CALLED IF ALREADY CLONED WITH COPY CONSTRUCTOR
		final PngChunkUNKNOWN c = (PngChunkUNKNOWN) other;
		data = c.data; // not deep copy
	}
}
