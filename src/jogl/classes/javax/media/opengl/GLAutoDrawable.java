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

package javax.media.opengl;

import jogamp.opengl.Debug;

/** A higher-level abstraction than {@link GLDrawable} which supplies
    an event based mechanism ({@link GLEventListener}) for performing
    OpenGL rendering. A GLAutoDrawable automatically creates a primary
    rendering context which is associated with the GLAutoDrawable for
    the lifetime of the object. This context has the {@link
    GLContext#setSynchronized synchronized} property enabled so that
    calls to {@link GLContext#makeCurrent makeCurrent} will block if
    the context is current on another thread. This allows the internal
    GLContext for the GLAutoDrawable to be used both by the event
    based rendering mechanism as well by end users directly.
    <p>
    The implementation shall initialize itself as soon as possible,
    ie if the attached {@link javax.media.nativewindow.NativeSurface NativeSurface} becomes visible/realized.
    The following protocol shall be satisfied:
    <ul>
        <li> Create the  {@link GLDrawable} with the requested {@link GLCapabilities}</li>
        <li> Notify {@link GLDrawable} to validate the {@link GLCapabilities} by calling {@link GLDrawable#setRealized setRealized(true)}.</li>
        <li> Create the new {@link GLContext}.</li>
        <li> Initialize all OpenGL resources by calling {@link GLEventListener#init init(..)} for all
             registered {@link GLEventListener}s. This can be done immediatly, or with the followup {@link #display display(..)} call.</li>
        <li> Send a reshape event by calling {@link GLEventListener#reshape reshape(..)} for all
             registered {@link GLEventListener}s. This shall be done after the {@link GLEventListener#init init(..)} calls.</li>
    </ul></P>
    <p>
    Another implementation detail is the drawable reconfiguration. One use case is where a window is being
    dragged to another screen with a different pixel configuration, ie {@link GLCapabilities}. The implementation
    shall be able to detect such cases in conjunction with the associated {@link javax.media.nativewindow.NativeSurface NativeSurface}.<br/>
    For example, AWT's {@link java.awt.Canvas} 's {@link java.awt.Canvas#getGraphicsConfiguration getGraphicsConfiguration()}
    is capable to determine a display device change. This is demonstrated within {@link javax.media.opengl.awt.GLCanvas}'s
    and NEWT's <code>AWTCanvas</code> {@link javax.media.opengl.awt.GLCanvas#getGraphicsConfiguration getGraphicsConfiguration()}
    specialization. Another demonstration is NEWT's {@link javax.media.nativewindow.NativeWindow NativeWindow}
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
    -Djogl.screenchange.action=false Disable the drawable reconfiguration (the default)
    -Djogl.screenchange.action=true  Enable  the drawable reconfiguration
    </PRE>
    </p>
  */
public interface GLAutoDrawable extends GLDrawable {
  /** Flag reflecting wheather the drawable reconfiguration will be issued in
    * case a screen device change occured, e.g. in a multihead environment,
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
   * Associate a new context to this drawable and also propagates the context/drawable switch by 
   * calling {@link GLContext#setGLDrawable(GLDrawable, boolean) newCtx.setGLDrawable(drawable, true);}.
   * <code>drawable</code> might be an inner GLDrawable instance if using a delegation pattern,
   * or this GLAutoDrawable instance.
   * <p>
   * If the old or new context was current on this thread, it is being released before switching the drawable.
   * The new context will be made current afterwards, if it was current before. 
   * However the user shall take extra care that no other thread
   * attempts to make this context current.
   * </p>
   * <p>
   * Be aware that the old context is still bound to the drawable, 
   * and that one context can only be bound to one drawable at one time!
   * </p>
   * <p>
   * In case you do not intend to use the old context anymore, i.e. 
   * not assigning it to another drawable, it shall be 
   * destroyed before setting the new context, i.e.:
   * <pre>
            GLContext oldCtx = glad.getContext();
            if(null != oldCtx) {
                oldCtx.destroy();
            }
            glad.setContext(newCtx);            
   * </pre> 
   * This is required, since a context must have a valid drawable at all times
   * and this API shall not restrict the user in any way. 
   * </p>
   * 
   * @param newCtx the new context
   * @return the replaced GLContext, maybe <code>null</code>
   *  
   * @see GLContext#setGLDrawable(GLDrawable, boolean)
   * @see GLContext#setGLReadDrawable(GLDrawable)
   * @see jogamp.opengl.GLDrawableHelper#switchContext(GLDrawable, GLContext, GLContext, int)
   */
  public GLContext setContext(GLContext newCtx);
  
  /** Adds a {@link GLEventListener} to the end of this drawable queue.
      The listeners are notified of events in the order of the queue. */
  public void addGLEventListener(GLEventListener listener);

  /**
   * Adds a {@link GLEventListener} at the given index of this drawable queue.
   * The listeners are notified of events in the order of the queue.
   * @param index Position where the listener will be inserted.
   *              Should be within (0 <= index && index <= size()).
   *              An index value of -1 is interpreted as the end of the list, size().
   * @param listener The GLEventListener object to be inserted
   * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index <= size()), or -1
   */
  public void addGLEventListener(int index, GLEventListener listener) throws IndexOutOfBoundsException;

  /** 
   * Removes a {@link GLEventListener} from this drawable. 
   * Note that if this is done from within a particular drawable's 
   * {@link GLEventListener} handler (reshape, display, etc.) that it is not
   * guaranteed that all other listeners will be evaluated properly
   * during this update cycle.
   * @param listener The GLEventListener object to be removed
   */  
  public void removeGLEventListener(GLEventListener listener);

