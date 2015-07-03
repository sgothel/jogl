package jogamp.opengl.util.pngj;

import jogamp.opengl.util.pngj.ImageLineHelper.ImageLineStats;

/**
 * Lightweight wrapper for an image scanline, used for read and write.
 * <p>
 * This object can be (usually it is) reused while iterating over the image
 * lines.
 * <p>
 * See <code>scanline</code> field, to understand the format.
 */
public class ImageLine {
	public final ImageInfo imgInfo;

	/**
	 * tracks the current row number (from 0 to rows-1)
	 */
	private int rown = 0;

	/**
	 * The 'scanline' is an array of integers, corresponds to an image line
	 * (row).
	 * <p>
	 * Except for 'packed' formats (gray/indexed with 1-2-4 bitdepth) each
	 * <code>int</code> is a "sample" (one for channel), (0-255 or 0-65535) in
	 * the corresponding PNG sequence: <code>R G B R G B...</code> or
	 * <code>R G B A R G B A...</tt>
	 * or <code>g g g ...</code> or <code>i i i</code> (palette index)
	 * <p>
	 * For bitdepth=1/2/4 , and if samplesUnpacked=false, each value is a PACKED
	 * byte!
	 * <p>
	 * To convert a indexed line to RGB balues, see
	 * <code>ImageLineHelper.palIdx2RGB()</code> (you can't do the reverse)
	 */
	public final int[] scanline;
	/**
	 * Same as {@link #scanline}, but with one byte per sample. Only one of
	 * scanline and scanlineb is valid - this depends on {@link #sampleType}
	 */
	public final byte[] scanlineb;

	protected FilterType filterUsed; // informational ; only filled by the reader
	final int channels; // copied from imgInfo, more handy
	final int bitDepth; // copied from imgInfo, more handy
	final int elementsPerRow; // = imgInfo.samplePerRowPacked, if packed:imgInfo.samplePerRow elswhere

	public enum SampleType {
		INT, // 4 bytes per sample
		// SHORT, // 2 bytes per sample
		BYTE // 1 byte per sample
	}

	/**
	 * tells if we are using BYTE or INT to store the samples.
	 */
	public final SampleType sampleType;

	/**
	 * true: each element of the scanline array represents a sample always, even
	 * for internally packed PNG formats
	 *
	 * false: if the original image was of packed type (bit depth less than 8)
	 * we keep samples packed in a single array element
	 */
	public final boolean samplesUnpacked;

	/**
	 * default mode: INT packed
	 */
	public ImageLine(final ImageInfo imgInfo) {
		this(imgInfo, SampleType.INT, false);
	}

	/**
	 *
	 * @param imgInfo
	 *            Inmutable ImageInfo, basic parameter of the image we are
	 *            reading or writing
	 * @param stype
	 *            INT or BYTE : this determines which scanline is the really
	 *            used one
	 * @param unpackedMode
	 *            If true, we use unpacked format, even for packed original
	 *            images
	 *
	 */
	public ImageLine(final ImageInfo imgInfo, final SampleType stype, final boolean unpackedMode) {
		this(imgInfo, stype, unpackedMode, null, null);
	}

	/**
	 * If a preallocated array is passed, the copy is shallow
	 */
	ImageLine(final ImageInfo imgInfo, final SampleType stype, final boolean unpackedMode, final int[] sci, final byte[] scb) {
		this.imgInfo = imgInfo;
		channels = imgInfo.channels;
		bitDepth = imgInfo.bitDepth;
		filterUsed = FilterType.FILTER_UNKNOWN;
		this.sampleType = stype;
		this.samplesUnpacked = unpackedMode || !imgInfo.packed;
		elementsPerRow = this.samplesUnpacked ? imgInfo.samplesPerRow : imgInfo.samplesPerRowPacked;
		if (stype == SampleType.INT) {
			scanline = sci != null ? sci : new int[elementsPerRow];
			scanlineb = null;
		} else if (stype == SampleType.BYTE) {
			scanlineb = scb != null ? scb : new byte[elementsPerRow];
			scanline = null;
		} else
			throw new PngjExceptionInternal("bad ImageLine initialization");
		this.rown = -1;
	}

