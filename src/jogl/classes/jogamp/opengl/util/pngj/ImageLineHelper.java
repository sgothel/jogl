package jogamp.opengl.util.pngj;

import jogamp.opengl.util.pngj.ImageLine.SampleType;
import jogamp.opengl.util.pngj.chunks.PngChunkPLTE;
import jogamp.opengl.util.pngj.chunks.PngChunkTRNS;

/**
 * Bunch of utility static methods to process/analyze an image line at the pixel
 * level.
 * <p>
 * Not essential at all, some methods are probably to be removed if future
 * releases.
 * <p>
 * WARNING: most methods for getting/setting values work currently only for
 * integer base imageLines
 */
public class ImageLineHelper {

	private final static double BIG_VALUE = Double.MAX_VALUE * 0.5;

	private final static double BIG_VALUE_NEG = Double.MAX_VALUE * (-0.5);

	/**
	 * Given an indexed line with a palette, unpacks as a RGB array, or RGBA if
	 * a non nul PngChunkTRNS chunk is passed
	 *
	 * @param line
	 *            ImageLine as returned from PngReader
	 * @param pal
	 *            Palette chunk
	 * @param buf
	 *            Preallocated array, optional
	 * @return R G B (A), one sample 0-255 per array element. Ready for
	 *         pngw.writeRowInt()
	 */
	public static int[] palette2rgb(ImageLine line, final PngChunkPLTE pal, final PngChunkTRNS trns, int[] buf) {
		final boolean isalpha = trns != null;
		final int channels = isalpha ? 4 : 3;
		final int nsamples = line.imgInfo.cols * channels;
		if (buf == null || buf.length < nsamples)
			buf = new int[nsamples];
		if (!line.samplesUnpacked)
			line = line.unpackToNewImageLine();
		final boolean isbyte = line.sampleType == SampleType.BYTE;
		final int nindexesWithAlpha = trns != null ? trns.getPalletteAlpha().length : 0;
		for (int c = 0; c < line.imgInfo.cols; c++) {
			final int index = isbyte ? (line.scanlineb[c] & 0xFF) : line.scanline[c];
			pal.getEntryRgb(index, buf, c * channels);
			if (isalpha) {
				final int alpha = index < nindexesWithAlpha ? trns.getPalletteAlpha()[index] : 255;
				buf[c * channels + 3] = alpha;
			}
		}
		return buf;
	}

	public static int[] palette2rgb(final ImageLine line, final PngChunkPLTE pal, final int[] buf) {
		return palette2rgb(line, pal, null, buf);
	}

	/**
	 * what follows is pretty uninteresting/untested/obsolete, subject to change
	 */
	/**
	 * Just for basic info or debugging. Shows values for first and last pixel.
	 * Does not include alpha
	 */
	public static String infoFirstLastPixels(final ImageLine line) {
		return line.imgInfo.channels == 1 ? String.format("first=(%d) last=(%d)", line.scanline[0],
				line.scanline[line.scanline.length - 1]) : String.format("first=(%d %d %d) last=(%d %d %d)",
				line.scanline[0], line.scanline[1], line.scanline[2], line.scanline[line.scanline.length
						- line.imgInfo.channels], line.scanline[line.scanline.length - line.imgInfo.channels + 1],
				line.scanline[line.scanline.length - line.imgInfo.channels + 2]);
	}

	public static String infoFull(final ImageLine line) {
		final ImageLineStats stats = new ImageLineStats(line);
		return "row=" + line.getRown() + " " + stats.toString() + "\n  " + infoFirstLastPixels(line);
	}

	/**
	 * Computes some statistics for the line. Not very efficient or elegant,
	 * mainly for tests. Only for RGB/RGBA Outputs values as doubles (0.0 - 1.0)
	 */
	static class ImageLineStats {
		public double[] prom = { 0.0, 0.0, 0.0, 0.0 }; // channel averages
		public double[] maxv = { BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE_NEG }; // maximo
		public double[] minv = { BIG_VALUE, BIG_VALUE, BIG_VALUE, BIG_VALUE };
		public double promlum = 0.0; // maximum global (luminance)
		public double maxlum = BIG_VALUE_NEG; // max luminance
		public double minlum = BIG_VALUE;
		public double[] maxdif = { BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE_NEG, BIG_VALUE }; // maxima
		public final int channels; // diferencia

