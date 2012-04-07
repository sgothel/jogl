package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayInputStream;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjException;


/**
 * this is a special chunk!
 */
public class PngChunkIHDR extends PngChunk {
	private int cols;
	private int rows;
	private int bitspc;
	private int colormodel;
	private int compmeth;
	private int filmeth;
	private int interlaced;

	// http://www.w3.org/TR/PNG/#11IHDR
	//
	public PngChunkIHDR(ImageInfo info) {
		super(ChunkHelper.IHDR, info);
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = new ChunkRaw(13, ChunkHelper.b_IHDR, true);
		int offset = 0;
		PngHelper.writeInt4tobytes(cols, c.data, offset);
		offset += 4;
		PngHelper.writeInt4tobytes(rows, c.data, offset);
		offset += 4;
		c.data[offset++] = (byte) bitspc;
		c.data[offset++] = (byte) colormodel;
		c.data[offset++] = (byte) compmeth;
		c.data[offset++] = (byte) filmeth;
		c.data[offset++] = (byte) interlaced;
		return c;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		if (c.len != 13)
			throw new PngjException("Bad IDHR len " + c.len);
		ByteArrayInputStream st = c.getAsByteStream();
		cols = PngHelper.readInt4(st);
		rows = PngHelper.readInt4(st);
		// bit depth: number of bits per channel
		bitspc = PngHelper.readByte(st);
		colormodel = PngHelper.readByte(st);
		compmeth = PngHelper.readByte(st);
		filmeth = PngHelper.readByte(st);
		interlaced = PngHelper.readByte(st);
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkIHDR otherx = (PngChunkIHDR) other;
		cols = otherx.cols;
		rows = otherx.rows;
		bitspc = otherx.bitspc;
		colormodel = otherx.colormodel;
		compmeth = otherx.compmeth;
		filmeth = otherx.filmeth;
		interlaced = otherx.interlaced;
	}

	public int getCols() {
		return cols;
	}

	public void setCols(int cols) {
		this.cols = cols;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public int getBitspc() {
		return bitspc;
	}

	public void setBitspc(int bitspc) {
		this.bitspc = bitspc;
	}

	public int getColormodel() {
		return colormodel;
	}

	public void setColormodel(int colormodel) {
		this.colormodel = colormodel;
	}

	public int getCompmeth() {
		return compmeth;
	}

	public void setCompmeth(int compmeth) {
		this.compmeth = compmeth;
	}

	public int getFilmeth() {
		return filmeth;
	}

	public void setFilmeth(int filmeth) {
		this.filmeth = filmeth;
	}

	public int getInterlaced() {
		return interlaced;
	}

	public void setInterlaced(int interlaced) {
		this.interlaced = interlaced;
	}
}
