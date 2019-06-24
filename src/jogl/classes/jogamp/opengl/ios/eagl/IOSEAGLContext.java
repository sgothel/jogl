/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

package jogamp.opengl.ios.eagl;

import java.util.Map;

import com.jogamp.nativewindow.AbstractGraphicsConfiguration;
import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.nativewindow.OffscreenLayerSurface;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableFactoryImpl;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.GLDynamicLookupHelper;
import jogamp.opengl.GLFBODrawableImpl;
import jogamp.opengl.GLDrawableFactoryImpl.OnscreenFBOColorbufferStorageDefinition;
import jogamp.opengl.GLFBODrawableImpl.SwapBufferContext;
import jogamp.opengl.DummyGLExtProcAddressTable;
import jogamp.opengl.ios.eagl.IOSEAGLDrawable.GLBackendType;

import com.jogamp.common.os.Platform;
import com.jogamp.gluegen.runtime.ProcAddressTable;
import com.jogamp.gluegen.runtime.opengl.GLProcAddressResolver;
import com.jogamp.opengl.GLRendererQuirks;

public class IOSEAGLContext extends GLContextImpl
{
  // Abstract interface for implementation of this context
  protected interface GLBackendImpl {
        /** Indicating CALayer, i.e. onscreen rendering using offscreen layer. */
        boolean isUsingCAEAGLLayer();
        long create(long share, int ctp, int major, int minor);
        boolean destroy(long ctx);
        void associateDrawable(boolean bound);
        boolean makeCurrent(long ctx);
        boolean release(long ctx);
  }

  static boolean isGLProfileSupported(final int ctp, final int major, final int minor) {
    if( 0 == ( CTX_PROFILE_ES & ctp ) ) {
        // only ES profiles supported
        return false;
    }
    return true;
  }
  static int GLProfile2EAGLProfileValue(final int ctp, final int major, final int minor) {
    if(!isGLProfileSupported(ctp, major, minor)) {
        throw new GLException("OpenGL profile not supported.0: "+getGLVersion(major, minor, ctp, "@GLProfile2EAGLProfileValue"));
    }
    switch( major ) {
    case 1:
        return EAGL.kEAGLRenderingAPIOpenGLES1;
    case 2:
        return EAGL.kEAGLRenderingAPIOpenGLES2;
    case 3:
        return EAGL.kEAGLRenderingAPIOpenGLES3;
    }
    throw new GLException("OpenGL profile not supported.1: "+getGLVersion(major, minor, ctp, "@GLProfile2EAGLProfileValue"));
  }

  private boolean haveSetOpenGLMode = false;
  private GLBackendType openGLMode = GLBackendType.CAEAGL_LAYER;

  // Implementation object (either NSOpenGL-based or CGL-based)
  protected GLBackendImpl impl;

  // CGL extension functions.
  private DummyGLExtProcAddressTable cglExtProcAddressTable;

  protected IOSEAGLContext(final GLDrawableImpl drawable,
                   final GLContext shareWith) {
    super(drawable, shareWith);
    initOpenGLImpl(getOpenGLMode());
  }

  @Override
  protected void resetStates(final boolean isInit) {
    // no inner state _cglExt = null;
    super.resetStates(isInit);
  }

  @Override
  public Object getPlatformGLExtensions() {
    return null;
  }

  @Override
  public final ProcAddressTable getPlatformExtProcAddressTable() {
    return getCGLExtProcAddressTable();
  }

  public final DummyGLExtProcAddressTable getCGLExtProcAddressTable() {
    return cglExtProcAddressTable;
  }

  @Override
  protected Map<String, String> getFunctionNameMap() { return null; }

  @Override
  protected Map<String, String> getExtensionNameMap() { return null; }

