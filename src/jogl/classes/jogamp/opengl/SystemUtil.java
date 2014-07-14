package jogamp.opengl;

public class SystemUtil {

    private static volatile boolean getenvSupported = true;
    /** Wrapper for System.getenv(), which doesn't work on platforms
        earlier than JDK 5 */
    public static String getenv(final String variableName) {
        if (getenvSupported) {
            try {
                return System.getenv(variableName);
            } catch (final Error e) {
                getenvSupported = false;
            }
        }
        return null;
    }
}
