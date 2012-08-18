/**
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.newt.driver.bcm.vc.iv;

import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;

import jogamp.newt.ScreenImpl;

public class ScreenDriver extends ScreenImpl {
    static {
        DisplayDriver.initSingleton();
    }

    public ScreenDriver() {
    }

    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), screen_idx);
        initNative();
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
    
    protected void setScreenSize(int width, int height) {
        cachedWidth = width;
        cachedHeight = height;
    }
    
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;    

    protected static native boolean initIDs();
    protected native void initNative();
}
