package jogamp.opengl.util.pngj.chunks;

// see http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
// http://www.w3.org/TR/PNG/#5Chunk-naming-conventions
// http://www.w3.org/TR/PNG/#table53
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjException;


public class ChunkHelper {
	public static final String IHDR = "IHDR";
	public static final String PLTE = "PLTE";
	public static final String IDAT = "IDAT";
	public static final String IEND = "IEND";
	public static final byte[] b_IHDR = toBytes(IHDR);
	public static final byte[] b_PLTE = toBytes(PLTE);
	public static final byte[] b_IDAT = toBytes(IDAT);
	public static final byte[] b_IEND = toBytes(IEND);

	public static final String cHRM = "cHRM";
	public static final String gAMA = "gAMA";
	public static final String iCCP = "iCCP";
	public static final String sBIT = "sBIT";
	public static final String sRGB = "sRGB";
	public static final String bKGD = "bKGD";
	public static final String hIST = "hIST";
	public static final String tRNS = "tRNS";
	public static final String pHYs = "pHYs";
	public static final String sPLT = "sPLT";
	public static final String tIME = "tIME";
	public static final String iTXt = "iTXt";
	public static final String tEXt = "tEXt";
	public static final String zTXt = "zTXt";

	public static Set<String> KNOWN_CHUNKS_CRITICAL = PngHelper.asSet(IHDR, PLTE, IDAT, IEND);

	public static byte[] toBytes(String x) {
		return x.getBytes(PngHelper.charsetLatin1);
	}

	public static String toString(byte[] x) {
		return new String(x, PngHelper.charsetLatin1);
	}

	public static boolean isCritical(String id) { // critical chunk ?
		// first letter is uppercase
		return (Character.isUpperCase(id.charAt(0)));
	}

	public static boolean isPublic(String id) { // public chunk?
		// second letter is uppercase
		return (Character.isUpperCase(id.charAt(1)));
	}

	/**
	 * "Unknown" just means that our chunk factory (even when it has been augmented by client code) did not recognize its id
	 */
	public static boolean isUnknown(PngChunk c) {
		return c instanceof PngChunkUNKNOWN;
	}

	public static boolean isSafeToCopy(String id) { // safe to copy?
		// fourth letter is lower case
		return (!Character.isUpperCase(id.charAt(3)));
	}

	public static int posNullByte(byte[] b) {
		for (int i = 0; i < b.length; i++)
			if (b[i] == 0)
				return i;
		return -1;
	}

	public static boolean shouldLoad(String id, ChunkLoadBehaviour behav) {
		if (isCritical(id))
			return true;
		boolean kwown = PngChunk.isKnown(id);
		switch (behav) {
		case LOAD_CHUNK_ALWAYS:
			return true;
		case LOAD_CHUNK_IF_SAFE:
			return kwown || isSafeToCopy(id);
		case LOAD_CHUNK_KNOWN:
			return kwown;
		case LOAD_CHUNK_NEVER:
			return false;
		}
		return false; // should not reach here
	}

	public final static byte[] compressBytes(byte[] ori, boolean compress) {
		return compressBytes(ori, 0, ori.length, compress);
	}

	public static byte[] compressBytes(byte[] ori, int offset, int len, boolean compress) {
		try {
			ByteArrayInputStream inb = new ByteArrayInputStream(ori, offset, len);
			InputStream in = compress ? inb : new InflaterInputStream(inb);
			ByteArrayOutputStream outb = new ByteArrayOutputStream();
			OutputStream out = compress ? new DeflaterOutputStream(outb) : outb;
			shovelInToOut(in, out);
			in.close();
			out.close();
			return outb.toByteArray();
		} catch (Exception e) {
			throw new PngjException(e);
		}
	}

	/**
	 * Shovels all data from an input stream to an output stream.
	 */
	private static void shovelInToOut(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
		int len;
		while ((len = in.read(buffer)) > 0) {
			out.write(buffer, 0, len);
		}
	}

	public static boolean maskMatch(int v, int mask) {
		return (v & mask) != 0;
	}

}
