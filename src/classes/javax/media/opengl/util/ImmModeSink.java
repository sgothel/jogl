
package javax.media.opengl.util;

import javax.media.opengl.*;
import java.nio.*;
import java.util.Iterator;
import java.util.ArrayList;

public class ImmModeSink {

  public static final boolean DEBUG_BEGIN_END = false;
  public static final boolean DEBUG_DRAW = false;
  public static final boolean FLOAT2FIXED = false;

  public static final int GL_QUADS      = 0x0007;
  public static final int GL_QUAD_STRIP = 0x0008;
  public static final int GL_POLYGON    = 0x0009;

  public ImmModeSink(int glDataType, int glDrawUsage,
                     int vComps, int nComps, int cComps, int tComps, int initialSize) {

    vboSet = new  VBOSet(glDataType, glDrawUsage, vComps, nComps, cComps, tComps, initialSize);
    this.vboSetList   = new ArrayList();
  }

  private void destroyList(GL gl) {
    for(Iterator i=vboSetList.iterator(); i.hasNext() ; ) {
        ((VBOSet)i.next()).destroy(gl);
    }
    vboSetList.clear();
  }

  public void destroy(GL gl) {
    destroyList(gl);

    vboSet.destroy(gl);
  }

  public void reset() {
    reset(null);
  }

  public void reset(GL gl) {
    destroyList(gl);
    vboSet.reset(gl);
  }

  public String toString() {
    return "ImmModeSink[listsz: "+vboSetList.size()+
                       ",\n"+vboSet+
                       "]";
  }

