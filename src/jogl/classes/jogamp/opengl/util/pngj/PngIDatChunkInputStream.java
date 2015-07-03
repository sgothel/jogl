package jogamp.opengl.util.pngj;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.CRC32;

import jogamp.opengl.util.pngj.chunks.ChunkHelper;


/**
 * Reads a sequence of contiguous IDAT chunks
 */
class PngIDatChunkInputStream extends InputStream {
	private final InputStream inputStream;
	private final CRC32 crcEngine;
	private boolean checkCrc = true;
	private int lenLastChunk;
	private final byte[] idLastChunk = new byte[4];
	private int toReadThisChunk = 0;
	private boolean ended = false;
	private long offset; // offset inside whole inputstream (counting bytes before IDAT)

	// just informational
	static class IdatChunkInfo {
		public final int len;
		public final long offset;

		private IdatChunkInfo(final int len, final long offset) {
			this.len = len;
			this.offset = offset;
		}
	}

	List<IdatChunkInfo> foundChunksInfo = new ArrayList<IdatChunkInfo>();

	/**
	 * Constructor must be called just after reading length and id of first IDAT
	 * chunk
	 **/
	PngIDatChunkInputStream(final InputStream iStream, final int lenFirstChunk, final long offset) {
		this.offset = offset;
		inputStream = iStream;
		this.lenLastChunk = lenFirstChunk;
		toReadThisChunk = lenFirstChunk;
		// we know it's a IDAT
		System.arraycopy(ChunkHelper.b_IDAT, 0, idLastChunk, 0, 4);
		crcEngine = new CRC32();
		crcEngine.update(idLastChunk, 0, 4);
		foundChunksInfo.add(new IdatChunkInfo(lenLastChunk, offset - 8));

		// PngHelper.logdebug("IDAT Initial fragment: len=" + lenLastChunk);
		if (this.lenLastChunk == 0)
			endChunkGoForNext(); // rare, but...
	}

	/**
	 * does NOT close the associated stream!
	 */
	@Override
	public void close() throws IOException {
		super.close(); // thsi does nothing
	}

	private void endChunkGoForNext() {
		// Called after readging the last byte of one IDAT chunk
		// Checks CRC, and read ID from next CHUNK
		// Those values are left in idLastChunk / lenLastChunk
		// Skips empty IDATS
		do {
			final int crc = PngHelperInternal.readInt4(inputStream); //
			offset += 4;
			if (checkCrc) {
				final int crccalc = (int) crcEngine.getValue();
				if (lenLastChunk > 0 && crc != crccalc)
					throw new PngjBadCrcException("error reading idat; offset: " + offset);
				crcEngine.reset();
			}
			lenLastChunk = PngHelperInternal.readInt4(inputStream);
			toReadThisChunk = lenLastChunk;
			PngHelperInternal.readBytes(inputStream, idLastChunk, 0, 4);
			offset += 8;
			// found a NON IDAT chunk? this stream is ended
			ended = !Arrays.equals(idLastChunk, ChunkHelper.b_IDAT);
			if (!ended) {
				foundChunksInfo.add(new IdatChunkInfo(lenLastChunk, offset - 8));
				if (checkCrc)
					crcEngine.update(idLastChunk, 0, 4);
			}
			// PngHelper.logdebug("IDAT ended. next len= " + lenLastChunk + " idat?" +
			// (!ended));
		} while (lenLastChunk == 0 && !ended);
		// rarely condition is true (empty IDAT ??)
	}

	/**
	 * sometimes last row read does not fully consumes the chunk here we read
	 * the reamaing dummy bytes
	 */
	void forceChunkEnd() {
		if (!ended) {
			final byte[] dummy = new byte[toReadThisChunk];
			PngHelperInternal.readBytes(inputStream, dummy, 0, toReadThisChunk);
			if (checkCrc)
				crcEngine.update(dummy, 0, toReadThisChunk);
			endChunkGoForNext();
		}
	}

	/**
	 * This can return less than len, but never 0 Returns -1 if "pseudo file"
	 * ended prematurely. That is our error.
	 */
	@Override
	public int read(final byte[] b, final int off, final int len) throws IOException {
		if (ended)
			return -1; // can happen only when raw reading, see Pngreader.readAndSkipsAllRows()
		if (toReadThisChunk == 0)
			throw new PngjExceptionInternal("this should not happen");
		final int n = inputStream.read(b, off, len >= toReadThisChunk ? toReadThisChunk : len);
		if (n > 0) {
			if (checkCrc)
				crcEngine.update(b, off, n);
			this.offset += n;
			toReadThisChunk -= n;
		}
		if (toReadThisChunk == 0) { // end of chunk: prepare for next
			endChunkGoForNext();
		}
		return n;
	}

	@Override
	public int read(final byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int read() throws IOException {
		// PngHelper.logdebug("read() should go here");
		// inneficient - but this should be used rarely
		final byte[] b1 = new byte[1];
		final int r = this.read(b1, 0, 1);
		return r < 0 ? -1 : (int) b1[0];
	}

	int getLenLastChunk() {
		return lenLastChunk;
	}

	byte[] getIdLastChunk() {
		return idLastChunk;
	}

	long getOffset() {
		return offset;
	}

	boolean isEnded() {
		return ended;
	}

	/**
	 * Disables CRC checking. This can make reading faster
	 */
	void disableCrcCheck() {
		checkCrc = false;
	}
}