  @Override
  protected long createContextARBImpl(final long share, final boolean direct, final int ctp, final int major, final int minor) {
    if(!isGLProfileSupported(ctp, major, minor)) {
        if(DEBUG) {
            System.err.println(getThreadName() + ": createContextARBImpl: Not supported "+getGLVersion(major, minor, ctp, "@creation on iOS "+Platform.getOSVersionNumber()));
        }
        return 0;
    }

    // Will throw exception upon error
    long ctx = impl.create(share, ctp, major, minor);
    if(0 != ctx) {
        if (!impl.makeCurrent(ctx)) {
          if(DEBUG) {
              System.err.println(getThreadName() + ": createContextARB couldn't make current "+getGLVersion(major, minor, ctp, "@creation"));
          }
          impl.release(ctx);
          impl.destroy(ctx);
          ctx = 0;
        } else if(DEBUG) {
            System.err.println(getThreadName() + ": createContextARBImpl: OK "+getGLVersion(major, minor, ctp, "@creation")+", share "+share+", direct "+direct+" on iOS "+Platform.getOSVersionNumber());
        }
    } else if(DEBUG) {
        System.err.println(getThreadName() + ": createContextARBImpl: NO "+getGLVersion(major, minor, ctp, "@creation on iOS "+Platform.getOSVersionNumber()));
    }
    return ctx;
  }

  @Override
  protected void destroyContextARBImpl(final long _context) {
      impl.release(_context);
      impl.destroy(_context);
  }

  @Override
  public final boolean isGLReadDrawableAvailable() {
    return false;
  }

