/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl;

import java.util.ArrayList;
import java.util.Arrays;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLBase;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import jogamp.opengl.Debug;

import com.jogamp.opengl.FBObject.Attachment.Type;

/**
 * Core utility class simplifying usage of framebuffer objects (FBO)
 * with all {@link GLProfile}s. 
 * <p>
 * Supports on-the-fly reconfiguration of dimension and multisample buffers via {@link #reset(GL, int, int, int)}
 * while preserving the {@link Attachment} references.
 * </p>
 * <p>
 * Integrates default read/write framebuffers via {@link GLContext#getDefaultReadFramebuffer()} and {@link GLContext#getDefaultReadFramebuffer()},
 * which is being hooked at {@link GL#glBindFramebuffer(int, int)} when the default (<code>zero</code>) framebuffer is selected.
 * </p>
 * 
 * <p>FIXME: Implement support for {@link Type#DEPTH_TEXTURE}, {@link Type#STENCIL_TEXTURE} .</p>
 */
public class FBObject {
    protected static final boolean DEBUG = Debug.debug("FBObject");
    
    /** 
     * Returns <code>true</code> if basic FBO support is available, otherwise <code>false</code>.
     * <p>
     * Basic FBO is supported if the context is either GL-ES >= 2.0, GL >= core 3.0 or implements the extensions
     * <code>ARB_ES2_compatibility</code>, <code>ARB_framebuffer_object</code>, <code>EXT_framebuffer_object</code> or <code>OES_framebuffer_object</code>.
     * </p>
     * <p>
     * Basic FBO support may only include one color attachment and no multisampling,
     * as well as limited internal formats for renderbuffer.
     * </p>
     * @see GLContext#hasFBO()
     */
    public static final boolean supportsBasicFBO(GL gl) {
        return gl.getContext().hasFBO();
    }
  
    /** 
     * Returns <code>true</code> if full FBO support is available, otherwise <code>false</code>.
     * <p>
     * Full FBO is supported if the context is either GL >= core 3.0 or implements the extensions
     * <code>ARB_framebuffer_object</code>, or all of
     * <code>EXT_framebuffer_object</code>, <code>EXT_framebuffer_multisample</code>, 
     * <code>EXT_framebuffer_blit</code>, <code>GL_EXT_packed_depth_stencil</code>.
     * </p>
     * <p>
     * Full FBO support includes multiple color attachments and multisampling.
     * </p>
     */
    public static final boolean supportsFullFBO(GL gl) {
        return gl.isGL3() ||                                                         // GL >= 3.0
                
               gl.isExtensionAvailable(GLExtensions.ARB_framebuffer_object) ||       // ARB_framebuffer_object
               
               ( gl.isExtensionAvailable(GLExtensions.EXT_framebuffer_object) &&     // All EXT_framebuffer_object*
                 gl.isExtensionAvailable(GLExtensions.EXT_framebuffer_multisample) &&
                 gl.isExtensionAvailable(GLExtensions.EXT_framebuffer_blit) &&
                 gl.isExtensionAvailable(GLExtensions.EXT_packed_depth_stencil) ) ;
    }
    
    public static final int getMaxSamples(GL gl) {
        if( supportsFullFBO(gl) ) {
            int[] val = new int[] { 0 } ;
            gl.glGetIntegerv(GL2GL3.GL_MAX_SAMPLES, val, 0);
            return val[0];
        } else {
            return 0;
        }
    }
    
    /** Common super class of all attachments */
    public static abstract class Attachment {
        public enum Type { 
            NONE, DEPTH, STENCIL, DEPTH_STENCIL, COLOR, COLOR_TEXTURE, DEPTH_TEXTURE, STENCIL_TEXTURE;
            
            /** 
             * Returns {@link #COLOR}, {@link #DEPTH}, {@link #STENCIL} or {@link #DEPTH_STENCIL}
             * @throws IllegalArgumentException if <code>format</code> cannot be handled. 
             */
            public static Type determine(int format) throws IllegalArgumentException {
                switch(format) {
                    case GL.GL_RGBA4:
                    case GL.GL_RGB5_A1:
                    case GL.GL_RGB565:
                    case GL.GL_RGB8:
                    case GL.GL_RGBA8:
                        return Type.COLOR;
                    case GL.GL_DEPTH_COMPONENT16:
                    case GL.GL_DEPTH_COMPONENT24:
                    case GL.GL_DEPTH_COMPONENT32:
                        return Type.DEPTH;
                    case GL.GL_STENCIL_INDEX1:
                    case GL.GL_STENCIL_INDEX4:
                    case GL.GL_STENCIL_INDEX8:
                        return Type.STENCIL;
                    case GL.GL_DEPTH24_STENCIL8:
                        return Type.DEPTH_STENCIL;
                    default:
                        throw new IllegalArgumentException("format invalid: 0x"+Integer.toHexString(format));
                }        
            }
        };
                
        /** immutable type [{@link #COLOR}, {@link #DEPTH}, {@link #STENCIL}, {@link #COLOR_TEXTURE}, {@link #DEPTH_TEXTURE}, {@link #STENCIL_TEXTURE} ] */
        public final Type type;
        
        /** immutable the internal format */
        public final int format;
        
        private int width, height;
        
        private int name;
        
        /** <code>true</code> if resource is initialized by {@link #initialize(GL)}, hence {@link #free(GL)} is allowed to free the GL resources. */
        protected boolean resourceOwner;
        
        private int initCounter;
        
        protected Attachment(Type type, int iFormat, int width, int height, int name) {
            this.type = type;
            this.format = iFormat;
            this.width = width;
            this.height = height;
            this.name = name;
            this.resourceOwner = false;
            this.initCounter = 0;
        }
        
        /** width of attachment */
        public final int getWidth() { return width; }
        /** height of attachment */
        public final int getHeight() { return height; }
        /* pp */ final void setSize(int w, int h) { width = w; height = h; }
        
        /** buffer name [1..max], maybe a texture or renderbuffer name, depending on type. */
        public final int getName() { return name; }        
        /* pp */ final void setName(int n) { name = n; }
        
        public final int getInitCounter() { return initCounter; }
        
        /** 
         * Initializes the attachment buffer and set it's parameter, if uninitialized, i.e. name is <code>zero</code>.
         * <p>Implementation employs an initialization counter, hence it can be paired recursively with {@link #free(GL)}.</p>
         * @throws GLException if buffer generation or setup fails. The just created buffer name will be deleted in this case. 
         */
        public void initialize(GL gl) throws GLException {
            initCounter++;
            /*
            super.initialize(gl);
            if(1 == getInitCounter() && 0 == getName()  ) {
                do init ..
                freeResources = true; // if all OK
            }
            */
        }
        
        /** 
         * Releases the attachment buffer if initialized, i.e. name is <code>zero</code>.
         * <p>Implementation employs an initialization counter, hence it can be paired recursively with {@link #initialize(GL)}.</p>
         * @throws GLException if buffer release fails. 
         */
        public void free(GL gl) throws GLException {
            /*
            if(1 == getInitCounter() && freeResources && .. ) {
                do free ..
            }
            super.free(gl);
            */
            initCounter--;
            if(0 == initCounter) {
                resourceOwner = false;
                name = 0;
            }
            if(DEBUG) {
                System.err.println("Attachment.free: "+this);
            }
        }
        
        /**
         * <p>
         * Comparison by {@link #type}, {@link #format}, {@link #width}, {@link #height} and {@link #name}.
         * </p>
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if( this == o ) return true;
            if( ! ( o instanceof Attachment ) ) return false;
            final Attachment a = (Attachment)o;
            return type == a.type &&
                   format == a.format ||
                   width == a.width   ||
                   height== a.height  ||
                   name == a.name     ;
        }
        
        /**
         * <p>
         * Hashed by {@link #type}, {@link #format}, {@link #width}, {@link #height} and {@link #name}.
         * </p>
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            // 31 * x == (x << 5) - x
            int hash = 31 + type.ordinal();
            hash = ((hash << 5) - hash) + format;
            hash = ((hash << 5) - hash) + width;
            hash = ((hash << 5) - hash) + height;
            hash = ((hash << 5) - hash) + name;
            return hash;
        }
        
        int objectHashCode() { return super.hashCode(); }
        
        public String toString() {
            return getClass().getSimpleName()+"[type "+type+", format 0x"+Integer.toHexString(format)+", "+width+"x"+height+
                   ", name 0x"+Integer.toHexString(name)+", obj 0x"+Integer.toHexString(objectHashCode())+
                   ", resOwner "+resourceOwner+", initCount "+initCounter+"]";
        }
        
        public static Type getType(int attachmentPoint, int maxColorAttachments) {
            if( GL.GL_COLOR_ATTACHMENT0 <= attachmentPoint && attachmentPoint < GL.GL_COLOR_ATTACHMENT0+maxColorAttachments ) {
                return Type.COLOR;
            }
            switch(attachmentPoint) {
                case GL.GL_DEPTH_ATTACHMENT:
                    return Type.DEPTH;
                case GL.GL_STENCIL_ATTACHMENT:            
                    return Type.STENCIL;
                default: 
                    throw new IllegalArgumentException("Invalid attachment point 0x"+Integer.toHexString(attachmentPoint));
            }
        }
    }
    
    /** Other renderbuffer attachment which maybe a colorbuffer, depth or stencil. */
    public static class RenderAttachment extends Attachment {
        private int samples;
        
        /**
         * @param type allowed types are {@link Type#DEPTH}, {@link Type#STENCIL} or {@link Type#COLOR}
         * @param iFormat
         * @param samples
         * @param width
         * @param height
         * @param name
         */
        public RenderAttachment(Type type, int iFormat, int samples, int width, int height, int name) {
            super(validateType(type), iFormat, width, height, name);
            this.samples = samples;
        }
        
