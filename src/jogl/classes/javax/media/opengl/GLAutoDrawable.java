/*
 * Copyright (c) 2003 Sun Microsystems, Inc. All Rights Reserved.
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
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl;

import java.util.List;

import com.jogamp.nativewindow.NativeSurface;

import com.jogamp.common.util.locks.RecursiveLock;

import jogamp.opengl.Debug;

/** A higher-level abstraction than {@link GLDrawable} which supplies
    an event based mechanism ({@link GLEventListener}) for performing
    OpenGL rendering. A GLAutoDrawable automatically creates a primary
    rendering context which is associated with the GLAutoDrawable for
    the lifetime of the object.
    <p>
    Since the {@link GLContext} {@link GLContext#makeCurrent makeCurrent}
    implementation is synchronized, i.e. blocks if the context
    is current on another thread, the internal
    {@link GLContext} for the GLAutoDrawable can be used for the event
    based rendering mechanism and by end users directly.
    </p>
    <h5><a name="initialization">GLAutoDrawable Initialization</a></h5>
    <p>
    The implementation shall initialize itself as soon as possible,
    which is only possible <i>after</i> the attached {@link com.jogamp.nativewindow.NativeSurface NativeSurface} becomes visible and and is realized.<br>
    The following initialization sequence should be implemented:
    <ul>
        <li> Create the  {@link GLDrawable} with the requested {@link GLCapabilities}</li>
        <li> Notify {@link GLDrawable} to validate the {@link GLCapabilities} by calling {@link GLDrawable#setRealized setRealized(true)}.</li>
        <li> Create the new {@link GLContext}.</li>
        <li> Initialize all OpenGL resources by calling {@link GLEventListener#init init(..)} for all
             registered {@link GLEventListener}s. This can be done immediately, or with the followup {@link #display display(..)} call.</li>
        <li> Send a reshape event by calling {@link GLEventListener#reshape reshape(..)} for all
             registered {@link GLEventListener}s. This shall be done after the {@link GLEventListener#init init(..)} calls.</li>
    </ul>
    Note: The last to {@link GLEventListener} actions shall be also performed, when {@link #addGLEventListener(GLEventListener) adding}
    a new one to an already initialized {@link GLAutoDrawable}.
    </p>
    <h5><a name="reconfiguration">GLAutoDrawable Reconfiguration</a></h5>
    <p>
    Another implementation detail is the {@link GLDrawable} reconfiguration. One use case is where a window is being
    dragged to another screen with a different pixel configuration, ie {@link GLCapabilities}. The implementation
    shall be able to detect such cases in conjunction with the associated {@link com.jogamp.nativewindow.NativeSurface NativeSurface}.<br/>
    For example, AWT's {@link java.awt.Canvas} 's {@link java.awt.Canvas#getGraphicsConfiguration getGraphicsConfiguration()}
    is capable to determine a display device change. This is demonstrated within {@link com.jogamp.opengl.awt.GLCanvas}'s
    and NEWT's <code>AWTCanvas</code> {@link com.jogamp.opengl.awt.GLCanvas#getGraphicsConfiguration getGraphicsConfiguration()}
    specialization. Another demonstration is NEWT's {@link com.jogamp.nativewindow.NativeWindow NativeWindow}
    implementation on the Windows platform, which utilizes the native platform's <i>MonitorFromWindow(HWND)</i> function.<br/>
    All OpenGL resources shall be regenerated, while the drawable's {@link GLCapabilities} has
    to be chosen again. The following protocol shall be satisfied.
    <ul>
        <li> Controlled disposal:</li>
        <ul>
            <li> Dispose all OpenGL resources by calling {@link GLEventListener#dispose dispose(..)} for all
                 registered {@link GLEventListener}s.</li>
            <li> Destroy the {@link GLContext}.</li>
            <li> Notify {@link GLDrawable} of the invalid state by calling {@link GLDrawable#setRealized setRealized(false)}.</li>
        </ul>
        <li> Controlled regeneration:</li>
        <ul>
            <li> Create the new {@link GLDrawable} with the requested {@link GLCapabilities}
            <li> Notify {@link GLDrawable} to revalidate the {@link GLCapabilities} by calling {@link GLDrawable#setRealized setRealized(true)}.</li>
            <li> Create the new {@link GLContext}.</li>
            <li> Initialize all OpenGL resources by calling {@link GLEventListener#init init(..)} for all
                 registered {@link GLEventListener}s. This can be done immediatly, or with the followup {@link #display display(..)} call.</li>
            <li> Send a reshape event by calling {@link GLEventListener#reshape reshape(..)} for all
                 registered {@link GLEventListener}s. This shall be done after the {@link GLEventListener#init init(..)} calls.</li>
        </ul>
    </ul>
    Note: Current graphics driver keep the surface configuration for a given window, even if the window is moved to
    a monitor with a different pixel configuration, ie 32bpp to 16bpp. However, it is best to not assume such behavior
    and make your application comply with the above protocol.
    <p>
    Avoiding breakage with older applications and because of the situation
    mentioned above, the <code>boolean</code> system property <code>jogl.screenchange.action</code> will control the
    screen change action as follows:<br/>
    <PRE>
    -Djogl.screenchange.action=false Disable the {@link GLDrawable} reconfiguration (the default)
    -Djogl.screenchange.action=true  Enable  the {@link GLDrawable} reconfiguration
    </PRE>
    </p>
    <h5><a name="locking">GLAutoDrawable Locking</a></h5>
    GLAutoDrawable implementations perform locking in the following order:
    <ol>
      <li> {@link #getUpstreamLock()}.{@link RecursiveLock#lock() lock()}</li>
      <li> {@link #getNativeSurface()}.{@link NativeSurface#lockSurface() lockSurface()} </li>
    </ol>
    and releases the locks accordingly:
    <ol>
      <li> {@link #getNativeSurface()}.{@link NativeSurface#unlockSurface() unlockSurface()} </li>
      <li> {@link #getUpstreamLock()}.{@link RecursiveLock#unlock() unlock()}</li>
    </ol>
    Above <i>locking order</i> is mandatory to guarantee
    atomicity of operation and to avoid race-conditions.
    A custom implementation or user applications requiring exclusive access
    shall follow the <i>locking order</i>.
    See:
    <ul>
      <li>{@link #getUpstreamLock()}</li>
      <li>{@link #invoke(boolean, GLRunnable)}</li>
      <li>{@link #invoke(boolean, List)}</li>
    </ul>
    </p>
  */
