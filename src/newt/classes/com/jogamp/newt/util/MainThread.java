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

import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.NativeWindowFactory;

import com.jogamp.common.util.ReflectionUtil;
import com.jogamp.common.util.RunnableTask;
import com.jogamp.newt.Display;
import jogamp.newt.Debug;
import jogamp.newt.NEWTJNILibLoader;
import jogamp.newt.driver.awt.AWTEDTUtil;

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
    private static final AccessControlContext localACC = AccessController.getContext();
    
    /** if true, use the main thread EDT, otherwise AWT's EDT */
    public static final boolean  HINT_USE_MAIN_THREAD = !NativeWindowFactory.isAWTAvailable() || 
                                                         Debug.getBooleanProperty("newt.MainThread.force", true, localACC);
    public static boolean useMainThread = false;
    
    protected static final boolean DEBUG = Debug.debug("MainThread");

    private static final MainThread singletonMainThread = new MainThread(); // one singleton MainThread

    private static boolean isExit=false;
    private static volatile boolean isRunning=false;
    private static final Object edtLock=new Object();
    private static boolean shouldStop;
    private static ArrayList<RunnableTask> tasks;
    private static Thread mainThread;

    private static Timer pumpMessagesTimer=null;
    private static TimerTask pumpMessagesTimerTask=null;
    private static final Map<Display, Runnable> pumpMessageDisplayMap = new HashMap<Display, Runnable>();

    static class MainAction extends Thread {
        private String mainClassName;
        private String[] mainClassArgs;

        private Method mainClassMain;

        public MainAction(String mainClassName, String[] mainClassArgs) {
            this.mainClassName=mainClassName;
            this.mainClassArgs=mainClassArgs;
        }

        @Override
        public void run() {
            if ( useMainThread ) {
                // we have to start first to provide the service ..
                singletonMainThread.waitUntilRunning();
            }

            // start user app ..
            try {
                Class<?> mainClass = ReflectionUtil.getClass(mainClassName, true, getClass().getClassLoader());
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
        useMainThread = HINT_USE_MAIN_THREAD;

        if(DEBUG) {
            System.err.println("MainThread.main(): "+Thread.currentThread().getName()+
                ", useMainThread "+ useMainThread +
                ", HINT_USE_MAIN_THREAD "+ HINT_USE_MAIN_THREAD +
                ", isAWTAvailable " + NativeWindowFactory.isAWTAvailable());
        }

        if(!useMainThread && !NativeWindowFactory.isAWTAvailable()) {
            throw new RuntimeException("!USE_MAIN_THREAD and no AWT available");
        }
        
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
            tasks = new ArrayList<RunnableTask>();
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

    public static MainThread getSingleton() {
        return singletonMainThread;
    }

    public static Runnable removePumpMessage(Display dpy) {
        synchronized(pumpMessageDisplayMap) {
            return pumpMessageDisplayMap.remove(dpy);
        }
    }

    public static void addPumpMessage(Display dpy, Runnable pumpMessage) {
        if(DEBUG) {
            System.err.println("MainThread.addPumpMessage(): "+Thread.currentThread().getName()+
                               " - dpy "+dpy+", USE_MAIN_THREAD " + useMainThread +
                               " - hasAWT " + NativeWindowFactory.isAWTAvailable() );
        }
        if(!useMainThread && !NativeWindowFactory.isAWTAvailable()) {
            throw new RuntimeException("!USE_MAIN_THREAD and no AWT available");
        }
        
        synchronized (pumpMessageDisplayMap) {
            if(!useMainThread && null == pumpMessagesTimer) {
                // AWT pump messages .. MAIN_THREAD uses main thread
                pumpMessagesTimer = new Timer();
                pumpMessagesTimerTask = new TimerTask() {
                    public void run() {
                        synchronized(pumpMessageDisplayMap) {
                            for(Iterator<Runnable> i = pumpMessageDisplayMap.values().iterator(); i.hasNext(); ) {
                                i.next().run();
                            }
                        }
                    }
                };
                pumpMessagesTimer.scheduleAtFixedRate(pumpMessagesTimerTask, 0, defaultEDTPollGranularity);
            }
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
            return isRunning;
        }
        return true; // AWT is always running
    }

    final public void invokeStop(Runnable r) {
        invokeImpl(true, r, true);
    }

    final public void invoke(boolean wait, Runnable r) {
        invokeImpl(wait, r, false);
    }

    private void invokeImpl(boolean wait, Runnable task, boolean stop) {
        if(task == null) {
            return;
        }

        if(NativeWindowFactory.isAWTAvailable()) {
            AWTEDTUtil.getSingleton().invokeImpl(wait, task, stop);
            return;
        }

        // if this main thread is not being used or
        // if this is already the main thread .. just execute.
        // FIXME: start if not started .. sync logic with DefaultEDTUtil!!!
        if( !isRunning() || mainThread == Thread.currentThread() ) {
            task.run();
            return;
        }

        Throwable throwable = null;
        RunnableTask rTask = null;
        Object rTaskLock = new Object();
        synchronized(rTaskLock) {
            synchronized(edtLock) { 
                if( shouldStop ) {
                    // drop task ..
                    if(DEBUG) {
                        System.err.println("Warning: EDT about (1) to stop, won't enqueue new task: "+this);
                        Thread.dumpStack();
                    }
                    return; 
                }
                // System.err.println(Thread.currentThread()+" XXX stop: "+stop+", tasks: "+tasks.size()+", task: "+task);
                // Thread.dumpStack();
                if(stop) {
                    shouldStop = true;
                    if(DEBUG) System.err.println("MainThread.stop(): "+Thread.currentThread().getName()+" start");
                }
                if( isCurrentThreadEDT() ) {
                    task.run();
                    wait = false; // running in same thread (EDT) -> no wait
                    if(stop && tasks.size()>0) {
                        System.err.println("Warning: EDT about (2) to stop, having remaining tasks: "+tasks.size()+" - "+this);
                        if(DEBUG) {
                            Thread.dumpStack();
                        }
                    }
                } else {
                    synchronized(tasks) {
                        start(); // start if not started yet and !shouldStop
                        wait = wait && isRunning();
                        rTask = new RunnableTask(task,
                                                 wait ? rTaskLock : null,
                                                 true /* always catch and report Exceptions, don't disturb EDT */);
                        if(stop) {
                            rTask.setAttachment(new Boolean(true)); // mark final task
                        }
                        // append task ..
                        tasks.add(rTask);
                        tasks.notifyAll();
                    }
                }
            }
            if( wait ) {
                try {
                    rTaskLock.wait(); // free lock, allow execution of rTask
                } catch (InterruptedException ie) {
                    throwable = ie;
                }
                if(null==throwable) {
                    throwable = rTask.getThrowable();
                }
                if(null!=throwable) {
                    if(throwable instanceof NativeWindowException) {
                        throw (NativeWindowException)throwable;
                    }
                    throw new RuntimeException(throwable);
                }
            }
        }
        if(DEBUG && stop) {
            System.err.println(Thread.currentThread()+": EDT signal STOP X edt: "+this);
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
        synchronized(edtLock) {
            if(isExit) return;

            while(!isRunning) {
                try {
                    edtLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void run() {
        if(DEBUG) System.err.println("MainThread.run(): "+Thread.currentThread().getName());
        synchronized(edtLock) {
            isRunning = true;
            edtLock.notifyAll();
        }
        
        RuntimeException error = null;
        try {
            do {
                // event dispatch
                if(!shouldStop) {
                    synchronized(pumpMessageDisplayMap) {
                        for(Iterator<Runnable> i = pumpMessageDisplayMap.values().iterator(); i.hasNext(); ) {
                            i.next().run();
                        }
                    }
                }
                // wait for something todo ..
                Runnable task = null;
                synchronized(tasks) {
                    // wait for tasks
                    if(!shouldStop && tasks.size()==0) {
                        try {
                            tasks.wait(defaultEDTPollGranularity);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // execute one task, if available
                    if(tasks.size()>0) {
                        task = tasks.remove(0);
                        tasks.notifyAll();
                    }
                }
                if(null!=task) {
                    // Exceptions are always catched, see Runnable creation above
                    task.run();
                }
            } while(!shouldStop) ;
        } catch (Throwable t) {
            // handle errors ..
            shouldStop = true;
            if(t instanceof RuntimeException) {
                error = (RuntimeException) t;
            } else {
                error = new RuntimeException("Within EDT", t);
            }
        } finally {
            if(DEBUG) {
                RunnableTask rt = ( tasks.size() > 0 ) ? tasks.get(0) : null ;
                System.err.println(/* getName()+*/"EDT run() END, tasks: "+tasks.size()+", "+rt+", "+error); 
            }
            synchronized(edtLock) {
                if(null==error) {
                    synchronized(tasks) {
                        // drain remaining tasks (stop not on EDT), 
                        // while having tasks and no previous-task, or previous-task is non final
                        RunnableTask task = null;
                        while ( ( null == task || task.getAttachment() == null ) && tasks.size() > 0 ) {
                            task = tasks.remove(0);
                            task.run();
                            tasks.notifyAll();
                        }
                        if(DEBUG) {
                            if(null!=task && task.getAttachment()==null) {
                                System.err.println("Warning: EDT exit: Last task Not Final: "+tasks.size()+", "+task);
                            } else if(tasks.size()>0) {
                                System.err.println("Warning: EDT exit: Remaining tasks Post Final: "+tasks.size());
                            }
                            Thread.dumpStack();
                        }
                    }
                }
                isRunning = !shouldStop;
                if(!isRunning) {
                    edtLock.notifyAll();
                }
            }
            if(DEBUG) {
                System.err.println("EDT run() EXIT "+ error);
            }
            if(null!=error) {
                throw error;
            }
        } // finally
        
        if(DEBUG) System.err.println("MainThread.run(): "+Thread.currentThread().getName()+" fin");
        synchronized(edtLock) {
            isRunning = false;
            isExit = true;
            edtLock.notifyAll();
        }
    }
}


