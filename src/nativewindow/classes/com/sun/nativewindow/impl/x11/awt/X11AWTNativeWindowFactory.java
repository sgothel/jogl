/*
 * Copyright (c) 2008-2009 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.sun.nativewindow.impl.x11.awt;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import javax.media.nativewindow.*;

import com.sun.nativewindow.impl.*;
import com.sun.nativewindow.impl.jawt.*;
import com.sun.nativewindow.impl.jawt.x11.*;
import com.sun.nativewindow.impl.x11.*;

public class X11AWTNativeWindowFactory extends NativeWindowFactoryImpl {

    // When running the AWT on X11 platforms, we use the AWT native
    // interface (JAWT) to lock and unlock the toolkit
    private ToolkitLock toolkitLock = new ToolkitLock() {
            private Thread owner;
            private int recursionCount;
            
            public synchronized void lock() {
                Thread cur = Thread.currentThread();
                if (owner == cur) {
                    ++recursionCount;
                    return;
                }
                while (owner != null) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                owner = cur;
                JAWTUtil.lockToolkit();
            }

            public synchronized void unlock() {
                if (owner != Thread.currentThread()) {
                    throw new RuntimeException("Not owner");
                }
                if (recursionCount > 0) {
                    --recursionCount;
                    return;
                }
                owner = null;
                JAWTUtil.unlockToolkit();
                notifyAll();
            }
        };

    public ToolkitLock getToolkitLock() {
        return toolkitLock;
    }
}