public interface GLAutoDrawable extends GLDrawable {
  /** Flag reflecting whether the {@link GLDrawable} reconfiguration will be issued in
    * case a screen device change occurred, e.g. in a multihead environment,
    * where you drag the window to another monitor. */
  public static final boolean SCREEN_CHANGE_ACTION_ENABLED = Debug.getBooleanProperty("jogl.screenchange.action", true);

  /**
   * If the implementation uses delegation, return the delegated {@link GLDrawable} instance,
   * otherwise return <code>this</code> instance.
   */
  public GLDrawable getDelegatedDrawable();

  /**
   * Returns the context associated with this drawable. The returned
   * context will be synchronized.
   * Don't rely on it's identity, the context may change.
   */
  public GLContext getContext();

  /**
   * Associate the new context, <code>newtCtx</code>, to this auto-drawable.
   * <p>
   * Remarks:
   * <ul>
   *   <li>The currently associated context will be destroyed if <code>destroyPrevCtx</code> is <code>true</code>,
   *       otherwise it will be disassociated from this auto-drawable
   *       via {@link GLContext#setGLDrawable(GLDrawable, boolean) setGLDrawable(null, true);} including {@link GL#glFinish() glFinish()}.</li>
   *   <li>The new context will be associated with this auto-drawable
   *       via {@link GLContext#setGLDrawable(GLDrawable, boolean) newCtx.setGLDrawable(drawable, true);}.</li>
   *   <li>If the old context was current on this thread, it is being released after disassociating this auto-drawable.</li>
   *   <li>If the new context was current on this thread, it is being released before associating this auto-drawable
   *       and made current afterwards.</li>
   *   <li>Implementation may issue {@link #makeCurrent()} and {@link #release()} while drawable reassociation.</li>
   *   <li>The user shall take extra care of thread synchronization,
   *       i.e. lock the involved {@link GLAutoDrawable auto-drawable's}
   *       {@link GLAutoDrawable#getUpstreamLock() upstream-locks} and {@link GLAutoDrawable#getNativeSurface() surfaces}
   *       to avoid a race condition. See <a href="#locking">GLAutoDrawable Locking</a>.</li>
   * </ul>
   * </p>
   *
   * @param newCtx the new context, maybe <code>null</code> for dis-association.
   * @param destroyPrevCtx if <code>true</code>, destroy the previous context if exists
   * @return the previous GLContext, maybe <code>null</code>
   *
   * @see GLContext#setGLDrawable(GLDrawable, boolean)
   * @see GLContext#setGLReadDrawable(GLDrawable)
   * @see jogamp.opengl.GLDrawableHelper#switchContext(GLDrawable, GLContext, boolean, GLContext, int)
   */
  public GLContext setContext(GLContext newCtx, boolean destroyPrevCtx);

