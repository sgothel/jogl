package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * tEXt chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11tEXt
 */
public class PngChunkTEXT extends PngChunkTextVar {
	public final static String ID = ChunkHelper.tEXt;

	public PngChunkTEXT(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkRaw createRawChunk() {
		if (key.isEmpty())
			throw new PngjException("Text chunk key must be non empty");
		final byte[] b = (key + "\0" + val).getBytes(PngHelperInternal.charsetLatin1);
		final ChunkRaw chunk = createEmptyChunk(b.length, false);
		chunk.data = b;
		return chunk;
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		int i;
		for (i = 0; i < c.data.length; i++)
			if (c.data[i] == 0)
				break;
		key = new String(c.data, 0, i, PngHelperInternal.charsetLatin1);
		i++;
		val = i < c.data.length ? new String(c.data, i, c.data.length - i, PngHelperInternal.charsetLatin1) : "";
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkTEXT otherx = (PngChunkTEXT) other;
		key = otherx.key;
		val = otherx.val;
	}
}
