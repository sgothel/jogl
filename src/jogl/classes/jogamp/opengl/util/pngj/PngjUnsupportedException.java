package jogamp.opengl.util.pngj;

/**
 * Exception thrown because of some valid feature of PNG standard that this
 * library does not support
 */
public class PngjUnsupportedException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PngjUnsupportedException() {
		super();
	}

	public PngjUnsupportedException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public PngjUnsupportedException(final String message) {
		super(message);
	}

	public PngjUnsupportedException(final Throwable cause) {
		super(cause);
	}
}
