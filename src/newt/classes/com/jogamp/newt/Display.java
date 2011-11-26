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
import jogamp.newt.Debug;

import java.util.*;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;

public abstract class Display {
    public static final boolean DEBUG = Debug.debug("Display");

    /** return precomputed hashCode from FQN {@link #getFQName()} */
    public abstract int hashCode();

    /** return true if obj is of type Display and both FQN {@link #getFQName()} equals */
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof Display) {
            Display d = (Display)obj;
            return d.getFQName().equals(getFQName());
        }
        return false;
    }

    /**
     * Manual trigger the native creation, if it is not done yet.<br>
     * This is useful to be able to request the {@link javax.media.nativewindow.AbstractGraphicsDevice}, via
     * {@link #getGraphicsDevice()}.<br>
     * Otherwise the abstract device won't be available before the dependent components (Screen and Window) are realized.
     * <p>
     * This method is usually invoke by {@link #addReference()}
     * </p>
     * @throws NativeWindowException if the native creation failed.
     */
    public abstract void createNative() throws NativeWindowException;

    /**
     * Manually trigger the destruction, incl. native destruction.<br>
     * <p>
     * This method is usually invoke by {@link #removeReference()}
     * </p>
     */
    public abstract void destroy();

    /**
     * Validate EDT running state.<br>
     * Stop the running EDT in case this display is destroyed already.<br>
     * @return true if EDT has been stopped (destroyed but running), otherwise false.
     */
    public abstract boolean validateEDT();

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

    /**
     * The 1st call will initiate native creation,
     * since we follow the lazy creation pattern.
     *
     * @return number of references after adding one
     * @throws NativeWindowException if the native creation failed.
     * @see #removeReference()
     */
    public abstract int addReference() throws NativeWindowException ;

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
     * @return This display connection name as defined at creation time. 
     *         The display connection name is a technical platform specific detail, see {@link AbstractGraphicsDevice#getConnection()}. 
     *
     * @see AbstractGraphicsDevice#getConnection()
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
    protected static ArrayList<Display> displayList = new ArrayList<Display>();
    protected static int displaysActive = 0;

    public static void dumpDisplayList(String prefix) {
        synchronized(displayList) {
            Iterator<Display> i = displayList.iterator();
            System.err.println(prefix+" DisplayList[] entries: "+displayList.size()+" - "+getThreadName());
            for(int j=0; i.hasNext(); j++) {
                Display d = i.next();
                System.err.println("  ["+j+"] : "+d);
            }
        }
    }

    /**
     * 
     * @param type
     * @param name
     * @param fromIndex start index, then increasing until found or end of list     * 
     * @return 
     */
    public static Display getFirstDisplayOf(String type, String name, int fromIndex) {
        return getDisplayOfImpl(type, name, fromIndex, 1);
    }

    /**
     *
     * @param type
     * @param name
     * @param fromIndex start index, then decreasing until found or end of list. -1 is interpreted as size - 1.
     * @return
     */
    public static Display getLastDisplayOf(String type, String name, int fromIndex) {
        return getDisplayOfImpl(type, name, fromIndex, -1);
    }

    private static Display getDisplayOfImpl(String type, String name, int fromIndex, int incr) {
        synchronized(displayList) {
            int i = fromIndex >= 0 ? fromIndex : displayList.size() - 1 ;
            while( ( incr > 0 ) ? i < displayList.size() : i >= 0 ) {
                Display display = (Display) displayList.get(i);
                if( display.getType().equals(type) &&
                    display.getName().equals(name) ) {
                    return display;
                }
                i+=incr;
            }
        }
        return null;
    }

    /** Returns the global display collection */
    @SuppressWarnings("unchecked")
    public static Collection<Display> getAllDisplays() {
        ArrayList<Display> list;
        synchronized(displayList) {
            list = (ArrayList<Display>) displayList.clone();
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

    public static int hashCodeNullSafe(Object o) {
        return ( null != o ) ? o.hashCode() : 0;
    }
}
