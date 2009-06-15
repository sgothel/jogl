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

package com.sun.opengl.util;

import com.sun.opengl.impl.ProjectFloat;                                                                       

import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

public class PMVMatrix implements GLMatrixFunc {

    public PMVMatrix() {
          projectFloat = new ProjectFloat();

          matrixIdent = BufferUtil.newFloatBuffer(1*16);
          projectFloat.gluMakeIdentityf(matrixIdent);
          matrixIdent.rewind();

          // T    Texture
          // P    Projection
          // Mv   ModelView
          // Mvi  Modelview-Inverse
          // Mvit Modelview-Inverse-Transpose
          // Pmv  P * Mv
          matrixTPMvMvitPmv = BufferUtil.newFloatBuffer(6*16);     // grouping T + P  + Mv + Mvi + Mvit + Pmv
          matrixPMvMvitPmv = slice(matrixTPMvMvitPmv, 1*16, 5*16); // grouping     P  + Mv + Mvi + Mvit + Pmv
          matrixT       = slice(matrixTPMvMvitPmv, 0*16, 1*16);    //          T
          matrixPMvMvit = slice(matrixTPMvMvitPmv, 1*16, 4*16);    // grouping     P  + Mv + Mvi + Mvit
          matrixPMvMvi  = slice(matrixTPMvMvitPmv, 1*16, 3*16);    // grouping     P  + Mv + Mvi
          matrixPMv     = slice(matrixTPMvMvitPmv, 1*16, 2*16);    // grouping     P  + Mv
          matrixP       = slice(matrixTPMvMvitPmv, 1*16, 1*16);    //              P
          matrixMv      = slice(matrixTPMvMvitPmv, 2*16, 1*16);    //                   Mv
          matrixMvi     = slice(matrixTPMvMvitPmv, 3*16, 1*16);    //                        Mvi
          matrixMvit    = slice(matrixTPMvMvitPmv, 4*16, 1*16);    //                              Mvit
          matrixPmv     = slice(matrixTPMvMvitPmv, 5*16, 1*16);    //                                     Pmv
          matrixTPMvMvitPmv.rewind();

          matrixMvit3 = BufferUtil.newFloatBuffer(3*3);

          localBuf = BufferUtil.newFloatBuffer(6*16);

          matrixMult=slice(localBuf, 0*16, 16);

          matrixTrans=slice(localBuf, 1*16, 16);
          projectFloat.gluMakeIdentityf(matrixTrans);

          matrixRot=slice(localBuf, 2*16, 16);
          projectFloat.gluMakeIdentityf(matrixRot);

          matrixScale=slice(localBuf, 3*16, 16);
          projectFloat.gluMakeIdentityf(matrixScale);

          matrixOrtho=slice(localBuf, 4*16, 16);
          projectFloat.gluMakeIdentityf(matrixOrtho);

          matrixFrustum=slice(localBuf, 5*16, 16);
          projectFloat.gluMakeZero(matrixFrustum);

          vec3f=new float[3];

          matrixPStack = new ArrayList();
          matrixMvStack= new ArrayList();

          // default values and mode
          glMatrixMode(GL_PROJECTION);
          glLoadIdentity();
          glMatrixMode(GL_MODELVIEW);
          glLoadIdentity();
          glMatrixMode(GL.GL_TEXTURE);
          glLoadIdentity();
          setDirty();
    }

