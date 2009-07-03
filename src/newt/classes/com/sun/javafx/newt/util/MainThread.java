/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.javafx.newt.util;

import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.security.*;

import javax.media.nativewindow.*;

import com.sun.javafx.newt.*;
import com.sun.javafx.newt.impl.*;
import com.sun.javafx.newt.macosx.MacDisplay;
import com.sun.nativewindow.impl.NWReflection;

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
 * have a look at the {@link com.sun.javafx.newt.macosx.MacWindow MacWindow} implementation.<br>
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
    java -XstartOnFirstThread com.sun.javafx.newt.util.MainThread demos.es1.RedSquare -GL2 -GL2 -GL2 -GL2
 </PRE>
 * Which starts 4 threads, each with a window and OpenGL rendering.<br>
 */
public class MainThread {
    private static AccessControlContext localACC = AccessController.getContext();
    public static final boolean USE_MAIN_THREAD = NativeWindowFactory.TYPE_MACOSX.equals(NativeWindowFactory.getNativeWindowType(false)) ||
                                                  Debug.getBooleanProperty("newt.MainThread.force", true, localACC);

    protected static final boolean DEBUG = Debug.debug("MainThread");

    private static boolean isExit=false;
    private static volatile boolean isRunning=false;
    private static Object taskWorkerLock=new Object();
    private static boolean shouldStop;
    private static ArrayList tasks;
    private static ArrayList tasksBlock;
    private static Thread mainThread;

    static class MainAction extends Thread {
        private String mainClassName;
        private String[] mainClassArgs;

        private Class mainClass;
        private Method mainClassMain;

        public MainAction(String mainClassName, String[] mainClassArgs) {
            this.mainClassName=mainClassName;
            this.mainClassArgs=mainClassArgs;
        }

        public void run() {
            if ( USE_MAIN_THREAD ) {
                // we have to start first to provide the service ..
                MainThread.waitUntilRunning();
            }

            // start user app ..
            try {
                Class mainClass = NWReflection.getClass(mainClassName, true);
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

            if ( USE_MAIN_THREAD ) {
                MainThread.exit();
                if(DEBUG) System.err.println("MainAction.run(): "+Thread.currentThread().getName()+" MainThread fin - exit");
                System.exit(0);
            }
        }
    }
    private static MainAction mainAction;

    /** Your new java application main entry, which pipelines your application */
    public static void main(String[] args) {
        if(DEBUG) System.err.println("MainThread.main(): "+Thread.currentThread().getName()+" USE_MAIN_THREAD "+ USE_MAIN_THREAD );

        if(args.length==0) {
            return;
        }

        String mainClassName=args[0];
        String[] mainClassArgs=new String[args.length-1];
        if(args.length>1) {
            System.arraycopy(args, 1, mainClassArgs, 0, args.length-1);
        }

        NativeLibLoader.loadNEWT();
        
        shouldStop = false;
        tasks = new ArrayList();
        tasksBlock = new ArrayList();
        mainThread = Thread.currentThread();

        mainAction = new MainAction(mainClassName, mainClassArgs);

        if(NativeWindowFactory.TYPE_MACOSX.equals(NativeWindowFactory.getNativeWindowType(false))) {
            MacDisplay.initSingleton();
        }

        if ( USE_MAIN_THREAD ) {
            // dispatch user's main thread ..
            mainAction.start();

            // do our main thread task scheduling
            run();
        } else {
            // run user's main in this thread 
            mainAction.run();
        }
    }

    /** invokes the given Runnable */
    public static void invoke(boolean wait, Runnable r) {
        if(r == null) {
            return;
        }

        // if this main thread is not being used or
        // if this is already the main thread .. just execute.
        if( !isRunning() || mainThread == Thread.currentThread() ) {
            r.run();
            return;
        }

        synchronized(taskWorkerLock) {
            tasks.add(r);
            if(wait) {
                tasksBlock.add(r);
            }
            taskWorkerLock.notifyAll();
            if(wait) {
                while(tasksBlock.size()>0) {
                    try {
                        taskWorkerLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void exit() {
        if(DEBUG) System.err.println("MainThread.exit(): "+Thread.currentThread().getName()+" start");
        synchronized(taskWorkerLock) { 
            if(isRunning) {
                shouldStop = true;
            }
            taskWorkerLock.notifyAll();
        }
        if(DEBUG) System.err.println("MainThread.exit(): "+Thread.currentThread().getName()+" end");
    }

    public static boolean isRunning() {
        synchronized(taskWorkerLock) { 
            return isRunning;
        }
    }

    private static void waitUntilRunning() {
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

    public static void run() {
        if(DEBUG) System.err.println("MainThread.run(): "+Thread.currentThread().getName());
        synchronized(taskWorkerLock) {
            isRunning = true;
            taskWorkerLock.notifyAll();
        }
        while(!shouldStop) {
            try {
                ArrayList localTasks=null;

                // wait for something todo ..
                synchronized(taskWorkerLock) {
                    while(!shouldStop && tasks.size()==0) {
                        try {
                            taskWorkerLock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    // seq. process all tasks until no blocking one exists in the list
                    for(Iterator i = tasks.iterator(); tasksBlock.size()>0 && i.hasNext(); ) {
                        Runnable task = (Runnable) i.next();
                        task.run();
                        i.remove();
                        tasksBlock.remove(task);
                    }

                    // take over the tasks ..
                    if(tasks.size()>0) {
                        localTasks = tasks;
                        tasks = new ArrayList();
                    }
                    taskWorkerLock.notifyAll();
                }

                // seq. process all unblocking tasks ..
                if(null!=localTasks) {
                    for(Iterator i = localTasks.iterator(); i.hasNext(); ) {
                        Runnable task = (Runnable) i.next();
                        task.run();
                    }
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


