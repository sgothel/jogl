package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.CRC32;

import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjBadCrcException;
import jogamp.opengl.util.pngj.PngjOutputException;


/**
 * Raw (physical) chunk.
 * <p>
 * Short lived object, to be created while serialing/deserializing Do not reuse
 * it for different chunks. <br>
 * See http://www.libpng.org/pub/png/spec/1.2/PNG-Structure.html
 */
public class ChunkRaw {
	/**
	 * The length counts only the data field, not itself, the chunk type code,
	 * or the CRC. Zero is a valid length. Although encoders and decoders should
	 * treat the length as unsigned, its value must not exceed 231-1 bytes.
	 */
	public final int len;

	/**
	 * A 4-byte chunk type code. uppercase and lowercase ASCII letters
	 */
	public final byte[] idbytes = new byte[4];

	/**
	 * The data bytes appropriate to the chunk type, if any. This field can be
	 * of zero length. Does not include crc
	 */
	public byte[] data = null;
	/**
	 * A 4-byte CRC (Cyclic Redundancy Check) calculated on the preceding bytes
	 * in the chunk, including the chunk type code and chunk data fields, but
	 * not including the length field.
	 */
	private int crcval = 0;

	/**
	 * @param len
	 *            : data len
	 * @param idbytes
	 *            : chunk type (deep copied)
	 * @param alloc
	 *            : it true, the data array will be allocced
	 */
	public ChunkRaw(final int len, final byte[] idbytes, final boolean alloc) {
		this.len = len;
		System.arraycopy(idbytes, 0, this.idbytes, 0, 4);
		if (alloc)
			allocData();
	}

	private void allocData() {
		if (data == null || data.length < len)
			data = new byte[len];
	}

	/**
	 * this is called after setting data, before writing to os
	 */
	private int computeCrc() {
		final CRC32 crcengine = PngHelperInternal.getCRC();
		crcengine.reset();
		crcengine.update(idbytes, 0, 4);
		if (len > 0)
			crcengine.update(data, 0, len); //
		return (int) crcengine.getValue();
	}

	/**
	 * Computes the CRC and writes to the stream. If error, a
	 * PngjOutputException is thrown
	 */
	public void writeChunk(final OutputStream os) {
		if (idbytes.length != 4)
			throw new PngjOutputException("bad chunkid [" + ChunkHelper.toString(idbytes) + "]");
		crcval = computeCrc();
		PngHelperInternal.writeInt4(os, len);
		PngHelperInternal.writeBytes(os, idbytes);
		if (len > 0)
			PngHelperInternal.writeBytes(os, data, 0, len);
		PngHelperInternal.writeInt4(os, crcval);
	}

	/**
	 * position before: just after chunk id. positon after: after crc Data
	 * should be already allocated. Checks CRC Return number of byte read.
	 */
	public int readChunkData(final InputStream is, final boolean checkCrc) {
		PngHelperInternal.readBytes(is, data, 0, len);
		crcval = PngHelperInternal.readInt4(is);
		if (checkCrc) {
			final int crc = computeCrc();
			if (crc != crcval)
				throw new PngjBadCrcException("chunk: " + this + " crc calc=" + crc + " read=" + crcval);
		}
		return len + 4;
	}

	ByteArrayInputStream getAsByteStream() { // only the data
		return new ByteArrayInputStream(data);
	}

	@Override
	public String toString() {
		return "chunkid=" + ChunkHelper.toString(idbytes) + " len=" + len;
	}

}
