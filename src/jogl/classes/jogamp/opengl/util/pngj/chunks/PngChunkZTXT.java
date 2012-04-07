package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjException;


public class PngChunkZTXT extends PngChunkTextVar {
	// http://www.w3.org/TR/PNG/#11zTXt
	public PngChunkZTXT(ImageInfo info) {
		super(ChunkHelper.zTXt, info);
	}

	@Override
	public ChunkRaw createChunk() {
		if (val.isEmpty() || key.isEmpty())
			return null;
		try {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ba.write(key.getBytes(PngHelper.charsetLatin1));
			ba.write(0); // separator
			ba.write(0); // compression method: 0
			byte[] textbytes = ChunkHelper.compressBytes(val.getBytes(PngHelper.charsetLatin1), true);
			ba.write(textbytes);
			byte[] b = ba.toByteArray();
			ChunkRaw chunk = createEmptyChunk(b.length, false);
			chunk.data = b;
			return chunk;
		} catch (IOException e) {
			throw new PngjException(e);
		}
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		int nullsep = -1;
		for (int i = 0; i < c.data.length; i++) { // look for first zero
			if (c.data[i] != 0)
				continue;
			nullsep = i;
			break;
		}
		if (nullsep < 0 || nullsep > c.data.length - 2)
			throw new PngjException("bad zTXt chunk: no separator found");
		key = new String(c.data, 0, nullsep, PngHelper.charsetLatin1);
		int compmet = (int) c.data[nullsep + 1];
		if (compmet != 0)
			throw new PngjException("bad zTXt chunk: unknown compression method");
		byte[] uncomp = ChunkHelper.compressBytes(c.data, nullsep + 2, c.data.length - nullsep - 2, false); // uncompress
		val = new String(uncomp, PngHelper.charsetLatin1);
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkZTXT otherx = (PngChunkZTXT) other;
		key = otherx.key;
		val = otherx.val;
	}
}