  /** 
   * Removes a {@link GLEventListener} at the given index from this drawable. 
   * Note that if this is done from within a particular drawable's 
   * {@link GLEventListener} handler (reshape, display, etc.) that it is not
   * guaranteed that all other listeners will be evaluated properly
   * during this update cycle.
   * @param index Position of the listener to be removed.
   *              Should be within (0 <= index && index < size()).
   *              An index value of -1 is interpreted as last listener, size()-1.
   * @return The removed GLEventListener object
   * @throws IndexOutOfBoundsException If the index is not within (0 <= index && index < size()), or -1
   */  
  public GLEventListener removeGLEventListener(int index) throws IndexOutOfBoundsException;

  /**
   * <p>
   * Registers the usage of an animator, an {@link javax.media.opengl.GLAnimatorControl} implementation.
   * The animator will be queried whether it's animating, ie periodically issuing {@link #display()} calls or not.</p><br>
   * <p>
   * This method shall be called by an animator implementation only,<br>
   * e.g. {@link com.jogamp.opengl.util.Animator#add(javax.media.opengl.GLAutoDrawable)}, passing it's control implementation,<br>
   * and {@link com.jogamp.opengl.util.Animator#remove(javax.media.opengl.GLAutoDrawable)}, passing <code>null</code>.</p><br>
   * <p>
   * Impacts {@link #display()} and {@link #invoke(boolean, GLRunnable)} semantics.</p><br>
   *
   * @param animator <code>null</code> reference indicates no animator is using
   *                 this <code>GLAutoDrawable</code>,<br>
   *                 a valid reference indicates an animator is using this <code>GLAutoDrawable</code>.
   *
   * @throws GLException if an animator is already registered.
   * @see #display()
   * @see #invoke(boolean, GLRunnable)
   * @see javax.media.opengl.GLAnimatorControl
   */
  public abstract void setAnimator(GLAnimatorControl animatorControl) throws GLException;

  /**
   * @return the registered {@link javax.media.opengl.GLAnimatorControl} implementation, using this <code>GLAutoDrawable</code>.
   *
   * @see #setAnimator(javax.media.opengl.GLAnimatorControl)
   * @see javax.media.opengl.GLAnimatorControl
   */
  public GLAnimatorControl getAnimator();

  /**
   * <p>
   * Enqueues a one-shot {@link GLRunnable},
   * which will be executed within the next {@link #display()} call
   * after all registered {@link GLEventListener}s
   * {@link GLEventListener#display(GLAutoDrawable) display(GLAutoDrawable)}
   * methods has been called.
   * </p>
   * <p>
   * If no {@link GLAnimatorControl} is animating (default),<br>
   * or if the current thread is the animator thread,<br>
   * a {@link #display()} call is issued after enqueue the <code>GLRunnable</code>.<br>
   * No extra synchronization is performed in case <code>wait</code> is true, since it is executed in the current thread.</p>
   * <p>
   * If an {@link GLAnimatorControl} is animating,<br>
   * no {@link #display()} call is issued, since the animator thread performs it.<br>
   * </p>
   * <p>
   * If <code>wait</code> is <code>true</code> the call blocks until the <code>glRunnable</code>
   * has been executed.<p>
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
   *
   * @param wait if <code>true</code> block until execution of <code>glRunnable</code> is finished, otherwise return immediatly w/o waiting
   * @param glRunnable the {@link GLRunnable} to execute within {@link #display()}
   * @return <code>true</code> if the {@link GLRunnable} has been processed or queued, otherwise <code>false</code>.
   * 
   * @see #setAnimator(GLAnimatorControl)
   * @see #display()
   * @see GLRunnable
   */
  public boolean invoke(boolean wait, GLRunnable glRunnable);

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
   *     <li> Executes all one-shot {@link javax.media.opengl.GLRunnable GLRunnable},
   *          enqueued via {@link #invoke(boolean, GLRunnable)}.</li>
   * </ul></p>
   * <p>
   * May be called periodically by a running {@link javax.media.opengl.GLAnimatorControl} implementation,<br>
   * which must register itself with {@link #setAnimator(javax.media.opengl.GLAnimatorControl)}.</p>
   * <p>
   * Called automatically by the window system toolkit upon receiving a repaint() request, <br>
   * except an {@link javax.media.opengl.GLAnimatorControl} implementation {@link javax.media.opengl.GLAnimatorControl#isAnimating()}.</p>
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
   * @see #setAnimator(javax.media.opengl.GLAnimatorControl)
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
   * To replace or set this GLAutoDrawable's GLContext you need to call {@link #setContext(GLContext)}. 
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
   *     <tr><td>AWT</td>      <td>{@link javax.media.opengl.awt.GLCanvas}</td>  <td>is a</td>   <td>{@link java.awt.Canvas}</td</tr>
   *     <tr><td>AWT</td>      <td>{@link javax.media.opengl.awt.GLJPanel}</td>  <td>is a</td>   <td>{@link javax.swing.JPanel}</td</tr>
   * </table>
   * However, the result may be other object types than the listed above 
   * due to new supported toolkits.
   * </p>
   * <p>
   * This method may also return <code>null</code> if no UI toolkit is being used,
   * as common for offscreen rendering.
   * </p>
   * @return
   */
  public Object getUpstreamWidget();

}
