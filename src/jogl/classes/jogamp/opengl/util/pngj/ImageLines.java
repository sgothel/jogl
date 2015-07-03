package jogamp.opengl.util.pngj;

import jogamp.opengl.util.pngj.ImageLine.SampleType;

/**
 * Wraps in a matrix a set of image rows, not necessarily contiguous - but
 * equispaced.
 *
 * The fields mirrors those of {@link ImageLine}, and you can access each row as
 * a ImageLine backed by the matrix row, see
 * {@link #getImageLineAtMatrixRow(int)}
 */
public class ImageLines {

	public final ImageInfo imgInfo;
	public final int channels;
	public final int bitDepth;
	public final SampleType sampleType;
	public final boolean samplesUnpacked;
	public final int elementsPerRow;
	public final int rowOffset;
	public final int nRows;
	public final int rowStep;
	public final int[][] scanlines;
	public final byte[][] scanlinesb;

	/**
	 * Allocates a matrix to store {@code nRows} image rows. See
	 * {@link ImageLine} and {@link PngReader#readRowsInt()}
	 * {@link PngReader#readRowsByte()}
	 *
	 * @param imgInfo
	 * @param stype
	 * @param unpackedMode
	 * @param rowOffset
	 * @param nRows
	 * @param rowStep
	 */
	public ImageLines(final ImageInfo imgInfo, final SampleType stype, final boolean unpackedMode, final int rowOffset, final int nRows, final int rowStep) {
		this.imgInfo = imgInfo;
		channels = imgInfo.channels;
		bitDepth = imgInfo.bitDepth;
		this.sampleType = stype;
		this.samplesUnpacked = unpackedMode || !imgInfo.packed;
		elementsPerRow = unpackedMode ? imgInfo.samplesPerRow : imgInfo.samplesPerRowPacked;
		this.rowOffset = rowOffset;
		this.nRows = nRows;
		this.rowStep = rowStep;
		if (stype == SampleType.INT) {
			scanlines = new int[nRows][elementsPerRow];
			scanlinesb = null;
		} else if (stype == SampleType.BYTE) {
			scanlinesb = new byte[nRows][elementsPerRow];
			scanlines = null;
		} else
			throw new PngjExceptionInternal("bad ImageLine initialization");
	}

	/**
	 * Warning: this always returns a valid matrix row (clamping on 0 : nrows-1,
	 * and rounding down) Eg: rowOffset=4,rowStep=2 imageRowToMatrixRow(17)
	 * returns 6 , imageRowToMatrixRow(1) returns 0
	 */
	public int imageRowToMatrixRow(final int imrow) {
		final int r = (imrow - rowOffset) / rowStep;
		return r < 0 ? 0 : (r < nRows ? r : nRows - 1);
	}

	/**
	 * Same as imageRowToMatrixRow, but returns negative if invalid
	 */
	public int imageRowToMatrixRowStrict(int imrow) {
		imrow -= rowOffset;
		final int mrow = imrow >= 0 && imrow % rowStep == 0 ? imrow / rowStep : -1;
		return mrow < nRows ? mrow : -1;
	}

	/**
	 * Converts from matrix row number (0 : nRows-1) to image row number
	 *
	 * @param mrow
	 *            Matrix row number
	 * @return Image row number. Invalid only if mrow is invalid
	 */
	public int matrixRowToImageRow(final int mrow) {
		return mrow * rowStep + rowOffset;
	}

	/**
	 * Returns a ImageLine is backed by the matrix, no allocation done
	 *
	 * @param mrow
	 *            Matrix row, from 0 to nRows This is not necessarily the image
	 *            row, see {@link #imageRowToMatrixRow(int)} and
	 *            {@link #matrixRowToImageRow(int)}
	 * @return A new ImageLine, backed by the matrix, with the correct ('real')
	 *         rownumber
	 */
	public ImageLine getImageLineAtMatrixRow(final int mrow) {
		if (mrow < 0 || mrow > nRows)
			throw new PngjException("Bad row " + mrow + ". Should be positive and less than " + nRows);
		final ImageLine imline = sampleType == SampleType.INT ? new ImageLine(imgInfo, sampleType, samplesUnpacked,
				scanlines[mrow], null) : new ImageLine(imgInfo, sampleType, samplesUnpacked, null, scanlinesb[mrow]);
		imline.setRown(matrixRowToImageRow(mrow));
		return imline;
	}
}
