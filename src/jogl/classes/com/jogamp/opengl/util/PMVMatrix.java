/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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

import com.jogamp.common.nio.Buffers;
import jogamp.opengl.ProjectFloat;

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

public class PMVMatrix implements GLMatrixFunc {

    protected final float[] matrixBufferArray;

    /**
     * Creates an instance of PMVMatrix {@link #PMVMatrix(boolean) PMVMatrix(boolean useBackingArray)},
     * with <code>useBackingArray = true</code>. 
     */
    public PMVMatrix() {
        this(true);
    }
    
    /**
     * Creates an instance of PMVMatrix.
     * 
     * @param useBackingArray <code>true</code> for non direct NIO Buffers with guaranteed backing array,
     *                        which allows faster access in Java computation.
     *                        <p><code>false</code> for direct NIO buffers w/o a guaranteed backing array.
     *                        In most Java implementations, direct NIO buffers have no backing array
     *                        and hence the Java computation will be throttled down by direct IO get/put 
     *                        operations.</p> 
     *                        <p>Depending on the application, ie. weather the Java computation or
     *                        JNI invocation and hence native data transfer part is heavier, 
     *                        this flag shall be set to <code>true</code> or <code>false</code></p>.
     */
    public PMVMatrix(boolean useBackingArray) {
          projectFloat = new ProjectFloat();

          // I    Identity
          // T    Texture
          // P    Projection
          // Mv   ModelView
          // Mvi  Modelview-Inverse
          // Mvit Modelview-Inverse-Transpose
          if(useBackingArray) {
              matrixBufferArray = new float[6*16];
              matrixBuffer = null;
              // matrixBuffer = FloatBuffer.wrap(new float[12*16]);
          } else {
              matrixBufferArray = null;
              matrixBuffer = Buffers.newDirectByteBuffer(6*16 * Buffers.SIZEOF_FLOAT);
              matrixBuffer.mark();
          }
          
          matrixIdent   = slice2Float(matrixBuffer, matrixBufferArray,  0*16, 1*16);  //  I
          matrixTex     = slice2Float(matrixBuffer, matrixBufferArray,  1*16, 1*16);  //      T
          matrixPMvMvit = slice2Float(matrixBuffer, matrixBufferArray,  2*16, 4*16);  //          P  + Mv + Mvi + Mvit          
          matrixPMvMvi  = slice2Float(matrixBuffer, matrixBufferArray,  2*16, 3*16);  //          P  + Mv + Mvi
          matrixPMv     = slice2Float(matrixBuffer, matrixBufferArray,  2*16, 2*16);  //          P  + Mv
          matrixP       = slice2Float(matrixBuffer, matrixBufferArray,  2*16, 1*16);  //          P
          matrixMv      = slice2Float(matrixBuffer, matrixBufferArray,  3*16, 1*16);  //               Mv
          matrixMvi     = slice2Float(matrixBuffer, matrixBufferArray,  4*16, 1*16);  //                    Mvi
          matrixMvit    = slice2Float(matrixBuffer, matrixBufferArray,  5*16, 1*16);  //                          Mvit
          
          if(null != matrixBuffer) {
              matrixBuffer.reset();
          }          
          ProjectFloat.gluMakeIdentityf(matrixIdent);
          
          vec3f         = new float[3];
          matrixMult    = new float[16];
          matrixTrans   = new float[16];
          matrixRot     = new float[16];
          matrixScale   = new float[16];
          matrixOrtho   = new float[16];
          matrixFrustum = new float[16];
          ProjectFloat.gluMakeIdentityf(matrixTrans, 0);
          ProjectFloat.gluMakeIdentityf(matrixRot, 0);
          ProjectFloat.gluMakeIdentityf(matrixScale, 0);
          ProjectFloat.gluMakeIdentityf(matrixOrtho, 0);
          ProjectFloat.gluMakeZero(matrixFrustum, 0);

          matrixPStack = new ArrayList<float[]>();
          matrixMvStack= new ArrayList<float[]>();

          // default values and mode
          glMatrixMode(GL_PROJECTION);
          glLoadIdentity();
          glMatrixMode(GL_MODELVIEW);
          glLoadIdentity();
          glMatrixMode(GL.GL_TEXTURE);
          glLoadIdentity();
          setDirty();
          update();
    }

