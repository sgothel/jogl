/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package com.jogamp.newt.util;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.media.nativewindow.NativeWindowFactory;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.newt.Display;
import jogamp.newt.Debug;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.newt.awt.AWTEDTUtil;

/**
 * NEWT Utility class MainThread<P>
 *
 * This class provides a startup singleton <i>main thread</i>,
 * from which a new thread with the users main class is launched.<br>
 *
 * Such behavior is necessary for native windowing toolkits,
 * where the windowing management must happen on the so called
 * <i>main thread</i> e.g. for Mac OS X !<br>
 *
 * Utilizing this class as a launchpad, now you are able to
 * use a NEWT multithreaded application with window handling within the different threads,
 * even on these restricted platforms.<br>
 *
 * To support your NEWT Window platform, 
 * you have to pass your <i>main thread</i> actions to {@link #invoke invoke(..)},
 * have a look at the {@link com.jogamp.newt.macosx.MacWindow MacWindow} implementation.<br>
 * <i>TODO</i>: Some hardcoded dependencies exist in this implementation, 
 * where you have to patch this code or factor it out. <P>
 * 
 * If your platform is not Mac OS X, but you want to test your code without modifying
 * this class, you have to set the system property <code>newt.MainThread.force</code> to <code>true</code>.<P>
 *
 * The code is compatible with all other platform, which support multithreaded windowing handling.
 * Since those platforms won't trigger the <i>main thread</i> serialization, the main method 
 * will be simply executed, in case you haven't set <code>newt.MainThread.force</code> to <code>true</code>.<P>
 *
 * Test case on Mac OS X (or any other platform):
 <PRE>
    java -XstartOnFirstThread com.jogamp.newt.util.MainThread demos.es1.RedSquare -GL2 -GL2 -GL2 -GL2
 </PRE>
 * Which starts 4 threads, each with a window and OpenGL rendering.<br>
 */
public class MainThread implements EDTUtil {
    private static AccessControlContext localACC = AccessController.getContext();
    public static final boolean  MAIN_THREAD_CRITERIA = ( !NativeWindowFactory.isAWTAvailable() &&
                                                           NativeWindowFactory.TYPE_MACOSX.equals(NativeWindowFactory.getNativeWindowType(false)) 
                                                        ) || Debug.getBooleanProperty("newt.MainThread.force", true, localACC);

    protected static final boolean DEBUG = Debug.debug("MainThread");

    private static MainThread singletonMainThread = new MainThread(); // one singleton MainThread

    private static boolean isExit=false;
    private static volatile boolean isRunning=false;
    private static Object taskWorkerLock=new Object();
    private static boolean shouldStop;
    private static ArrayList tasks;
    private static Thread mainThread;

    private static Timer pumpMessagesTimer=null;
    private static TimerTask pumpMessagesTimerTask=null;
    private static Map/*<Display, Runnable>*/ pumpMessageDisplayMap = new HashMap();

    private static boolean useMainThread = false;

    static class MainAction extends Thread {
        private String mainClassName;
        private String[] mainClassArgs;

        private Method mainClassMain;

        public MainAction(String mainClassName, String[] mainClassArgs) {
            this.mainClassName=mainClassName;
            this.mainClassArgs=mainClassArgs;
        }

        public void run() {
            if ( useMainThread ) {
                // we have to start first to provide the service ..
                singletonMainThread.waitUntilRunning();
            }

            // start user app ..
            try {
                Class mainClass = ReflectionUtil.getClass(mainClassName, true, getClass().getClassLoader());
                if(null==mainClass) {
                    throw new RuntimeException(new ClassNotFoundException("MainThread couldn't find main class "+mainClassName));
                }
                try {
                    mainClassMain = mainClass.getDeclaredMethod("main", new Class[] { String[].class });
                    mainClassMain.setAccessible(true);
                } catch (Throwable t) {
                    throw new RuntimeException(t);
                }
                if(DEBUG) System.err.println("MainAction.run(): "+Thread.currentThread().getName()+" invoke "+mainClassName);
                mainClassMain.invoke(null, new Object[] { mainClassArgs } );
            } catch (InvocationTargetException ite) {
                ite.getTargetException().printStackTrace();
            } catch (Throwable t) {
                t.printStackTrace();
            }

            if(DEBUG) System.err.println("MainAction.run(): "+Thread.currentThread().getName()+" user app fin");

            if ( useMainThread ) {
                singletonMainThread.invokeStop(new Runnable() {
                    public void run() {
                        // nop
                    }});
                if(DEBUG) System.err.println("MainAction.run(): "+Thread.currentThread().getName()+" MainThread fin - stop");
                System.exit(0);
            }
        }
    }
    private static MainAction mainAction;

    /** Your new java application main entry, which pipelines your application */
    public static void main(String[] args) {
        useMainThread = MAIN_THREAD_CRITERIA;

        if(DEBUG) System.err.println("MainThread.main(): "+Thread.currentThread().getName()+" useMainThread "+ useMainThread );

        if(args.length==0) {
            return;
        }

        String mainClassName=args[0];
        String[] mainClassArgs=new String[args.length-1];
        if(args.length>1) {
            System.arraycopy(args, 1, mainClassArgs, 0, args.length-1);
        }

        NEWTJNILibLoader.loadNEWT();
        
        mainAction = new MainAction(mainClassName, mainClassArgs);

        if(NativeWindowFactory.TYPE_MACOSX.equals(NativeWindowFactory.getNativeWindowType(false))) {
            ReflectionUtil.callStaticMethod("jogamp.newt.macosx.MacDisplay", "initSingleton", 
                null, null, MainThread.class.getClassLoader());
        }

        if ( useMainThread ) {
            shouldStop = false;
            tasks = new ArrayList();
            mainThread = Thread.currentThread();

            // dispatch user's main thread ..
            mainAction.start();

            // do our main thread task scheduling
            singletonMainThread.run();
        } else {
            // run user's main in this thread 
            mainAction.run();
        }
    }