  /**
   * Adds the given {@link GLEventListener listener} to the end of this drawable queue.
   * The {@link GLEventListener listeners} are notified of events in the order of the queue.
   * <p>
   * The newly added listener's {@link GLEventListener#init(GLAutoDrawable) init(..)}
   * method will be called once before any other of it's callback methods.
   * See {@link #getGLEventListenerInitState(GLEventListener)} for details.
   * </p>
   * @param listener The GLEventListener object to be inserted
   */
  public void addGLEventListener(GLEventListener listener);

  /**
   * Adds the given {@link GLEventListener listener} at the given index of this drawable queue.
   * The {@link GLEventListener listeners} are notified of events in the order of the queue.
   * <p>
   * The newly added listener's {@link GLEventListener#init(GLAutoDrawable) init(..)}
   * method will be called once before any other of it's callback methods.
   * See {@link #getGLEventListenerInitState(GLEventListener)} for details.
   * </p>
   * @param index Position where the listener will be inserted.
   *              Should be within (0 <= index && index <= size()).
   *              An index value of -1 is interpreted as the end of the list, size().
   * @param listener The GLEventListener object to be inserted
   * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
   */
  public void addGLEventListener(int index, GLEventListener listener) throws IndexOutOfBoundsException;

  /**
   * Returns the number of {@link GLEventListener} of this drawable queue.
   * @return The number of GLEventListener objects of this drawable queue.
   */
  public int getGLEventListenerCount();

  /**
   * Returns true if all added {@link GLEventListener} are initialized, otherwise false.
   * @since 2.2
   */
  boolean areAllGLEventListenerInitialized();

  /**
   * Returns the {@link GLEventListener} at the given index of this drawable queue.
   * @param index Position of the listener to be returned.
   *              Should be within (0 <= index && index < size()).
   *              An index value of -1 is interpreted as last listener, size()-1.
   * @return The GLEventListener object at the given index.
   * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index < size()), or -1
   */
  public GLEventListener getGLEventListener(int index) throws IndexOutOfBoundsException;

  /**
   * Retrieves whether the given {@link GLEventListener listener} is initialized or not.
   * <p>
   * After {@link #addGLEventListener(GLEventListener) adding} a {@link GLEventListener} it is
   * marked <i>uninitialized</i> and added to a list of to be initialized {@link GLEventListener}.
   * If such <i>uninitialized</i> {@link GLEventListener}'s handler methods (reshape, display)
   * are about to be invoked, it's {@link GLEventListener#init(GLAutoDrawable) init(..)} method is invoked first.
   * Afterwards the {@link GLEventListener} is marked <i>initialized</i>
   * and removed from the list of to be initialized {@link GLEventListener}.
   * </p>
   * <p>
   * This methods returns the {@link GLEventListener} initialized state,
   * i.e. returns <code>false</code> if it is included in the list of to be initialized {@link GLEventListener},
   * otherwise <code>true</code>.
   * </p>
   * @param listener the GLEventListener object to query it's initialized state.
   */
  public boolean getGLEventListenerInitState(GLEventListener listener);

  /**
   * Sets the given {@link GLEventListener listener's} initialized state.
   * <p>
   * This methods allows manually setting the {@link GLEventListener} initialized state,
   * i.e. adding it to, or removing it from the list of to be initialized {@link GLEventListener}.
   * See {@link #getGLEventListenerInitState(GLEventListener)} for details.
   * </p>
   * <p>
   * <b>Warning:</b> This method does not validate whether the given {@link GLEventListener listener's}
   * is member of this drawable queue, i.e. {@link #addGLEventListener(GLEventListener) added}.
   * </p>
   * <p>
   * This method is only exposed to allow users full control over the {@link GLEventListener}'s state
   * and is usually not recommended to change.
   * </p>
   * <p>
   * One use case is moving a {@link GLContext} and their initialized {@link GLEventListener}
   * from one {@link GLAutoDrawable} to another,
   * where a subsequent {@link GLEventListener#init(GLAutoDrawable) init(..)} call after adding it
   * to the new owner is neither required nor desired.
   * See {@link com.jogamp.opengl.util.GLDrawableUtil#swapGLContextAndAllGLEventListener(GLAutoDrawable, GLAutoDrawable) swapGLContextAndAllGLEventListener(..)}.
   * </p>
   * @param listener the GLEventListener object to perform a state change.
   * @param initialized if <code>true</code>, mark the listener initialized, otherwise uninitialized.
   */
  public void setGLEventListenerInitState(GLEventListener listener, boolean initialized);

