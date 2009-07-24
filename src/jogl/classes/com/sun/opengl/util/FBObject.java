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

package com.sun.opengl.util;

import javax.media.opengl.*;

public class FBObject {
    private int width, height, attr;
    private int fb, fbo_tex, depth_rb, stencil_rb, vStatus;
    private int texInternalFormat, texDataFormat, texDataType;

    public static final int ATTR_DEPTH   = 1 << 0;
    public static final int ATTR_STENCIL = 1 << 1;

    public FBObject(int width, int height, int attributes) {
        this.width = width;
        this.height = height;
        this.attr = attributes;
    }        


    public boolean validateStatus(GL gl) {
        vStatus = getStatus(gl, fb);
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
                return false;
        }
    }

    public static int getStatus(GL gl, int fb) {
        if(!gl.glIsFramebuffer(fb)) {
            return -1;
        }
        return gl.glCheckFramebufferStatus(gl.GL_FRAMEBUFFER);
        //return gl.glCheckFramebufferStatus(fb);
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

    public void init(GL gl) {
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
        init(gl, textureInternalFormat, textureDataFormat, textureDataType);
    }

    public void init(GL gl, int textureInternalFormat, int textureDataFormat, int textureDataType) {
        texInternalFormat=textureInternalFormat; 
        texDataFormat=textureDataFormat;
        texDataType=textureDataType;

        // generate fbo ..
        int name[] = new int[1];

        gl.glGenFramebuffers(1, name, 0);
        fb = name[0];
        System.out.println("fb: "+fb);

        gl.glGenTextures(1, name, 0);
        fbo_tex = name[0];
        System.out.println("fbo_tex: "+fbo_tex);

        if(0!=(attr&ATTR_DEPTH)) {
            gl.glGenRenderbuffers(1, name, 0);
            depth_rb = name[0];
            System.out.println("depth_rb: "+depth_rb);
        } else {
            depth_rb = 0;
        }
        if(0!=(attr&ATTR_STENCIL)) {
            gl.glGenRenderbuffers(1, name, 0);
            stencil_rb = name[0];
            System.out.println("stencil_rb: "+stencil_rb);
        } else {
            stencil_rb = 0;
        }

        // bind fbo ..
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fb);

        gl.glBindTexture(GL.GL_TEXTURE_2D, fbo_tex);
        gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, texInternalFormat, width, height, 0,
                        texDataFormat, texDataType, null);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        //gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        //gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);


        // Set up the color buffer for use as a renderable texture:
        gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER,
                                  GL.GL_COLOR_ATTACHMENT0,
                                  GL.GL_TEXTURE_2D, fbo_tex, 0); 

        if(depth_rb!=0) {
            // Initialize the depth buffer:
            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, depth_rb);
            gl.glRenderbufferStorage(GL.GL_RENDERBUFFER,
                                        GL.GL_DEPTH_COMPONENT16, width, height);
            // Set up the depth buffer attachment:
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER,
                                            GL.GL_DEPTH_ATTACHMENT,
                                            GL.GL_RENDERBUFFER, depth_rb);
        }

        if(stencil_rb!=0) {
            // Initialize the stencil buffer:
            gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, stencil_rb);
            gl.glRenderbufferStorage(GL.GL_RENDERBUFFER,
                                        GL.GL_STENCIL_INDEX8, width, height);
            gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER,
                                            GL.GL_STENCIL_ATTACHMENT,
                                            GL.GL_RENDERBUFFER, stencil_rb);
        }

        // Check the FBO for completeness
        if(validateStatus(gl)) {
            System.out.println("Framebuffer " + fb + " is complete");
        } else {
            System.out.println("Framebuffer " + fb + " is incomplete: status = 0x" + Integer.toHexString(vStatus) + 
                               " : " + getStatusString());
        }

        unbind(gl);
    }

    public void destroy(GL gl) {
        unbind(gl);

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

    public void bind(GL gl) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, fbo_tex);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fb); 
    }

    public void unbind(GL gl) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
    }

    public void use(GL gl) {
        gl.glBindTexture(GL.GL_TEXTURE_2D, 0);
        gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, fbo_tex); // to use it ..
    }

    public int getFBName() {
        return fb;
    }
    public int getTextureName() {
        return fbo_tex;
    }
}
