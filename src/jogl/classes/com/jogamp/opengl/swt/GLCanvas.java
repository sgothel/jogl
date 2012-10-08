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

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
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

import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.GLDrawableImpl;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.opengl.JoglVersion;

/**
 * Native SWT Canvas implementing GLAutoDrawable
 * 
 * <p>Note: To employ custom GLCapabilities, NewtCanvasSWT shall be used instead.</p>
 * 
 */
public class GLCanvas extends Canvas implements GLAutoDrawable {
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
   
   private final GLContext shareWith;
   private final GLCapabilitiesImmutable capsRequested;
   private final GLCapabilitiesChooser capsChooser; 
   
   private volatile GLDrawableImpl drawable; // volatile: avoid locking for read-only access
   private GLContextImpl context;

   /* Native window surface */
   private AbstractGraphicsDevice device;

   /* Construction parameters stored for GLAutoDrawable accessor methods */
   private int additionalCtxCreationFlags = 0;


   /* Flag indicating whether an unprocessed reshape is pending. */
   private volatile boolean sendReshape; // volatile: maybe written by WindowManager thread w/o locking

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
            helper.reshape(GLCanvas.this, 0, 0, getWidth(), getHeight());
            sendReshape = false;
         }
         helper.display(GLCanvas.this);
      }
   };

   /* Action to make specified context current prior to running displayAction */
   private final Runnable makeCurrentAndDisplayOnEDTAction = new Runnable() {
      @Override
      public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {            
            helper.invokeGL(drawable, context, displayAction, initAction);
        } finally {
            _lock.unlock();
        }
      }
   };

   /* Swaps buffers, assuming the GLContext is current */
   private final Runnable swapBuffersOnEDTAction = new Runnable() {
      @Override
      public void run() {
        final RecursiveLock _lock = lock;
        _lock.lock();
        try {
            if(null != drawable) {
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
   private final Runnable postDisposeGLAction = new Runnable() {
      @Override
      public void run() {
         context = null;
         if (null != drawable) {
            drawable.setRealized(false);
            drawable = null;
         }
      }
   };

   private final Runnable disposeOnEDTGLAction = new Runnable() {
      @Override
      public void run() {
         final RecursiveLock _lock = lock;
         _lock.lock();
         try {
             if (null != drawable && null != context) {
                boolean animatorPaused = false;
                final GLAnimatorControl animator = getAnimator();
                if (null != animator) {
                   animatorPaused = animator.pause();
                }
        
                if(context.isCreated()) {
                    helper.disposeGL(GLCanvas.this, drawable, context, postDisposeGLAction);
                }
        
                if (animatorPaused) {
                   animator.resume();
                }
             }
             // SWT is owner of the device handle, not us.
             // Hence close() operation is a NOP. 
             if (null != device) {
                device.close();
                device = null;
             }
             SWTAccessor.setRealized(GLCanvas.this, false); // unrealize ..
         } finally {
             _lock.unlock();
         }
      }
   };

   /**
    * Storage for the client area rectangle so that it may be accessed from outside of the SWT thread.
    */
   private volatile Rectangle clientArea;

   /** 
    * Creates an instance using {@link #GLCanvas(Composite, int, GLCapabilitiesImmutable, GLCapabilitiesChooser, GLContext)} 
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
    * @param shareWith
    *           Optional GLContext to share state (textures, vbos, shaders, etc.) with.
    * @return a new instance
    */
   public static GLCanvas create(final Composite parent, final int style, final GLCapabilitiesImmutable caps,
                                 final GLCapabilitiesChooser chooser, final GLContext shareWith) {
       final GLCanvas[] res = new GLCanvas[] { null }; 
       parent.getDisplay().syncExec(new Runnable() {
           public void run() {
               res[0] = new GLCanvas( parent, style, caps, chooser, shareWith );
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
   public GLCanvas(final Composite parent, final int style, GLCapabilitiesImmutable caps,
                   final GLCapabilitiesChooser chooser, final GLContext shareWith) {
      /* NO_BACKGROUND required to avoid clearing bg in native SWT widget (we do this in the GL display) */
      super(parent, style | SWT.NO_BACKGROUND);

      GLProfile.initSingleton(); // ensure JOGL is completly initialized

      SWTAccessor.setRealized(this, true);

      clientArea = GLCanvas.this.getClientArea();

      /* Get the nativewindow-Graphics Device associated with this control (which is determined by the parent Composite). 
       * Note: SWT is owner of the native handle, hence closing operation will be a NOP. */
      device = SWTAccessor.getDevice(this);

      /* Select default GLCapabilities if none was provided, otherwise clone provided caps to ensure safety */
      if(null == caps) {
          caps = new GLCapabilities(GLProfile.getDefault(device));
      }
      this.capsRequested = caps;
      this.capsChooser = chooser;
      this.shareWith = shareWith;

      // post create .. when ready
      drawable = null;
      context = null;
      
      /* Register SWT listeners (e.g. PaintListener) to render/resize GL surface. */
      /* TODO: verify that these do not need to be manually de-registered when destroying the SWT component */
      addPaintListener(new PaintListener() {
         @Override
        public void paintControl(final PaintEvent arg0) {
            if ( !helper.isExternalAnimatorAnimating() ) {                
               display(); // checks: null != drawable
            }
         }
      });

      addControlListener(new ControlAdapter() {
         @Override
         public void controlResized(final ControlEvent arg0) {
            updateSizeCheck();
         }
      });
   }
   private final UpstreamSurfaceHook swtCanvasUpStreamHook = new UpstreamSurfaceHook() {
       @Override
       public final void create(ProxySurface s) { /* nop */ }

       @Override
       public final void destroy(ProxySurface s) { /* nop */ }

       @Override
       public final int getWidth(ProxySurface s) {
           return clientArea.width;
       }

       @Override
       public final int getHeight(ProxySurface s) {
           return clientArea.height;
       }

       @Override
       public String toString() {
           return "SWTCanvasUpstreamSurfaceHook[upstream: "+GLCanvas.this.toString()+", "+clientArea.width+"x"+clientArea.height+"]";
       }
   };

   protected final void updateSizeCheck() {
      final Rectangle oClientArea = clientArea;
      final Rectangle nClientArea = GLCanvas.this.getClientArea();
      if ( nClientArea != null && 
           ( nClientArea.width != oClientArea.width || nClientArea.height != oClientArea.height )
         ) {
          clientArea = nClientArea; // write back new value
          
          GLDrawableImpl _drawable = drawable;
          if( null != _drawable ) {
              if(DEBUG) {
                  System.err.println("GLCanvas.sizeChanged: ("+Thread.currentThread().getName()+"): "+nClientArea.width+"x"+nClientArea.height+" - surfaceHandle 0x"+Long.toHexString(getNativeSurface().getSurfaceHandle()));
              }
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
                  sendReshape = true; // async if display() doesn't get called below, but avoiding deadlock
              }
          }          
      }
   }
   
   @Override
   public void display() {
      if( null != drawable || validateDrawableAndContext() ) {
          runInGLThread(makeCurrentAndDisplayOnEDTAction);
      }
   }

   
   /** assumes drawable == null ! */
   protected final boolean validateDrawableAndContext() {
      if( GLCanvas.this.isDisposed() ) {
          return false;
      }
      final Rectangle nClientArea = clientArea;
      if(0 >= nClientArea.width || 0 >= nClientArea.height) {
          return false;
      }
               
      final RecursiveLock _lock = lock;
      _lock.lock();
      try {
          final GLDrawableFactory glFactory = GLDrawableFactory.getFactory(capsRequested.getGLProfile());
    
          /* Native handle for the control, used to associate with GLContext */
          final long nativeWindowHandle = SWTAccessor.getWindowHandle(this);
          
          /* Create a NativeWindow proxy for the SWT canvas */
          ProxySurface proxySurface = null;
          try {
              proxySurface = glFactory.createProxySurface(device, 0 /* screenIdx */, nativeWindowHandle, 
                                                          capsRequested, capsChooser, swtCanvasUpStreamHook);
          } catch (GLException gle) {
              // not ready yet ..
              if(DEBUG) { System.err.println(gle.getMessage()); }
          }
          
          if(null != proxySurface) {
              /* Associate a GL surface with the proxy */
              drawable = (GLDrawableImpl) glFactory.createGLDrawable(proxySurface);
              drawable.setRealized(true);
        
              context = (GLContextImpl) drawable.createContext(shareWith);
          }
      } finally {
          _lock.unlock();
      }
      final boolean res = null != drawable;
      if(DEBUG && res) {
          System.err.println("SWT GLCanvas realized! "+this+", "+drawable);
          Thread.dumpStack();
      }
      return res;
   }
   
   @Override
   public final Object getUpstreamWidget() {
       return this;
   }
   
   @Override
   public int getWidth() {
      return clientArea.width;
   }

   @Override
   public int getHeight() {
      return clientArea.height;
   }

   @Override
   public void addGLEventListener(final GLEventListener arg0) {
      helper.addGLEventListener(arg0);
   }

   @Override
   public void addGLEventListener(final int arg0, final GLEventListener arg1) throws IndexOutOfBoundsException {
      helper.addGLEventListener(arg0, arg1);
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
   public boolean getAutoSwapBufferMode() {
      return helper.getAutoSwapBufferMode();
   }

   @Override
   public final GLDrawable getDelegatedDrawable() {
      return drawable;
   }
   
   @Override
   public GLContext getContext() {
      return null != drawable ? context : null;
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
   public boolean invoke(final boolean wait, final GLRunnable run) {
      return helper.invoke(this, wait, run);
   }

   @Override
   public void removeGLEventListener(final GLEventListener arg0) {
      helper.removeGLEventListener(arg0);
   }

   @Override
   public GLEventListener removeGLEventListener(int index) throws IndexOutOfBoundsException {
      return helper.removeGLEventListener(index);
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
   public GLContext setContext(GLContext newCtx) {
      final RecursiveLock _lock = lock;
      _lock.lock();
      try {            
          final GLContext oldCtx = context;
          final boolean newCtxCurrent = GLDrawableHelper.switchContext(drawable, oldCtx, newCtx, additionalCtxCreationFlags);
          context=(GLContextImpl)newCtx;
          if(newCtxCurrent) {
              context.makeCurrent();
          }
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

   /**
    * Accessor for the GLCapabilities that were requested (via the constructor parameter).
    *
    * @return Non-null GLCapabilities.
    */
   public GLCapabilitiesImmutable getRequestedGLCapabilities() {
      final GLDrawable _drawable = drawable; 
      return null != _drawable ? (GLCapabilitiesImmutable)_drawable.getNativeSurface().getGraphicsConfiguration().getRequestedCapabilities() : null;
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
      runInGLThread(swapBuffersOnEDTAction);
   }

   @Override
   public void update() {
      // don't paint background etc .. nop avoids flickering
   }

   @Override
   public void dispose() {
      runInGLThread(disposeOnEDTGLAction);
      super.dispose();
   }

   /**
    * Runs the specified action in an SWT compatible thread, which is:
    * <ul>
    *   <li>Mac OSX
    *   <ul>
    *     <!--li>AWT EDT: In case AWT is available, the AWT EDT is the OSX UI main thread</li-->
    *     <li><i>Main Thread</i>: Run on OSX UI main thread.</li>
    *   </ul></li>
    *   <li>Linux, Windows, ..
    *   <ul>
    *     <li>Use {@link Threading#invokeOnOpenGLThread(boolean, Runnable)}</li>
    *   </ul></li>  
    * </ul>
    * @see Platform#AWT_AVAILABLE
    * @see Platform#getOSType()
    */
   private static void runInGLThread(final Runnable action) {
      if(Platform.OSType.MACOS == Platform.OS_TYPE) {
          SWTAccessor.invoke(true, action);
      } else {
          Threading.invokeOnOpenGLThread(true, action);
      }
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

       final GLCanvas canvas = new GLCanvas(shell, 0, caps, null, null);

       canvas.addGLEventListener(new GLEventListener() {
           @Override
           public void init(final GLAutoDrawable drawable) {
               GL gl = drawable.getGL();
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
