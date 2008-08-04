
package javax.media.opengl.util;

import javax.media.opengl.*;
import com.sun.opengl.impl.ProjectFloat;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;

public class PMVMatrix {

    public PMVMatrix() {
          projectFloat = new ProjectFloat();

          matrixIdent = BufferUtil.newFloatBuffer(1*16);
          projectFloat.gluMakeIdentityf(matrixIdent);
          matrixIdent.rewind();

          matrixPMvMviT = BufferUtil.newFloatBuffer(4*16);     // grouping P  + Mv + Mvi + MviT
          matrixPMvMvi = slice(matrixPMvMviT, 0*16, 3*16);     // grouping P  + Mv + Mvi
          matrixPMv    = slice(matrixPMvMviT, 0*16, 2*16);     // grouping P  + Mv
          matrixP      = slice(matrixPMvMviT, 0*16, 1*16);
          matrixMv     = slice(matrixPMvMviT, 1*16, 1*16);
          matrixMvi    = slice(matrixPMvMviT, 2*16, 1*16);
          matrixMvit   = slice(matrixPMvMviT, 3*16, 1*16);
          matrixPMvMviT.rewind();

          matrixMvit3 = BufferUtil.newFloatBuffer(3*3);

          FloatBuffer buf = BufferUtil.newFloatBuffer(6*16);

          matrixMult=slice(buf, 0*16, 16);

          matrixTrans=slice(buf, 1*16, 16);
          projectFloat.gluMakeIdentityf(matrixTrans);

          matrixRot=slice(buf, 2*16, 16);
          projectFloat.gluMakeIdentityf(matrixRot);

          matrixScale=slice(buf, 3*16, 16);
          projectFloat.gluMakeIdentityf(matrixScale);

          matrixOrtho=slice(buf, 4*16, 16);
          projectFloat.gluMakeIdentityf(matrixOrtho);

          matrixFrustum=slice(buf, 5*16, 16);
          projectFloat.gluMakeZero(matrixFrustum);

          vec3f=new float[3];

          matrixPStack = new ArrayList();
          matrixMvStack= new ArrayList();

          // default values and mode
          glMatrixMode(GL.GL_PROJECTION);
          glLoadIdentity();
          glMatrixMode(GL.GL_MODELVIEW);
          glLoadIdentity();
          modified = true;
    }

    private static FloatBuffer slice(FloatBuffer buf, int pos, int len) {
        buf.position(pos);
        buf.limit(pos + len);
        return buf.slice();
    }

    public boolean isDirty() {
        return modified;
    }

    public boolean update() {
        boolean res = modified;
        if(res) {
            setMviMvit();
            modified=false;
        }
        return res;
    }

    public final int  glGetMatrixMode() {
        return matrixMode;
    }

    public void glMatrixMode(int matrixName) {
        switch(matrixName) {
            case GL.GL_MODELVIEW:
            case GL.GL_PROJECTION:
                break;
            default:
              throw new GLUnsupportedException("unsupported matrixName: "+matrixName);
        }
        matrixMode = matrixName;
    }

