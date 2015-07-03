package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;


/**
 * iTXt chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11iTXt
 */
public class PngChunkITXT extends PngChunkTextVar {
	public final static String ID = ChunkHelper.iTXt;

	private boolean compressed = false;
	private String langTag = "";
	private String translatedTag = "";

	// http://www.w3.org/TR/PNG/#11iTXt
	public PngChunkITXT(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkRaw createRawChunk() {
		if (key.isEmpty())
			throw new PngjException("Text chunk key must be non empty");
		try {
			final ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ba.write(ChunkHelper.toBytes(key));
			ba.write(0); // separator
			ba.write(compressed ? 1 : 0);
			ba.write(0); // compression method (always 0)
			ba.write(ChunkHelper.toBytes(langTag));
			ba.write(0); // separator
			ba.write(ChunkHelper.toBytesUTF8(translatedTag));
			ba.write(0); // separator
			byte[] textbytes = ChunkHelper.toBytesUTF8(val);
			if (compressed) {
				textbytes = ChunkHelper.compressBytes(textbytes, true);
			}
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
		int nullsFound = 0;
		final int[] nullsIdx = new int[3];
		for (int i = 0; i < c.data.length; i++) {
			if (c.data[i] != 0)
				continue;
			nullsIdx[nullsFound] = i;
			nullsFound++;
			if (nullsFound == 1)
				i += 2;
			if (nullsFound == 3)
				break;
		}
		if (nullsFound != 3)
			throw new PngjException("Bad formed PngChunkITXT chunk");
		key = ChunkHelper.toString(c.data, 0, nullsIdx[0]);
		int i = nullsIdx[0] + 1;
		compressed = c.data[i] == 0 ? false : true;
		i++;
		if (compressed && c.data[i] != 0)
			throw new PngjException("Bad formed PngChunkITXT chunk - bad compression method ");
		langTag = new String(c.data, i, nullsIdx[1] - i, PngHelperInternal.charsetLatin1);
		translatedTag = new String(c.data, nullsIdx[1] + 1, nullsIdx[2] - nullsIdx[1] - 1,
				PngHelperInternal.charsetUTF8);
		i = nullsIdx[2] + 1;
		if (compressed) {
			final byte[] bytes = ChunkHelper.compressBytes(c.data, i, c.data.length - i, false);
			val = ChunkHelper.toStringUTF8(bytes);
		} else {
			val = ChunkHelper.toStringUTF8(c.data, i, c.data.length - i);
		}
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkITXT otherx = (PngChunkITXT) other;
		key = otherx.key;
		val = otherx.val;
		compressed = otherx.compressed;
		langTag = otherx.langTag;
		translatedTag = otherx.translatedTag;
	}

	public boolean isCompressed() {
		return compressed;
	}

	public void setCompressed(final boolean compressed) {
		this.compressed = compressed;
	}

	public String getLangtag() {
		return langTag;
	}

	public void setLangtag(final String langtag) {
		this.langTag = langtag;
	}

	public String getTranslatedTag() {
		return translatedTag;
	}

	public void setTranslatedTag(final String translatedTag) {
		this.translatedTag = translatedTag;
	}
}
