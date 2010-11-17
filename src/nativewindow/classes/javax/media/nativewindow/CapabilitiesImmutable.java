package javax.media.nativewindow;

/**
 * Specifies an immutable set of capabilities that a window's rendering context
 * must support, such as color depth per channel.
 * 
 * @see javax.media.nativewindow.Capabilities
 */
public interface CapabilitiesImmutable extends Cloneable {

    /**
     * Returns the number of bits requested for the color buffer's red
     * component. On some systems only the color depth, which is the sum of the
     * red, green, and blue bits, is considered.
     */
    int getRedBits();

    /**
     * Returns the number of bits requested for the color buffer's green
     * component. On some systems only the color depth, which is the sum of the
     * red, green, and blue bits, is considered.
     */
    int getGreenBits();

    /**
     * Returns the number of bits requested for the color buffer's blue
     * component. On some systems only the color depth, which is the sum of the
     * red, green, and blue bits, is considered.
     */
    int getBlueBits();

    /**
     * Returns the number of bits requested for the color buffer's alpha
     * component. On some systems only the color depth, which is the sum of the
     * red, green, and blue bits, is considered.
     */
    int getAlphaBits();

    /**
     * Indicates whether the background of this OpenGL context should be
     * considered opaque. Defaults to true.
     */
    boolean isBackgroundOpaque();

    /**
     * Indicates whether the drawable surface is onscreen. Defaults to true.
     */
    boolean isOnscreen();

    /**
     * Gets the transparent red value for the frame buffer configuration. This
     * value is undefined if; equals true.
     */
    int getTransparentRedValue();

    /**
     * Gets the transparent green value for the frame buffer configuration. This
     * value is undefined if; equals true.
     */
    int getTransparentGreenValue();

    /**
     * Gets the transparent blue value for the frame buffer configuration. This
     * value is undefined if; equals true.
     */
    int getTransparentBlueValue();

    /**
     * Gets the transparent alpha value for the frame buffer configuration. This
     * value is undefined if; equals true.
     */
    int getTransparentAlphaValue();

    /**
     * Get a mutable clone of this instance.
     * 
     * @see java.lang.Object#clone()
     */
    Capabilities cloneCapabilites();

}
