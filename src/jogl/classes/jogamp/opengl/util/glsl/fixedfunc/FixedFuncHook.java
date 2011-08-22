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

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.*;
import javax.media.opengl.glu.*;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.util.*;
import com.jogamp.opengl.util.glsl.*;
import java.nio.*;

public class FixedFuncHook implements GLLightingFunc, GLMatrixFunc, GLPointerFunc {
    public static final int MAX_TEXTURE_UNITS = 8;

    protected FixedFuncPipeline fixedFunction=null;
    protected PMVMatrix pmvMatrix=null;
    protected GL2ES2 gl=null;

    public FixedFuncHook (GL2ES2 gl) {
        this(gl, null);
    }

    public FixedFuncHook (GL2ES2 gl, PMVMatrix matrix) {
        this.gl = gl;
        pmvMatrix = (null!=matrix)?matrix:new PMVMatrix();

        fixedFunction = new FixedFuncPipeline(gl, pmvMatrix);
    }

    public FixedFuncHook(GL2ES2 gl, PMVMatrix matrix, 
                       Class shaderRootClass, String shaderSrcRoot, String shaderBinRoot, 
                       String vertexColorFile,
                       String vertexColorLightFile,
                       String fragmentColorFile,
                       String fragmentColorTextureFile) {
        this.gl = gl;
        pmvMatrix = matrix;

        fixedFunction = new FixedFuncPipeline(gl, pmvMatrix,
                                              shaderRootClass, shaderSrcRoot, shaderBinRoot, 
                                              vertexColorFile, vertexColorLightFile, fragmentColorFile, fragmentColorTextureFile);
    }

    public void destroy() {
        fixedFunction.destroy(gl);
        fixedFunction = null;
    }

    public PMVMatrix getMatrix() { return pmvMatrix; }

    //
    // FixedFuncHookIf - hooks 
    //
    public void glDrawArrays(int mode, int first, int count) {
        fixedFunction.validate(gl); 
        gl.glDrawArrays(mode, first, count);
    }
    public void glDrawElements(int mode, int count, int type, java.nio.Buffer indices) {
        fixedFunction.validate(gl); 
        gl.glDrawElements(mode, count, type, indices);
    }
    public void glDrawElements(int mode, int count, int type, long indices_buffer_offset) {
        fixedFunction.validate(gl); 
        gl.glDrawElements(mode, count, type, indices_buffer_offset);
    }

    public void glActiveTexture(int texture) {
        fixedFunction.glActiveTexture(gl, texture);
        gl.glActiveTexture(texture);
    }
    public void glEnable(int cap) {
        if(fixedFunction.glEnable(gl, cap, true)) {
            gl.glEnable(cap);
        }
    }
    public void glDisable(int cap) {
        if(fixedFunction.glEnable(gl, cap, false)) {
            gl.glDisable(cap);
        }
    }
    public void glCullFace(int faceName) {
        fixedFunction.glCullFace(gl, faceName);
        gl.glCullFace(faceName);
    }

    public void glGetFloatv(int pname, java.nio.FloatBuffer params) {
        if(pmvMatrix.isMatrixGetName(pname)) {
            pmvMatrix.glGetFloatv(pname, params);
            return;
        }
        gl.glGetFloatv(pname, params);
    }
    public void glGetFloatv(int pname, float[] params, int params_offset) {
        if(pmvMatrix.isMatrixGetName(pname)) {
            pmvMatrix.glGetFloatv(pname, params, params_offset);
            return;
        }
        gl.glGetFloatv(pname, params, params_offset);
    }
    public void glGetIntegerv(int pname, IntBuffer params) {
        if(pmvMatrix.isMatrixGetName(pname)) {
            pmvMatrix.glGetIntegerv(pname, params);
            return;
        }
        gl.glGetIntegerv(pname, params);
    }
    public void glGetIntegerv(int pname, int[] params, int params_offset) {
        if(pmvMatrix.isMatrixGetName(pname)) {
            pmvMatrix.glGetIntegerv(pname, params, params_offset);
            return;
        }
        gl.glGetIntegerv(pname, params, params_offset);
    }

