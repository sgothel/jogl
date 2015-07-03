/**
 * Copyright 2011 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.opengl.swt;

import java.util.List;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.AbstractGraphicsScreen;
import com.jogamp.nativewindow.GraphicsConfigurationFactory;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.NativeWindowException;
import com.jogamp.nativewindow.ProxySurface;
import com.jogamp.nativewindow.UpstreamSurfaceHook;
import com.jogamp.nativewindow.VisualIDHolder;
import com.jogamp.nativewindow.VisualIDHolder.VIDType;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesChooser;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.opengl.GLSharedContextSetter;
import com.jogamp.opengl.Threading;

import jogamp.nativewindow.x11.X11Util;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.GLDrawableImpl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.nativewindow.x11.X11GraphicsDevice;
import com.jogamp.opengl.JoglVersion;

/**
 * Native SWT Canvas implementing GLAutoDrawable
 * <p>
 * Implementation allows use of custom {@link GLCapabilities}.
 * </p>
 * <p>
 * <a name="contextSharing"><h5>OpenGL Context Sharing</h5></a>
 * To share a {@link GLContext} see the following note in the documentation overview:
 * <a href="../../../../overview-summary.html#SHARING">context sharing</a>
 * as well as {@link GLSharedContextSetter}.
 * </p>
 */
public class GLCanvas extends Canvas implements GLAutoDrawable, GLSharedContextSetter {
  private static final boolean DEBUG = Debug.debug("GLCanvas");

   /*
    * Flag for whether the SWT thread should be used for OpenGL calls when in single-threaded mode. This is controlled
    * by the setting of the threading mode to worker (do not use SWT thread), awt (use SWT thread), or false (always use
    * calling thread).
    *
    * @see Threading
    *
    * Now done dynamically to avoid early loading of gluegen library.
    */
   //private static final boolean useSWTThread = ThreadingImpl.getMode() != ThreadingImpl.WORKER;

   /* GL Stuff */
   private final RecursiveLock lock = LockFactory.createRecursiveLock();
   private final GLDrawableHelper helper = new GLDrawableHelper();

   private final GLCapabilitiesImmutable capsRequested;
   private final GLCapabilitiesChooser capsChooser;

   private volatile Rectangle clientArea;
   private volatile GLDrawableImpl drawable; // volatile: avoid locking for read-only access
   private volatile GLContextImpl context; // volatile: avoid locking for read-only access

   /* Native window surface */
   private final boolean useX11GTK;
   private volatile long gdkWindow; // either GDK child window ..
   private volatile long x11Window; // .. or X11 child window (for GL rendering)
   private final AbstractGraphicsScreen screen;

   /* Construction parameters stored for GLAutoDrawable accessor methods */
   private int additionalCtxCreationFlags = 0;


   /* Flag indicating whether an unprocessed reshape is pending. */
   private volatile boolean sendReshape; // volatile: maybe written by WindowManager thread w/o locking

   private static String getThreadName() { return Thread.currentThread().getName(); }
   private static String toHexString(final int v) { return "0x"+Integer.toHexString(v); }
   private static String toHexString(final long v) { return "0x"+Long.toHexString(v); }

   /*
    * Invokes init(...) on all GLEventListeners. Assumes context is current when run.
    */
   private final Runnable initAction = new Runnable() {
      @Override
      public void run() {
         helper.init(GLCanvas.this, !sendReshape);
      }
   };

   /*
    * Action to handle display in OpenGL, also processes reshape since they should be done at the same time.
    *
    * Assumes GLContext is current when run.
    */
   private final Runnable displayAction = new Runnable() {
      @Override
      public void run() {
         if (sendReshape) {
            helper.reshape(GLCanvas.this, 0, 0, clientArea.width, clientArea.height);
            sendReshape = false;
         }
         helper.display(GLCanvas.this);
      }
   };

