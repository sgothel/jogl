package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * zTXt chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11zTXt
 */
public class PngChunkZTXT extends PngChunkTextVar {
	public final static String ID = ChunkHelper.zTXt;

	// http://www.w3.org/TR/PNG/#11zTXt
	public PngChunkZTXT(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkRaw createRawChunk() {
		if (key.isEmpty())
			throw new PngjException("Text chunk key must be non empty");
		try {
			final ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ba.write(key.getBytes(PngHelperInternal.charsetLatin1));
			ba.write(0); // separator
			ba.write(0); // compression method: 0
			final byte[] textbytes = ChunkHelper.compressBytes(val.getBytes(PngHelperInternal.charsetLatin1), true);
			ba.write(textbytes);
			final byte[] b = ba.toByteArray();
			final ChunkRaw chunk = createEmptyChunk(b.length, false);
			chunk.data = b;
			return chunk;
		} catch (final IOException e) {
			throw new PngjException(e);
		}
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		int nullsep = -1;
		for (int i = 0; i < c.data.length; i++) { // look for first zero
			if (c.data[i] != 0)
				continue;
			nullsep = i;
			break;
		}
		if (nullsep < 0 || nullsep > c.data.length - 2)
			throw new PngjException("bad zTXt chunk: no separator found");
		key = new String(c.data, 0, nullsep, PngHelperInternal.charsetLatin1);
		final int compmet = c.data[nullsep + 1];
		if (compmet != 0)
			throw new PngjException("bad zTXt chunk: unknown compression method");
		final byte[] uncomp = ChunkHelper.compressBytes(c.data, nullsep + 2, c.data.length - nullsep - 2, false); // uncompress
		val = new String(uncomp, PngHelperInternal.charsetLatin1);
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkZTXT otherx = (PngChunkZTXT) other;
		key = otherx.key;
		val = otherx.val;
	}
}
