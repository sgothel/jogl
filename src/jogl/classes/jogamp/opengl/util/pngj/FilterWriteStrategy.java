package jogamp.opengl.util.pngj;

/**
 * Manages the writer strategy for selecting the internal png predictor filter
 */
class FilterWriteStrategy {
	private static final int COMPUTE_STATS_EVERY_N_LINES = 8;

	final ImageInfo imgInfo;
	public final FilterType configuredType; // can be negative (fin dout)
	private FilterType currentType; // 0-4
	private int lastRowTested = -1000000;
	// performance of each filter (less is better) (can be negative)
	private final double[] lastSums = new double[5];
	// performance of each filter (less is better) (can be negative)
	private final double[] lastEntropies = new double[5];
	// a priori preference (NONE SUB UP AVERAGE PAETH)
	private double[] preference = new double[] { 1.1, 1.1, 1.1, 1.1, 1.2 };
	private int discoverEachLines = -1;
	private final double[] histogram1 = new double[256];

	FilterWriteStrategy(final ImageInfo imgInfo, final FilterType configuredType) {
		this.imgInfo = imgInfo;
		this.configuredType = configuredType;
		if (configuredType.val < 0) { // first guess
			if ((imgInfo.rows < 8 && imgInfo.cols < 8) || imgInfo.indexed || imgInfo.bitDepth < 8)
				currentType = FilterType.FILTER_NONE;
			else
				currentType = FilterType.FILTER_PAETH;
		} else {
			currentType = configuredType;
		}
		if (configuredType == FilterType.FILTER_AGGRESSIVE)
			discoverEachLines = COMPUTE_STATS_EVERY_N_LINES;
		if (configuredType == FilterType.FILTER_VERYAGGRESSIVE)
			discoverEachLines = 1;
	}

	boolean shouldTestAll(final int rown) {
		if (discoverEachLines > 0 && lastRowTested + discoverEachLines <= rown) {
			currentType = null;
			return true;
		} else
			return false;
	}

	public void setPreference(final double none, final double sub, final double up, final double ave, final double paeth) {
		preference = new double[] { none, sub, up, ave, paeth };
	}

	public boolean computesStatistics() {
		return (discoverEachLines > 0);
	}

	void fillResultsForFilter(final int rown, final FilterType type, final double sum, final int[] histo, final boolean tentative) {
		lastRowTested = rown;
		lastSums[type.val] = sum;
		if (histo != null) {
			double v, alfa, beta, e;
			alfa = rown == 0 ? 0.0 : 0.3;
			beta = 1 - alfa;
			e = 0.0;
			for (int i = 0; i < 256; i++) {
				v = ((double) histo[i]) / imgInfo.cols;
				v = histogram1[i] * alfa + v * beta;
				if (tentative)
					e += v > 0.00000001 ? v * Math.log(v) : 0.0;
				else
					histogram1[i] = v;
			}
			lastEntropies[type.val] = (-e);
		}
	}

	FilterType gimmeFilterType(final int rown, final boolean useEntropy) {
		if (currentType == null) { // get better
			if (rown == 0)
				currentType = FilterType.FILTER_SUB;
			else {
				double bestval = Double.MAX_VALUE;
				double val;
				for (int i = 0; i < 5; i++) {
					val = useEntropy ? lastEntropies[i] : lastSums[i];
					val /= preference[i];
					if (val <= bestval) {
						bestval = val;
						currentType = FilterType.getByVal(i);
					}
				}
			}
		}
		if (configuredType == FilterType.FILTER_CYCLIC) {
			currentType = FilterType.getByVal((currentType.val + 1) % 5);
		}
		return currentType;
	}
}
