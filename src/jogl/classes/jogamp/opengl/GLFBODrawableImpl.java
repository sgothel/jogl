package jogamp.opengl;

import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowException;
import javax.media.nativewindow.ProxySurface;
import javax.media.nativewindow.UpstreamSurfaceHook;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLFBODrawable;

import com.jogamp.common.util.PropertyAccess;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.Colorbuffer;
import com.jogamp.opengl.FBObject.TextureAttachment;
import com.jogamp.opengl.JoglVersion;

/**
 * {@link FBObject} offscreen GLDrawable implementation, i.e. {@link GLFBODrawable}.
 * <p>
 * It utilizes the context lifecycle hook {@link #contextRealized(GLContext, boolean)}
 * to initialize the {@link FBObject} instance.
 * </p>
 * <p>
 * It utilizes the context current hook {@link #contextMadeCurrent(GLContext, boolean) contextMadeCurrent(context, true)}
 * to {@link FBObject#bind(GL) bind} the FBO.
 * </p>
 * See {@link GLFBODrawable} for double buffering details.
 *
 * @see GLDrawableImpl#contextRealized(GLContext, boolean)
 * @see GLDrawableImpl#contextMadeCurrent(GLContext, boolean)
 * @see GLDrawableImpl#getDefaultDrawFramebuffer()
 * @see GLDrawableImpl#getDefaultReadFramebuffer()
 */
public class GLFBODrawableImpl extends GLDrawableImpl implements GLFBODrawable {
    protected static final boolean DEBUG;
    protected static final boolean DEBUG_SWAP;

    static {
        Debug.initSingleton();
        DEBUG = GLDrawableImpl.DEBUG || Debug.debug("FBObject");
        DEBUG_SWAP = DEBUG || PropertyAccess.isPropertyDefined("jogl.debug.FBObject.Swap", true);
    }

    private final GLDrawableImpl parent;
    private GLCapabilitiesImmutable origParentChosenCaps;

    private boolean initialized;
    private int texUnit;
    private int samples;
    private boolean fboResetQuirk;

    private FBObject[] fbos;
    private int fboIBack;  // points to GL_BACK buffer
    private int fboIFront; // points to GL_FRONT buffer
    private int pendingFBOReset = -1;
    /** Indicated whether the FBO is bound. */
    private boolean fboBound;
    /** Indicated whether the FBO is swapped, resets to false after makeCurrent -> contextMadeCurrent. */
    private boolean fboSwapped;

    /** dump fboResetQuirk info only once pre ClassLoader and only in DEBUG mode */
    private static volatile boolean resetQuirkInfoDumped = false;

    /** number of FBOs for double buffering. TODO: Possible to configure! */
    private static final int bufferCount = 2;

    // private DoubleBufferMode doubleBufferMode; // TODO: Add or remove TEXTURE (only) DoubleBufferMode support

    private SwapBufferContext swapBufferContext;

    public static interface SwapBufferContext {
        public void swapBuffers(boolean doubleBuffered);
    }

    /**
     * @param factory
     * @param parent
     * @param surface
     * @param fboCaps the requested FBO capabilities
     * @param textureUnit
     */
    protected GLFBODrawableImpl(final GLDrawableFactoryImpl factory, final GLDrawableImpl parent, final NativeSurface surface,
                                final GLCapabilitiesImmutable fboCaps, final int textureUnit) {
        super(factory, surface, fboCaps, false);
        this.initialized = false;

        this.parent = parent;
        this.origParentChosenCaps = getChosenGLCapabilities(); // just to avoid null, will be reset at initialize(..)
        this.texUnit = textureUnit;
        this.samples = fboCaps.getNumSamples();
        fboResetQuirk = false;

        // default .. // TODO: Add or remove TEXTURE (only) DoubleBufferMode support
        // this.doubleBufferMode = ( samples > 0 || fboCaps.getDoubleBuffered() ) ? DoubleBufferMode.FBO : DoubleBufferMode.NONE ;

        this.swapBufferContext = null;
    }

