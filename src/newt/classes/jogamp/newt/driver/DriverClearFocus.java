package jogamp.newt.driver;

/**
 * Interface tagging driver requirement of clearing the focus.
 * <p>
 * Some drivers require a programmatic {@link #clearFocus()} when traversing the focus.
 * </p>
 */
public interface DriverClearFocus {
    /** Programmatic clear the focus */
    void clearFocus();
}
