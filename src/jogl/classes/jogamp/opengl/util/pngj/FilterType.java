package jogamp.opengl.util.pngj;

import java.util.HashMap;

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
	 * Default strategy: select one of the above filters depending on global
	 * image parameters
	 */
	FILTER_DEFAULT(-1),
	/**
	 * Aggressive strategy: select one of the above filters trying each of the
	 * filters (every 8 rows)
	 */
	FILTER_AGGRESSIVE(-2),
	/**
	 * Very aggressive strategy: select one of the above filters trying each of
	 * the filters (for every row!)
	 */
	FILTER_VERYAGGRESSIVE(-3),
	/**
	 * Uses all fiters, one for lines, cyciclally. Only for tests.
	 */
	FILTER_CYCLIC(-50),

	/**
	 * Not specified, placeholder for unknown or NA filters.
	 */
	FILTER_UNKNOWN(-100), ;
	public final int val;

	private FilterType(final int val) {
		this.val = val;
	}

	private static HashMap<Integer, FilterType> byVal;

	static {
		byVal = new HashMap<Integer, FilterType>();
		for (final FilterType ft : values()) {
			byVal.put(ft.val, ft);
		}
	}

	public static FilterType getByVal(final int i) {
		return byVal.get(i);
	}

}