	/** This row number inside the image (0 is top) */
	public int getRown() {
		return rown;
	}

	/** Sets row number (0 : Rows-1) */
	public void setRown(final int n) {
		this.rown = n;
	}

	/*
	 * Unpacks scanline (for bitdepth 1-2-4)
	 *
	 * Arrays must be prealocated. src : samplesPerRowPacked dst : samplesPerRow
	 *
	 * This usually works in place (with src==dst and length=samplesPerRow)!
	 *
	 * If not, you should only call this only when necesary (bitdepth <8)
	 *
	 * If <code>scale==true<code>, it scales the value (just a bit shift) towards 0-255.
	 */
	static void unpackInplaceInt(final ImageInfo iminfo, final int[] src, final int[] dst, final boolean scale) {
		final int bitDepth = iminfo.bitDepth;
		if (bitDepth >= 8)
			return; // nothing to do
		final int mask0 = ImageLineHelper.getMaskForPackedFormatsLs(bitDepth);
		final int scalefactor = 8 - bitDepth;
		final int offset0 = 8 * iminfo.samplesPerRowPacked - bitDepth * iminfo.samplesPerRow;
		int mask, offset, v;
		if (offset0 != 8) {
			mask = mask0 << offset0;
			offset = offset0; // how many bits to shift the mask to the right to recover mask0
		} else {
			mask = mask0;
			offset = 0;
		}
		for (int j = iminfo.samplesPerRow - 1, i = iminfo.samplesPerRowPacked - 1; j >= 0; j--) {
			v = (src[i] & mask) >> offset;
			if (scale)
				v <<= scalefactor;
			dst[j] = v;
			mask <<= bitDepth;
			offset += bitDepth;
			if (offset == 8) {
				mask = mask0;
				offset = 0;
				i--;
			}
		}
	}

	/*
	 * Unpacks scanline (for bitdepth 1-2-4)
	 *
	 * Arrays must be prealocated. src : samplesPerRow dst : samplesPerRowPacked
	 *
	 * This usually works in place (with src==dst and length=samplesPerRow)! If not, you should only call this only when
	 * necesary (bitdepth <8)
	 *
	 * The trailing elements are trash
	 *
	 *
	 * If <code>scale==true<code>, it scales the value (just a bit shift) towards 0-255.
	 */
	static void packInplaceInt(final ImageInfo iminfo, final int[] src, final int[] dst, final boolean scaled) {
		final int bitDepth = iminfo.bitDepth;
		if (bitDepth >= 8)
			return; // nothing to do
		final int mask0 = ImageLineHelper.getMaskForPackedFormatsLs(bitDepth);
		final int scalefactor = 8 - bitDepth;
		final int offset0 = 8 - bitDepth;
		int v, v0;
		int offset = 8 - bitDepth;
		v0 = src[0]; // first value is special for in place
		dst[0] = 0;
		if (scaled)
			v0 >>= scalefactor;
		v0 = ((v0 & mask0) << offset);
		for (int i = 0, j = 0; j < iminfo.samplesPerRow; j++) {
			v = src[j];
			if (scaled)
				v >>= scalefactor;
			dst[i] |= ((v & mask0) << offset);
			offset -= bitDepth;
			if (offset < 0) {
				offset = offset0;
				i++;
				dst[i] = 0;
			}
		}
		dst[0] |= v0;
	}

