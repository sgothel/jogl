package jogamp.opengl.util.pngj.chunks;

/**
 * Defines gral strategy about what to do with ancillary (non-critical) chunks
 * when reading
 */
public enum ChunkLoadBehaviour {
	/**
	 * All non-critical chunks are skipped
	 */
	LOAD_CHUNK_NEVER,
	/**
	 * Ancillary chunks are loaded only if 'known' (registered with the
	 * factory).
	 */
	LOAD_CHUNK_KNOWN,
	/**
	 *
	 * Load chunk if "known" or "safe to copy".
	 */
	LOAD_CHUNK_IF_SAFE,
	/**
	 * Load all chunks. <br>
	 * Notice that other restrictions might apply, see
	 * PngReader.skipChunkMaxSize PngReader.skipChunkIds
	 */
	LOAD_CHUNK_ALWAYS;
}