    // 
    // MatrixIf
    //
    public int  glGetMatrixMode() {
        return pmvMatrix.glGetMatrixMode();
    }
    public void glMatrixMode(int mode) {
        pmvMatrix.glMatrixMode(mode);
    }
    public void glLoadMatrixf(java.nio.FloatBuffer m) {
        pmvMatrix.glLoadMatrixf(m);
    }
    public void glLoadMatrixf(float[] m, int m_offset) {
        glLoadMatrixf(GLBuffers.newDirectFloatBuffer(m, m_offset));
    }
    public void glPopMatrix() {
        pmvMatrix.glPopMatrix();
    }
    public void glPushMatrix() {
        pmvMatrix.glPushMatrix();
    }
    public void glLoadIdentity() {
        pmvMatrix.glLoadIdentity();
    }
    public void glMultMatrixf(java.nio.FloatBuffer m) {
        pmvMatrix.glMultMatrixf(m);
    }
    public void glMultMatrixf(float[] m, int m_offset) {
        glMultMatrixf(GLBuffers.newDirectFloatBuffer(m, m_offset));
    }
    public void glTranslatef(float x, float y, float z) {
        pmvMatrix.glTranslatef(x, y, z);
    }
    public void glRotatef(float angdeg, float x, float y, float z) {
        pmvMatrix.glRotatef(angdeg, x, y, z);
    }
    public void glScalef(float x, float y, float z) {
        pmvMatrix.glScalef(x, y, z);
    }
    public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar) {
        pmvMatrix.glOrthof(left, right, bottom, top, zNear, zFar);
    }
    public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar) {
        pmvMatrix.glFrustumf(left, right, bottom, top, zNear, zFar);
    }

    // 
    // LightingIf
    //
    public void glColor4f(float red, float green, float blue, float alpha) {
      fixedFunction.glColor4fv(gl, GLBuffers.newDirectFloatBuffer(new float[] { red, green, blue, alpha }));
    }

    public void glLightfv(int light, int pname, java.nio.FloatBuffer params) {
      fixedFunction.glLightfv(gl, light, pname, params);
    }
    public void glLightfv(int light, int pname, float[] params, int params_offset) {
        glLightfv(light, pname, GLBuffers.newDirectFloatBuffer(params, params_offset));
    }
    public void glMaterialfv(int face, int pname, java.nio.FloatBuffer params) {
      fixedFunction.glMaterialfv(gl, face, pname, params);
    }
    public void glMaterialfv(int face, int pname, float[] params, int params_offset) {
        glMaterialfv(face, pname, GLBuffers.newDirectFloatBuffer(params, params_offset));
    }
    public void glMaterialf(int face, int pname, float param) {
        glMaterialfv(face, pname, GLBuffers.newDirectFloatBuffer(new float[] { param }));
    }
    public void glShadeModel(int mode) {
      fixedFunction.glShadeModel(gl, mode);
    }

    //
    // PointerIf
    //
    public void glEnableClientState(int glArrayIndex) {
      fixedFunction.glEnableClientState(gl, glArrayIndex);
    }
    public void glDisableClientState(int glArrayIndex) {
      fixedFunction.glDisableClientState(gl, glArrayIndex);
    }

    public void glVertexPointer(GLArrayData array) {
      if(array.isVBO()) {
          if(!gl.glIsVBOArrayEnabled()) {
            throw new GLException("VBO array is not enabled: "+array);
          }
      } else {
          if(gl.glIsVBOArrayEnabled()) {
            throw new GLException("VBO array is not disabled: "+array);
          }
          Buffers.rangeCheck(array.getBuffer(), 1);
          if (!Buffers.isDirect(array.getBuffer())) {
            throw new GLException("Argument \"pointer\" was not a direct buffer"); }
      }
      fixedFunction.glVertexPointer(gl, array);
    }

    public void glVertexPointer(int size, int type, int stride, java.nio.Buffer pointer) {
      glVertexPointer(GLArrayDataWrapper.createFixed(GL_VERTEX_ARRAY, size, type, false, stride, pointer, 0, 0, 0, GL.GL_ARRAY_BUFFER));
    }
    public void glVertexPointer(int size, int type, int stride, long pointer_buffer_offset) {
      int vboName = gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER);
      if(vboName==0) {
        throw new GLException("no GL_ARRAY_BUFFER VBO bound");
      }
      glVertexPointer(GLArrayDataWrapper.createFixed(GL_VERTEX_ARRAY, size, type, false, stride, 
                                                     null, vboName, pointer_buffer_offset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER));
    }

    public void glColorPointer(GLArrayData array) {
      if(array.isVBO()) {
          if(!gl.glIsVBOArrayEnabled()) {
            throw new GLException("VBO array is not enabled: "+array);
          }
      } else {
          if(gl.glIsVBOArrayEnabled()) {
            throw new GLException("VBO array is not disabled: "+array);
          }
          Buffers.rangeCheck(array.getBuffer(), 1);
          if (!Buffers.isDirect(array.getBuffer())) {
            throw new GLException("Argument \"pointer\" was not a direct buffer"); }
      }
      fixedFunction.glColorPointer(gl, array);
    }
    public void glColorPointer(int size, int type, int stride, java.nio.Buffer pointer) {
      glColorPointer(GLArrayDataWrapper.createFixed(GL_COLOR_ARRAY, size, type, false, stride, 
                                                    pointer, 0, 0, 0, GL.GL_ARRAY_BUFFER));
    }
    public void glColorPointer(int size, int type, int stride, long pointer_buffer_offset) {
      int vboName = gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER);
      if(vboName==0) {
        throw new GLException("no GL_ARRAY_BUFFER VBO bound");
      }
      glColorPointer(GLArrayDataWrapper.createFixed(GL_COLOR_ARRAY, size, type, false, stride, 
                                                   null, vboName, pointer_buffer_offset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER));
    }

    public void glNormalPointer(GLArrayData array) {
      if(array.getComponentCount()!=3) {
        throw new GLException("Only 3 components per normal allowed");
      }
      if(array.isVBO()) {
          if(!gl.glIsVBOArrayEnabled()) {
            throw new GLException("VBO array is not enabled: "+array);
          }
      } else {
          if(gl.glIsVBOArrayEnabled()) {
            throw new GLException("VBO array is not disabled: "+array);
          }
          Buffers.rangeCheck(array.getBuffer(), 1);
          if (!Buffers.isDirect(array.getBuffer())) {
            throw new GLException("Argument \"pointer\" was not a direct buffer"); }
      }
      fixedFunction.glNormalPointer(gl, array);
    }
    public void glNormalPointer(int type, int stride, java.nio.Buffer pointer) {
      glNormalPointer(GLArrayDataWrapper.createFixed(GL_NORMAL_ARRAY, 3, type, false, stride, 
                                                     pointer, 0, 0, 0, GL.GL_ARRAY_BUFFER));
    }
    public void glNormalPointer(int type, int stride, long pointer_buffer_offset) {
      int vboName = gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER);
      if(vboName==0) {
        throw new GLException("no GL_ARRAY_BUFFER VBO bound");
      }
      glNormalPointer(GLArrayDataWrapper.createFixed(GL_NORMAL_ARRAY, 3, type, false, stride, 
                                                     null, vboName, pointer_buffer_offset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER));
    }

    public void glTexCoordPointer(GLArrayData array) {
      if(array.isVBO()) {
          if(!gl.glIsVBOArrayEnabled()) {
            throw new GLException("VBO array is not enabled: "+array);
          }
      } else {
          if(gl.glIsVBOArrayEnabled()) {
            throw new GLException("VBO array is not disabled: "+array);
          }
          Buffers.rangeCheck(array.getBuffer(), 1);
          if (!Buffers.isDirect(array.getBuffer())) {
            throw new GLException("Argument \"pointer\" was not a direct buffer"); }
      }
      fixedFunction.glTexCoordPointer(gl, array);
    }
    public void glTexCoordPointer(int size, int type, int stride, java.nio.Buffer pointer) {
      glTexCoordPointer(
        GLArrayDataWrapper.createFixed(GL_TEXTURE_COORD_ARRAY, size, type, false, stride, pointer, 0, 0, 0, GL.GL_ARRAY_BUFFER));
    }
    public void glTexCoordPointer(int size, int type, int stride, long pointer_buffer_offset) {
      int vboName = gl.glGetBoundBuffer(GL.GL_ARRAY_BUFFER);
      if(vboName==0) {
        throw new GLException("no GL_ARRAY_BUFFER VBO bound");
      }
      glTexCoordPointer(
        GLArrayDataWrapper.createFixed(GL_TEXTURE_COORD_ARRAY, size, type, false, stride, 
                                       null, vboName, pointer_buffer_offset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER) );
    }

    public final String toString() {
          StringBuffer buf = new StringBuffer();
          buf.append(getClass().getName()+" (");
          if(null!=pmvMatrix) {
              buf.append(", matrixDirty: "+pmvMatrix.isDirty());
          }
          buf.append("\n\t, FixedFunction: "+fixedFunction);
          buf.append(gl);
          buf.append(" )");

          return buf.toString();
    }

}


