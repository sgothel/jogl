package jogamp.opengl.util.pngj.chunks;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jogamp.opengl.util.pngj.ImageInfo;
import jogamp.opengl.util.pngj.PngjException;
import jogamp.opengl.util.pngj.PngjOutputException;

public class ChunksListForWrite extends ChunksList {

	/**
	 * chunks not yet writen - does not include IHDR, IDAT, END, perhaps yes
	 * PLTE
	 */
	private final List<PngChunk> queuedChunks = new ArrayList<PngChunk>();

	// redundant, just for eficciency
	private final HashMap<String, Integer> alreadyWrittenKeys = new HashMap<String, Integer>();

	public ChunksListForWrite(final ImageInfo imfinfo) {
		super(imfinfo);
	}

	/**
	 * Same as getById(), but looking in the queued chunks
	 */
	public List<? extends PngChunk> getQueuedById(final String id) {
		return getQueuedById(id, null);
	}

	/**
	 * Same as getById(), but looking in the queued chunks
	 */
	public List<? extends PngChunk> getQueuedById(final String id, final String innerid) {
		return getXById(queuedChunks, id, innerid);
	}

	/**
	 * Same as getById1(), but looking in the queued chunks
	 **/
	public PngChunk getQueuedById1(final String id, final String innerid, final boolean failIfMultiple) {
		final List<? extends PngChunk> list = getQueuedById(id, innerid);
		if (list.isEmpty())
			return null;
		if (list.size() > 1 && (failIfMultiple || !list.get(0).allowsMultiple()))
			throw new PngjException("unexpected multiple chunks id=" + id);
		return list.get(list.size() - 1);
	}

	/**
	 * Same as getById1(), but looking in the queued chunks
	 **/
	public PngChunk getQueuedById1(final String id, final boolean failIfMultiple) {
		return getQueuedById1(id, null, failIfMultiple);
	}

	/**
	 * Same as getById1(), but looking in the queued chunks
	 **/
	public PngChunk getQueuedById1(final String id) {
		return getQueuedById1(id, false);
	}

	/**
	 * Remove Chunk: only from queued
	 *
	 * WARNING: this depends on c.equals() implementation, which is
	 * straightforward for SingleChunks. For MultipleChunks, it will normally
	 * check for reference equality!
	 */
	public boolean removeChunk(final PngChunk c) {
		return queuedChunks.remove(c);
	}

	/**
	 * Adds chunk to queue
	 *
	 * Does not check for duplicated or anything
	 *
	 * @param c
	 */
	public boolean queue(final PngChunk c) {
		queuedChunks.add(c);
		return true;
	}

	/**
	 * this should be called only for ancillary chunks and PLTE (groups 1 - 3 -
	 * 5)
	 **/
	private static boolean shouldWrite(final PngChunk c, final int currentGroup) {
		if (currentGroup == CHUNK_GROUP_2_PLTE)
			return c.id.equals(ChunkHelper.PLTE);
		if (currentGroup % 2 == 0)
			throw new PngjOutputException("bad chunk group?");
		int minChunkGroup, maxChunkGroup;
		if (c.getOrderingConstraint().mustGoBeforePLTE())
			minChunkGroup = maxChunkGroup = ChunksList.CHUNK_GROUP_1_AFTERIDHR;
		else if (c.getOrderingConstraint().mustGoBeforeIDAT()) {
			maxChunkGroup = ChunksList.CHUNK_GROUP_3_AFTERPLTE;
			minChunkGroup = c.getOrderingConstraint().mustGoAfterPLTE() ? ChunksList.CHUNK_GROUP_3_AFTERPLTE
					: ChunksList.CHUNK_GROUP_1_AFTERIDHR;
		} else {
			maxChunkGroup = ChunksList.CHUNK_GROUP_5_AFTERIDAT;
			minChunkGroup = ChunksList.CHUNK_GROUP_1_AFTERIDHR;
		}

		int preferred = maxChunkGroup;
		if (c.hasPriority())
			preferred = minChunkGroup;
		if (ChunkHelper.isUnknown(c) && c.getChunkGroup() > 0)
			preferred = c.getChunkGroup();
		if (currentGroup == preferred)
			return true;
		if (currentGroup > preferred && currentGroup <= maxChunkGroup)
			return true;
		return false;
	}

	public int writeChunks(final OutputStream os, final int currentGroup) {
		int cont = 0;
		final Iterator<PngChunk> it = queuedChunks.iterator();
		while (it.hasNext()) {
			final PngChunk c = it.next();
			if (!shouldWrite(c, currentGroup))
				continue;
			if (ChunkHelper.isCritical(c.id) && !c.id.equals(ChunkHelper.PLTE))
				throw new PngjOutputException("bad chunk queued: " + c);
			if (alreadyWrittenKeys.containsKey(c.id) && !c.allowsMultiple())
				throw new PngjOutputException("duplicated chunk does not allow multiple: " + c);
			c.write(os);
			chunks.add(c);
			alreadyWrittenKeys.put(c.id, alreadyWrittenKeys.containsKey(c.id) ? alreadyWrittenKeys.get(c.id) + 1 : 1);
			c.setChunkGroup(currentGroup);
			it.remove();
			cont++;
		}
		return cont;
	}

	/**
	 * warning: this is NOT a copy, do not modify
	 */
	public List<PngChunk> getQueuedChunks() {
		return queuedChunks;
	}

	@Override
	public String toString() {
		return "ChunkList: written: " + chunks.size() + " queue: " + queuedChunks.size();
	}

	/**
	 * for debugging
	 */
	@Override
	public String toStringFull() {
		final StringBuilder sb = new StringBuilder(toString());
		sb.append("\n Written:\n");
		for (final PngChunk chunk : chunks) {
			sb.append(chunk).append(" G=" + chunk.getChunkGroup() + "\n");
		}
		if (!queuedChunks.isEmpty()) {
			sb.append(" Queued:\n");
			for (final PngChunk chunk : queuedChunks) {
				sb.append(chunk).append("\n");
			}

		}
		return sb.toString();
	}
}
