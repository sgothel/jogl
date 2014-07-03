package jogamp.opengl.util.pngj.chunks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngjException;

/**
 * All chunks that form an image, read or to be written.
 * <p>
 * chunks include all chunks, but IDAT is a single pseudo chunk without data
 **/
public class ChunksList {
	// ref: http://www.w3.org/TR/PNG/#table53
	public static final int CHUNK_GROUP_0_IDHR = 0; // required - single
	public static final int CHUNK_GROUP_1_AFTERIDHR = 1; // optional - multiple
	public static final int CHUNK_GROUP_2_PLTE = 2; // optional - single
	public static final int CHUNK_GROUP_3_AFTERPLTE = 3; // optional - multple
	public static final int CHUNK_GROUP_4_IDAT = 4; // required (single pseudo chunk)
	public static final int CHUNK_GROUP_5_AFTERIDAT = 5; // optional - multple
	public static final int CHUNK_GROUP_6_END = 6; // only 1 chunk - requried

	/**
	 * All chunks, read (or written)
	 *
	 * But IDAT is a single pseudo chunk without data
	 */
	protected List<PngChunk> chunks = new ArrayList<PngChunk>();

	final ImageInfo imageInfo; // only required for writing

	public ChunksList(final ImageInfo imfinfo) {
		this.imageInfo = imfinfo;
	}

	/**
	 * Keys of processed (read or writen) chunks
	 *
	 * @return key:chunk id, val: number of occurrences
	 */
	public HashMap<String, Integer> getChunksKeys() {
		final HashMap<String, Integer> ck = new HashMap<String, Integer>();
		for (final PngChunk c : chunks) {
			ck.put(c.id, ck.containsKey(c.id) ? ck.get(c.id) + 1 : 1);
		}
		return ck;
	}

	/**
	 * Returns a copy of the list (but the chunks are not copied) <b> This
	 * should not be used for general metadata handling
	 */
	public ArrayList<PngChunk> getChunks() {
		return new ArrayList<PngChunk>(chunks);
	}

	protected static List<PngChunk> getXById(final List<PngChunk> list, final String id, final String innerid) {
		if (innerid == null)
			return ChunkHelper.filterList(list, new ChunkPredicate() {
				@Override
				public boolean match(final PngChunk c) {
					return c.id.equals(id);
				}
			});
		else
			return ChunkHelper.filterList(list, new ChunkPredicate() {
				@Override
				public boolean match(final PngChunk c) {
					if (!c.id.equals(id))
						return false;
					if (c instanceof PngChunkTextVar && !((PngChunkTextVar) c).getKey().equals(innerid))
						return false;
					if (c instanceof PngChunkSPLT && !((PngChunkSPLT) c).getPalName().equals(innerid))
						return false;
					return true;
				}
			});
	}

	/**
	 * Adds chunk in next position. This is used onyl by the pngReader
	 */
	public void appendReadChunk(final PngChunk chunk, final int chunkGroup) {
		chunk.setChunkGroup(chunkGroup);
		chunks.add(chunk);
	}

	/**
	 * All chunks with this ID
	 *
	 * @param id
	 * @return List, empty if none
	 */
	public List<? extends PngChunk> getById(final String id) {
		return getById(id, null);
	}

	/**
	 * If innerid!=null and the chunk is PngChunkTextVar or PngChunkSPLT, it's
	 * filtered by that id
	 *
	 * @param id
	 * @return innerid Only used for text and SPLT chunks
	 * @return List, empty if none
	 */
	public List<? extends PngChunk> getById(final String id, final String innerid) {
		return getXById(chunks, id, innerid);
	}

	/**
	 * Returns only one chunk
	 *
	 * @param id
	 * @return First chunk found, null if not found
	 */
	public PngChunk getById1(final String id) {
		return getById1(id, false);
	}

	/**
	 * Returns only one chunk or null if nothing found - does not include queued
	 * <p>
	 * If more than one chunk is found, then an exception is thrown
	 * (failifMultiple=true or chunk is single) or the last one is returned
	 * (failifMultiple=false)
	 **/
	public PngChunk getById1(final String id, final boolean failIfMultiple) {
		return getById1(id, null, failIfMultiple);
	}

	/**
	 * Returns only one chunk or null if nothing found - does not include queued
	 * <p>
	 * If more than one chunk (after filtering by inner id) is found, then an
	 * exception is thrown (failifMultiple=true or chunk is single) or the last
	 * one is returned (failifMultiple=false)
	 **/
	public PngChunk getById1(final String id, final String innerid, final boolean failIfMultiple) {
		final List<? extends PngChunk> list = getById(id, innerid);
		if (list.isEmpty())
			return null;
		if (list.size() > 1 && (failIfMultiple || !list.get(0).allowsMultiple()))
			throw new PngjException("unexpected multiple chunks id=" + id);
		return list.get(list.size() - 1);
	}

	/**
	 * Finds all chunks "equivalent" to this one
	 *
	 * @param c2
	 * @return Empty if nothing found
	 */
	public List<PngChunk> getEquivalent(final PngChunk c2) {
		return ChunkHelper.filterList(chunks, new ChunkPredicate() {
			@Override
			public boolean match(final PngChunk c) {
				return ChunkHelper.equivalent(c, c2);
			}
		});
	}

	@Override
	public String toString() {
		return "ChunkList: read: " + chunks.size();
	}

	/**
	 * for debugging
	 */
	public String toStringFull() {
		final StringBuilder sb = new StringBuilder(toString());
		sb.append("\n Read:\n");
		for (final PngChunk chunk : chunks) {
			sb.append(chunk).append(" G=" + chunk.getChunkGroup() + "\n");
		}
		return sb.toString();
	}

}
