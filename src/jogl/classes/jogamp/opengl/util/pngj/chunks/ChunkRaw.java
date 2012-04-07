package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjBadCrcException;
import jogamp.opengl.util.pngj.PngjOutputException;


/**
 * Wraps the raw chunk data Short lived object, to be created while serialing/deserializing Do not reuse it for
 * different chunks
 * 
 * see http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
 */
public class ChunkRaw {
	public final int len;
	public final byte[] idbytes = new byte[4]; // 4 bytes
	public byte[] data = null; // crc not included
	private int crcval = 0;

	// public int offset=-1; // only for read chunks - informational
	public ChunkRaw(int len, byte[] idbytes, boolean alloc) {
		this.len = len;
		System.arraycopy(idbytes, 0, this.idbytes, 0, 4);
		if (alloc)
			allocData();
	}

	public void writeChunk(OutputStream os) {
		if (idbytes.length != 4)
			throw new PngjOutputException("bad chunkid [" + ChunkHelper.toString(idbytes) + "]");
		computeCrc();
		PngHelper.writeInt4(os, len);
		PngHelper.writeBytes(os, idbytes);
		if (len > 0)
			PngHelper.writeBytes(os, data, 0, len);
		// System.err.println("writing chunk " + this.toString() + "crc=" + crcval);

		PngHelper.writeInt4(os, crcval);
	}

	/**
	 * called after setting data, before writing to os
	 */
	private void computeCrc() {
		CRC32 crcengine = PngHelper.getCRC();
		crcengine.reset();
		crcengine.update(idbytes, 0, 4);
		if (len > 0)
			crcengine.update(data, 0, len); //
		crcval = (int) crcengine.getValue();
	}

	public String toString() {
		return "chunkid=" + ChunkHelper.toString(idbytes) + " len=" + len;
	}

	/**
	 * position before: just after chunk id. positon after: after crc Data should be already allocated. Checks CRC
	 * Return number of byte read.
	 */
	public int readChunkData(InputStream is) {
		PngHelper.readBytes(is, data, 0, len);
		int crcori = PngHelper.readInt4(is);
		computeCrc();
		if (crcori != crcval)
			throw new PngjBadCrcException("crc invalid for chunk " + toString() + " calc=" + crcval + " read=" + crcori);
		return len + 4;
	}

	public ByteArrayInputStream getAsByteStream() { // only the data
		return new ByteArrayInputStream(data);
	}

	private void allocData() {
		if (data == null || data.length < len)
			data = new byte[len];
	}
}
