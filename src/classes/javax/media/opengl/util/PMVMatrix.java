
package javax.media.opengl.util;

import javax.media.opengl.*;
import com.sun.opengl.impl.ProjectFloat;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;

public class PMVMatrix {

    protected ProjectFloat pvmProjectf = null;
    protected FloatBuffer matrixPMV, matrixP, matrixMV;
    protected FloatBuffer matrixTemp, matrixTrans, matrixRot, matrixScale, matrixOrtho, matrixPersp, matrixFrustum;
    protected float[] vec3f;
    protected List/*FloatBuffer*/ matrixPStack, matrixMVStack;
    protected int matrixMode = GL.GL_MODELVIEW;
    protected boolean modifiedPMV = false;

    public PMVMatrix() {
          pvmProjectf = new ProjectFloat();

          matrixPMV = BufferUtil.newFloatBuffer(2*16);
          matrixP  = slice(matrixPMV, 0*16, 16);
          matrixMV = slice(matrixPMV, 1*16, 16);
          matrixPMV.rewind();

          FloatBuffer buf = BufferUtil.newFloatBuffer(7*16);

          matrixTemp=slice(buf, 0*16, 16);

          matrixTrans=slice(buf, 1*16, 16);
          pvmProjectf.gluMakeIdentityf(matrixTrans);

          matrixRot=slice(buf, 2*16, 16);
          pvmProjectf.gluMakeIdentityf(matrixRot);

          matrixScale=slice(buf, 3*16, 16);
          pvmProjectf.gluMakeIdentityf(matrixScale);

          matrixOrtho=slice(buf, 4*16, 16);
          pvmProjectf.gluMakeIdentityf(matrixOrtho);

          matrixFrustum=slice(buf, 5*16, 16);
          pvmProjectf.gluMakeZero(matrixFrustum);

          matrixPersp=slice(buf, 6*16, 16);
          pvmProjectf.gluMakeIdentityf(matrixPersp);

          vec3f=new float[3];

          matrixPStack = new ArrayList();
          matrixMVStack= new ArrayList();

          // default values and mode
          glMatrixMode(GL.GL_PROJECTION);
          glLoadIdentity();
          glMatrixMode(GL.GL_MODELVIEW);
          glLoadIdentity();
          modifiedPMV = true;
    }

    private static FloatBuffer slice(FloatBuffer buf, int pos, int len) {
        buf.position(pos);
        buf.limit(pos + len);
        return buf.slice();
    }

    public boolean isDirty() {
        return modifiedPMV;
    }