   /* Action to make specified context current prior to running displayAction */
   private final Runnable makeCurrentAndDisplayOnGLAction = new Runnable() {
      @Override
      public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {
            if( !GLCanvas.this.isDisposed() ) {
                helper.invokeGL(drawable, context, displayAction, initAction);
            }
        } finally {
            _lock.unlock();
        }
      }
   };

   /* Swaps buffers, assuming the GLContext is current */
   private final Runnable swapBuffersOnGLAction = new Runnable() {
      @Override
      public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {
            final boolean drawableOK = null != drawable && drawable.isRealized();
            if( drawableOK && !GLCanvas.this.isDisposed() ) {
                drawable.swapBuffers();
            }
        } finally {
            _lock.unlock();
        }
      }
   };

   /*
    * Disposes of OpenGL resources
    */
   private final Runnable disposeOnEDTGLAction = new Runnable() {
      @Override
      public void run() {
         final RecursiveLock _lock = lock;
         _lock.lock();
         try {
             final GLAnimatorControl animator = getAnimator();
             final boolean animatorPaused;
             if(null!=animator) {
                 // can't remove us from animator for recreational addNotify()
                 animatorPaused = animator.pause();
             } else {
                 animatorPaused = false;
             }

             GLException exceptionOnDisposeGL = null;
             if( null != context ) {
                 if( context.isCreated() ) {
                     try {
                         if( !GLCanvas.this.isDisposed() ) {
                             helper.disposeGL(GLCanvas.this, context, true);
                         } else {
                             context.destroy();
                         }
                     } catch (final GLException gle) {
                         exceptionOnDisposeGL = gle;
                     }
                 }
                 context = null;
             }

             Throwable exceptionOnUnrealize = null;
             if( null != drawable ) {
                 try {
                     drawable.setRealized(false);
                 } catch( final Throwable re ) {
                     exceptionOnUnrealize = re;
                 }
                 drawable = null;
             }

             Throwable exceptionOnDeviceClose = null;
             try {
                 if( 0 != x11Window) {
                     SWTAccessor.destroyX11Window(screen.getDevice(), x11Window);
                     x11Window = 0;
                 } else if( 0 != gdkWindow) {
                     SWTAccessor.destroyGDKWindow(gdkWindow);
                     gdkWindow = 0;
                 }
                 screen.getDevice().close();
             } catch (final Throwable re) {
                 exceptionOnDeviceClose = re;
             }

             if (animatorPaused) {
                 animator.resume();
             }

             // throw exception in order of occurrence ..
             if( null != exceptionOnDisposeGL ) {
                 throw exceptionOnDisposeGL;
             }
             if( null != exceptionOnUnrealize ) {
                 throw GLException.newGLException(exceptionOnUnrealize);
             }
             if( null != exceptionOnDeviceClose ) {
                 throw GLException.newGLException(exceptionOnDeviceClose);
             }
         } finally {
             _lock.unlock();
         }
      }
   };

   private class DisposeGLEventListenerAction implements Runnable {
       private GLEventListener listener;
       private final boolean remove;
       private DisposeGLEventListenerAction(final GLEventListener listener, final boolean remove) {
           this.listener = listener;
           this.remove = remove;
       }

       @Override
       public void run() {
           final RecursiveLock _lock = lock;
           _lock.lock();
           try {
               if( !GLCanvas.this.isDisposed() ) {
                   listener = helper.disposeGLEventListener(GLCanvas.this, drawable, context, listener, remove);
               }
           } finally {
               _lock.unlock();
           }
       }
   };

   /**
    * Creates an instance using {@link #GLCanvas(Composite, int, GLCapabilitiesImmutable, GLCapabilitiesChooser)}
    * on the SWT thread.
    *
    * @param parent
    *           Required (non-null) parent Composite.
    * @param style
    *           Optional SWT style bit-field. The {@link SWT#NO_BACKGROUND} bit is set before passing this up to the
    *           Canvas constructor, so OpenGL handles the background.
    * @param caps
    *           Optional GLCapabilities. If not provided, the default capabilities for the default GLProfile for the
    *           graphics device determined by the parent Composite are used. Note that the GLCapabilities that are
    *           actually used may differ based on the capabilities of the graphics device.
    * @param chooser
    *           Optional GLCapabilitiesChooser to customize the selection of the used GLCapabilities based on the
    *           requested GLCapabilities, and the available capabilities of the graphics device.
    * @return a new instance
    */
   public static GLCanvas create(final Composite parent, final int style, final GLCapabilitiesImmutable caps,
                                 final GLCapabilitiesChooser chooser) {
       final GLCanvas[] res = new GLCanvas[] { null };
       parent.getDisplay().syncExec(new Runnable() {
           @Override
           public void run() {
               res[0] = new GLCanvas( parent, style, caps, chooser );
           }
       });
       return res[0];
   }

   /**
    * Creates a new SWT GLCanvas.
    *
    * @param parent
    *           Required (non-null) parent Composite.
    * @param style
    *           Optional SWT style bit-field. The {@link SWT#NO_BACKGROUND} bit is set before passing this up to the
    *           Canvas constructor, so OpenGL handles the background.
    * @param capsReqUser
    *           Optional GLCapabilities. If not provided, the default capabilities for the default GLProfile for the
    *           graphics device determined by the parent Composite are used. Note that the GLCapabilities that are
    *           actually used may differ based on the capabilities of the graphics device.
    * @param capsChooser
    *           Optional GLCapabilitiesChooser to customize the selection of the used GLCapabilities based on the
    *           requested GLCapabilities, and the available capabilities of the graphics device.
    */
   public GLCanvas(final Composite parent, final int style, final GLCapabilitiesImmutable capsReqUser,
                   final GLCapabilitiesChooser capsChooser) {
      /* NO_BACKGROUND required to avoid clearing bg in native SWT widget (we do this in the GL display) */
      super(parent, style | SWT.NO_BACKGROUND);

      GLProfile.initSingleton(); // ensure JOGL is completly initialized

      SWTAccessor.setRealized(this, true);

      clientArea = GLCanvas.this.getClientArea();

      /* Get the nativewindow-Graphics Device associated with this control (which is determined by the parent Composite).
       * Note: SWT is owner of the native handle, hence closing operation will be a NOP. */
      final AbstractGraphicsDevice swtDevice = SWTAccessor.getDevice(this);

      useX11GTK = SWTAccessor.useX11GTK();
      if(useX11GTK) {
          // Decoupled X11 Device/Screen allowing X11 display lock-free off-thread rendering
          final long x11DeviceHandle = X11Util.openDisplay(swtDevice.getConnection());
          if( 0 == x11DeviceHandle ) {
              throw new RuntimeException("Error creating display(EDT): "+swtDevice.getConnection());
          }
          final AbstractGraphicsDevice x11Device = new X11GraphicsDevice(x11DeviceHandle, AbstractGraphicsDevice.DEFAULT_UNIT, true /* owner */);
          screen = SWTAccessor.getScreen(x11Device, -1 /* default */);
      } else {
          screen = SWTAccessor.getScreen(swtDevice, -1 /* default */);
      }

      /* Select default GLCapabilities if none was provided, otherwise use cloned provided caps */
      if(null == capsReqUser) {
          this.capsRequested = new GLCapabilities(GLProfile.getDefault(screen.getDevice()));
      } else {
          this.capsRequested = (GLCapabilitiesImmutable) capsReqUser.cloneMutable();
      }
      this.capsChooser = capsChooser;

      // post create .. when ready
      gdkWindow = 0;
      x11Window = 0;
      drawable = null;
      context = null;

      final Listener listener = new Listener () {
          @Override
          public void handleEvent (final Event event) {
              switch (event.type) {
              case SWT.Paint:
                  displayIfNoAnimatorNoCheck();
                  break;
              case SWT.Resize:
                  updateSizeCheck();
                  break;
              case SWT.Dispose:
                  GLCanvas.this.dispose();
                  break;
              }
          }
      };
      addListener (SWT.Resize, listener);
      addListener (SWT.Paint, listener);
      addListener (SWT.Dispose, listener);
   }

   @Override
   public final void setSharedContext(final GLContext sharedContext) throws IllegalStateException {
       helper.setSharedContext(this.context, sharedContext);
   }

   @Override
   public final void setSharedAutoDrawable(final GLAutoDrawable sharedAutoDrawable) throws IllegalStateException {
       helper.setSharedAutoDrawable(this, sharedAutoDrawable);
   }

   private final UpstreamSurfaceHook swtCanvasUpStreamHook = new UpstreamSurfaceHook() {
       @Override
       public final void create(final ProxySurface s) { /* nop */ }

       @Override
       public final void destroy(final ProxySurface s) { /* nop */ }

       @Override
       public final int getSurfaceWidth(final ProxySurface s) {
           return clientArea.width;
       }

       @Override
       public final int getSurfaceHeight(final ProxySurface s) {
           return clientArea.height;
       }

       @Override
       public String toString() {
           return "SWTCanvasUpstreamSurfaceHook[upstream: "+GLCanvas.this.toString()+", "+clientArea.width+"x"+clientArea.height+"]";
       }

       /**
        * {@inheritDoc}
        * <p>
        * Returns <code>null</code>.
        * </p>
        */
       @Override
       public final NativeSurface getUpstreamSurface() {
           return null;
       }
   };

   protected final void updateSizeCheck() {
      final Rectangle oClientArea = clientArea;
      final Rectangle nClientArea = GLCanvas.this.getClientArea();
      if ( nClientArea != null &&
           ( nClientArea.width != oClientArea.width || nClientArea.height != oClientArea.height )
         ) {
          clientArea = nClientArea; // write back new value

          final GLDrawableImpl _drawable = drawable;
          final boolean drawableOK = null != _drawable && _drawable.isRealized();
          if(DEBUG) {
              final long dh = drawableOK ? _drawable.getHandle() : 0;
              System.err.println(getThreadName()+": GLCanvas.sizeChanged: ("+Thread.currentThread().getName()+"): "+nClientArea.x+"/"+nClientArea.y+" "+nClientArea.width+"x"+nClientArea.height+" - drawableHandle "+toHexString(dh));
          }
          if( drawableOK ) {
              if( ! _drawable.getChosenGLCapabilities().isOnscreen() ) {
                  final RecursiveLock _lock = lock;
                  _lock.lock();
                  try {
                      final GLDrawableImpl _drawableNew = GLDrawableHelper.resizeOffscreenDrawable(_drawable, context, nClientArea.width, nClientArea.height);
                      if(_drawable != _drawableNew) {
                          // write back
                          drawable = _drawableNew;
                      }
                  } finally {
                      _lock.unlock();
                  }
              }
          }
          if(0 != x11Window) {
              SWTAccessor.resizeX11Window(screen.getDevice(), clientArea, x11Window);
          } else if(0 != gdkWindow) {
              SWTAccessor.resizeGDKWindow(clientArea, gdkWindow);
          }
          sendReshape = true; // async if display() doesn't get called below, but avoiding deadlock
      }
   }

   private boolean isValidAndVisibleOnEDTActionResult;
   private final Runnable isValidAndVisibleOnEDTAction = new Runnable() {
       @Override
       public void run() {
           isValidAndVisibleOnEDTActionResult = !GLCanvas.this.isDisposed() && GLCanvas.this.isVisible();
       } };

   private final boolean isValidAndVisibleOnEDT() {
       synchronized(isValidAndVisibleOnEDTAction) {
           runOnEDTIfAvail(true, isValidAndVisibleOnEDTAction);
           return isValidAndVisibleOnEDTActionResult;
       }
   }

   /** assumes drawable == null (implying !drawable.isRealized()) !  Checks of !isDispose() and isVisible() */
   protected final boolean validateDrawableAndContextWithCheck() {
      if( !isValidAndVisibleOnEDT() ) {
          return false;
      }
      return validateDrawableAndContextPostCheck();
   }

   private final boolean isDrawableAndContextValid() {
       // drawable != null implies drawable.isRealized()==true
       return null != drawable && null != context;
   }

   /** assumes drawable == null (implying !drawable.isRealized()) || context == null ! No check of !isDispose() and isVisible() */
   private final boolean validateDrawableAndContextPostCheck() {
      boolean res;
      final RecursiveLock _lock = lock;
      _lock.lock();
      try {
          if(null == drawable) {
              // 'displayable' (isValidAndVisibleOnEDT()) must have been checked upfront if appropriate!
              createDrawableImpl(); // checks clientArea size (i.e. drawable size) and perf. realization
          }
          final GLDrawable _drawable = drawable;
          if ( null != _drawable ) {
              // drawable realization goes in-hand w/ it's construction
              if( null == context ) {
                  // re-try context creation
                  res = createContextImpl(_drawable); // pending creation.
              } else {
                  res = true;
              }
              if(res) {
                  sendReshape = true;
              }
          } else {
              if(DEBUG) {
                  System.err.println(getThreadName()+": SWT.GLCanvas.validate "+toHexString(hashCode())+": null drawable");
              }
              res = false;
          }
          if(DEBUG) {
              System.err.println(getThreadName()+": SWT.GLCanvas.validate.X  "+toHexString(hashCode())+": "+res+", drawable-realized "+drawable.isRealized()+", has context "+(null!=context));
          }
      } finally {
          _lock.unlock();
      }
      return res;
   }
   private final void createDrawableImpl() {
       final Rectangle nClientArea = clientArea;
       if(0 >= nClientArea.width || 0 >= nClientArea.height) {
          if(DEBUG) {
              System.err.println(getThreadName()+": SWT.GLCanvas.validate.X "+toHexString(hashCode())+": drawable could not be created: size < 0x0");
          }
          return; // early out
       }
       final AbstractGraphicsDevice device = screen.getDevice();
       device.open();

       final long nativeWindowHandle;
       if( useX11GTK ) {
           final GraphicsConfigurationFactory factory = GraphicsConfigurationFactory.getFactory(device, capsRequested);
           final AbstractGraphicsConfiguration cfg = factory.chooseGraphicsConfiguration(
                   capsRequested, capsRequested, capsChooser, screen, VisualIDHolder.VID_UNDEFINED);
           if(DEBUG) {
               System.err.println(getThreadName()+": SWT.GLCanvas.X11 "+toHexString(hashCode())+": factory: "+factory+", chosen config: "+cfg);
           }
           if (null == cfg) {
               throw new NativeWindowException("Error choosing GraphicsConfiguration creating window: "+this);
           }
           final int visualID = cfg.getVisualID(VIDType.NATIVE);
           if( VisualIDHolder.VID_UNDEFINED != visualID ) {
               // gdkWindow = SWTAccessor.createCompatibleGDKChildWindow(this, visualID, clientArea.width, clientArea.height);
               // nativeWindowHandle = SWTAccessor.gdk_window_get_xwindow(gdkWindow);
               x11Window = SWTAccessor.createCompatibleX11ChildWindow(screen, this, visualID, clientArea.width, clientArea.height);
               nativeWindowHandle = x11Window;
           } else {
              throw new GLException("Could not choose valid visualID: "+toHexString(visualID)+", "+this);
           }
       } else {
           nativeWindowHandle = SWTAccessor.getWindowHandle(this);
       }
       final GLDrawableFactory glFactory = GLDrawableFactory.getFactory(capsRequested.getGLProfile());

       // Create a NativeWindow proxy for the SWT canvas
       final ProxySurface proxySurface = glFactory.createProxySurface(device, screen.getIndex(), nativeWindowHandle,
                                                                capsRequested, capsChooser, swtCanvasUpStreamHook);
       // Associate a GL surface with the proxy
       final GLDrawableImpl _drawable = (GLDrawableImpl) glFactory.createGLDrawable(proxySurface);
       _drawable.setRealized(true);
       if(!_drawable.isRealized()) {
           // oops
           if(DEBUG) {
               System.err.println(getThreadName()+": SWT.GLCanvas.validate.X "+toHexString(hashCode())+": Drawable could not be realized: "+_drawable);
           }
       } else {
           if(DEBUG) {
               System.err.println(getThreadName()+": SWT.GLCanvas.validate "+toHexString(hashCode())+": Drawable created and realized");
           }
           drawable = _drawable;
       }
   }
   private boolean createContextImpl(final GLDrawable drawable) {
       final GLContext[] shareWith = { null };
       if( !helper.isSharedGLContextPending(shareWith) ) {
           context = (GLContextImpl) drawable.createContext(shareWith[0]);
           context.setContextCreationFlags(additionalCtxCreationFlags);
           if(DEBUG) {
               System.err.println(getThreadName()+": SWT.GLCanvas.validate "+toHexString(hashCode())+": Context created: has shared "+(null != shareWith[0]));
           }
           return true;
       } else {
           if(DEBUG) {
               System.err.println(getThreadName()+": SWT.GLCanvas.validate "+toHexString(hashCode())+": Context !created: pending share");
           }
           return false;
       }
   }

   @Override
   public void update() {
       // don't paint background etc .. nop avoids flickering
       // super.update();
   }

   /**
   @Override
   public boolean forceFocus() {
       final boolean r = super.forceFocus();
       if(r && 0 != gdkWindow) {
           SWTGTKUtil.focusGDKWindow(gdkWindow);
       }
       return r;
   } */

   @Override
   public void dispose() {
     runInGLThread(disposeOnEDTGLAction);
     super.dispose();
   }

   private final void displayIfNoAnimatorNoCheck() {
       if ( !helper.isAnimatorAnimatingOnOtherThread() ) {
           if( isDrawableAndContextValid() || validateDrawableAndContextPostCheck() ) {
               runInGLThread(makeCurrentAndDisplayOnGLAction);
           }
       }
   }

   //
   // GL[Auto]Drawable
   //

   @Override
   public void display() {
      if( isDrawableAndContextValid() || validateDrawableAndContextWithCheck() ) {
          runInGLThread(makeCurrentAndDisplayOnGLAction);
      }
   }

   @Override
   public final Object getUpstreamWidget() {
       return this;
   }

   @Override
   public final RecursiveLock getUpstreamLock() { return lock; }

   @Override
   public int getSurfaceWidth() {
      return clientArea.width;
   }

   @Override
   public int getSurfaceHeight() {
      return clientArea.height;
   }

   @Override
   public boolean isGLOriented() {
      final GLDrawable _drawable = drawable;
      return null != _drawable ? _drawable.isGLOriented() : true;
   }

   @Override
   public void addGLEventListener(final GLEventListener listener) {
      helper.addGLEventListener(listener);
   }

   @Override
   public void addGLEventListener(final int idx, final GLEventListener listener) throws IndexOutOfBoundsException {
      helper.addGLEventListener(idx, listener);
   }

   @Override
   public int getGLEventListenerCount() {
      return helper.getGLEventListenerCount();
   }

   @Override
   public GLEventListener getGLEventListener(final int index) throws IndexOutOfBoundsException {
      return helper.getGLEventListener(index);
   }

   @Override
   public boolean areAllGLEventListenerInitialized() {
      return helper.areAllGLEventListenerInitialized();
   }

   @Override
   public boolean getGLEventListenerInitState(final GLEventListener listener) {
       return helper.getGLEventListenerInitState(listener);
   }

   @Override
   public void setGLEventListenerInitState(final GLEventListener listener, final boolean initialized) {
       helper.setGLEventListenerInitState(listener, initialized);
   }

   @Override
   public GLEventListener disposeGLEventListener(final GLEventListener listener, final boolean remove) {
       final DisposeGLEventListenerAction r = new DisposeGLEventListenerAction(listener, remove);
       runInGLThread(r);
       return r.listener;
   }

   @Override
   public GLEventListener removeGLEventListener(final GLEventListener listener) {
      return helper.removeGLEventListener(listener);
   }

   /**
    * {@inheritDoc}
    *
    * <p>
    * This impl. calls this class's {@link #dispose()} SWT override,
    * where the actual implementation resides.
    * </p>
    */
   @Override
   public void destroy() {
      dispose();
   }

   @Override
   public GLAnimatorControl getAnimator() {
      return helper.getAnimator();
   }

   @Override
   public final Thread setExclusiveContextThread(final Thread t) throws GLException {
       return helper.setExclusiveContextThread(t, context);
   }

   @Override
   public final Thread getExclusiveContextThread() {
       return helper.getExclusiveContextThread();
   }

   @Override
   public boolean getAutoSwapBufferMode() {
      return helper.getAutoSwapBufferMode();
   }

   @Override
   public final GLDrawable getDelegatedDrawable() {
      return drawable;
   }

   @Override
   public GLContext getContext() {
      return context;
   }

   @Override
   public int getContextCreationFlags() {
      return additionalCtxCreationFlags;
   }

   @Override
   public GL getGL() {
      final GLContext _context = context;
      return (null == _context) ? null : _context.getGL();
   }

   @Override
   public boolean invoke(final boolean wait, final GLRunnable runnable) throws IllegalStateException {
      return helper.invoke(this, wait, runnable);
   }

   @Override
   public boolean invoke(final boolean wait, final List<GLRunnable> runnables) throws IllegalStateException {
      return helper.invoke(this, wait, runnables);
   }

   @Override
   public void flushGLRunnables() {
       helper.flushGLRunnables();
   }

   @Override
   public void setAnimator(final GLAnimatorControl arg0) throws GLException {
      helper.setAnimator(arg0);
   }

   @Override
   public void setAutoSwapBufferMode(final boolean arg0) {
      helper.setAutoSwapBufferMode(arg0);
   }

   @Override
   public GLContext setContext(final GLContext newCtx, final boolean destroyPrevCtx) {
      final RecursiveLock _lock = lock;
      _lock.lock();
      try {
          final GLContext oldCtx = context;
          GLDrawableHelper.switchContext(drawable, oldCtx, destroyPrevCtx, newCtx, additionalCtxCreationFlags);
          context=(GLContextImpl)newCtx;
          return oldCtx;
      } finally {
          _lock.unlock();
      }
   }

   @Override
   public void setContextCreationFlags(final int arg0) {
      additionalCtxCreationFlags = arg0;
      final GLContext _context = context;
      if(null != _context) {
        _context.setContextCreationFlags(additionalCtxCreationFlags);
      }
   }

   @Override
   public GL setGL(final GL arg0) {
       final GLContext _context = context;
      if (null != _context) {
         _context.setGL(arg0);
         return arg0;
      }
      return null;
   }

   @Override
   public GLContext createContext(final GLContext shareWith) {
     final RecursiveLock _lock = lock;
     _lock.lock();
     try {
         if(drawable != null) {
             final GLContext _ctx = drawable.createContext(shareWith);
             _ctx.setContextCreationFlags(additionalCtxCreationFlags);
             return _ctx;
         }
         return null;
     } finally {
         _lock.unlock();
     }
   }

   @Override
   public GLCapabilitiesImmutable getChosenGLCapabilities() {
      final GLDrawable _drawable = drawable;
      return null != _drawable ? (GLCapabilitiesImmutable)_drawable.getChosenGLCapabilities() : null;
   }

   @Override
   public GLCapabilitiesImmutable getRequestedGLCapabilities() {
      final GLDrawable _drawable = drawable;
      return null != _drawable ? (GLCapabilitiesImmutable)_drawable.getRequestedGLCapabilities() : null;
   }

   @Override
   public GLDrawableFactory getFactory() {
      final GLDrawable _drawable = drawable;
      return (_drawable != null) ? _drawable.getFactory() : null;
   }

   @Override
   public GLProfile getGLProfile() {
      return capsRequested.getGLProfile();
   }

   @Override
   public long getHandle() {
      final GLDrawable _drawable = drawable;
      return (_drawable != null) ? _drawable.getHandle() : 0;
   }

   @Override
   public NativeSurface getNativeSurface() {
      final GLDrawable _drawable = drawable;
      return (_drawable != null) ? _drawable.getNativeSurface() : null;
   }

   @Override
   public boolean isRealized() {
      final GLDrawable _drawable = drawable;
      return (_drawable != null) ? _drawable.isRealized() : false;
   }

   @Override
   public void setRealized(final boolean arg0) {
      /* Intentionally empty */
   }

   @Override
   public void swapBuffers() throws GLException {
      runInGLThread(swapBuffersOnGLAction);
   }

   /**
    * {@inheritDoc}
    * <p>
    * Implementation always supports multithreading, hence method always returns <code>true</code>.
    * </p>
    */
   @Override
   public final boolean isThreadGLCapable() { return true; }

   /**
    * Runs the specified action in an SWT compatible thread, which is:
    * <ul>
    *   <li>Mac OSX
    *   <ul>
    *     <!--li>AWT EDT: In case AWT is available, the AWT EDT is the OSX UI main thread</li-->
    *     <!--li><i>Main Thread</i>: Run on OSX UI main thread.</li-->
    *     <li>Current thread</li>
    *   </ul></li>
    *   <li>Linux, Windows, ..
    *   <ul>
    *     <!--li>Use {@link Threading#invokeOnOpenGLThread(boolean, Runnable)}</li-->
    *     <li>Current thread</li>
    *   </ul></li>
    * </ul>
    * The current thread seems to be valid for all platforms,
    * since no SWT lifecycle tasks are being performed w/ this call.
    * Only GL task, which are independent from the SWT threading model.
    *
    * @see Platform#AWT_AVAILABLE
    * @see Platform#getOSType()
    */
   private void runInGLThread(final Runnable action) {
      /**
      if(Platform.OSType.MACOS == Platform.OS_TYPE) {
          SWTAccessor.invoke(true, action);
      } else {
          Threading.invokeOnOpenGLThread(true, action);
      } */
      /**
      if( !isDisposed() ) {
          final Display d = getDisplay();
          if( d.getThread() == Thread.currentThread() ) {
              action.run();
          } else {
              d.syncExec(action);
          }
      } */
      action.run();
   }

   private void runOnEDTIfAvail(final boolean wait, final Runnable action) {
       final Display d = isDisposed() ? null : getDisplay();
       if( null == d || d.isDisposed() || d.getThread() == Thread.currentThread() ) {
           action.run();
       } else if(wait) {
           d.syncExec(action);
       } else {
           d.asyncExec(action);
       }
   }

   @Override
   public String toString() {
       final GLDrawable _drawable = drawable;
       final int dw = (null!=_drawable) ? _drawable.getSurfaceWidth() : -1;
       final int dh = (null!=_drawable) ? _drawable.getSurfaceHeight() : -1;

       return "SWT-GLCanvas[Realized "+isRealized()+
               ",\n\t"+((null!=_drawable)?_drawable.getClass().getName():"null-drawable")+
               ",\n\tFactory   "+getFactory()+
               ",\n\thandle    "+toHexString(getHandle())+
               ",\n\tDrawable size "+dw+"x"+dh+
               ",\n\tSWT size "+getSurfaceWidth()+"x"+getSurfaceHeight()+"]";
   }

   public static void main(final String[] args) {
       System.err.println(VersionUtil.getPlatformInfo());
       System.err.println(GlueGenVersion.getInstance());
       // System.err.println(NativeWindowVersion.getInstance());
       System.err.println(JoglVersion.getInstance());

       System.err.println(JoglVersion.getDefaultOpenGLInfo(null, null, true).toString());

       final GLCapabilitiesImmutable caps = new GLCapabilities( GLProfile.getDefault(GLProfile.getDefaultDevice()) );
       final Display display = new Display();
       final Shell shell = new Shell(display);
       shell.setSize(128,128);
       shell.setLayout(new FillLayout());

       final GLCanvas canvas = new GLCanvas(shell, 0, caps, null);

       canvas.addGLEventListener(new GLEventListener() {
           @Override
           public void init(final GLAutoDrawable drawable) {
               final GL gl = drawable.getGL();
               System.err.println(JoglVersion.getGLInfo(gl, null));
           }
           @Override
           public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
           @Override
           public void display(final GLAutoDrawable drawable) {}
           @Override
           public void dispose(final GLAutoDrawable drawable) {}
       });
       shell.open();
       canvas.display();
       display.dispose();
   }
}
