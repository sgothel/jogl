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

package com.jogamp.newt.impl.macosx;

import javax.media.nativewindow.*;
import javax.media.nativewindow.macosx.*;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.newt.*;
import com.jogamp.newt.impl.*;
import com.jogamp.newt.util.EDTUtil;
import com.jogamp.newt.util.MainThread;

public class MacDisplay extends Display {
    static {
        NEWTJNILibLoader.loadNEWT();

        if(!initNSApplication0()) {
            throw new NativeWindowException("Failed to initialize native Application hook");
        }
        if(!MacWindow.initIDs0()) {
            throw new NativeWindowException("Failed to initialize jmethodIDs");
        }
        if(DEBUG) System.out.println("MacDisplay.init App and IDs OK "+Thread.currentThread().getName());
    }

    public static void initSingleton() {
        // just exist to ensure static init has been run
    }
    
    public MacDisplay() {
    }

    protected void dispatchMessagesNative() {
        dispatchMessages0();
    }
    
    protected void createNative() {
        aDevice = new MacOSXGraphicsDevice();
    }

    protected void closeNative() { }

    public EDTUtil getEDTUtil() {
        if( null == edtUtil ) {
            synchronized (this) {
                if( null == edtUtil ) {
                    if(NewtFactory.useEDT()) {
                        final Display f_dpy = this;
                        MainThread.addPumpMessage(this, 
                                              new Runnable() {
                                                  public void run() {
                                                      if(null!=f_dpy.getGraphicsDevice()) {
                                                          f_dpy.dispatchMessages();
                                                      } } } );
                        edtUtil = MainThread.getSingleton();
                        edtUtil.start();
                    }
                }
            }
        }
        return edtUtil;
    }

    protected void releaseEDTUtil() {
        if(null!=edtUtil) { 
            MainThread.removePumpMessage(this);
            edtUtil.waitUntilStopped();
            edtUtil=null;
        }
    }

    private static native boolean initNSApplication0();
    protected native void dispatchMessages0();
}

