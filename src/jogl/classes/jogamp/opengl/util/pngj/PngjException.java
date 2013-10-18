package jogamp.opengl.util.pngj;

/**
 * Generic exception
 *
 * @author Hernan J Gonzalez
 *
 */
public class PngjException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public PngjException(String message, Throwable cause) {
		super(message, cause);
	}

	public PngjException(String message) {
		super(message);
	}

	public PngjException(Throwable cause) {
		super(cause);
	}
}
