package jogamp.newt.driver;

/**
 * Interface tagging driver requirement of absolute positioning, ie. depend on parent position.
 */
public interface DriverUpdatePosition {
    /**
     * Programmatic update the top-left corner
     * of the client area relative to it's parent.
     *
     * @param x x-component
     * @param y y-component
     **/
    void updatePosition(int x, int y);
}