  public void draw(GL gl, boolean disableBufferAfterDraw) {
    if(DEBUG_DRAW) {
        Exception e = new Exception("ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
        e.printStackTrace();
    }
    int n=0;
    for(Iterator i=vboSetList.iterator(); i.hasNext() ; n++) {
        ((VBOSet)i.next()).draw(gl, disableBufferAfterDraw, n);
    }
  }

  public void glBegin(int mode) {
    if(DEBUG_BEGIN_END) {
        Exception e = new Exception("ImmModeSink.glBegin("+vboSet.mode+"):\n\t"+this);
        e.printStackTrace();
    }
    vboSet.modeOrig = mode;
    switch(mode) {
        case GL_QUADS:
            mode=GL.GL_TRIANGLE_STRIP;
            break;
        case GL_QUAD_STRIP:
            mode=GL.GL_TRIANGLE_STRIP;
            break;
        case GL_POLYGON:
            mode=GL.GL_LINES;
            break;
    }
    vboSet.mode = mode;
    vboSet.checkSeal(false);
  }

  public final void glEnd(GL gl) {
      glEnd(gl, true);
  }

  public void glEnd(GL gl, boolean immediateDraw) {
    if(DEBUG_BEGIN_END) {
        Exception e = new Exception("ImmModeSink START glEnd(immediate: "+immediateDraw+"):\n\t"+this);
        e.printStackTrace();
    }
    if(immediateDraw) {
        vboSet.seal(gl, false);
        vboSet.draw(gl, true, -1);
        reset(gl);
    } else {
        vboSet.seal(gl, true);
        vboSetList.add(vboSet);
        vboSet = vboSet.regenerate();
    }
  }

  public final void glVertex2f(float x, float y) {
    vboSet.glVertex2f(x,y);
  }

  public final void glVertex3f(float x, float y, float z) {
    vboSet.glVertex3f(x,y,z);
  }

  public final void glNormal3f(float x, float y, float z) {
    vboSet.glNormal3f(x,y,z);
  }

  public final void glColor3f(float x, float y, float z) {
    vboSet.glColor3f(x,y,z);
  }

  public final void glTexCoord2f(float x, float y) {
    vboSet.glTexCoord2f(x,y);
  }

  public final void glTexCoord3f(float x, float y, float z) {
    vboSet.glTexCoord3f(x,y,z);
  }

  private VBOSet vboSet;
  private ArrayList vboSetList;

  protected static class VBOSet {
    protected VBOSet(int glDataType, int glDrawUsage,
                   int vComps, int nComps, int cComps, int tComps, int initialSize) {
        nComps = 0;
        tComps = 0;
        if(FLOAT2FIXED && glDataType==GL.GL_FLOAT) {
            glDataType=GL.GL_FIXED;
        }
        this.glDataType=glDataType;
        this.glDrawUsage=glDrawUsage;
        this.vComps=vComps;
        this.nComps=nComps;
        this.cComps=cComps;
        this.tComps=tComps;
        this.initialSize=initialSize;

        this.vertexVBO   = VBOBufferDraw.create(GL2ES1.GL_VERTEX_ARRAY,        glDataType, glDrawUsage, vComps, initialSize);
        this.normalVBO   = VBOBufferDraw.create(GL2ES1.GL_NORMAL_ARRAY,        glDataType, glDrawUsage, nComps, initialSize);
        this.colorVBO    = VBOBufferDraw.create(GL2ES1.GL_COLOR_ARRAY,         glDataType, glDrawUsage, cComps, initialSize);
        this.texcoordVBO = VBOBufferDraw.create(GL2ES1.GL_TEXTURE_COORD_ARRAY, glDataType, glDrawUsage, tComps, initialSize);

        this.sealed=false;
        this.mode = -1;
        this.modeOrig = -1;
    }

    protected final VBOSet regenerate() {
        return new VBOSet(glDataType, glDrawUsage, vComps, nComps, cComps, tComps, initialSize);
    }

    protected void destroy(GL gl) {
        vertexVBO.destroy(gl);
        normalVBO.destroy(gl);
        colorVBO.destroy(gl);
        texcoordVBO.destroy(gl);

        this.mode = -1;
        this.modeOrig = -1;
        this.sealed=false;
    }

    protected void reset(GL gl) {
        vertexVBO.reset(gl);
        normalVBO.reset(gl);
        colorVBO.reset(gl);
        texcoordVBO.reset(gl);

        this.mode = -1;
        this.modeOrig = -1;
        this.sealed=false;
    }

    public String toString() {
        return "VBOSet[mode "+mode+ 
                       ", modeOrig "+modeOrig+ 
                       ", sealed "+sealed+ 
                       ",\n\t vertexVBO "+vertexVBO+
                       ",\n\t normalVBO "+normalVBO+
                       ",\n\t colorVBO "+colorVBO+
                       ",\n\t texcoordVBO "+texcoordVBO+
                       "]";
    }

    protected void checkSeal(boolean test) throws GLException {
        if(mode<0) {
                throw new GLException("No mode set yet, call glBegin(mode) first:\n\t"+this); 
        }
        if(sealed!=test) {
            if(test) {
                throw new GLException("Not Sealed yet, call glEnd() first:\n\t"+this); 
            } else {
                throw new GLException("Already Sealed, can't modify VBO after glEnd():\n\t"+this); 
            }
        }
    }

    protected void rewind() {
        checkSeal(true);

        vertexVBO.rewind();
        normalVBO.rewind();
        colorVBO.rewind();
        texcoordVBO.rewind();
    }

    protected void seal(GL gl, boolean disableBufferAfterSeal)
    {
        checkSeal(false);
        sealed = true;

        vertexVBO.seal(gl, disableBufferAfterSeal);
        normalVBO.seal(gl, disableBufferAfterSeal);
        colorVBO.seal(gl, disableBufferAfterSeal);
        texcoordVBO.seal(gl, disableBufferAfterSeal);
    }

    protected void draw(GL gl, boolean disableBufferAfterDraw, int i)
    {
        if(DEBUG_DRAW) {
            Exception e = new Exception("ImmModeSink.draw["+i+"](disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
            e.printStackTrace();
        }
        vertexVBO.enableBuffer(gl);
        normalVBO.enableBuffer(gl);
        colorVBO.enableBuffer(gl);
        texcoordVBO.enableBuffer(gl);

        if (vertexVBO.getBuffer()!=null) {
            gl.glDrawArrays(mode, 0, vertexVBO.getVerticeNumber());
        }

        if(disableBufferAfterDraw) {
            vertexVBO.disableBuffer(gl);
            normalVBO.disableBuffer(gl);
            colorVBO.disableBuffer(gl);
            texcoordVBO.disableBuffer(gl);
        }
    }

    protected void glVertex2f(float x, float y) {
        checkSeal(false);
        vertexVBO.putf(x);
        if(vertexVBO.getComponents()>1) 
            vertexVBO.putf(y);
        vertexVBO.padding(2);
    }

    protected void glVertex3f(float x, float y, float z) {
        checkSeal(false);
        vertexVBO.putf(x);
        if(vertexVBO.getComponents()>1) 
            vertexVBO.putf(y);
        if(vertexVBO.getComponents()>2) 
            vertexVBO.putf(z);
        vertexVBO.padding(3);
    }

    protected void glNormal3f(float x, float y, float z) {
        checkSeal(false);
        normalVBO.putf(x);
        if(normalVBO.getComponents()>1) 
            normalVBO.putf(y);
        if(normalVBO.getComponents()>2) 
            normalVBO.putf(z);
        normalVBO.padding(3);
    }

    protected void glColor3f(float x, float y, float z) {
        checkSeal(false);
        colorVBO.putf(x);
        if(colorVBO.getComponents()>1) 
            colorVBO.putf(y);
        if(colorVBO.getComponents()>2) 
            colorVBO.putf(z);
        colorVBO.padding(3);
    }

    protected void glTexCoord2f(float x, float y) {
        checkSeal(false);
        texcoordVBO.putf(x);
        if(texcoordVBO.getComponents()>1) 
            texcoordVBO.putf(y);
        texcoordVBO.padding(2);
    }

    protected void glTexCoord3f(float x, float y, float z) {
        checkSeal(false);
        texcoordVBO.putf(x);
        if(texcoordVBO.getComponents()>1) 
            texcoordVBO.putf(y);
        if(texcoordVBO.getComponents()>2) 
            texcoordVBO.putf(z);
        texcoordVBO.padding(3);
    }

    VBOBufferDraw vertexVBO;
    VBOBufferDraw normalVBO;
    VBOBufferDraw colorVBO;
    VBOBufferDraw texcoordVBO;
    int mode, modeOrig;
    int glDataType, glDrawUsage, vComps, nComps, cComps, tComps, initialSize;
    boolean sealed;
  }

}

