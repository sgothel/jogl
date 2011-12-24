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
package jogamp.newt.driver.windows;

import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;

import jogamp.newt.ScreenImpl;

import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.util.ScreenModeUtil;

public class WindowsScreen extends ScreenImpl {

    static {
        WindowsDisplay.initSingleton();
    }

    public WindowsScreen() {
    }

    protected void createNativeImpl() {
        aScreen = new DefaultGraphicsScreen(getDisplay().getGraphicsDevice(), screen_idx);
    }
    
    protected void closeNativeImpl() {
    }

    private int[] getScreenModeIdx(int idx) {
        int[] modeProps = getScreenMode0(screen_idx, idx);
        if (null == modeProps || 0 == modeProps.length) {
            return null;
        }
        if(modeProps.length < ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL) {
            throw new RuntimeException("properties array too short, should be >= "+ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL+", is "+modeProps.length);
        }
        return modeProps;
    }

    private int nativeModeIdx;

    protected int[] getScreenModeFirstImpl() {
        nativeModeIdx = 0;
        return getScreenModeNextImpl();
    }

    protected int[] getScreenModeNextImpl() {
        int[] modeProps = getScreenModeIdx(nativeModeIdx);
        if (null != modeProps && 0 < modeProps.length) {
            nativeModeIdx++;
            return modeProps;
        }
        return null;
    }

    protected ScreenMode getCurrentScreenModeImpl() {
        int[] modeProps = getScreenModeIdx(-1);
        if (null != modeProps && 0 < modeProps.length) {
            return ScreenModeUtil.streamIn(modeProps, 0);
        }
        return null;
    }

    protected boolean setCurrentScreenModeImpl(ScreenMode sm) {
        return setScreenMode0(screen_idx, 
                              sm.getMonitorMode().getSurfaceSize().getResolution().getWidth(),
                              sm.getMonitorMode().getSurfaceSize().getResolution().getHeight(),
                              sm.getMonitorMode().getSurfaceSize().getBitsPerPixel(),
                              sm.getMonitorMode().getRefreshRate(),
                              sm.getRotation());
    }

    protected int validateScreenIndex(int idx) {
        return 0; // big-desktop, only one screen available 
    }
        
    protected void getVirtualScreenOriginAndSize(Point virtualOrigin, Dimension virtualSize) {
        virtualOrigin.setX(getOriginX0(screen_idx));
        virtualOrigin.setY(getOriginY0(screen_idx));
        virtualSize.setWidth(getWidthImpl0(screen_idx));
        virtualSize.setHeight(getHeightImpl0(screen_idx));
    }
    
    // Native calls
    private native int getOriginX0(int screen_idx);
    private native int getOriginY0(int screen_idx);
    private native int getWidthImpl0(int scrn_idx);
    private native int getHeightImpl0(int scrn_idx);

    private native int[] getScreenMode0(int screen_index, int mode_index);
    private native boolean setScreenMode0(int screen_index, int width, int height, int bits, int freq, int rot);
}
