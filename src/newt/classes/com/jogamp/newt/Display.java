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

package com.jogamp.newt;

import javax.media.nativewindow.*;
import com.jogamp.nativewindow.impl.RecursiveToolkitLock;
import com.jogamp.newt.event.*;
import com.jogamp.newt.impl.event.*;
import com.jogamp.newt.impl.Debug;
import com.jogamp.newt.util.EDTUtil;
import com.jogamp.newt.util.MainThread;
import com.jogamp.newt.util.DefaultEDTUtil;
import java.util.*;

public abstract class Display {
    public static final boolean DEBUG = Debug.debug("Display");
    public static final boolean DEBUG_TEST_EDT_MAINTHREAD = Debug.isPropertyDefined("newt.test.EDTMainThread", true); // JAU EDT Test ..

    private static Class getDisplayClass(String type) 
        throws ClassNotFoundException 
    {
        Class displayClass = NewtFactory.getCustomClass(type, "Display");
        if(null==displayClass) {
            if (NativeWindowFactory.TYPE_EGL.equals(type)) {
                displayClass = Class.forName("com.jogamp.newt.impl.opengl.kd.KDDisplay");
            } else if (NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
                displayClass = Class.forName("com.jogamp.newt.impl.windows.WindowsDisplay");
            } else if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
                displayClass = Class.forName("com.jogamp.newt.impl.macosx.MacDisplay");
            } else if (NativeWindowFactory.TYPE_X11.equals(type)) {
                displayClass = Class.forName("com.jogamp.newt.impl.x11.X11Display");
            } else if (NativeWindowFactory.TYPE_AWT.equals(type)) {
                displayClass = Class.forName("com.jogamp.newt.impl.awt.AWTDisplay");
            } else {
                throw new RuntimeException("Unknown display type \"" + type + "\"");
            }
        }
        return displayClass;
    }

    // Unique Display for each thread
    private static ThreadLocal currentDisplayMap = new ThreadLocal();

    /** Returns the thread local display map */
    public static Map getCurrentDisplayMap() {
        Map displayMap = (Map) currentDisplayMap.get();
        if(null==displayMap) {
            displayMap = new HashMap();
            currentDisplayMap.set( displayMap );
        }
        return displayMap;
    }

    /** maps the given display to the thread local display map
      * and notifies all threads synchronized to this display map. */
    protected static Display setCurrentDisplay(Display display) {
        Map displayMap = getCurrentDisplayMap();
        Display oldDisplay = null;
        synchronized(displayMap) {
            oldDisplay = (Display) displayMap.put(display.getFQName(), display);
            displayMap.notifyAll();
        }
        return oldDisplay;
    }

    /** removes the mapping of the given name from the thread local display map
      * and notifies all threads synchronized to this display map. */
    protected static Display removeCurrentDisplay(String type, String name) {
        Map displayMap = getCurrentDisplayMap();
        Display oldDisplay = null;
        synchronized(displayMap) {
            oldDisplay = (Display) displayMap.remove(getFQName(type,name));
            displayMap.notifyAll();
        }
        return oldDisplay;
    }

    /** Returns the thread local display mapped to the given name */
    public static Display getCurrentDisplay(String type, String name) {
        Map displayMap = getCurrentDisplayMap();
        Display display = (Display) displayMap.get(getFQName(type,name));
        return display;
    }

    public static void dumpDisplayMap(String prefix) {
        Map displayMap = getCurrentDisplayMap();
        Set entrySet = displayMap.entrySet();
        Iterator i = entrySet.iterator();
        System.err.println(prefix+" DisplayMap[] entries: "+entrySet.size()+" - "+getThreadName());
        for(int j=0; i.hasNext(); j++) {
            Map.Entry entry = (Map.Entry) i.next();
            System.err.println("  ["+j+"] "+entry.getKey()+" -> "+entry.getValue());
        }
    }

    /** Returns the thread local display collection */
    public static Collection getCurrentDisplays() {
        return getCurrentDisplayMap().values();
    }

    /** Make sure to reuse a Display with the same name */
    protected static synchronized Display create(String type, String name, final long handle) {
        try {
            Class displayClass = getDisplayClass(type);
            Display tmpDisplay = (Display) displayClass.newInstance();
            name = tmpDisplay.validateDisplayName(name, handle);

            if(DEBUG) {
                dumpDisplayMap("Display.create("+getFQName(type, name)+") BEGIN");
            }
            Display display = getCurrentDisplay(type, name);
            if(null==display) {
                display = tmpDisplay;
                tmpDisplay = null;
                display.name = name;
                display.type=type;
                display.refCount=0;
                if(DEBUG) {
                    System.err.println("Display.create("+getFQName(type, name)+") NEW: refCount "+display.refCount+", "+display+" "+getThreadName());
                }
            } else {
                tmpDisplay = null;
                if(DEBUG) {
                    System.err.println("Display.create("+getFQName(type, name)+") REUSE: refCount "+display.refCount+", "+display+" "+getThreadName());
                }
            }
            synchronized(display) {
                display.refCount++;
                if(null==display.aDevice) {
                    final Display f_dpy = display;
                    display.runOnEDTIfAvail(true, new Runnable() {
                        public void run() {
                            f_dpy.createNative();
                        }});
                    if(null==display.aDevice) {
                        throw new RuntimeException("Display.createNative() failed to instanciate an AbstractGraphicsDevice");
                    }
                    setCurrentDisplay(display);
                    if(DEBUG) {
                        System.err.println("Display.create("+getFQName(type, name)+") CreateNative: "+display+" "+getThreadName());
                    }
                }
            }
            if(DEBUG) {
                dumpDisplayMap("Display.create("+getFQName(type, name)+") END");
            }
            return display;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean getShallRunOnEDT() { 
        return true; 
    }

    public EDTUtil getEDTUtil() {
        if( null == edtUtil ) {
            synchronized (this) {
                if( null == edtUtil ) {
                    if(NewtFactory.useEDT()) {
                        final Display f_dpy = this;
                        if ( ! DEBUG_TEST_EDT_MAINTHREAD ) {
                            Thread current = Thread.currentThread();
                            edtUtil = new DefaultEDTUtil(current.getThreadGroup(), 
                                                  "Display_"+getFQName(),
                                                  new Runnable() {
                                                      public void run() {
                                                          if(null!=f_dpy.getGraphicsDevice()) {
                                                              f_dpy.dispatchMessages();
                                                          } } } );
                        } else {
                            // Begin JAU EDT Test ..
                            MainThread.addPumpMessage(this, 
                                                  new Runnable() {
                                                      public void run() {
                                                          if(null!=f_dpy.getGraphicsDevice()) {
                                                              f_dpy.dispatchMessages();
                                                          } } } );
                            edtUtil = MainThread.getSingleton();
                            System.err.println("Display.getEDTUtil("+getFQName()+") Test EDT MainThread: "+edtUtil.getClass().getName());
                            // End JAU EDT Test ..
                        }
                        edtUtil.start();
                    }
                }
            }
        }
        return edtUtil;
    }

    protected void releaseEDTUtil() {
        if(null!=edtUtil) { 
            if ( DEBUG_TEST_EDT_MAINTHREAD ) {
                MainThread.removePumpMessage(this); // JAU EDT Test ..
            }
            edtUtil.waitUntilStopped();
            edtUtil=null;
        }
    }

    public void runOnEDTIfAvail(boolean wait, final Runnable task) {
        EDTUtil _edtUtil = getEDTUtil();
        if(getShallRunOnEDT() && null!=_edtUtil) {
            _edtUtil.invoke(wait, task);
        } else {
            task.run();
        }
    }

    public synchronized void destroy() {
        if(DEBUG) {
            dumpDisplayMap("Display.destroy("+getFQName()+") BEGIN");
        }
        refCount--;
        if(0==refCount) {
            removeCurrentDisplay(type, name);
            if(DEBUG) {
                System.err.println("Display.destroy("+getFQName()+") REMOVE: "+this+" "+getThreadName());
            }
            final Display f_dpy = this;
            final EDTUtil f_edtUtil = edtUtil;
            runOnEDTIfAvail(true, new Runnable() {
                public void run() {
                    f_dpy.closeNative();
                    if(null!=f_edtUtil) {
                        f_edtUtil.stop();
                    }
                }
            } );
            releaseEDTUtil();
            aDevice = null;
        } else {
            if(DEBUG) {
                System.err.println("Display.destroy("+getFQName()+") KEEP: refCount "+refCount+", "+this+" "+getThreadName());
            }
        }
        if(DEBUG) {
            dumpDisplayMap("Display.destroy("+getFQName()+") END");
        }
    }

    protected abstract void createNative();
    protected abstract void closeNative();

    public final String getType() {
        return type;
    }

    public final String getName() {
        return name;
    }

    public final String getFQName() {
        return getFQName(type, name);
    }

    static final String nilString = "nil" ;

    protected String validateDisplayName(String name, long handle) {
        if(null==name && 0!=handle) {
            name="wrapping-"+toHexString(handle);
        }
        return ( null == name ) ? nilString : name ;
    }

    public static final String getFQName(String type, String name) {
        if(null==type) type=nilString;
        if(null==name) name=nilString;
        return type+"_"+name;
    }

    public long getHandle() {
        if(null!=aDevice) {
            return aDevice.getHandle();
        }
        return 0;
    }

    public AbstractGraphicsDevice getGraphicsDevice() {
        return aDevice;
    }

    public synchronized void pumpMessages() {
        dispatchMessages();
    }

    public String toString() {
        return "NEWT-Display["+getFQName()+", refCount "+refCount+", hasEDT "+(null!=edtUtil)+", "+aDevice+"]";
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


    protected abstract void dispatchMessagesNative();

    private Object eventsLock = new Object();
    private LinkedList/*<NEWTEvent>*/ events = new LinkedList();

    public void dispatchMessages() {
        if(0==refCount) return; // in destruction ..

        LinkedList/*<NEWTEvent>*/ _events = null;

        if(!events.isEmpty()) {
            // swap events list to free ASAP
            synchronized(eventsLock) {
                if(!events.isEmpty()) {
                    _events = events;
                    events = new LinkedList();
                }
                eventsLock.notifyAll();
            }
            if( null != _events ) {
                for (Iterator iter = _events.iterator(); iter.hasNext(); ) {
                    NEWTEventTask eventTask = (NEWTEventTask) iter.next();
                    NEWTEvent event = eventTask.get();
                    Object source = event.getSource();
                    if(source instanceof NEWTEventConsumer) {
                        NEWTEventConsumer consumer = (NEWTEventConsumer) source ;
                        if(!consumer.consumeEvent(event)) {
                            enqueueEvent(false, event);
                        }
                    } else {
                        throw new RuntimeException("Event source not NEWT: "+source.getClass().getName()+", "+source);
                    }
                    eventTask.notifyIssuer();
                }
            }
        }

        // lock();
        try {
            dispatchMessagesNative();
        } finally {
            // unlock();
        }
    }

    public void enqueueEvent(boolean wait, NEWTEvent e) {
        Object lock = new Object();
        NEWTEventTask eTask = new NEWTEventTask(e, wait?lock:null);
        synchronized(lock) {
            synchronized(eventsLock) {
                events.addLast(eTask);
                eventsLock.notifyAll();
            }
            if( wait ) {
                try {
                    lock.wait();
                } catch (InterruptedException ie) {
                    throw new RuntimeException(ie);
                }
            }
        }
    }

    public void lock() { 
        aDevice.lock();
    }

    public void unlock() { 
        aDevice.unlock();
    }

    protected EDTUtil edtUtil = null;
    protected String name;
    protected String type;
    protected int refCount;
    protected AbstractGraphicsDevice aDevice;
}

