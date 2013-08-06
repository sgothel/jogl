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

import com.jogamp.newt.event.MonitorModeListener;
import jogamp.newt.Debug;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Rectangle;
import javax.media.nativewindow.util.RectangleImmutable;

/** 
 * A screen may span multiple {@link MonitorDevice}s representing their combined virtual size.
 */
public abstract class Screen {

    /**
     * A 10s timeout for screen mode change. It is observed, that some platforms
     * need a notable amount of time for this task, especially in case of rotation change.
     */
    public static final int SCREEN_MODE_CHANGE_TIMEOUT = 10000;

    public static final boolean DEBUG = Debug.debug("Screen");

    /** return precomputed hashCode from FQN {@link #getFQName()} */
    public abstract int hashCode();

    /** return true if obj is of type Display and both FQN {@link #getFQName()} equals */
    public boolean equals(Object obj) {
        if (this == obj) { return true; }
        if (obj instanceof Screen) {
            Screen s = (Screen)obj;
            return s.getFQName().equals(getFQName());
        }
        return false;
    }

    /**
     * Manual trigger the native creation, if not done yet..<br>
     * This is useful to be able to request the {@link javax.media.nativewindow.AbstractGraphicsScreen}, via
     * {@link #getGraphicsScreen()}.<br>
     * Otherwise the abstract device won't be available before the dependent component (Window) is realized.
     * <p>
     * This method is usually invoke by {@link #addReference()}
     * </p>
     * <p>
     * This method invokes {@link Display#addReference()} after creating the native peer,<br>
     * which will issue {@link Display#createNative()} if the reference count was 0.
     * </p>
     * @throws NativeWindowException if the native creation failed.
     */
    public abstract void createNative() throws NativeWindowException;

    /**
     * Manually trigger the destruction, incl. native destruction.<br>
     * <p>
     * This method is usually invoke by {@link #removeReference()}
     * </p>
     * <p>
     * This method invokes {@link Display#removeReference()} after it's own destruction,<br>
     * which will issue {@link Display#destroy()} if the reference count becomes 0.
     * </p>
     */
    public abstract void destroy();

    public abstract boolean isNativeValid();

    /**
     * @return number of references
     */
    public abstract int getReferenceCount();

    /**
     * See {@link Display#addReference()}
     * 
     * @return number of references post operation
     * @throws NativeWindowException if the native creation failed.
     * @see #removeReference()
     * @see #setDestroyWhenUnused(boolean)
     * @see #getDestroyWhenUnused()
     */
    public abstract int addReference() throws NativeWindowException;

    /**
     * See {@link Display#removeReference()}
     * 
     * @return number of references post operation
     * @see #addReference()
     * @see #setDestroyWhenUnused(boolean)
     * @see #getDestroyWhenUnused()
     */
    public abstract int removeReference();

    public abstract AbstractGraphicsScreen getGraphicsScreen();

    /**
     * @return this Screen index of all Screens of {@link #getDisplay()}.
     */
    public abstract int getIndex();

    /**
     * @return the x position of the virtual viewport's top-left origin.
     */
    public abstract int getX();
    
    /**
     * @return the y position of the virtual viewport's top-left origin.
     */
    public abstract int getY();
    
    /**
     * @return the <b>rotated</b> virtual viewport's width.
     */
    public abstract int getWidth();

    /**
     * @return the <b>rotated</b> virtual viewport's height.
     */
    public abstract int getHeight();

    /**
     * @return the <b>rotated</b> virtual viewport, i.e. origin and size.
     */
    public abstract RectangleImmutable getViewport();
    
    /**
     * @return the associated Display
     */
    public abstract Display getDisplay();

    /** 
     * @return The screen fully qualified Screen name,
     * which is a key of {@link com.jogamp.newt.Display#getFQName()} + {@link #getIndex()}.
     */
    public abstract String getFQName();

    /** 
     * Return a list of all {@link MonitorMode}s for all {@link MonitorDevice}s.
     * <p>
     * The list is ordered in descending order,
     * see {@link MonitorMode#compareTo(MonitorMode)}.
     * </p>
     */
    public abstract List<MonitorMode> getMonitorModes();

    /** 
     * Return a list of available {@link MonitorDevice}s.
     */
    public abstract List<MonitorDevice> getMonitorDevices();