    public final FloatBuffer glGetPMvMviTMatrixf() {
        return matrixPMvMviT;
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

    public final FloatBuffer glGetNormalMatrixf() {
        return matrixMvit3;
    }

    public final FloatBuffer glGetMatrixf() {
        return glGetMatrixf(matrixMode);
    }

    public final FloatBuffer glGetMatrixf(int matrixName) {
        if(matrixName==GL.GL_MODELVIEW) {
            return matrixMv;
        } else if(matrixName==GL.GL_PROJECTION) {
            return matrixP;
        }
        return null;
    }

    public void glLoadMatrixf(float[] values, int offset) {
        int len = values.length-offset;
        if(matrixMode==GL.GL_MODELVIEW) {
            matrixMv.clear();
            matrixMv.put(values, offset, len);
            matrixMv.rewind();
        } else if(matrixMode==GL.GL_PROJECTION) {
            matrixP.clear();
            matrixP.put(values, offset, len);
            matrixP.rewind();
        } 
        modified = true;
    }

    public void glLoadMatrixf(java.nio.FloatBuffer m) {
        int spos = m.position();
        if(matrixMode==GL.GL_MODELVIEW) {
            matrixMv.clear();
            matrixMv.put(m);
            matrixMv.rewind();
        } else if(matrixMode==GL.GL_PROJECTION) {
            matrixP.clear();
            matrixP.put(m);
            matrixP.rewind();
        } 
        m.position(spos);
        modified = true;
    }

    public void glPopMatrix() {
        float[] stackEntry=null;
        if(matrixMode==GL.GL_MODELVIEW) {
            stackEntry = (float[])matrixMvStack.remove(0);
        } else if(matrixMode==GL.GL_PROJECTION) {
            stackEntry = (float[])matrixPStack.remove(0);
        } 
        glLoadMatrixf(stackEntry, 0);
    }

    public void glPushMatrix() {
        float[] stackEntry = new float[1*16];
        if(matrixMode==GL.GL_MODELVIEW) {
            matrixMv.get(stackEntry);
            matrixMv.rewind();
            matrixMvStack.add(0, stackEntry);
        } else if(matrixMode==GL.GL_PROJECTION) {
            matrixP.get(stackEntry);
            matrixP.rewind();
            matrixPStack.add(0, stackEntry);
        }
    }

    public void glLoadIdentity() {
        if(matrixMode==GL.GL_MODELVIEW) {
            matrixMv.clear();
            matrixMv.put(matrixIdent);
            matrixMv.rewind();
            matrixIdent.rewind();
        } else if(matrixMode==GL.GL_PROJECTION) {
            matrixP.clear();
            matrixP.put(matrixIdent);
            matrixP.rewind();
            matrixIdent.rewind();
        } 
        modified = true;
    }

    public void glMultMatrixf(FloatBuffer a, FloatBuffer b, FloatBuffer p) {
       for (int i = 0; i < 4; i++) {
          final float ai0=a.get(i+0*4),  ai1=a.get(i+1*4),  ai2=a.get(i+2*4),  ai3=a.get(i+3*4);
          p.put(i+0*4 , ai0 * b.get(0+0*4) + ai1 * b.get(1+0*4) + ai2 * b.get(2+0*4) + ai3 * b.get(3+0*4) );
          p.put(i+1*4 , ai0 * b.get(0+1*4) + ai1 * b.get(1+1*4) + ai2 * b.get(2+1*4) + ai3 * b.get(3+1*4) );
          p.put(i+2*4 , ai0 * b.get(0+2*4) + ai1 * b.get(1+2*4) + ai2 * b.get(2+2*4) + ai3 * b.get(3+2*4) );
          p.put(i+3*4 , ai0 * b.get(0+3*4) + ai1 * b.get(1+3*4) + ai2 * b.get(2+3*4) + ai3 * b.get(3+3*4) );
       }
       // or .. projectFloat.gluMultMatricesf(b, a, p); 
    }

    public void glMultMatrixf(FloatBuffer m) {
        if(matrixMode==GL.GL_MODELVIEW) {
            glMultMatrixf(matrixMv, m, matrixMult);
            matrixMv.clear();
            matrixMv.put(matrixMult);
            matrixMv.rewind();
        } else if(matrixMode==GL.GL_PROJECTION) {
            glMultMatrixf(matrixP, m, matrixMult);
            matrixP.clear();
            matrixP.put(matrixMult);
            matrixP.rewind();
        } 
        matrixMult.rewind();
        modified = true;
    }

    public void glTranslatef(float x, float y, float z) {
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

    public void glRotatef(float angdeg, float x, float y, float z) {
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
        if(false) {
        matrixRot.put(0+4*0, x*x*ic+c);
        matrixRot.put(0+4*1, xy*ic+zs);
        matrixRot.put(0+4*2, xz*ic-ys);

        matrixRot.put(1+4*0, xy*ic+zs);
        matrixRot.put(1+4*1, y*y*ic+c);
        matrixRot.put(1+4*2, yz*ic-xs);

        matrixRot.put(2+4*0, xz*ic-ys);
        matrixRot.put(2+4*1, yz*ic+xs);
        matrixRot.put(2+4*2, z*z*ic+c);
        } else {
        matrixRot.put(0*4+0, x*x*ic+c);
        matrixRot.put(0*4+1, xy*ic+zs);
        matrixRot.put(0*4+2, xz*ic-ys);

        matrixRot.put(1*4+0, xy*ic-zs);
        matrixRot.put(1*4+1, y*y*ic+c);
        matrixRot.put(1*4+2, yz*ic+xs);

        matrixRot.put(2*4+0, xz*ic+ys);
        matrixRot.put(2*4+1, yz*ic-xs);
        matrixRot.put(2*4+2, z*z*ic+c);
        }

        glMultMatrixf(matrixRot);
    }

    public void glScalef(float x, float y, float z) {
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

    public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar) {
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

    public void glFrustumf(float left, float right, float bottom, float top, float zNear, float zFar) {
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

    public void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
      float top=(float)Math.tan(fovy*((float)Math.PI)/360.0f)*zNear;
      float bottom=-1.0f*top;
      float left=aspect*bottom;
      float right=aspect*top;
      glFrustumf(left, right, bottom, top, zNear, zFar);
    }

    private void setMviMvit() {
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
    protected FloatBuffer matrixPMvMviT, matrixPMvMvi, matrixPMv, matrixP, matrixMv, matrixMvi, matrixMvit;
    protected FloatBuffer matrixMvit3;
    protected FloatBuffer matrixMult, matrixTrans, matrixRot, matrixScale, matrixOrtho, matrixFrustum;
    protected float[] vec3f;
    protected List/*FloatBuffer*/ matrixPStack, matrixMvStack;
    protected int matrixMode = GL.GL_MODELVIEW;
    protected boolean modified = false;
    protected ProjectFloat projectFloat;

}


