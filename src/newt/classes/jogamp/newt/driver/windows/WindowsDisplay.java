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

import jogamp.nativewindow.windows.RegisteredClass;
import jogamp.nativewindow.windows.RegisteredClassFactory;
import jogamp.newt.DisplayImpl;
import jogamp.newt.NEWTJNILibLoader;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.windows.WindowsGraphicsDevice;

public class WindowsDisplay extends DisplayImpl {

    private static final String newtClassBaseName = "_newt_clazz" ;
    private static RegisteredClassFactory sharedClassFactory;

    static {
        NEWTJNILibLoader.loadNEWT();

        if (!WindowsWindow.initIDs0()) {
            throw new NativeWindowException("Failed to initialize WindowsWindow jmethodIDs");
        }        
        sharedClassFactory = new RegisteredClassFactory(newtClassBaseName, WindowsWindow.getNewtWndProc0());
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }

    private RegisteredClass sharedClass;

    public WindowsDisplay() {
    }

    protected void createNativeImpl() {
        sharedClass = sharedClassFactory.getSharedClass();
        aDevice = new WindowsGraphicsDevice(AbstractGraphicsDevice.DEFAULT_UNIT);
    }

    protected void closeNativeImpl() { 
        sharedClassFactory.releaseSharedClass();
    }

    protected void dispatchMessagesNative() {
        DispatchMessages0();
    }

    protected long getHInstance() {
        return sharedClass.getHandle();
    }

    protected String getWindowClassName() {
        return sharedClass.getName();
    }

    //----------------------------------------------------------------------
    // Internals only
    //
    private static native void DispatchMessages0();
}