	static void unpackInplaceByte(final ImageInfo iminfo, final byte[] src, final byte[] dst, final boolean scale) {
		final int bitDepth = iminfo.bitDepth;
		if (bitDepth >= 8)
			return; // nothing to do
		final int mask0 = ImageLineHelper.getMaskForPackedFormatsLs(bitDepth);
		final int scalefactor = 8 - bitDepth;
		final int offset0 = 8 * iminfo.samplesPerRowPacked - bitDepth * iminfo.samplesPerRow;
		int mask, offset, v;
		if (offset0 != 8) {
			mask = mask0 << offset0;
			offset = offset0; // how many bits to shift the mask to the right to recover mask0
		} else {
			mask = mask0;
			offset = 0;
		}
		for (int j = iminfo.samplesPerRow - 1, i = iminfo.samplesPerRowPacked - 1; j >= 0; j--) {
			v = (src[i] & mask) >> offset;
			if (scale)
				v <<= scalefactor;
			dst[j] = (byte) v;
			mask <<= bitDepth;
			offset += bitDepth;
			if (offset == 8) {
				mask = mask0;
				offset = 0;
				i--;
			}
		}
	}

	/**
	 * size original: samplesPerRow sizeFinal: samplesPerRowPacked (trailing
	 * elements are trash!)
	 **/
	static void packInplaceByte(final ImageInfo iminfo, final byte[] src, final byte[] dst, final boolean scaled) {
		final int bitDepth = iminfo.bitDepth;
		if (bitDepth >= 8)
			return; // nothing to do
		final int mask0 = ImageLineHelper.getMaskForPackedFormatsLs(bitDepth);
		final int scalefactor = 8 - bitDepth;
		final int offset0 = 8 - bitDepth;
		int v, v0;
		int offset = 8 - bitDepth;
		v0 = src[0]; // first value is special
		dst[0] = 0;
		if (scaled)
			v0 >>= scalefactor;
		v0 = ((v0 & mask0) << offset);
		for (int i = 0, j = 0; j < iminfo.samplesPerRow; j++) {
			v = src[j];
			if (scaled)
				v >>= scalefactor;
			dst[i] |= ((v & mask0) << offset);
			offset -= bitDepth;
			if (offset < 0) {
				offset = offset0;
				i++;
				dst[i] = 0;
			}
		}
		dst[0] |= v0;
	}

	/**
	 * Creates a new ImageLine similar to this, but unpacked
	 *
	 * The caller must be sure that the original was really packed
	 */
	public ImageLine unpackToNewImageLine() {
		final ImageLine newline = new ImageLine(imgInfo, sampleType, true);
		if (sampleType == SampleType.INT)
			unpackInplaceInt(imgInfo, scanline, newline.scanline, false);
		else
			unpackInplaceByte(imgInfo, scanlineb, newline.scanlineb, false);
		return newline;
	}

	/**
	 * Creates a new ImageLine similar to this, but packed
	 *
	 * The caller must be sure that the original was really unpacked
	 */
	public ImageLine packToNewImageLine() {
		final ImageLine newline = new ImageLine(imgInfo, sampleType, false);
		if (sampleType == SampleType.INT)
			packInplaceInt(imgInfo, scanline, newline.scanline, false);
		else
			packInplaceByte(imgInfo, scanlineb, newline.scanlineb, false);
		return newline;
	}

	public FilterType getFilterUsed() {
		return filterUsed;
	}

	public void setFilterUsed(final FilterType ft) {
		filterUsed = ft;
	}

	public int[] getScanlineInt() {
		return scanline;
	}

	public byte[] getScanlineByte() {
		return scanlineb;
	}

	/**
	 * Basic info
	 */
	@Override
	public String toString() {
		return "row=" + rown + " cols=" + imgInfo.cols + " bpc=" + imgInfo.bitDepth + " size=" + scanline.length;
	}

	/**
	 * Prints some statistics - just for debugging
	 */
	public static void showLineInfo(final ImageLine line) {
		System.out.println(line);
		final ImageLineStats stats = new ImageLineHelper.ImageLineStats(line);
		System.out.println(stats);
		System.out.println(ImageLineHelper.infoFirstLastPixels(line));
	}

}