    private final void initialize(final boolean realize, final GL gl) {
        if( !initialized && !realize ) {
            if( DEBUG ) {
                System.err.println("GLFBODrawableImpl.initialize(): WARNING - Already unrealized!");
                Thread.dumpStack();
            }
            return; // NOP, no exception for de-init twice or no init!
        }
        if( initialized == realize ) {
            throw new IllegalStateException("initialize already in state "+realize+": "+this);
        }
        if(realize) {
            final GLCapabilities chosenFBOCaps = (GLCapabilities) getChosenGLCapabilities(); // cloned at setRealized(true)

            final int maxSamples = gl.getMaxRenderbufferSamples();
            {
                final int newSamples = samples <= maxSamples ? samples : maxSamples;
                if(DEBUG) {
                    System.err.println("GLFBODrawableImpl.initialize(): samples "+samples+" -> "+newSamples+"/"+maxSamples);
                }
                samples = newSamples;
            }

            final int fbosN;
            if(samples > 0) {
                fbosN = 1;
            } else if( chosenFBOCaps.getDoubleBuffered() ) {
                fbosN = bufferCount;
            } else {
                fbosN = 1;
            }

            fbos = new FBObject[fbosN];
            fboIBack = 0;                // head
            fboIFront = fbos.length - 1; // tail

            for(int i=0; i<fbosN; i++) {
                fbos[i] = new FBObject();
                fbos[i].reset(gl, getSurfaceWidth(), getSurfaceHeight(), samples, false);
                if(fbos[i].getNumSamples() != samples) {
                    throw new InternalError("Sample number mismatch: "+samples+", fbos["+i+"] "+fbos[i]);
                }
                if(samples > 0) {
                    fbos[i].attachColorbuffer(gl, 0, chosenFBOCaps.getAlphaBits()>0);
                } else {
                    fbos[i].attachTexture2D(gl, 0, chosenFBOCaps.getAlphaBits()>0);
                }
                if( chosenFBOCaps.getStencilBits() > 0 ) {
                    fbos[i].attachRenderbuffer(gl, Attachment.Type.DEPTH_STENCIL, 24);
                } else {
                    fbos[i].attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
                }
            }
            fbos[fboIFront].resetSamplingSink(gl);

            fbos[0].formatToGLCapabilities(chosenFBOCaps);
            chosenFBOCaps.setDoubleBuffered( chosenFBOCaps.getDoubleBuffered() || samples > 0 );
        } else {
            for(int i=0; i<fbos.length; i++) {
                fbos[i].destroy(gl);
            }
            fbos=null;
        }
        fboBound = false;
        fboSwapped = false;
        pendingFBOReset = -1;
        initialized = realize;

        if(DEBUG) {
            System.err.println("GLFBODrawableImpl.initialize("+realize+"): "+this);
            Thread.dumpStack();
        }
    }

    public final void setSwapBufferContext(final SwapBufferContext sbc) {
        swapBufferContext = sbc;
    }

    private final void reset(final GL gl, final int idx, final int width, final int height, final int samples, final int alphaBits, final int stencilBits) {
        if( !fboResetQuirk ) {
            try {
                fbos[idx].reset(gl, width, height, samples, false);
                if(fbos[idx].getNumSamples() != samples) {
                    throw new InternalError("Sample number mismatch: "+samples+", fbos["+idx+"] "+fbos[idx]);
                }
                return;
            } catch (final GLException e) {
                fboResetQuirk = true;
                if(DEBUG) {
                    if(!resetQuirkInfoDumped) {
                        resetQuirkInfoDumped = true;
                        System.err.println("GLFBODrawable: FBO Reset failed: "+e.getMessage());
                        System.err.println("GLFBODrawable: Enabling FBOResetQuirk, due to GL driver bug.");
                        final JoglVersion joglVersion = JoglVersion.getInstance();
                        if(DEBUG) {
                            System.err.println(VersionUtil.getPlatformInfo());
                            System.err.println(joglVersion.toString());
                            System.err.println(JoglVersion.getGLInfo(gl, null));
                        } else {
                            System.err.println(joglVersion.getBriefOSGLBuildInfo(gl, null));
                        }
                        e.printStackTrace();
                    }
                }
                // 'fallthrough' intended
            }
        }
        // resetQuirk fallback
        fbos[idx].destroy(gl);
        fbos[idx] = new FBObject();
        fbos[idx].reset(gl, getSurfaceWidth(), getSurfaceHeight(), samples, false);
        if(fbos[idx].getNumSamples() != samples) {
            throw new InternalError("Sample number mismatch: "+samples+", fbos["+idx+"] "+fbos[idx]);
        }
        if(samples > 0) {
            fbos[idx].attachColorbuffer(gl, 0, alphaBits>0);
        } else {
            fbos[idx].attachTexture2D(gl, 0, alphaBits>0);
        }
        if( stencilBits > 0 ) {
            fbos[idx].attachRenderbuffer(gl, Attachment.Type.DEPTH_STENCIL, 24);
        } else {
            fbos[idx].attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
        }
    }

