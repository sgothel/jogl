package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;

public class PngChunkIEND extends PngChunk {
	// http://www.w3.org/TR/PNG/#11IEND
	// this is a dummy placeholder
	public PngChunkIEND(ImageInfo info) {
		super(ChunkHelper.IEND, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = new ChunkRaw(0, ChunkHelper.b_IEND, false);
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		// this is not used
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
	}
}