		@Override
		public String toString() {
			return channels == 3 ? String.format(
					"prom=%.1f (%.1f %.1f %.1f) max=%.1f (%.1f %.1f %.1f) min=%.1f (%.1f %.1f %.1f)", promlum, prom[0],
					prom[1], prom[2], maxlum, maxv[0], maxv[1], maxv[2], minlum, minv[0], minv[1], minv[2])
					+ String.format(" maxdif=(%.1f %.1f %.1f)", maxdif[0], maxdif[1], maxdif[2]) : String.format(
					"prom=%.1f (%.1f %.1f %.1f %.1f) max=%.1f (%.1f %.1f %.1f %.1f) min=%.1f (%.1f %.1f %.1f %.1f)",
					promlum, prom[0], prom[1], prom[2], prom[3], maxlum, maxv[0], maxv[1], maxv[2], maxv[3], minlum,
					minv[0], minv[1], minv[2], minv[3])
					+ String.format(" maxdif=(%.1f %.1f %.1f %.1f)", maxdif[0], maxdif[1], maxdif[2], maxdif[3]);
		}

		public ImageLineStats(final ImageLine line) {
			this.channels = line.channels;
			if (line.channels < 3)
				throw new PngjException("ImageLineStats only works for RGB - RGBA");
			int ch = 0;
			double lum, x, d;
			for (int i = 0; i < line.imgInfo.cols; i++) {
				lum = 0;
				for (ch = channels - 1; ch >= 0; ch--) {
					x = int2double(line, line.scanline[i * channels]);
					if (ch < 3)
						lum += x;
					prom[ch] += x;
					if (x > maxv[ch])
						maxv[ch] = x;
					if (x < minv[ch])
						minv[ch] = x;
					if (i >= channels) {
						d = Math.abs(x - int2double(line, line.scanline[i - channels]));
						if (d > maxdif[ch])
							maxdif[ch] = d;
					}
				}
				promlum += lum;
				if (lum > maxlum)
					maxlum = lum;
				if (lum < minlum)
					minlum = lum;
			}
			for (ch = 0; ch < channels; ch++) {
				prom[ch] /= line.imgInfo.cols;
			}
			promlum /= (line.imgInfo.cols * 3.0);
			maxlum /= 3.0;
			minlum /= 3.0;
		}
	}

	/**
	 * integer packed R G B only for bitdepth=8! (does not check!)
	 *
	 **/
	public static int getPixelRGB8(final ImageLine line, final int column) {
		final int offset = column * line.channels;
		return (line.scanline[offset] << 16) + (line.scanline[offset + 1] << 8) + (line.scanline[offset + 2]);
	}

	public static int getPixelARGB8(final ImageLine line, final int column) {
		final int offset = column * line.channels;
		return (line.scanline[offset + 3] << 24) + (line.scanline[offset] << 16) + (line.scanline[offset + 1] << 8)
				+ (line.scanline[offset + 2]);
	}

	public static void setPixelsRGB8(final ImageLine line, final int[] rgb) {
		for (int i = 0, j = 0; i < line.imgInfo.cols; i++) {
			line.scanline[j++] = ((rgb[i] >> 16) & 0xFF);
			line.scanline[j++] = ((rgb[i] >> 8) & 0xFF);
			line.scanline[j++] = ((rgb[i] & 0xFF));
		}
	}

	public static void setPixelRGB8(final ImageLine line, int col, final int r, final int g, final int b) {
		col *= line.channels;
		line.scanline[col++] = r;
		line.scanline[col++] = g;
		line.scanline[col] = b;
	}

