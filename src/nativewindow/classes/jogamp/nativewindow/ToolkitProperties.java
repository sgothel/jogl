package jogamp.nativewindow;

import com.jogamp.nativewindow.NativeWindowFactory;

/**
 * Marker interface.
 * <p>
 * Implementation requires to provide static methods:
 * <pre>
    public static void initSingleton() {}

    public static void shutdown() {}

    public static boolean requiresToolkitLock() {}

    public static boolean hasThreadingIssues() {}
 * </pre>
 * Above static methods are invoked by {@link NativeWindowFactory#initSingleton()},
 * or {@link NativeWindowFactory#shutdown()} via reflection.
 * </p>
 */
public interface ToolkitProperties {

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     */
    // void initSingleton();

    /**
     * Cleanup resources.
     * <p>
     * Called by {@link NativeWindowFactory#shutdown()}
     * </p>
     */
    // void shutdown();

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     */
    // boolean requiresToolkitLock();

    /**
     * Called by {@link NativeWindowFactory#initSingleton()}
     */
    // boolean hasThreadingIssues();

}
