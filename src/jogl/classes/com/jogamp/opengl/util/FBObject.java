/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.jogamp.opengl.util;

import javax.media.opengl.*;

public class FBObject {
    private int width, height;
    private int fb, fbo_tex, depth_rb, stencil_rb, vStatus;
    private int texInternalFormat, texDataFormat, texDataType;

    public static final int ATTR_DEPTH   = 1 << 0;
    public static final int ATTR_STENCIL = 1 << 1;
    
    public FBObject(int width, int height) {
        this.width = width;
        this.height = height;
        this.fb = 0;
        this.fbo_tex = 0;
        this.depth_rb = 0;
        this.stencil_rb = 0;        
    }        

    public boolean validateStatus(GL gl) {
        /* vStatus = */ getStatus(gl);
        switch(vStatus) {
            case GL.GL_FRAMEBUFFER_COMPLETE:
                return true;
            case GL.GL_FRAMEBUFFER_UNSUPPORTED:
            case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
            case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
            case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
            case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
            //case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
            //case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
            //case GL2.GL_FRAMEBUFFER_INCOMPLETE_DUPLICATE_ATTACHMENT:
            case 0:
            default:
                System.out.println("Framebuffer " + fb + " is incomplete: status = 0x" + Integer.toHexString(vStatus) + 
                        " : " + getStatusString(vStatus));
                return false;
        }
    }
        
    /** @return {@link GL.GL_FRAMEBUFFER_COMPLETE} if ok, otherwise return GL FBO error state or -1 */
    public int getStatus() {
        return vStatus;
    }

    public int getStatus(GL gl) {
        if(!gl.glIsFramebuffer(fb)) {
            vStatus = -1;
        } else {
            vStatus = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
        }
        return vStatus;
    }

    public String getStatusString() {
        return getStatusString(vStatus);
    }
    
    public static String getStatusString(int fbStatus) {
        switch(fbStatus) {
            case -1:
                return "NOT A FBO";
            case GL.GL_FRAMEBUFFER_COMPLETE:
                return "OK";
            case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
                return("GL FBO: incomplete,incomplete attachment\n");
            case GL.GL_FRAMEBUFFER_UNSUPPORTED:
                return("GL FBO: Unsupported framebuffer format");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
                return("GL FBO: incomplete,missing attachment");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:
                return("GL FBO: incomplete,attached images must have same dimensions");
            case GL.GL_FRAMEBUFFER_INCOMPLETE_FORMATS:
                 return("GL FBO: incomplete,attached images must have same format");
                 /*
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
                return("GL FBO: incomplete,missing draw buffer");
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
                return("GL FBO: incomplete,missing read buffer");
            case GL2.GL_FRAMEBUFFER_INCOMPLETE_DUPLICATE_ATTACHMENT:
                return("GL FBO: incomplete, duplicate attachment");
                */
            case 0:
                return("GL FBO: incomplete, implementation fault");
            default:
                return("GL FBO: incomplete, implementation ERROR");
        }
    }
    
    /**
     * Initializes this FBO's instance with it's texture,
     * selecting the texture data type and format automatically.
     * 
     * Leaves the FBO bound!
     * 
     * @param gl the current GL context
     * @param magFilter if > 0 value for {@link GL#GL_TEXTURE_MAG_FILTER}
     * @param minFilter if > 0 value for {@link GL#GL_TEXTURE_MIN_FILTER} 
     * @param wrapS if > 0 value for {@link GL#GL_TEXTURE_WRAP_S}
     * @param wrapT if > 0 value for {@link GL#GL_TEXTURE_WRAP_T}
     * @return
     */
    public boolean init(GL gl, int magFilter, int minFilter, int wrapS, int wrapT) {
        int textureInternalFormat, textureDataFormat, textureDataType;

        if(gl.isGL2()) { 
            textureInternalFormat=GL.GL_RGBA8;
            textureDataFormat=GL2.GL_BGRA;
            textureDataType=GL2.GL_UNSIGNED_INT_8_8_8_8_REV;
        } else if(gl.isGLES()) { 
            textureInternalFormat=GL.GL_RGBA;
            textureDataFormat=GL.GL_RGBA;
            textureDataType=GL.GL_UNSIGNED_BYTE;
        } else {
            textureInternalFormat=GL.GL_RGB;
            textureDataFormat=GL.GL_RGB;
            textureDataType=GL.GL_UNSIGNED_BYTE;
        }
        return init(gl, textureInternalFormat, textureDataFormat, textureDataType, magFilter, minFilter, wrapS, wrapT);
    }

    /**
     * Initializes this FBO's instance with it's texture.
     * 
     * Leaves the FBO bound!
     * 
     * @param gl the current GL context
     * @param textureInternalFormat internalFormat parameter to {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, long)}
     * @param textureDataFormat format parameter to {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, long)}
     * @param textureDataType type parameter to {@link GL#glTexImage2D(int, int, int, int, int, int, int, int, long)}
     * @param magFilter if > 0 value for {@link GL#GL_TEXTURE_MAG_FILTER}
     * @param minFilter if > 0 value for {@link GL#GL_TEXTURE_MIN_FILTER} 
     * @param wrapS if > 0 value for {@link GL#GL_TEXTURE_WRAP_S}
     * @param wrapT if > 0 value for {@link GL#GL_TEXTURE_WRAP_T}
     * @return true if successful otherwise false
     */
    public boolean init(GL gl, int textureInternalFormat, int textureDataFormat, int textureDataType,
                               int magFilter, int minFilter, int wrapS, int wrapT) {
        checkBound(false);
        if(0<fb || 0<fbo_tex) {
            throw new GLException("FBO already initialized (fb "+fb+", tex "+fbo_tex+")");
        }
        
        texInternalFormat=textureInternalFormat; 
        texDataFormat=textureDataFormat;
        texDataType=textureDataType;

        // generate fbo ..
        int name[] = new int[1];

        gl.glGenFramebuffers(1, name, 0);
        fb = name[0];
        if(fb==0) {
            throw new GLException("null generated framebuffer");
        }

        gl.glGenTextures(1, name, 0);
        fbo_tex = name[0];
        if(fbo_tex==0) {
            throw new GLException("null generated texture");
        }

        // bind fbo ..
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fb);
        if(!gl.glIsFramebuffer(fb)) {
            destroy(gl);
            System.err.println("not a framebuffer: "+ fb);
            return false;            
        }
        bound = true;        

