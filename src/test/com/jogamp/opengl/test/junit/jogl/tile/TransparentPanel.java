package com.jogamp.opengl.test.junit.jogl.tile;

import java.awt.*;
import javax.swing.*;
import java.lang.reflect.Method;

public class TransparentPanel extends JPanel {
    public TransparentPanel() {
        super.setOpaque(false);
        setMixingCutoutShape(new Rectangle());
    }

    @Override
        public void setOpaque(final boolean isOpaque) {
        // Don't let this panel become opaque
    }

    /**
     * Helper utility needed to implement TransparentPanel.
     * This class provides the ability to cut out the background of a lightweight
     * panel so that it can be layered on top of a heavyweight component and have
     * the heavyweight component show through.  For more infromation, see:
     *
     * http://today.java.net/article/2009/11/02/transparent-panel-mixing-heavyweight-and-lightweight-components
     */
    private static Method mSetComponentMixing;

    /**
     * Set the cut out shape on a given Component.
     *
     * @param c The Component on which to set the cut out shape.
     * @param s The shape to cut out of the given Component.
     */
    public void setMixingCutoutShape(final Shape s)
    {
        // Get the cut out shape method
        if (mSetComponentMixing == null) {
            try {
                final Class<?> awtUtilitiesClass =
                    Class.forName("com.sun.awt.AWTUtilities");
                mSetComponentMixing =
                    awtUtilitiesClass.getMethod(
                        "setComponentMixingCutoutShape",
                        Component.class, Shape.class);
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        }

        // Cut out the shape
        if (mSetComponentMixing != null) {
            try {
                mSetComponentMixing.invoke( null, this, s );
            } catch (final Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
