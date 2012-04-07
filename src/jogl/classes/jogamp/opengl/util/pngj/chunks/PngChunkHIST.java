package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjException;

/*
 */
public class PngChunkHIST extends PngChunk {
	// http://www.w3.org/TR/PNG/#11hIST
	// only for palette images

	private int[] hist = new int[0]; // should have same lenght as palette

	public PngChunkHIST(ImageInfo info) {
		super(ChunkHelper.hIST, info);
	}

	@Override
	public boolean mustGoBeforeIDAT() {
		return true;
	}

	@Override
	public boolean mustGoAfterPLTE() {
		return true;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		if (!imgInfo.indexed)
			throw new PngjException("only indexed images accept a HIST chunk");
		int nentries = c.data.length / 2;
		hist = new int[nentries];
		for (int i = 0; i < hist.length; i++) {
			hist[i] = PngHelper.readInt2fromBytes(c.data, i * 2);
		}
	}

	@Override
	public ChunkRaw createChunk() {
		if (!imgInfo.indexed)
			throw new PngjException("only indexed images accept a HIST chunk");
		ChunkRaw c = null;
		c = createEmptyChunk(hist.length * 2, true);
		for (int i = 0; i < hist.length; i++) {
			PngHelper.writeInt2tobytes(hist[i], c.data, i * 2);
		}
		return c;
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkHIST otherx = (PngChunkHIST) other;
		hist = new int[otherx.hist.length];
		System.arraycopy(otherx.hist, 0, hist, 0, otherx.hist.length);
	}

	public int[] getHist() {
		return hist;
	}

	public void setHist(int[] hist) {
		this.hist = hist;
	}

}
