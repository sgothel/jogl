package jogamp.nativewindow;

import javax.media.nativewindow.NativeWindowFactory;

/**
 * Marker interface.
 * <p>
 * Implementation requires to provide static methods:
 * <pre>
    public static void initSingleton() {}
    
    public static void shutdown() {}
    
    public static boolean requiresToolkitLock() {}
    
    public static boolean requiresGlobalToolkitLock() {}
 * </pre>
 * Above static methods are invoked by {@link NativeWindowFactory#initSingleton()}, 
 * or {@link NativeWindowFactory#shutdown()} via reflection.
 * </p>
 * <p>
 * If <code>requiresGlobalToolkitLock() == true</code>, then 
 * <code>requiresToolkitLock() == true</code> shall be valid as well.
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
    // boolean requiresGlobalToolkitLock();    
    
}
