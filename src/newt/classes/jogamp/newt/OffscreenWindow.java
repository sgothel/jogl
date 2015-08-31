/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package jogamp.newt;

import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.MutableSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.nativewindow.util.Point;

import com.jogamp.newt.MonitorDevice;

public class OffscreenWindow extends WindowImpl implements MutableSurface {

    long surfaceHandle;

    public OffscreenWindow() {
        surfaceHandle = 0;
    }

    static long nextWindowHandle = 0x100; // start here - a marker

    @Override
    protected void createNativeImpl() {
        if(capsRequested.isOnscreen()) {
            throw new NativeWindowException("Capabilities is onscreen");
        }
        final AbstractGraphicsScreen aScreen = getScreen().getGraphicsScreen();
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(aScreen.getDevice(), capsRequested).chooseGraphicsConfiguration(
                                                         capsRequested, capsRequested, capabilitiesChooser, aScreen, VisualIDHolder.VID_UNDEFINED);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);

        synchronized(OffscreenWindow.class) {
            setWindowHandle(nextWindowHandle++);  // just a marker
        }
        visibleChanged(false, true);
    }

    @Override
    protected void closeNativeImpl() {
        // nop
    }

    @Override
    public synchronized void destroy() {
        super.destroy();
        surfaceHandle = 0;
    }

    @Override
    public void setSurfaceHandle(final long handle) {
        surfaceHandle = handle ;
    }

    @Override
    public long getSurfaceHandle() {
        return surfaceHandle;
    }

    @Override
    protected void requestFocusImpl(final boolean reparented) {
    }

    @Override
    public void setPosition(final int x, final int y) {
        // nop
    }

    @Override
    public boolean setFullscreen(final boolean fullscreen) {
        return false; // nop
    }

    @Override
    public boolean setFullscreen(final List<MonitorDevice> monitors) {
        return false; // nop
    }

    @Override
    protected final int getSupportedReconfigMaskImpl() {
        return minimumReconfigStateMask;
    }

    @Override
    protected boolean reconfigureWindowImpl(final int x, final int y, final int width, final int height, final int flags) {
        sizeChanged(false, width, height, false);
        if( 0 != ( CHANGE_MASK_VISIBILITY & flags) ) {
            visibleChanged(false, 0 != ( STATE_MASK_VISIBLE & flags));
        } else {
            /**
             * silently ignore:
                FLAG_CHANGE_PARENTING
                FLAG_CHANGE_DECORATION
                FLAG_CHANGE_FULLSCREEN
                FLAG_CHANGE_ALWAYSONTOP
             */
        }
        return true;
    }

    @Override
    public Point getLocationOnScreen(final Point storage) {
     if(null!=storage) {
        storage.set(0, 0);
        return storage;
     }
     return new Point(0,0);
    }

    @Override
    protected Point getLocationOnScreenImpl(final int x, final int y) {
        return new Point(x,y);
    }
}

