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

import javax.media.nativewindow.*;
import javax.media.nativewindow.util.Insets;
import javax.media.nativewindow.util.Point;

public class OffscreenWindow extends WindowImpl implements SurfaceChangeable {

    long surfaceHandle = 0;

    public OffscreenWindow() {
    }

    static long nextWindowHandle = 0x100; // start here - a marker

    protected void createNativeImpl() {
        if(capsRequested.isOnscreen()) {
            throw new NativeWindowException("Capabilities is onscreen");
        }
        final AbstractGraphicsScreen aScreen = getScreen().getGraphicsScreen();
        final AbstractGraphicsConfiguration cfg = GraphicsConfigurationFactory.getFactory(aScreen.getDevice()).chooseGraphicsConfiguration(
                                                         capsRequested, capsRequested, capabilitiesChooser, aScreen);
        if (null == cfg) {
            throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
        }
        setGraphicsConfiguration(cfg);

        synchronized(OffscreenWindow.class) {
            setWindowHandle(nextWindowHandle++);
        }
    }

    protected void closeNativeImpl() {
        // nop
    }

    public void surfaceSizeChanged(int width, int height) {
         sizeChanged(false, width, height, false);
    }
    
    @Override
    public synchronized void destroy() {
        super.destroy();
        surfaceHandle = 0;
    }

    public void setSurfaceHandle(long handle) {
        surfaceHandle = handle ;
    }

    @Override
    public long getSurfaceHandle() {
        return surfaceHandle;
    }

    protected void requestFocusImpl(boolean reparented) {
    }

    @Override
    public void setPosition(int x, int y) {
        // nop
    }
    
    @Override
    public boolean setFullscreen(boolean fullscreen) {
        // nop
        return false;
    }

    protected boolean reconfigureWindowImpl(int x, int y, int width, int height, int flags) {
        if( 0 != ( FLAG_CHANGE_VISIBILITY & flags) ) {
            sizeChanged(false, width, height, false);
            visibleChanged(false, 0 != ( FLAG_IS_VISIBLE & flags));
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
    public Point getLocationOnScreen(Point storage) {
     if(null!=storage) {
        storage.setX(0);
        storage.setY(0);
        return storage;
     }
     return new Point(0,0);
    }
    
    protected Point getLocationOnScreenImpl(int x, int y) {
        return new Point(x,y);
    }
    
    protected void updateInsetsImpl(Insets insets) {
        // nop ..        
    }
}

