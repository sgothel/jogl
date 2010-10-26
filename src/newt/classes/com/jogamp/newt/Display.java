/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.newt;

import com.jogamp.newt.util.EDTUtil;
import com.jogamp.newt.impl.Debug;
import com.jogamp.newt.impl.DisplayImpl;

import java.util.*;

import javax.media.nativewindow.AbstractGraphicsDevice;

public abstract class Display {
    public static final boolean DEBUG = Debug.debug("Display");

    /**
     * @return true if the native display handle is valid and ready to operate,
     * otherwise false.
     *
     * @see #destroy()
     */
    public abstract boolean isNativeValid();

    /**
     * @return number of references by Screen
     */
    public abstract int getReferenceCount();

    public abstract void destroy();

    /**
     * @return the value set by {@link #setDestroyWhenUnused(boolean)}
     * or the default <code>false</code>.
     *
     * @see #addReference()
     * @see #removeReference()
     */
    public abstract boolean getDestroyWhenUnused();

    /**
     * Handles the lifecycle of the native Display instance.<br>
     * If set to <code>true</code>, the last {@link #removeReference()} call
     * will destroy this instance, otherwise it will stay alive.<br>
     * Default is <code>false</code>.
     *
     * @see #addReference()
     * @see #removeReference()
     */
    public abstract void setDestroyWhenUnused(boolean v);

    /**
     * The 1st call will initiate native creation,
     * since we follow the lazy creation pattern.
     *
     * @return number of references after adding one
     * @see #removeReference()
     */
    public abstract int addReference();

    /**
     * The last call may destroy this instance,
     * if {@link #getDestroyWhenUnused()} returns <code>true</code>.
     *
     * @return number of references after removing one
     * @see #addReference()
     * @see #getDestroyWhenUnused()
     * @see #setDestroyWhenUnused(boolean)
     */
    public abstract int removeReference();

    public abstract AbstractGraphicsDevice getGraphicsDevice();

    /**
     * @return the fully qualified Display name,
     * which is a key of {@link #getType()} + {@link #getName()} + {@link #getId()}
     */
    public abstract String getFQName();

    public abstract long getHandle();

    /**
     * @return this display internal serial id
     */
    public abstract int getId();

    /**
     * @return this display instance name as defined at creation time
     */
    public abstract String getName();

    /**
     * @return the native display type, ie {@link javax.media.nativewindow.NativeWindowFactory#getNativeWindowType(boolean)}
     */
    public abstract String getType();

    public abstract EDTUtil getEDTUtil();

    public abstract boolean isEDTRunning();

    public abstract void dispatchMessages();
    
    // Global Displays
    protected static ArrayList displayList = new ArrayList();
    protected static int displaysActive = 0;

    public static void dumpDisplayList(String prefix) {
        synchronized(displayList) {
            Iterator i = displayList.iterator();
            System.err.println(prefix+" DisplayList[] entries: "+displayList.size()+" - "+getThreadName());
            for(int j=0; i.hasNext(); j++) {
                DisplayImpl d = (DisplayImpl) i.next();
                System.err.println("  ["+j+"] : "+d);
            }
        }
    }

    /** Returns the global display collection */
    public static Collection getAllDisplays() {
        ArrayList list;
        synchronized(displayList) {
            list = (ArrayList) displayList.clone();
        }
        return list;
    }

    public static int getActiveDisplayNumber() {
        synchronized(displayList) {
            return displaysActive;
        }
    }

    public static String getThreadName() {
        return Thread.currentThread().getName();
    }

    public static String toHexString(int hex) {
        return "0x" + Integer.toHexString(hex);
    }

    public static String toHexString(long hex) {
        return "0x" + Long.toHexString(hex);
    }

    public static int hashCode(Object o) {
        return ( null != o ) ? o.hashCode() : 0;
    }
}
