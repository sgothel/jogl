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

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IOUtil;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.NEWTEventConsumer;

import jogamp.newt.event.NEWTEventTask;

import com.jogamp.newt.util.EDTUtil;
import com.jogamp.opengl.util.PNGPixelRect;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.util.PixelFormatUtil;
import com.jogamp.nativewindow.util.PixelRectangle;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;

public abstract class DisplayImpl extends Display {
    private static int serialno = 1;
    private static final boolean pngUtilAvail;

    static {
        NativeWindowFactory.addCustomShutdownHook(true /* head */, new Runnable() {
           @Override
           public void run() {
               WindowImpl.shutdownAll();
               ScreenImpl.shutdownAll();
               DisplayImpl.shutdownAll();
           }
        });

        final ClassLoader cl = DisplayImpl.class.getClassLoader();
        pngUtilAvail = ReflectionUtil.isClassAvailable("com.jogamp.opengl.util.PNGPixelRect", cl);
    }

    public static final boolean isPNGUtilAvailable() { return pngUtilAvail; }

    final ArrayList<PointerIconImpl> pointerIconList = new ArrayList<PointerIconImpl>();

    /** Executed from EDT! */
    private void destroyAllPointerIconFromList(final long dpy) {
        synchronized(pointerIconList) {
            final int count = pointerIconList.size();
            for( int i=0; i < count; i++ ) {
                final PointerIconImpl item = pointerIconList.get(i);
                if(DEBUG) {
                    System.err.println("destroyAllPointerIconFromList: dpy "+toHexString(dpy)+", # "+i+"/"+count+": "+item+" @ "+getThreadName());
                }
                if( null != item && item.isValid() ) {
                    item.destroyOnEDT(dpy);
                }
            }
            pointerIconList.clear();
        }
    }

    @Override
    public PixelFormat getNativePointerIconPixelFormat() { return PixelFormat.BGRA8888; }
    @Override
    public boolean getNativePointerIconForceDirectNIO() { return false; }

