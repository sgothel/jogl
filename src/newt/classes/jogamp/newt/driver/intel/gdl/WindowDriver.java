/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 */

package jogamp.newt.driver.intel.gdl;

import com.jogamp.nativewindow.*;
import com.jogamp.nativewindow.util.Point;

public class WindowDriver extends jogamp.newt.WindowImpl {
    static {
        DisplayDriver.initSingleton();
    }

    public WindowDriver() {
    }

    static long nextWindowHandle = 1;

    @Override
    protected void createNativeImpl() {
        if(0!=getParentWindowHandle()) {
            throw new NativeWindowException("GDL Window does not support window parenting");
        }
        final AbstractGraphicsScreen aScreen = getScreen().getGraphicsScreen();
        final AbstractGraphicsDevice aDevice = getScreen().getDisplay().getGraphicsDevice();

        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(aDevice, capsRequested).chooseGraphicsConfiguration(
                capsRequested, capsRequested, capabilitiesChooser, aScreen, VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);

        synchronized(WindowDriver.class) {
            setWindowHandle(nextWindowHandle++); // just a marker

            surfaceHandle = CreateSurface(aDevice.getHandle(), getScreen().getWidth(), getScreen().getHeight(),
                                          getX(), getY(), getSurfaceWidth(), getSurfaceHeight());
            if (surfaceHandle == 0) {
                throw new NativeWindowException("Error creating window");
            }
        }
    }

    @Override
    protected void closeNativeImpl() {
        if(0!=surfaceHandle) {
            synchronized(WindowDriver.class) {
                CloseSurface(getDisplayHandle(), surfaceHandle);
            }
            surfaceHandle = 0;
            ((DisplayDriver)getScreen().getDisplay()).setFocus(null);
        }
    }

    @Override
    protected final int getSupportedReconfigMaskImpl() {
        return minimumReconfigStateMask;
    }

    @Override
    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, final int flags) {
        final ScreenDriver  screen = (ScreenDriver) getScreen();

        // Note for GDL: window units == pixel units
        if(width>screen.getWidth()) {
            width=screen.getWidth();
        }
        if(height>screen.getHeight()) {
            height=screen.getHeight();
        }
        if((x+width)>screen.getWidth()) {
            x=screen.getWidth()-width;
        }
        if((y+height)>screen.getHeight()) {
            y=screen.getHeight()-height;
        }

        if(0!=surfaceHandle) {
            SetBounds0(surfaceHandle, getScreen().getWidth(), getScreen().getHeight(), x, y, width, height);
        }

        if( 0 != ( CHANGE_MASK_VISIBILITY & flags) ) {
            if(0 != ( STATE_MASK_VISIBLE & flags)) {
                ((DisplayDriver)getScreen().getDisplay()).setFocus(this);
            }
            visibleChanged(false, 0 != ( STATE_MASK_VISIBLE & flags));
        }

        return true;
    }

    @Override
    protected void requestFocusImpl(final boolean reparented) {
        ((DisplayDriver)getScreen().getDisplay()).setFocus(this);
    }

    @Override
    public final long getSurfaceHandle() {
        return surfaceHandle;
    }

    @Override
    protected Point getLocationOnScreenImpl(final int x, final int y) {
        return new Point(x,y);
    }

    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private        native long CreateSurface(long displayHandle, int scrn_width, int scrn_height, int x, int y, int width, int height);
    private        native void CloseSurface(long displayHandle, long surfaceHandle);
    private        native void SetBounds0(long surfaceHandle, int scrn_width, int scrn_height, int x, int y, int width, int height);

    private void updateBounds(final int x, final int y, final int width, final int height) {
        definePosition(x, y);
        defineSize(width, height);
    }

    private long   surfaceHandle;
}
