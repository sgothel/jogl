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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.PointImmutable;

import jogamp.newt.Debug;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.util.EDTUtil;

public abstract class Display {
    public static final boolean DEBUG = Debug.debug("Display");

    /** return precomputed hashCode from FQN {@link #getFQName()} */
    @Override
    public abstract int hashCode();

    /** return true if obj is of type Display and both FQN {@link #getFQName()} equals */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof Display) {
            Display d = (Display)obj;
            return d.getFQName().equals(getFQName());
        }
        return false;
    }

    /**
     * Native PointerIcon handle.
     * <p>
     * Instances can be created via {@link Display}'s {@link Display#createPointerIcon(com.jogamp.common.util.IOUtil.ClassResources, int, int) createPointerIcon(..)}.
     * </p>
     * <p>
     * Instance is {@link #destroy()}'ed automatically if it's {@link #getDisplay() associated Display} is destroyed.
     * </p>
     * <p>
     * Instance can be re-validated after destruction via {@link #validate()}.
     * </p>
     * <p>
     * {@link PointerIcon} may be {@link #destroy() destroyed} manually after use,
     * i.e. when no {@link Window} {@link Window#setPointerIcon(PointerIcon) uses them} anymore.
     * </p>
     * <p>
     * PointerIcons can be used via {@link Window#setPointerIcon(PointerIcon)}.
     * </p>
     */
    public static interface PointerIcon {
        /**
         * @return the associated Display
         */
        Display getDisplay();

        /**
         * @return the single {@link IOUtil.ClassResources}.
         */
        IOUtil.ClassResources getResource();

        /**
         * Returns true if valid, otherwise false.
         * <p>
         * A PointerIcon instance becomes invalid if it's {@link #getDisplay() associated Display} is destroyed.
         * </p>
         */
        boolean isValid();

        /**
         * Returns true if instance {@link #isValid()} or validation was successful, otherwise false.
         * <p>
         * Validation, i.e. recreation, is required if instance became invalid, see {@link #isValid()}.
         * </p>
         */
        boolean validate();

        /**
         * Destroys this instance.
         * <p>
         * Will be called automatically if it's {@link #getDisplay() associated Display} is destroyed.
         * </p>
         */
        void destroy();

        /** Returns the size, i.e. width and height. */
        DimensionImmutable getSize();

        /** Returns the hotspot. */
        PointImmutable getHotspot();

        @Override
        String toString();
    }

    /**
     * Returns the newly created {@link PointerIcon} or <code>null</code> if not implemented on platform.
     * <p>
     * See {@link PointerIcon} for lifecycle semantics.
     * </p>
     *
     * @param pngResource single PNG resource, only the first entry of {@link IOUtil.ClassResources#resourcePaths} is used.
     * @param hotX pointer hotspot x-coord, origin is upper-left corner
     * @param hotY pointer hotspot y-coord, origin is upper-left corner
     * @throws IllegalStateException if this Display instance is not {@link #isNativeValid() valid yet}.
     * @throws MalformedURLException
     * @throws InterruptedException
     * @throws IOException
     *
     * @see PointerIcon
     * @see Window#setPointerIcon(PointerIcon)
     */
    public abstract PointerIcon createPointerIcon(final IOUtil.ClassResources pngResource, final int hotX, final int hotY) throws IllegalStateException, MalformedURLException, InterruptedException, IOException;

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
    public abstract boolean validateEDTStopped();

    /**
     * @return true if the native display handle is valid and ready to operate,
     * otherwise false.
     *
     * @see #destroy()
     */
    public abstract boolean isNativeValid();

    /**
     * @return number of references
     */
    public abstract int getReferenceCount();

    /**
     * The 1st call will initiate native creation,
     * since we follow the lazy creation pattern.
     *
     * @return number of references post operation
     * @throws NativeWindowException if the native creation failed.
     * @see #removeReference()
     */
    public abstract int addReference() throws NativeWindowException ;

    /**
     * The last call may destroy this instance,
     * if {@link #getDestroyWhenUnused()} returns <code>true</code>.
     *
     * @return number of references post operation
     * @see #addReference()
     * @see #getDestroyWhenUnused()
     * @see #setDestroyWhenUnused(boolean)
     */
    public abstract int removeReference();

    /**
     * Return the {@link AbstractGraphicsDevice} used for depending resources lifecycle,
     * i.e. {@link Screen} and {@link Window}, as well as the event dispatching (EDT). */
    public abstract AbstractGraphicsDevice getGraphicsDevice();

    /**
     * Return the handle of the {@link AbstractGraphicsDevice} as returned by {@link #getGraphicsDevice()}.
     */
    public abstract long getHandle();

    /**
     * @return The fully qualified Display name,
     * which is a key of {@link #getType()} + {@link #getName()} + {@link #getId()}.
     */
    public abstract String getFQName();

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

    /** Return true if this instance is exclusive, i.e. will not be shared. */
    public abstract boolean isExclusive();

    /**
     * Sets a new {@link EDTUtil} and returns the previous one.
     * <p>
     * If <code>usrEDTUtil</code> is <code>null</code>,
     * the device's default EDTUtil is created and used.
     * </p>
     * <p>
     * If a previous one exists and it differs from <code>usrEDTUtil</code>,
     * it's being stopped, wait-until-idle.
     * </p>
     * <p>
     * If <code>usrEDTUtil</code> is not null and equals the previous one,
     * no change is being made.
     * </p>
     */
    public abstract EDTUtil setEDTUtil(EDTUtil usrEDTUtil);

    public abstract EDTUtil getEDTUtil();

    /**
     * @return true if EDT is running and not subject to be stopped, otherwise false.
     */
    public abstract boolean isEDTRunning();

    public abstract void dispatchMessages();

    // Global Displays
    protected static final ArrayList<WeakReference<Display>> displayList = new ArrayList<WeakReference<Display>>();
    protected static int displaysActive = 0;

    public static void dumpDisplayList(String prefix) {
        synchronized(displayList) {
            System.err.println(prefix+" DisplayList[] entries: "+displayList.size()+" - "+getThreadName());
            final Iterator<WeakReference<Display>> ri = displayList.iterator();
            for(int j=0; ri.hasNext(); j++) {
                final Display d = ri.next().get();
                System.err.println("  ["+j+"] : "+d+", GC'ed "+(null==d));
            }
        }
    }

    /**
     *
     * @param type
     * @param name
     * @param fromIndex start index, then increasing until found or end of list
     * @paran shared if true, only shared instances are found, otherwise also exclusive
     * @return
     */
    public static Display getFirstDisplayOf(String type, String name, int fromIndex, boolean shared) {
        return getDisplayOfImpl(type, name, fromIndex, 1, shared);
    }

    /**
     *
     * @param type
     * @param name
     * @param fromIndex start index, then decreasing until found or end of list. -1 is interpreted as size - 1.
     * @paran shared if true, only shared instances are found, otherwise also exclusive
     * @return
     */
    public static Display getLastDisplayOf(String type, String name, int fromIndex, boolean shared) {
        return getDisplayOfImpl(type, name, fromIndex, -1, shared);
    }

    private static Display getDisplayOfImpl(String type, String name, final int fromIndex, final int incr, boolean shared) {
        synchronized(displayList) {
            int i = fromIndex >= 0 ? fromIndex : displayList.size() - 1 ;
            while( ( incr > 0 ) ? i < displayList.size() : i >= 0 ) {
                final Display display = displayList.get(i).get();
                if( null == display ) {
                    // Clear GC'ed dead reference entry!
                    displayList.remove(i);
                    if( incr < 0 ) {
                        // decrease
                        i+=incr;
                    } // else nop - remove shifted subsequent elements to the left
                } else {
                    if( display.getType().equals(type) &&
                        display.getName().equals(name) &&
                        ( !shared || shared && !display.isExclusive() )
                      ) {
                        return display;
                    }
                    i+=incr;
                }
            }
        }
        return null;
    }

    protected static void addDisplay2List(Display display) {
        synchronized(displayList) {
            // GC before add
            int i=0;
            while( i < displayList.size() ) {
                if( null == displayList.get(i).get() ) {
                    displayList.remove(i);
                } else {
                    i++;
                }
            }
            displayList.add(new WeakReference<Display>(display));
        }
    }

    /** Returns the global display collection */
    public static Collection<Display> getAllDisplays() {
        ArrayList<Display> list;
        synchronized(displayList) {
            list = new ArrayList<Display>();
            int i = 0;
            while( i < displayList.size() ) {
                final Display d = displayList.get(i).get();
                if( null == d ) {
                    displayList.remove(i);
                } else {
                    list.add( displayList.get(i).get() );
                    i++;
                }
            }
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