    /**
     * Returns the {@link MonitorDevice} which {@link MonitorDevice#getViewport() viewport} 
     * {@link MonitorDevice#coverage(RectangleImmutable) covers} the given rectangle the most.
     * <p>
     * If no coverage is detected the first {@link MonitorDevice} is returned. 
     * </p>
     */
    public final MonitorDevice getMainMonitor(RectangleImmutable r) {
        MonitorDevice res = null;
        float maxCoverage = Float.MIN_VALUE;
        final List<MonitorDevice> monitors = getMonitorDevices();
        for(int i=monitors.size()-1; i>=0; i--) {            
            final MonitorDevice monitor = monitors.get(i);
            final float coverage = monitor.coverage(r);
            if( coverage > maxCoverage ) {
                maxCoverage = coverage;
                res = monitor;
            }
        }
        if( maxCoverage > 0.0f && null != res ) {
            return res;
        }
        return monitors.get(0);
    }

    /**
     * Returns the union of all monitor's {@link MonitorDevice#getViewport() viewport}.
     * <p>
     * Should be equal to {@link #getX()}, {@link #getY()}, {@link #getWidth()} and {@link #getHeight()},
     * however, some native toolkits may choose a different virtual screen area. 
     * </p>
     * @param result storage for result, will be returned
     */
    public final Rectangle unionOfMonitorViewportSize(final Rectangle result) {
        return MonitorDevice.unionOfViewports(result, getMonitorDevices());
    }
    
    /**
     * @param sml {@link MonitorModeListener} to be added for {@link MonitorEvent}
     */
    public abstract void addMonitorModeListener(MonitorModeListener sml);

    /**
     * @param sml {@link MonitorModeListener} to be removed from {@link MonitorEvent}
     */
    public abstract void removeMonitorModeListener(MonitorModeListener sml);

    // Global Screens
    protected static final ArrayList<WeakReference<Screen>> screenList = new ArrayList<WeakReference<Screen>>();
    protected static int screensActive = 0;

    /**
     *
     * @param type
     * @param name
     * @param fromIndex start index, then increasing until found or end of list     *
     * @return
     */
    public static Screen getFirstScreenOf(Display display, int idx, int fromIndex) {
        return getScreenOfImpl(display, idx, fromIndex, 1);
    }

    /**
     *
     * @param type
     * @param name
     * @param fromIndex start index, then decreasing until found or end of list. -1 is interpreted as size - 1.
     * @return
     */
    public static Screen getLastScreenOf(Display display, int idx, int fromIndex) {
        return getScreenOfImpl(display, idx, fromIndex, -1);
    }

    private static Screen getScreenOfImpl(Display display, int idx, int fromIndex, int incr) {
        synchronized(screenList) {
            int i = fromIndex >= 0 ? fromIndex : screenList.size() - 1 ;
            while( ( incr > 0 ) ? i < screenList.size() : i >= 0 ) {
                final Screen screen = (Screen) screenList.get(i).get();
                if( null == screen ) {
                    // Clear GC'ed dead reference entry!
                    screenList.remove(i);
                    if( incr < 0 ) {
                        // decrease
                        i+=incr;
                    } // else nop - remove shifted subsequent elements to the left
                } else {
                    if( screen.getDisplay().equals(display) &&
                        screen.getIndex() == idx ) {
                        return screen;
                    }
                    i+=incr;
                }
            }
        }
        return null;
    }
    
    protected static void addScreen2List(Screen screen) {
        synchronized(screenList) {
            // GC before add
            int i=0;
            while( i < screenList.size() ) {
                if( null == screenList.get(i).get() ) {
                    screenList.remove(i);
                } else {
                    i++;
                }
            }
            screenList.add(new WeakReference<Screen>(screen));
        }
    }
    
    /** Returns the global screen collection */
    public static Collection<Screen> getAllScreens() {
        ArrayList<Screen> list;
        synchronized(screenList) {
            list = new ArrayList<Screen>();
            int i = 0;
            while( i < screenList.size() ) {
                final Screen s = screenList.get(i).get();
                if( null == s ) {
                    screenList.remove(i);
                } else {
                    list.add( screenList.get(i).get() );
                    i++;
                }
            }
        }
        return list;
    }
    
    public static int getActiveScreenNumber() {
        synchronized(screenList) {
            return screensActive;
        }
    }
}