    public void destroy() {
        if(null!=projectFloat) {
            projectFloat.destroy(); projectFloat=null;
        }

        if(null!=matrixIdent) {
            matrixIdent.clear(); matrixIdent=null;
        }
        if(null!=matrixTPMvMvitPmv) {
            matrixTPMvMvitPmv.clear(); matrixTPMvMvitPmv=null;
        }
        if(null!=matrixMvit3) {
            matrixMvit3.clear(); matrixMvit3=null;
        }
        if(null!=localBuf) {
            localBuf.clear(); localBuf=null;
        }

        if(null!=matrixPStack) {
            matrixPStack.clear(); matrixPStack=null;
        }
        vec3f=null;
        if(null!=matrixMvStack) {
            matrixMvStack.clear(); matrixMvStack=null;
        }
        if(null!=matrixPStack) {
        matrixPStack.clear(); matrixPStack=null;
        }
        if(null!=matrixTStack) {
            matrixTStack.clear(); matrixTStack=null;
        }

        matrixTPMvMvitPmv=null; matrixPMvMvit=null; matrixPMvMvitPmv=null; matrixPMvMvi=null; matrixPMv=null; 
        matrixP=null; matrixT=null; matrixMv=null; matrixMvi=null; matrixMvit=null; matrixPmv=null;
        matrixMult=null; matrixTrans=null; matrixRot=null; matrixScale=null; matrixOrtho=null; matrixFrustum=null;
    }

