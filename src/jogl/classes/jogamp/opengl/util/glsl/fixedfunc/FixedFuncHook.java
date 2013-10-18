/*
 * Copyright 2009 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package jogamp.opengl.util.glsl.fixedfunc;

import java.nio.Buffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLArrayData;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLLightingFunc;
import javax.media.opengl.fixedfunc.GLMatrixFunc;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.ValueConv;
import com.jogamp.opengl.util.GLArrayDataWrapper;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.fixedfunc.ShaderSelectionMode;

public class FixedFuncHook implements GLLightingFunc, GLMatrixFunc, GLPointerFunc {
    public static final int MAX_TEXTURE_UNITS = 8;

    protected FixedFuncPipeline fixedFunction;
    protected PMVMatrix pmvMatrix;
    protected boolean ownsPMVMatrix;
    protected GL2ES2 gl;

    /**
     * @param gl
     * @param mode TODO
     * @param pmvMatrix optional pass through PMVMatrix for the {@link FixedFuncHook} and {@link FixedFuncPipeline}
     */
    public FixedFuncHook (GL2ES2 gl, ShaderSelectionMode mode, PMVMatrix pmvMatrix) {
        this.gl = gl;
        if(null != pmvMatrix) {
            this.ownsPMVMatrix = false;
            this.pmvMatrix = pmvMatrix;
        } else {
            this.ownsPMVMatrix = true;
            this.pmvMatrix = new PMVMatrix();
        }
        fixedFunction = new FixedFuncPipeline(this.gl, mode, this.pmvMatrix);
    }

    /**
     * @param gl
     * @param mode TODO
     * @param pmvMatrix optional pass through PMVMatrix for the {@link FixedFuncHook} and {@link FixedFuncPipeline}
     */
    public FixedFuncHook(GL2ES2 gl, ShaderSelectionMode mode, PMVMatrix pmvMatrix,
                         Class<?> shaderRootClass, String shaderSrcRoot, String shaderBinRoot,
                         String vertexColorFile, String vertexColorLightFile,
                         String fragmentColorFile, String fragmentColorTextureFile) {
        this.gl = gl;
        if(null != pmvMatrix) {
            this.ownsPMVMatrix = false;
            this.pmvMatrix = pmvMatrix;
        } else {
            this.ownsPMVMatrix = true;
            this.pmvMatrix = new PMVMatrix();
        }

        fixedFunction = new FixedFuncPipeline(this.gl, mode, this.pmvMatrix, shaderRootClass, shaderSrcRoot,
                                              shaderBinRoot, vertexColorFile, vertexColorLightFile, fragmentColorFile, fragmentColorTextureFile);
    }

    public boolean verbose() { return fixedFunction.verbose(); }

    public void setVerbose(boolean v) { fixedFunction.setVerbose(v); }

    public void destroy() {
        fixedFunction.destroy(gl);
        fixedFunction = null;
        if(ownsPMVMatrix) {
            pmvMatrix.destroy();
        }
        pmvMatrix=null;
        gl=null;
    }

    public PMVMatrix getMatrix() { return pmvMatrix; }

    //
    // FixedFuncHookIf - hooks
    //
    public void glDrawArrays(int mode, int first, int count) {
        fixedFunction.glDrawArrays(gl, mode, first, count);
    }
    public void glDrawElements(int mode, int count, int type, java.nio.Buffer indices) {
        fixedFunction.glDrawElements(gl, mode, count, type, indices);
    }
    public void glDrawElements(int mode, int count, int type, long indices_buffer_offset) {
        fixedFunction.glDrawElements(gl, mode, count, type, indices_buffer_offset);
    }

    public void glActiveTexture(int texture) {
        fixedFunction.glActiveTexture(texture);
        gl.glActiveTexture(texture);
    }
    public void glEnable(int cap) {
        if(fixedFunction.glEnable(cap, true)) {
            gl.glEnable(cap);
        }
    }
    public void glDisable(int cap) {
        if(fixedFunction.glEnable(cap, false)) {
            gl.glDisable(cap);
        }
    }
    @Override
    public void glGetFloatv(int pname, java.nio.FloatBuffer params) {
        if(PMVMatrix.isMatrixGetName(pname)) {
            pmvMatrix.glGetFloatv(pname, params);
            return;
        }
        gl.glGetFloatv(pname, params);
    }
    @Override
    public void glGetFloatv(int pname, float[] params, int params_offset) {
        if(PMVMatrix.isMatrixGetName(pname)) {
            pmvMatrix.glGetFloatv(pname, params, params_offset);
            return;
        }
        gl.glGetFloatv(pname, params, params_offset);
    }
    @Override
    public void glGetIntegerv(int pname, IntBuffer params) {
        if(PMVMatrix.isMatrixGetName(pname)) {
            pmvMatrix.glGetIntegerv(pname, params);
            return;
        }
        gl.glGetIntegerv(pname, params);
    }
    @Override
    public void glGetIntegerv(int pname, int[] params, int params_offset) {
        if(PMVMatrix.isMatrixGetName(pname)) {
            pmvMatrix.glGetIntegerv(pname, params, params_offset);
            return;
        }
        gl.glGetIntegerv(pname, params, params_offset);
    }

    public void glTexEnvi(int target, int pname, int value) {
        fixedFunction.glTexEnvi(target, pname, value);
    }
    public void glGetTexEnviv(int target, int pname,  IntBuffer params) {
        fixedFunction.glGetTexEnviv(target, pname, params);
    }
    public void glGetTexEnviv(int target, int pname,  int[] params, int params_offset) {
        fixedFunction.glGetTexEnviv(target, pname, params, params_offset);
    }
    public void glBindTexture(int target, int texture) {
        fixedFunction.glBindTexture(target, texture);
        gl.glBindTexture(target, texture);
    }
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                             int format, int type,  Buffer pixels) {
        // align internalformat w/ format, an ES2 requirement
        switch(internalformat) {
            case 3: internalformat= ( GL.GL_RGBA == format ) ? GL.GL_RGBA : GL.GL_RGB; break;
            case 4: internalformat= ( GL.GL_RGB  == format ) ? GL.GL_RGB  : GL.GL_RGBA; break;
        }
        fixedFunction.glTexImage2D(target, /* level, */ internalformat, /*width, height, border, */ format /*, type, pixels*/);
        gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels);
    }
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border,
                             int format, int type,  long pixels_buffer_offset) {
        // align internalformat w/ format, an ES2 requirement
        switch(internalformat) {
            case 3: internalformat= ( GL.GL_RGBA == format ) ? GL.GL_RGBA : GL.GL_RGB; break;
            case 4: internalformat= ( GL.GL_RGB  == format ) ? GL.GL_RGB  : GL.GL_RGBA; break;
        }
        fixedFunction.glTexImage2D(target, /* level, */ internalformat, /*width, height, border, */ format /*, type, pixels*/);
        gl.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels_buffer_offset);
    }

    public void glPointSize(float size) {
        fixedFunction.glPointSize(size);
    }
    public  void glPointParameterf(int pname, float param) {
        fixedFunction.glPointParameterf(pname, param);
    }
    public  void glPointParameterfv(int pname, float[] params, int params_offset) {
        fixedFunction.glPointParameterfv(pname, params, params_offset);
    }
    public  void glPointParameterfv(int pname, java.nio.FloatBuffer params) {
        fixedFunction.glPointParameterfv(pname, params);
    }

    //
    // MatrixIf
    //
    public int  glGetMatrixMode() {
        return pmvMatrix.glGetMatrixMode();
    }
    @Override
    public void glMatrixMode(int mode) {
        pmvMatrix.glMatrixMode(mode);
    }
    @Override
    public void glLoadMatrixf(java.nio.FloatBuffer m) {
        pmvMatrix.glLoadMatrixf(m);
    }
    @Override
    public void glLoadMatrixf(float[] m, int m_offset) {
        glLoadMatrixf(GLBuffers.newDirectFloatBuffer(m, m_offset));
    }
    @Override
    public void glPopMatrix() {
        pmvMatrix.glPopMatrix();
    }
    @Override
    public void glPushMatrix() {
        pmvMatrix.glPushMatrix();
    }
    @Override
    public void glLoadIdentity() {
        pmvMatrix.glLoadIdentity();
    }
    @Override
    public void glMultMatrixf(java.nio.FloatBuffer m) {
        pmvMatrix.glMultMatrixf(m);
    }
    @Override
    public void glMultMatrixf(float[] m, int m_offset) {
        glMultMatrixf(GLBuffers.newDirectFloatBuffer(m, m_offset));
    }
    @Override
    public void glTranslatef(float x, float y, float z) {
        pmvMatrix.glTranslatef(x, y, z);
    }
    @Override
    public void glRotatef(float angdeg, float x, float y, float z) {
        pmvMatrix.glRotatef(angdeg, x, y, z);
    }
    @Override
    public void glScalef(float x, float y, float z) {
        pmvMatrix.glScalef(x, y, z);
    }
    public void glOrtho(double left, double right, double bottom, double top, double near_val, double far_val) {
        glOrthof((float) left, (float) right, (float) bottom, (float) top, (float) near_val, (float) far_val);
    }
    @Override
    public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar) {
        pmvMatrix.glOrthof(left, right, bottom, top, zNear, zFar);
    }
    public void glFrustum(double left, double right, double bottom, double top, double zNear, double zFar) {
        glFrustumf((float) left, (float) right, (float) bottom, (float) top, (float) zNear, (float) zFar);
    }
    @Override
    public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar) {
        pmvMatrix.glFrustumf(left, right, bottom, top, zNear, zFar);
    }

    //
    // LightingIf
    //
    @Override
    public void glColor4f(float red, float green, float blue, float alpha) {
      fixedFunction.glColor4f(gl, red, green, blue, alpha);
    }

    public  void glColor4ub(byte red, byte green, byte blue, byte alpha) {
      glColor4f(ValueConv.byte_to_float(red, false),
                ValueConv.byte_to_float(green, false),
                ValueConv.byte_to_float(blue, false),
                ValueConv.byte_to_float(alpha, false) );
    }
    @Override
    public void glLightfv(int light, int pname, java.nio.FloatBuffer params) {
      fixedFunction.glLightfv(gl, light, pname, params);
    }
    @Override
    public void glLightfv(int light, int pname, float[] params, int params_offset) {
        glLightfv(light, pname, GLBuffers.newDirectFloatBuffer(params, params_offset));
    }
    @Override
    public void glMaterialfv(int face, int pname, java.nio.FloatBuffer params) {
      fixedFunction.glMaterialfv(gl, face, pname, params);
    }
    @Override
    public void glMaterialfv(int face, int pname, float[] params, int params_offset) {
        glMaterialfv(face, pname, GLBuffers.newDirectFloatBuffer(params, params_offset));
    }
    @Override
    public void glMaterialf(int face, int pname, float param) {
        glMaterialfv(face, pname, GLBuffers.newDirectFloatBuffer(new float[] { param }));
    }

    //
    // Misc Simple States
    //
    @Override
    public void glShadeModel(int mode) {
      fixedFunction.glShadeModel(gl, mode);
    }
    public  void glAlphaFunc(int func, float ref) {
        fixedFunction.glAlphaFunc(func, ref);
    }

    /** ES2 supports CullFace implicit
    public void glCullFace(int faceName) {
        fixedFunction.glCullFace(faceName);
        gl.glCullFace(faceName);
    } */

    //
    // PointerIf
    //
    public void glClientActiveTexture(int textureUnit) {
      fixedFunction.glClientActiveTexture(textureUnit);
    }
    @Override
    public void glEnableClientState(int glArrayIndex) {
      fixedFunction.glEnableClientState(gl, glArrayIndex);
    }
    @Override
    public void glDisableClientState(int glArrayIndex) {
      fixedFunction.glDisableClientState(gl, glArrayIndex);
    }

    @Override
    public void glVertexPointer(GLArrayData array) {
      if(array.isVBO()) {
          if(!gl.glIsVBOArrayBound()) {
            throw new GLException("VBO array is not enabled: "+array);
          }
      } else {
          if(gl.glIsVBOArrayBound()) {
            throw new GLException("VBO array is not disabled: "+array);
          }
          Buffers.rangeCheck(array.getBuffer(), 1);
          if (!Buffers.isDirect(array.getBuffer())) {
            throw new GLException("Argument \"pointer\" was not a direct buffer"); }
      }
      fixedFunction.glVertexPointer(gl, array);
    }

    @Override
    public void glVertexPointer(int size, int type, int stride, java.nio.Buffer pointer) {
      glVertexPointer(GLArrayDataWrapper.createFixed(GL_VERTEX_ARRAY, size, type, GLBuffers.isGLTypeFixedPoint(type), stride,
                                                     pointer, 0, 0, 0, GL.GL_ARRAY_BUFFER));
    }
    @Override
    public void glVertexPointer(int size, int type, int stride, long pointer_buffer_offset) {
      int vboName = gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER);
      if(vboName==0) {
        throw new GLException("no GL_ARRAY_BUFFER VBO bound");
      }
      glVertexPointer(GLArrayDataWrapper.createFixed(GL_VERTEX_ARRAY, size, type, GLBuffers.isGLTypeFixedPoint(type), stride,
                                                     null, vboName, pointer_buffer_offset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER));
    }

    @Override
    public void glColorPointer(GLArrayData array) {
      if(array.isVBO()) {
          if(!gl.glIsVBOArrayBound()) {
            throw new GLException("VBO array is not enabled: "+array);
          }
      } else {
          if(gl.glIsVBOArrayBound()) {
            throw new GLException("VBO array is not disabled: "+array);
          }
          Buffers.rangeCheck(array.getBuffer(), 1);
          if (!Buffers.isDirect(array.getBuffer())) {
            throw new GLException("Argument \"pointer\" was not a direct buffer"); }
      }
      fixedFunction.glColorPointer(gl, array);
    }
    @Override
    public void glColorPointer(int size, int type, int stride, java.nio.Buffer pointer) {
      glColorPointer(GLArrayDataWrapper.createFixed(GL_COLOR_ARRAY, size, type, GLBuffers.isGLTypeFixedPoint(type), stride,
                                                    pointer, 0, 0, 0, GL.GL_ARRAY_BUFFER));
    }
    @Override
    public void glColorPointer(int size, int type, int stride, long pointer_buffer_offset) {
      int vboName = gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER);
      if(vboName==0) {
        throw new GLException("no GL_ARRAY_BUFFER VBO bound");
      }
      glColorPointer(GLArrayDataWrapper.createFixed(GL_COLOR_ARRAY, size, type, GLBuffers.isGLTypeFixedPoint(type), stride,
                                                   null, vboName, pointer_buffer_offset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER));
    }

    @Override
    public void glNormalPointer(GLArrayData array) {
      if(array.getComponentCount()!=3) {
        throw new GLException("Only 3 components per normal allowed");
      }
      if(array.isVBO()) {
          if(!gl.glIsVBOArrayBound()) {
            throw new GLException("VBO array is not enabled: "+array);
          }
      } else {
          if(gl.glIsVBOArrayBound()) {
            throw new GLException("VBO array is not disabled: "+array);
          }
          Buffers.rangeCheck(array.getBuffer(), 1);
          if (!Buffers.isDirect(array.getBuffer())) {
            throw new GLException("Argument \"pointer\" was not a direct buffer"); }
      }
      fixedFunction.glNormalPointer(gl, array);
    }
    @Override
    public void glNormalPointer(int type, int stride, java.nio.Buffer pointer) {
      glNormalPointer(GLArrayDataWrapper.createFixed(GL_NORMAL_ARRAY, 3, type, GLBuffers.isGLTypeFixedPoint(type), stride,
                                                     pointer, 0, 0, 0, GL.GL_ARRAY_BUFFER));
    }
    @Override
    public void glNormalPointer(int type, int stride, long pointer_buffer_offset) {
      int vboName = gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER);
      if(vboName==0) {
        throw new GLException("no GL_ARRAY_BUFFER VBO bound");
      }
      glNormalPointer(GLArrayDataWrapper.createFixed(GL_NORMAL_ARRAY, 3, type, GLBuffers.isGLTypeFixedPoint(type), stride,
                                                     null, vboName, pointer_buffer_offset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER));
    }

    @Override
    public void glTexCoordPointer(GLArrayData array) {
      if(array.isVBO()) {
          if(!gl.glIsVBOArrayBound()) {
            throw new GLException("VBO array is not enabled: "+array);
          }
      } else {
          if(gl.glIsVBOArrayBound()) {
            throw new GLException("VBO array is not disabled: "+array);
          }
          Buffers.rangeCheck(array.getBuffer(), 1);
          if (!Buffers.isDirect(array.getBuffer())) {
            throw new GLException("Argument \"pointer\" was not a direct buffer"); }
      }
      fixedFunction.glTexCoordPointer(gl, array);
    }
    @Override
    public void glTexCoordPointer(int size, int type, int stride, java.nio.Buffer pointer) {
      glTexCoordPointer(
        GLArrayDataWrapper.createFixed(GL_TEXTURE_COORD_ARRAY, size, type, GLBuffers.isGLTypeFixedPoint(type), stride,
                                       pointer, 0, 0, 0, GL.GL_ARRAY_BUFFER));
    }
    @Override
    public void glTexCoordPointer(int size, int type, int stride, long pointer_buffer_offset) {
      int vboName = gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER);
      if(vboName==0) {
        throw new GLException("no GL_ARRAY_BUFFER VBO bound");
      }
      glTexCoordPointer(
        GLArrayDataWrapper.createFixed(GL_TEXTURE_COORD_ARRAY, size, type, GLBuffers.isGLTypeFixedPoint(type), stride,
                                       null, vboName, pointer_buffer_offset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER) );
    }

    @Override
    public final String toString() {
          StringBuilder buf = new StringBuilder();
          buf.append(getClass().getName()+" (");
          if(null!=pmvMatrix) {
              buf.append(", matrixDirty: "+ (0 != pmvMatrix.getModifiedBits(false)));
          }
          buf.append("\n\t, FixedFunction: "+fixedFunction);
          buf.append(gl);
          buf.append(" )");

          return buf.toString();
    }

}


