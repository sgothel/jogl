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

package jogamp.newt.driver.intel.gdl;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;

public class Screen extends jogamp.newt.ScreenImpl {

    static {
        Display.initSingleton();
    }

    public Screen() {
    }

    protected void createNativeImpl() {
        AbstractGraphicsDevice adevice = getDisplay().getGraphicsDevice();
        GetScreenInfo(adevice.getHandle(), screen_idx);
        aScreen = new DefaultGraphicsScreen(adevice, screen_idx);
    }

    protected void closeNativeImpl() { }

    protected int validateScreenIndex(int idx) {
        return 0; // only one screen available 
    }
        
    protected void getVirtualScreenOriginAndSize(Point virtualOrigin, Dimension virtualSize) {
        virtualOrigin.setX(0);
        virtualOrigin.setY(0);
        virtualSize.setWidth(cachedWidth);
        virtualSize.setHeight(cachedHeight);
    }
    
    //----------------------------------------------------------------------
    // Internals only
    //

    protected static native boolean initIDs();
    private          native void GetScreenInfo(long displayHandle, int screen_idx);

    // called by GetScreenInfo() ..
    private void screenCreated(int width, int height) {
        cachedWidth = width;
        cachedHeight = height;
    }
    
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;
}

