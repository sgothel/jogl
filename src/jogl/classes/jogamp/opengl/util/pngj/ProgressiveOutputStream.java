package jogamp.opengl.util.pngj;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * stream that outputs to memory and allows to flush fragments every 'size'
 * bytes to some other destination
 */
abstract class ProgressiveOutputStream extends ByteArrayOutputStream {
	private final int size;
	private long countFlushed = 0;

	public ProgressiveOutputStream(final int size) {
		this.size = size;
	}

	@Override
	public final void close() throws IOException {
		flush();
		super.close();
	}

	@Override
	public final void flush() throws IOException {
		super.flush();
		checkFlushBuffer(true);
	}

	@Override
	public final void write(final byte[] b, final int off, final int len) {
		super.write(b, off, len);
		checkFlushBuffer(false);
	}

	@Override
	public final void write(final byte[] b) throws IOException {
		super.write(b);
		checkFlushBuffer(false);
	}

	@Override
	public final void write(final int arg0) {
		super.write(arg0);
		checkFlushBuffer(false);
	}

	@Override
	public final synchronized void reset() {
		super.reset();
	}

	/**
	 * if it's time to flush data (or if forced==true) calls abstract method
	 * flushBuffer() and cleans those bytes from own buffer
	 */
	private final void checkFlushBuffer(final boolean forced) {
		while (forced || count >= size) {
			int nb = size;
			if (nb > count)
				nb = count;
			if (nb == 0)
				return;
			flushBuffer(buf, nb);
			countFlushed += nb;
			final int bytesleft = count - nb;
			count = bytesleft;
			if (bytesleft > 0)
				System.arraycopy(buf, nb, buf, 0, bytesleft);
		}
	}

	protected abstract void flushBuffer(byte[] b, int n);

	public long getCountFlushed() {
		return countFlushed;
	}
}