  /**
   * Disposes the given {@link GLEventListener listener} via {@link GLEventListener#dispose(GLAutoDrawable) dispose(..)}
   * if it has been initialized and added to this queue.
   * <p>
   * If <code>remove</code> is <code>true</code>, the {@link GLEventListener} is removed from this drawable queue before disposal,
   * otherwise marked uninitialized.
   * </p>
   * <p>
   * If an {@link GLAnimatorControl} is being attached and the current thread is different
   * than {@link GLAnimatorControl#getThread() the animator's thread}, it is paused during the operation.
   * </p>
   * <p>
   * Note that this is an expensive operation, since {@link GLEventListener#dispose(GLAutoDrawable) dispose(..)}
   * is decorated by {@link GLContext#makeCurrent()} and {@link GLContext#release()}.
   * </p>
   * <p>
   * Use {@link #removeGLEventListener(GLEventListener) removeGLEventListener(listener)} instead
   * if you just want to remove the {@link GLEventListener listener} and <i>don't care</i> about the disposal of the it's (OpenGL) resources.
   * </p>
   * <p>
   * Also note that this is done from within a particular drawable's
   * {@link GLEventListener} handler (reshape, display, etc.), that it is not
   * guaranteed that all other listeners will be evaluated properly
   * during this update cycle.
   * </p>
   * @param listener The GLEventListener object to be disposed and removed if <code>remove</code> is <code>true</code>
   * @param remove pass <code>true</code> to have the <code>listener</code> removed from this drawable queue, otherwise pass <code>false</code>
   * @return the disposed and/or removed GLEventListener, or null if no action was performed, i.e. listener was not added
   */
  public GLEventListener disposeGLEventListener(GLEventListener listener, boolean remove);

  /**
   * Removes the given {@link GLEventListener listener} from this drawable queue.
   * <p>
   * This is an inexpensive operation, since the removed listener's
   * {@link GLEventListener#dispose(GLAutoDrawable) dispose(..)} method will <i>not</i> be called.
   * </p>
   * <p>
   * Use {@link #disposeGLEventListener(GLEventListener, boolean) disposeGLEventListener(listener, true)}
   * instead to ensure disposal of the {@link GLEventListener listener}'s (OpenGL) resources.
   * </p>
   * <p>
   * Note that if this is done from within a particular drawable's
   * {@link GLEventListener} handler (reshape, display, etc.), that it is not
   * guaranteed that all other listeners will be evaluated properly
   * during this update cycle.
   * </p>
   * @param listener The GLEventListener object to be removed
   * @return the removed GLEventListener, or null if listener was not added
   */
  public GLEventListener removeGLEventListener(GLEventListener listener);

  /**
   * Registers the usage of an animator, an {@link com.jogamp.opengl.GLAnimatorControl} implementation.
   * The animator will be queried whether it's animating, ie periodically issuing {@link #display()} calls or not.
   * <p>
   * This method shall be called by an animator implementation only,<br>
   * e.g. {@link com.jogamp.opengl.util.Animator#add(com.jogamp.opengl.GLAutoDrawable)}, passing it's control implementation,<br>
   * and {@link com.jogamp.opengl.util.Animator#remove(com.jogamp.opengl.GLAutoDrawable)}, passing <code>null</code>.
   * </p>
   * <p>
   * Impacts {@link #display()} and {@link #invoke(boolean, GLRunnable)} semantics.</p><br>
   *
   * @param animatorControl <code>null</code> reference indicates no animator is using
   *                        this <code>GLAutoDrawable</code>,<br>
   *                        a valid reference indicates an animator is using this <code>GLAutoDrawable</code>.
   *
   * @throws GLException if an animator is already registered.
   * @see #display()
   * @see #invoke(boolean, GLRunnable)
   * @see com.jogamp.opengl.GLAnimatorControl
   */
  public abstract void setAnimator(GLAnimatorControl animatorControl) throws GLException;