        gl.glBindTexture(GL.GL_TEXTURE_2D, fbo_tex);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, texInternalFormat, width, height, 0,
                        texDataFormat, texDataType, null);
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

        // Set up the color buffer for use as a renderable texture:
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
                                  GL.GL_COLOR_ATTACHMENT0,
                                  GL.GL_TEXTURE_2D, fbo_tex, 0); 

        return validateStatus(gl);                
    }

    /**
     * Assumes a bound FBO
     * Leaves the FBO bound!
     * @param depthComponentType {@link GL#GL_DEPTH_COMPONENT16}, {@link GL#GL_DEPTH_COMPONENT24} or {@link GL#GL_DEPTH_COMPONENT32}
     * @return true if successful otherwise false 
     */
    public boolean attachDepthBuffer(GL gl, int depthComponentType) {
        if(0>=fb || 0>=fbo_tex) {
            throw new GLException("FBO not initialized (fb "+fb+", tex "+fbo_tex+")");
        }
        if(depth_rb != 0) {
            throw new GLException("FBO depth buffer already attached (rb "+depth_rb+")");
        }
        checkBound(true);
        int name[] = new int[1];
        gl.glGenRenderbuffers(1, name, 0);
        depth_rb = name[0];
        
        if(depth_rb==0) {
            throw new GLException("null generated renderbuffer");
        }
        // Initialize the depth buffer:
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, depth_rb);
        if(!gl.glIsRenderbuffer(depth_rb)) {
            System.err.println("not a depthbuffer: "+ depth_rb);
            name[0] = depth_rb;
            gl.glDeleteRenderbuffers(1, name, 0);
            depth_rb=0;
            return false;            
        }
        
        gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, depthComponentType, width, height);
        // Set up the depth buffer attachment:
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER,
                                        GL.GL_DEPTH_ATTACHMENT,
                                        GL.GL_RENDERBUFFER, depth_rb);
        return validateStatus(gl);                
    }
    
    /**
     * Assumes a bound FBO
     * Leaves the FBO bound!
     * @param stencilComponentType {@link GL#GL_STENCIL_INDEX1}, {@link GL#GL_STENCIL_INDEX4} or {@link GL#GL_STENCIL_INDEX8}
     * @return true if successful otherwise false 
     */
    public boolean attachStencilBuffer(GL gl, int stencilComponentType) {
        if(0>=fb || 0>=fbo_tex) {
            throw new GLException("FBO not initialized (fb "+fb+", tex "+fbo_tex+")");
        }
        if(stencil_rb != 0) {
            throw new GLException("FBO stencil buffer already attached (rb "+stencil_rb+")");
        }
        checkBound(true);
        int name[] = new int[1];
        gl.glGenRenderbuffers(1, name, 0);
        gl.glGenRenderbuffers(1, name, 0);
        stencil_rb = name[0];
        if(stencil_rb==0) {
            throw new GLException("null generated stencilbuffer");
        }
        // Initialize the stencil buffer:
        gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, stencil_rb);
        if(!gl.glIsRenderbuffer(stencil_rb)) {
            destroy(gl);
            System.err.println("not a stencilbuffer: "+ depth_rb);
            return false;            
        }
        gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, stencilComponentType, width, height);
        gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER,
                                        GL.GL_STENCIL_ATTACHMENT,
                                        GL.GL_RENDERBUFFER, stencil_rb);
        return validateStatus(gl);                
    }

    public void destroy(GL gl) {
        if(bound) {
            unbind(gl);
        }

        int name[] = new int[1];

        if(0!=stencil_rb) {
            name[0] = stencil_rb;
            gl.glDeleteRenderbuffers(1, name, 0);
            stencil_rb = 0;
        }
        if(0!=depth_rb) {
            name[0] = depth_rb;
            gl.glDeleteRenderbuffers(1, name, 0);
            depth_rb=0;
        }
        if(0!=fbo_tex) {
            name[0] = fbo_tex;
            gl.glDeleteTextures(1, name, 0);
            fbo_tex = 0;
        }
        if(0!=fb) {
            name[0] = fb;
            gl.glDeleteFramebuffers(1, name, 0);
            fb = 0;
        }
    }

    boolean bound = false;
    
    private final void checkBound(boolean shallBeBound) {
        if(bound != shallBeBound) {
            final String s0 = shallBeBound ? "not" : "already" ; 
            throw new GLException("FBO "+s0+" bound "+toString());
        }
    }
    
    public void bind(GL gl) {
        checkBound(false);
        gl.glBindTexture(GL.GL_TEXTURE_2D, fbo_tex);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fb);
        bound = true;
    }

    public void unbind(GL gl) {
        checkBound(true);
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        bound = false;
    }

    public void use(GL gl) {
        if(bound) {
            unbind(gl);
        }
        gl.glBindTexture(GL.GL_TEXTURE_2D, fbo_tex); // to use it ..
    }

    public int getFBName() {
        return fb;
    }
    public int getTextureName() {
        return fbo_tex;
    }
}
