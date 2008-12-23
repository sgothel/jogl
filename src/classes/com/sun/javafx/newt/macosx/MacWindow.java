/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.javafx.newt.macosx;

import javax.media.opengl.GLCapabilities;

import com.sun.javafx.newt.*;
import com.sun.opengl.impl.*;

public class MacWindow extends Window {
    
    private static native boolean _initIDs();
    
    static {
        NativeLibLoader.loadNEWT();
        
        if (_initIDs() == false) {
            throw new RuntimeException("Failed to initialize jmethodIDs");
        }
    }
    
    public MacWindow() {
    }
    
    protected final void createNative(GLCapabilities caps) {
        chosenCaps = (GLCapabilities) caps.clone(); // FIXME: visualID := f1(caps); caps := f2(visualID)
        visualID = 0; // n/a
    }

    protected final void closeNative() {
    }
    
    public long getSurfaceHandle() {
        return 0;
    }
    
    public final int getDisplayWidth() {
        return 640;
    }
    
    public final int getDisplayHeight() {
        return 480;
    }
    
    public final void setVisible(boolean visible) {
    }
    
    public final void setSize(int width, int height) {
    }
    
    public final void setPosition(int x, int y) {
    }
    
    public boolean setFullscreen(boolean fullscreen) {
        return true;
    }
    
    protected final void dispatchMessages(int eventMask) {
    
    }
    
/*
    private void sizeChanged(int newWidth, int newHeight) {
        width = newWidth;
        height = newHeight;
        sendWindowEvent(WindowEvent.EVENT_WINDOW_RESIZED);
    }
    
    private void positionChanged(int newX, int newY) {
        x = newX;
        y = newY;
        sendWindowEvent(WindowEvent.EVENT_WINDOW_MOVED);
    }
    
    private void windowClosed() {
    }
    
    private void windowDestroyed() {
    }
*/
}