  /**
   * @return the registered {@link com.jogamp.opengl.GLAnimatorControl} implementation, using this <code>GLAutoDrawable</code>.
   *
   * @see #setAnimator(com.jogamp.opengl.GLAnimatorControl)
   * @see com.jogamp.opengl.GLAnimatorControl
   */
  public GLAnimatorControl getAnimator();

  /**
   * Dedicates this instance's {@link GLContext} to the given thread.<br/>
   * The thread will exclusively claim the {@link GLContext} via {@link #display()} and not release it
   * until {@link #destroy()} or <code>setExclusiveContextThread(null)</code> has been called.
   * <p>
   * Default non-exclusive behavior is <i>requested</i> via <code>setExclusiveContextThread(null)</code>,
   * which will cause the next call of {@link #display()} on the exclusive thread to
   * release the {@link GLContext}. Only after it's async release, {@link #getExclusiveContextThread()}
   * will return <code>null</code>.
   * </p>
   * <p>
   * To release a previous made exclusive thread, a user issues <code>setExclusiveContextThread(null)</code>
   * and may poll {@link #getExclusiveContextThread()} until it returns <code>null</code>,
   * <i>while</i> the exclusive thread is still running.
   * </p>
   * <p>
   * Note: Setting a new exclusive thread without properly releasing a previous one
   * will throw an GLException.
   * </p>
   * <p>
   * Note: Utilizing this feature w/ AWT could lead to an AWT-EDT deadlock, depending on the AWT implementation.
   * Hence it is advised not to use it with native AWT GLAutoDrawable like GLCanvas.
   * </p>
   * <p>
   * One scenario could be to dedicate the context to the {@link GLAnimatorControl#getThread() animator thread}
   * and spare redundant context switches, see {@link com.jogamp.opengl.util.AnimatorBase#setExclusiveContext(boolean)}.
   * </p>
   * @param t the exclusive thread to claim the context, or <code>null</code> for default operation.
   * @return previous exclusive context thread
   * @throws GLException If an exclusive thread is still active but a new one is attempted to be set
   * @see com.jogamp.opengl.util.AnimatorBase#setExclusiveContext(boolean)
   */
  public Thread setExclusiveContextThread(Thread t) throws GLException;

  /**
   * @see #setExclusiveContextThread(Thread)
   */
  public Thread getExclusiveContextThread();

  /**
   * Enqueues a one-shot {@link GLRunnable},
   * which will be executed within the next {@link #display()} call
   * after all registered {@link GLEventListener}s
   * {@link GLEventListener#display(GLAutoDrawable) display(GLAutoDrawable)}
   * methods have been called.
   * <p>
   * If no {@link GLAnimatorControl} is animating (default),<br>
   * or if the current thread is the animator thread,<br>
   * a {@link #display()} call is issued after enqueue the <code>GLRunnable</code>,
   * hence the {@link GLRunnable} will be executed right away.<br/>
   * </p>
   * <p>
   * If an {@link GLAnimatorControl animator} is running,<br>
   * no explicit {@link #display()} call is issued, allowing the {@link GLAnimatorControl animator} to perform at due time.<br>
   * </p>
   * <p>
   * If <code>wait</code> is <code>true</code> the call blocks until the <code>glRunnable</code>
   * has been executed by the {@link GLAnimatorControl animator}, otherwise the method returns immediately.
   * </p>
   * <p>
   * If <code>wait</code> is <code>true</code> <b>and</b>
   * {@link #isRealized()} returns <code>false</code> <i>or</i> {@link #getContext()} returns <code>null</code>,
   * the call is ignored and returns <code>false</code>.<br>
   * This helps avoiding deadlocking the caller.
   * </p>
   * <p>
   * The internal queue of {@link GLRunnable}'s is being flushed with {@link #destroy()}
   * where all blocked callers are being notified.
   * </p>
   * <p>
   * To avoid a deadlock situation which causes an {@link IllegalStateException} one should
   * avoid issuing {@link #invoke(boolean, GLRunnable) invoke} while this <a href="#locking">GLAutoDrawable is being locked</a>.<br>
   * Detected deadlock situations throwing an {@link IllegalStateException} are:
   * <ul>
   *   <li>{@link #getAnimator() Animator} is running on another thread and waiting and is locked on current thread, but is not {@link #isThreadGLCapable() GL-Thread}</li>
   *   <li>No {@link #getAnimator() Animator} is running on another thread and is locked on current thread, but is not {@link #isThreadGLCapable() GL-Thread}</li>
   * </ul>
   * </p>
   *
   * @param wait if <code>true</code> block until execution of <code>glRunnable</code> is finished, otherwise return immediately w/o waiting
   * @param glRunnable the {@link GLRunnable} to execute within {@link #display()}
   * @return <code>true</code> if the {@link GLRunnable} has been processed or queued, otherwise <code>false</code>.
   * @throws IllegalStateException in case of a detected deadlock situation ahead, see above.
   *
   * @see #setAnimator(GLAnimatorControl)
   * @see #display()
   * @see GLRunnable
   * @see #invoke(boolean, List)
   * @see #flushGLRunnables()
   */
  public boolean invoke(boolean wait, GLRunnable glRunnable) throws IllegalStateException ;

