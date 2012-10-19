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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.SurfaceSize;


import com.jogamp.common.util.ArrayHashSet;
import com.jogamp.common.util.IntIntHashMap;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.ScreenMode;
import com.jogamp.newt.event.ScreenModeListener;
import com.jogamp.newt.util.MonitorMode;
import com.jogamp.newt.util.ScreenModeUtil;

public abstract class ScreenImpl extends Screen implements ScreenModeListener {
    protected static final boolean DEBUG_TEST_SCREENMODE_DISABLED = Debug.isPropertyDefined("newt.test.Screen.disableScreenMode", true);

    protected static final int default_sm_bpp = 32;
    protected static final int default_sm_widthmm = 519;
    protected static final int default_sm_heightmm = 324;
    protected static final int default_sm_rate = 60;
    protected static final int default_sm_rotation = 0;
    
    protected DisplayImpl display;
    protected int screen_idx;
    protected String fqname;
    protected int hashCode;
    protected AbstractGraphicsScreen aScreen;
    protected int refCount; // number of Screen references by Window
    protected Point vOrigin = new Point(0, 0); // virtual top-left origin
    protected Dimension vSize = new Dimension(0, 0); // virtual rotated screen size
    protected static Dimension usrSize = null; // property values: newt.ws.swidth and newt.ws.sheight
    protected static volatile boolean usrSizeQueried = false;
    private ArrayList<ScreenModeListener> referencedScreenModeListener = new ArrayList<ScreenModeListener>();
    private long tCreated; // creationTime

    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                registerShutdownHook();
                return null;
            }
        });
    }
    
    private static Class<?> getScreenClass(String type) throws ClassNotFoundException 
    {
        final Class<?> screenClass = NewtFactory.getCustomClass(type, "ScreenDriver");
        if(null==screenClass) {
            throw new ClassNotFoundException("Failed to find NEWT Screen Class <"+type+".ScreenDriver>");            
        }
        return screenClass;
    }

    public static Screen create(Display display, int idx) {
        try {
            if(!usrSizeQueried) {
                synchronized (Screen.class) {
                    if(!usrSizeQueried) {
                        usrSizeQueried = true;
                        final int w = Debug.getIntProperty("newt.ws.swidth", true, 0);
                        final int h = Debug.getIntProperty("newt.ws.sheight", true, 0);                        
                        if(w>0 && h>0) {
                            usrSize = new Dimension(w, h);
                            System.err.println("User screen size "+usrSize);
                        }
                    }
                }
            }
            synchronized(screenList) {
                Class<?> screenClass = getScreenClass(display.getType());
                ScreenImpl screen  = (ScreenImpl) screenClass.newInstance();
                screen.display = (DisplayImpl) display;
                idx = screen.validateScreenIndex(idx);
                {
                    Screen screen0 = ScreenImpl.getLastScreenOf(display, idx, -1);
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
                screenList.add(screen);
                if(DEBUG) {
                    System.err.println("Screen.create() NEW: "+screen+" "+Display.getThreadName());
                }
                return screen;
            }            
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object obj) {
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

    public int hashCode() {
        return hashCode;
    }

    public synchronized final void createNative()
            throws NativeWindowException
    {        
        if(null == aScreen) {
            if(DEBUG) {
                tCreated = System.nanoTime();
                System.err.println("Screen.createNative() START ("+DisplayImpl.getThreadName()+", "+this+")");
            } else {
                tCreated = 0;
            }
            
            display.addReference();
            
            createNativeImpl();
            if(null == aScreen) {
                throw new NativeWindowException("Screen.createNative() failed to instanciate an AbstractGraphicsScreen");
            }
            
            initScreenModeStatus();
            updateVirtualScreenOriginAndSize();            
            if(DEBUG) {
                System.err.println("Screen.createNative() END ("+DisplayImpl.getThreadName()+", "+this+"), total "+ (System.nanoTime()-tCreated)/1e6 +"ms");
            }
            synchronized(screenList) {
                screensActive++;
            }
        }
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        sms.addListener(this);
    }

    public synchronized final void destroy() {
        releaseScreenModeStatus();

        synchronized(screenList) {
            screenList.remove(this);
            if(0 < screensActive) {
                screensActive--;
            }
        }

        if ( null != aScreen ) {
            closeNativeImpl();
            aScreen = null;
        }
        refCount = 0;
        display.removeReference();
    }

    public synchronized final int addReference() throws NativeWindowException {
        if(DEBUG) {
            System.err.println("Screen.addReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount+1));
            // Thread.dumpStack();
        }
        if ( 0 == refCount ) {
            createNative();
        }
        if(null == aScreen) {
            throw new NativeWindowException("Screen.addReference() (refCount "+refCount+") null AbstractGraphicsScreen");
        }
        return ++refCount;
    }

    public synchronized final int removeReference() {
        if(DEBUG) {
            System.err.println("Screen.removeReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount-1));
            // Thread.dumpStack();
        }
        refCount--; // could become < 0, in case of manual destruction without actual creation/addReference
        if(0>=refCount) {
            destroy();
            refCount=0; // fix < 0
        }
        return refCount;
    }

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
     * Stores the virtual origin and virtual <b>rotated</b> screen size.
     * <p>
     * This method is called after the ScreenMode has been set, 
     * hence you may utilize it.
     * </p> 
     * @param virtualOrigin the store for the virtual origin
     * @param virtualSize the store for the virtual rotated size
     */
    protected abstract void getVirtualScreenOriginAndSize(Point virtualOrigin, Dimension virtualSize); 
    
    public final String getFQName() {
        return fqname;
    }

    /**
     * Updates the <b>rotated</b> virtual ScreenSize using the native impl.
     */
    protected void updateVirtualScreenOriginAndSize() {
        getVirtualScreenOriginAndSize(vOrigin, vSize);
        if(DEBUG) {
            System.err.println("Detected virtual screen origin "+vOrigin+", size "+vSize);
        }
    }

    public final Display getDisplay() {
        return display;
    }

    public final int getIndex() {
        return screen_idx;
    }

    public final AbstractGraphicsScreen getGraphicsScreen() {
        return aScreen;
    }

    public synchronized final boolean isNativeValid() {
        return null != aScreen;
    }

    public int getX() { return vOrigin.getX(); }
    public int getY() { return vOrigin.getY(); }
    
    public final int getWidth() {
        return (null != usrSize) ? usrSize.getWidth() : vSize.getWidth();
    }

    public final int getHeight() {
        return (null != usrSize) ? usrSize.getHeight() : vSize.getHeight();
    }

    @Override
    public String toString() {
        return "NEWT-Screen["+getFQName()+", idx "+screen_idx+", refCount "+refCount+", "+getWidth()+"x"+getHeight()+", "+aScreen+", "+display+"]";
    }

    public final List<ScreenMode> getScreenModes() {
        ArrayHashSet<ScreenMode> screenModes = getScreenModesOrig();
        if(null != screenModes && 0 < screenModes.size()) {
            return screenModes.toArrayList();
        }
        return null;
    }

    private final ScreenModeStatus getScreenModeStatus(boolean throwException) {
        final String key = this.getFQName();
        final ScreenModeStatus res = ScreenModeStatus.getScreenModeStatus(key);
        if(null == res & throwException) {
            throw new InternalError("ScreenModeStatus.getScreenModeStatus("+key+") == null");
        }
        return res;
    }
    
    public ScreenMode getOriginalScreenMode() {
        final ScreenModeStatus sms = getScreenModeStatus(false);
        return ( null != sms ) ? sms.getOriginalScreenMode() : null ;
    }

    public ScreenMode getCurrentScreenMode() {
        ScreenMode smU = null;
        final ScreenModeStatus sms = getScreenModeStatus(true);
        final ScreenMode sm0 = getCurrentScreenModeIntern();
        if(null == sm0) {
            throw new InternalError("getCurrentScreenModeImpl() == null");
        }
        sms.lock();
        try {
            smU = sms.getScreenModes().getOrAdd(sm0); // unified instance, maybe new

            // if mode has changed somehow, update it ..
            if( sms.getCurrentScreenMode().hashCode() != smU.hashCode() ) {
                sms.fireScreenModeChanged(smU, true);
            }
        } finally {
            sms.unlock();
        }
        return smU;
    }

    public boolean setCurrentScreenMode(ScreenMode screenMode) {
        final ScreenMode smC = getCurrentScreenMode();
        ScreenMode smU = getScreenModesOrig().get(screenMode); // unify via value hash
        if(smU.equals(smC)) {
            if(DEBUG) {
                System.err.println("Screen.setCurrentScreenMode ("+(System.nanoTime()-tCreated)+"): 0.0 is-current (skip) "+smU+" == "+smC);
            }            
            return true;
        }
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        if(null == sms) {
            throw new InternalError("ScreenModeStatus.getScreenModeStatus("+this.getFQName()+") == null");            
        }
        boolean success;
        sms.lock();
        try {
            final long tStart;
            if(DEBUG) {
                tStart = System.nanoTime();                
            } else {
                tStart = 0;
            }
            
            sms.fireScreenModeChangeNotify(smU);
            if(DEBUG) {
                System.err.println("Screen.setCurrentScreenMode ("+(System.nanoTime()-tStart)/1e6+"ms): fireScreenModeChangeNotify() "+smU);
            }

            success = setCurrentScreenModeImpl(smU);
            if(success) {
                if(DEBUG) {
                    System.err.println("Screen.setCurrentScreenMode ("+(System.nanoTime()-tStart)/1e6+"ms): setCurrentScreenModeImpl() "+smU+", success(1): "+success);
                }
            } else {
                // 2nd attempt validate!
                final ScreenMode queriedCurrent = getCurrentScreenMode(); // may fireScreenModeChanged(..) if successful and differs!
                final ScreenMode smsCurrent = sms.getCurrentScreenMode();
                success = smsCurrent.hashCode() == smU.hashCode() && queriedCurrent.hashCode() == smU.hashCode() ;
                if(DEBUG) {
                    System.err.println("Screen.setCurrentScreenMode.2: queried "+queriedCurrent);
                    System.err.println("Screen.setCurrentScreenMode.2:     SMS "+smsCurrent);
                    System.err.println("Screen.setCurrentScreenMode ("+(System.nanoTime()-tStart)/1e6+"ms): setCurrentScreenModeImpl() "+smU+", success(2): "+success);
                }
            }
            sms.fireScreenModeChanged(smU, success);
            if(DEBUG) {
                System.err.println("Screen.setCurrentScreenMode ("+(System.nanoTime()-tStart)/1e6+"ms): X.X "+smU+", success: "+success);
            }
        } finally {
            sms.unlock();
        }
        return success;
    }

    public void screenModeChangeNotify(ScreenMode sm) {
        for(int i=0; i<referencedScreenModeListener.size(); i++) {
            ((ScreenModeListener)referencedScreenModeListener.get(i)).screenModeChangeNotify(sm);
        }
    }

    public void screenModeChanged(ScreenMode sm, boolean success) {
        if(success) {
            updateVirtualScreenOriginAndSize();
        }
        for(int i=0; i<referencedScreenModeListener.size(); i++) {
            ((ScreenModeListener)referencedScreenModeListener.get(i)).screenModeChanged(sm, success);
        }
    }

    public synchronized final void addScreenModeListener(ScreenModeListener sml) {
        referencedScreenModeListener.add(sml);
    }

    public synchronized final void removeScreenModeListener(ScreenModeListener sml) {
        referencedScreenModeListener.remove(sml);
    }

    /** ScreenModeStatus bridge to native implementation */
    protected final ArrayHashSet<ScreenMode> getScreenModesOrig() {
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        if(null!=sms) {
            return sms.getScreenModes();
        }
        return null;
    }

    /** ScreenModeStatus bridge to native implementation */
    protected final IntIntHashMap getScreenModesIdx2NativeIdx() {
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
        if(null!=sms) {
            return sms.getScreenModesIdx2NativeIdx();
        }
        return null;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     * Is called only to collect the ScreenModes, usually at startup setting up modes.<br>
     * <br>
     * <b>WARNING</b>: must be synchronized with {@link com.jogamp.newt.util.ScreenModeUtil#NUM_SCREEN_MODE_PROPERTIES},
     * ie {@link com.jogamp.newt.util.ScreenModeUtil#streamIn(com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, int[], int)}<br>
     * <br>
     * <b>Note</b>: Additional 1st element is native mode id.
     */
    protected int[] getScreenModeFirstImpl() {
        return null;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     * Is called only to collect the ScreenModes, usually at startup setting up modes.<br>
     * <br>
     * <b>WARNING</b>: must be synchronized with {@link com.jogamp.newt.util.ScreenModeUtil#NUM_SCREEN_MODE_PROPERTIES},
     * ie {@link com.jogamp.newt.util.ScreenModeUtil#streamIn(com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, com.jogamp.common.util.ArrayHashSet, int[], int)}<br>
     * <br>
     * <b>Note</b>: Additional 1st element is native mode id.
     */
    protected int[] getScreenModeNextImpl() {
        return null;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     */
    protected ScreenMode getCurrentScreenModeImpl() {
        return null;
    }
    
    /**
     * Utilizes {@link #getCurrentScreenModeImpl()}, if the latter returns null it uses
     * the current screen size and dummy values.
     */
    protected ScreenMode getCurrentScreenModeIntern() {
        ScreenMode res;
        if(DEBUG_TEST_SCREENMODE_DISABLED) {
            res = null;
        } else {
            res = getCurrentScreenModeImpl();
        }
        if(null == res) {
            if( 0>=getWidth() || 0>=getHeight() ) {
                updateVirtualScreenOriginAndSize();
            }
            int[] props = new int[ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL];
            int i = 0;
            props[i++] = 0; // set later for verification of iterator
            props[i++] = getWidth();  // width
            props[i++] = getHeight(); // height
            props[i++] = default_sm_bpp;
            props[i++] = default_sm_widthmm;
            props[i++] = default_sm_heightmm;
            props[i++] = default_sm_rate;
            props[i++] = default_sm_rotation;
            props[i - ScreenModeUtil.NUM_SCREEN_MODE_PROPERTIES_ALL] = i; // count
            res = ScreenModeUtil.streamIn(props, 0);
        }
        return res;
    }

    /**
     * To be implemented by the native specification.<br>
     * Is called within a thread safe environment.<br>
     */
    protected boolean setCurrentScreenModeImpl(ScreenMode screenMode) {
        return false;
    }

    private ScreenModeStatus initScreenModeStatus() {
        long t0;
        if(DEBUG) {
            t0 = System.nanoTime();
            System.err.println("Screen.initScreenModeStatus() START ("+DisplayImpl.getThreadName()+", "+this+")");
        } else {
            t0 = 0;
        }

        ScreenModeStatus sms;
        ScreenModeStatus.lockScreenModeStatus();
        try {
            sms = ScreenModeStatus.getScreenModeStatus(this.getFQName());
            if(null==sms) {                
                IntIntHashMap screenModesIdx2NativeIdx = new IntIntHashMap();
                final ScreenMode currentSM = getCurrentScreenModeIntern();
                if(null == currentSM) {
                    throw new InternalError("getCurrentScreenModeImpl() == null");
                }

                ArrayHashSet<ScreenMode> screenModes = collectNativeScreenModes(screenModesIdx2NativeIdx);
                screenModes.getOrAdd(currentSM);
                if(DEBUG) {
                    int i=0;
                    for(Iterator<ScreenMode> iter=screenModes.iterator(); iter.hasNext(); i++) {
                        System.err.println(i+": "+iter.next());
                    }
                }
                
                sms = new ScreenModeStatus(screenModes, screenModesIdx2NativeIdx);
                ScreenMode originalScreenMode0 = screenModes.get(currentSM); // unify via value hash
                if(null == originalScreenMode0) {
                    throw new RuntimeException(currentSM+" could not be hashed from ScreenMode list");
                }
                sms.setOriginalScreenMode(originalScreenMode0);
                ScreenModeStatus.mapScreenModeStatus(this.getFQName(), sms);
            }
        } finally {
            ScreenModeStatus.unlockScreenModeStatus();
        }
        if(DEBUG) {
            System.err.println("Screen.initScreenModeStatus() END dt "+ (System.nanoTime()-t0)/1e6 +"ms");
        }
        return sms;
    }

    /** ignores bpp < 15 */
    private ArrayHashSet<ScreenMode> collectNativeScreenModes(IntIntHashMap screenModesIdx2NativeId) {
        ArrayHashSet<DimensionImmutable> resolutionPool   = new ArrayHashSet<DimensionImmutable>();
        ArrayHashSet<SurfaceSize>        surfaceSizePool  = new ArrayHashSet<SurfaceSize>();
        ArrayHashSet<DimensionImmutable> screenSizeMMPool = new ArrayHashSet<DimensionImmutable>();
        ArrayHashSet<MonitorMode>        monitorModePool  = new ArrayHashSet<MonitorMode>();
        ArrayHashSet<ScreenMode>         screenModePool   = new ArrayHashSet<ScreenMode>();

        int[] smProps = null;
        int num = 0;
        final int idxBpp =   1 // native mode
                           + 1 // count
                           + ScreenModeUtil.NUM_RESOLUTION_PROPERTIES
                           + ScreenModeUtil.NUM_SURFACE_SIZE_PROPERTIES
                           - 1 ; // index 0 based
        do {
            if(DEBUG_TEST_SCREENMODE_DISABLED) {
                smProps = null;
            } else if(0 == num) {
                smProps = getScreenModeFirstImpl();
            } else {
                smProps = getScreenModeNextImpl();
            }
            if(null != smProps && 0 < smProps.length && smProps[idxBpp] >= 15) {
                int nativeId = smProps[0];
                int screenModeIdx = ScreenModeUtil.streamIn(resolutionPool, surfaceSizePool, screenSizeMMPool,
                                                            monitorModePool, screenModePool, smProps, 1);
                if(DEBUG) {
                    System.err.println("ScreenImpl.collectNativeScreenModes: #"+num+": idx: "+nativeId+" native -> "+screenModeIdx+" newt");
                }
                
                if(screenModeIdx >= 0) {
                    screenModesIdx2NativeId.put(screenModeIdx, nativeId);
                }
            } else if(DEBUG) {
                System.err.println("ScreenImpl.collectNativeScreenModes: #"+num+": smProps: "+(null!=smProps)+
                                   ", len: "+(null != smProps ? smProps.length : 0)+
                                   ", bpp: "+(null != smProps && 0 < smProps.length ? smProps[idxBpp] : 0)+
                                   " - DROPPING");
            }
            num++;
        } while ( null != smProps && 0 < smProps.length );

        if(DEBUG) {
            System.err.println("ScreenImpl.collectNativeScreenModes: ScreenMode number  : "+screenModePool.size());
            System.err.println("ScreenImpl.collectNativeScreenModes: MonitorMode number : "+monitorModePool.size());
            System.err.println("ScreenImpl.collectNativeScreenModes: ScreenSizeMM number: "+screenSizeMMPool.size());
            System.err.println("ScreenImpl.collectNativeScreenModes: SurfaceSize number : "+surfaceSizePool.size());
            System.err.println("ScreenImpl.collectNativeScreenModes: Resolution number  : "+resolutionPool.size());
        }

        return screenModePool;
    }

    private void releaseScreenModeStatus() {
        ScreenModeStatus sms;
        ScreenModeStatus.lockScreenModeStatus();
        try {
            sms = ScreenModeStatus.getScreenModeStatus(getFQName());
            if(null != sms) {
                sms.lock();
                try {
                    if(0 == sms.removeListener(this)) {
                        if(sms.isOriginalModeChangedByOwner()) {
                            System.err.println("Screen.destroy(): "+sms.getCurrentScreenMode()+" -> "+sms.getOriginalScreenMode());
                            try {
                                setCurrentScreenMode(sms.getOriginalScreenMode());
                            } catch (Throwable t) {
                                // be verbose but continue
                                t.printStackTrace();
                            }
                        }
                        ScreenModeStatus.unmapScreenModeStatus(getFQName());
                    }
                } finally {
                    sms.unlock();
                }
            }            
        } finally {
            ScreenModeStatus.unlockScreenModeStatus();
        }
    }
    
    private final void shutdown() {
        ScreenModeStatus sms = ScreenModeStatus.getScreenModeStatusUnlocked(getFQName());
        if(null != sms) {
            if(sms.isOriginalModeChangedByOwner()) {
                try {
                    System.err.println("Screen.shutdown(): "+sms.getCurrentScreenMode()+" -> "+sms.getOriginalScreenMode());
                    setCurrentScreenModeImpl(sms.getOriginalScreenMode());
                } catch (Throwable t) {
                    // be quiet .. shutdown
                }
            }
            ScreenModeStatus.unmapScreenModeStatusUnlocked(getFQName());
        }            
    }
    private static final void shutdownAll() {
        for(int i=0; i < screenList.size(); i++) {
            ((ScreenImpl)screenList.get(i)).shutdown();
        }
    }
    
    private static synchronized void registerShutdownHook() {
        final Thread shutdownHook = new Thread(new Runnable() {
            public void run() {
                ScreenImpl.shutdownAll();
            }
        });
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                Runtime.getRuntime().addShutdownHook(shutdownHook);
                return null;
            }
        });
    }
}