    @Override
    public final PointerIcon createPointerIcon(final IOUtil.ClassResources pngResource, final int hotX, final int hotY)
            throws IllegalArgumentException, IllegalStateException, IOException
    {
        if( null == pngResource || 0 >= pngResource.resourceCount() ) {
            throw new IllegalArgumentException("Null or invalid pngResource "+pngResource);
        }
        if( !pngUtilAvail ) {
            return null;
        }
        final PointerIconImpl[] res = { null };
        final Exception[] ex = { null };
        final String exStr = "Could not resolve "+pngResource.resourcePaths[0];
        runOnEDTIfAvail(true, new Runnable() {
            public void run() {
                try {
                    if( !DisplayImpl.this.isNativeValidAsync() ) {
                        throw new IllegalStateException("Display.createPointerIcon: Display invalid "+DisplayImpl.this);
                    }
                    final URLConnection urlConn = pngResource.resolve(0);
                    if( null == urlConn ) {
                        throw new IOException(exStr);
                    }
                    final PNGPixelRect image = PNGPixelRect.read(urlConn.getInputStream(),
                                                                 getNativePointerIconPixelFormat(),
                                                                 getNativePointerIconForceDirectNIO(),
                                                                 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                    final long handle = createPointerIconImplChecked(image.getPixelformat(), image.getSize().getWidth(), image.getSize().getHeight(),
                                                                     image.getPixels(), hotX, hotY);
                    final PointImmutable hotspot = new Point(hotX, hotY);
                    if( DEBUG_POINTER_ICON ) {
                        System.err.println("createPointerIconPNG.0: "+image+", handle: "+toHexString(handle)+", hot "+hotspot);
                    }
                    if( 0 == handle ) {
                        throw new IOException(exStr);
                    }
                    res[0] = new PointerIconImpl(DisplayImpl.this, image, hotspot, handle);
                    if( DEBUG_POINTER_ICON ) {
                        System.err.println("createPointerIconPNG.0: "+res[0]);
                    }
                } catch (final Exception e) {
                    ex[0] = e;
                }
            } } );
        if( null != ex[0] ) {
            final Exception e = ex[0];
            if( e instanceof IllegalArgumentException) {
                throw new IllegalArgumentException(e);
            }
            if( e instanceof IllegalStateException) {
                throw new IllegalStateException(e);
            }
            throw new IOException(e);
        }
        if( null == res[0] ) {
            throw new IOException(exStr);
        }
        synchronized(pointerIconList) {
            pointerIconList.add(res[0]);
        }
        return res[0];
    }

    @Override
    public final PointerIcon createPointerIcon(final PixelRectangle pixelrect, final int hotX, final int hotY)
            throws IllegalArgumentException, IllegalStateException
    {
        if( null == pixelrect ) {
            throw new IllegalArgumentException("Null or pixelrect");
        }
        final PixelRectangle fpixelrect;
        if( getNativePointerIconPixelFormat() != pixelrect.getPixelformat() || pixelrect.isGLOriented() ) {
            // conversion !
            fpixelrect = PixelFormatUtil.convert(pixelrect, getNativePointerIconPixelFormat(),
                                                      0 /* ddestStride */, false /* isGLOriented */, getNativePointerIconForceDirectNIO() );
            if( DEBUG_POINTER_ICON ) {
                System.err.println("createPointerIconRES.0: Conversion-FMT "+pixelrect+" -> "+fpixelrect);
            }
        } else if( getNativePointerIconForceDirectNIO() && !Buffers.isDirect(pixelrect.getPixels()) ) {
            // transfer to direct NIO
            final ByteBuffer sBB = pixelrect.getPixels();
            final ByteBuffer dBB = Buffers.newDirectByteBuffer(sBB.array(), sBB.arrayOffset());
            fpixelrect = new PixelRectangle.GenericPixelRect(pixelrect.getPixelformat(), pixelrect.getSize(), pixelrect.getStride(), pixelrect.isGLOriented(), dBB);
            if( DEBUG_POINTER_ICON ) {
                System.err.println("createPointerIconRES.0: Conversion-NIO "+pixelrect+" -> "+fpixelrect);
            }
        } else {
            fpixelrect = pixelrect;
            if( DEBUG_POINTER_ICON ) {
                System.err.println("createPointerIconRES.0: No conversion "+fpixelrect);
            }
        }
        final PointerIconImpl[] res = { null };
        runOnEDTIfAvail(true, new Runnable() {
            public void run() {
                try {
                    if( !DisplayImpl.this.isNativeValidAsync() ) {
                        throw new IllegalStateException("Display.createPointerIcon: Display invalid "+DisplayImpl.this);
                    }
                    if( null != fpixelrect ) {
                        final long handle = createPointerIconImplChecked(fpixelrect.getPixelformat(),
                                                                         fpixelrect.getSize().getWidth(),
                                                                         fpixelrect.getSize().getHeight(),
                                                                         fpixelrect.getPixels(), hotX, hotY);
                        if( 0 != handle ) {
                            res[0] = new PointerIconImpl(DisplayImpl.this, fpixelrect, new Point(hotX, hotY), handle);
                        }
                    }
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } } );
        if( null != res[0] ) {
            synchronized(pointerIconList) {
                pointerIconList.add(res[0]);
            }
        }
        return res[0];
    }

    /**
     * Executed from EDT!
     *
     * @param pixelformat the <code>pixels</code>'s format
     * @param width the <code>pixels</code>'s width
     * @param height the <code>pixels</code>'s height
     * @param pixels the <code>pixels</code>
     * @param hotX the PointerIcon's hot-spot x-coord
     * @param hotY the PointerIcon's hot-spot x-coord
     * @return if successful a valid handle (not null), otherwise null.
     */
    protected final long createPointerIconImplChecked(final PixelFormat pixelformat, final int width, final int height, final ByteBuffer pixels, final int hotX, final int hotY) {
        if( getNativePointerIconPixelFormat() != pixelformat ) {
            throw new IllegalArgumentException("Pixelformat no "+getNativePointerIconPixelFormat()+", but "+pixelformat);
        }
        if( getNativePointerIconForceDirectNIO() && !Buffers.isDirect(pixels) ) {
            throw new IllegalArgumentException("pixel buffer is not direct "+pixels);
        }
        return createPointerIconImpl(pixelformat, width, height, pixels, hotX, hotY);
    }

    /**
     * Executed from EDT!
     *
     * @param pixelformat the <code>pixels</code>'s format
     * @param width the <code>pixels</code>'s width
     * @param height the <code>pixels</code>'s height
     * @param pixels the <code>pixels</code>
     * @param hotX the PointerIcon's hot-spot x-coord
     * @param hotY the PointerIcon's hot-spot x-coord
     * @return if successful a valid handle (not null), otherwise null.
     */
    protected long createPointerIconImpl(final PixelFormat pixelformat, final int width, final int height, final ByteBuffer pixels, final int hotX, final int hotY) {
        return 0;
    }

    /** Executed from EDT! */
    protected void destroyPointerIconImpl(final long displayHandle, final long piHandle) { }

    /** Ensure static init has been run. */
    /* pp */static void initSingleton() { }

    private static Class<?> getDisplayClass(final String type)
        throws ClassNotFoundException
    {
        final Class<?> displayClass = NewtFactory.getCustomClass(type, "DisplayDriver");
        if(null==displayClass) {
            throw new ClassNotFoundException("Failed to find NEWT Display Class <"+type+".DisplayDriver>");
        }
        return displayClass;
    }

    /** Make sure to reuse a Display with the same name */
    public static Display create(final String type, String name, final long handle, final boolean reuse) {
        try {
            final Class<?> displayClass = getDisplayClass(type);
            final DisplayImpl display = (DisplayImpl) displayClass.newInstance();
            name = display.validateDisplayName(name, handle);
            synchronized(displayList) {
                if(reuse) {
                    final Display display0 = Display.getLastDisplayOf(type, name, -1, true /* shared only */);
                    if(null != display0) {
                        if(DEBUG) {
                            System.err.println("Display.create() REUSE: "+display0+" "+getThreadName());
                        }
                        return display0;
                    }
                }
                display.exclusive = !reuse;
                display.name = name;
                display.type=type;
                display.refCount=0;
                display.id = serialno++;
                display.fqname = getFQName(display.type, display.name, display.id);
                display.hashCode = display.fqname.hashCode();
                display.setEDTUtil( display.edtUtil ); // device's default if EDT is used, or null
                Display.addDisplay2List(display);
            }

            if(DEBUG) {
                System.err.println("Display.create() NEW: "+display+" "+getThreadName());
            }
            return display;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DisplayImpl other = (DisplayImpl) obj;
        if (this.id != other.id) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
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
        if( null == aDevice ) {
            if(DEBUG) {
                System.err.println("Display.createNative() START ("+getThreadName()+", "+this+")");
            }
            final DisplayImpl f_dpy = this;
            try {
                runOnEDTIfAvail(true, new Runnable() {
                    @Override
                    public void run() {
                        f_dpy.createNativeImpl();
                    }});
            } catch (final Throwable t) {
                throw new NativeWindowException(t);
            }
            if( null == aDevice ) {
                throw new NativeWindowException("Display.createNative() failed to instanciate an AbstractGraphicsDevice");
            }
            synchronized(displayList) {
                displaysActive++;
                if(DEBUG) {
                    System.err.println("Display.createNative() END ("+getThreadName()+", "+this+", active "+displaysActive+")");
                }
            }
        }
    }

    protected EDTUtil createEDTUtil() {
        final EDTUtil def;
        if(NewtFactory.useEDT()) {
            def = new DefaultEDTUtil(Thread.currentThread().getThreadGroup(), "Display-"+getFQName(), dispatchMessagesRunnable);
            if(DEBUG) {
                System.err.println("Display.createEDTUtil("+getFQName()+"): "+def.getClass().getName());
            }
        } else {
            def = null;
        }
        return def;
    }

    @Override
    public synchronized EDTUtil setEDTUtil(final EDTUtil usrEDTUtil) {
        final EDTUtil oldEDTUtil = edtUtil;
        if( null != usrEDTUtil && usrEDTUtil == oldEDTUtil ) {
            if( DEBUG ) {
                System.err.println("Display.setEDTUtil: "+usrEDTUtil+" - keep!");
            }
            return oldEDTUtil;
        }
        if(DEBUG) {
            final String msg = ( null == usrEDTUtil ) ? "default" : "custom";
            System.err.println("Display.setEDTUtil("+msg+"): "+oldEDTUtil+" -> "+usrEDTUtil);
        }
        stopEDT( oldEDTUtil, null );
        edtUtil = ( null == usrEDTUtil ) ? createEDTUtil() : usrEDTUtil;
        return oldEDTUtil;
    }

    @Override
    public final EDTUtil getEDTUtil() {
        return edtUtil;
    }

    private static void stopEDT(final EDTUtil edtUtil, final Runnable task) {
        if( null != edtUtil ) {
            if( edtUtil.isRunning() ) {
                final boolean res = edtUtil.invokeStop(true, task);
                if( DEBUG ) {
                    if ( !res ) {
                        System.err.println("Warning: invokeStop() failed");
                        ExceptionUtils.dumpStack(System.err);
                    }
                }
            }
            edtUtil.waitUntilStopped();
            // ready for restart ..
        } else if( null != task ) {
            task.run();
        }
    }

    public void runOnEDTIfAvail(final boolean wait, final Runnable task) {
        final EDTUtil _edtUtil = edtUtil;
        if( !_edtUtil.isRunning() ) { // start EDT if not running yet
            synchronized( this ) {
                if( !_edtUtil.isRunning() ) { // // volatile dbl-checked-locking OK
                    if( DEBUG ) {
                        System.err.println("Info: EDT start "+Thread.currentThread().getName()+", "+this);
                        ExceptionUtils.dumpStack(System.err);
                    }
                    _edtUtil.start();
                }
            }
        }
        if( !_edtUtil.isCurrentThreadEDT() ) {
            if( _edtUtil.invoke(wait, task) ) {
                return; // done
            }
            if( DEBUG ) {
                System.err.println("Warning: invoke(wait "+wait+", ..) on EDT failed .. invoke on current thread "+Thread.currentThread().getName());
                ExceptionUtils.dumpStack(System.err);
            }
        }
        task.run();
    }

    @Override
    public boolean validateEDTStopped() {
        if( 0==refCount && null == aDevice ) {
            final EDTUtil _edtUtil = edtUtil;
            if( null != _edtUtil && _edtUtil.isRunning() ) {
                synchronized( this ) {
                    if( null != edtUtil && edtUtil.isRunning() ) { // // volatile dbl-checked-locking OK
                        stopEDT( edtUtil, null );
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public synchronized final void destroy() {
        if(DEBUG) {
            dumpDisplayList("Display.destroy("+getFQName()+") BEGIN");
        }
        synchronized(displayList) {
            if(0 < displaysActive) {
                displaysActive--;
            }
            if(DEBUG) {
                System.err.println("Display.destroy(): "+this+", active "+displaysActive+" "+getThreadName());
            }
        }
        final DisplayImpl f_dpy = this;
        final AbstractGraphicsDevice f_aDevice = aDevice;
        aDevice = null;
        refCount=0;
        stopEDT( edtUtil, new Runnable() { // blocks!
            @Override
            public void run() {
                if ( null != f_aDevice ) {
                    f_dpy.destroyAllPointerIconFromList(f_aDevice.getHandle());
                    f_dpy.closeNativeImpl(f_aDevice);
                }
            }
        } );
        if(DEBUG) {
            dumpDisplayList("Display.destroy("+getFQName()+") END");
        }
    }

    /** May be utilized at a shutdown hook, impl. does not block. */
    /* pp */ static final void shutdownAll() {
        final int dCount = displayList.size();
        if(DEBUG) {
            dumpDisplayList("Display.shutdownAll "+dCount+" instances, on thread "+getThreadName());
        }
        for(int i=0; i<dCount && displayList.size()>0; i++) { // be safe ..
            final DisplayImpl d = (DisplayImpl) displayList.remove(0).get();
            if(DEBUG) {
                System.err.println("Display.shutdownAll["+(i+1)+"/"+dCount+"]: "+d+", GCed "+(null==d));
            }
            if( null != d ) { // GC'ed ?
                if(0 < displaysActive) {
                    displaysActive--;
                }
                final EDTUtil edtUtil = d.getEDTUtil();
                final AbstractGraphicsDevice f_aDevice = d.aDevice;
                d.aDevice = null;
                d.refCount=0;
                final Runnable closeNativeTask = new Runnable() {
                    @Override
                    public void run() {
                        if ( null != d.getGraphicsDevice() ) {
                            d.destroyAllPointerIconFromList(f_aDevice.getHandle());
                            d.closeNativeImpl(f_aDevice);
                        }
                    }
                };
                if(null != edtUtil) {
                    final long coopSleep = edtUtil.getPollPeriod() * 2;
                    if( edtUtil.isRunning() ) {
                        edtUtil.invokeStop(false, closeNativeTask); // don't block
                    }
                    try {
                        Thread.sleep( coopSleep < 50 ? coopSleep : 50 );
                    } catch (final InterruptedException e) { }
                } else {
                    closeNativeTask.run();
                }
            }
        }
    }

    @Override
    public synchronized final int addReference() {
        if(DEBUG) {
            System.err.println("Display.addReference() ("+Display.getThreadName()+"): "+refCount+" -> "+(refCount+1));
        }
        if ( 0 == refCount ) {
            createNative();
        }
        if(null == aDevice) {
            throw new NativeWindowException ("Display.addReference() (refCount "+refCount+") null AbstractGraphicsDevice");
        }
        return refCount++;
    }


    @Override
    public synchronized final int removeReference() {
        if(DEBUG) {
            System.err.println("Display.removeReference() ("+Display.getThreadName()+"): "+refCount+" -> "+(refCount-1));
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
    protected abstract void closeNativeImpl(AbstractGraphicsDevice aDevice);

    @Override
    public final int getId() {
        return id;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getFQName() {
        return fqname;
    }

    @Override
    public final boolean isExclusive() {
        return exclusive;
    }

    public static final String nilString = "nil" ;

    public String validateDisplayName(String name, final long handle) {
        if(null==name && 0!=handle) {
            name="wrapping-"+toHexString(handle);
        }
        return ( null == name ) ? nilString : name ;
    }

    private static String getFQName(String type, String name, final int id) {
        if(null==type) type=nilString;
        if(null==name) name=nilString;
        final StringBuilder sb = new StringBuilder();
        sb.append(type);
        sb.append("_");
        sb.append(name);
        sb.append("-");
        sb.append(id);
        return sb.toString();
    }

    @Override
    public final long getHandle() {
        if(null!=aDevice) {
            return aDevice.getHandle();
        }
        return 0;
    }

    @Override
    public final AbstractGraphicsDevice getGraphicsDevice() {
        return aDevice;
    }

    @Override
    public synchronized final boolean isNativeValid() {
        return null != aDevice;
    }
    protected final boolean isNativeValidAsync() {
        return null != aDevice;
    }

    @Override
    public boolean isEDTRunning() {
        final EDTUtil _edtUtil = edtUtil;
        if( null != _edtUtil ) {
            return _edtUtil.isRunning();
        }
        return false;
    }

    @Override
    public String toString() {
        final EDTUtil _edtUtil = edtUtil;
        final boolean _edtUtilRunning = ( null != _edtUtil ) ? _edtUtil.isRunning() : false;
        return "NEWT-Display["+getFQName()+", excl "+exclusive+", refCount "+refCount+", hasEDT "+(null!=_edtUtil)+", edtRunning "+_edtUtilRunning+", "+aDevice+"]";
    }

    /** Dispatch native Toolkit messageges */
    protected abstract void dispatchMessagesNative();

    private final Object eventsLock = new Object();
    private ArrayList<NEWTEventTask> events = new ArrayList<NEWTEventTask>();
    private volatile boolean haveEvents = false;

    final protected Runnable dispatchMessagesRunnable = new Runnable() {
        @Override
        public void run() {
            DisplayImpl.this.dispatchMessages();
        } };

    final void dispatchMessage(final NEWTEvent event) {
        try {
            final Object source = event.getSource();
            if(source instanceof NEWTEventConsumer) {
                final NEWTEventConsumer consumer = (NEWTEventConsumer) source ;
                if(!consumer.consumeEvent(event)) {
                    // enqueue for later execution
                    enqueueEvent(false, event);
                }
            } else {
                throw new RuntimeException("Event source not NEWT: "+source.getClass().getName()+", "+source);
            }
        } catch (final Throwable t) {
            final RuntimeException re;
            if(t instanceof RuntimeException) {
                re = (RuntimeException) t;
            } else {
                re = new RuntimeException(t);
            }
            throw re;
        }
    }

    final void dispatchMessage(final NEWTEventTask eventTask) {
        final NEWTEvent event = eventTask.get();
        try {
            if(null == event) {
                // Ooops ?
                System.err.println("Warning: event of eventTask is NULL");
                ExceptionUtils.dumpStack(System.err);
                return;
            }
            dispatchMessage(event);
        } catch (final RuntimeException re) {
            if( eventTask.isCallerWaiting() ) {
                // propagate exception to caller
                eventTask.setException(re);
            } else {
                throw re;
            }
        } finally {
            eventTask.notifyCaller();
        }
    }

    @Override
    public void dispatchMessages() {
        // System.err.println("Display.dispatchMessages() 0 "+this+" "+getThreadName());
        if(0==refCount || // no screens
           null==getGraphicsDevice() // no native device
          )
        {
            return;
        }

        ArrayList<NEWTEventTask> _events = null;

        if(haveEvents) { // volatile: ok
            synchronized(eventsLock) {
                if(haveEvents) {
                    // swap events list to free ASAP
                    _events = events;
                    events = new ArrayList<NEWTEventTask>();
                    haveEvents = false;
                }
                eventsLock.notifyAll();
            }
            if( null != _events ) {
                for (int i=0; i < _events.size(); i++) {
                    final NEWTEventTask e = _events.get(i);
                    if( !e.isDispatched() ) {
                        dispatchMessage(e);
                    }
                }
            }
        }

        // System.err.println("Display.dispatchMessages() NATIVE "+this+" "+getThreadName());
        dispatchMessagesNative();
    }

    public void enqueueEvent(final boolean wait, final NEWTEvent e) {
        final EDTUtil _edtUtil = edtUtil;
        if( !_edtUtil.isRunning() ) {
            // oops .. we are already dead
            if(DEBUG) {
                System.err.println("Warning: EDT already stopped: wait:="+wait+", "+e);
                ExceptionUtils.dumpStack(System.err);
            }
            return;
        }

        // can't wait if we are on EDT or NEDT -> consume right away
        if(wait && _edtUtil.isCurrentThreadEDTorNEDT() ) {
            dispatchMessage(e);
            return;
        }

        final Object lock = new Object();
        final NEWTEventTask eTask = new NEWTEventTask(e, wait?lock:null);
        synchronized(lock) {
            synchronized(eventsLock) {
                events.add(eTask);
                haveEvents = true;
                eventsLock.notifyAll();
            }
            while( wait && !eTask.isDispatched() ) {
                try {
                    lock.wait();
                } catch (final InterruptedException ie) {
                    eTask.setDispatched(); // Cancels NEWTEvent ..
                    throw new InterruptedRuntimeException(ie);
                }
                if( null != eTask.getException() ) {
                    throw eTask.getException();
                }
            }
        }
    }

    public interface DisplayRunnable<T> {
        T run(long dpy);
    }
    public static final <T> T runWithLockedDevice(final AbstractGraphicsDevice device, final DisplayRunnable<T> action) {
        T res;
        device.lock();
        try {
            res = action.run(device.getHandle());
        } finally {
            device.unlock();
        }
        return res;
    }
    public final <T> T runWithLockedDisplayDevice(final DisplayRunnable<T> action) {
        final AbstractGraphicsDevice device = getGraphicsDevice();
        if(null == device) {
            throw new RuntimeException("null device - not initialized: "+this);
        }
        return runWithLockedDevice(device, action);
    }

    protected volatile EDTUtil edtUtil = null;
    protected int id;
    protected String name;
    protected String type;
    protected String fqname;
    protected int hashCode;
    protected int refCount; // number of Display references by Screen
    protected boolean exclusive; // do not share this display, uses NullLock!
    protected AbstractGraphicsDevice aDevice;
}

