package jogamp.opengl.egl;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.DefaultGraphicsScreen;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
import javax.media.nativewindow.VisualIDHolder.VIDType;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLException;

import com.jogamp.nativewindow.egl.EGLGraphicsDevice;

/**
 * <pre>
 * EGLWrappedSurface [ is_a -> WrappedSurface -> ProxySurfaceImpl -> ProxySurface -> MutableSurface -> NativeSurface] has_a
 *     EGLUpstreamSurfaceHook [ is_a -> UpstreamSurfaceHook.MutableSize -> UpstreamSurfaceHook ] has_a
 *        NativeSurface (e.g. native X11 surface)
 * </pre>
 */
public class EGLUpstreamSurfaceHook implements UpstreamSurfaceHook.MutableSize {
    protected static final boolean DEBUG = EGLDrawableFactory.DEBUG;
    private final NativeSurface upstreamSurface;
    private final UpstreamSurfaceHook.MutableSize upstreamSurfaceHookMutableSize;

    public EGLUpstreamSurfaceHook(final NativeSurface upstream) {
        upstreamSurface = upstream;
        if(upstreamSurface instanceof ProxySurface) {
            final UpstreamSurfaceHook ush = ((ProxySurface)upstreamSurface).getUpstreamSurfaceHook();
            if(ush instanceof UpstreamSurfaceHook.MutableSize) {
                // offscreen NativeSurface w/ MutableSize (default)
                upstreamSurfaceHookMutableSize = (UpstreamSurfaceHook.MutableSize) ush;
            } else {
                upstreamSurfaceHookMutableSize = null;
            }
        } else {
            upstreamSurfaceHookMutableSize = null;
        }
    }

    static String getThreadName() { return Thread.currentThread().getName(); }

    /**
     * {@inheritDoc}
     * <p>
     * Returns the actual upstream {@link NativeSurface}, e.g. native X11 surface.
     * </p>
     */
    @Override
    public final NativeSurface getUpstreamSurface() { return upstreamSurface; }

    @Override
    public final void setSurfaceSize(final int width, final int height) {
        if(null != upstreamSurfaceHookMutableSize) {
            upstreamSurfaceHookMutableSize.setSurfaceSize(width, height);
        }
    }

    @Override
    public final void create(final ProxySurface surface) {
        final String dbgPrefix;
        if(DEBUG) {
            dbgPrefix = getThreadName() + ": EGLUpstreamSurfaceHook.create( up "+upstreamSurface.getClass().getSimpleName()+" -> this "+surface.getClass().getSimpleName()+" ): ";
            System.err.println(dbgPrefix+this);
        } else {
            dbgPrefix = null;
        }

        if(upstreamSurface instanceof ProxySurface) {
            // propagate createNotify(..) so upstreamSurface will be created
            ((ProxySurface)upstreamSurface).createNotify();
        }

        // lock upstreamSurface, so it can be used in case EGLDisplay is derived from it!
        if(NativeSurface.LOCK_SURFACE_NOT_READY >= upstreamSurface.lockSurface()) {
            throw new GLException("Could not lock: "+upstreamSurface);
        }
        try {
            evalUpstreamSurface(dbgPrefix, surface);
        } finally {
            upstreamSurface.unlockSurface();
        }
    }

    private final void evalUpstreamSurface(final String dbgPrefix, final ProxySurface surface) {
        //
        // evaluate nature of upstreamSurface, may create EGL instances if required
        //

        boolean isEGLSurfaceValid = true; // assume yes

        final EGLGraphicsDevice eglDevice;
        final AbstractGraphicsConfiguration aConfig;
        {
            final AbstractGraphicsConfiguration surfaceConfig = surface.getGraphicsConfiguration();
            final AbstractGraphicsDevice surfaceDevice = null != surfaceConfig ? surfaceConfig.getScreen().getDevice() : null;
            if(DEBUG) {
                System.err.println(dbgPrefix+"SurfaceDevice: "+surfaceDevice.getClass().getSimpleName()+", hash 0x"+Integer.toHexString(surfaceDevice.hashCode())+", "+surfaceDevice);
                System.err.println(dbgPrefix+"SurfaceConfig: "+surfaceConfig.getClass().getSimpleName()+", hash 0x"+Integer.toHexString(surfaceConfig.hashCode())+", "+surfaceConfig);
            }

            final AbstractGraphicsConfiguration upstreamConfig = upstreamSurface.getGraphicsConfiguration();
            final AbstractGraphicsDevice upstreamDevice = upstreamConfig.getScreen().getDevice();
            if(DEBUG) {
                System.err.println(dbgPrefix+"UpstreamDevice: "+upstreamDevice.getClass().getSimpleName()+", hash 0x"+Integer.toHexString(upstreamDevice.hashCode())+", "+upstreamDevice);
                System.err.println(dbgPrefix+"UpstreamConfig: "+upstreamConfig.getClass().getSimpleName()+", hash 0x"+Integer.toHexString(upstreamConfig.hashCode())+", "+upstreamConfig);
            }

            if( surfaceDevice instanceof EGLGraphicsDevice ) {
                eglDevice = (EGLGraphicsDevice) surfaceDevice;
                aConfig = surfaceConfig;
                if(DEBUG) {
                    System.err.println(dbgPrefix+"Reusing this eglDevice: "+eglDevice+", using this config "+aConfig.getClass().getSimpleName()+" "+aConfig);
                }
                if(EGL.EGL_NO_DISPLAY == eglDevice.getHandle()) {
                    eglDevice.open();
                    isEGLSurfaceValid = false;
                    surface.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
                }
            } else if( upstreamDevice instanceof EGLGraphicsDevice ) {
                eglDevice = (EGLGraphicsDevice) upstreamDevice;
                aConfig = upstreamConfig;
                if(DEBUG) {
                    System.err.println(dbgPrefix+"Reusing upstream eglDevice: "+eglDevice+", using upstream config "+aConfig.getClass().getSimpleName()+" "+aConfig);
                }
                if(EGL.EGL_NO_DISPLAY == eglDevice.getHandle()) {
                    eglDevice.open();
                    isEGLSurfaceValid = false;
                    surface.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
                }
            } else {
                eglDevice = EGLDisplayUtil.eglCreateEGLGraphicsDevice(upstreamSurface);
                eglDevice.open();
                aConfig = upstreamConfig;
                isEGLSurfaceValid = false;
                surface.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_DEVICE );
            }
        }