  @Override
  protected boolean createImpl(final long shareWithHandle) throws GLException {
    final MutableGraphicsConfiguration config = (MutableGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice device = config.getScreen().getDevice();
    final GLCapabilitiesImmutable glCaps = (GLCapabilitiesImmutable) config.getChosenCapabilities();
    final GLProfile glp = glCaps.getGLProfile();
    final boolean createContextARBAvailable = isCreateContextARBAvail(device);
    if(DEBUG) {
        System.err.println(getThreadName() + ": IOSEAGLContext.createImpl: START "+glCaps+", share "+toHexString(shareWithHandle));
        System.err.println(getThreadName() + ": Use ARB[avail["+getCreateContextARBAvailStr(device)+
                "] -> "+createContextARBAvailable+"]]");
    }
    if( !glp.isGLES() ) {
        throw new GLException("Desktop OpenGL profile not supported on iOS "+Platform.getOSVersionNumber()+": "+glp);
    }
    contextHandle = createContextARB(shareWithHandle, true);
    return 0 != contextHandle;
  }

  @Override
  protected void makeCurrentImpl() throws GLException {
    if ( !impl.makeCurrent(contextHandle) ) {
      throw new GLException("Error making Context current: "+this);
    }
    drawableUpdatedNotify();
  }

  @Override
  protected void releaseImpl() throws GLException {
    if (!impl.release(contextHandle)) {
      throw new GLException("Error releasing OpenGL Context: "+this);
    }
  }

  @Override
  protected void destroyImpl() throws GLException {
    if(!impl.destroy(contextHandle)) {
        throw new GLException("Error destroying OpenGL Context: "+this);
    }
  }

  @Override
  protected void drawableUpdatedNotify() throws GLException {
      // NOTE to resize: GLFBODrawableImpl.resetSize(GL) called from many
      // high level instances in the GLDrawable* space,
      // e.g. GLAutoDrawableBase's defaultWindowResizedOp(..)
  }

  @Override
  protected void associateDrawable(final boolean bound) {
      // context stuff depends on drawable stuff
      final GLFBODrawableImpl fboDrawable;
      final boolean taggedOnscreenFBOEAGLLayer;
      {
          final GLDrawableImpl drawable = getDrawableImpl();
          final GLDrawableFactoryImpl factory = drawable.getFactoryImpl();
          final IOSEAGLDrawableFactory iosFactory = (factory instanceof IOSEAGLDrawableFactory) ? (IOSEAGLDrawableFactory) factory : null;
          final OnscreenFBOColorbufferStorageDefinition onscreenFBOColorbufStorageDef = (null != iosFactory) ? iosFactory.getOnscreenFBOColorbufStorageDef() : null;

          fboDrawable = (drawable instanceof GLFBODrawableImpl) ? (GLFBODrawableImpl)drawable : null;
          taggedOnscreenFBOEAGLLayer = (null != fboDrawable && null != onscreenFBOColorbufStorageDef) ?
                  fboDrawable.hasColorRenderbufferStorageDef(onscreenFBOColorbufStorageDef) : false;
      }
      if( DEBUG ) {
          System.err.println(getThreadName() + ": IOSEAGLContext.associateDrawable(bound "+bound+"): taggedOnscreenFBOEAGLLayer "+taggedOnscreenFBOEAGLLayer+
                             ", hasFBODrawable "+(null != fboDrawable)+", drawable: "+getDrawableImpl().getClass().getName());
      }
      if(bound) {
          if( taggedOnscreenFBOEAGLLayer ) {
              // Done in GLDrawableFactory.createGDrawable(..) for onscreen drawables:
              // fboDrawable.setColorRenderbufferStorageDef(iosFactory.getOnscreenFBOColorbufStorageDef());
              fboDrawable.setSwapBufferContext(new SwapBufferContext() {
                  @Override
                  public void swapBuffers(final boolean doubleBuffered) {
                      EAGL.eaglPresentRenderbuffer(contextHandle, GL.GL_RENDERBUFFER);
                  } } );
          }
          super.associateDrawable(true);   // 1) init drawable stuff (FBO init, ..)
          impl.associateDrawable(true);    // 2) init context stuff
      } else {
          impl.associateDrawable(false);   // 1) free context stuff
          super.associateDrawable(false);  // 2) free drawable stuff
          if( taggedOnscreenFBOEAGLLayer ) {
              EAGL.eaglBindDrawableStorageToRenderbuffer(contextHandle, GL.GL_RENDERBUFFER, 0);
          }
      }
  }

  @Override
  protected void copyImpl(final GLContext source, final int mask) throws GLException {
      throw new GLException("copyImpl n/a: "+this);
  }

  /**
   * {@inheritDoc}
   * <p>
   * Ignoring {@code contextFQN}, using {@code iOS}-{@link AbstractGraphicsDevice#getUniqueID()}.
   * </p>
   */
  @Override
  protected final void updateGLXProcAddressTable(final String contextFQN, final GLDynamicLookupHelper dlh) {
    if( null == dlh ) {
        throw new GLException("No GLDynamicLookupHelper for "+this);
    }
    final AbstractGraphicsConfiguration aconfig = drawable.getNativeSurface().getGraphicsConfiguration();
    final AbstractGraphicsDevice adevice = aconfig.getScreen().getDevice();
    final String key = "iOS-"+adevice.getUniqueID();
    if (DEBUG) {
      System.err.println(getThreadName() + ": Initializing EAGL extension address table: "+key);
    }
    ProcAddressTable table = null;
    synchronized(mappedContextTypeObjectLock) {
        table = mappedGLXProcAddress.get( key );
    }
    if(null != table) {
        cglExtProcAddressTable = (DummyGLExtProcAddressTable) table;
        if(DEBUG) {
            System.err.println(getThreadName() + ": GLContext CGL ProcAddressTable reusing key("+key+") -> "+toHexString(table.hashCode()));
        }
    } else {
        cglExtProcAddressTable = new DummyGLExtProcAddressTable(new GLProcAddressResolver());
        resetProcAddressTable(getCGLExtProcAddressTable(), dlh);
        synchronized(mappedContextTypeObjectLock) {
            mappedGLXProcAddress.put(key, getCGLExtProcAddressTable());
            if(DEBUG) {
                System.err.println(getThreadName() + ": GLContext CGL ProcAddressTable mapping key("+key+") -> "+toHexString(getCGLExtProcAddressTable().hashCode()));
            }
        }
    }
  }

  @Override
  protected final StringBuilder getPlatformExtensionsStringImpl() {
    return new StringBuilder();
  }

  // Support for "mode switching" as described in IOSEAGLDrawable
  public void setOpenGLMode(final GLBackendType mode) {
      if (mode == openGLMode) {
        return;
      }
      if (haveSetOpenGLMode) {
        throw new GLException("Can't switch between using EAGL and ... more than once");
      }
      destroyImpl();
      ((IOSEAGLDrawable)drawable).setOpenGLMode(mode);
      if (DEBUG) {
        System.err.println("IOSEAGLContext: Switching context mode " + openGLMode + " -> " + mode);
      }
      initOpenGLImpl(mode);
      openGLMode = mode;
      haveSetOpenGLMode = true;
  }
  public final GLBackendType getOpenGLMode() { return openGLMode; }

  protected void initOpenGLImpl(final GLBackendType backend) {
    switch (backend) {
      case CAEAGL_LAYER:
        impl = new CAEAGLLayerImpl();
        break;
      default:
        throw new InternalError("Illegal implementation mode " + backend);
    }
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    sb.append(" [");
    super.append(sb);
    sb.append("] ");
    return sb.toString();
  }

  class CAEAGLLayerImpl implements GLBackendImpl {
      private final OffscreenLayerSurface backingLayerHost = null;

      @Override
      public boolean isUsingCAEAGLLayer() { return null != backingLayerHost; }

      @Override
      public long create(final long share, final int ctp, final int major, final int minor) {
          long ctx = 0;
          final MutableGraphicsConfiguration config = (MutableGraphicsConfiguration) drawable.getNativeSurface().getGraphicsConfiguration();
          final GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable)config.getChosenCapabilities();
          // Create new context
          if (DEBUG) {
              System.err.println("Share context for EAGL-based context is " + toHexString(share));
          }
          final boolean isFBO = drawable instanceof GLFBODrawableImpl;
          final int api = GLProfile2EAGLProfileValue(ctp, major, minor);
          if( 0 != share ) {
              ctx = EAGL.eaglCreateContextShared(api, EAGL.eaglGetSharegroup(share));
          } else {
              ctx = EAGL.eaglCreateContext(api);
          }
          if (0 != ctx) {
              final GLCapabilitiesImmutable fixedCaps;
              if( isFBO ) {
                  fixedCaps = chosenCaps;
              } else {
                  if( DEBUG ) {
                      System.err.println("Warning: CAEAGLLayer w/ non FBO caps");
                  }
                  fixedCaps = chosenCaps;
              }
              if(DEBUG) {
                  System.err.println("NS create backingLayerHost: "+backingLayerHost);
                  System.err.println("NS create share: "+share);
                  System.err.println("NS create drawable type: "+drawable.getClass().getName());
                  System.err.println("NS create chosenCaps: "+chosenCaps);
                  System.err.println("NS create fixedCaps: "+fixedCaps);
                  System.err.println("NS create drawable native-handle: "+toHexString(drawable.getHandle()));
                  System.err.println("NS create surface native-handle: "+toHexString(drawable.getNativeSurface().getSurfaceHandle()));
                  // Thread.dumpStack();
              }
              config.setChosenCapabilities(fixedCaps);
              if(DEBUG) {
                  System.err.println("EAGL create fixedCaps: "+fixedCaps);
              }
          }
          return ctx;
      }

      @Override
      public boolean destroy(final long ctx) {
          return EAGL.eaglDeleteContext(ctx, true /* releaseOnMainThread */);
      }

      @Override
      public void associateDrawable(final boolean bound) {
      }

      @Override
      public boolean makeCurrent(final long ctx) {
          return EAGL.eaglMakeCurrentContext(ctx);
      }

      @Override
      public boolean release(final long ctx) {
          try {
              if( hasRendererQuirk(GLRendererQuirks.GLFlushBeforeRelease) && null != IOSEAGLContext.this.getGLProcAddressTable() ) {
                  gl.glFlush();
              }
          } catch (final GLException gle) {
              if(DEBUG) {
                  System.err.println("IOSEAGLContext.CGLImpl.release: INFO: glFlush() caught exception:");
                  gle.printStackTrace();
              }
          }
          return EAGL.eaglMakeCurrentContext(0);
      }
  }

    @Override
    protected Integer setSwapIntervalImpl2(final int interval) {
        // TODO
        return null;
    }
}
