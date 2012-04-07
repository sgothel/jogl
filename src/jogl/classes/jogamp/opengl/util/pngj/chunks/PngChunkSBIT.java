package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjException;

/*
 */
public class PngChunkSBIT extends PngChunk {
	// http://www.w3.org/TR/PNG/#11sBIT
	// this chunk structure depends on the image type

	// significant bits
	private int graysb, alphasb;
	private int redsb, greensb, bluesb;

	public PngChunkSBIT(ImageInfo info) {
		super(ChunkHelper.sBIT, info);
	}

	@Override
	public boolean mustGoBeforeIDAT() {
		return true;
	}

	@Override
	public boolean mustGoBeforePLTE() {
		return true;
	}

	private int getLen() {
		int len = imgInfo.greyscale ? 1 : 3;
		if (imgInfo.alpha)
			len += 1;
		return len;
	}

	@Override
	public void parseFromChunk(ChunkRaw c) {
		if (c.len != getLen())
			throw new PngjException("bad chunk length " + c);
		if (imgInfo.greyscale) {
			graysb = PngHelper.readInt1fromByte(c.data, 0);
			if (imgInfo.alpha)
				alphasb = PngHelper.readInt1fromByte(c.data, 1);
		} else {
			redsb = PngHelper.readInt1fromByte(c.data, 0);
			greensb = PngHelper.readInt1fromByte(c.data, 1);
			bluesb = PngHelper.readInt1fromByte(c.data, 2);
			if (imgInfo.alpha)
				alphasb = PngHelper.readInt1fromByte(c.data, 3);
		}
	}

	@Override
	public ChunkRaw createChunk() {
		ChunkRaw c = null;
		c = createEmptyChunk(getLen(), true);
		if (imgInfo.greyscale) {
			c.data[0] = (byte) graysb;
			if (imgInfo.alpha)
				c.data[1] = (byte) alphasb;
		} else {
			c.data[0] = (byte) redsb;
			c.data[1] = (byte) greensb;
			c.data[2] = (byte) bluesb;
			if (imgInfo.alpha)
				c.data[3] = (byte) alphasb;
		}
		return c;
	}

	@Override
	public void cloneDataFromRead(PngChunk other) {
		PngChunkSBIT otherx = (PngChunkSBIT) other;
		graysb = otherx.graysb;
		redsb = otherx.redsb;
		greensb = otherx.greensb;
		bluesb = otherx.bluesb;
		alphasb = otherx.alphasb;
	}

	public void setGraysb(int gray) {
		if (!imgInfo.greyscale)
			throw new PngjException("only greyscale images support this");
		graysb = gray;
	}

	public int getGraysb() {
		if (!imgInfo.greyscale)
			throw new PngjException("only greyscale images support this");
		return graysb;
	}

	public void setAlphasb(int a) {
		if (!imgInfo.alpha)
			throw new PngjException("only images with alpha support this");
		alphasb = a;
	}

	public int getAlphasb() {
		if (!imgInfo.alpha)
			throw new PngjException("only images with alpha support this");
		return alphasb;
	}

	/**
	 * Set rgb values
	 * 
	 */
	public void setRGB(int r, int g, int b) {
		if (imgInfo.greyscale || imgInfo.indexed)
			throw new PngjException("only rgb or rgba images support this");
		redsb = r;
		greensb = g;
		bluesb = b;
	}

	public int[] getRGB() {
		if (imgInfo.greyscale || imgInfo.indexed)
			throw new PngjException("only rgb or rgba images support this");
		return new int[] { redsb, greensb, bluesb };
	}
}
