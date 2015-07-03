package jogamp.opengl.util.pngj.chunks;

/**
 * Chunk copy policy to apply when copyng from a pngReader to a pngWriter.
 * <p>
 * http://www.w3.org/TR/PNG/#14 <br>
 * These are masks, can be OR-ed
 **/
public class ChunkCopyBehaviour {

	/** dont copy anywhing */
	public static final int COPY_NONE = 0;

	/** copy the palette */
	public static final int COPY_PALETTE = 1;

	/** copy all 'safe to copy' chunks */
	public static final int COPY_ALL_SAFE = 1 << 2;
	public static final int COPY_ALL = 1 << 3; // includes palette!
	public static final int COPY_PHYS = 1 << 4; // dpi
	public static final int COPY_TEXTUAL = 1 << 5; // all textual types
	public static final int COPY_TRANSPARENCY = 1 << 6; //
	public static final int COPY_UNKNOWN = 1 << 7; // all unknown (by the factory!)
	public static final int COPY_ALMOSTALL = 1 << 8; // almost all known (except HIST and TIME and textual)
}
