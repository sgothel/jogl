package jogamp.newt.driver;

/** 
 * Interface tagging driver requirement of absolute positioning, ie. depend on parent position.
 */
public interface DriverUpdatePosition {
    /** Programmatic update the position */
    void updatePosition();
}
