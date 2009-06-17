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

package com.sun.javafx.newt;

import javax.media.nativewindow.*;
import com.sun.javafx.newt.impl.Debug;
import java.util.*;

public abstract class Display implements Runnable {
    public static final boolean DEBUG = Debug.debug("Display");

    private static Class getDisplayClass(String type) 
        throws ClassNotFoundException 
    {
        Class displayClass = null;
        if (NativeWindowFactory.TYPE_EGL.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.opengl.kd.KDDisplay");
        } else if (NativeWindowFactory.TYPE_WINDOWS.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.windows.WindowsDisplay");
        } else if (NativeWindowFactory.TYPE_MACOSX.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.macosx.MacDisplay");
        } else if (NativeWindowFactory.TYPE_X11.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.x11.X11Display");
        } else if (NativeWindowFactory.TYPE_AWT.equals(type)) {
            displayClass = Class.forName("com.sun.javafx.newt.awt.AWTDisplay");
        } else {
            throw new RuntimeException("Unknown display type \"" + type + "\"");
        }
        return displayClass;
    }

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
            String name = display.getName();
            if(null==name) name="nil";
            oldDisplay = (Display) displayMap.put(name, display);
            displayMap.notifyAll();
        }
        return oldDisplay;
    }

    /** removes the mapping of the given name from the thread local display map
      * and notifies all threads synchronized to this display map. */
    protected static Display removeCurrentDisplay(String name) {
        if(null==name) name="nil";
        Map displayMap = getCurrentDisplayMap();
        Display oldDisplay = null;
        synchronized(displayMap) {
            oldDisplay = (Display) displayMap.remove(name);
            displayMap.notifyAll();
        }
        return oldDisplay;
    }

    /** Returns the thread local display mapped to the given name */
    public static Display getCurrentDisplay(String name) {
        if(null==name) name="nil";
        Map displayMap = getCurrentDisplayMap();
        Display display = (Display) displayMap.get(name);
        return display;
    }

    private static void dumpDisplayMap(String prefix) {
        Map displayMap = getCurrentDisplayMap();
        Set entrySet = displayMap.entrySet();
        Iterator i = entrySet.iterator();
        System.err.println(prefix+" DisplayMap["+entrySet.size()+"] "+Thread.currentThread().getName());
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
    protected static Display create(String type, String name) {
        try {
            Display display = getCurrentDisplay(name);
            if(null==display) {
                Class displayClass = getDisplayClass(type);
                display = (Display) displayClass.newInstance();
                display.name=name;
                display.refCount=1;
                display.createNative();
                if(null==display.aDevice) {
                    throw new RuntimeException("Display.createNative() failed to instanciate an AbstractGraphicsDevice");
                }
                setCurrentDisplay(display);
                if(DEBUG) {
                    System.err.println("Display.create("+name+") NEW: "+display+" "+Thread.currentThread().getName());
                }
            } else {
                synchronized(display) {
                    display.refCount++;
                    if(DEBUG) {
                        System.err.println("Display.create("+name+") REUSE: refCount "+display.refCount+", "+display+" "+Thread.currentThread().getName());
                    }
                }
            }
            return display;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void destroy() {
        refCount--;
        if(0==refCount) {
            removeCurrentDisplay(name);
            if(DEBUG) {
                System.err.println("Display.destroy("+name+") REMOVE: "+this+" "+Thread.currentThread().getName());
            }
            closeNative();
        } else {
            if(DEBUG) {
                System.err.println("Display.destroy("+name+") KEEP: refCount "+refCount+", "+this+" "+Thread.currentThread().getName());
            }
        }
    }

    protected static Display wrapHandle(String type, String name, AbstractGraphicsDevice aDevice) {
        try {
            Class displayClass = getDisplayClass(type);
            Display display = (Display) displayClass.newInstance();
            display.name=name;
            display.aDevice=aDevice;
            return display;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract void createNative();
    protected abstract void closeNative();

    public String getName() {
        return name;
    }

    public long getHandle() {
        return aDevice.getHandle();
    }

    public AbstractGraphicsDevice getGraphicsDevice() {
        return aDevice;
    }

    public synchronized void pumpMessages() {
        dispatchMessages();
    }

    /** calls {@link #pumpMessages} */
    public void run() {
        pumpMessages();
    }

    public static interface Action {
        public void run(Display display);
    }

    /** Calls {@link Display.Action#run(Display)} on all Display's 
        bound to the current thread. */
    public static void runCurrentThreadDisplaysAction(Display.Action action) {
        Iterator iter = getCurrentDisplays().iterator(); // Thread local .. no sync necessary
        while(iter.hasNext()) {
            action.run((Display) iter.next());
        }
    }

    public String toString() {
        return "NEWT-Display["+name+", refCount "+refCount+", "+aDevice+"]";
    }

    protected abstract void dispatchMessages();

    protected String name;
    protected int refCount;
    protected AbstractGraphicsDevice aDevice;
}

