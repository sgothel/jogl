package jogamp.opengl.util.pngj.chunks;

import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngjExceptionInternal;

/**
 * Represents a instance of a PNG chunk.
 * <p>
 * See <a
 * href="http://www.libpng.org/pub/png/spec/1.2/PNG-Chunks.html">http://www
 * .libpng.org/pub/png/spec/1.2/PNG-Chunks .html</a> </a>
 * <p>
 * Concrete classes should extend {@link PngChunkSingle} or
 * {@link PngChunkMultiple}
 * <p>
 * Note that some methods/fields are type-specific (getOrderingConstraint(),
 * allowsMultiple()),<br>
 * some are 'almost' type-specific (id,crit,pub,safe; the exception is
 * PngUKNOWN), <br>
 * and the rest are instance-specific
 */
public abstract class PngChunk {

	/**
	 * Chunk-id: 4 letters
	 */
	public final String id;
	/**
	 * Autocomputed at creation time
	 */
	public final boolean crit, pub, safe;

	protected final ImageInfo imgInfo;

	/**
	 * Possible ordering constraint for a PngChunk type -only relevant for
	 * ancillary chunks. Theoretically, there could be more general constraints,
	 * but these cover the constraints for standard chunks.
	 */
	public enum ChunkOrderingConstraint {
		/**
		 * no ordering constraint
		 */
		NONE,
		/**
		 * Must go before PLTE (and hence, also before IDAT)
		 */
		BEFORE_PLTE_AND_IDAT,
		/**
		 * Must go after PLTE but before IDAT
		 */
		AFTER_PLTE_BEFORE_IDAT,
		/**
		 * Must before IDAT (before or after PLTE)
		 */
		BEFORE_IDAT,
		/**
		 * Does not apply
		 */
		NA;

		public boolean mustGoBeforePLTE() {
			return this == BEFORE_PLTE_AND_IDAT;
		}

		public boolean mustGoBeforeIDAT() {
			return this == BEFORE_IDAT || this == BEFORE_PLTE_AND_IDAT || this == AFTER_PLTE_BEFORE_IDAT;
		}

		public boolean mustGoAfterPLTE() {
			return this == AFTER_PLTE_BEFORE_IDAT;
		}
	}

	private boolean priority = false; // For writing. Queued chunks with high priority will be written as soon as
										// possible

	protected int chunkGroup = -1; // chunk group where it was read or writen
	protected int length = -1; // merely informational, for read chunks
	protected long offset = 0; // merely informational, for read chunks

