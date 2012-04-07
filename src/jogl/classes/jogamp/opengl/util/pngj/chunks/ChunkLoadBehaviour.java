package jogamp.opengl.util.pngj.chunks;

public enum ChunkLoadBehaviour {
	// what to do with non critical chunks when reading?
	LOAD_CHUNK_NEVER, /* ignore non-critical chunks */
	LOAD_CHUNK_KNOWN, /* load chunk if 'known' */
	LOAD_CHUNK_IF_SAFE, /* load chunk if 'known' or safe to copy */
	LOAD_CHUNK_ALWAYS /* load chunk always */
	;
}
