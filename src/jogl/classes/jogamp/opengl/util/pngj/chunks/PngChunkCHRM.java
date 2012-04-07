package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjException;

/*
 */
public class PngChunkCHRM extends PngChunk {
	// http://www.w3.org/TR/PNG/#11cHRM
	private double whitex, whitey;
	private double redx, redy;
	private double greenx, greeny;
	private double bluex, bluey;

	public PngChunkCHRM(ImageInfo info) {
		super(ChunkHelper.cHRM, info);
	}

	@Override
	public boolean mustGoBeforeIDAT() {
		return true;
	}

	@Override
	public boolean mustGoBeforePLTE() {
		return true;
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = null;
		c = createEmptyChunk(32, true);
		PngHelper.writeInt4tobytes(PngHelper.doubleToInt100000(whitex), c.data, 0);
		PngHelper.writeInt4tobytes(PngHelper.doubleToInt100000(whitey), c.data, 4);
		PngHelper.writeInt4tobytes(PngHelper.doubleToInt100000(redx), c.data, 8);
		PngHelper.writeInt4tobytes(PngHelper.doubleToInt100000(redy), c.data, 12);
		PngHelper.writeInt4tobytes(PngHelper.doubleToInt100000(greenx), c.data, 16);
		PngHelper.writeInt4tobytes(PngHelper.doubleToInt100000(greeny), c.data, 20);
		PngHelper.writeInt4tobytes(PngHelper.doubleToInt100000(bluex), c.data, 24);
		PngHelper.writeInt4tobytes(PngHelper.doubleToInt100000(bluey), c.data, 28);
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		if (c.len != 32)
			throw new PngjException("bad chunk " + c);
		whitex = PngHelper.intToDouble100000(PngHelper.readInt4fromBytes(c.data, 0));
		whitey = PngHelper.intToDouble100000(PngHelper.readInt4fromBytes(c.data, 4));
		redx = PngHelper.intToDouble100000(PngHelper.readInt4fromBytes(c.data, 8));
		redy = PngHelper.intToDouble100000(PngHelper.readInt4fromBytes(c.data, 12));
		greenx = PngHelper.intToDouble100000(PngHelper.readInt4fromBytes(c.data, 16));
		greeny = PngHelper.intToDouble100000(PngHelper.readInt4fromBytes(c.data, 20));
		bluex = PngHelper.intToDouble100000(PngHelper.readInt4fromBytes(c.data, 24));
		bluey = PngHelper.intToDouble100000(PngHelper.readInt4fromBytes(c.data, 28));
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkCHRM otherx = (PngChunkCHRM) other;
		whitex = otherx.whitex;
		whitey = otherx.whitex;
		redx = otherx.redx;
		redy = otherx.redy;
		greenx = otherx.greenx;
		greeny = otherx.greeny;
		bluex = otherx.bluex;
		bluey = otherx.bluey;
	}

	public void setChromaticities(double whitex, double whitey, double redx, double redy, double greenx, double greeny,
			double bluex, double bluey) {
		this.whitex = whitex;
		this.redx = redx;
		this.greenx = greenx;
		this.bluex = bluex;
		this.whitey = whitey;
		this.redy = redy;
		this.greeny = greeny;
		this.bluey = bluey;
	}

	public double[] getChromaticities() {
		return new double[] { whitex, whitey, redx, redy, greenx, greeny, bluex, bluey };
	}

}