    private final void reset(final GL gl, int newSamples) throws GLException {
        if(!initialized) {
            // NOP if not yet initializes
            return;
        }

        final GLContext curContext = GLContext.getCurrent();
        final GLContext ourContext = gl.getContext();
        final boolean ctxSwitch = null != curContext && curContext != ourContext;
        if(DEBUG) {
            System.err.println("GLFBODrawableImpl.reset(newSamples "+newSamples+"): BEGIN - ctxSwitch "+ctxSwitch+", "+this);
            Thread.dumpStack();
        }
        Throwable tFBO = null;
        Throwable tGL = null;
        ourContext.makeCurrent();
        gl.glFinish(); // sync GL command stream
        fboBound = false; // clear bound-flag immediatly, caused by contextMadeCurrent(..) - otherwise we would swap @ release
        fboSwapped = false;
        try {
            final int maxSamples = gl.getMaxRenderbufferSamples();
            newSamples = newSamples <= maxSamples ? newSamples : maxSamples;

            if(0==samples && 0<newSamples || 0<samples && 0==newSamples) {
                // MSAA on/off switch
                if(DEBUG) {
                    System.err.println("GLFBODrawableImpl.reset(): samples [on/off] reconfig: "+samples+" -> "+newSamples+"/"+maxSamples);
                }
                initialize(false, gl);
                samples = newSamples;
                initialize(true, gl);
            } else {
                if(DEBUG) {
                    System.err.println("GLFBODrawableImpl.reset(): simple reconfig: "+samples+" -> "+newSamples+"/"+maxSamples);
                }
                final int nWidth = getSurfaceWidth();
                final int nHeight = getSurfaceHeight();
                samples = newSamples;
                pendingFBOReset = ( 1 < fbos.length ) ? fboIFront : -1; // pending-front reset only w/ double buffering (or zero samples)
                final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) surface.getGraphicsConfiguration().getChosenCapabilities();
                for(int i=0; i<fbos.length; i++) {
                    if( pendingFBOReset != i ) {
                        reset(gl, i, nWidth, nHeight, samples, caps.getAlphaBits(), caps.getStencilBits());
                    }
                }
                final GLCapabilities fboCapsNative = (GLCapabilities) surface.getGraphicsConfiguration().getChosenCapabilities();
                fbos[0].formatToGLCapabilities(fboCapsNative);
            }
        } catch (final Throwable t) {
            tFBO = t;
        } finally {
            try {
                ourContext.release();
                if(ctxSwitch) {
                    curContext.makeCurrent();
                }
            } catch (final Throwable t) {
                tGL = t;
            }
        }
        if(null != tFBO) {
            throw new GLException("GLFBODrawableImpl.reset(..) FBObject.reset(..) exception", tFBO);
        }
        if(null != tGL) {
            throw new GLException("GLFBODrawableImpl.reset(..) GLContext.release() exception", tGL);
        }
        if(DEBUG) {
            System.err.println("GLFBODrawableImpl.reset(newSamples "+newSamples+"): END "+this);
        }
    }

    //
    // GLDrawable
    //

    @Override
    public final GLContext createContext(final GLContext shareWith) {
        final GLContext ctx = parent.createContext(shareWith);
        ctx.setGLDrawable(this, false);
        return ctx;
    }

    //
    // GLDrawableImpl
    //

    @Override
    public final GLDynamicLookupHelper getGLDynamicLookupHelper() {
        return parent.getGLDynamicLookupHelper();
    }

    @Override
    protected final int getDefaultDrawFramebuffer() { return initialized ? fbos[fboIBack].getWriteFramebuffer() : 0; }

    @Override
    protected final int getDefaultReadFramebuffer() { return initialized ? fbos[fboIFront].getReadFramebuffer() : 0; }

    @Override
    protected final int getDefaultReadBuffer(final GL gl, final boolean hasDedicatedDrawableRead) {
        return initialized ? fbos[fboIFront].getDefaultReadBuffer() : GL.GL_COLOR_ATTACHMENT0 ;
    }

    @Override
    protected final void setRealizedImpl() {
        final MutableGraphicsConfiguration msConfig = (MutableGraphicsConfiguration) surface.getGraphicsConfiguration();
        if(realized) {
            parent.setRealized(true);
            origParentChosenCaps = (GLCapabilitiesImmutable) msConfig.getChosenCapabilities();
            final GLCapabilities chosenFBOCaps = (GLCapabilities) origParentChosenCaps.cloneMutable(); // incl. <Type>GLCapabilities, e.g. X11GLCapabilities
            chosenFBOCaps.copyFrom(getRequestedGLCapabilities()); // copy user values
            msConfig.setChosenCapabilities(chosenFBOCaps);
        } else {
            msConfig.setChosenCapabilities(origParentChosenCaps);
            parent.setRealized(false);
        }
    }

    @Override
    protected void associateContext(final GLContext glc, final boolean bound) {
        initialize(bound, glc.getGL());
    }

    @Override
    protected final void contextMadeCurrent(final GLContext glc, final boolean current) {
        final GL gl = glc.getGL();
        if(current) {
            if( !initialized ) {
                throw new GLException("Not initialized: "+this);
            }
            fbos[fboIBack].bind(gl);
            fboBound = true;
            fboSwapped = false;
        } else if( fboBound && !fboSwapped ) {
            swapFBOImpl(glc);
            swapFBOImplPost(glc);
            fboBound=false;
            fboSwapped=true;
            if(DEBUG_SWAP) {
                System.err.println("Post FBO swap(@release): done");
            }
        }
    }

    @Override
    protected void swapBuffersImpl(final boolean doubleBuffered) {
        final GLContext ctx = GLContext.getCurrent();
        boolean doPostSwap;
        if( null != ctx && ctx.getGLDrawable() == this && fboBound ) {
            swapFBOImpl(ctx);
            doPostSwap = true;
            fboSwapped = true;
            if(DEBUG_SWAP) {
                System.err.println("Post FBO swap(@swap): done");
            }
        } else {
            doPostSwap = false;
        }
        if( null != swapBufferContext ) {
            swapBufferContext.swapBuffers(doubleBuffered);
        }
        if(doPostSwap) {
            swapFBOImplPost(ctx);
        }
    }

    private final void swapFBOImplPost(final GLContext glc) {
        // Safely reset the previous front FBO - after completing propagating swap
        if(0 <= pendingFBOReset) {
            final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) surface.getGraphicsConfiguration().getChosenCapabilities();
            reset(glc.getGL(), pendingFBOReset, getSurfaceWidth(), getSurfaceHeight(), samples, caps.getAlphaBits(), caps.getStencilBits());
            pendingFBOReset = -1;
        }
    }

    private final void swapFBOImpl(final GLContext glc) {
        final GL gl = glc.getGL();
        fbos[fboIBack].markUnbound(); // fast path, use(gl,..) is called below

        if(DEBUG) {
            final int _fboIFront = ( fboIFront + 1 ) % fbos.length;
            if(_fboIFront != fboIBack) { throw new InternalError("XXX: "+_fboIFront+"!="+fboIBack); }
        }
        fboIFront = fboIBack;
        fboIBack  = ( fboIBack  + 1 ) % fbos.length;

        final Colorbuffer colorbuffer = samples > 0 ? fbos[fboIFront].getSamplingSink() : fbos[fboIFront].getColorbuffer(0);
        final TextureAttachment texAttachment;
        if(colorbuffer instanceof TextureAttachment) {
            texAttachment = (TextureAttachment) colorbuffer;
        } else {
            if(null == colorbuffer) {
                throw new GLException("Front colorbuffer is null: samples "+samples+", "+this);
            } else {
                throw new GLException("Front colorbuffer is not a texture: "+colorbuffer.getClass().getName()+": samples "+samples+", "+colorbuffer+", "+this);
            }
        }
        gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit);
        fbos[fboIFront].use(gl, texAttachment);

        /* Included in above use command:
                gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, fbos[fboIBack].getDrawFramebuffer());
                gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, fbos[fboIFront].getReadFramebuffer());
        } */

        if(DEBUG_SWAP) {
            System.err.println("Post FBO swap(X): fboI back "+fboIBack+", front "+fboIFront+", num "+fbos.length);
        }
    }

    //
    // GLFBODrawable
    //

    @Override
    public final boolean isInitialized() {
        return initialized;
    }

    @Override
    public final void resetSize(final GL gl) throws GLException {
        reset(gl, samples);
    }

    @Override
    public final int getTextureUnit() { return texUnit; }

    @Override
    public final void setTextureUnit(final int u) { texUnit = u; }

    @Override
    public final int getNumSamples() { return samples; }

    @Override
    public void setNumSamples(final GL gl, final int newSamples) throws GLException {
        if(samples != newSamples) {
            reset(gl, newSamples);
        }
    }

    @Override
    public final int setNumBuffers(final int bufferCount) throws GLException {
        // FIXME: Implement
        return bufferCount;
    }

    @Override
    public final int getNumBuffers() {
        return bufferCount;
    }

    /** // TODO: Add or remove TEXTURE (only) DoubleBufferMode support
    @Override
    public final DoubleBufferMode getDoubleBufferMode() {
        return doubleBufferMode;
    }

    @Override
    public final void setDoubleBufferMode(DoubleBufferMode mode) throws GLException {
        if(initialized) {
            throw new GLException("Not allowed past initialization: "+this);
        }
        final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) surface.getGraphicsConfiguration().getChosenCapabilities();
        if(0 == samples && caps.getDoubleBuffered() && DoubleBufferMode.NONE != mode) {
            doubleBufferMode = mode;
        }
    } */

    @Override
    public FBObject getFBObject(final int bufferName) throws IllegalArgumentException {
        if(!initialized) {
            return null;
        }
        final FBObject res;
        switch(bufferName) {
            case GL.GL_FRONT:
                if( samples > 0 ) {
                    res = fbos[0].getSamplingSinkFBO();
                } else {
                    res = fbos[fboIFront];
                }
                break;
            case GL.GL_BACK:
                res = fbos[fboIBack];
                break;
            default:
                throw new IllegalArgumentException(illegalBufferName+toHexString(bufferName));
        }
        return res;
    }

    @Override
    public final TextureAttachment getTextureBuffer(final int bufferName) throws IllegalArgumentException {
        if(!initialized) {
            return null;
        }
        final TextureAttachment res;
        switch(bufferName) {
            case GL.GL_FRONT:
                if( samples > 0 ) {
                    res = fbos[0].getSamplingSink();
                } else {
                    res = (TextureAttachment) fbos[fboIFront].getColorbuffer(0);
                }
                break;
            case GL.GL_BACK:
                if( samples > 0 ) {
                    throw new IllegalArgumentException("Cannot access GL_BACK buffer of MSAA FBO: "+this);
                } else {
                    res = (TextureAttachment) fbos[fboIBack].getColorbuffer(0);
                }
                break;
            default:
                throw new IllegalArgumentException(illegalBufferName+toHexString(bufferName));
        }
        return res;
    }
    private static final String illegalBufferName = "Only GL_FRONT and GL_BACK buffer are allowed, passed ";

    @Override
    public String toString() {
        return getClass().getSimpleName()+"[Initialized "+initialized+", realized "+isRealized()+", texUnit "+texUnit+", samples "+samples+
                ",\n\tFactory   "+getFactory()+
                ",\n\tHandle    "+toHexString(getHandle())+
                ",\n\tCaps      "+surface.getGraphicsConfiguration().getChosenCapabilities()+
                ",\n\tfboI back "+fboIBack+", front "+fboIFront+", num "+(initialized ? fbos.length : 0)+
                ",\n\tFBO front read "+getDefaultReadFramebuffer()+", "+getFBObject(GL.GL_FRONT)+
                ",\n\tFBO back  write "+getDefaultDrawFramebuffer()+", "+getFBObject(GL.GL_BACK)+
                ",\n\tSurface   "+surface+
                "]";
    }

    public static class ResizeableImpl extends GLFBODrawableImpl implements GLFBODrawable.Resizeable {
        protected ResizeableImpl(final GLDrawableFactoryImpl factory, final GLDrawableImpl parent, final ProxySurface surface,
                                 final GLCapabilitiesImmutable fboCaps, final int textureUnit) {
            super(factory, parent, surface, fboCaps, textureUnit);
        }

        @Override
        public final void setSurfaceSize(final GLContext context, final int newWidth, final int newHeight) throws NativeWindowException, GLException {
            if(DEBUG) {
                System.err.println("GLFBODrawableImpl.ResizeableImpl setSize: ("+getThreadName()+"): "+newWidth+"x"+newHeight+" - surfaceHandle 0x"+Long.toHexString(getNativeSurface().getSurfaceHandle()));
            }
            final int lockRes = lockSurface();
            if (NativeSurface.LOCK_SURFACE_NOT_READY >= lockRes) {
                throw new NativeWindowException("Could not lock surface: "+this);
            }
            try {
                // propagate new size
                final ProxySurface ps = (ProxySurface) getNativeSurface();
                final UpstreamSurfaceHook ush = ps.getUpstreamSurfaceHook();
                if(ush instanceof UpstreamSurfaceHook.MutableSize) {
                    ((UpstreamSurfaceHook.MutableSize)ush).setSurfaceSize(newWidth, newHeight);
                } else {
                    throw new InternalError("GLFBODrawableImpl.ResizableImpl's ProxySurface doesn't hold a UpstreamSurfaceHookMutableSize but "+ush.getClass().getName()+", "+ps+", ush");
                }
                if( null != context && context.isCreated() ) {
                    resetSize(context.getGL());
                }
            } finally {
                unlockSurface();
            }
        }
    }
}
