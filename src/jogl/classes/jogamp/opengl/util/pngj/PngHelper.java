package jogamp.opengl.util.pngj;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.CRC32;

/**
 * Some utility static methods.
 * <p>
 * See also <code>FileHelper</code> (if not sandboxed).
 * <p>
 * Client code should rarely need these methods.
 */
public class PngHelper {
	/**
	 * Default charset, used internally by PNG for several things
	 */
	public static Charset charsetLatin1 = Charset.forName("ISO-8859-1");
	public static Charset charsetUTF8 = Charset.forName("UTF-8"); // only for some chunks

	static boolean DEBUG = false;

	public static int readByte(InputStream is) {
		try {
			return is.read();
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	/**
	 * -1 if eof
	 * 
	 * PNG uses "network byte order"
	 */
	public static int readInt2(InputStream is) {
		try {
			int b1 = is.read();
			int b2 = is.read();
			if (b1 == -1 || b2 == -1)
				return -1;
			return (b1 << 8) + b2;
		} catch (IOException e) {
			throw new PngjInputException("error reading readInt2", e);
		}
	}

	/**
	 * -1 if eof
	 */
	public static int readInt4(InputStream is) {
		try {
			int b1 = is.read();
			int b2 = is.read();
			int b3 = is.read();
			int b4 = is.read();
			if (b1 == -1 || b2 == -1 || b3 == -1 || b4 == -1)
				return -1;
			return (b1 << 24) + (b2 << 16) + (b3 << 8) + b4;
		} catch (IOException e) {
			throw new PngjInputException("error reading readInt4", e);
		}
	}

	public static int readInt1fromByte(byte[] b, int offset) {
		return (b[offset] & 0xff);
	}

	public static int readInt2fromBytes(byte[] b, int offset) {
		return ((b[offset] & 0xff) << 16) | ((b[offset + 1] & 0xff));
	}

	public static int readInt4fromBytes(byte[] b, int offset) {
		return ((b[offset] & 0xff) << 24) | ((b[offset + 1] & 0xff) << 16) | ((b[offset + 2] & 0xff) << 8)
				| (b[offset + 3] & 0xff);
	}

	public static void writeByte(OutputStream os, byte b) {
		try {
			os.write(b);
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static void writeInt2(OutputStream os, int n) {
		byte[] temp = { (byte) ((n >> 8) & 0xff), (byte) (n & 0xff) };
		writeBytes(os, temp);
	}

	public static void writeInt4(OutputStream os, int n) {
		byte[] temp = new byte[4];
		writeInt4tobytes(n, temp, 0);
		writeBytes(os, temp);
	}

	public static void writeInt2tobytes(int n, byte[] b, int offset) {
		b[offset] = (byte) ((n >> 8) & 0xff);
		b[offset + 1] = (byte) (n & 0xff);
	}

	public static void writeInt4tobytes(int n, byte[] b, int offset) {
		b[offset] = (byte) ((n >> 24) & 0xff);
		b[offset + 1] = (byte) ((n >> 16) & 0xff);
		b[offset + 2] = (byte) ((n >> 8) & 0xff);
		b[offset + 3] = (byte) (n & 0xff);
	}

	/**
	 * guaranteed to read exactly len bytes. throws error if it cant
	 */
	public static void readBytes(InputStream is, byte[] b, int offset, int len) {
		if (len == 0)
			return;
		try {
			int read = 0;
			while (read < len) {
				int n = is.read(b, offset + read, len - read);
				if (n < 1)
					throw new RuntimeException("error reading bytes, " + n + " !=" + len);
				read += n;
			}
		} catch (IOException e) {
			throw new PngjInputException("error reading", e);
		}
	}

	public static void writeBytes(OutputStream os, byte[] b) {
		try {
			os.write(b);
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static void writeBytes(OutputStream os, byte[] b, int offset, int n) {
		try {
			os.write(b, offset, n);
		} catch (IOException e) {
			throw new PngjOutputException(e);
		}
	}

	public static void logdebug(String msg) {
		if (DEBUG)
			System.out.println(msg);
	}

	public static Set<String> asSet(String... values) {
		return new HashSet<String>(java.util.Arrays.asList(values));
	}

	public static Set<String> unionSets(Set<String> set1, Set<String> set2) {
		Set<String> s = new HashSet<String>();
		s.addAll(set1);
		s.addAll(set2);
		return s;
	}

	public static Set<String> unionSets(Set<String> set1, Set<String> set2, Set<String> set3) {
		Set<String> s = new HashSet<String>();
		s.addAll(set1);
		s.addAll(set2);
		s.addAll(set3);
		return s;
	}

	private static final ThreadLocal<CRC32> crcProvider = new ThreadLocal<CRC32>() {
		protected CRC32 initialValue() {
			return new CRC32();
		}
	};

	/** thread-singleton crc engine */
	public static CRC32 getCRC() {
		return crcProvider.get();
	}

	static final byte[] pngIdBytes = { -119, 80, 78, 71, 13, 10, 26, 10 }; // png magic

	public static double resMetersToDpi(long res) {
		return (double) res * 0.0254;
	}

	public static long resDpiToMeters(double dpi) {
		return (long) (dpi / 0.0254 + 0.5);
	}

	public static int doubleToInt100000(double d) {
		return (int) (d * 100000.0 + 0.5);
	}

	public static double intToDouble100000(int i) {
		return i / 100000.0;
	}

	public static int clampTo_0_255(int i) {
		return i > 255 ? 255 : (i < 0 ? 0 : i);
	}

	public static int clampTo_0_65535(int i) {
		return i > 65535 ? 65535 : (i < 0 ? 0 : i);
	}

	public static int clampTo_128_127(int x) {
		return x > 127 ? 127 : (x < -128 ? -128 : x);
	}

}