    public void clear() {
        modifiedPMV=false;
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

    public final FloatBuffer glGetPMVMatrixf() {
        return matrixPMV;
    }

    public final FloatBuffer glGetMatrixf() {
        return glGetMatrixf(matrixMode);
    }

    public final FloatBuffer glGetMatrixf(int matrixName) {
        if(matrixName==GL.GL_MODELVIEW) {
            return matrixMV;
        } else if(matrixName==GL.GL_PROJECTION) {
            return matrixP;
        }
        return null;
    }

    public void glLoadMatrixf(java.nio.FloatBuffer m) {
        if(matrixMode==GL.GL_MODELVIEW) {
            matrixMV.clear();
            matrixMV.put(m);
            matrixMV.rewind();
        } else if(matrixMode==GL.GL_PROJECTION) {
            matrixP.clear();
            matrixP.put(m);
            matrixP.rewind();
        } 
        modifiedPMV = true;
    }

    public void glPopMatrix() {
        if(matrixMode==GL.GL_MODELVIEW) {
            matrixMV=(FloatBuffer)matrixMVStack.remove(0);
        } else if(matrixMode==GL.GL_PROJECTION) {
            matrixP=(FloatBuffer)matrixPStack.remove(0);
        } 
        modifiedPMV = true;
    }

    public void glPushMatrix() {
        if(matrixMode==GL.GL_MODELVIEW) {
            matrixMVStack.add(0, matrixMV);
        } else if(matrixMode==GL.GL_PROJECTION) {
            matrixPStack.add(0, matrixP);
        } 
    }

    public void glLoadIdentity() {
        if(matrixMode==GL.GL_MODELVIEW) {
            matrixMV.clear();
            pvmProjectf.gluMakeIdentityf(matrixMV);
            matrixMV.rewind();
        } else if(matrixMode==GL.GL_PROJECTION) {
            matrixP.clear();
            pvmProjectf.gluMakeIdentityf(matrixP);
            matrixP.rewind();
        } 
        modifiedPMV = true;
    }

    public void glMultMatrixf(FloatBuffer m) {
        if(matrixMode==GL.GL_MODELVIEW) {
            pvmProjectf.gluMultMatricesf(m, matrixMV, matrixTemp);
            matrixMV.clear();
            matrixMV.put(matrixTemp);
            matrixMV.rewind();
        } else if(matrixMode==GL.GL_PROJECTION) {
            pvmProjectf.gluMultMatricesf(m, matrixP, matrixTemp);
            matrixP.clear();
            matrixP.put(matrixTemp);
            matrixP.rewind();
        } 
        matrixTemp.rewind();
        modifiedPMV = true;
    }

    public void glTranslatef(float x, float y, float z) {
        // Translation matrix: 
        //  1 0 0 0
        //  0 1 0 0
        //  0 0 1 0
        //  x y z 1
        matrixTrans.put(3*4+0, x);
        matrixTrans.put(3*4+1, y);
        matrixTrans.put(3*4+2, z);

        glMultMatrixf(matrixTrans);
    }

    public void glRotatef(float angdeg, float x, float y, float z) {
        float angrad = angdeg   * (float) Math.PI / 180;
        float c = (float)Math.cos(angrad);
        float ic= 1.0f - c; 
        float s = (float)Math.sin(angrad);

        vec3f[0]=x; vec3f[1]=y; vec3f[2]=z;
        pvmProjectf.normalize(vec3f);
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

    public void glScalef(float x, float y, float z) {
        // Scale matrix: 
        //  x 0 0 0
        //  0 y 0 0
        //  0 0 z 0
        //  0 0 0 1
        matrixScale.put(0*4+0, x);
        matrixScale.put(1*4+1, y);
        matrixScale.put(2*4+2, z);

        glMultMatrixf(matrixScale);
    }

    public void glOrthof(float left, float right, float bottom, float top, float zNear, float zFar) {
        // Ortho matrix: 
        //  2/dx  0     0    0
        //  0     2/dy  0    0
        //  0     0     2/dz 0
        //  tx    tx    tx   1
        float dx=right-left;
        float dy=top-bottom;
        float dz=zFar-zNear;
        float tx=-1.0f*(right+left)/dx;
        float ty=-1.0f*(top+bottom)/dy;
        float tz=-1.0f*(zFar+zNear)/dz;

        matrixOrtho.put(0*4+0, 2.0f/dx);
        matrixOrtho.put(1*4+1, 2.0f/dy);
        matrixOrtho.put(2*4+2, -2.0f/dz);
        matrixOrtho.put(3*4+0, tx);
        matrixOrtho.put(3*4+1, ty);
        matrixOrtho.put(3*4+2, tz);

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

        matrixFrustum.put(0*4+0, zNear2/dx);
        matrixFrustum.put(1*4+1, zNear2/dy);
        matrixFrustum.put(2*4+2, C);
        matrixFrustum.put(0*4+2, A);
        matrixFrustum.put(1*4+2, B);
        matrixFrustum.put(2*4+3, D);
        matrixFrustum.put(3*4+2, -1.0f);

        glMultMatrixf(matrixFrustum);
    }

    public void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
        float radians = fovy/2 * (float) Math.PI / 180;

        float sine, cotangent, deltaZ;

        deltaZ = zFar - zNear;
        sine = (float) Math.sin(radians);

        if ((deltaZ == 0.0f) || (sine == 0.0f) || (aspect == 0.0f)) {
          return;
        }

        cotangent = (float) Math.cos(radians) / sine;

        matrixPersp.put(0 * 4 + 0, cotangent / aspect);
        matrixPersp.put(1 * 4 + 1, cotangent);
        matrixPersp.put(2 * 4 + 2, - (zFar + zNear) / deltaZ);
        matrixPersp.put(2 * 4 + 3, -1);
        matrixPersp.put(3 * 4 + 2, -2 * zNear * zFar / deltaZ);
        matrixPersp.put(3 * 4 + 3, 0);

        glMultMatrixf(matrixPersp);
    }
}


