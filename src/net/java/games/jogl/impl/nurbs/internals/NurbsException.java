/** Encapsulates functionality of the C GLU NURBS implementation's
    setjmp/longjmp wrappers. */

public class NurbsException extends RuntimeException {
  private int errorCode;
  
  public NurbsException(int code) {
    super();
    errorCode = code;
  }

  public NurbsException(String message, int code) {
    super(message);
    errorCode = code;
  }

  public NurbsException(String message, Throwable cause, int code) {
    super(message, cause);
    errorCode = code;
  }

  public NurbsException(Throwable cause, int code) {
    super(cause);
    errorCode = code;
  }

  public int errorCode() {
    return errorCode;
  }
}