  /**
   * Extends {@link #invoke(boolean, GLRunnable)} functionality
   * allowing to inject a list of {@link GLRunnable}s.
   * @param wait if <code>true</code> block until execution of the last <code>glRunnable</code> is finished, otherwise return immediately w/o waiting
   * @param glRunnables the {@link GLRunnable}s to execute within {@link #display()}
   * @return <code>true</code> if the {@link GLRunnable}s has been processed or queued, otherwise <code>false</code>.
   * @throws IllegalStateException in case of a detected deadlock situation ahead, see {@link #invoke(boolean, GLRunnable)}.
   * @see #invoke(boolean, GLRunnable)
   * @see #flushGLRunnables()
   */
  public boolean invoke(boolean wait, List<GLRunnable> glRunnables) throws IllegalStateException;

  /**
   * Flushes all {@link #invoke(boolean, GLRunnable) enqueued} {@link GLRunnable} of this {@link GLAutoDrawable}
   * including notifying waiting executor.
   * <p>
   * The executor which might have been blocked until notified
   * will be unblocked and all tasks removed from the queue.
   * </p>
   * @see #invoke(boolean, GLRunnable)
   * @since 2.2
   */
  public void flushGLRunnables();

  /** Destroys all resources associated with this GLAutoDrawable,
      inclusive the GLContext.
      If a window is attached to it's implementation, it shall be closed.
      Causes disposing of all OpenGL resources
      by calling {@link GLEventListener#dispose dispose(..)} for all
      registered {@link GLEventListener}s. Called automatically by the
      window system toolkit upon receiving a destroy notification. This
      routine may be called manually. */
  public void destroy();

  /**
   * <p>
   * Causes OpenGL rendering to be performed for this GLAutoDrawable
   * in the following order:
   * <ul>
   *     <li> Calling {@link GLEventListener#display display(..)} for all
   *          registered {@link GLEventListener}s. </li>
   *     <li> Executes all one-shot {@link com.jogamp.opengl.GLRunnable GLRunnable},
   *          enqueued via {@link #invoke(boolean, GLRunnable)}.</li>
   * </ul></p>
   * <p>
   * May be called periodically by a running {@link com.jogamp.opengl.GLAnimatorControl} implementation,<br>
   * which must register itself with {@link #setAnimator(com.jogamp.opengl.GLAnimatorControl)}.</p>
   * <p>
   * Called automatically by the window system toolkit upon receiving a repaint() request, <br>
   * except an {@link com.jogamp.opengl.GLAnimatorControl} implementation {@link com.jogamp.opengl.GLAnimatorControl#isAnimating()}.</p>
   * <p>
   * This routine may also be called manually for better control over the
   * rendering process. It is legal to call another GLAutoDrawable's
   * display method from within the {@link GLEventListener#display
   * display(..)} callback.</p>
   * <p>
   * In case of a new generated OpenGL context,
   * the implementation shall call {@link GLEventListener#init init(..)} for all
   * registered {@link GLEventListener}s <i>before</i> making the
   * actual {@link GLEventListener#display display(..)} calls,
   * in case this has not been done yet.</p>
   *
   * @see #setAnimator(com.jogamp.opengl.GLAnimatorControl)
   */
  public void display();

