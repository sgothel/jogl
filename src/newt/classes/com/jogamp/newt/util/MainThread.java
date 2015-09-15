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
import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.NativeWindowFactory;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.ReflectionUtil;

import jogamp.newt.Debug;
import jogamp.newt.NEWTJNILibLoader;

/**
 * NEWT Utility class MainThread<P>
 *
 * <p>
 * FIXME: Update this documentation!
 * This class just provides a main-thread utility, forking of a main java class
 * on another thread while being able to continue doing platform specific things
 * on the main-thread. The latter is essential for eg. MacOSX, where we continue
 * to run NSApp.run().
 * </p>
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
 * have a look at the {@link jogamp.newt.driver.macosx.WindowDriver NEWT Mac OSX Window} driver implementation.<br>
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
public class MainThread {
    private static final String MACOSXDisplayClassName = "jogamp.newt.driver.macosx.DisplayDriver";
    private static final Platform.OSType osType;
    private static final boolean isMacOSX;
    private static final ThreadGroup rootThreadGroup;

    /** if true, use the main thread EDT, otherwise AWT's EDT */
    public static final boolean  HINT_USE_MAIN_THREAD;

    static {
        NativeWindowFactory.initSingleton();
        NEWTJNILibLoader.loadNEWT();
        HINT_USE_MAIN_THREAD = !NativeWindowFactory.isAWTAvailable() ||
                                PropertyAccess.getBooleanProperty("newt.MainThread.force", true);
        osType = Platform.getOSType();
        isMacOSX = osType == Platform.OSType.MACOS;
        rootThreadGroup = getRootThreadGroup();
    }

    public static boolean useMainThread = false;

    protected static final boolean DEBUG = Debug.debug("MainThread");

    private static final MainThread singletonMainThread = new MainThread(); // one singleton MainThread

    private static final ThreadGroup getRootThreadGroup() {
        ThreadGroup rootGroup = Thread.currentThread( ).getThreadGroup( );
        ThreadGroup parentGroup;
        while ( ( parentGroup = rootGroup.getParent() ) != null ) {
            rootGroup = parentGroup;
        }
        return rootGroup;
    }

    private static final Thread[] getAllThreads(final int[] count) {
        int tn;
        Thread[] threads = new Thread[ rootThreadGroup.activeCount() ];
        while ( ( tn = rootThreadGroup.enumerate( threads, true ) ) == threads.length ) {
            threads = new Thread[ threads.length * 2 ];
        }
        count[0] = tn;
        return threads;
    }
    private static final List<Thread> getNonDaemonThreads() {
        final List<Thread> res = new ArrayList<Thread>();
        final int[] tn = { 0 };
        final Thread[] threads = getAllThreads(tn);
        for(int i = tn[0] - 1; i >= 0; i--) {
            final Thread thread = threads[i];
            try {
                if(thread.isAlive() && !thread.isDaemon()) {
                    res.add(thread);
                    if(DEBUG) System.err.println("XXX0: "+thread.getName()+", "+thread);
                }
            } catch (final Throwable t) {
                t.printStackTrace();
            }
        }
        return res;
    }
    private static final int getNonDaemonThreadCount(final List<Thread> ignoreThreads) {
        int res = 0;
        final int[] tn = { 0 };
        final Thread[] threads = getAllThreads(tn);

        for(int i = tn[0] - 1; i >= 0; i--) {
            final Thread thread = threads[i];
            try {
                if(thread.isAlive() && !thread.isDaemon() && !ignoreThreads.contains(thread)) {
                    res++;
                    if(DEBUG) System.err.println("MainAction.run(): non daemon thread: "+thread);
                }
            } catch (final Throwable t) {
                t.printStackTrace();
            }
        }
        return res;
    }

    static class UserApp extends InterruptSource.Thread {
        private final String mainClassNameShort;
        private final String mainClassName;
        private final String[] mainClassArgs;
        private final Method mainClassMain;
        private List<java.lang.Thread> nonDaemonThreadsAtStart;

        public UserApp(final String mainClassName, final String[] mainClassArgs) throws SecurityException, NoSuchMethodException, ClassNotFoundException {
            this.mainClassName=mainClassName;
            this.mainClassArgs=mainClassArgs;

            final Class<?> mainClass = ReflectionUtil.getClass(mainClassName, true, getClass().getClassLoader());
            if(null==mainClass) {
                throw new ClassNotFoundException("MainAction couldn't find main class "+mainClassName);
            }
            mainClassNameShort = mainClass.getSimpleName();
            mainClassMain = mainClass.getDeclaredMethod("main", new Class[] { String[].class });
            mainClassMain.setAccessible(true);

            setName(getName()+"-UserApp-"+mainClassNameShort);
            setDaemon(false);

            if(DEBUG) System.err.println("MainAction(): instantiated: "+getName()+", is daemon "+isDaemon()+", main-class: "+mainClass.getName());
        }

        @Override
        public void run() {
            nonDaemonThreadsAtStart = getNonDaemonThreads();
            if(DEBUG) System.err.println("MainAction.run(): "+java.lang.Thread.currentThread().getName()+" start, nonDaemonThreadsAtStart "+nonDaemonThreadsAtStart);
            // start user app ..
            try {
                if(DEBUG) System.err.println("MainAction.run(): "+java.lang.Thread.currentThread().getName()+" invoke "+mainClassName);
                mainClassMain.invoke(null, new Object[] { mainClassArgs } );
            } catch (final InvocationTargetException ite) {
                ite.getTargetException().printStackTrace();
                return;
            } catch (final Throwable t) {
                t.printStackTrace();
                return;
            }

            // wait until no more active non-daemon threads are running
            {
                int ndtr;
                while( 0 < ( ndtr = getNonDaemonThreadCount(nonDaemonThreadsAtStart) ) ) {
                    if(DEBUG) System.err.println("MainAction.run(): post user app, non daemon threads alive: "+ndtr);
                    try {
                        java.lang.Thread.sleep(1000);
                    } catch (final InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(DEBUG) System.err.println("MainAction.run(): "+java.lang.Thread.currentThread().getName()+" user app fin: "+ndtr);
            }

            if ( useMainThread ) {
                if(isMacOSX) {
                    try {
                        if(DEBUG) {
                            System.err.println("MainAction.main(): "+java.lang.Thread.currentThread()+" MainAction fin - stopNSApp.0");
                        }
                        ReflectionUtil.callStaticMethod(MACOSXDisplayClassName, "stopNSApplication",
                            null, null, MainThread.class.getClassLoader());
                        if(DEBUG) {
                            System.err.println("MainAction.main(): "+java.lang.Thread.currentThread()+" MainAction fin - stopNSApp.X");
                        }
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    if(DEBUG) System.err.println("MainAction.run(): "+java.lang.Thread.currentThread().getName()+" MainAction fin - System.exit(0)");
                    System.exit(0);
                }
            }
        }
    }
    private static UserApp mainAction;

    /** Your new java application main entry, which pipelines your application
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws SecurityException */
    public static void main(final String[] args) throws SecurityException, NoSuchMethodException, ClassNotFoundException {
        final Thread cur = Thread.currentThread();

        useMainThread = HINT_USE_MAIN_THREAD;

        if(DEBUG) {
            System.err.println("MainThread.main(): "+cur.getName()+
                ", useMainThread "+ useMainThread +
                ", HINT_USE_MAIN_THREAD "+ HINT_USE_MAIN_THREAD +
                ", isAWTAvailable " + NativeWindowFactory.isAWTAvailable() + ", ostype "+osType+", isMacOSX "+isMacOSX);
        }

        if(!useMainThread && !NativeWindowFactory.isAWTAvailable()) {
            throw new RuntimeException("!USE_MAIN_THREAD and no AWT available");
        }

        if(args.length==0) {
            return;
        }

        final String mainClassName=args[0];
        final String[] mainClassArgs=new String[args.length-1];
        if(args.length>1) {
            System.arraycopy(args, 1, mainClassArgs, 0, args.length-1);
        }

        mainAction = new UserApp(mainClassName, mainClassArgs);

        if(isMacOSX) {
            ReflectionUtil.callStaticMethod(MACOSXDisplayClassName, "initSingleton",
                null, null, MainThread.class.getClassLoader());
        }

        if ( useMainThread ) {
            try {
                cur.setName(cur.getName()+"-MainThread");
            } catch (final Exception e) {}

            // dispatch user's main thread ..
            mainAction.start();

            if(isMacOSX) {
                try {
                    if(DEBUG) {
                        System.err.println("MainThread.main(): "+cur.getName()+"- runNSApp");
                    }
                    ReflectionUtil.callStaticMethod(MACOSXDisplayClassName, "runNSApplication",
                        null, null, MainThread.class.getClassLoader());
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
            if(DEBUG) { System.err.println("MainThread - wait until last non daemon thread ends ..."); }
        } else {
            // run user's main in this thread
            mainAction.run();
        }
    }

    public static MainThread getSingleton() {
        return singletonMainThread;
    }

}


