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

package com.jogamp.newt.impl;

import com.jogamp.newt.*;
import javax.media.nativewindow.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.impl.event.*;
import com.jogamp.newt.util.EDTUtil;
import com.jogamp.newt.util.MainThread;
import java.util.*;

public abstract class DisplayImpl extends Display {
    public static final boolean DEBUG_TEST_EDT_MAINTHREAD = Debug.isPropertyDefined("newt.test.EDTMainThread", true); // JAU EDT Test ..

    private static int serialno = 1;

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

    /** Make sure to reuse a Display with the same name */
    public static DisplayImpl create(String type, String name, final long handle) {
        try {
            Class displayClass = getDisplayClass(type);
            DisplayImpl display = (DisplayImpl) displayClass.newInstance();
            name = display.validateDisplayName(name, handle);
            display.name = name;
            display.type=type;
            display.destroyWhenUnused=false;
            display.refCount=0;
            synchronized(displayList) {
                display.id = serialno++;
                display.fqname = getFQName(display.id, display.type, display.name);
                displayList.add(display);
            }
            display.createEDTUtil();
            if(DEBUG) {
                System.err.println("Display.create() NEW: "+display+" "+getThreadName());
            }
            return display;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected  synchronized final void createNative() {
        if(null==aDevice) {
            if(DEBUG) {
                System.err.println("Display.createNative() START ("+getThreadName()+", "+this+")");
            }
            final DisplayImpl f_dpy = this;
            runOnEDTIfAvail(true, new Runnable() {
                public void run() {
                    f_dpy.createNativeImpl();
                }});
            if(null==aDevice) {
                throw new RuntimeException("Display.createNative() failed to instanciate an AbstractGraphicsDevice");
            }
            if(DEBUG) {
                System.err.println("Display.createNative() END ("+getThreadName()+", "+this+")");
            }
            synchronized(displayList) {
                displaysActive++;
            }
        }
    }

    protected boolean shallRunOnEDT() { 
        return true; 
    }

    protected void createEDTUtil() {
        if(NewtFactory.useEDT()) {
            if ( ! DEBUG_TEST_EDT_MAINTHREAD ) {
                Thread current = Thread.currentThread();
                edtUtil = new DefaultEDTUtil(current.getThreadGroup(), "Display-"+getFQName(), dispatchMessagesRunnable);
            } else {
                // Begin JAU EDT Test ..
                MainThread.addPumpMessage(this, dispatchMessagesRunnable); 
                edtUtil = MainThread.getSingleton();
                // End JAU EDT Test ..
            }
            if(DEBUG) {
                System.err.println("Display.createNative("+getFQName()+") Create EDTUtil: "+edtUtil.getClass().getName());
            }
        }
    }

    public final EDTUtil getEDTUtil() {
        return edtUtil;
    }

    private void stopEDT(final Runnable task) {
        if( shallRunOnEDT() && null!=edtUtil ) {
            edtUtil.invokeStop(task);
        } else {
            task.run();
        }
    }

    public void runOnEDTIfAvail(boolean wait, final Runnable task) {
        if( shallRunOnEDT() && null!=edtUtil ) {
            edtUtil.invoke(wait, task);
        } else {
            task.run();
        }
    }

    public synchronized final void destroy() {
        if(DEBUG) {
            dumpDisplayList("Display.destroy("+getFQName()+") BEGIN");
        }
        synchronized(displayList) {
            displayList.remove(this);
            displaysActive--;
        }
        if(DEBUG) {
            System.err.println("Display.destroy(): "+this+" "+getThreadName());
        }
        final AbstractGraphicsDevice f_aDevice = aDevice;
        final DisplayImpl f_dpy = this;
        stopEDT( new Runnable() {
            public void run() {
                if ( null != f_aDevice ) {
                    f_dpy.closeNativeImpl();
                }
            }
        } );
        if(null!=edtUtil) {
            if ( DEBUG_TEST_EDT_MAINTHREAD ) {
                MainThread.removePumpMessage(this); // JAU EDT Test ..
            }
            edtUtil.waitUntilStopped();
            edtUtil.reset();
        }
        aDevice = null;
        refCount=0;
        if(DEBUG) {
            dumpDisplayList("Display.destroy("+getFQName()+") END");
        }
    }

    protected synchronized final int addReference() {
        if(DEBUG) {
            System.err.println("Display.addReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount+1));
        }
        if ( 0 == refCount ) {
            createNative();
        }
        if(null == aDevice) {
            throw new RuntimeException("Display.addReference() (refCount "+refCount+") null AbstractGraphicsDevice");
        }
        return ++refCount;
    }


    protected synchronized final int removeReference() {
        if(DEBUG) {
            System.err.println("Display.removeReference() ("+DisplayImpl.getThreadName()+"): "+refCount+" -> "+(refCount-1));
        }
        refCount--; // could become < 0, in case of forced destruction without actual creation/addReference
        if(0>=refCount && getDestroyWhenUnused()) {
            destroy();
        }
        return refCount;
    }

    public synchronized final int getReferenceCount() {
        return refCount;
    }

    public final boolean getDestroyWhenUnused() { return destroyWhenUnused; }
    public final void setDestroyWhenUnused(boolean v) { destroyWhenUnused=v; }

    protected abstract void createNativeImpl();
    protected abstract void closeNativeImpl();

    public final int getId() {
        return id;
    }

    public final String getType() {
        return type;
    }

    public final String getName() {
        return name;
    }

    public final String getFQName() {
        return fqname;
    }

    public static final String nilString = "nil" ;

    public String validateDisplayName(String name, long handle) {
        if(null==name && 0!=handle) {
            name="wrapping-"+toHexString(handle);
        }
        return ( null == name ) ? nilString : name ;
    }

    public static final String getFQName(int id, String type, String name) {
        if(null==type) type=nilString;
        if(null==name) name=nilString;
        StringBuffer sb = new StringBuffer();
        sb.append(type);
        sb.append("_");
        sb.append(name);
        sb.append("-");
        sb.append(id);
        return sb.toString();
    }

    public final long getHandle() {
        if(null!=aDevice) {
            return aDevice.getHandle();
        }
        return 0;
    }

    public final AbstractGraphicsDevice getGraphicsDevice() {
        return aDevice;
    }

    public final boolean isNativeValid() {
        return null != aDevice;
    }

    public boolean isEDTRunning() {
        if(null!=edtUtil) {
            return edtUtil.isRunning();
        }
        return false;
    }

    public String toString() {
        return "NEWT-Display["+getFQName()+", refCount "+refCount+", hasEDT "+(null!=edtUtil)+", edtRunning "+isEDTRunning()+", "+aDevice+"]";
    }

    protected abstract void dispatchMessagesNative();

    private Object eventsLock = new Object();
    private LinkedList/*<NEWTEvent>*/ events = new LinkedList();

    class DispatchMessagesRunnable implements Runnable {
        public void run() {
            DisplayImpl.this.dispatchMessages();
        }
    }
    DispatchMessagesRunnable dispatchMessagesRunnable = new DispatchMessagesRunnable();

    public void dispatchMessages() {
        // System.err.println("Display.dispatchMessages() 0 "+this+" "+getThreadName());
        if(0==refCount) return; // no screens 
        if(null==getGraphicsDevice()) return; // no native device

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
            // System.err.println("Display.dispatchMessages() NATIVE "+this+" "+getThreadName());
            dispatchMessagesNative();
        } finally {
            // unlock();
            // System.err.println("Display.dispatchMessages() X "+this+" "+getThreadName());
        }
    }

    public void enqueueEvent(boolean wait, NEWTEvent e) {
        if(!isEDTRunning()) {
            // oops .. we are already dead
            if(DEBUG) {
                Throwable t = new Throwable("Warning: EDT already stopped: wait:="+wait+", "+e);
                t.printStackTrace();
            }
            return;
        }
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

    protected EDTUtil edtUtil = null;
    protected int id;
    protected String name;
    protected String type;
    protected String fqname;
    protected int refCount; // number of Display references by Screen
    protected boolean destroyWhenUnused;
    protected AbstractGraphicsDevice aDevice;
}

