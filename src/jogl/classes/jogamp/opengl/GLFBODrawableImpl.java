package jogamp.opengl;

import javax.media.nativewindow.NativeSurface;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;

import com.jogamp.nativewindow.MutableGraphicsConfiguration;
import com.jogamp.opengl.FBObject;
import com.jogamp.opengl.FBObject.Attachment;
import com.jogamp.opengl.FBObject.TextureAttachment;

/**
 * Offscreen GLDrawable implementation using framebuffer object (FBO)
 * as it's offscreen rendering mechanism.
 * 
 * @see GLDrawableImpl#contextRealized(GLContext, boolean)
 * @see GLDrawableImpl#contextMadeCurrent(GLContext, boolean)
 * @see GLDrawableImpl#getDefaultDrawFramebuffer()
 * @see GLDrawableImpl#getDefaultReadFramebuffer()
 */
public class GLFBODrawableImpl extends GLDrawableImpl {
    final GLDrawableImpl parent;
    final FBObject fbo;
    int texUnit;
    int samplesTexUnit = 0;
    int width=0, height=0, samples=0;
    
    protected GLFBODrawableImpl(GLDrawableFactoryImpl factory, GLDrawableImpl parent, 
                                NativeSurface surface, int initialWidth, int initialHeight, int textureUnit) {
        super(factory, surface, false);
        this.parent = parent;
        this.texUnit = textureUnit;
        final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) surface.getGraphicsConfiguration().getChosenCapabilities();
        this.width = initialWidth;
        this.height = initialHeight;
        this.samples = caps.getNumSamples();
        this.fbo = new FBObject();
    }
    
    @Override
    protected void contextRealized(GLContext glc, boolean realized) {
        final GLCapabilitiesImmutable caps = (GLCapabilitiesImmutable) surface.getGraphicsConfiguration().getChosenCapabilities();
        final GL gl = glc.getGL();
        if(realized) {                   
            fbo.reset(gl, width, height, samples);
            samples = fbo.getNumSamples(); // update, maybe capped
            if(samples > 0) {
                fbo.attachColorbuffer(gl, 0, caps.getAlphaBits()>0);
            } else {
                fbo.attachTexture2D(gl, 0, caps.getAlphaBits()>0);
            }
            if( caps.getStencilBits() > 0 ) {
                fbo.attachRenderbuffer(gl, Attachment.Type.DEPTH_STENCIL, 24);
            } else {
                fbo.attachRenderbuffer(gl, Attachment.Type.DEPTH, 24);
            }
        } else if(null != fbo) {
            fbo.destroy(gl);
        }
    }
    
    @Override
    protected void contextMadeCurrent(GLContext glc, boolean current) {
        final GL gl = glc.getGL();
        if(current) {
            fbo.bind(gl);
        } else {
            fbo.unbind(gl);
            gl.glActiveTexture(GL.GL_TEXTURE0 + texUnit);
            fbo.use(gl, samples > 0 ? fbo.getSamplingSink() : (TextureAttachment) fbo.getColorbuffer(0) );
            if( samples > 0) {
                gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, fbo.getReadFramebuffer());
            }
        }
    }
    
    @Override
    protected int getDefaultDrawFramebuffer() { return fbo.getWriteFramebuffer(); }
    
    @Override
    protected int getDefaultReadFramebuffer() { return fbo.getReadFramebuffer(); }
    
    public FBObject getFBObject() { return fbo; }
    
    public void setSize(GL gl, int newWidth, int newHeight) throws GLException {
        width = newWidth;
        height = newHeight;        
        fbo.reset(gl, width, height, samples);
        samples = fbo.getNumSamples(); // update, maybe capped
    }
    
    public void setSamples(GL gl, int newSamples) throws GLException {
        samples = newSamples;
        fbo.reset(gl, width, height, samples);
        samples = fbo.getNumSamples(); // update, maybe capped
    }
    
    
    @Override
    public GLContext createContext(GLContext shareWith) {
        final GLContext ctx = parent.createContext(shareWith);
        ctx.setGLDrawable(this, false);
        return ctx;
    }

    @Override
    public GLDynamicLookupHelper getGLDynamicLookupHelper() {
        return parent.getGLDynamicLookupHelper();
    }

    @Override
    protected void swapBuffersImpl() {
    }

    @Override
    protected void setRealizedImpl() {
        parent.setRealized(realized);
        if(realized) {    
            final MutableGraphicsConfiguration msConfig = (MutableGraphicsConfiguration) surface.getGraphicsConfiguration();
            final GLCapabilitiesImmutable chosenCaps = (GLCapabilitiesImmutable) msConfig.getChosenCapabilities();
            final GLCapabilitiesImmutable chosenFBOCaps = GLGraphicsConfigurationUtil.fixOffscreenGLCapabilities(chosenCaps, true /*FBO*/, false /*PBO*/);    
            msConfig.setChosenCapabilities(chosenFBOCaps);
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }    
}