  /** Enables or disables automatic buffer swapping for this drawable.
      By default this property is set to true; when true, after all
      GLEventListeners have been called for a display() event, the
      front and back buffers are swapped, displaying the results of
      the render. When disabled, the user is responsible for calling
      {@link #swapBuffers(..)} manually. */
  public void setAutoSwapBufferMode(boolean enable);

  /** Indicates whether automatic buffer swapping is enabled for this
      drawable. See {@link #setAutoSwapBufferMode}. */
  public boolean getAutoSwapBufferMode();

  /**
   * @param flags Additional context creation flags.
   *
   * @see GLContext#setContextCreationFlags(int)
   * @see GLContext#enableGLDebugMessage(boolean)
   */
  public void setContextCreationFlags(int flags);

  /**
   * @return Additional context creation flags
   */
  public int getContextCreationFlags();

  /**
   * {@inheritDoc}
   * <p>
   * This GLAutoDrawable implementation holds it's own GLContext reference,
   * thus created a GLContext using this methods won't replace it implicitly.
   * To replace or set this GLAutoDrawable's GLContext you need to call {@link #setContext(GLContext, boolean)}.
   * </p>
   * <p>
   * The GLAutoDrawable implementation shall also set the
   * context creation flags as customized w/ {@link #setContextCreationFlags(int)}.
   * </p>
   */
  @Override
  public GLContext createContext(GLContext shareWith);

  /** Returns the {@link GL} pipeline object this GLAutoDrawable uses.
      If this method is called outside of the {@link
      GLEventListener}'s callback methods (init, display, etc.) it may
      return null. Users should not rely on the identity of the
      returned GL object; for example, users should not maintain a
      hash table with the GL object as the key. Additionally, the GL
      object should not be cached in client code, but should be
      re-fetched from the GLAutoDrawable at the beginning of each call
      to init, display, etc. */
  public GL getGL();

  /** Sets the {@link GL} pipeline object this GLAutoDrawable uses.
      This should only be called from within the GLEventListener's
      callback methods, and usually only from within the init()
      method, in order to install a composable pipeline. See the JOGL
      demos for examples.
      @return the set GL pipeline or null if not successful */
  public GL setGL(GL gl);

  /**
   * Method <i>may</i> return the upstream UI toolkit object
   * holding this {@link GLAutoDrawable} instance, if exist.
   * <p>
   * Currently known Java UI toolkits and it's known return types are:
   *
   * <table border="1">
   *     <tr><td>Toolkit</td>  <td>GLAutoDrawable Implementation</td>            <td>~</td>      <td>Return Type of getUpstreamWidget()</td</tr>
   *     <tr><td>NEWT</td>     <td>{@link com.jogamp.newt.opengl.GLWindow}</td>  <td>has a</td>  <td>{@link com.jogamp.newt.Window}</td</tr>
   *     <tr><td>SWT</td>      <td>{@link com.jogamp.opengl.swt.GLCanvas}</td>   <td>is a</td>   <td>{@link org.eclipse.swt.widgets.Canvas}</td</tr>
   *     <tr><td>AWT</td>      <td>{@link com.jogamp.opengl.awt.GLCanvas}</td>  <td>is a</td>   <td>{@link java.awt.Canvas}</td</tr>
   *     <tr><td>AWT</td>      <td>{@link com.jogamp.opengl.awt.GLJPanel}</td>  <td>is a</td>   <td>{@link javax.swing.JPanel}</td</tr>
   * </table>
   * However, the result may be other object types than the listed above
   * due to new supported toolkits.
   * </p>
   * <p>
   * This method may also return <code>null</code> if no UI toolkit is being used,
   * as common for offscreen rendering.
   * </p>
   */
  public Object getUpstreamWidget();

  /**
   * Returns the recursive lock object of the {@link #getUpstreamWidget() upstream widget}
   * to synchronize multithreaded access on top of {@link NativeSurface#lockSurface()}.
   * <p>
   * See <a href="#locking">GLAutoDrawable Locking</a>.
   * </p>
   * @since 2.2
   */
  public RecursiveLock getUpstreamLock();

  /**
   * Indicates whether the current thread is capable of
   * performing OpenGL-related work.
   * <p>
   * Implementation utilizes this knowledge to determine
   * whether {@link #display()} performs the OpenGL commands on the current thread directly
   * or spawns them on the dedicated OpenGL thread.
   * </p>
   * @since 2.2
   */
  public boolean isThreadGLCapable();

}
