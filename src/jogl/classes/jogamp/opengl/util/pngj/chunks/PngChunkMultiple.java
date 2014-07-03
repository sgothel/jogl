package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;

/**
 * PNG chunk type (abstract) that allows multiple instances in same image.
 */
public abstract class PngChunkMultiple extends PngChunk {

	protected PngChunkMultiple(final String id, final ImageInfo imgInfo) {
		super(id, imgInfo);
	}

	@Override
	public final boolean allowsMultiple() {
		return true;
	}

	/**
	 * NOTE: this chunk uses the default Object's equals() hashCode()
	 * implementation.
	 *
	 * This is the right thing to do, normally.
	 *
	 * This is important, eg see ChunkList.removeFromList()
	 */

}
