package jogamp.opengl.util.pngj;

import java.io.OutputStream;

import jogamp.opengl.util.pngj.chunks.ChunkHelper;
import jogamp.opengl.util.pngj.chunks.ChunkRaw;


/**
 * outputs the stream for IDAT chunk , fragmented at fixed size (32k default).
 */
class PngIDatChunkOutputStream extends ProgressiveOutputStream {
	private static final int SIZE_DEFAULT = 32768; // 32k
	private final OutputStream outputStream;

	PngIDatChunkOutputStream(final OutputStream outputStream) {
		this(outputStream, 0);
	}

	PngIDatChunkOutputStream(final OutputStream outputStream, final int size) {
		super(size > 0 ? size : SIZE_DEFAULT);
		this.outputStream = outputStream;
	}

	@Override
	protected final void flushBuffer(final byte[] b, final int len) {
		final ChunkRaw c = new ChunkRaw(len, ChunkHelper.b_IDAT, false);
		c.data = b;
		c.writeChunk(outputStream);
	}
}