        /** number of samples, or zero for no multisampling */
        public final int getSamples() { return samples; }
        /* pp */ final void setSamples(int s) { samples = s; }
        
        private static Type validateType(Type type) {
            switch(type) {
                case DEPTH:
                case STENCIL:
                case COLOR:
                    return type;
                default: 
                    throw new IllegalArgumentException("Invalid type: "+type);
            }
        }
        
        /**
         * <p>
         * Comparison by {@link #type}, {@link #format}, {@link #samples}, {@link #width}, {@link #height} and {@link #name}.
         * </p>
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object o) {
            if( this == o ) return true;
            if( ! ( o instanceof RenderAttachment ) ) return false;
            return super.equals(o) &&
                   samples == ((RenderAttachment)o).samples;
        }
        
        /**
         * <p>
         * Hashed by {@link #type}, {@link #format}, {@link #samples}, {@link #width}, {@link #height} and {@link #name}.
         * </p>
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            // 31 * x == (x << 5) - x
            int hash = super.hashCode();
            hash = ((hash << 5) - hash) + samples;
            return hash;
        }

        @Override
        public void initialize(GL gl) throws GLException {
            super.initialize(gl);
            if( 1 == getInitCounter() && 0 == getName() ) {
                final int[] name = new int[] { -1 };
                gl.glGenRenderbuffers(1, name, 0);
                if( 0 == name[0] ) {
                    throw new GLException("null renderbuffer, "+this);
                }
                setName(name[0]);
                
                gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, getName());
                if( samples > 0 ) {
                    ((GL2GL3)gl).glRenderbufferStorageMultisample(GL.GL_RENDERBUFFER, samples, format, getWidth(), getHeight());            
                } else {
                    gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, format, getWidth(), getHeight());
                }
                int glerr = gl.glGetError();
                if(GL.GL_NO_ERROR != glerr) {
                    gl.glDeleteRenderbuffers(1, name, 0);
                    setName(0);
                    throw new GLException("GL Error 0x"+Integer.toHexString(glerr)+" while creating "+this);
                }
                resourceOwner = true;
                if(DEBUG) {
                    System.err.println("Attachment.init: "+this);
                }
            }
        }
        
        @Override
        public void free(GL gl) {
            if(1 == getInitCounter() && resourceOwner && 0 != getName() ) {
                final int[] name = new int[] { getName() };
                gl.glDeleteRenderbuffers(1, name, 0);
            }
            super.free(gl);
        }
        
        public String toString() {
            return getClass().getSimpleName()+"[type "+type+", format 0x"+Integer.toHexString(format)+", samples "+samples+", "+getWidth()+"x"+getHeight()+
                   ", name 0x"+Integer.toHexString(getName())+", obj 0x"+Integer.toHexString(objectHashCode())+
                   ", resOwner "+resourceOwner+", initCount "+getInitCounter()+"]";
        }
    }
    
    /** 
     * Marker interface, denotes a color buffer attachment.
     * <p>Always an instance of {@link Attachment}.</p>
     * <p>Either an instance of {@link ColorAttachment} or {@link TextureAttachment}.</b> 
     */
    public static interface Colorbuffer {        
    }
    
    /** Color render buffer attachment  */
    public static class ColorAttachment extends RenderAttachment implements Colorbuffer {
        public ColorAttachment(int iFormat, int samples, int width, int height, int name) {
            super(Type.COLOR, iFormat, samples, width, height, name);
        }    
    }
    
    /** Texture attachment */
    public static class TextureAttachment extends Attachment implements Colorbuffer  {
        /** details of the texture setup */
        public final int dataFormat, dataType, magFilter, minFilter, wrapS, wrapT;

        /**
         * @param type allowed types are [ {@link Type#COLOR_TEXTURE}, {@link Type#DEPTH_TEXTURE}, {@link Type#STENCIL_TEXTURE} ]
         * @param iFormat
         * @param width
         * @param height
         * @param dataFormat
         * @param dataType
         * @param magFilter
         * @param minFilter
         * @param wrapS
         * @param wrapT
         * @param name
         */
        public TextureAttachment(Type type, int iFormat, int width, int height, int dataFormat, int dataType, 
                                 int magFilter, int minFilter, int wrapS, int wrapT, int name) {
            super(validateType(type), iFormat, width, height, name);
            this.dataFormat = dataFormat;
            this.dataType = dataType;
            this.magFilter = magFilter;
            this.minFilter = minFilter;
            this.wrapS = wrapS;
            this.wrapT = wrapT;
        }
        
        private static Type validateType(Type type) {
            switch(type) {
                case COLOR_TEXTURE:
                case DEPTH_TEXTURE:
                case STENCIL_TEXTURE:
                    return type;
                default: 
                    throw new IllegalArgumentException("Invalid type: "+type);
            }
        }
        
