package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;

/**
 * hIST chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11hIST <br>
 * only for palette images
 */
public class PngChunkHIST extends PngChunkSingle {
	public final static String ID = ChunkHelper.hIST;

	private int[] hist = new int[0]; // should have same lenght as palette

	public PngChunkHIST(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.AFTER_PLTE_BEFORE_IDAT;
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		if (!imgInfo.indexed)
			throw new PngjException("only indexed images accept a HIST chunk");
		final int nentries = c.data.length / 2;
		hist = new int[nentries];
		for (int i = 0; i < hist.length; i++) {
			hist[i] = PngHelperInternal.readInt2fromBytes(c.data, i * 2);
		}
	}

	@Override
	public ChunkRaw createRawChunk() {
		if (!imgInfo.indexed)
			throw new PngjException("only indexed images accept a HIST chunk");
		ChunkRaw c = null;
		c = createEmptyChunk(hist.length * 2, true);
		for (int i = 0; i < hist.length; i++) {
			PngHelperInternal.writeInt2tobytes(hist[i], c.data, i * 2);
		}
		return c;
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkHIST otherx = (PngChunkHIST) other;
		hist = new int[otherx.hist.length];
		System.arraycopy(otherx.hist, 0, hist, 0, otherx.hist.length);
	}

	public int[] getHist() {
		return hist;
	}

	public void setHist(final int[] hist) {
		this.hist = hist;
	}

}
