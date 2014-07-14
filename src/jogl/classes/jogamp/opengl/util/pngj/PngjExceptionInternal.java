package jogamp.opengl.util.pngj;

/**
 * Exception for anomalous internal problems (sort of asserts) that point to
 * some issue with the library
 *
 * @author Hernan J Gonzalez
 *
 */
public class PngjExceptionInternal extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PngjExceptionInternal(final String message, final Throwable cause) {
		super(message, cause);
	}

	public PngjExceptionInternal(final String message) {
		super(message);
	}

	public PngjExceptionInternal(final Throwable cause) {
		super(cause);
	}
}