        /** 
         * Initializes the texture and set it's parameter, if uninitialized, i.e. name is <code>zero</code>.
         * @throws GLException if texture generation and setup fails. The just created texture name will be deleted in this case. 
         */
        @Override
        public void initialize(GL gl) throws GLException {
            super.initialize(gl);
            if( 1 == getInitCounter() && 0 == getName() ) {
                final int[] name = new int[] { -1 };            
                gl.glGenTextures(1, name, 0);
                if(0 == name[0]) {
                    throw new GLException("null texture, "+this);
                }
                setName(name[0]);
                
                gl.glBindTexture(GL.GL_TEXTURE_2D, name[0]);
                gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, format, getWidth(), getHeight(), 0, dataFormat, dataType, null);
                if( 0 < magFilter ) {
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, magFilter);
                }
                if( 0 < minFilter ) {
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, minFilter);
                }
                if( 0 < wrapS ) {
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, wrapS);
                }
                if( 0 < wrapT ) {
                    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, wrapT);            
                }        
                int glerr = gl.glGetError();
                if(GL.GL_NO_ERROR != glerr) {
                    gl.glDeleteTextures(1, name, 0);
                    setName(0);
                    throw new GLException("GL Error 0x"+Integer.toHexString(glerr)+" while creating "+this);
                }
                resourceOwner = true;
            }
            if(DEBUG) {
                System.err.println("Attachment.init: "+this);
            }
        }

        @Override
        public void free(GL gl) {
            if(1 == getInitCounter() && resourceOwner && 0 != getName() ) {
                final int[] name = new int[] { getName() };
                gl.glDeleteTextures(1, name, 0);
            }
            super.free(gl);
        }
    }
    
    private boolean initialized;
    private boolean basicFBOSupport;
    private boolean fullFBOSupport;
    private boolean rgba8Avail;
    private boolean depth24Avail;
    private boolean depth32Avail;
    private boolean stencil01Avail;
    private boolean stencil04Avail;
    private boolean stencil08Avail;
    private boolean stencil16Avail;
    private boolean packedDepthStencilAvail;            
    private int maxColorAttachments, maxSamples, maxTextureSize, maxRenderbufferSize;

    private int width, height, samples;
    private int vStatus;
    private int fbName;
    private boolean bound;

    private int colorAttachmentCount;
    private Colorbuffer[] colorAttachmentPoints; // colorbuffer attachment points 
    private RenderAttachment depth, stencil; // depth and stencil maybe equal in case of packed-depth-stencil

    private final FBObject samplesSink; // MSAA sink
    private TextureAttachment samplesSinkTexture; 
    private boolean samplesSinkDirty;

    //
    // ColorAttachment helper ..
    //
    
    private final void validateColorAttachmentPointRange(int point) {
        if(maxColorAttachments != colorAttachmentPoints.length) {
            throw new InternalError("maxColorAttachments "+maxColorAttachments+", array.lenght "+colorAttachmentPoints);
        }
        if(0 > point || point >= maxColorAttachments) {
            throw new IllegalArgumentException("attachment point out of range: "+point+", should be within [0.."+(maxColorAttachments-1)+"]");
        }
    }
    
    private final void validateAddColorAttachment(int point, Colorbuffer ca) {
        validateColorAttachmentPointRange(point);
        if( null != colorAttachmentPoints[point] ) {
            throw new IllegalArgumentException("Cannot attach "+ca+", attachment point already in use by "+colorAttachmentPoints[point]);
        }        
    }
    
    private final void addColorAttachment(int point, Colorbuffer ca) {
        validateColorAttachmentPointRange(point);
        final Colorbuffer c = colorAttachmentPoints[point];
        if( null != c && c != ca ) {
            throw new IllegalArgumentException("Add failed: requested to add "+ca+" at "+point+", but slot is holding "+c+"; "+this);
        }
        colorAttachmentPoints[point] = ca;
        colorAttachmentCount++;
    }
    
    private final void removeColorAttachment(int point, Colorbuffer ca) {
        validateColorAttachmentPointRange(point);
        final Colorbuffer c = colorAttachmentPoints[point];
        if( null != c && c != ca ) {
            throw new IllegalArgumentException("Remove failed: requested to removed "+ca+" at "+point+", but slot is holding "+c+"; "+this);
        }
        colorAttachmentPoints[point] = null;
        colorAttachmentCount--;
    }
    
    /**
     * Return the {@link Colorbuffer} attachment at <code>attachmentPoint</code> if it is attached to this FBO, otherwise null.
     * 
     * @see #attachColorbuffer(GL, boolean)
     * @see #attachColorbuffer(GL, boolean)
     * @see #attachTexture2D(GL, int, boolean, int, int, int, int)
     * @see #attachTexture2D(GL, int, int, int, int, int, int, int, int) 
     */
    public final Colorbuffer getColorbuffer(int attachmentPoint) {
        validateColorAttachmentPointRange(attachmentPoint);        
        return colorAttachmentPoints[attachmentPoint];
    }
    
    /**
     * Finds the passed {@link Colorbuffer} within the valid range of attachment points
     * using <i>reference</i> comparison only.
     * <p>
     * Note: Slow. Implementation uses a logN array search to save resources, i.e. not using a HashMap.
     * </p>
     * @param ca the {@link Colorbuffer} to look for.
     * @return -1 if the {@link Colorbuffer} could not be found, otherwise [0..{@link #getMaxColorAttachments()}-1] 
     */
    public final int getColorbufferAttachmentPoint(Colorbuffer ca) {
        for(int i=0; i<colorAttachmentPoints.length; i++) {
            if( colorAttachmentPoints[i] == ca ) {
                return i; 
            }
        }
        return -1;
    }
    
    /**
     * Returns the passed {@link Colorbuffer} if it is attached to this FBO, otherwise null.
     * Implementation compares the <i>reference</i> only.
     * 
     * <p>
     * Note: Slow. Uses {@link #getColorbufferAttachmentPoint(Colorbuffer)} to determine it's attachment point
     *       to be used for {@link #getColorbuffer(int)}
     * </p>
     * 
     * @see #attachColorbuffer(GL, boolean)
     * @see #attachColorbuffer(GL, boolean)
     * @see #attachTexture2D(GL, int, boolean, int, int, int, int)
     * @see #attachTexture2D(GL, int, int, int, int, int, int, int, int) 
     */
    public final Colorbuffer getColorbuffer(Colorbuffer ca) {
        final int p = getColorbufferAttachmentPoint(ca);
        return p>=0 ? getColorbuffer(p) : null;
    }
        
    /**
     * Creates an uninitialized FBObject instance.
     * <p>
     * Call {@link #init(GL, int, int, int)} .. etc to use it.
     * </p>
     */
    public FBObject() {
        this(false);
    }
    /* pp */ FBObject(boolean isSampleSink) {
        this.initialized = false;
        
        // TBD @ init
        this.basicFBOSupport = false;
        this.fullFBOSupport = false;
        this.rgba8Avail = false;
        this.depth24Avail = false;
        this.depth32Avail = false;
        this.stencil01Avail = false;
        this.stencil04Avail = false;
        this.stencil08Avail = false;
        this.stencil16Avail = false;
        this.packedDepthStencilAvail = false;
        this.maxColorAttachments=-1;
        this.maxSamples=-1;
        this.maxTextureSize = 0;
        this.maxRenderbufferSize = 0;
        
        this.width = 0;
        this.height = 0;
        this.samples = 0;
        this.vStatus = -1;        
        this.fbName = 0;
        this.bound = false;
        
        this.colorAttachmentPoints = null; // at init ..
        this.colorAttachmentCount = 0;
        this.depth = null;
        this.stencil = null;                
        
        this.samplesSink = isSampleSink ? null : new FBObject(true);
        this.samplesSinkTexture = null;
        this.samplesSinkDirty = true;
    }
    
    private void init(GL gl, int width, int height, int samples) throws GLException {
        if(initialized) {
            throw new GLException("FBO already initialized");
        }        
        fullFBOSupport = supportsFullFBO(gl);
        
        if( !fullFBOSupport && !supportsBasicFBO(gl) ) {
            throw new GLException("FBO not supported w/ context: "+gl.getContext()+", "+this);
        }
        
        basicFBOSupport = true;
        
        rgba8Avail = gl.isGL2GL3() || gl.isExtensionAvailable(GLExtensions.OES_rgb8_rgba8);
        depth24Avail = fullFBOSupport || gl.isExtensionAvailable(GLExtensions.OES_depth24);
        depth32Avail = fullFBOSupport || gl.isExtensionAvailable(GLExtensions.OES_depth32);
        stencil01Avail = fullFBOSupport || gl.isExtensionAvailable(GLExtensions.OES_stencil1);
        stencil04Avail = fullFBOSupport || gl.isExtensionAvailable(GLExtensions.OES_stencil4);
        stencil08Avail = fullFBOSupport || gl.isExtensionAvailable(GLExtensions.OES_stencil8);
        stencil16Avail = fullFBOSupport;
        
        packedDepthStencilAvail = fullFBOSupport || gl.isExtensionAvailable(GLExtensions.OES_packed_depth_stencil);
        
        final boolean NV_fbo_color_attachments = gl.isExtensionAvailable(GLExtensions.NV_fbo_color_attachments);
                                
        int val[] = new int[1];
        
        int glerr = gl.glGetError();
        if(DEBUG && GL.GL_NO_ERROR != glerr) {
            System.err.println("FBObject.init-preexisting.0 GL Error 0x"+Integer.toHexString(glerr));
        }

        int realMaxColorAttachments = 1;
        maxColorAttachments = 1;
        if( null != samplesSink && fullFBOSupport || NV_fbo_color_attachments ) {
            try {
                gl.glGetIntegerv(GL2GL3.GL_MAX_COLOR_ATTACHMENTS, val, 0);
                glerr = gl.glGetError();
                if(GL.GL_NO_ERROR == glerr) {
                    realMaxColorAttachments = 1 <= val[0] ? val[0] : 1; // cap minimum to 1
                } else if(DEBUG) {
                    System.err.println("FBObject.init-GL_MAX_COLOR_ATTACHMENTS query GL Error 0x"+Integer.toHexString(glerr));
                }
            } catch (GLException gle) {}
        }
        maxColorAttachments = realMaxColorAttachments <= 8 ? realMaxColorAttachments : 8; // cap to limit array size
        
        colorAttachmentPoints = new Colorbuffer[maxColorAttachments];
        colorAttachmentCount = 0;
        
        maxSamples = 0;
        if(fullFBOSupport) {
            gl.glGetIntegerv(GL2GL3.GL_MAX_SAMPLES, val, 0);
            glerr = gl.glGetError();
            if(GL.GL_NO_ERROR == glerr) {
                maxSamples = val[0];
            } else if(DEBUG) {
                System.err.println("FBObject.init-GL_MAX_SAMPLES query GL Error 0x"+Integer.toHexString(glerr));
            }
        }
        gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, val, 0);
        maxTextureSize = val[0];
        gl.glGetIntegerv(GL.GL_MAX_RENDERBUFFER_SIZE, val, 0);
        this.maxRenderbufferSize = val[0];
        
        glerr = gl.glGetError();
        if(DEBUG && GL.GL_NO_ERROR != glerr) {
            System.err.println("FBObject.init-preexisting.1 GL Error 0x"+Integer.toHexString(glerr));
        }
        
        this.width = width;
        this.height = height;
        this.samples = samples <= maxSamples ? samples : maxSamples;
        
        if(DEBUG) {
            System.err.println("FBObject "+width+"x"+height+", "+samples+" -> "+samples+" samples");
            System.err.println("basicFBOSupport:          "+basicFBOSupport);
            System.err.println("fullFBOSupport:           "+fullFBOSupport);
            System.err.println("maxColorAttachments:      "+maxColorAttachments+"/"+realMaxColorAttachments);
            System.err.println("maxSamples:               "+maxSamples);
            System.err.println("maxTextureSize:           "+maxTextureSize);
            System.err.println("maxRenderbufferSize:      "+maxRenderbufferSize);
            System.err.println("rgba8:                    "+rgba8Avail);
            System.err.println("depth24:                  "+depth24Avail);
            System.err.println("depth32:                  "+depth32Avail);
            System.err.println("stencil01:                "+stencil01Avail);
            System.err.println("stencil04:                "+stencil04Avail);
            System.err.println("stencil08:                "+stencil08Avail);
            System.err.println("stencil16:                "+stencil16Avail);
            System.err.println("packedDepthStencil:       "+packedDepthStencilAvail);
            System.err.println("NV_fbo_color_attachments: "+NV_fbo_color_attachments);
            System.err.println(gl.getContext().getGLVersion());
            System.err.println(JoglVersion.getGLStrings(gl, null).toString());
            System.err.println(gl.getContext());
        }
        
        checkNoError(null, gl.glGetError(), "FBObject Init.pre"); // throws GLException if error
        
        if(width > 2 + maxTextureSize  || height> 2 + maxTextureSize ||
           width > maxRenderbufferSize || height> maxRenderbufferSize  ) {
            throw new GLException("size "+width+"x"+height+" exceeds on of the maxima [texture "+maxTextureSize+", renderbuffer "+maxRenderbufferSize+"]");
        }

        if(null != samplesSink) {        
            // init sampling sink
            samplesSink.reset(gl, width, height);
            resetMSAATexture2DSink(gl);
        }

        // generate fbo ..
        gl.glGenFramebuffers(1, val, 0);
        fbName = val[0];
        if(0 == fbName) {
            throw new GLException("null framebuffer");
        }

        // bind fbo ..
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fbName);        
        checkNoError(gl, gl.glGetError(), "FBObject Init.bindFB");  // throws GLException if error        
        if(!gl.glIsFramebuffer(fbName)) {
            checkNoError(gl, GL.GL_INVALID_VALUE, "FBObject Init.isFB"); // throws GLException
        }
        bound = true;        
        initialized = true;
        
        updateStatus(gl);
        if(DEBUG) {
            System.err.println("FBObject.init(): "+this);
        }
    }

    /**
     * Initializes or resets this FBO's instance.
     * <p>
     * In case the new parameters are compatible with the current ones
     * no action will be performed. Otherwise all attachments will be recreated
     * to match the new given parameters.
     * </p>
     * <p>
     * Currently incompatibility and hence recreation is given if
     * the size or sample count doesn't match for subsequent calls.
     * </p>
     * 
     * <p>Leaves the FBO bound state untouched</p>
     * 
     * @param gl the current GL context
     * @param newWidth
     * @param newHeight
     * @throws GLException in case of an error
     */
    public final void reset(GL gl, int newWidth, int newHeight) {
        reset(gl, newWidth, newHeight, 0);
    }
    
    /**
     * Initializes or resets this FBO's instance.
     * <p>
     * In case the new parameters are compatible with the current ones
     * no action will be performed. Otherwise all attachments will be recreated
     * to match the new given parameters.
     * </p>
     * <p>
     * Currently incompatibility and hence recreation is given if
     * the size or sample count doesn't match for subsequent calls.
     * </p>
     * 
     * <p>Leaves the FBO bound state untouched</p>
     * 
     * @param gl the current GL context
     * @param newWidth
     * @param newHeight
     * @param newSamples if > 0, MSAA will be used, otherwise no multisampling. Will be capped to {@link #getMaxSamples()}.
     * @throws GLException in case of an error
     */
    public final void reset(GL gl, int newWidth, int newHeight, int newSamples) {
        if(!initialized) {
            init(gl, newWidth, newHeight, newSamples);
            return;
        }
        newSamples = newSamples <= maxSamples ? newSamples : maxSamples; // clamp
        
        if( newWidth !=  width || newHeight !=  height || newSamples != samples ) {
            if(DEBUG) {
                System.err.println("FBObject.reset - START - "+this);
            }        
            
            final boolean wasBound = isBound();
            
            width = newWidth;
            height = newHeight;
            samples = newSamples;
            detachAllImpl(gl, true , true);
            resetMSAATexture2DSink(gl);
            
            if(wasBound) {
                bind(gl);
            } else {
                unbind(gl);
            }
            
            if(DEBUG) {
                System.err.println("FBObject.reset - END - "+this);
            }
        }        
    }
            
    /** 
     * Note that the status may reflect an incomplete state during transition of attachments.
     * @return The FB status. {@link GL.GL_FRAMEBUFFER_COMPLETE} if ok, otherwise return GL FBO error state or -1
     * @see #validateStatus() 
     */
    public final int getStatus() {
        return vStatus;
    }

    /** return the {@link #getStatus()} as a string. */
    public final String getStatusString() {
        return getStatusString(vStatus);
    }
    
    public static final String getStatusString(int fbStatus) {
        switch(fbStatus) {
            case -1:
                return "NOT A FBO";
                
            case GL.GL_FRAMEBUFFER_COMPLETE:
                return "OK";
                
            case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                return("GL FBO: incomplete, incomplete attachment\n");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                return("GL FBO: incomplete, missing attachment");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                return("GL FBO: incomplete, attached images must have same dimensions");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
                 return("GL FBO: incomplete, attached images must have same format");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                return("GL FBO: incomplete, missing draw buffer");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                return("GL FBO: incomplete, missing read buffer");
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
                return("GL FBO: incomplete, missing multisample buffer");
            case GL3.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:                
                return("GL FBO: incomplete, layer targets");
                
            case GL.GL_FRAMEBUFFER_UNSUPPORTED:
                return("GL FBO: Unsupported framebuffer format");
            case GL2GL3.GL_FRAMEBUFFER_UNDEFINED:
                 return("GL FBO: framebuffer undefined");
                 
            case 0:
                return("GL FBO: incomplete, implementation fault");
            default:
                return("GL FBO: incomplete, implementation ERROR 0x"+Integer.toHexString(fbStatus));
        }
    }
    
    /**
     * The status may even be valid if incomplete during transition of attachments.
     * @see #getStatus()
     */
    public final boolean isStatusValid() {
        switch(vStatus) {
            case GL.GL_FRAMEBUFFER_COMPLETE:
                return true;
                
            case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
            case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
            case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
            case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
            case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_MULTISAMPLE:
            case GL3.GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS:
                if(0 == colorAttachmentCount || null == depth) {
                    // we are in transition
                    return true;
                }
         
            case GL.GL_FRAMEBUFFER_UNSUPPORTED:
            case GL2GL3.GL_FRAMEBUFFER_UNDEFINED:
                
            case 0:                
            default:
                System.out.println("Framebuffer " + fbName + " is incomplete: status = 0x" + Integer.toHexString(vStatus) + 
                        " : " + getStatusString(vStatus));
                return false;
        }
    }
        
    private final boolean checkNoError(GL gl, int err, String exceptionMessage) throws GLException {
        if(GL.GL_NO_ERROR != err) {
            if(null != gl) {
                destroy(gl);
            }
            if(null != exceptionMessage) {
                throw new GLException(exceptionMessage+" GL Error 0x"+Integer.toHexString(err));
            }
            return false;
        }
        return true;
    }

    private final void checkInitialized() throws GLException {
        if(!initialized) {
            throw new GLException("FBO not initialized, call init(GL) first.");
        }                
    }
    
    /**
     * Attaches a Texture2D Color Buffer to this FBO's instance at the given attachment point,
     * selecting the texture data type and format automatically.
     * 
     * <p>Using default min/mag filter {@link GL#GL_NEAREST} and default wrapS/wrapT {@link GL#GL_CLAMP_TO_EDGE}.</p>
     * 
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl the current GL context
     * @param attachmentPoint the color attachment point ranging from [0..{@link #getMaxColorAttachments()}-1]
     * @param alpha set to <code>true</code> if you request alpha channel, otherwise <code>false</code>;
     * @return TextureAttachment instance describing the new attached texture colorbuffer if bound and configured successfully, otherwise GLException is thrown
     * @throws GLException in case the texture colorbuffer couldn't be allocated or MSAA has been chosen
     */
    public final TextureAttachment attachTexture2D(GL gl, int attachmentPoint, boolean alpha) throws GLException {
        return attachTexture2D(gl, attachmentPoint, alpha, GL.GL_NEAREST, GL.GL_NEAREST, GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE);
    }
    
    /**
     * Attaches a Texture2D Color Buffer to this FBO's instance at the given attachment point,
     * selecting the texture data type and format automatically.
     * 
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl the current GL context
     * @param attachmentPoint the color attachment point ranging from [0..{@link #getMaxColorAttachments()}-1]
     * @param alpha set to <code>true</code> if you request alpha channel, otherwise <code>false</code>;
     * @param magFilter if > 0 value for {@link GL#GL_TEXTURE_MAG_FILTER}
     * @param minFilter if > 0 value for {@link GL#GL_TEXTURE_MIN_FILTER} 
     * @param wrapS if > 0 value for {@link GL#GL_TEXTURE_WRAP_S}
     * @param wrapT if > 0 value for {@link GL#GL_TEXTURE_WRAP_T}
     * @return TextureAttachment instance describing the new attached texture colorbuffer if bound and configured successfully, otherwise GLException is thrown
     * @throws GLException in case the texture colorbuffer couldn't be allocated or MSAA has been chosen
     */
    public final TextureAttachment attachTexture2D(GL gl, int attachmentPoint, boolean alpha, int magFilter, int minFilter, int wrapS, int wrapT) throws GLException {
        final int textureInternalFormat, textureDataFormat, textureDataType;
        if(gl.isGLES()) { 
            textureInternalFormat = alpha ? GL.GL_RGBA : GL.GL_RGB;
            textureDataFormat = alpha ? GL.GL_RGBA : GL.GL_RGB;
            textureDataType = GL.GL_UNSIGNED_BYTE;
        } else { 
            textureInternalFormat = alpha ? GL.GL_RGBA8 : GL.GL_RGB8;
            textureDataFormat = alpha ? GL.GL_BGRA : GL.GL_RGB;
            textureDataType = alpha ? GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV : GL.GL_UNSIGNED_BYTE;
        }
        return attachTexture2D(gl, attachmentPoint, textureInternalFormat, textureDataFormat, textureDataType, magFilter, minFilter, wrapS, wrapT);
    }
    
    /**
     * Attaches a Texture2D Color Buffer to this FBO's instance at the given attachment point.
     * 
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl the current GL context
     * @param attachmentPoint the color attachment point ranging from [0..{@link #getMaxColorAttachments()}-1]
     * @param internalFormat internalFormat parameter to {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, long)}
     * @param dataFormat format parameter to {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, long)}
     * @param dataType type parameter to {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, long)}
     * @param magFilter if > 0 value for {@link GL#GL_TEXTURE_MAG_FILTER}
     * @param minFilter if > 0 value for {@link GL#GL_TEXTURE_MIN_FILTER} 
     * @param wrapS if > 0 value for {@link GL#GL_TEXTURE_WRAP_S}
     * @param wrapT if > 0 value for {@link GL#GL_TEXTURE_WRAP_T}
     * @return TextureAttachment instance describing the new attached texture colorbuffer if bound and configured successfully, otherwise GLException is thrown
     * @throws GLException in case the texture colorbuffer couldn't be allocated or MSAA has been chosen
     */
    public final TextureAttachment attachTexture2D(GL gl, int attachmentPoint,
                                                   int internalFormat, int dataFormat, int dataType,
                                                   int magFilter, int minFilter, int wrapS, int wrapT) throws GLException {
        return attachTexture2D(gl, attachmentPoint, 
                               new TextureAttachment(Type.COLOR_TEXTURE, internalFormat, width, height, dataFormat, dataType, 
                                                     magFilter, minFilter, wrapS, wrapT, 0 /* name */));
    }
    
    /**
     * Attaches a Texture2D Color Buffer to this FBO's instance at the given attachment point.
     *  
     * <p>
     * In case the passed TextureAttachment <code>texA</code> is uninitialized, i.e. it's texture name is <code>zero</code>,
     * a new texture name is generated and setup w/ the texture parameter.<br/>
     * Otherwise, i.e. texture name is not <code>zero</code>, the passed TextureAttachment <code>texA</code> is
     * considered complete and assumed matching this FBO requirement. A GL error may occur is the latter is untrue. 
     * </p>
     *    
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl the current GL context
     * @param attachmentPoint the color attachment point ranging from [0..{@link #getMaxColorAttachments()}-1]
     * @param texA the to be attached {@link TextureAttachment}. Maybe complete or uninitialized, see above. 
     * @return the passed TextureAttachment <code>texA</code> instance describing the new attached texture colorbuffer if bound and configured successfully, otherwise GLException is thrown
     * @throws GLException in case the texture colorbuffer couldn't be allocated or MSAA has been chosen
     */
    public final TextureAttachment attachTexture2D(GL gl, int attachmentPoint, TextureAttachment texA) throws GLException {
        validateAddColorAttachment(attachmentPoint, texA);
        
        if(samples>0) {
            removeColorAttachment(attachmentPoint, texA);
            throw new GLException("Texture2D not supported w/ MSAA. If you have enabled MSAA with exisiting texture attachments, you may want to detach them via detachAllTexturebuffer(gl).");
        }
        
        texA.initialize(gl);
        addColorAttachment(attachmentPoint, texA);
                
        bind(gl);

        // Set up the color buffer for use as a renderable texture:
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
                                  GL.GL_COLOR_ATTACHMENT0 + attachmentPoint,
                                  GL.GL_TEXTURE_2D, texA.getName(), 0);
        updateStatus(gl);
        
        if(!isStatusValid()) {
            detachColorbuffer(gl, attachmentPoint);
            throw new GLException("attachTexture2D "+texA+" at "+attachmentPoint+" failed "+getStatusString()+", "+this);
        }
        if(DEBUG) {
            System.err.println("FBObject.attachTexture2D: "+this);
        }
        return texA;
    }
    
    /**
     * Attaches a Color Buffer to this FBO's instance at the given attachment point,
     * selecting the format automatically.
     *  
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl the current GL context
     * @param attachmentPoint the color attachment point ranging from [0..{@link #getMaxColorAttachments()}-1]
     * @param alpha set to <code>true</code> if you request alpha channel, otherwise <code>false</code>;
     * @return ColorAttachment instance describing the new attached colorbuffer if bound and configured successfully, otherwise GLException is thrown
     * @throws GLException in case the colorbuffer couldn't be allocated
     */
    public final ColorAttachment attachColorbuffer(GL gl, int attachmentPoint, boolean alpha) throws GLException {
        final int internalFormat;
        if( rgba8Avail ) {
            internalFormat = alpha ? GL.GL_RGBA8 : GL.GL_RGB8 ;
        } else {
            internalFormat = alpha ? GL.GL_RGBA4 : GL.GL_RGB565;
        }
        return attachColorbuffer(gl, attachmentPoint, internalFormat);
    }
    
    /**
     * Attaches a Color Buffer to this FBO's instance at the given attachment point.
     *  
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl the current GL context
     * @param attachmentPoint the color attachment point ranging from [0..{@link #getMaxColorAttachments()}-1]
     * @param internalFormat usually {@link GL#GL_RGBA4}, {@link GL#GL_RGB5_A1}, {@link GL#GL_RGB565}, {@link GL#GL_RGB8} or {@link GL#GL_RGBA8}  
     * @return ColorAttachment instance describing the new attached colorbuffer if bound and configured successfully, otherwise GLException is thrown
     * @throws GLException in case the colorbuffer couldn't be allocated
     * @throws IllegalArgumentException if <code>internalFormat</code> doesn't reflect a colorbuffer
     */
    public final ColorAttachment attachColorbuffer(GL gl, int attachmentPoint, int internalFormat) throws GLException, IllegalArgumentException {
        final Attachment.Type atype = Attachment.Type.determine(internalFormat);
        if( Attachment.Type.COLOR != atype ) {
            throw new IllegalArgumentException("colorformat invalid: 0x"+Integer.toHexString(internalFormat)+", "+this);
        }
        
        return attachColorbuffer(gl, attachmentPoint, new ColorAttachment(internalFormat, samples, width, height, 0));
    }
    
    /**
     * Attaches a Color Buffer to this FBO's instance at the given attachment point.
     *  
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl
     * @param attachmentPoint the color attachment point ranging from [0..{@link #getMaxColorAttachments()}-1]
     * @param colA the template for the new {@link ColorAttachment}   
     * @return ColorAttachment instance describing the new attached colorbuffer if bound and configured successfully, otherwise GLException is thrown
     * @throws GLException in case the colorbuffer couldn't be allocated
     */
    public final ColorAttachment attachColorbuffer(GL gl, int attachmentPoint, ColorAttachment colA) throws GLException {
        validateAddColorAttachment(attachmentPoint, colA);
        
        colA.initialize(gl);        
        addColorAttachment(attachmentPoint, colA);
        
        bind(gl);
                
        // Attach the color buffer
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, 
                                     GL.GL_COLOR_ATTACHMENT0 + attachmentPoint, 
                                     GL.GL_RENDERBUFFER, colA.getName());

        updateStatus(gl);                
        if(!isStatusValid()) {
            detachColorbuffer(gl, attachmentPoint);
            throw new GLException("attachColorbuffer "+colA+" at "+attachmentPoint+" failed "+getStatusString()+", "+this);
        }
        if(DEBUG) {
            System.err.println("FBObject.attachColorbuffer: "+this);
        }
        return colA;
    }

    
    /**
     * Attaches one depth, stencil or packed-depth-stencil buffer to this FBO's instance,
     * selecting the internalFormat automatically.
     * <p>
     * Stencil and depth buffer can be attached only once.
     * </p>
     * <p>
     * In case the desired type or bit-number is not supported, the next available one is chosen.
     * </p>  
     * <p>
     * Use {@link #getDepthAttachment()} and/or {@link #getStencilAttachment()} to retrieve details
     * about the attached buffer. The details cannot be returned, since it's possible 2 buffers
     * are being created, depth and stencil.
     * </p>
     * 
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl
     * @param atype either {@link Type#DEPTH}, {@link Type#STENCIL} or {@link Type#DEPTH_STENCIL}  
     * @param reqBits desired bits for depth or -1 for default (24 bits)
     * @throws GLException in case the renderbuffer couldn't be allocated or one is already attached.
     * @throws IllegalArgumentException
     * @see #getDepthAttachment()
     * @see #getStencilAttachment()
     */
    public final void attachRenderbuffer(GL gl, Attachment.Type atype, int reqBits) throws GLException, IllegalArgumentException {        
        if( 0 > reqBits ) {
            reqBits = 24;
        }        
        final int internalFormat;
        int internalStencilFormat = -1;
        
        switch ( atype ) {
            case DEPTH:
                if( 32 <= reqBits && depth32Avail ) {
                    internalFormat = GL.GL_DEPTH_COMPONENT32;
                } else if( 24 <= reqBits && depth24Avail ) {
                    internalFormat = GL.GL_DEPTH_COMPONENT24;
                } else {
                    internalFormat = GL.GL_DEPTH_COMPONENT16;                    
                }
                break;
                
            case STENCIL:
                if( 16 <= reqBits && stencil16Avail ) {
                    internalFormat = GL2GL3.GL_STENCIL_INDEX16;
                } else if( 8 <= reqBits && stencil08Avail ) {
                    internalFormat = GL.GL_STENCIL_INDEX8;
                } else if( 4 <= reqBits && stencil04Avail ) {
                    internalFormat = GL.GL_STENCIL_INDEX4;
                } else if( 1 <= reqBits && stencil01Avail ) {
                    internalFormat = GL.GL_STENCIL_INDEX1;
                } else {
                    throw new GLException("stencil buffer n/a");        
                }
                break;
                
            case DEPTH_STENCIL:
                if( packedDepthStencilAvail ) {
                    internalFormat = GL.GL_DEPTH24_STENCIL8;
                } else {
                    if( 24 <= reqBits && depth24Avail ) {
                        internalFormat = GL.GL_DEPTH_COMPONENT24;
                    } else {
                        internalFormat = GL.GL_DEPTH_COMPONENT16;                    
                    }
                    if( stencil08Avail ) {
                        internalStencilFormat = GL.GL_STENCIL_INDEX8;
                    } else if( stencil04Avail ) {
                        internalStencilFormat = GL.GL_STENCIL_INDEX4;
                    } else if( stencil01Avail ) {
                        internalStencilFormat = GL.GL_STENCIL_INDEX1;
                    } else {
                        throw new GLException("stencil buffer n/a");
                    }
                }
                break;
            default:
                throw new IllegalArgumentException("only depth/stencil types allowed, was "+atype+", "+this);
        }
        
        attachRenderbufferImpl(gl, atype, internalFormat);
        
        if(0<=internalStencilFormat) {
            attachRenderbufferImpl(gl, Attachment.Type.STENCIL, internalStencilFormat);
        }
    }
    
    /**
     * Attaches one depth, stencil or packed-depth-stencil buffer to this FBO's instance,
     * depending on the <code>internalFormat</code>. 
     * <p>
     * Stencil and depth buffer can be attached only once.
     * </p>
     * <p>
     * Use {@link #getDepthAttachment()} and/or {@link #getStencilAttachment()} to retrieve details
     * about the attached buffer. The details cannot be returned, since it's possible 2 buffers
     * are being created, depth and stencil.
     * </p>
     * 
     * <p>Leaves the FBO bound.</p>
     * 
     * @param gl the current GL context
     * @param internalFormat {@link GL#GL_DEPTH_COMPONENT16}, {@link GL#GL_DEPTH_COMPONENT24}, {@link GL#GL_DEPTH_COMPONENT32},
     *                       {@link GL#GL_STENCIL_INDEX1}, {@link GL#GL_STENCIL_INDEX4}, {@link GL#GL_STENCIL_INDEX8}
     *                       or {@link GL#GL_DEPTH24_STENCIL8}
     * @throws GLException in case the renderbuffer couldn't be allocated or one is already attached.
     * @throws IllegalArgumentException
     * @see #getDepthAttachment()
     * @see #getStencilAttachment()
     */
    public final void attachRenderbuffer(GL gl, int internalFormat) throws GLException, IllegalArgumentException {
        final Attachment.Type atype = Attachment.Type.determine(internalFormat);
        if( Attachment.Type.DEPTH != atype && Attachment.Type.STENCIL != atype && Attachment.Type.DEPTH_STENCIL != atype ) {
            throw new IllegalArgumentException("renderformat invalid: 0x"+Integer.toHexString(internalFormat)+", "+this);
        }
        attachRenderbufferImpl(gl, atype, internalFormat);
    }
    
    protected final void attachRenderbufferImpl(GL gl, Attachment.Type atype, int internalFormat) throws GLException {
        if( null != depth && ( Attachment.Type.DEPTH == atype || Attachment.Type.DEPTH_STENCIL == atype ) ) {
            throw new GLException("FBO depth buffer already attached (rb "+depth+"), type is "+atype+", 0x"+Integer.toHexString(internalFormat)+", "+this);
        }        
        if( null != stencil && ( Attachment.Type.STENCIL== atype || Attachment.Type.DEPTH_STENCIL == atype ) ) {
            throw new GLException("FBO stencil buffer already attached (rb "+stencil+"), type is "+atype+", 0x"+Integer.toHexString(internalFormat)+", "+this);
        }
        attachRenderbufferImpl2(gl, atype, internalFormat);
    }
        
    private final void attachRenderbufferImpl2(GL gl, Attachment.Type atype, int internalFormat) throws GLException {
        if( Attachment.Type.DEPTH == atype ) {
            if(null == depth) {
                depth = new RenderAttachment(Type.DEPTH, internalFormat, samples, width, height, 0);
            } else {
                depth.setSize(width, height);
                depth.setSamples(samples);
            }
            depth.initialize(gl);
        } else if( Attachment.Type.STENCIL == atype ) {
            if(null == stencil) {
                stencil = new RenderAttachment(Type.STENCIL, internalFormat, samples, width, height, 0);
            } else {
                stencil.setSize(width, height);
                stencil.setSamples(samples);
            }
            stencil.initialize(gl);
        } else if( Attachment.Type.DEPTH_STENCIL == atype ) {
            if(null == depth) {
                depth = new RenderAttachment(Type.DEPTH, internalFormat, samples, width, height, 0);
            } else {
                depth.setSize(width, height);
                depth.setSamples(samples);
            }
            depth.initialize(gl);
            if(null == stencil) {
                stencil = new RenderAttachment(Type.STENCIL, internalFormat, samples, width, height, depth.getName());
            } else {
                stencil.setName(depth.getName());
                stencil.setSize(width, height);
                stencil.setSamples(samples);
            }
            stencil.initialize(gl);
        }

        bind(gl);
        
        // Attach the buffer
        if( Attachment.Type.DEPTH == atype ) {
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, depth.getName());
        } else if( Attachment.Type.STENCIL == atype ) {
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, stencil.getName());
        } else if( Attachment.Type.DEPTH_STENCIL == atype ) {
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, depth.getName());            
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, stencil.getName());
        }
        
        updateStatus(gl);
        if( !isStatusValid() ) {
            detachRenderbuffer(gl, atype);
            throw new GLException("renderbuffer attachment failed: "+this.getStatusString());
        }

        if(DEBUG) {
            System.err.println("FBObject.attachRenderbuffer: "+this);
        }
    }
    
    /**
     * <p>Leaves the FBO bound!</p>
     * @param gl
     * @param ca
     * @throws IllegalArgumentException
     */
    public final void detachColorbuffer(GL gl, int attachmentPoint) throws IllegalArgumentException {
        if(null == detachColorbufferImpl(gl, attachmentPoint, false)) {
            throw new IllegalArgumentException("ColorAttachment at "+attachmentPoint+", not attached, "+this);            
        }
        if(DEBUG) {
            System.err.println("FBObject.detachAll: "+this);
        }
    }
    
    private final Colorbuffer detachColorbufferImpl(GL gl, int attachmentPoint, boolean recreate) {
        final Colorbuffer colbuf = colorAttachmentPoints[attachmentPoint]; // shortcut, don't validate here
        
        if(null == colbuf) {
            return null;
        }
        
        bind(gl);
        
        if(colbuf instanceof TextureAttachment) {
            final TextureAttachment texA = (TextureAttachment) colbuf;
            if( 0 != texA.getName() ) {
                gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
                              GL.GL_COLOR_ATTACHMENT0 + attachmentPoint,
                              GL.GL_TEXTURE_2D, 0, 0);
                gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
            }
            texA.free(gl);
            removeColorAttachment(attachmentPoint, texA);                
            if(recreate) {
                texA.setSize(width, height);
                attachTexture2D(gl, attachmentPoint, texA);
            }
        } else if(colbuf instanceof ColorAttachment) {
            final ColorAttachment colA = (ColorAttachment) colbuf;
            if( 0 != colA.getName() ) {
                gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, 
                                             GL.GL_COLOR_ATTACHMENT0+attachmentPoint, 
                                             GL.GL_RENDERBUFFER, 0);
            }
            colA.free(gl);
            removeColorAttachment(attachmentPoint, colbuf);
            if(recreate) {
                colA.setSize(width, height);
                colA.setSamples(samples);
                attachColorbuffer(gl, attachmentPoint, colA);
            }
        }
        return colbuf;
    }
    
    /**
     * 
     * @param gl
     * @param reqAType {@link Type#DEPTH}, {@link Type#DEPTH} or {@link Type#DEPTH_STENCIL} 
     */
    public final void detachRenderbuffer(GL gl, Attachment.Type atype) throws IllegalArgumentException {
        detachRenderbufferImpl(gl, atype, false);
    }
    
    public final boolean isDepthStencilPackedFormat() {
        final boolean res = null != depth && null != stencil &&
                            depth.format == stencil.format ;
        if(res && depth.getName() != stencil.getName() ) {
            throw new InternalError("depth/stencil packed format not sharing: depth "+depth+", stencil "+stencil);
        }
        return res;
    }
        
    private final void detachRenderbufferImpl(GL gl, Attachment.Type atype, boolean recreate) throws IllegalArgumentException {
        switch ( atype ) {
            case DEPTH:
            case STENCIL:
            case DEPTH_STENCIL:             
             break;
             default:
                 throw new IllegalArgumentException("only depth/stencil types allowed, was "+atype+", "+this);
        }        
        if( null == depth && null == stencil ) {
            return ; // nop
        } 
        // reduction of possible combinations, create unique atype command(s)
        final ArrayList<Attachment.Type> actions = new ArrayList<Attachment.Type>(2);  
        if( isDepthStencilPackedFormat() ) {
            // packed
            actions.add(Attachment.Type.DEPTH_STENCIL);
        } else {
            // individual
            switch ( atype ) {
                case DEPTH:
                    if( null != depth ) { actions.add(Attachment.Type.DEPTH); }
                    break;
                case STENCIL:
                    if( null != stencil ) { actions.add(Attachment.Type.STENCIL); }
                    break;
                case DEPTH_STENCIL:
                    if( null != depth ) { actions.add(Attachment.Type.DEPTH); }
                    if( null != stencil ) { actions.add(Attachment.Type.STENCIL); }
                    break;
                 default: // handled
            }
        }
        
        bind(gl);        
        
        for(int i = 0; i < actions.size(); i++) {
            final int format;
            
            Attachment.Type action = actions.get(i);
            switch ( action ) {
                case DEPTH:
                    format = depth.format;
                    if( 0 != depth.getName() ) {
                        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, 0);
                    }
                    depth.free(gl);
                    if(!recreate) {
                        depth = null;
                    }
                    break;
                case STENCIL:
                    format = stencil.format;
                    if(0 != stencil.getName()) {
                        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, 0);
                    }
                    stencil.free(gl);
                    if(!recreate) {
                        stencil = null;
                    }
                    break;
                case DEPTH_STENCIL:
                    format = depth.format;
                    if(0 != depth.getName()) {
                        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, GL.GL_RENDERBUFFER, 0);
                        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_STENCIL_ATTACHMENT, GL.GL_RENDERBUFFER, 0);
                    }
                    depth.free(gl);
                    stencil.free(gl);
                    if(!recreate) {
                        depth = null;
                        stencil = null;
                    }
                    break;
                 default:
                    throw new InternalError("XXX");
            }
            if(recreate) {
                attachRenderbufferImpl2(gl, action, format);
            }
        }        
    }
        
    /** 
     * Detaches all {@link ColorAttachment}s, {@link TextureAttachment}s and {@link RenderAttachment}s.
     * <p>Leaves the FBO bound!</p>
     * <p>
     * An attached sampling sink texture will be detached as well, see {@link #getSamplingSink()}.
     * </p> 
     * @param gl the current GL context
     */
    public final void detachAll(GL gl) {
        if(null != samplesSink) {
            samplesSink.detachAll(gl);
        }        
        detachAllImpl(gl, true/* detachNonColorbuffer */, false /* recreate */);
    }
    
    /** 
     * Detaches all {@link ColorAttachment}s and {@link TextureAttachment}s.
     * <p>Leaves the FBO bound!</p>
     * <p>
     * An attached sampling sink texture will be detached as well, see {@link #getSamplingSink()}.
     * </p> 
     * @param gl the current GL context
     */
    public final void detachAllColorbuffer(GL gl) {
        if(null != samplesSink) {
            samplesSink.detachAllColorbuffer(gl);
        }        
        detachAllImpl(gl, false/* detachNonColorbuffer */, false /* recreate */);
    }
    
    /** 
     * Detaches all {@link TextureAttachment}s 
     * <p>Leaves the FBO bound!</p>
     * <p>
     * An attached sampling sink texture will be detached as well, see {@link #getSamplingSink()}.
     * </p> 
     * @param gl the current GL context
     */
    public final void detachAllTexturebuffer(GL gl) {
        if(null != samplesSink) {
            samplesSink.detachAllTexturebuffer(gl);
        }
        for(int i=0; i<maxColorAttachments; i++) {
            if(colorAttachmentPoints[i] instanceof TextureAttachment) {
                detachColorbufferImpl(gl, i, false);
            }
        }
    }
    
    public final void detachAllRenderbuffer(GL gl) {
        if(null != samplesSink) {
            samplesSink.detachAllRenderbuffer(gl);
        }
        detachRenderbufferImpl(gl, Attachment.Type.DEPTH_STENCIL, false);
    }
    
    private final void detachAllImpl(GL gl, boolean detachNonColorbuffer, boolean recreate) {
        for(int i=0; i<maxColorAttachments; i++) {
            detachColorbufferImpl(gl, i, recreate);
        }
        if( !recreate && colorAttachmentCount>0 ) {
            throw new InternalError("Non zero ColorAttachments "+this);
        }
        
        if(detachNonColorbuffer) {
            detachRenderbufferImpl(gl, Attachment.Type.DEPTH_STENCIL, recreate);
        }
        
        if(DEBUG) {
            System.err.println("FBObject.detachAll: [resetNonColorbuffer "+detachNonColorbuffer+", recreate "+recreate+"]: "+this);
        }
    }
        
    /**
     * @param gl the current GL context
     */
    public final void destroy(GL gl) {
        if(null != samplesSink) {
            samplesSink.destroy(gl);
        }
        
        detachAllImpl(gl, true /* detachNonColorbuffer */, false /* recreate */);
        
        // cache FB names, preset exposed to zero,
        // braking ties w/ GL/GLContext link to getReadFramebuffer()/getWriteFramebuffer()
        final int fb_cache = fbName;
        fbName = 0;

        int name[] = new int[1];
        if(0!=fb_cache) {
            name[0] = fb_cache;
            gl.glDeleteFramebuffers(1, name, 0);
        }        
        initialized = false;
        bound = false;
        if(DEBUG) {
            System.err.println("FBObject.destroy: "+this);
        }
    }

    private final boolean sampleSinkSizeMismatch() {
        return samplesSink.getWidth() != width || samplesSink.getHeight() != height ;
    }
    private final boolean sampleSinkTexMismatch() {
        return null == samplesSinkTexture || 0 == samplesSinkTexture.getName() ;
    }
    private final boolean sampleSinkDepthStencilMismatch() {
        final boolean depthMismatch   = ( null != depth && null == samplesSink.depth ) ||
                                        ( null != depth && null != samplesSink.depth &&
                                          depth.format != samplesSink.depth.format );
        
        final boolean stencilMismatch = ( null != stencil && null == samplesSink.stencil ) ||
                                        ( null != stencil && null != samplesSink.stencil &&
                                          stencil.format != samplesSink.stencil.format );        
        
        return depthMismatch || stencilMismatch;                
    }
        
    private final void resetMSAATexture2DSink(GL gl) throws GLException {
        if(0 == samples) {
            // MSAA off
            if(null != samplesSink) {
                samplesSink.detachAll(gl);
            }
            return;
        }
        
        boolean sampleSinkSizeMismatch = sampleSinkSizeMismatch();
        boolean sampleSinkTexMismatch = sampleSinkTexMismatch();
        boolean sampleSinkDepthStencilMismatch = sampleSinkDepthStencilMismatch();
        
        /** if(DEBUG) {
            System.err.println("FBObject.resetMSAATexture2DSink.0: \n\tTHIS "+this+",\n\tSINK "+samplesSink+
                               "\n\t size "+sampleSinkSizeMismatch +", tex "+sampleSinkTexMismatch +", depthStencil "+sampleSinkDepthStencilMismatch);
        } */
        
        if(!sampleSinkSizeMismatch && !sampleSinkTexMismatch && !sampleSinkDepthStencilMismatch) {
            // all properties match .. 
            return;            
        }
        
        unbind(gl);
        
        if(DEBUG) {
            System.err.println("FBObject.resetMSAATexture2DSink: BEGIN\n\tTHIS "+this+",\n\tSINK "+samplesSink+
                               "\n\t size "+sampleSinkSizeMismatch +", tex "+sampleSinkTexMismatch +", depthStencil "+sampleSinkDepthStencilMismatch);
        }
                
        if( sampleSinkDepthStencilMismatch ) {
            samplesSink.detachAllRenderbuffer(gl);
        }
        
        if( sampleSinkSizeMismatch ) {
            samplesSink.reset(gl, width, height);
        }
        
        if(null == samplesSinkTexture) {
            samplesSinkTexture = samplesSink.attachTexture2D(gl, 0, true);
        } else if( 0 == samplesSinkTexture.getName() ) {
            samplesSinkTexture.setSize(width, height);
            samplesSink.attachTexture2D(gl, 0, samplesSinkTexture);
        }
        
        if( sampleSinkDepthStencilMismatch ) {
            samplesSink.attachRenderbuffer(gl, depth.format);
            if( null != stencil && !isDepthStencilPackedFormat() ) {
                samplesSink.attachRenderbuffer(gl, stencil.format);
            }
        }        
        
        sampleSinkSizeMismatch = sampleSinkSizeMismatch();
        sampleSinkTexMismatch = sampleSinkTexMismatch();
        sampleSinkDepthStencilMismatch = sampleSinkDepthStencilMismatch();
        if(sampleSinkSizeMismatch || sampleSinkTexMismatch || sampleSinkDepthStencilMismatch) {
            throw new InternalError("Samples sink mismatch after reset: \n\tTHIS "+this+",\n\t SINK "+samplesSink+
                                    "\n\t size "+sampleSinkSizeMismatch +", tex "+sampleSinkTexMismatch +", depthStencil "+sampleSinkDepthStencilMismatch);
        }
        
        if(DEBUG) {
            System.err.println("FBObject.resetMSAATexture2DSink: END\n\tTHIS "+this+",\n\tSINK "+samplesSink+
                               "\n\t size "+sampleSinkSizeMismatch +", tex "+sampleSinkTexMismatch +", depthStencil "+sampleSinkDepthStencilMismatch);
        }
    }
    
    /** 
     * Bind this FBO, i.e. bind write framebuffer to {@link #getWriteFramebuffer()}.
     * 
     * <p>If multisampling is used, it sets the read framebuffer to the sampling sink {@link #getWriteFramebuffer()}, 
     * if full FBO is supported.</p>
     *  
     * <p> 
     * In case you have attached more than one color buffer,
     * you may want to setup {@link GL2GL3#glDrawBuffers(int, int[], int)}.
     * </p>
     * @param gl the current GL context
     * @throws GLException
     */
    public final void bind(GL gl) throws GLException {
        if(!bound || fbName != gl.getBoundFramebuffer(GL.GL_FRAMEBUFFER)) {
            checkInitialized();
            if(samples > 0 && fullFBOSupport) {
                // draw to multisampling - read from samplesSink
                gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, getWriteFramebuffer());
                gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, getReadFramebuffer());
            } else {
                // one for all
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, getWriteFramebuffer());                
            }

            checkNoError(null, gl.glGetError(), "FBObject post-bind"); // throws GLException if error
            bound = true;
            samplesSinkDirty = true;
        }
    }

    /** 
     * Unbind this FBO, i.e. bind read and write framebuffer to default, see {@link GLBase#getDefaultDrawFramebuffer()}.
     * 
     * <p>If full FBO is supported, sets the read and write framebuffer individually to default, hence not disturbing 
     * an optional operating MSAA FBO, see {@link GLBase#getDefaultReadFramebuffer()} and {@link GLBase#getDefaultDrawFramebuffer()}</p>
     *  
     * @param gl the current GL context
     * @throws GLException
     */
    public final void unbind(GL gl) throws GLException {
        if(bound) {
            if(fullFBOSupport) {
                // default read/draw buffers, may utilize GLContext/GLDrawable override of 
                // GLContext.getDefaultDrawFramebuffer() and GLContext.getDefaultReadFramebuffer()
                gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, 0);
                gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, 0);
            } else {
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0); // default draw buffer                
            }
            checkNoError(null, gl.glGetError(), "FBObject post-unbind"); // throws GLException if error
            bound = false;
        }
    }

    /** 
     * Returns <code>true</code> if framebuffer object is bound via {@link #bind(GL)}, otherwise <code>false</code>.
     * <p>
     * Method verifies the bound state via {@link GL#getBoundFramebuffer(int)}.
     * </p>
     * @param gl the current GL context
     */
    public final boolean isBound(GL gl) { 
        bound = bound &&  fbName != gl.getBoundFramebuffer(GL.GL_FRAMEBUFFER) ;
        return bound;
    }
    
    /** Returns <code>true</code> if framebuffer object is bound via {@link #bind(GL)}, otherwise <code>false</code>. */
    public final boolean isBound() { return bound; }
    
    /** 
     * Samples the multisampling colorbuffer (msaa-buffer) to it's sink {@link #getSamplingSink()}.
     *
     * <p>The operation is skipped, if no multisampling is used or 
     * the msaa-buffer has not been flagged dirty by a previous call of {@link #bind(GL)},
     * see {@link #isSamplingBufferDirty()} </p>
     * 
     * <p>If full FBO is supported, sets the read and write framebuffer individually to default after sampling, hence not disturbing 
     * an optional operating MSAA FBO, see {@link GLBase#getDefaultReadFramebuffer()} and {@link GLBase#getDefaultDrawFramebuffer()}</p>
     * 
     * <p>In case you intend to employ {@link GL#glReadPixels(int, int, int, int, int, int, java.nio.Buffer) glReadPixels(..)}
     * you may want to call {@link GL#glBindFramebuffer(int, int) glBindFramebuffer}({@link GL2GL3#GL_READ_FRAMEBUFFER}, {@link #getReadFramebuffer()});
     * </p>
     * 
     * <p>Leaves the FBO unbound.</p>
     * 
     * @param gl the current GL context
     * @param ta {@link TextureAttachment} to use, prev. attached w/  {@link #attachTexture2D(GL, int, boolean, int, int, int, int) attachTexture2D(..)}
     * @throws IllegalArgumentException  
     */
    public final void syncSamplingBuffer(GL gl) {
        unbind(gl);
        if(samples>0 && samplesSinkDirty) {
            samplesSinkDirty = false;
            resetMSAATexture2DSink(gl);
            gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, fbName);
            gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, samplesSink.getWriteFramebuffer());
            ((GL2GL3)gl).glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, // since MSAA is supported, ugly cast is OK
                                           GL.GL_COLOR_BUFFER_BIT, GL.GL_NEAREST);
            if(fullFBOSupport) {
                // default read/draw buffers, may utilize GLContext/GLDrawable override of 
                // GLContext.getDefaultDrawFramebuffer() and GLContext.getDefaultReadFramebuffer()
                gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, 0);
                gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, 0);
            } else {
                gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0); // default draw buffer                
            }
        }
    }
    
    /** 
     * Bind the given texture colorbuffer.
     * 
     * <p>If multisampling is being used, {@link #syncSamplingBuffer(GL)} is being called.</p>
     *  
     * <p>Leaves the FBO unbound!</p>
     * 
     * @param gl the current GL context
     * @param ta {@link TextureAttachment} to use, prev. attached w/  {@link #attachTexture2D(GL, int, boolean, int, int, int, int) attachTexture2D(..)}
     * @throws IllegalArgumentException  
     */
    public final void use(GL gl, TextureAttachment ta) throws IllegalArgumentException {
        if(null == ta) { throw new IllegalArgumentException("null TextureAttachment"); }
        if(samples > 0 && samplesSinkTexture == ta) {
            syncSamplingBuffer(gl);
        } else {
            unbind(gl);            
        }
        gl.glBindTexture(GL.GL_TEXTURE_2D, ta.getName()); // use it ..
    }

    /** 
     * Unbind texture, ie bind 'non' texture 0
     *  
     * <p>Leaves the FBO unbound.</p>
     */    
    public final void unuse(GL gl) {
        unbind(gl);
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0); // don't use it
    }

    /** 
     * Returns <code>true</code> if <i>basic</i> or <i>full</i> FBO is supported, otherwise <code>false</code>.
     * @param full <code>true</code> for <i>full</i> FBO supported query, otherwise <code>false</code> for <i>basic</i> FBO support query.
     * @see #supportsFullFBO(GL)
     * @see #supportsBasicFBO(GL)
     * @throws GLException if {@link #init(GL)} hasn't been called.
     */
    public final boolean supportsFBO(boolean full) throws GLException { checkInitialized(); return full ? fullFBOSupport : basicFBOSupport; }
    
    /** 
     * Returns <code>true</code> if renderbuffer accepts internal format {@link GL#GL_RGB8} and {@link GL#GL_RGBA8}, otherwise <code>false</code>.
     * @throws GLException if {@link #init(GL)} hasn't been called.
     */
    public final boolean supportsRGBA8() throws GLException { checkInitialized(); return rgba8Avail; }
    
    /** 
     * Returns <code>true</code> if {@link GL#GL_DEPTH_COMPONENT16}, {@link GL#GL_DEPTH_COMPONENT24} or {@link GL#GL_DEPTH_COMPONENT32} is supported, otherwise <code>false</code>.
     * @param bits 16, 24 or 32 bits
     * @throws GLException if {@link #init(GL)} hasn't been called.
     */
    public final boolean supportsDepth(int bits) throws GLException {
        checkInitialized();
        switch(bits) {
            case 16: return basicFBOSupport; 
            case 24: return depth24Avail;
            case 32: return depth32Avail;
            default: return false;            
        }
    }
    
    /** 
     * Returns <code>true</code> if {@link GL#GL_STENCIL_INDEX1}, {@link GL#GL_STENCIL_INDEX4}, {@link GL#GL_STENCIL_INDEX8} or {@link GL2GL3#GL_STENCIL_INDEX16} is supported, otherwise <code>false</code>.
     * @param bits 1, 4, 8 or 16 bits
     * @throws GLException if {@link #init(GL)} hasn't been called.
     */
    public final boolean supportsStencil(int bits) throws GLException {
        checkInitialized();
        switch(bits) {
            case  1: return stencil01Avail; 
            case  4: return stencil04Avail;
            case  8: return stencil08Avail;
            case 16: return stencil16Avail;
            default: return false;            
        }
    }
    
    /** 
     * Returns <code>true</code> if {@link GL#GL_DEPTH24_STENCIL8} is supported, otherwise <code>false</code>.
     * @throws GLException if {@link #init(GL)} hasn't been called.
     */
    public final boolean supportsPackedDepthStencil() throws GLException { checkInitialized(); return packedDepthStencilAvail; }
    
    /**
     * Returns the maximum number of colorbuffer attachments.
     * @throws GLException if {@link #init(GL)} hasn't been called.
     */
    public final int getMaxColorAttachments() throws GLException { checkInitialized(); return maxColorAttachments; }
    
    /**
     * Returns the maximum number of samples for multisampling. Maybe zero if multisampling is not supported. 
     * @throws GLException if {@link #init(GL)} hasn't been called.
     */
    public final int getMaxSamples() throws GLException { checkInitialized(); return maxSamples; }
    
    /**
     * Returns <code>true</code> if this instance has been initialized with {@link #reset(GL, int, int)} 
     * or {@link #reset(GL, int, int, int)}, otherwise <code>false</code>
     */
    public final boolean isInitialized() { return initialized; }
    /** Returns the width */
    public final int getWidth() { return width; }
    /** Returns the height */
    public final int getHeight() { return height; }
    /** Returns the number of samples for multisampling (MSAA). zero if no multisampling is used. */
    public final int getNumSamples() { return samples; }
    /** Returns the framebuffer name to render to. */
    public final int getWriteFramebuffer() { return fbName; }
    /** Returns the framebuffer name to read from. Depending on multisampling, this may be a different framebuffer. */
    public final int getReadFramebuffer() { return ( samples > 0 ) ? samplesSink.getReadFramebuffer() : fbName; }
    /** Return the number of color/texture attachments */
    public final int getColorAttachmentCount() { return colorAttachmentCount; }
    /** Return the stencil {@link RenderAttachment} attachment, if exist. Maybe share the same {@link Attachment#getName()} as {@link #getDepthAttachment()}, if packed depth-stencil is being used. */ 
    public final RenderAttachment getStencilAttachment() { return stencil; }
    /** Return the depth {@link RenderAttachment} attachment. Maybe share the same {@link Attachment#getName()} as {@link #getStencilAttachment()}, if packed depth-stencil is being used. */ 
    public final RenderAttachment getDepthAttachment() { return depth; }
    
    /** Return the complete multisampling {@link FBObject} sink, if using multisampling. */ 
    public final FBObject getSamplingSinkFBO() { return samplesSink; }
    
    /** Return the multisampling {@link TextureAttachment} sink, if using multisampling. */ 
    public final TextureAttachment getSamplingSink() { return samplesSinkTexture; }
    /** 
     * Returns <code>true</code> if the multisampling colorbuffer (msaa-buffer) 
     * has been flagged dirty by a previous call of {@link #bind(GL)},
     * otherwise <code>false</code>.
     */
    public final boolean isSamplingBufferDirty() { return samplesSinkDirty; }
    
    int objectHashCode() { return super.hashCode(); }
    
    public final String toString() {
        final String caps = null != colorAttachmentPoints ? Arrays.asList(colorAttachmentPoints).toString() : null ; 
        return "FBO[name r/w "+fbName+"/"+getReadFramebuffer()+", init "+initialized+", bound "+bound+", size "+width+"x"+height+", samples "+samples+"/"+maxSamples+
               ", depth "+depth+", stencil "+stencil+", color attachments: "+colorAttachmentCount+"/"+maxColorAttachments+
               ": "+caps+", msaa-sink "+samplesSinkTexture+", isSamplesSink "+(null == samplesSink)+
               ", obj 0x"+Integer.toHexString(objectHashCode())+"]";
    }
    
    private final void updateStatus(GL gl) {
        if( 0 == fbName ) {
            vStatus = -1;
        } else {
            vStatus = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
        }
    }       
}
