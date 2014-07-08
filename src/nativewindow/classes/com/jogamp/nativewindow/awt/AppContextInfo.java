package com.jogamp.nativewindow.awt;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

import com.jogamp.common.util.RunnableTask;

import jogamp.nativewindow.jawt.JAWTUtil;

/**
 * Instance of this class holds information about a {@link ThreadGroup} associated {@link sun.awt.AppContext}.
 * <p>
 * Non intrusive workaround for Bug 983 and Bug 1004, see {@link #getCachedThreadGroup()}.
 * </p>
 */
public class AppContextInfo {
  private static final boolean DEBUG;

  private static final Method getAppContextMethod;
  private static final Object mainThreadAppContextLock = new Object();
  private volatile WeakReference<Object> mainThreadAppContextWR = null;
  private volatile WeakReference<ThreadGroup> mainThreadGroupWR = null;

  static {
      DEBUG = JAWTUtil.DEBUG;
      final Method[] _getAppContextMethod = { null };
      AccessController.doPrivileged(new PrivilegedAction<Object>() {
          @Override
          public Object run() {
              try {
                  final Class<?> appContextClass = Class.forName("sun.awt.AppContext");
                  _getAppContextMethod[0] = appContextClass.getMethod("getAppContext");
              } catch(final Throwable ex) {
                  System.err.println("Bug 1004: Caught @ static: "+ex.getMessage());
                  ex.printStackTrace();
              }
              return null;
          } } );
      getAppContextMethod = _getAppContextMethod[0];
  }

  public AppContextInfo(final String info) {
      update(info);
  }

  /**
   * Returns <code>true</code> if this instance has valid {@link sun.awt.AppContext} information,
   * i.e. {@link #getCachedThreadGroup()} returns not <code>null</code>.
   */
  public final boolean isValid() {
      return null != getCachedThreadGroup();
  }

  /**
   * Returns the {@link ThreadGroup} belonging to the
   * last known {@link sun.awt.AppContext} as queried via {@link #update(String)}.
   * <p>
   * Returns <code>null</code> if no {@link sun.awt.AppContext} has been queried.
   * </p>
   * <p>
   * The returned {@link ThreadGroup} allows users to create a custom thread
   * belonging to it and hence mitigating Bug 983 and Bug 1004.
   * </p>
   * <p>
   * {@link #update(String)} should be called from a thread belonging to the
   * desired {@link sun.awt.AppContext}, i.e. early from within the special threaded application.
   * </p>
   * <p>
   * E.g. {@link JAWTWindow} issues {@link #update(String)} in it's constructor.
   * </p>
   */
  public final ThreadGroup getCachedThreadGroup() {
      final WeakReference<ThreadGroup> tgRef = mainThreadGroupWR;
      return null != tgRef ? tgRef.get() : null;
  }

  /**
   * Invokes <code>runnable</code> on a {@link Thread} belonging to the {@link sun.awt.AppContext} {@link ThreadGroup},
   * see {@link #getCachedThreadGroup()}.
   * <p>
   * {@link #update(String)} is issued first, which returns <code>true</code>
   * if the current thread belongs to an AppContext {@link ThreadGroup}.
   * In this case the <code>runnable</code> is invoked on the current thread,
   * otherwise a new {@link Thread} will be started.
   * </p>
   * <p>
   * If a new {@link Thread} is required, the AppContext {@link ThreadGroup} is being used
   * if {@link #isValid() available}, otherwise the default system {@link ThreadGroup}.
   * </p>
   *
   * @param waitUntilDone if <code>true</code>, waits until <code>runnable</code> execution is completed, otherwise returns immediately.
   * @param runnable the {@link Runnable} to be executed. If <code>waitUntilDone</code> is <code>true</code>,
   *                 the runnable <b>must exist</b>, i.e. not loop forever.
   * @param threadBaseName the base name for the new thread if required.
   *        The resulting thread name will have either '-OnAppContextTG' or '-OnSystemTG' appended
   * @return the {@link Thread} used to invoke the <code>runnable</code>, which may be the current {@link Thread} or a newly created one, see above.
   */
  public Thread invokeOnAppContextThread(final boolean waitUntilDone, final Runnable runnable, final String threadBaseName) {
      final Thread t;
      if( update("invoke") ) {
          t = Thread.currentThread();
          if( DEBUG ) {
              System.err.println("Bug 1004: Invoke.0 on current AppContext thread: "+t+" "+toHexString(t.hashCode()));
          }
          runnable.run();
      } else {
          final ThreadGroup tg = getCachedThreadGroup();
          final String tName = threadBaseName + ( null != tg ? "-OnAppContextTG" : "-OnSystemTG" );
          t = RunnableTask.invokeOnNewThread(tg, waitUntilDone, runnable, tName);
          if( DEBUG ) {
              final int tgHash = null != tg ? tg.hashCode() : 0;
              System.err.println("Bug 1004: Invoke.1 on new AppContext thread: "+t+" "+toHexString(t.hashCode())+", tg "+tg+" "+toHexString(tgHash));
          }
      }
      return t;
  }

