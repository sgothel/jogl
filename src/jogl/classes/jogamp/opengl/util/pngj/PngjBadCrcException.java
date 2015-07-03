package jogamp.opengl.util.pngj;

/**
 * Exception thrown by bad CRC check
 */
public class PngjBadCrcException extends PngjInputException {
	private static final long serialVersionUID = 1L;

	public PngjBadCrcException(final String message, final Throwable cause) {
		super(message, cause);
	}

	public PngjBadCrcException(final String message) {
		super(message);
	}

	public PngjBadCrcException(final Throwable cause) {
		super(cause);
	}
}
