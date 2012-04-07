package jogamp.opengl.util.pngj;

/**
 * Simple immutable wrapper for basic image info.
 * <p>
 * Some parameters are redundant, but the constructor receives an 'orthogonal' subset.
 * <p>
 * ref: http://www.w3.org/TR/PNG/#11IHDR
 */
public class ImageInfo {

	// very big value ; actually we are ok with 2**22=4M, perhaps even more
	private static final int MAX_COLS_ROWS_VAL = 1000000;

	/**
	 * Image width, in pixels.
	 */
	public final int cols;

	/**
	 * Image height, in pixels
	 */
	public final int rows;

	/**
	 * Bits per sample (per channel) in the buffer (1-2-4-8-16). This is 8-16 for RGB/ARGB images, 1-2-4-8 for
	 * grayscale. For indexed images, number of bits per palette index (1-2-4-8)
	 */
	public final int bitDepth;

	/**
	 * Number of channels, as used internally. This is 3 for RGB, 4 for RGBA, 2 for GA (gray with alpha), 1 for
	 * grayscales or indexed.
	 */
	public final int channels;

	/**
	 * Flag: true if has alpha channel (RGBA/GA)
	 */
	public final boolean alpha;

	/**
	 * Flag: true if is grayscale (G/GA)
	 */
	public final boolean greyscale;

	/**
	 * Flag: true if image is indexed, i.e., it has a palette
	 */
	public final boolean indexed;

	/**
	 * Flag: true if image internally uses less than one byte per sample (bit depth 1-2-4)
	 */
	public final boolean packed;

	/**
	 * Bits used for each pixel in the buffer: channel * bitDepth
	 */
	public final int bitspPixel;

	/**
	 * rounded up value: this is only used internally for filter
	 */
	public final int bytesPixel;

	/**
	 * ceil(bitspp*cols/8)
	 */
	public final int bytesPerRow;

	/**
	 * Equals cols * channels
	 */
	public final int samplesPerRow;

	/**
	 * For internal use only. Samples available for our packed scanline. Equals samplesPerRow if not packed. Elsewhere,
	 * it's lower
	 */
	final int samplesPerRowP;

	/**
	 * Short constructor: assumes truecolor (RGB/RGBA)
	 */
	public ImageInfo(int cols, int rows, int bitdepth, boolean alpha) {
		this(cols, rows, bitdepth, alpha, false, false);
	}

	/**
	 * Full constructor
	 * 
	 * @param cols
	 *            Width in pixels
	 * @param rows
	 *            Height in pixels
	 * @param bitdepth
	 *            Bits per sample, in the buffer : 8-16 for RGB true color and greyscale
	 * @param alpha
	 *            Flag: has an alpha channel (RGBA or GA)
	 * @param grayscale
	 *            Flag: is gray scale (any bitdepth, with or without alpha)
	 * @param indexed
	 *            Flag: has palette
	 */
	public ImageInfo(int cols, int rows, int bitdepth, boolean alpha, boolean grayscale, boolean indexed) {
		this.cols = cols;
		this.rows = rows;
		this.alpha = alpha;
		this.indexed = indexed;
		this.greyscale = grayscale;
		if (greyscale && indexed)
			throw new PngjException("palette and greyscale are mutually exclusive");
		this.channels = (grayscale || indexed) ? (alpha ? 2 : 1) : (alpha ? 4 : 3);
		// http://www.w3.org/TR/PNG/#11IHDR
		this.bitDepth = bitdepth;
		this.packed = bitdepth < 8;
		this.bitspPixel = (channels * this.bitDepth);
		this.bytesPixel = (bitspPixel + 7) / 8;
		this.bytesPerRow = (bitspPixel * cols + 7) / 8;
		this.samplesPerRow = channels * this.cols;
		this.samplesPerRowP = packed ? bytesPerRow : samplesPerRow;
		// several checks
		switch (this.bitDepth) {
		case 1:
		case 2:
		case 4:
			if (!(this.indexed || this.greyscale))
				throw new PngjException("only indexed or grayscale can have bitdepth=" + this.bitDepth);
			break;
		case 8:
			break;
		case 16:
			if (this.indexed)
				throw new PngjException("indexed can't have bitdepth=" + this.bitDepth);
			break;
		default:
			throw new PngjException("invalid bitdepth=" + this.bitDepth);
		}
		if (cols < 1 || cols > MAX_COLS_ROWS_VAL)
			throw new PngjException("invalid cols=" + cols + " ???");
		if (rows < 1 || rows > MAX_COLS_ROWS_VAL)
			throw new PngjException("invalid rows=" + rows + " ???");
	}

	@Override
	public String toString() {
		return "ImageInfo [cols=" + cols + ", rows=" + rows + ", bitDepth=" + bitDepth + ", channels=" + channels
				+ ", bitspPixel=" + bitspPixel + ", bytesPixel=" + bytesPixel + ", bytesPerRow=" + bytesPerRow
				+ ", samplesPerRow=" + samplesPerRow + ", samplesPerRowP=" + samplesPerRowP + ", alpha=" + alpha
				+ ", greyscale=" + greyscale + ", indexed=" + indexed + ", packed=" + packed + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (alpha ? 1231 : 1237);
		result = prime * result + bitDepth;
		result = prime * result + bitspPixel;
		result = prime * result + bytesPerRow;
		result = prime * result + bytesPixel;
		result = prime * result + channels;
		result = prime * result + cols;
		result = prime * result + (greyscale ? 1231 : 1237);
		result = prime * result + (indexed ? 1231 : 1237);
		result = prime * result + (packed ? 1231 : 1237);
		result = prime * result + rows;
		result = prime * result + samplesPerRow;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageInfo other = (ImageInfo) obj;
		if (alpha != other.alpha)
			return false;
		if (bitDepth != other.bitDepth)
			return false;
		if (bitspPixel != other.bitspPixel)
			return false;
		if (bytesPerRow != other.bytesPerRow)
			return false;
		if (bytesPixel != other.bytesPixel)
			return false;
		if (channels != other.channels)
			return false;
		if (cols != other.cols)
			return false;
		if (greyscale != other.greyscale)
			return false;
		if (indexed != other.indexed)
			return false;
		if (packed != other.packed)
			return false;
		if (rows != other.rows)
			return false;
		if (samplesPerRow != other.samplesPerRow)
			return false;
		return true;
	}
}
