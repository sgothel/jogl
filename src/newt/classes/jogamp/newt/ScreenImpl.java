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

package jogamp.newt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;

import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.newt.Display;
import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.MonitorMode;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.MonitorEvent;
import com.jogamp.newt.event.MonitorModeListener;
import com.jogamp.newt.util.MonitorModeUtil;

public abstract class ScreenImpl extends Screen implements MonitorModeListener {
    protected static final boolean DEBUG_TEST_SCREENMODE_DISABLED;

    static {
        Debug.initSingleton();
        DEBUG_TEST_SCREENMODE_DISABLED = PropertyAccess.isPropertyDefined("newt.test.Screen.disableScreenMode", true);
    }

    public static final int default_sm_bpp = 32;
    public static final int default_sm_widthmm = 519;
    public static final int default_sm_heightmm = 324;
    public static final int default_sm_rate = 60;
    public static final int default_sm_rotation = 0;

    static {
        DisplayImpl.initSingleton();
    }

    /** Ensure static init has been run. */
    /* pp */static void initSingleton() { }

    protected DisplayImpl display;
    protected int screen_idx;
    protected String fqname;
    protected int hashCode;
    protected AbstractGraphicsScreen aScreen;
    protected int refCount; // number of Screen references by Window
    protected Rectangle virtViewportPU = new Rectangle(0, 0, 0, 0); // virtual rotated viewport in pixel units
    protected Rectangle virtViewportWU = new Rectangle(0, 0, 0, 0); // virtual rotated viewport in window units
    protected static Dimension usrSize = null; // property values: newt.ws.swidth and newt.ws.sheight
    protected static volatile boolean usrSizeQueried = false;
    private final ArrayList<MonitorModeListener> refMonitorModeListener = new ArrayList<MonitorModeListener>();

    private long tCreated; // creationTime

    private static Class<?> getScreenClass(final String type) throws ClassNotFoundException
    {
        final Class<?> screenClass = NewtFactory.getCustomClass(type, "ScreenDriver");
        if(null==screenClass) {
            throw new ClassNotFoundException("Failed to find NEWT Screen Class <"+type+".ScreenDriver>");
        }
        return screenClass;
    }

