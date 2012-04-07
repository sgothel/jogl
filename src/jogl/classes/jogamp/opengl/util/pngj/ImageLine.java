package jogamp.opengl.util.pngj;

import java.util.Arrays;

/**
 * Lightweight wrapper for an image scanline, used for read and write.
 * <p>
 * This object can be (usually it is) reused while iterating over the image lines.
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
	 * The 'scanline' is an array of integers, corresponds to an image line (row).
	 * <p>
	 * Except for 'packed' formats (gray/indexed with 1-2-4 bitdepth) each int is a "sample" (one for channel), (0-255
	 * or 0-65535) in the respective PNG sequence sequence : (R G B R G B...) or (R G B A R G B A...) or (g g g ...) or
	 * ( i i i) (palette index)
	 * <p>
	 * For bitdepth 1/2/4 , each element is a PACKED byte! To get an unpacked copy, see <code>tf_pack()</code> and its
	 * inverse <code>tf_unpack()</code>
	 * <p>
	 * To convert a indexed line to RGB balues, see <code>ImageLineHelper.tf_palIdx2RGB()</code> (can't do the reverse)
	 */
	public final int[] scanline; // see explanation above!!

	protected FilterType filterUsed; // informational ; only filled by the reader
	public final int channels; // copied from imgInfo, more handy
	public final int bitDepth; // copied from imgInfo, more handy

	public ImageLine(ImageInfo imgInfo) {
		this.imgInfo = imgInfo;
		channels = imgInfo.channels;
		scanline = new int[imgInfo.samplesPerRowP];
		this.bitDepth = imgInfo.bitDepth;
	}

	/** This row number inside the image (0 is top) */
	public int getRown() {
		return rown;
	}

	/** Increments row number */
	public void incRown() {
		this.rown++;
	}

	/** Sets row number */
	public void setRown(int n) {
		this.rown = n;
	}

	/** Sets scanline, making copy from passed array */
	public void setScanLine(int[] b) {
		System.arraycopy(b, 0, scanline, 0, scanline.length);
	}

	/**
	 * Returns a copy from scanline, in byte array.
	 * 
	 * You can (OPTIONALLY) pass an preallocated array to use.
	 **/
	public int[] getScanLineCopy(int[] b) {
		if (b == null || b.length < scanline.length)
			b = new int[scanline.length];
		System.arraycopy(scanline, 0, b, 0, scanline.length);
		return b;
	}

	/**
	 * Unpacks scanline (for bitdepth 1-2-4) into buffer.
	 * <p>
	 * You can (OPTIONALLY) pass an preallocated array to use.
	 * <p>
	 * If scale==TRUE scales the value (just a bit shift).
	 */
	public int[] tf_unpack(int[] buf, boolean scale) {
		int len = scanline.length;
		if (bitDepth == 1)
			len *= 8;
		else if (bitDepth == 2)
			len *= 4;
		else if (bitDepth == 4)
			len *= 2;
		if (buf == null)
			buf = new int[len];
		if (bitDepth >= 8)
			System.arraycopy(scanline, 0, buf, 0, scanline.length);
		else {
			int mask, offset, v;
			int mask0 = getMaskForPackedFormats();
			int offset0 = 8 - bitDepth;
			mask = mask0;
			offset = offset0;
			for (int i = 0, j = 0; i < len; i++) {
				v = (scanline[j] & mask) >> offset;
				if (scale)
					v <<= offset0;
				buf[i] = v;
				mask = mask >> bitDepth;
				offset -= bitDepth;
				if (mask == 0) { // new byte in source
					mask = mask0;
					offset = offset0;
					j++;
				}
			}
		}
		return buf;
	}

	/**
	 * Packs scanline (for bitdepth 1-2-4) from buffer.
	 * <p>
	 * If scale==TRUE scales the value (just a bit shift).
	 */
	public void tf_pack(int[] buf, boolean scale) { // writes scanline
		int len = scanline.length;
		if (bitDepth == 1)
			len *= 8;
		else if (bitDepth == 2)
			len *= 4;
		else if (bitDepth == 4)
			len *= 2;
		if (bitDepth >= 8)
			System.arraycopy(buf, 0, scanline, 0, scanline.length);
		else {
			int offset0 = 8 - bitDepth;
			int mask0 = getMaskForPackedFormats() >> offset0;
			int offset, v;
			offset = offset0;
			Arrays.fill(scanline, 0);
			for (int i = 0, j = 0; i < len; i++) {
				v = buf[i];
				if (scale)
					v >>= offset0;
				v = (v & mask0) << offset;
				scanline[j] |= v;
				offset -= bitDepth;
				if (offset < 0) { // new byte in scanline
					offset = offset0;
					j++;
				}
			}
		}
	}

	private int getMaskForPackedFormats() { // Utility function for pacj/unpack
		if (bitDepth == 1)
			return 0x80;
		if (bitDepth == 2)
			return 0xc0;
		if (bitDepth == 4)
			return 0xf0;
		throw new RuntimeException("?");
	}

	public FilterType getFilterUsed() {
		return filterUsed;
	}

	/**
	 * Basic info
	 */
	public String toString() {
		return "row=" + rown + " cols=" + imgInfo.cols + " bpc=" + imgInfo.bitDepth + " size=" + scanline.length;
	}
}