	/**
	 * This static map defines which PngChunk class correspond to which ChunkID
	 * <p>
	 * The client can add other chunks to this map statically, before reading an
	 * image, calling PngChunk.factoryRegister(id,class)
	 */
	private final static Map<String, Class<? extends PngChunk>> factoryMap = new HashMap<String, Class<? extends PngChunk>>();
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
		// extended
		factoryMap.put(PngChunkOFFS.ID, PngChunkOFFS.class);
		factoryMap.put(PngChunkSTER.ID, PngChunkSTER.class);
	}

	/**
	 * Registers a chunk-id (4 letters) to be associated with a PngChunk class
	 * <p>
	 * This method should be called by user code that wants to add some chunks
	 * (not implmemented in this library) to the factory, so that the PngReader
	 * knows about it.
	 */
	public static void factoryRegister(final String chunkId, final Class<? extends PngChunk> chunkClass) {
		factoryMap.put(chunkId, chunkClass);
	}

	/**
	 * True if the chunk-id type is known.
	 * <p>
	 * A chunk is known if we recognize its class, according with
	 * <code>factoryMap</code>
	 * <p>
	 * This is not necessarily the same as being "STANDARD", or being
	 * implemented in this library
	 * <p>
	 * Unknown chunks will be parsed as instances of {@link PngChunkUNKNOWN}
	 */
	public static boolean isKnown(final String id) {
		return factoryMap.containsKey(id);
	}

	protected PngChunk(final String id, final ImageInfo imgInfo) {
		this.id = id;
		this.imgInfo = imgInfo;
		this.crit = ChunkHelper.isCritical(id);
		this.pub = ChunkHelper.isPublic(id);
		this.safe = ChunkHelper.isSafeToCopy(id);
	}

	/**
	 * This factory creates the corresponding chunk and parses the raw chunk.
	 * This is used when reading.
	 */
	public static PngChunk factory(final ChunkRaw chunk, final ImageInfo info) {
		final PngChunk c = factoryFromId(ChunkHelper.toString(chunk.idbytes), info);
		c.length = chunk.len;
		c.parseFromRaw(chunk);
		return c;
	}

	/**
	 * Creates one new blank chunk of the corresponding type, according to
	 * factoryMap (PngChunkUNKNOWN if not known)
	 */
	public static PngChunk factoryFromId(final String cid, final ImageInfo info) {
		PngChunk chunk = null;
		try {
			final Class<? extends PngChunk> cla = factoryMap.get(cid);
			if (cla != null) {
				final Constructor<? extends PngChunk> constr = cla.getConstructor(ImageInfo.class);
				chunk = constr.newInstance(info);
			}
		} catch (final Exception e) {
			// this can happen for unkown chunks
		}
		if (chunk == null)
			chunk = new PngChunkUNKNOWN(cid, info);
		return chunk;
	}

	protected final ChunkRaw createEmptyChunk(final int len, final boolean alloc) {
		final ChunkRaw c = new ChunkRaw(len, ChunkHelper.toBytes(id), alloc);
		return c;
	}

	/**
	 * Makes a clone (deep copy) calling {@link #cloneDataFromRead(PngChunk)}
	 */
	@SuppressWarnings("unchecked")
	public static <T extends PngChunk> T cloneChunk(final T chunk, final ImageInfo info) {
		final PngChunk cn = factoryFromId(chunk.id, info);
		if (cn.getClass() != chunk.getClass())
			throw new PngjExceptionInternal("bad class cloning chunk: " + cn.getClass() + " " + chunk.getClass());
		cn.cloneDataFromRead(chunk);
		return (T) cn;
	}

	/**
	 * In which "chunkGroup" (see {@link ChunksList}for definition) this chunks
	 * instance was read or written.
	 * <p>
	 * -1 if not read or written (eg, queued)
	 */
	final public int getChunkGroup() {
		return chunkGroup;
	}

	/**
	 * @see #getChunkGroup()
	 */
	final public void setChunkGroup(final int chunkGroup) {
		this.chunkGroup = chunkGroup;
	}

	public boolean hasPriority() {
		return priority;
	}

	public void setPriority(final boolean priority) {
		this.priority = priority;
	}

	final void write(final OutputStream os) {
		final ChunkRaw c = createRawChunk();
		if (c == null)
			throw new PngjExceptionInternal("null chunk ! creation failed for " + this);
		c.writeChunk(os);
	}

	public int getLength() {
		return length;
	}

	/*
	 * public void setLength(int length) { this.length = length; }
	 */

	public long getOffset() {
		return offset;
	}

	public void setOffset(final long offset) {
		this.offset = offset;
	}

	/**
	 * Creates the physical chunk. This is used when writing (serialization).
	 * Each particular chunk class implements its own logic.
	 *
	 * @return A newly allocated and filled raw chunk
	 */
	public abstract ChunkRaw createRawChunk();

	/**
	 * Parses raw chunk and fill inside data. This is used when reading
	 * (deserialization). Each particular chunk class implements its own logic.
	 */
	public abstract void parseFromRaw(ChunkRaw c);

	/**
	 * Makes a copy of the chunk.
	 * <p>
	 * This is used when copying chunks from a reader to a writer
	 * <p>
	 * It should normally be a deep copy, and after the cloning
	 * this.equals(other) should return true
	 */
	public abstract void cloneDataFromRead(PngChunk other);

	public abstract boolean allowsMultiple(); // this is implemented in PngChunkMultiple/PngChunSingle

	/**
	 * see {@link ChunkOrderingConstraint}
	 */
	public abstract ChunkOrderingConstraint getOrderingConstraint();

	@Override
	public String toString() {
		return "chunk id= " + id + " (len=" + length + " offset=" + offset + ") c=" + getClass().getSimpleName();
	}

}
