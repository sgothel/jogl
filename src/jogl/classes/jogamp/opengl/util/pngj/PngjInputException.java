package jogamp.opengl.util.pngj;

/**
 * Exception thrown by reading process
 */
public class PngjInputException extends PngjException {
	private static final long serialVersionUID = 1L;

	public PngjInputException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public PngjInputException(final String message) {
		super(message);
	}

	public PngjInputException(final Throwable cause) {
		super(cause);
	}
}
