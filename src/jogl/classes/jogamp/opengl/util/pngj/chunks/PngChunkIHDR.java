package jogamp.opengl.util.pngj.chunks;

import java.io.ByteArrayInputStream;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelperInternal;
import jogamp.opengl.util.pngj.PngjException;


/**
 * IHDR chunk.
 * <p>
 * see http://www.w3.org/TR/PNG/#11IHDR
 * <p>
 * This is a special critical Chunk.
 */
public class PngChunkIHDR extends PngChunkSingle {
	public final static String ID = ChunkHelper.IHDR;

	private int cols;
	private int rows;
	private int bitspc;
	private int colormodel;
	private int compmeth;
	private int filmeth;
	private int interlaced;

	// http://www.w3.org/TR/PNG/#11IHDR
	//
	public PngChunkIHDR(final ImageInfo info) {
		super(ID, info);
	}

	@Override
	public ChunkOrderingConstraint getOrderingConstraint() {
		return ChunkOrderingConstraint.NA;
	}

	@Override
	public ChunkRaw createRawChunk() {
		final ChunkRaw c = new ChunkRaw(13, ChunkHelper.b_IHDR, true);
		int offset = 0;
		PngHelperInternal.writeInt4tobytes(cols, c.data, offset);
		offset += 4;
		PngHelperInternal.writeInt4tobytes(rows, c.data, offset);
		offset += 4;
		c.data[offset++] = (byte) bitspc;
		c.data[offset++] = (byte) colormodel;
		c.data[offset++] = (byte) compmeth;
		c.data[offset++] = (byte) filmeth;
		c.data[offset++] = (byte) interlaced;
		return c;
	}

	@Override
	public void parseFromRaw(final ChunkRaw c) {
		if (c.len != 13)
			throw new PngjException("Bad IDHR len " + c.len);
		final ByteArrayInputStream st = c.getAsByteStream();
		cols = PngHelperInternal.readInt4(st);
		rows = PngHelperInternal.readInt4(st);
		// bit depth: number of bits per channel
		bitspc = PngHelperInternal.readByte(st);
		colormodel = PngHelperInternal.readByte(st);
		compmeth = PngHelperInternal.readByte(st);
		filmeth = PngHelperInternal.readByte(st);
		interlaced = PngHelperInternal.readByte(st);
	}

	@Override
	public void cloneDataFromRead(final PngChunk other) {
		final PngChunkIHDR otherx = (PngChunkIHDR) other;
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

	public void setCols(final int cols) {
		this.cols = cols;
	}

	public int getRows() {
		return rows;
	}

	public void setRows(final int rows) {
		this.rows = rows;
	}

	public int getBitspc() {
		return bitspc;
	}

	public void setBitspc(final int bitspc) {
		this.bitspc = bitspc;
	}

	public int getColormodel() {
		return colormodel;
	}

	public void setColormodel(final int colormodel) {
		this.colormodel = colormodel;
	}

	public int getCompmeth() {
		return compmeth;
	}

	public void setCompmeth(final int compmeth) {
		this.compmeth = compmeth;
	}

	public int getFilmeth() {
		return filmeth;
	}

	public void setFilmeth(final int filmeth) {
		this.filmeth = filmeth;
	}

	public int getInterlaced() {
		return interlaced;
	}

	public void setInterlaced(final int interlaced) {
		this.interlaced = interlaced;
	}
}
