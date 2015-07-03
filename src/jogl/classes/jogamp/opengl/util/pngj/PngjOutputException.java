package jogamp.opengl.util.pngj;

/**
 * Exception thrown by writing process
 */
public class PngjOutputException extends PngjException {
	private static final long serialVersionUID = 1L;

	public PngjOutputException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public PngjOutputException(final String message) {
		super(message);
	}

	public PngjOutputException(final Throwable cause) {
		super(cause);
	}
}