    private static FloatBuffer slice(FloatBuffer buf, int pos, int len) {
        buf.position(pos);
        buf.limit(pos + len);
        return buf.slice();
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

    public boolean update() {
        // if(0==modified) return false;

        // int res = modified;
        int res = DIRTY_MODELVIEW | DIRTY_PROJECTION ;
        if( (res&DIRTY_MODELVIEW)!=0 ) {
            setMviMvit();
        }
        if( (res&DIRTY_MODELVIEW)!=0 || (res&DIRTY_PROJECTION)!=0 ) {
            glMultMatrixf(matrixP, matrixMv, matrixPmv);
        }
        modified=0;
        return res!=0;
    }

    public final int  glGetMatrixMode() {
        return matrixMode;
    }

    public final FloatBuffer glGetTMatrixf() {
        return matrixT;
    }

    public final FloatBuffer glGetPMatrixf() {
        return matrixP;
    }

    public final FloatBuffer glGetMvMatrixf() {
        return matrixMv;
    }

    public final FloatBuffer glGetPMvMvitPmvMatrixf() {
        return matrixPMvMvitPmv;
    }

    public final FloatBuffer glGetPMvMvitMatrixf() {
        return matrixPMvMvit;
    }

    public final FloatBuffer glGetPMvMviMatrixf() {
        return matrixPMvMvi;
    }

    public final FloatBuffer glGetPMvMatrixf() {
        return matrixPMv;
    }

    public final FloatBuffer glGetMviMatrixf() {
        return matrixMvi;
    }

    public final FloatBuffer glGetPmvMatrixf() {
        return matrixPmv;
    }

    public final FloatBuffer glGetNormalMatrixf() {
        return matrixMvit3;
    }

   /*
    * @return the current matrix
    */
    public final FloatBuffer glGetMatrixf() {
        return glGetMatrixf(matrixMode);
    }

  /**
   * @param pname GL_MODELVIEW, GL_PROJECTION or GL.GL_TEXTURE
   * @return the given matrix
   */
    public final FloatBuffer glGetMatrixf(final int matrixName) {
        if(matrixName==GL_MODELVIEW) {
            return matrixMv;
        } else if(matrixName==GL_PROJECTION) {
            return matrixP;
        } else if(matrixName==GL.GL_TEXTURE) {
            return matrixT;
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

    public static final void glMultMatrixf(final FloatBuffer a, final FloatBuffer b, FloatBuffer p) {
       for (int i = 0; i < 4; i++) {
          final float ai0=a.get(i+0*4),  ai1=a.get(i+1*4),  ai2=a.get(i+2*4),  ai3=a.get(i+3*4);
          p.put(i+0*4 , ai0 * b.get(0+0*4) + ai1 * b.get(1+0*4) + ai2 * b.get(2+0*4) + ai3 * b.get(3+0*4) );
          p.put(i+1*4 , ai0 * b.get(0+1*4) + ai1 * b.get(1+1*4) + ai2 * b.get(2+1*4) + ai3 * b.get(3+1*4) );
          p.put(i+2*4 , ai0 * b.get(0+2*4) + ai1 * b.get(1+2*4) + ai2 * b.get(2+2*4) + ai3 * b.get(3+2*4) );
          p.put(i+3*4 , ai0 * b.get(0+3*4) + ai1 * b.get(1+3*4) + ai2 * b.get(2+3*4) + ai3 * b.get(3+3*4) );
       }
    }
    public static final void glMultMatrixf(final FloatBuffer a, final float[] b, int b_off, FloatBuffer p) {
       for (int i = 0; i < 4; i++) {
          final float ai0=a.get(i+0*4),  ai1=a.get(i+1*4),  ai2=a.get(i+2*4),  ai3=a.get(i+3*4);
          p.put(i+0*4 , ai0 * b[b_off+0+0*4] + ai1 * b[b_off+1+0*4] + ai2 * b[b_off+2+0*4] + ai3 * b[b_off+3+0*4] );
          p.put(i+1*4 , ai0 * b[b_off+0+1*4] + ai1 * b[b_off+1+1*4] + ai2 * b[b_off+2+1*4] + ai3 * b[b_off+3+1*4] );
          p.put(i+2*4 , ai0 * b[b_off+0+2*4] + ai1 * b[b_off+1+2*4] + ai2 * b[b_off+2+2*4] + ai3 * b[b_off+3+2*4] );
          p.put(i+3*4 , ai0 * b[b_off+0+3*4] + ai1 * b[b_off+1+3*4] + ai2 * b[b_off+2+3*4] + ai3 * b[b_off+3+3*4] );
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
            params.put(matrix);
            matrix.rewind();
        }
        params.position(pos);
    }
    public void glGetFloatv(int matrixGetName, float[] params, int params_offset) {
        if(matrixGetName==GL_MATRIX_MODE) {
            params[params_offset]=(float)matrixMode;
        } else {
            FloatBuffer matrix = glGetMatrixf(matrixGetName2MatrixModeName(matrixGetName));
            matrix.get(params, params_offset, 16);
            matrix.rewind();
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
            matrixMv.clear();
            matrixMv.put(values, offset, len);
            matrixMv.rewind();
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.clear();
            matrixP.put(values, offset, len);
            matrixP.rewind();
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixT.clear();
            matrixT.put(values, offset, len);
            matrixT.rewind();
            modified |= DIRTY_TEXTURE ;
        } 
    }

    public final void glLoadMatrixf(java.nio.FloatBuffer m) {
        int spos = m.position();
        if(matrixMode==GL_MODELVIEW) {
            matrixMv.clear();
            matrixMv.put(m);
            matrixMv.rewind();
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.clear();
            matrixP.put(m);
            matrixP.rewind();
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixT.clear();
            matrixT.put(m);
            matrixT.rewind();
            modified |= DIRTY_TEXTURE ;
        } 
        m.position(spos);
    }

    public final void glPopMatrix() {
        float[] stackEntry=null;
        if(matrixMode==GL_MODELVIEW) {
            stackEntry = (float[])matrixMvStack.remove(0);
        } else if(matrixMode==GL_PROJECTION) {
            stackEntry = (float[])matrixPStack.remove(0);
        } else if(matrixMode==GL.GL_TEXTURE) {
            stackEntry = (float[])matrixTStack.remove(0);
        } 
        glLoadMatrixf(stackEntry, 0);
    }

    public final void glPushMatrix() {
        float[] stackEntry = new float[1*16];
        if(matrixMode==GL_MODELVIEW) {
            matrixMv.get(stackEntry);
            matrixMv.rewind();
            matrixMvStack.add(0, stackEntry);
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.get(stackEntry);
            matrixP.rewind();
            matrixPStack.add(0, stackEntry);
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixT.get(stackEntry);
            matrixT.rewind();
            matrixTStack.add(0, stackEntry);
        }
    }

    public final void glLoadIdentity() {
        if(matrixMode==GL_MODELVIEW) {
            matrixMv.clear();
            matrixMv.put(matrixIdent);
            matrixMv.rewind();
            matrixIdent.rewind();
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            matrixP.clear();
            matrixP.put(matrixIdent);
            matrixP.rewind();
            matrixIdent.rewind();
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            matrixT.clear();
            matrixT.put(matrixIdent);
            matrixT.rewind();
            matrixIdent.rewind();
            modified |= DIRTY_TEXTURE ;
        } 
    }

    public final void glMultMatrixf(final FloatBuffer m) {
        if(matrixMode==GL_MODELVIEW) {
            glMultMatrixf(matrixMv, m, matrixMult);
            matrixMv.clear();
            matrixMv.put(matrixMult);
            matrixMv.rewind();
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            glMultMatrixf(matrixP, m, matrixMult);
            matrixP.clear();
            matrixP.put(matrixMult);
            matrixP.rewind();
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            glMultMatrixf(matrixT, m, matrixMult);
            matrixT.clear();
            matrixT.put(matrixMult);
            matrixT.rewind();
            modified |= DIRTY_TEXTURE ;
        } 
        matrixMult.rewind();
    }

    public void glMultMatrixf(float[] m, int m_offset) {
        if(matrixMode==GL_MODELVIEW) {
            glMultMatrixf(matrixMv, m, m_offset, matrixMult);
            matrixMv.clear();
            matrixMv.put(matrixMult);
            matrixMv.rewind();
            modified |= DIRTY_MODELVIEW ;
        } else if(matrixMode==GL_PROJECTION) {
            glMultMatrixf(matrixP, m, m_offset, matrixMult);
            matrixP.clear();
            matrixP.put(matrixMult);
            matrixP.rewind();
            modified |= DIRTY_PROJECTION ;
        } else if(matrixMode==GL.GL_TEXTURE) {
            glMultMatrixf(matrixT, m, m_offset, matrixMult);
            matrixT.clear();
            matrixT.put(matrixMult);
            matrixT.rewind();
            modified |= DIRTY_TEXTURE ;
        } 
        matrixMult.rewind();
    }

    public final void glTranslatef(final float x, final float y, final float z) {
        // Translation matrix: 
        //  1 0 0 x
        //  0 1 0 y
        //  0 0 1 z
        //  0 0 0 1
        matrixTrans.put(0+4*3, x);
        matrixTrans.put(1+4*3, y);
        matrixTrans.put(2+4*3, z);
        glMultMatrixf(matrixTrans);
    }

    public final void glRotatef(final float angdeg, float x, float y, float z) {
        float angrad = angdeg   * (float) Math.PI / 180;
        float c = (float)Math.cos(angrad);
        float ic= 1.0f - c; 
        float s = (float)Math.sin(angrad);

        vec3f[0]=x; vec3f[1]=y; vec3f[2]=z;
        projectFloat.normalize(vec3f);
        x = vec3f[0]; y = vec3f[1]; z = vec3f[2];

        // Rotation matrix:
        //      xx(1−c)+c  xy(1−c)+zs xz(1−c)-ys 0
        //      xy(1−c)-zs yy(1−c)+c  yz(1−c)+xs 0
        //      xz(1−c)+ys yz(1−c)-xs zz(1−c)+c  0
        //      0          0          0          1
        float xy = x*y;
        float xz = x*z;
        float xs = x*s;
        float ys = y*s;
        float yz = y*z;
        float zs = z*s;
        matrixRot.put(0*4+0, x*x*ic+c);
        matrixRot.put(0*4+1, xy*ic+zs);
        matrixRot.put(0*4+2, xz*ic-ys);

        matrixRot.put(1*4+0, xy*ic-zs);
        matrixRot.put(1*4+1, y*y*ic+c);
        matrixRot.put(1*4+2, yz*ic+xs);

        matrixRot.put(2*4+0, xz*ic+ys);
        matrixRot.put(2*4+1, yz*ic-xs);
        matrixRot.put(2*4+2, z*z*ic+c);

        glMultMatrixf(matrixRot);
    }

    public final void glScalef(final float x, final float y, final float z) {
        // Scale matrix: 
        //  x 0 0 0
        //  0 y 0 0
        //  0 0 z 0
        //  0 0 0 1
        matrixScale.put(0+4*0, x);
        matrixScale.put(1+4*1, y);
        matrixScale.put(2+4*2, z);

        glMultMatrixf(matrixScale);
    }

    public final void glOrthof(final float left, final float right, final float bottom, final float top, final float zNear, final float zFar) {
        // Ortho matrix: 
        //  2/dx  0     0    tx
        //  0     2/dy  0    ty
        //  0     0     2/dz tz
        //  0     0     0    1
        float dx=right-left;
        float dy=top-bottom;
        float dz=zFar-zNear;
        float tx=-1.0f*(right+left)/dx;
        float ty=-1.0f*(top+bottom)/dy;
        float tz=-1.0f*(zFar+zNear)/dz;

        matrixOrtho.put(0+4*0, 2.0f/dx);
        matrixOrtho.put(1+4*1, 2.0f/dy);
        matrixOrtho.put(2+4*2, -2.0f/dz);
        matrixOrtho.put(0+4*3, tx);
        matrixOrtho.put(1+4*3, ty);
        matrixOrtho.put(2+4*3, tz);

        glMultMatrixf(matrixOrtho);
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
        float zNear2 = 2.0f*zNear;
        float dx=right-left;
        float dy=top-bottom;
        float dz=zFar-zNear;
        float A=(right+left)/dx;
        float B=(top+bottom)/dy;
        float C=-1.0f*(zFar+zNear)/dz;
        float D=-2.0f*(zFar*zNear)/dz;

        matrixFrustum.put(0+4*0, zNear2/dx);
        matrixFrustum.put(1+4*1, zNear2/dy);
        matrixFrustum.put(2+4*2, C);

        matrixFrustum.put(0+4*2, A);
        matrixFrustum.put(1+4*2, B);

        matrixFrustum.put(2+4*3, D);
        matrixFrustum.put(3+4*2, -1.0f);

        glMultMatrixf(matrixFrustum);
    }

    //
    // private 
    //

    private final void setMviMvit() {
        if(!projectFloat.gluInvertMatrixf(matrixMv, matrixMvi)) {
            throw new GLException("Invalid source Mv matrix, can't compute inverse");
        }

        // transpose matrix 
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                matrixMvit.put(j+i*4, matrixMvi.get(i+j*4));
            }
        }

        // fetch 3x3
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                matrixMvit3.put(i+j*3, matrixMvit.get(i+j*4));
            }
        }
    }

    protected FloatBuffer matrixIdent;
    protected FloatBuffer matrixTPMvMvitPmv, matrixPMvMvit, matrixPMvMvitPmv, matrixPMvMvi, matrixPMv, matrixP, matrixT, matrixMv, matrixMvi, matrixMvit, matrixPmv;
    protected FloatBuffer matrixMvit3;
    protected FloatBuffer localBuf, matrixMult, matrixTrans, matrixRot, matrixScale, matrixOrtho, matrixFrustum;
    protected float[] vec3f;
    protected List/*FloatBuffer*/ matrixTStack, matrixPStack, matrixMvStack;
    protected int matrixMode = GL_MODELVIEW;
    protected int modified = 0;
    protected ProjectFloat projectFloat;

    public static final int DIRTY_MODELVIEW  = 1 << 0;
    public static final int DIRTY_PROJECTION = 1 << 1;
    public static final int DIRTY_TEXTURE    = 1 << 2;
}
