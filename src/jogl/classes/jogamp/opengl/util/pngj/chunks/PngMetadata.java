package jogamp.opengl.util.pngj.chunks;

import jogamp.opengl.util.pngj.PngHelper;
import jogamp.opengl.util.pngj.PngjException;

/**
 * We consider "image metadata" every info inside the image except for the most basic image info (IHDR chunk - ImageInfo
 * class) and the pixels values.
 * 
 * This includes the palette (if present) and all the ancillary chunks
 * 
 * This class provides a wrapper over the collection of chunks of a image (read or to write) and provides some high
 * level methods to access them
 * 
 */
public class PngMetadata {
	private final ChunkList chunkList;
	private final boolean readonly;

	public PngMetadata(ChunkList chunks, boolean readonly) {
		this.chunkList = chunks;
		this.readonly = readonly;
	}

	/**
	 * Queues the chunk at the writer
	 */
	public boolean setChunk(PngChunk c, boolean overwriteIfPresent) {
		if (readonly)
			throw new PngjException("cannot set chunk : readonly metadata");
		return chunkList.setChunk(c, overwriteIfPresent);
	}

	
	/**
	 * Returns only one chunk or null if nothing found - does not include queued chunks
	 *
	 * If more than one chunk (after filtering by inner id) is found, then an exception is thrown (failifMultiple=true)
	 * or the last one is returned (failifMultiple=false)
	 * 
	 * @param id Chunk id
	 * @param innerid if not null, the chunk is assumed to be PngChunkTextVar or PngChunkSPLT, and filtered by that 'internal id'
	 * @param failIfMultiple throw exception if more that one
	 * @return chunk (not cloned)
	 */
	public PngChunk getChunk1(String id, String innerid, boolean failIfMultiple) {
		return chunkList.getChunk1(id, innerid, failIfMultiple);
	}

	/**
	 *  Same as  getChunk1(id,  innerid=null, failIfMultiple=true);
	 */
	public PngChunk getChunk1(String id) {
		return chunkList.getChunk1(id);
	}

	// ///// high level utility methods follow ////////////

	// //////////// DPI

	/** 
	 * returns -1 if not found or dimension unknown 
	 **/
	public double[] getDpi() {
		PngChunk c = getChunk1(ChunkHelper.pHYs, null, true);
		if (c == null)
			return new double[] { -1, -1 };
		else
			return ((PngChunkPHYS) c).getAsDpi2();
	}

	public void setDpi(double x) {
		setDpi(x, x);
	}

	public void setDpi(double x, double y) {
		PngChunkPHYS c = new PngChunkPHYS(chunkList.imageInfo);
		c.setAsDpi2(x, y);
		setChunk(c, true);
	}

	// //////////// TIME

	public void setTimeNow(int secsAgo) {
		PngChunkTIME c = new PngChunkTIME(chunkList.imageInfo);
		c.setNow(secsAgo);
		setChunk(c, true);
	}

	public void setTimeYMDHMS(int yearx, int monx, int dayx, int hourx, int minx, int secx) {
		PngChunkTIME c = new PngChunkTIME(chunkList.imageInfo);
		c.setYMDHMS(yearx, monx, dayx, hourx, minx, secx);
		setChunk(c, true);
	}

	public String getTimeAsString() {
		PngChunk c = getChunk1(ChunkHelper.tIME, null, true);
		return c != null ? ((PngChunkTIME) c).getAsString() : "";
	}

	// //////////// TEXT

	public void setText(String k, String val, boolean useLatin1, boolean compress) {
		if (compress && !useLatin1)
			throw new PngjException("cannot compress non latin text");
		PngChunkTextVar c;
		if (useLatin1) {
			if (compress) {
				c = new PngChunkZTXT(chunkList.imageInfo);
			} else {
				c = new PngChunkTEXT(chunkList.imageInfo);
			}
		} else {
			c = new PngChunkITXT(chunkList.imageInfo);
			((PngChunkITXT) c).setLangtag(k); // we use the same orig tag (this is not quite right)
		}
		c.setKeyVal(k, val);
		setChunk(c, true);
	}

	public void setText(String k, String val) {
		setText(k, val, false, val.length() > 400);
	}

	/** tries all text chunks - returns null if not found */
	public String getTxtForKey(String k) {
		PngChunk c = getChunk1(ChunkHelper.tEXt, k, true);
		if (c == null)
			c = getChunk1(ChunkHelper.zTXt, k, true);
		if (c == null)
			c = getChunk1(ChunkHelper.iTXt, k, true);
		return c != null ? ((PngChunkTextVar) c).getVal() : null;
	}

}
