package jogamp.opengl.util.pngj.chunks;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngjException;


// see http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html
public abstract class PngChunk {

	public final String id; // 4 letters
	public final boolean crit, pub, safe;
	private int lenori = -1; // merely informational, for read chunks

	private boolean writePriority = false; // for queued chunks
	protected final ImageInfo imgInfo;

	private int chunkGroup = -1; // chunk group where it was read or writen

	/**
	 * This static map defines which PngChunk class correspond to which ChunkID The client can add other chunks to this
	 * map statically, before reading
	 */
	public final static Map<String, Class<? extends PngChunk>> factoryMap = new HashMap<String, Class<? extends PngChunk>>();
	static {
		factoryMap.put(ChunkHelper.IDAT, PngChunkIDAT.class);
		factoryMap.put(ChunkHelper.IHDR, PngChunkIHDR.class);
		factoryMap.put(ChunkHelper.PLTE, PngChunkPLTE.class);
		factoryMap.put(ChunkHelper.IEND, PngChunkIEND.class);
		factoryMap.put(ChunkHelper.tEXt, PngChunkTEXT.class);
		factoryMap.put(ChunkHelper.iTXt, PngChunkITXT.class);
		factoryMap.put(ChunkHelper.zTXt, PngChunkZTXT.class);
		factoryMap.put(ChunkHelper.bKGD, PngChunkBKGD.class);
		factoryMap.put(ChunkHelper.gAMA, PngChunkGAMA.class);
		factoryMap.put(ChunkHelper.pHYs, PngChunkPHYS.class);
		factoryMap.put(ChunkHelper.iCCP, PngChunkICCP.class);
		factoryMap.put(ChunkHelper.tIME, PngChunkTIME.class);
		factoryMap.put(ChunkHelper.tRNS, PngChunkTRNS.class);
		factoryMap.put(ChunkHelper.cHRM, PngChunkCHRM.class);
		factoryMap.put(ChunkHelper.sBIT, PngChunkSBIT.class);
		factoryMap.put(ChunkHelper.sRGB, PngChunkSRGB.class);
		factoryMap.put(ChunkHelper.hIST, PngChunkHIST.class);
		factoryMap.put(ChunkHelper.sPLT, PngChunkSPLT.class);
	}

	protected PngChunk(String id, ImageInfo imgInfo) {
		this.id = id;
		this.imgInfo = imgInfo;
		this.crit = ChunkHelper.isCritical(id);
		this.pub = ChunkHelper.isPublic(id);
		this.safe = ChunkHelper.isSafeToCopy(id);
	}

	public abstract ChunkRaw createChunk();

	public abstract void parseFromChunk(ChunkRaw c);

	// override to make deep copy from read data to write
	public abstract void cloneDataFromRead(PngChunk other);

	@SuppressWarnings("unchecked")
	public static <T extends PngChunk> T cloneChunk(T chunk, ImageInfo info) {
		PngChunk cn = factoryFromId(chunk.id, info);
		if (cn.getClass() != chunk.getClass())
			throw new PngjException("bad class cloning chunk: " + cn.getClass() + " " + chunk.getClass());
		cn.cloneDataFromRead(chunk);
		return (T) cn;
	}

	public static PngChunk factory(ChunkRaw chunk, ImageInfo info) {
		PngChunk c = factoryFromId(ChunkHelper.toString(chunk.idbytes), info);
		c.lenori = chunk.len;
		c.parseFromChunk(chunk);
		return c;
	}

	public static PngChunk factoryFromId(String cid, ImageInfo info) {
		PngChunk chunk = null;
		try {
			Class<? extends PngChunk> cla = factoryMap.get(cid);
			if (cla != null) {
				Constructor<? extends PngChunk> constr = cla.getConstructor(ImageInfo.class);
				chunk = constr.newInstance(info);
			}
		} catch (Exception e) {
			// this can happend for unkown chunks
		}
		if (chunk == null)
			chunk = new PngChunkUNKNOWN(cid, info);
		return chunk;
	}

	protected ChunkRaw createEmptyChunk(int len, boolean alloc) {
		ChunkRaw c = new ChunkRaw(len, ChunkHelper.toBytes(id), alloc);
		return c;
	}

	@Override
	public String toString() {
		return "chunk id= " + id + " (" + lenori + ") c=" + getClass().getSimpleName();
	}

	void setPriority(boolean highPrioriy) {
		writePriority = highPrioriy;
	}

	void write(OutputStream os) {
		ChunkRaw c = createChunk();
		if (c == null)
			throw new PngjException("null chunk ! creation failed for " + this);
		c.writeChunk(os);
	}

	public boolean isWritePriority() {
		return writePriority;
	}

	/** must be overriden - only relevant for ancillary chunks */
	public boolean allowsMultiple() {
		return false; // override if allows multiple ocurrences
	}

	/** mustGoBeforeXX/After must be overriden - only relevant for ancillary chunks */
	public boolean mustGoBeforeIDAT() {
		return false;
	}

	public boolean mustGoBeforePLTE() {
		return false;
	}

	public boolean mustGoAfterPLTE() {
		return false;
	}

	static boolean isKnown(String id) {
		return factoryMap.containsKey(id);
	}

	public int getChunkGroup() {
		return chunkGroup;
	}

	public void setChunkGroup(int chunkGroup) {
		this.chunkGroup = chunkGroup;
	}

}
