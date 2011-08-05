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

import jogamp.newt.*;
import javax.media.nativewindow.*;

public class Display extends jogamp.newt.DisplayImpl {
    static int initCounter = 0;

    static {
        NEWTJNILibLoader.loadNEWT();

        if (!Screen.initIDs()) {
            throw new NativeWindowException("Failed to initialize GDL Screen jmethodIDs");
        }
        if (!Window.initIDs()) {
            throw new NativeWindowException("Failed to initialize GDL Window jmethodIDs");
        }
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }


    public Display() {
    }

    protected void createNativeImpl() {
        synchronized(Display.class) {
            if(0==initCounter) {
                displayHandle = CreateDisplay();
                if(0==displayHandle) {
                    throw new NativeWindowException("Couldn't initialize GDL Display");
                }
            }
            initCounter++;
        }
        aDevice = new DefaultGraphicsDevice(NativeWindowFactory.TYPE_DEFAULT, AbstractGraphicsDevice.DEFAULT_CONNECTION, AbstractGraphicsDevice.DEFAULT_UNIT, displayHandle);
    }

    protected void closeNativeImpl() {
        if(0==displayHandle) {
            throw new NativeWindowException("displayHandle null; initCnt "+initCounter);
        }
        synchronized(Display.class) {
            if(initCounter>0) {
                initCounter--;
                if(0==initCounter) {
                    DestroyDisplay(displayHandle);
                }
            }
        }
    }

    protected void dispatchMessagesNative() {
        if(0!=displayHandle) {
            DispatchMessages(displayHandle, focusedWindow);
        }
    }

    protected void setFocus(Window focus) {
        focusedWindow = focus;
    }

    private long displayHandle = 0;
    private Window focusedWindow = null;
    private native long CreateDisplay();
    private native void DestroyDisplay(long displayHandle);
    private native void DispatchMessages(long displayHandle, Window focusedWindow);
}