    public void destroy() {
        if(null!=projectFloat) {
            projectFloat.destroy(); projectFloat=null;
        }

        matrixBuffer=null;
        matrixBuffer=null; matrixPMvMvit=null; matrixPMvMvi=null; matrixPMv=null; 
        matrixP=null; matrixTex=null; matrixMv=null; matrixMvi=null; matrixMvit=null;        

        vec3f         = null;
        matrixMult    = null;
        matrixTrans   = null;
        matrixRot     = null;
        matrixScale   = null;
        matrixOrtho   = null;
        matrixFrustum = null;
        
        if(null!=matrixPStack) {
            matrixPStack.clear(); matrixPStack=null;
        }
        if(null!=matrixMvStack) {
            matrixMvStack.clear(); matrixMvStack=null;
        }
        if(null!=matrixPStack) {
            matrixPStack.clear(); matrixPStack=null;
        }
        if(null!=matrixTStack) {
            matrixTStack.clear(); matrixTStack=null;
        }
    }


    /**
     * Slices a ByteBuffer to a FloatBuffer at the given position with the given size
     * in float-space. Using a ByteBuffer as the source guarantees 
     * keeping the source native order programmatically.  
     * This works around <a href="http://code.google.com/p/android/issues/detail?id=16434">Honeycomb / Android 3.0 Issue 16434</a>. 
     * This bug is resolved at least in Android 3.2.
     * 
     * @param buf source ByteBuffer
     * @param backing source float array
     * @param posFloat {@link Buffers#SIZEOF_FLOAT} position
     * @param lenFloat {@link Buffers#SIZEOF_FLOAT} size 
     * @return FloatBuffer w/ native byte order as given ByteBuffer
     */
    private static FloatBuffer slice2Float(Buffer buf, float[] backing, int posFloat, int lenFloat) {
        if(buf instanceof ByteBuffer) {
            ByteBuffer bb = (ByteBuffer) buf;
            bb.position( posFloat * Buffers.SIZEOF_FLOAT );
            bb.limit( (posFloat + lenFloat) * Buffers.SIZEOF_FLOAT );
            FloatBuffer fb = bb.slice().order(bb.order()).asFloatBuffer(); // slice and duplicate may change byte order
            fb.mark();
            return fb;
        } else if(null != backing) {
            FloatBuffer fb  = FloatBuffer.wrap(backing, posFloat, lenFloat);
            fb.mark();
            return fb;
        } else if(buf instanceof FloatBuffer) {
            FloatBuffer fb = (FloatBuffer) buf;
            fb.position( posFloat );
            fb.limit( posFloat + lenFloat );
            FloatBuffer fb0 = fb.slice(); // slice and duplicate may change byte order
            fb0.mark();
            return fb0;
        } else {
            throw new InternalError("XXX");
        }
    }

    public static final boolean isMatrixModeName(final int matrixModeName) {
        switch(matrixModeName) {
            case GL_MODELVIEW_MATRIX:
            case GL_PROJECTION_MATRIX:
            case GL_TEXTURE_MATRIX:
                return true;
        }
        return false;
    }

    public static final int matrixModeName2MatrixGetName(final int matrixModeName) {
        switch(matrixModeName) {
            case GL_MODELVIEW:
                return GL_MODELVIEW_MATRIX;
            case GL_PROJECTION:
                return GL_PROJECTION_MATRIX;
            case GL.GL_TEXTURE:
                return GL_TEXTURE_MATRIX;
            default:
              throw new GLException("unsupported matrixName: "+matrixModeName);
        }
    }

    public static final boolean isMatrixGetName(final int matrixGetName) {
        switch(matrixGetName) {
            case GL_MATRIX_MODE:
            case GL_MODELVIEW_MATRIX:
            case GL_PROJECTION_MATRIX:
            case GL_TEXTURE_MATRIX:
                return true;
        }
        return false;
    }

    public static final int matrixGetName2MatrixModeName(final int matrixGetName) {
        switch(matrixGetName) {
            case GL_MODELVIEW_MATRIX:
                return GL_MODELVIEW;
            case GL_PROJECTION_MATRIX:
                return GL_PROJECTION;
            case GL_TEXTURE_MATRIX:
                return GL.GL_TEXTURE;
            default:
              throw new GLException("unsupported matrixGetName: "+matrixGetName);
        }
    }