        final GLCapabilitiesImmutable capsRequested = (GLCapabilitiesImmutable) aConfig.getRequestedCapabilities();
        final EGLGraphicsConfiguration eglConfig;
        if( aConfig instanceof EGLGraphicsConfiguration ) {
            // Config is already in EGL type - reuse ..
            final EGLGLCapabilities capsChosen = (EGLGLCapabilities) aConfig.getChosenCapabilities();
            if( !isEGLSurfaceValid || !EGLGraphicsConfiguration.isEGLConfigValid(eglDevice.getHandle(), capsChosen.getEGLConfig()) ) {
                // 'refresh' the native EGLConfig handle
                capsChosen.setEGLConfig(EGLGraphicsConfiguration.EGLConfigId2EGLConfig(eglDevice.getHandle(), capsChosen.getEGLConfigID()));
                if( 0 == capsChosen.getEGLConfig() ) {
                    throw new GLException("Refreshing native EGLConfig handle failed with error "+EGLContext.toHexString(EGL.eglGetError())+": "+eglDevice+", "+capsChosen+" of "+aConfig);
                }
                final AbstractGraphicsScreen eglScreen = new DefaultGraphicsScreen(eglDevice, aConfig.getScreen().getIndex());
                eglConfig  = new EGLGraphicsConfiguration(eglScreen, capsChosen, capsRequested, null);
                if(DEBUG) {
                    System.err.println(dbgPrefix+"Refreshing eglConfig: "+eglConfig);
                }
                isEGLSurfaceValid = false;
            } else {
                eglConfig = (EGLGraphicsConfiguration) aConfig;
                if(DEBUG) {
                    System.err.println(dbgPrefix+"Reusing eglConfig: "+eglConfig);
                }
            }
        } else {
            final AbstractGraphicsScreen eglScreen = new DefaultGraphicsScreen(eglDevice, aConfig.getScreen().getIndex());
            eglConfig = EGLGraphicsConfigurationFactory.chooseGraphicsConfigurationStatic(
                    capsRequested, capsRequested, null, eglScreen, aConfig.getVisualID(VIDType.NATIVE), false /* forceTransparencyFlag */);

            if (null == eglConfig) {
                throw new GLException("Couldn't create EGLGraphicsConfiguration from "+eglScreen);
            } else if(DEBUG) {
                System.err.println(dbgPrefix+"Chosen eglConfig: "+eglConfig);
            }
            isEGLSurfaceValid = false;
        }
        surface.setGraphicsConfiguration(eglConfig);

        if(isEGLSurfaceValid) {
            isEGLSurfaceValid = EGLDrawable.isValidEGLSurface(eglDevice.getHandle(), upstreamSurface.getSurfaceHandle());
        }
        if(isEGLSurfaceValid) {
            surface.setSurfaceHandle(upstreamSurface.getSurfaceHandle());
            surface.clearUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
            if(DEBUG) {
                System.err.println(dbgPrefix+"Fin: Already valid EGL surface - use as-is: "+upstreamSurface);
            }
        } else {
            surface.setSurfaceHandle(EGL.EGL_NO_SURFACE);
            surface.addUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE ); // create/destroy in EGLDrawable
            if(DEBUG) {
                System.err.println(dbgPrefix+"Fin: EGL surface n/a - TBD: "+upstreamSurface);
            }
        }
    }

    @Override
    public final void destroy(final ProxySurface surface) {
        if(EGLDrawableFactory.DEBUG) {
            System.err.println("EGLUpstreamSurfaceHook.destroy("+surface.getClass().getSimpleName()+"): "+this);
        }
        surface.clearUpstreamOptionBits( ProxySurface.OPT_PROXY_OWNS_UPSTREAM_SURFACE );
        if(upstreamSurface instanceof ProxySurface) {
            ((ProxySurface)upstreamSurface).destroyNotify();
        }
    }

    @Override
    public final int getSurfaceWidth(final ProxySurface s) {
        return upstreamSurface.getSurfaceWidth();
    }

    @Override
    public final int getSurfaceHeight(final ProxySurface s) {
        return upstreamSurface.getSurfaceHeight();
    }

    @Override
    public String toString() {
        final String us_s;
        final int sw, sh;
        if( null != upstreamSurface ) {
            us_s = upstreamSurface.getClass().getName() + ": 0x" + Long.toHexString(upstreamSurface.getSurfaceHandle());
            sw = upstreamSurface.getSurfaceWidth();
            sh = upstreamSurface.getSurfaceHeight();
        } else {
            us_s = "nil";
            sw = -1;
            sh = -1;
        }
        return "EGLUpstreamSurfaceHook[ "+ sw + "x" + sh + ", " + us_s+ "]";
    }

}