  /**
   * Update {@link sun.awt.AppContext} information for the current ThreadGroup if uninitialized or {@link sun.awt.AppContext} changed.
   * <p>
   * See {@link #getCachedThreadGroup()} for usage.
   * </p>
   * @param info informal string for logging purposes
   * @return <code>true</code> if the current ThreadGroup is mapped to an {@link sun.awt.AppContext} and the information is good, otherwise false.
   */
  public final boolean update(final String info) {
      if ( null != getAppContextMethod ) {
          // Test whether the current thread's ThreadGroup is mapped to an AppContext.
          final Object thisThreadAppContext = fetchAppContext();
          final boolean tgMapped = null != thisThreadAppContext;

          final Thread thread = Thread.currentThread();
          final ThreadGroup threadGroup = thread.getThreadGroup();
          final Object mainThreadAppContext;
          {
              final WeakReference<Object> _mainThreadAppContextWR = mainThreadAppContextWR;
              mainThreadAppContext = null != _mainThreadAppContextWR ? _mainThreadAppContextWR.get() : null;
          }

          if( tgMapped ) { // null != thisThreadAppContext
              // Update info is possible
              if( null == mainThreadAppContext ||
                  mainThreadAppContext != thisThreadAppContext ) {
                  // GC'ed or 1st fetch !
                  final int mainThreadAppContextHash = null != mainThreadAppContext ? mainThreadAppContext.hashCode() : 0;
                  final int thisThreadAppContextHash;
                  synchronized(mainThreadAppContextLock) {
                      mainThreadGroupWR = new WeakReference<ThreadGroup>(threadGroup);
                      mainThreadAppContextWR = new WeakReference<Object>(thisThreadAppContext);
                      thisThreadAppContextHash = thisThreadAppContext.hashCode();
                  }
                  if( DEBUG ) {
                      System.err.println("Bug 1004[TGMapped "+tgMapped+"]: Init AppContext @ "+info+" on thread "+thread.getName()+" "+toHexString(thread.hashCode())+
                                         ": tg "+threadGroup.getName()+" "+toHexString(threadGroup.hashCode())+
                                         " -> appCtx [ main "+mainThreadAppContext+" "+toHexString(mainThreadAppContextHash)+
                                         " -> this "+thisThreadAppContext+" "+toHexString(thisThreadAppContextHash) + " ] ");
                  }
              } else {
                  // old info is OK
                  if( DEBUG ) {
                      final int mainThreadAppContextHash = mainThreadAppContext.hashCode();
                      final int thisThreadAppContextHash = thisThreadAppContext.hashCode();
                      System.err.println("Bug 1004[TGMapped "+tgMapped+"]: OK AppContext @ "+info+" on thread "+thread.getName()+" "+toHexString(thread.hashCode())+
                                         ": tg "+threadGroup.getName()+" "+toHexString(threadGroup.hashCode())+
                                         "  : appCtx [ this "+thisThreadAppContext+" "+toHexString(thisThreadAppContextHash)+
                                         "  , main "+mainThreadAppContext+" "+toHexString(mainThreadAppContextHash) + " ] ");
                  }
              }
              return true;
          } else {
              if( DEBUG ) {
                  final int mainThreadAppContextHash = null != mainThreadAppContext ? mainThreadAppContext.hashCode() : 0;
                  final int thisThreadAppContextHash = null != thisThreadAppContext ? thisThreadAppContext.hashCode() : 0;
                  System.err.println("Bug 1004[TGMapped "+tgMapped+"]: No AppContext @ "+info+" on thread "+thread.getName()+" "+toHexString(thread.hashCode())+
                                     ": tg "+threadGroup.getName()+" "+toHexString(threadGroup.hashCode())+
                                     " -> appCtx [ this "+thisThreadAppContext+" "+toHexString(thisThreadAppContextHash)+
                                     " -> main "+mainThreadAppContext+" "+toHexString(mainThreadAppContextHash) + " ] ");
              }
          }
      }
      return false;
  }
  private static Object fetchAppContext() {
      try {
          return getAppContextMethod.invoke(null);
      } catch(final Exception ex) {
          System.err.println("Bug 1004: Caught: "+ex.getMessage());
          ex.printStackTrace();
          return null;
      }
  }

  private static String toHexString(final int i) {
      return "0x"+Integer.toHexString(i);
  }

}