    public void setDirty() {
          modified   = DIRTY_MODELVIEW | DIRTY_PROJECTION | DIRTY_TEXTURE ;
          matrixMode = GL_MODELVIEW;
    }

    public int getDirtyBits() {
        return modified;
    }

    public boolean isDirty(final int matrixName) {
        boolean res;
        switch(matrixName) {
            case GL_MODELVIEW:
                res = (modified&DIRTY_MODELVIEW)!=0 ;
                break;
            case GL_PROJECTION:
                res = (modified&DIRTY_PROJECTION)!=0 ;
                break;
            case GL.GL_TEXTURE:
                res = (modified&DIRTY_TEXTURE)!=0 ;
                break;
            default:
              throw new GLException("unsupported matrixName: "+matrixName);
        }
        return res;
    }

    public boolean isDirty() {
        return modified!=0;
    }

    /**
     * Update the derived Mvi, Mvit and Pmv matrices
     * in case Mv or P has changed.
     * 
     * @return
     */
    public boolean update() {
        if(0==modified) return false;

        final int res = modified;
        if( (res&DIRTY_MODELVIEW)!=0 ) {
            setMviMvit();
        }
        modified=0;
        return res!=0;
    }

    public final int  glGetMatrixMode() {
        return matrixMode;
    }

    public final FloatBuffer glGetTMatrixf() {
        return matrixTex;
    }

    public final FloatBuffer glGetPMatrixf() {
        return matrixP;
    }

    public final FloatBuffer glGetMvMatrixf() {
        return matrixMv;
    }

    public final FloatBuffer glGetPMvMviMatrixf() {
        usesMviMvit |= 1;
        return matrixPMvMvi;
    }

    public final FloatBuffer glGetPMvMatrixf() {
        return matrixPMv;
    }

    public final FloatBuffer glGetMviMatrixf() {
        usesMviMvit |= 1;
        return matrixMvi;
    }

    public final FloatBuffer glGetPMvMvitMatrixf() {
        usesMviMvit |= 1 | 2;
        return matrixPMvMvit;
    }
    
    public final FloatBuffer glGetMvitMatrixf() {
        usesMviMvit |= 1 | 2;
        return matrixMvit;
    }
    
   /*
    * @return the current matrix
    */
    public final FloatBuffer glGetMatrixf() {
        return glGetMatrixf(matrixMode);
    }

  /**
   * @param matrixName GL_MODELVIEW, GL_PROJECTION or GL.GL_TEXTURE
   * @return the given matrix
   */
    public final FloatBuffer glGetMatrixf(final int matrixName) {
        if(matrixName==GL_MODELVIEW) {
            return matrixMv;
        } else if(matrixName==GL_PROJECTION) {
            return matrixP;
        } else if(matrixName==GL.GL_TEXTURE) {
            return matrixTex;
        } else {
            throw new GLException("unsupported matrixName: "+matrixName);
        }
    }

    public final void gluPerspective(final float fovy, final float aspect, final float zNear, final float zFar) {
      float top=(float)Math.tan(fovy*((float)Math.PI)/360.0f)*zNear;
      float bottom=-1.0f*top;
      float left=aspect*bottom;
      float right=aspect*top;
      glFrustumf(left, right, bottom, top, zNear, zFar);
    }

