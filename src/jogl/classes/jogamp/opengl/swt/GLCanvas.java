/**
 * 
 */
package jogamp.opengl.swt;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.opengl.GL;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;
import javax.media.opengl.Threading;

import jogamp.nativewindow.swt.SWTAccessor;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.ThreadingImpl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;

/**
 *
 */
public class GLCanvas extends Canvas implements GLAutoDrawable {

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
   private final GLDrawableHelper drawableHelper = new GLDrawableHelper();
   private GLDrawable drawable;
   private GLContext context;

   /* Native window surface */
   private AbstractGraphicsDevice device;
   private final long nativeWindowHandle;
   private final ProxySurface proxySurface;

   /* Construction parameters stored for GLAutoDrawable accessor methods */
   private int ctxCreationFlags = 0;
   
//   private final AbstractGraphicsConfiguration graphicsConfiguration;
   private final GLCapabilitiesImmutable glCapsRequested;

   /*
    * Lock for access to GLDrawable, as used in GLCanvas,
    */
   private final RecursiveLock lock = LockFactory.createRecursiveLock();

   /* Flag indicating whether an unprocessed reshape is pending. */
   private volatile boolean sendReshape;

   /*
    * Invokes init(...) on all GLEventListeners. Assumes context is current when run.
    */
   private final Runnable initAction = new Runnable() {
      @Override
      public void run() {
         drawableHelper.init(GLCanvas.this);
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
            drawableHelper.reshape(GLCanvas.this, 0, 0, getWidth(), getHeight());
            sendReshape = false;
         }
         drawableHelper.display(GLCanvas.this);
      }
   };

   /* Action to make specified context current prior to running displayAction */
   private final Runnable makeCurrentAndDisplayAction = new Runnable() {
      @Override
      public void run() {
         drawableHelper.invokeGL(drawable, context, displayAction, initAction);
      }
   };

   /* Swaps buffers, assuming the GLContext is current */
   private final Runnable swapBuffersAction = new Runnable() {
      @Override
      public void run() {
         drawable.swapBuffers();
      }
   };

   /* Swaps buffers, making the GLContext current first */
   private final Runnable makeCurrentAndSwapBuffersAction = new Runnable() {
      @Override
      public void run() {
         drawableHelper.invokeGL(drawable, context, swapBuffersAction, initAction);
      }
   };

   /*
    * Disposes of OpenGL resources
    */
   private final Runnable disposeGLAction = new Runnable() {
      @Override
      public void run() {
         drawableHelper.dispose(GLCanvas.this);

         if (null != context) {
            context.makeCurrent(); // implicit wait for lock ..
            context.destroy();
            context = null;
         }

         if (null != drawable) {
            drawable.setRealized(false);
            drawable = null;
         }
      }
   };

   private final Runnable makeCurrentAndDisposeGLAction = new Runnable() {
      @Override
      public void run() {
         drawableHelper.invokeGL(drawable, context, disposeGLAction, null);
      }
   };

   private final Runnable disposeGraphicsDeviceAction = new Runnable() {
      @Override
      public void run() {
         if (null != device) {
            device.close();
            device = null;
         }
      }
   };

   /**
    * Creates a new SWT GLCanvas.
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
    * @param shareWith
    *           Optional GLContext to share state (textures, vbos, shaders, etc.) with.
    */
   public GLCanvas(final Composite parent, final int style, final GLCapabilities caps,
         final GLCapabilitiesChooser chooser, final GLContext shareWith) {
      /* NO_BACKGROUND required to avoid clearing bg in native SWT widget (we do this in the GL display) */
      super(parent, style | SWT.NO_BACKGROUND);

      SWTAccessor.setRealized(this, true);

      /* Get the nativewindow-Graphics Device associated with this control (which is determined by the parent Composite) */
      device = SWTAccessor.getDevice(this);
      /* Native handle for the control, used to associate with GLContext */
      nativeWindowHandle = SWTAccessor.getWindowHandle(this);

      /* Select default GLCapabilities if none was provided, otherwise clone provided caps to ensure safety */
      final GLCapabilitiesImmutable fixedCaps = (caps == null) ? new GLCapabilities(GLProfile.getDefault(device))
            : (GLCapabilitiesImmutable) caps.cloneMutable();
      glCapsRequested = fixedCaps;
      
//      this.graphicsConfiguration = chooseGraphicsConfiguration(fixedCaps, fixedCaps, chooser);
      
      final GLDrawableFactory glFactory = GLDrawableFactory.getFactory(fixedCaps.getGLProfile());

      /* Create a NativeWindow proxy for the SWT canvas */
      proxySurface = glFactory.createProxySurface(device, nativeWindowHandle, fixedCaps, null);

      /* Associate a GL surface with the proxy */
      drawable = glFactory.createGLDrawable(proxySurface);
      drawable.setRealized(true);

      context = drawable.createContext(shareWith);

      /* Register SWT listeners (e.g. PaintListener) to render/resize GL surface. */
      /* TODO: verify that these do not need to be manually de-registered when destroying the SWT component */
      addPaintListener(new PaintListener() {

         @Override
         public void paintControl(final PaintEvent arg0) {
            if (!drawableHelper.isExternalAnimatorAnimating()) {
               display();
            }
         }
      });
      addControlListener(new ControlAdapter() {

         @Override
         public void controlResized(final ControlEvent arg0) {
            /* Mark for OpenGL reshape next time the control is painted */
            sendReshape = true;
         }
      });
   }
   
   private AbstractGraphicsConfiguration chooseGraphicsConfiguration(final GLCapabilitiesImmutable capsChosen,
         final GLCapabilitiesImmutable capsRequested,
         final GLCapabilitiesChooser chooser) {
      //FIXME: Need to get platform specific screen implementation...
      //TODO: is this safe to run in any thread?  Probably should be run in SWT thread.
      final AbstractGraphicsScreen screen = null;
      return GraphicsConfigurationFactory.getFactory(device.getClass()).chooseGraphicsConfiguration(capsChosen,
                                                                                                   capsRequested,
                                                                                                   chooser, screen);
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#addGLEventListener(javax.media.opengl.GLEventListener)
    */
   @Override
   public void addGLEventListener(final GLEventListener arg0) {
      drawableHelper.addGLEventListener(arg0);
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#addGLEventListener(int, javax.media.opengl.GLEventListener)
    */
   @Override
   public void addGLEventListener(final int arg0, final GLEventListener arg1) throws IndexOutOfBoundsException {
      drawableHelper.addGLEventListener(arg0, arg1);
   }

   /**
    * {@inheritDoc}
    * <p>
    * Also disposes of the SWT component.
    */
   @Override
   public void destroy() {
      drawable.setRealized(false);
      dispose();
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#display()
    */
   @Override
   public void display() {
      runInGLThread(makeCurrentAndDisplayAction, displayAction);
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#getAnimator()
    */
   @Override
   public GLAnimatorControl getAnimator() {
      return drawableHelper.getAnimator();
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#getAutoSwapBufferMode()
    */
   @Override
   public boolean getAutoSwapBufferMode() {
      return drawableHelper.getAutoSwapBufferMode();
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#getContext()
    */
   @Override
   public GLContext getContext() {
      return context;
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#getContextCreationFlags()
    */
   @Override
   public int getContextCreationFlags() {
      return ctxCreationFlags;
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#getGL()
    */
   @Override
   public GL getGL() {
      final GLContext ctx = getContext();
      return (ctx == null) ? null : ctx.getGL();
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#invoke(boolean, javax.media.opengl.GLRunnable)
    */
   @Override
   public void invoke(final boolean wait, final GLRunnable run) {
      /* Queue task for running during the next display(). */
      drawableHelper.invoke(this, wait, run);
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#removeGLEventListener(javax.media.opengl.GLEventListener)
    */
   @Override
   public void removeGLEventListener(final GLEventListener arg0) {
      drawableHelper.removeGLEventListener(arg0);
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#setAnimator(javax.media.opengl.GLAnimatorControl)
    */
   @Override
   public void setAnimator(final GLAnimatorControl arg0) throws GLException {
      drawableHelper.setAnimator(arg0);
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#setAutoSwapBufferMode(boolean)
    */
   @Override
   public void setAutoSwapBufferMode(final boolean arg0) {
      drawableHelper.setAutoSwapBufferMode(arg0);
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#setContext(javax.media.opengl.GLContext)
    */
   @Override
   public void setContext(final GLContext ctx) {
      this.context = ctx;
      if (ctx instanceof GLContextImpl) {
         ((GLContextImpl) ctx).setContextCreationFlags(ctxCreationFlags);
      }
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#setContextCreationFlags(int)
    */
   @Override
   public void setContextCreationFlags(final int arg0) {
      ctxCreationFlags = arg0;
   }

   /*
    * @see javax.media.opengl.GLAutoDrawable#setGL(javax.media.opengl.GL)
    */
   @Override
   public GL setGL(final GL arg0) {
      final GLContext ctx = getContext();
      if (ctx != null) {
         ctx.setGL(arg0);
         return arg0;
      }
      return null;
   }

   /*
    * @see javax.media.opengl.GLDrawable#createContext(javax.media.opengl.GLContext)
    */
   @Override
   public GLContext createContext(final GLContext arg0) {
      lock.lock();
      try {
         final GLDrawable drawable = this.drawable;
         return (drawable != null) ? drawable.createContext(arg0) : null;
      } finally {
         lock.unlock();
      }
   }

   /*
    * @see javax.media.opengl.GLDrawable#getChosenGLCapabilities()
    */
   @Override
   public GLCapabilitiesImmutable getChosenGLCapabilities() {
      return glCapsRequested;
      /* FIXME: once the chooseGraphicsConfiguration method is correct, then do the following instead: */
//      return (GLCapabilitiesImmutable)graphicsConfiguration.getChosenCapabilities();
   }

   /**
    * Accessor for the GLCapabilities that were requested (via the constructor parameter).
    * 
    * @return Non-null GLCapabilities.
    */
   public GLCapabilitiesImmutable getRequestedGLCapabilities() {
      return glCapsRequested;
      /* FIXME: once the chooseGraphicsConfiguration method is correct, then do the following instead: */
      //return (GLCapabilitiesImmutable)graphicsConfiguration.getRequestedCapabilities();
   }

   /*
    * @see javax.media.opengl.GLDrawable#getFactory()
    */
   @Override
   public GLDrawableFactory getFactory() {
      lock.lock();
      try {
         final GLDrawable drawable = this.drawable;
         return (drawable != null) ? drawable.getFactory() : null;
      } finally {
         lock.unlock();
      }
   }

   /*
    * @see javax.media.opengl.GLDrawable#getGLProfile()
    */
   @Override
   public GLProfile getGLProfile() {
      return glCapsRequested.getGLProfile();
   }

   /*
    * @see javax.media.opengl.GLDrawable#getHandle()
    */
   @Override
   public long getHandle() {
      lock.lock();
      try {
         final GLDrawable drawable = this.drawable;
         return (drawable != null) ? drawable.getHandle() : 0;
      } finally {
         lock.unlock();
      }
   }

   /*
    * @see javax.media.opengl.GLDrawable#getHeight()
    */
   @Override
   public int getHeight() {
      return getClientArea().height;
   }

   /*
    * @see javax.media.opengl.GLDrawable#getNativeSurface()
    */
   @Override
   public NativeSurface getNativeSurface() {
      lock.lock();
      try {
         final GLDrawable drawable = this.drawable;
         return (drawable != null) ? drawable.getNativeSurface() : null;
      } finally {
         lock.unlock();
      }
   }

   /*
    * @see javax.media.opengl.GLDrawable#getWidth()
    */
   @Override
   public int getWidth() {
      return getClientArea().width;
   }

   /*
    * @see javax.media.opengl.GLDrawable#isRealized()
    */
   @Override
   public boolean isRealized() {
      lock.lock();
      try {
         final GLDrawable drawable = this.drawable;
         return (drawable != null) ? drawable.isRealized() : false;
      } finally {
         lock.unlock();
      }
   }

   /*
    * @see javax.media.opengl.GLDrawable#setRealized(boolean)
    */
   @Override
   public void setRealized(final boolean arg0) {
      /* Intentionally empty */
   }

   /*
    * @see javax.media.opengl.GLDrawable#swapBuffers()
    */
   @Override
   public void swapBuffers() throws GLException {
      runInGLThread(makeCurrentAndSwapBuffersAction, swapBuffersAction);
   }

   /*
    * @see mil.afrl.rrs.ifsb.jview.graph.graph3d.RenderSurface#update()
    */
   @Override
   public void update() {
//      display();
   }

   /*
    * @see mil.afrl.rrs.ifsb.jview.graph.graph3d.RenderSurface#dispose()
    */
   @Override
   public void dispose() {
      lock.lock();
      try {
         final Display display = getDisplay();

         if (null != context) {
            boolean animatorPaused = false;
            final GLAnimatorControl animator = getAnimator();
            if (null != animator) {
               // can't remove us from animator for recreational addNotify()
               animatorPaused = animator.pause();
            }
            if (Threading.isSingleThreaded() && !Threading.isOpenGLThread()) {
               runInDesignatedGLThread(makeCurrentAndDisposeGLAction);
            } else if (context.isCreated()) {
               drawableHelper.invokeGL(drawable, context, disposeGLAction, null);
            }

            if (animatorPaused) {
               animator.resume();
            }
         }
         if (display.getThread() == Thread.currentThread())
            disposeGraphicsDeviceAction.run();
         else
            display.syncExec(disposeGraphicsDeviceAction);
      } finally {
         lock.unlock();
      }
      super.dispose();
   }

   /**
    * Determines whether the current thread is the appropriate thread to use the GLContext in. If we are using one of
    * the single-threaded policies in {@link Threading}, than this is either the SWT event dispatch thread, or the
    * OpenGL worker thread depending on the state of {@link #useSWTThread}. Otherwise this always returns true because
    * the threading model is user defined.
    * <p>
    * TODO: should this be moved to {@link Threading}?
    * 
    * @return true if the calling thread is the correct thread to execute OpenGL calls in, false otherwise.
    */
   protected boolean isRenderThread() {
      if (Threading.isSingleThreaded()) {
         if (ThreadingImpl.getMode() != ThreadingImpl.WORKER) {
            final Display display = getDisplay();
            return display != null && display.getThread() == Thread.currentThread();
         }
         return Threading.isOpenGLThread();
      }
      /*
       * For multi-threaded rendering, the render thread is not defined...
       */
      return true;
   }

   /**
    * Runs the specified action in the designated OpenGL thread. If the current thread is designated, then the
    * syncAction is run synchronously, otherwise the asyncAction is dispatched to the appropriate worker thread.
    * 
    * @param asyncAction
    *           The non-null action to dispatch to an OpenGL worker thread. This action should not assume that a
    *           GLContext is current when invoked.
    * @param syncAction
    *           The non-null action to run synchronously if the current thread is designated to handle OpenGL calls.
    *           This action may assume the GLContext is current.
    */
   private void runInGLThread(final Runnable asyncAction, final Runnable syncAction) {
      if (Threading.isSingleThreaded() && !isRenderThread()) {
         /* Run in designated GL thread */
         runInDesignatedGLThread(asyncAction);
      } else {
         /* Run in current thread... */
         drawableHelper.invokeGL(drawable, context, syncAction, initAction);
      }
   }

   /**
    * Dispatches the specified runnable to the appropriate OpenGL worker thread (either the SWT event dispatch thread,
    * or the OpenGL worker thread depending on the state of {@link #useSWTThread}).
    * 
    * @param makeCurrentAndRunAction
    *           The non-null action to dispatch.
    */
   private void runInDesignatedGLThread(final Runnable makeCurrentAndRunAction) {
      if (ThreadingImpl.getMode() != ThreadingImpl.WORKER) {
         final Display display = getDisplay();
         assert display.getThread() != Thread.currentThread() : "Incorrect use of thread dispatching.";
         display.syncExec(makeCurrentAndRunAction);
      } else {
         Threading.invokeOnOpenGLThread(makeCurrentAndRunAction);
      }
   }

   
   public static void main(final String[] args) {
      GLProfile.initSingleton(true);
      final Display display = new Display();
      final Shell shell = new Shell(display);
      shell.setSize(800,600);
      shell.setLayout(new FillLayout());

      final GLCanvas canvas = new GLCanvas(shell,
            0, null, null, null);

      canvas.addGLEventListener(new GLEventListener() {
         
         @Override
         public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
            System.out.println("Reshape");
         }
         
         @Override
         public void init(final GLAutoDrawable drawable) {
            System.out.println("Init");
         }
         
         @Override
         public void dispose(final GLAutoDrawable drawable) {
            System.out.println("Dispose");
         }
         
         @Override
         public void display(final GLAutoDrawable drawable) {
            System.out.println("Display");
         }
      });
      shell.setSize(500, 500);
      shell.open();
      while (!shell.isDisposed()) {
         if (!display.readAndDispatch())
            display.sleep();
      }
      display.dispose();
   }
}
