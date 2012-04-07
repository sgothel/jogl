package jogamp.opengl.util.pngj;

/**
 * Internal PNG predictor filter, or strategy to select it.
 * 
 */
public enum FilterType {
	/**
	 * No filter.
	 */
	FILTER_NONE(0),
	/**
	 * SUB filter (uses same row)
	 */
	FILTER_SUB(1),
	/**
	 * UP filter (uses previous row)
	 */
	FILTER_UP(2),
	/**
	 * AVERAGE filter
	 */
	FILTER_AVERAGE(3),
	/**
	 * PAETH predictor
	 */
	FILTER_PAETH(4),
	/**
	 * Default strategy: select one of the above filters depending on global image parameters
	 */
	FILTER_DEFAULT(-1),
	/**
	 * Aggresive strategy: select one of the above filters trying each of the filters (this is done every 8 rows)
	 */
	FILTER_AGGRESSIVE(-2),
	/**
	 * Uses all fiters, one for lines, cyciclally. Only for tests.
	 */
	FILTER_ALTERNATE(-3),
	/**
	 * Aggresive strategy: select one of the above filters trying each of the filters (this is done for every row!)
	 */
	FILTER_VERYAGGRESSIVE(-4), ;
	public final int val;

	private FilterType(int val) {
		this.val = val;
	}

	public static FilterType getByVal(int i) {
		for (FilterType ft : values()) {
			if (ft.val == i)
				return ft;
		}
		return null;
	}

	public static int unfilterRowNone(int r) {
		return (int) (r & 0xFF);
	}

	public static int unfilterRowSub(int r, int left) {
		return ((int) (r + left) & 0xFF);
	}

	public static int unfilterRowUp(int r, int up) {
		return ((int) (r + up) & 0xFF);
	}

	public static int unfilterRowAverage(int r, int left, int up) {
		return (r + (left + up) / 2) & 0xFF;
	}

	public static int unfilterRowPaeth(int r, int a, int b, int c) { // a = left, b = above, c = upper left
		return (r + filterPaethPredictor(a, b, c)) & 0xFF;
	}

	public static int filterPaethPredictor(int a, int b, int c) {
		// from http://www.libpng.org/pub/png/spec/1.2/PNG-Filters.html
		// a = left, b = above, c = upper left
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
}