    public static final MainThread getSingleton() {
        return singletonMainThread;
    }

    public static Runnable removePumpMessage(Display dpy) {
        synchronized(pumpMessageDisplayMap) {
            return (Runnable) pumpMessageDisplayMap.remove(dpy);
        }
    }

    public static void addPumpMessage(Display dpy, Runnable pumpMessage) {
        if ( useMainThread ) {
            return; // error ?
        }
        if(null == pumpMessagesTimer) {
            synchronized (MainThread.class) {
                if(null == pumpMessagesTimer) {
                    pumpMessagesTimer = new Timer();
                    pumpMessagesTimerTask = new TimerTask() {
                        public void run() {
                            synchronized(pumpMessageDisplayMap) {
                                for(Iterator i = pumpMessageDisplayMap.values().iterator(); i.hasNext(); ) {
                                    ((Runnable) i.next()).run();
                                }
                            }
                        }
                    };
                    pumpMessagesTimer.scheduleAtFixedRate(pumpMessagesTimerTask, 0, defaultEDTPollGranularity);
                }
            }
        }
        synchronized(pumpMessageDisplayMap) {
            pumpMessageDisplayMap.put(dpy, pumpMessage);
        }
    }

    final public void reset() {
        if(NativeWindowFactory.isAWTAvailable()) {
            AWTEDTUtil.getSingleton().reset();
        }
        // nop
    }

    final public void start() {
        if(NativeWindowFactory.isAWTAvailable()) {
            AWTEDTUtil.getSingleton().start();
        }
        // nop
    }

    final public boolean isCurrentThreadEDT() {
        if(NativeWindowFactory.isAWTAvailable()) {
            return AWTEDTUtil.getSingleton().isCurrentThreadEDT();
        }
        return isRunning() && mainThread == Thread.currentThread() ;
    }

    final public boolean isRunning() {
        if( useMainThread ) {
            synchronized(taskWorkerLock) { 
                return isRunning;
            }
        }
        return true; // AWT is always running
    }

    private void invokeLater(Runnable task) {
        synchronized(taskWorkerLock) {
            if(isRunning() && mainThread != Thread.currentThread()) {
                tasks.add(task);
                taskWorkerLock.notifyAll();
            } else {
                // if !running or isEDTThread, do it right away
                task.run();
            }
        }
    }

    final public void invokeStop(Runnable r) {
        invokeImpl(true, r, true);
    }

    final public void invoke(boolean wait, Runnable r) {
        invokeImpl(wait, r, false);
    }

    private void invokeImpl(boolean wait, Runnable r, boolean stop) {
        if(r == null) {
            return;
        }

        if(NativeWindowFactory.isAWTAvailable()) {
            AWTEDTUtil.getSingleton().invokeImpl(wait, r, stop);
            return;
        }

        // if this main thread is not being used or
        // if this is already the main thread .. just execute.
        // FIXME: start if not started .. sync logic with DefaultEDTUtil!!!
        if( !isRunning() || mainThread == Thread.currentThread() ) {
            r.run();
            return;
        }

        boolean doWait = wait && isRunning() && mainThread != Thread.currentThread();
        Object lock = new Object();
        RunnableTask rTask = new RunnableTask(r, doWait?lock:null, true);
        Throwable throwable = null;
        synchronized(lock) {
            invokeLater(rTask);
            // FIXME ..
            synchronized(taskWorkerLock) { 
                if(isRunning) {
                    shouldStop = true;
                    if(DEBUG) System.err.println("MainThread.stop(): "+Thread.currentThread().getName()+" start");
                }
                taskWorkerLock.notifyAll();
            }
            if( doWait ) {
                try {
                    lock.wait();
                } catch (InterruptedException ie) {
                    throwable = ie;
                }
            }
        }
        if(null==throwable) {
            throwable = rTask.getThrowable();
        }
        if(null!=throwable) {
            throw new RuntimeException(throwable);
        }
    }

    final public void waitUntilIdle() {
        if(NativeWindowFactory.isAWTAvailable()) {
            AWTEDTUtil.getSingleton().waitUntilIdle();
        }
    }

    final public void waitUntilStopped() {
        if(NativeWindowFactory.isAWTAvailable()) {
            AWTEDTUtil.getSingleton().waitUntilStopped();
        }
    }

    private void waitUntilRunning() {
        synchronized(taskWorkerLock) {
            if(isExit) return;

            while(!isRunning) {
                try {
                    taskWorkerLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void run() {
        if(DEBUG) System.err.println("MainThread.run(): "+Thread.currentThread().getName());
        synchronized(taskWorkerLock) {
            isRunning = true;
            taskWorkerLock.notifyAll();
        }
        while(!shouldStop) {
            try {
                // wait for something todo ..
                synchronized(taskWorkerLock) {
                    while(!shouldStop && tasks.size()==0) {
                        try {
                            taskWorkerLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // take over the tasks ..
                    if(!shouldStop && tasks.size()>0) {
                        Runnable task = (Runnable) tasks.remove(0);
                        task.run(); // FIXME: could be run outside of lock
                    }
                    taskWorkerLock.notifyAll();
                }
            } catch (Throwable t) {
                // handle errors ..
                t.printStackTrace();
            } finally {
                // epilog - unlock locked stuff
            }
        }
        if(DEBUG) System.err.println("MainThread.run(): "+Thread.currentThread().getName()+" fin");
        synchronized(taskWorkerLock) {
            isRunning = false;
            isExit = true;
            taskWorkerLock.notifyAll();
        }
    }
}


