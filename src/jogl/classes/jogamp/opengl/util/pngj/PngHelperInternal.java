package jogamp.opengl.util.pngj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.zip.CRC32;

/**
 * Some utility static methods for internal use.
 * <p>
 * Client code should not normally use this class
 * <p>
 */
public class PngHelperInternal {
	/**
	 * Default charset, used internally by PNG for several things
	 */
	public static final Charset charsetLatin1 = Charset.forName("ISO-8859-1");
	/**
	 * UTF-8 is only used for some chunks
	 */
	public static final Charset charsetUTF8 = Charset.forName("UTF-8");

	static final boolean DEBUG = false;

	/**
	 * PNG magic bytes
	 */
	public static byte[] getPngIdSignature() {
		return new byte[] { -119, 80, 78, 71, 13, 10, 26, 10 };
	}

	public static int doubleToInt100000(final double d) {
		return (int) (d * 100000.0 + 0.5);
	}

	public static double intToDouble100000(final int i) {
		return i / 100000.0;
	}

	public static int readByte(final InputStream is) {
		try {
			return is.read();
		} catch (final IOException e) {
			throw new PngjInputException("error reading byte", e);
		}
	}

	/**
	 * -1 if eof
	 *
	 * PNG uses "network byte order"
	 */
	public static int readInt2(final InputStream is) {
		try {
			final int b1 = is.read();
			final int b2 = is.read();
			if (b1 == -1 || b2 == -1)
				return -1;
			return (b1 << 8) + b2;
		} catch (final IOException e) {
			throw new PngjInputException("error reading readInt2", e);
		}
	}

	/**
	 * -1 if eof
	 */
	public static int readInt4(final InputStream is) {
		try {
			final int b1 = is.read();
			final int b2 = is.read();
			final int b3 = is.read();
			final int b4 = is.read();
			if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1)
				return -1;
			return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
		} catch (final IOException e) {
			throw new PngjInputException("error reading readInt4", e);
		}
	}

	public static int readInt1fromByte(final byte[] b, final int offset) {
		return (b[offset] & 0xff);
	}

	public static int readInt2fromBytes(final byte[] b, final int offset) {
		return ((b[offset] & 0xff) << 16) | ((b[offset + 1] & 0xff));
	}

	public static int readInt4fromBytes(final byte[] b, final int offset) {
		return ((b[offset] & 0xff) << 24) | ((b[offset + 1] & 0xff) << 16) | ((b[offset + 2] & 0xff) << 8)
				| (b[offset + 3] & 0xff);
	}

	public static void writeByte(final OutputStream os, final byte b) {
		try {
			os.write(b);
		} catch (final IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static void writeInt2(final OutputStream os, final int n) {
		final byte[] temp = { (byte) ((n >> 8) & 0xff), (byte) (n & 0xff) };
		writeBytes(os, temp);
	}

	public static void writeInt4(final OutputStream os, final int n) {
		final byte[] temp = new byte[4];
		writeInt4tobytes(n, temp, 0);
		writeBytes(os, temp);
	}

	public static void writeInt2tobytes(final int n, final byte[] b, final int offset) {
		b[offset] = (byte) ((n >> 8) & 0xff);
		b[offset + 1] = (byte) (n & 0xff);
	}

	public static void writeInt4tobytes(final int n, final byte[] b, final int offset) {
		b[offset] = (byte) ((n >> 24) & 0xff);
		b[offset + 1] = (byte) ((n >> 16) & 0xff);
		b[offset + 2] = (byte) ((n >> 8) & 0xff);
		b[offset + 3] = (byte) (n & 0xff);
	}

	/**
	 * guaranteed to read exactly len bytes. throws error if it can't
	 */
	public static void readBytes(final InputStream is, final byte[] b, final int offset, final int len) {
		if (len == 0)
			return;
		try {
			int read = 0;
			while (read < len) {
				final int n = is.read(b, offset + read, len - read);
				if (n < 1)
					throw new PngjInputException("error reading bytes, " + n + " !=" + len);
				read += n;
			}
		} catch (final IOException e) {
			throw new PngjInputException("error reading", e);
		}
	}

	public static void skipBytes(final InputStream is, long len) {
		try {
			while (len > 0) {
				final long n1 = is.skip(len);
				if (n1 > 0) {
					len -= n1;
				} else if (n1 == 0) { // should we retry? lets read one byte
					if (is.read() == -1) // EOF
						break;
					else
						len--;
				} else
					// negative? this should never happen but...
					throw new IOException("skip() returned a negative value ???");
			}
		} catch (final IOException e) {
			throw new PngjInputException(e);
		}
	}

	public static void writeBytes(final OutputStream os, final byte[] b) {
		try {
			os.write(b);
		} catch (final IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static void writeBytes(final OutputStream os, final byte[] b, final int offset, final int n) {
		try {
			os.write(b, offset, n);
		} catch (final IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static void logdebug(final String msg) {
		if (DEBUG)
			System.out.println(msg);
	}

	private static final ThreadLocal<CRC32> crcProvider = new ThreadLocal<CRC32>() {
		@Override
		protected CRC32 initialValue() {
			return new CRC32();
		}
	};

	/** thread-singleton crc engine */
	public static CRC32 getCRC() {
		return crcProvider.get();
	}

	// / filters
	public static int filterRowNone(final int r) {
		return r & 0xFF;
	}

	public static int filterRowSub(final int r, final int left) {
		return (r - left & 0xFF);
	}

	public static int filterRowUp(final int r, final int up) {
		return (r - up & 0xFF);
	}

	public static int filterRowAverage(final int r, final int left, final int up) {
		return (r - (left + up) / 2) & 0xFF;
	}

	public static int filterRowPaeth(final int r, final int left, final int up, final int upleft) { // a = left, b = above, c = upper left
		return (r - filterPaethPredictor(left, up, upleft)) & 0xFF;
	}

	public static int unfilterRowNone(final int r) {
		return r & 0xFF;
	}

	public static int unfilterRowSub(final int r, final int left) {
		return (r + left & 0xFF);
	}

	public static int unfilterRowUp(final int r, final int up) {
		return (r + up & 0xFF);
	}

	public static int unfilterRowAverage(final int r, final int left, final int up) {
		return (r + (left + up) / 2) & 0xFF;
	}

	public static int unfilterRowPaeth(final int r, final int left, final int up, final int upleft) { // a = left, b = above, c = upper left
		return (r + filterPaethPredictor(left, up, upleft)) & 0xFF;
	}

	final static int filterPaethPredictor(final int a, final int b, final int c) { // a = left, b = above, c = upper
																					// left
		// from http://www.libpng.org/pub/png/spec/1.2/PNG-Filters.html

		final int p = a + b - c;// ; initial estimate
		final int pa = p >= a ? p - a : a - p;
		final int pb = p >= b ? p - b : b - p;
		final int pc = p >= c ? p - c : c - p;
		// ; return nearest of a,b,c,
		// ; breaking ties in order a,b,c.
		if (pa <= pb && pa <= pc)
			return a;
		else if (pb <= pc)
			return b;
		else
			return c;
	}

	/*
	 * we put this methods here so as to not pollute the public interface of PngReader
	 */
	public final static void initCrcForTests(final PngReader pngr) {
		pngr.initCrctest();
	}

	public final static long getCrctestVal(final PngReader pngr) {
		return pngr.getCrctestVal();
	}

}
