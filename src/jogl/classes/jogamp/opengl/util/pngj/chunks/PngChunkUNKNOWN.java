package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;

public class PngChunkUNKNOWN extends PngChunk { // unkown, custom or not

	private byte[] data;

	public PngChunkUNKNOWN(String id, ImageInfo info) {
		super(id, info);
	}

	@Override
	public boolean allowsMultiple() {
		return true;
	}

	private PngChunkUNKNOWN(PngChunkUNKNOWN c, ImageInfo info) {
		super(c.id, info);
		System.arraycopy(c.data, 0, data, 0, c.data.length);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw p = createEmptyChunk(data.length, false);
		p.data = this.data;
		return p;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		data = c.data;
	}

	/* does not copy! */
	public byte[] getData() {
		return data;
	}

	/* does not copy! */
	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		// THIS SHOULD NOT BE CALLED IF ALREADY CLONED WITH COPY CONSTRUCTOR
		PngChunkUNKNOWN c = (PngChunkUNKNOWN) other;
		data = c.data; // not deep copy
	}
}