    public static Screen create(final Display display, int idx) {
        try {
            if(!usrSizeQueried) {
                synchronized (Screen.class) {
                    if(!usrSizeQueried) {
                        usrSizeQueried = true;
                        final int w = PropertyAccess.getIntProperty("newt.ws.swidth", true, 0);
                        final int h = PropertyAccess.getIntProperty("newt.ws.sheight", true, 0);
                        if(w>0 && h>0) {
                            usrSize = new Dimension(w, h);
                            System.err.println("User screen size "+usrSize);
                        }
                    }
                }
            }
            synchronized(screenList) {
                final Class<?> screenClass = getScreenClass(display.getType());
                ScreenImpl screen  = (ScreenImpl) screenClass.newInstance();
                screen.display = (DisplayImpl) display;
                idx = screen.validateScreenIndex(idx);
                {
                    final Screen screen0 = Screen.getLastScreenOf(display, idx, -1);
                    if(null != screen0) {
                        if(DEBUG) {
                            System.err.println("Screen.create() REUSE: "+screen0+" "+Display.getThreadName());
                        }
                        screen = null;
                        return screen0;
                    }
                }
                screen.screen_idx = idx;
                screen.fqname = display.getFQName()+"-s"+idx;
                screen.hashCode = screen.fqname.hashCode();
                screen.instantiationFinished();
                Screen.addScreen2List(screen);
                if(DEBUG) {
                    System.err.println("Screen.create() NEW: "+screen+" "+Display.getThreadName());
                }
                return screen;
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Notifies the driver impl. that the instantiation is finished,
     * ie. instance created and all fields set.
     */
    protected void instantiationFinished() {
        // nop
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ScreenImpl other = (ScreenImpl) obj;
        if (this.display != other.display && (this.display == null || !this.display.equals(other.display))) {
            return false;
        }
        if (this.screen_idx != other.screen_idx) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public synchronized final void createNative()
            throws NativeWindowException
    {
        if(null == aScreen) {
            if(DEBUG) {
                tCreated = System.nanoTime();
                System.err.println("Screen.createNative() START ("+Display.getThreadName()+", "+this+")");
            } else {
                tCreated = 0;
            }
            display.addReference();

            createNativeImpl();
            if(null == aScreen) {
                throw new NativeWindowException("Screen.createNative() failed to instanciate an AbstractGraphicsScreen");
            }

            initMonitorState();
            synchronized(screenList) {
                screensActive++;
                if(DEBUG) {
                    System.err.println("Screen.createNative() END ("+Display.getThreadName()+", "+this+"), active "+screensActive+", total "+ (System.nanoTime()-tCreated)/1e6 +"ms");
                }
            }
            ScreenMonitorState.getScreenMonitorState(this.getFQName()).addListener(this);
        }
    }

    @Override
    public synchronized final void destroy() {
        synchronized(screenList) {
            if(0 < screensActive) {
                screensActive--;
            }
            if(DEBUG) {
                System.err.println("Screen.destroy() ("+Display.getThreadName()+"): active "+screensActive);
                // Thread.dumpStack();
            }
        }

        if ( null != aScreen ) {
            releaseMonitorState();
            closeNativeImpl();
            aScreen = null;
        }
        refCount = 0;
        display.removeReference();
    }

    @Override
    public synchronized final int addReference() throws NativeWindowException {
        if(DEBUG) {
            System.err.println("Screen.addReference() ("+Display.getThreadName()+"): "+refCount+" -> "+(refCount+1));
            // Thread.dumpStack();
        }
        if ( 0 == refCount ) {
            createNative();
        } else if(null == aScreen) {
            throw new NativeWindowException("Screen.addReference() (refCount "+refCount+") null AbstractGraphicsScreen");
        }
        return ++refCount;
    }

    @Override
    public synchronized final int removeReference() {
        if(DEBUG) {
            System.err.println("Screen.removeReference() ("+Display.getThreadName()+"): "+refCount+" -> "+(refCount-1));
            // Thread.dumpStack();
        }
        refCount--; // could become < 0, in case of manual destruction without actual creation/addReference
        if(0>=refCount) {
            destroy();
            refCount=0; // fix < 0
        }
        return refCount;
    }

    @Override
    public synchronized final int getReferenceCount() {
        return refCount;
    }

    protected abstract void createNativeImpl();
    protected abstract void closeNativeImpl();

    /**
     * Returns the validated screen index, which is either the passed <code>idx</code>
     * value or <code>0</code>.
     * <p>
     * On big-desktops this shall return always 0.
     * </p>
     */
    protected abstract int validateScreenIndex(int idx);

    /**
     * Calculates the virtual rotated viewport in pixel- and window units.
     * <p>
     * This method is called after the MonitorMode has been set or changed,
     * hence you may utilize it.
     * </p>
     * <p>
     * Default implementation uses the union of all monitor's viewport,
     * calculated via {@link #unionOfMonitorViewportSize()}, however driver impl. may chose a different methodology.
     * </p>
     * @param viewport storage for result in pixel units
     * @param viewportInWindowUnits storage for result in window units
     */
    protected void calcVirtualScreenOriginAndSize(final Rectangle viewport, final Rectangle viewportInWindowUnits) {
        unionOfMonitorViewports(viewport, viewportInWindowUnits);
    }

    @Override
    public final String getFQName() {
        return fqname;
    }

    /**
     * Updates the <b>rotated</b> virtual viewport, may use native impl.
     */
    protected void updateVirtualScreenOriginAndSize() {
        if(null != usrSize ) {
            virtViewportPU.set(0, 0, usrSize.getWidth(), usrSize.getHeight());
            virtViewportWU.set(0, 0, usrSize.getWidth(), usrSize.getHeight());
            if(DEBUG) {
                System.err.println("Update user virtual screen viewport @ "+Thread.currentThread().getName()+": "+virtViewportPU);
            }
        } else {
            calcVirtualScreenOriginAndSize(virtViewportPU, virtViewportWU);
            if(DEBUG) {
                System.err.println("Updated virtual screen viewport @ "+Thread.currentThread().getName()+": "+virtViewportPU+" [pixel], "+virtViewportWU+" [window]");
            }
        }
    }

    @Override
    public final Display getDisplay() {
        return display;
    }

    @Override
    public final int getIndex() {
        return screen_idx;
    }

    @Override
    public final AbstractGraphicsScreen getGraphicsScreen() {
        return aScreen;
    }

    @Override
    public synchronized final boolean isNativeValid() {
        return null != aScreen;
    }

    @Override
    public final int getX() { return virtViewportPU.getX(); }
    @Override
    public final int getY() { return virtViewportPU.getY(); }
    @Override
    public final int getWidth() { return virtViewportPU.getWidth(); }
    @Override
    public final int getHeight() { return virtViewportPU.getHeight(); }
    @Override
    public final RectangleImmutable getViewport() { return virtViewportPU; }
    @Override
    public final RectangleImmutable getViewportInWindowUnits() { return virtViewportWU; }

    @Override
    public String toString() {
        return "NEWT-Screen["+getFQName()+", idx "+screen_idx+", refCount "+refCount+", vsize "+virtViewportPU+" [pixels], "+virtViewportWU+" [window], "+aScreen+", "+display+
                            ", monitors: "+getMonitorDevices()+"]";
    }

    //
    // MonitorDevice and MonitorMode
    //

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     * Is called only to collect the {@link MonitorMode}s and {@link MonitorDevice}s, usually at startup setting up modes.<br>
     * <br>
     * <b>WARNING</b>: must be synchronized with
     * <ul>
     *   <li>{@link MonitorModeProps#NUM_SCREEN_MODE_PROPERTIES} and </li>
     *   <li>{@link MonitorModeProps#MIN_MONITOR_DEVICE_PROPERTIES}</li>
     * </ul>, i.e.
     * <ul>
     *   <li>{@link MonitorModeProps#streamInMonitorDevice(jogamp.newt.MonitorModeProps.Cache, ScreenImpl, double[], int[], int, int[])}</li>
     *   <li>{@link MonitorModeProps#streamInMonitorDevice(int[], jogamp.newt.MonitorModeProps.Cache, ArrayHashSet, int[], int, ScreenImpl)}</li>
     *   <li>{@link MonitorModeProps#streamInMonitorMode(int[], jogamp.newt.MonitorModeProps.Cache, int[], int)}</li>
     * </ul>
     * @param cache memory pool caching the result
     */
    protected abstract void collectNativeMonitorModesAndDevicesImpl(MonitorModeProps.Cache cache);

    protected boolean updateNativeMonitorDeviceViewportImpl(final MonitorDevice monitor,
                                                            final float[] pixelScale,
                                                            final Rectangle viewportPU, final Rectangle viewportWU) {
        return false;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     * <p>
     * Implementation shall not unify the result w/ monitor's supported modes or a locally
     * saved {@link MonitorModeProps.Cache}, since caller will perform such tasks.
     * </p>
     */
    protected abstract MonitorMode queryCurrentMonitorModeImpl(MonitorDevice monitor);

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     */
    protected abstract boolean setCurrentMonitorModeImpl(MonitorDevice monitor, MonitorMode mode);

    @Override
    public final List<MonitorMode> getMonitorModes() {
        final ScreenMonitorState sms = getScreenMonitorStatus(false);
        return null != sms ? sms.getMonitorModes().getData() : null;
    }

    @Override
    public final List<MonitorDevice> getMonitorDevices() {
        final ScreenMonitorState sms = getScreenMonitorStatus(false);
        return null != sms ? sms.getMonitorDevices().getData() : null;
    }

    @Override
    public MonitorDevice getPrimaryMonitor() {
        final ScreenMonitorState sms = getScreenMonitorStatus(false);
        return null != sms ? sms.getPrimaryMonitorDevice() : null;
    }

    final ScreenMonitorState getScreenMonitorStatus(final boolean throwException) {
        final String key = this.getFQName();
        final ScreenMonitorState res = ScreenMonitorState.getScreenMonitorState(key);
        if(null == res & throwException) {
            throw new InternalError("ScreenMonitorStatus.getMonitorModeStatus("+key+") == null");
        }
        return res;
    }

    @Override
    public void monitorModeChangeNotify(final MonitorEvent me) {
        if(DEBUG) {
            System.err.println("monitorModeChangeNotify @ "+Thread.currentThread().getName()+": "+me);
        }
        for(int i=0; i<refMonitorModeListener.size(); i++) {
            refMonitorModeListener.get(i).monitorModeChangeNotify(me);
        }
    }

    private void updateNativeMonitorDevicesViewport() {
        final List<MonitorDevice> monitors = getMonitorDevices();
        for(int i=monitors.size()-1; i>=0; i--) {
            final MonitorDeviceImpl monitor = (MonitorDeviceImpl) monitors.get(i);
            final float[] pixelScale = monitor.getPixelScale(new float[2]);
            final boolean viewportUpdated = updateNativeMonitorDeviceViewportImpl(monitor, pixelScale,
                                                                                  monitor.getMutuableViewportPU(),
                                                                                  monitor.getMutuableViewportWU());
            if( DEBUG ) {
                System.err.println("Screen.updateMonitorViewport["+Display.toHexString(monitor.getId())+"] @  "+Thread.currentThread().getName()+": updated: "+viewportUpdated+
                                   ", PU "+monitor.getViewport()+", WU "+monitor.getViewportInWindowUnits()+
                                   ", pixelScale ["+pixelScale[0]+", "+pixelScale[1]+"]");
            }
        }
    }

    @Override
    public void monitorModeChanged(final MonitorEvent me, final boolean success) {
        if(success) {
            updateNativeMonitorDevicesViewport();
            updateVirtualScreenOriginAndSize();
        }
        if(DEBUG) {
            System.err.println("monitorModeChangeNotify @ "+Thread.currentThread().getName()+": success "+success+", "+me);
        }
        for(int i=0; i<refMonitorModeListener.size(); i++) {
            refMonitorModeListener.get(i).monitorModeChanged(me, success);
        }
    }

    @Override
    public synchronized final void addMonitorModeListener(final MonitorModeListener sml) {
        refMonitorModeListener.add(sml);
    }

    @Override
    public synchronized final void removeMonitorModeListener(final MonitorModeListener sml) {
        refMonitorModeListener.remove(sml);
    }

    /**
     *
     * @param cache optional ..
     * @param modeId
     * @return
     */
    private final MonitorMode getVirtualMonitorMode(final MonitorModeProps.Cache cache, final int modeId) {
        final int[] props = new int[MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL];
        int i = 0;
        props[i++] = MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL;
        props[i++] = getWidth();  // width
        props[i++] = getHeight(); // height
        props[i++] = default_sm_bpp;
        props[i++] = default_sm_rate * 100;
        props[i++] = 0; // flags
        props[i++] = modeId;
        props[i++] = default_sm_rotation;
        if( MonitorModeProps.NUM_MONITOR_MODE_PROPERTIES_ALL != i ) {
            throw new InternalError("XX");
        }
        return MonitorModeProps.streamInMonitorMode(null, cache, props, 0);
    }

    /**
     *
     * @param cache mandatory !
     * @param monitorId
     * @param currentMode
     * @return
     */
    private final MonitorDevice getVirtualMonitorDevice(final MonitorModeProps.Cache cache, final int monitorId, final MonitorMode currentMode) {
        final int[] props = new int[MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES];
        int i = 0;
        props[i++] = MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES;
        props[i++] = monitorId;
        props[i++] = 0; // is-clone
        props[i++] = 0 == monitorId ? 1 : 0; // is-primary
        props[i++] = default_sm_widthmm;
        props[i++] = default_sm_heightmm;
        props[i++] = 0; // rotated viewport x pixel-units
        props[i++] = 0; // rotated viewport y pixel-units
        props[i++] = currentMode.getRotatedWidth();  // rotated viewport width pixel-units
        props[i++] = currentMode.getRotatedHeight(); // rotated viewport height pixel-units
        props[i++] = 0; // rotated viewport x window-units
        props[i++] = 0; // rotated viewport y window-units
        props[i++] = currentMode.getRotatedWidth();  // rotated viewport width window-units
        props[i++] = currentMode.getRotatedHeight(); // rotated viewport height window-units
        props[i++] = currentMode.getId(); // current mode id
        props[i++] = currentMode.getRotation();
        props[i++] = currentMode.getId(); // supported mode id #1
        if( MonitorModeProps.MIN_MONITOR_DEVICE_PROPERTIES != i ) {
            throw new InternalError("XX");
        }
        return MonitorModeProps.streamInMonitorDevice(cache, this, null, props, 0, null);
    }

    /**
     * Utilizes {@link #getCurrentMonitorModeImpl()}, if the latter returns null it uses
     * the current screen size and dummy values.
     */
    protected final MonitorMode queryCurrentMonitorModeIntern(final MonitorDevice monitor) {
        MonitorMode res;
        if(DEBUG_TEST_SCREENMODE_DISABLED) {
            res = null;
        } else {
            res = queryCurrentMonitorModeImpl(monitor);
        }
        if(null == res) {
            if( 0>=getWidth() || 0>=getHeight() ) {
                updateVirtualScreenOriginAndSize();
            }
            res = getVirtualMonitorMode(null, monitor.getCurrentMode().getId());
        }
        return res;
    }

    private final ScreenMonitorState initMonitorState() {
        long t0;
        if(DEBUG) {
            t0 = System.nanoTime();
            System.err.println("Screen.initMonitorState() START ("+Display.getThreadName()+", "+this+")");
        } else {
            t0 = 0;
        }

        boolean vScrnSizeUpdated = false;
        ScreenMonitorState sms;
        ScreenMonitorState.lockScreenMonitorState();
        try {
            sms = ScreenMonitorState.getScreenMonitorState(this.getFQName());
            if(null==sms) {
                final MonitorModeProps.Cache cache = new MonitorModeProps.Cache();
                if( 0 >= collectNativeMonitorModes(cache) ) {
                    updateVirtualScreenOriginAndSize();
                    vScrnSizeUpdated = true;
                    final MonitorMode mode = getVirtualMonitorMode(cache, 0);
                    cache.monitorModes.getOrAdd(mode);
                    final MonitorDevice monitor = getVirtualMonitorDevice(cache, 0, mode);
                    ((MonitorDeviceImpl)monitor).setIsPrimary(true);
                    cache.monitorDevices.getOrAdd(monitor);
                    cache.setPrimary(monitor);
                }
                if( null == cache.getPrimary() ) {
                    // Fallback ..
                    final MonitorDevice p = cache.monitorDevices.get(0);
                    ((MonitorDeviceImpl)p).setIsPrimary(true);
                    cache.setPrimary(p);
                    if( DEBUG ) {
                        System.err.println("WARNING: Fallback primary: "+p);
                    }
                }
                // Sort MonitorModes (all and per device) in descending order - default!
                MonitorModeUtil.sort(cache.monitorModes.getData(), false ); // descending order
                for(final Iterator<MonitorDevice> iMonitor=cache.monitorDevices.iterator(); iMonitor.hasNext(); ) {
                    MonitorModeUtil.sort(iMonitor.next().getSupportedModes(), false ); // descending order
                }
                if(DEBUG) {
                    int i=0;
                    for(final Iterator<MonitorMode> iMode=cache.monitorModes.iterator(); iMode.hasNext(); i++) {
                        System.err.println("All["+i+"]: "+iMode.next());
                    }
                    i=0;
                    for(final Iterator<MonitorDevice> iMonitor=cache.monitorDevices.iterator(); iMonitor.hasNext(); i++) {
                        final MonitorDevice crt = iMonitor.next();
                        System.err.println("["+i+"]: "+crt);
                        int j=0;
                        for(final Iterator<MonitorMode> iMode=crt.getSupportedModes().iterator(); iMode.hasNext(); j++) {
                            System.err.println("["+i+"]["+j+"]: "+iMode.next());
                        }
                    }
                    System.err.println("Primary: "+cache.getPrimary());
                }
                sms = new ScreenMonitorState(cache.monitorDevices, cache.monitorModes, cache.getPrimary());
                ScreenMonitorState.mapScreenMonitorState(this.getFQName(), sms);
            }
        } finally {
            ScreenMonitorState.unlockScreenMonitorState();
        }
        if(DEBUG) {
            System.err.println("Screen.initMonitorState() END dt "+ (System.nanoTime()-t0)/1e6 +"ms");
        }
        if( !vScrnSizeUpdated ) {
            updateVirtualScreenOriginAndSize();
        }

        return sms;
    }

    /**
     * Returns the number of successful collected {@link MonitorDevice}s.
     * <p>
     * Collects {@link MonitorDevice}s and {@link MonitorMode}s within the given cache.
     * </p>
     */
    private final int collectNativeMonitorModes(final MonitorModeProps.Cache cache) {
        if(!DEBUG_TEST_SCREENMODE_DISABLED) {
            collectNativeMonitorModesAndDevicesImpl(cache);
            MonitorModeProps.identifyMonitorDevices(cache);
        }
        // filter out insufficient modes
        for(int i=cache.monitorModes.size()-1; i>=0; i--) {
            final MonitorMode mode = cache.monitorModes.get(i);
            if( 16 > mode.getSurfaceSize().getBitsPerPixel() ) {
                boolean keep = false;
                for(int j=cache.monitorDevices.size()-1; !keep && j>=0; j--) {
                    final MonitorDevice monitor = cache.monitorDevices.get(j);
                    keep = monitor.getCurrentMode().equals(mode);
                }
                if(!keep) {
                    cache.monitorModes.remove(i);
                    for(int j=cache.monitorDevices.size()-1; j>=0; j--) {
                        final MonitorDeviceImpl monitor = (MonitorDeviceImpl) cache.monitorDevices.get(j);
                        monitor.getSupportedModesImpl().remove(mode);
                    }
                }
            }
        }
        if( DEBUG ) {
            System.err.println("ScreenImpl.collectNativeMonitorModes: MonitorDevice number : "+cache.monitorDevices.size());
            System.err.println("ScreenImpl.collectNativeMonitorModes: MonitorMode number   : "+cache.monitorModes.size());
            System.err.println("ScreenImpl.collectNativeMonitorModes: SizeAndRate number   : "+cache.sizeAndRates.size());
            System.err.println("ScreenImpl.collectNativeMonitorModes: SurfaceSize number   : "+cache.surfaceSizes.size());
            System.err.println("ScreenImpl.collectNativeMonitorModes: Resolution number    : "+cache.resolutions.size());
        }
        return cache.monitorDevices.size();
    }

    private final void releaseMonitorState() {
        ScreenMonitorState sms;
        ScreenMonitorState.lockScreenMonitorState();
        try {
            sms = ScreenMonitorState.getScreenMonitorState(getFQName());
            if(null != sms) {
                sms.lock();
                try {
                    if(0 == sms.removeListener(this)) {
                        final ArrayList<MonitorDevice> monitorDevices = sms.getMonitorDevices().getData();
                        for(int i=0; i<monitorDevices.size(); i++) {
                            final MonitorDevice monitor = monitorDevices.get(i);
                            if( monitor.isModeChangedByUs() ) {
                                System.err.println("Screen.destroy(): Reset "+monitor);
                                try {
                                    monitor.setCurrentMode(monitor.getOriginalMode());
                                } catch (final Throwable t) {
                                    // be verbose but continue
                                    t.printStackTrace();
                                }
                            }
                        }
                        ScreenMonitorState.unmapScreenMonitorState(getFQName());
                    }
                } finally {
                    sms.unlock();
                }
            }
        } finally {
            ScreenMonitorState.unlockScreenMonitorState();
        }
    }

    private final void shutdown() {
        final ScreenMonitorState sms = ScreenMonitorState.getScreenMonitorStateUnlocked(getFQName());
        if(null != sms) {
            final ArrayList<MonitorDevice> monitorDevices = sms.getMonitorDevices().getData();
            for(int i=0; i<monitorDevices.size(); i++) {
                final MonitorDevice monitor = monitorDevices.get(i);
                if( monitor.isModeChangedByUs() ) {
                    System.err.println("Screen.shutdown(): Reset "+monitor);
                    try {
                        monitor.setCurrentMode(monitor.getOriginalMode());
                    } catch (final Throwable t) {
                        // be quiet .. shutdown
                    }
                }
            }
            ScreenMonitorState.unmapScreenMonitorStateUnlocked(getFQName());
        }
    }

    /** pp */ static final void shutdownAll() {
        final int sCount = screenList.size();
        if(DEBUG) {
            System.err.println("Screen.shutdownAll "+sCount+" instances, on thread "+Display.getThreadName());
        }
        for(int i=0; i<sCount && screenList.size()>0; i++) { // be safe ..
            final ScreenImpl s = (ScreenImpl) screenList.remove(0).get();
            if(DEBUG) {
                System.err.println("Screen.shutdownAll["+(i+1)+"/"+sCount+"]: "+s+", GCed "+(null==s));
            }
            if( null != s ) {
                s.shutdown();
            }
        }
    }
}