	public static void setPixelRGB8(final ImageLine line, final int col, final int rgb) {
		setPixelRGB8(line, col, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
	}

	public static void setPixelsRGBA8(final ImageLine line, final int[] rgb) {
		for (int i = 0, j = 0; i < line.imgInfo.cols; i++) {
			line.scanline[j++] = ((rgb[i] >> 16) & 0xFF);
			line.scanline[j++] = ((rgb[i] >> 8) & 0xFF);
			line.scanline[j++] = ((rgb[i] & 0xFF));
			line.scanline[j++] = ((rgb[i] >> 24) & 0xFF);
		}
	}

	public static void setPixelRGBA8(final ImageLine line, int col, final int r, final int g, final int b, final int a) {
		col *= line.channels;
		line.scanline[col++] = r;
		line.scanline[col++] = g;
		line.scanline[col++] = b;
		line.scanline[col] = a;
	}

	public static void setPixelRGBA8(final ImageLine line, final int col, final int rgb) {
		setPixelRGBA8(line, col, (rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, (rgb >> 24) & 0xFF);
	}

	public static void setValD(final ImageLine line, final int i, final double d) {
		line.scanline[i] = double2int(line, d);
	}

	public static int interpol(final int a, final int b, final int c, final int d, final double dx, final double dy) {
		// a b -> x (0-1)
		// c d
		//
		final double e = a * (1.0 - dx) + b * dx;
		final double f = c * (1.0 - dx) + d * dx;
		return (int) (e * (1 - dy) + f * dy + 0.5);
	}

	public static double int2double(final ImageLine line, final int p) {
		return line.bitDepth == 16 ? p / 65535.0 : p / 255.0;
		// TODO: replace my multiplication? check for other bitdepths
	}

	public static double int2doubleClamped(final ImageLine line, final int p) {
		// TODO: replace my multiplication?
		final double d = line.bitDepth == 16 ? p / 65535.0 : p / 255.0;
		return d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
	}

	public static int double2int(final ImageLine line, double d) {
		d = d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
		return line.bitDepth == 16 ? (int) (d * 65535.0 + 0.5) : (int) (d * 255.0 + 0.5); //
	}

	public static int double2intClamped(final ImageLine line, double d) {
		d = d <= 0.0 ? 0 : (d >= 1.0 ? 1.0 : d);
		return line.bitDepth == 16 ? (int) (d * 65535.0 + 0.5) : (int) (d * 255.0 + 0.5); //
	}

	public static int clampTo_0_255(final int i) {
		return i > 255 ? 255 : (i < 0 ? 0 : i);
	}

	public static int clampTo_0_65535(final int i) {
		return i > 65535 ? 65535 : (i < 0 ? 0 : i);
	}

	public static int clampTo_128_127(final int x) {
		return x > 127 ? 127 : (x < -128 ? -128 : x);
	}

	/**
	 * Unpacks scanline (for bitdepth 1-2-4) into a array <code>int[]</code>
	 * <p>
	 * You can (OPTIONALLY) pass an preallocated array, that will be filled and
	 * returned. If null, it will be allocated
	 * <p>
	 * If
	 * <code>scale==true<code>, it scales the value (just a bit shift) towards 0-255.
	 * <p>
	 * You probably should use {@link ImageLine#unpackToNewImageLine()}
	 *
	 */
	public static int[] unpack(final ImageInfo imgInfo, final int[] src, int[] dst, final boolean scale) {
		final int len1 = imgInfo.samplesPerRow;
		final int len0 = imgInfo.samplesPerRowPacked;
		if (dst == null || dst.length < len1)
			dst = new int[len1];
		if (imgInfo.packed)
			ImageLine.unpackInplaceInt(imgInfo, src, dst, scale);
		else
			System.arraycopy(src, 0, dst, 0, len0);
		return dst;
	}

	public static byte[] unpack(final ImageInfo imgInfo, final byte[] src, byte[] dst, final boolean scale) {
		final int len1 = imgInfo.samplesPerRow;
		final int len0 = imgInfo.samplesPerRowPacked;
		if (dst == null || dst.length < len1)
			dst = new byte[len1];
		if (imgInfo.packed)
			ImageLine.unpackInplaceByte(imgInfo, src, dst, scale);
		else
			System.arraycopy(src, 0, dst, 0, len0);
		return dst;
	}

	/**
	 * Packs scanline (for bitdepth 1-2-4) from array into the scanline
	 * <p>
	 * If <code>scale==true<code>, it scales the value (just a bit shift).
	 *
	 * You probably should use {@link ImageLine#packToNewImageLine()}
	 */
	public static int[] pack(final ImageInfo imgInfo, final int[] src, int[] dst, final boolean scale) {
		final int len0 = imgInfo.samplesPerRowPacked;
		if (dst == null || dst.length < len0)
			dst = new int[len0];
		if (imgInfo.packed)
			ImageLine.packInplaceInt(imgInfo, src, dst, scale);
		else
			System.arraycopy(src, 0, dst, 0, len0);
		return dst;
	}

	public static byte[] pack(final ImageInfo imgInfo, final byte[] src, byte[] dst, final boolean scale) {
		final int len0 = imgInfo.samplesPerRowPacked;
		if (dst == null || dst.length < len0)
			dst = new byte[len0];
		if (imgInfo.packed)
			ImageLine.packInplaceByte(imgInfo, src, dst, scale);
		else
			System.arraycopy(src, 0, dst, 0, len0);
		return dst;
	}

	static int getMaskForPackedFormats(final int bitDepth) { // Utility function for pack/unpack
		if (bitDepth == 4)
			return 0xf0;
		else if (bitDepth == 2)
			return 0xc0;
		else
			return 0x80; // bitDepth == 1
	}

	static int getMaskForPackedFormatsLs(final int bitDepth) { // Utility function for pack/unpack
		if (bitDepth == 4)
			return 0x0f;
		else if (bitDepth == 2)
			return 0x03;
		else
			return 0x01; // bitDepth == 1
	}

}