    public static final void glMultMatrixf(final FloatBuffer a, final FloatBuffer b, FloatBuffer d) {
       final int aP = a.position(); 
       final int bP = b.position();
       final int dP = d.position();
       for (int i = 0; i < 4; i++) {
          final float ai0=a.get(aP+i+0*4),  ai1=a.get(aP+i+1*4),  ai2=a.get(aP+i+2*4),  ai3=a.get(aP+i+3*4);
          d.put(dP+i+0*4 , ai0 * b.get(bP+0+0*4) + ai1 * b.get(bP+1+0*4) + ai2 * b.get(bP+2+0*4) + ai3 * b.get(bP+3+0*4) );
          d.put(dP+i+1*4 , ai0 * b.get(bP+0+1*4) + ai1 * b.get(bP+1+1*4) + ai2 * b.get(bP+2+1*4) + ai3 * b.get(bP+3+1*4) );
          d.put(dP+i+2*4 , ai0 * b.get(bP+0+2*4) + ai1 * b.get(bP+1+2*4) + ai2 * b.get(bP+2+2*4) + ai3 * b.get(bP+3+2*4) );
          d.put(dP+i+3*4 , ai0 * b.get(bP+0+3*4) + ai1 * b.get(bP+1+3*4) + ai2 * b.get(bP+2+3*4) + ai3 * b.get(bP+3+3*4) );
       }
    }
    public static final void glMultMatrixf(final FloatBuffer a, final float[] b, int b_off, FloatBuffer d) {
       final int aP = a.position(); 
       final int dP = d.position();
       for (int i = 0; i < 4; i++) {
          final float ai0=a.get(aP+i+0*4),  ai1=a.get(aP+i+1*4),  ai2=a.get(aP+i+2*4),  ai3=a.get(aP+i+3*4);
          d.put(dP+i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
          d.put(dP+i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
          d.put(dP+i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
          d.put(dP+i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
       }
    }

    // 
    // MatrixIf
    //

    public void glMatrixMode(final int matrixName) {
        switch(matrixName) {
            case GL_MODELVIEW:
            case GL_PROJECTION:
            case GL.GL_TEXTURE:
                break;
            default:
              throw new GLException("unsupported matrixName: "+matrixName);
        }
        matrixMode = matrixName;
    }

    public void glGetFloatv(int matrixGetName, FloatBuffer params) {
        int pos = params.position();
        if(matrixGetName==GL_MATRIX_MODE) {
            params.put((float)matrixMode);
        } else {
            FloatBuffer matrix = glGetMatrixf(matrixGetName2MatrixModeName(matrixGetName));
            params.put(matrix); // matrix -> params
            matrix.reset();
        }
        params.position(pos);
    }
    public void glGetFloatv(int matrixGetName, float[] params, int params_offset) {
        if(matrixGetName==GL_MATRIX_MODE) {
            params[params_offset]=(float)matrixMode;
        } else {
            FloatBuffer matrix = glGetMatrixf(matrixGetName2MatrixModeName(matrixGetName));
            matrix.get(params, params_offset, 16); // matrix -> params
            matrix.reset();
        }
    }
    public void glGetIntegerv(int pname, IntBuffer params) {
        int pos = params.position();
        if(pname==GL_MATRIX_MODE) {
            params.put(matrixMode);
        } else {
            throw new GLException("unsupported pname: "+pname);
        }
        params.position(pos);
    }
    public void glGetIntegerv(int pname, int[] params, int params_offset) {
        if(pname==GL_MATRIX_MODE) {
            params[params_offset]=matrixMode;
        } else {
            throw new GLException("unsupported pname: "+pname);
        }
    }

    public final void glLoadMatrixf(final float[] values, final int offset) {
        int len = values.length-offset;
        if(matrixMode==GL_MODELVIEW) {
            matrixMv.put(values, offset, len);
            matrixMv.reset();
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.put(values, offset, len);
            matrixP.reset();
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixTex.put(values, offset, len);
            matrixTex.reset();
            modified |= DIRTY_TEXTURE ;
        } 
    }

    public final void glLoadMatrixf(java.nio.FloatBuffer m) {
        int spos = m.position();
        if(matrixMode==GL_MODELVIEW) {
            matrixMv.put(m);
            matrixMv.reset();
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.put(m);
            matrixP.reset();
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixTex.put(m);
            matrixTex.reset();
            modified |= DIRTY_TEXTURE ;
        } 
        m.position(spos);
    }

    public final void glPopMatrix() {
        float[] stackEntry=null;
        if(matrixMode==GL_MODELVIEW) {
            stackEntry = matrixMvStack.remove(0);
        } else if(matrixMode==GL_PROJECTION) {
            stackEntry = matrixPStack.remove(0);
        } else if(matrixMode==GL.GL_TEXTURE) {
            stackEntry = matrixTStack.remove(0);
        } 
        glLoadMatrixf(stackEntry, 0);
    }

    public final void glPushMatrix() {
        float[] stackEntry = new float[1*16];
        if(matrixMode==GL_MODELVIEW) {
            matrixMv.get(stackEntry);
            matrixMv.reset();
            matrixMvStack.add(0, stackEntry);
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.get(stackEntry);
            matrixP.reset();
            matrixPStack.add(0, stackEntry);
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixTex.get(stackEntry);
            matrixTex.reset();
            matrixTStack.add(0, stackEntry);
        }
    }

    public final void glLoadIdentity() {
        if(matrixMode==GL_MODELVIEW) {
            matrixMv.put(matrixIdent);
            matrixMv.reset();
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.put(matrixIdent);
            matrixP.reset();
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixTex.put(matrixIdent);
            matrixTex.reset();
            modified |= DIRTY_TEXTURE ;
        } 
        matrixIdent.reset();
    }

    public final void glMultMatrixf(final FloatBuffer m) {
        if(matrixMode==GL_MODELVIEW) {
            glMultMatrixf(matrixMv, m, matrixMv);
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            glMultMatrixf(matrixP, m, matrixP);
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            glMultMatrixf(matrixTex, m, matrixTex);
            modified |= DIRTY_TEXTURE ;
        } 
    }

    public void glMultMatrixf(float[] m, int m_offset) {
        if(matrixMode==GL_MODELVIEW) {
            glMultMatrixf(matrixMv, m, m_offset, matrixMv);
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            glMultMatrixf(matrixP, m, m_offset, matrixP);
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            glMultMatrixf(matrixTex, m, m_offset, matrixTex);
            modified |= DIRTY_TEXTURE ;
        } 
    }

    public final void glTranslatef(final float x, final float y, final float z) {
        // Translation matrix: 
        //  1 0 0 x
        //  0 1 0 y
        //  0 0 1 z
        //  0 0 0 1
        matrixTrans[0+4*3] = x;
        matrixTrans[1+4*3] = y;
        matrixTrans[2+4*3] = z;
        glMultMatrixf(matrixTrans, 0);
    }

    public final void glRotatef(final float angdeg, float x, float y, float z) {
        final float angrad = angdeg   * (float) Math.PI / 180.0f;
        final float c = (float)Math.cos(angrad);
        final float ic= 1.0f - c; 
        final float s = (float)Math.sin(angrad);

        vec3f[0]=x; vec3f[1]=y; vec3f[2]=z;
        ProjectFloat.normalize(vec3f);
        x = vec3f[0]; y = vec3f[1]; z = vec3f[2];

        // Rotation matrix:
        //      xx(1−c)+c  xy(1−c)+zs xz(1−c)-ys 0
        //      xy(1−c)-zs yy(1−c)+c  yz(1−c)+xs 0
        //      xz(1−c)+ys yz(1−c)-xs zz(1−c)+c  0
        //      0          0          0          1
        final float xy = x*y;
        final float xz = x*z;
        final float xs = x*s;
        final float ys = y*s;
        final float yz = y*z;
        final float zs = z*s;
        matrixRot[0*4+0] = x*x*ic+c;
        matrixRot[0*4+1] = xy*ic+zs;
        matrixRot[0*4+2] = xz*ic-ys;

        matrixRot[1*4+0] = xy*ic-zs;
        matrixRot[1*4+1] = y*y*ic+c;
        matrixRot[1*4+2] = yz*ic+xs;

        matrixRot[2*4+0] = xz*ic+ys;
        matrixRot[2*4+1] = yz*ic-xs;
        matrixRot[2*4+2] = z*z*ic+c;

        glMultMatrixf(matrixRot, 0);
    }

    public final void glScalef(final float x, final float y, final float z) {
        // Scale matrix: 
        //  x 0 0 0
        //  0 y 0 0
        //  0 0 z 0
        //  0 0 0 1
        matrixScale[0+4*0] = x;
        matrixScale[1+4*1] = y;
        matrixScale[2+4*2] = z;

        glMultMatrixf(matrixScale, 0);
    }

    public final void glOrthof(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        // Ortho matrix: 
        //  2/dx  0     0    tx
        //  0     2/dy  0    ty
        //  0     0     2/dz tz
        //  0     0     0    1
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float tx=-1.0f*(right+left)/dx;
        final float ty=-1.0f*(top+bottom)/dy;
        final float tz=-1.0f*(zFar+zNear)/dz;

        matrixOrtho[0+4*0] =  2.0f/dx;
        matrixOrtho[1+4*1] =  2.0f/dy;
        matrixOrtho[2+4*2] = -2.0f/dz;
        matrixOrtho[0+4*3] = tx;
        matrixOrtho[1+4*3] = ty;
        matrixOrtho[2+4*3] = tz;

        glMultMatrixf(matrixOrtho, 0);
    }

    public final void glFrustumf(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        if(zNear<=0.0f||zFar<0.0f) {
            throw new GLException("GL_INVALID_VALUE: zNear and zFar must be positive, and zNear>0");
        }
        if(left==right || top==bottom) {
            throw new GLException("GL_INVALID_VALUE: top,bottom and left,right must not be equal");
        }
        // Frustum matrix: 
        //  2*zNear/dx   0          A  0
        //  0            2*zNear/dy B  0
        //  0            0          C  D
        //  0            0         −1  0
        final float zNear2 = 2.0f*zNear;
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float A=(right+left)/dx;
        final float B=(top+bottom)/dy;
        final float C=-1.0f*(zFar+zNear)/dz;
        final float D=-2.0f*(zFar*zNear)/dz;

        matrixFrustum[0+4*0] = zNear2/dx;
        matrixFrustum[1+4*1] = zNear2/dy;
        matrixFrustum[2+4*2] = C;

        matrixFrustum[0+4*2] = A;
        matrixFrustum[1+4*2] = B;

        matrixFrustum[2+4*3] = D;
        matrixFrustum[3+4*2] = -1.0f;

        glMultMatrixf(matrixFrustum, 0);
    }

    //
    // private 
    //
    private int nioBackupArraySupported = 0; // -1 not supported, 0 - TBD, 1 - supported
    private final String msgCantComputeInverse = "Invalid source Mv matrix, can't compute inverse";

    private final void setMviMvit() {
        if( 0 != (usesMviMvit & 1) ) {
            if(nioBackupArraySupported>=0) {
                try {
                    setMviMvitNIOBackupArray();
                    nioBackupArraySupported = 1;
                    return;
                } catch(UnsupportedOperationException uoe) {
                    nioBackupArraySupported = -1;
                }
            }
            setMviMvitNIODirectAccess();
        }
    }
    private final void setMviMvitNIOBackupArray() {
        final float[] _matrixMvi = matrixMvi.array();
        final int _matrixMviOffset = matrixMvi.position();
        if(!projectFloat.gluInvertMatrixf(matrixMv.array(), matrixMv.position(), _matrixMvi, _matrixMviOffset)) {
            throw new GLException(msgCantComputeInverse);
        }
        if( 0 != (usesMviMvit & 2) ) {
            // transpose matrix 
            final float[] _matrixMvit = matrixMvit.array();
            final int _matrixMvitOffset = matrixMvit.position();
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    _matrixMvit[_matrixMvitOffset+j+i*4] = _matrixMvi[_matrixMviOffset+i+j*4];
                }
            }
        }        
    }
    
    private final void setMviMvitNIODirectAccess() {
        if(!projectFloat.gluInvertMatrixf(matrixMv, matrixMvi)) {
            throw new GLException(msgCantComputeInverse);
        }
        if( 0 != (usesMviMvit & 2) ) {
            // transpose matrix 
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    matrixMvit.put(j+i*4, matrixMvi.get(i+j*4));
                }
            }
        }        
    }

    protected Buffer matrixBuffer;
    protected FloatBuffer matrixIdent, matrixPMvMvit, matrixPMvMvi, matrixPMv, matrixP, matrixTex, matrixMv, matrixMvi, matrixMvit;
    protected float[] matrixMult, matrixTrans, matrixRot, matrixScale, matrixOrtho, matrixFrustum, vec3f;
    protected List<float[]> matrixTStack, matrixPStack, matrixMvStack;
    protected int matrixMode = GL_MODELVIEW;
    protected int modified = 0;
    protected int usesMviMvit = 0; // 0 - none, 1 - Mvi, 2 - Mvit, 3 - MviMvit (ofc no Mvit w/o Mvi!)
    protected ProjectFloat projectFloat;

    public static final int DIRTY_MODELVIEW  = 1 << 0;
    public static final int DIRTY_PROJECTION = 1 << 1;
    public static final int DIRTY_TEXTURE    = 1 << 2;
}
