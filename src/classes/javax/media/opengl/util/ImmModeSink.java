
package javax.media.opengl.util;

import javax.media.opengl.*;
import com.sun.opengl.impl.GLReflection;
import java.nio.*;
import java.util.Iterator;
import java.util.ArrayList;

public class ImmModeSink {

  public static final boolean DEBUG_BEGIN_END = false;
  public static final boolean DEBUG_DRAW = false;

  // public static final int GL_QUADS      = 0x0007; // Needs data manipulation
  public static final int GL_QUAD_STRIP = 0x0008;
  public static final int GL_POLYGON    = 0x0009;

  /**
   * Uses a GL2ES1, or ES2 fixed function emulation immediate mode sink
   */
  public static ImmModeSink createFixed(int glBufferUsage, int initialSize,
                                        int vComps, int vDataType,
                                        int cComps, int cDataType, 
                                        int nComps, int nDataType, 
                                        int tComps, int tDataType) {
    return new ImmModeSink(glBufferUsage, initialSize, 
                           vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, false);
  }

  /**
   * Uses a GL2ES2 GLSL shader immediate mode sink.
   * To issue the draw() command,
   * a ShaderState must be current, using ShaderState.glUseProgram().
   *
   * @see #draw(GL, boolean)
   * @see javax.media.opengl.glsl.ShaderState#glUseProgram(GL2ES2, boolean)
   * @see javax.media.opengl.glsl.ShaderState#getCurrent()
   */
  public static ImmModeSink createGLSL(int glBufferUsage, int initialSize,
                                       int vComps, int vDataType,
                                       int cComps, int cDataType, 
                                       int nComps, int nDataType, 
                                       int tComps, int tDataType) {
    return new ImmModeSink(glBufferUsage, initialSize, 
                           vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, true);
  }

  public static boolean usesVBO() { return vboUsage; }

  public static void setVBOUsage(boolean v) { vboUsage = v; }

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
    StringBuffer sb = new StringBuffer("ImmModeSink[");
    sb.append(",\n\tVBO list: "+vboSetList.size()+" [");
    for(Iterator i=vboSetList.iterator(); i.hasNext() ; ) {
        sb.append("\n\t");
        sb.append( (VBOSet)i.next() );
    }
    if(vboSetList.size()>0) {
        sb.append("\n\t],\nVBO current: NOP]");
    } else {
        sb.append("\n\t],\nVBO current: \n");
        sb.append(vboSet);
        sb.append("\n]");
    }
    return sb.toString();
  }

  public void draw(GL gl, boolean disableBufferAfterDraw) {
    if(DEBUG_DRAW) {
        Exception e = new Exception("ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
        e.printStackTrace();
    }
    int n=0;
    for(Iterator i=vboSetList.iterator(); i.hasNext() ; n++) {
        ((VBOSet)i.next()).draw(gl, null, disableBufferAfterDraw, n);
    }
  }

  public void draw(GL gl, Buffer indices, boolean disableBufferAfterDraw) {
    if(DEBUG_DRAW) {
        Exception e = new Exception("ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
        e.printStackTrace();
    }
    int n=0;
    for(Iterator i=vboSetList.iterator(); i.hasNext() ; n++) {
        ((VBOSet)i.next()).draw(gl, indices, disableBufferAfterDraw, n);
    }
  }

  public void glBegin(int mode) {
    if(DEBUG_BEGIN_END) {
        Exception e = new Exception("ImmModeSink.glBegin("+vboSet.mode+"):\n\t"+this);
        e.printStackTrace();
    }
    vboSet.modeOrig = mode;
    switch(mode) {
        // Needs data manipulation ..
        //case GL_QUADS:
        //    mode=GL.GL_LINES;
        //    break;
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
      glEnd(gl, null, true);
  }

  public void glEnd(GL gl, boolean immediateDraw) {
      glEnd(gl, null, immediateDraw);
  }

  public final void glEnd(GL gl, Buffer indices) {
      glEnd(gl, indices, true);
  }

  private void glEnd(GL gl, Buffer indices, boolean immediateDraw) {
    if(DEBUG_BEGIN_END) {
        Exception e = new Exception("ImmModeSink START glEnd(immediate: "+immediateDraw+"):\n\t"+this);
        e.printStackTrace();
    }
    if(immediateDraw) {
        vboSet.seal(gl, false);
        vboSet.draw(gl, indices, true, -1);
        reset(gl);
    } else {
        vboSet.seal(gl, true);
        vboSetList.add(vboSet);
        vboSet = vboSet.regenerate();
    }
  }

  public void glVertexv(Buffer v) {
    vboSet.glVertexv(v);
  }
  public final void glVertex2f(float x, float y) {
    vboSet.glVertex2f(x,y);
  }

  public final void glVertex3f(float x, float y, float z) {
    vboSet.glVertex3f(x,y,z);
  }

  public void glNormalv(Buffer v) {
    vboSet.glNormalv(v);
  }
  public final void glNormal3f(float x, float y, float z) {
    vboSet.glNormal3f(x,y,z);
  }

  public void glColorv(Buffer v) {
    vboSet.glColorv(v);
  }
  public final void glColor3f(float x, float y, float z) {
    vboSet.glColor3f(x,y,z);
  }

  public final void glColor4f(float x, float y, float z, float a) {
    vboSet.glColor4f(x,y,z, a);
  }

  public void glTexCoordv(Buffer v) {
    vboSet.glTexCoordv(v);
  }
  public final void glTexCoord2f(float x, float y) {
    vboSet.glTexCoord2f(x,y);
  }

  public final void glTexCoord3f(float x, float y, float z) {
    vboSet.glTexCoord3f(x,y,z);
  }

  public final void glVertex2s(short x, short y) {
    vboSet.glVertex2s(x,y);
  }

  public final void glVertex3s(short x, short y, short z) {
    vboSet.glVertex3s(x,y,z);
  }

  public final void glNormal3s(short x, short y, short z) {
    vboSet.glNormal3s(x,y,z);
  }

  public final void glColor3s(short x, short y, short z) {
    vboSet.glColor3s(x,y,z);
  }

  public final void glColor4s(short x, short y, short z, short a) {
    vboSet.glColor4s(x,y,z,a);
  }

  public final void glTexCoord2s(short x, short y) {
    vboSet.glTexCoord2s(x,y);
  }

  public final void glTexCoord3s(short x, short y, short z) {
    vboSet.glTexCoord3s(x,y,z);
  }

  public final void glVertex2b(byte x, byte y) {
    vboSet.glVertex2b(x,y);
  }

  public final void glVertex3b(byte x, byte y, byte z) {
    vboSet.glVertex3b(x,y,z);
  }

  public final void glNormal3b(byte x, byte y, byte z) {
    vboSet.glNormal3b(x,y,z);
  }

  public final void glColor3b(byte x, byte y, byte z) {
    vboSet.glColor3b(x,y,z);
  }

  public final void glColor4b(byte x, byte y, byte z, byte a) {
    vboSet.glColor4b(x,y,z,a);
  }

  public final void glTexCoord2b(byte x, byte y) {
    vboSet.glTexCoord2b(x,y);
  }

  public final void glTexCoord3b(byte x, byte y, byte z) {
    vboSet.glTexCoord3b(x,y,z);
  }

  protected ImmModeSink(int glBufferUsage, int initialSize,
                        int vComps, int vDataType,
                        int cComps, int cDataType, 
                        int nComps, int nDataType, 
                        int tComps, int tDataType, boolean useGLSL) {
    if(useGLSL && !GLProfile.isGL2ES2()) {
        throw new GLException("ImmModeSink GLSL usage not supported for profile: "+GLProfile.getProfile());
    }
    vboSet = new  VBOSet(glBufferUsage, initialSize, 
                         vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, useGLSL);
    this.vboSetList   = new ArrayList();
  }

  private void destroyList(GL gl) {
    for(Iterator i=vboSetList.iterator(); i.hasNext() ; ) {
        ((VBOSet)i.next()).destroy(gl);
    }
    vboSetList.clear();
  }

  private VBOSet vboSet;
  private ArrayList vboSetList;
  private static boolean vboUsage = true;

  protected static class VBOSet {
    protected VBOSet (int glBufferUsage, int initialSize,
                      int vComps, int vDataType,
                      int cComps, int cDataType, 
                      int nComps, int nDataType, 
                      int tComps, int tDataType, boolean useGLSL) {
        this.glBufferUsage=glBufferUsage;
        this.initialSize=initialSize;
        this.vDataType=vDataType;
        this.vComps=vComps;
        this.cDataType=cDataType;
        this.cComps=cComps;
        this.nDataType=nDataType;
        this.nComps=nComps;
        this.tDataType=tDataType;
        this.tComps=tComps;
        this.useGLSL=useGLSL;

        if(!useGLSL) {
          this.vertexVBO   = GLArrayDataServer.createFixed(GL.GL_VERTEX_ARRAY, null, vComps, vDataType, false, initialSize, glBufferUsage);
          this.colorVBO    = GLArrayDataServer.createFixed(GL.GL_COLOR_ARRAY,  null, cComps, cDataType, false, initialSize, glBufferUsage);
          this.normalVBO   = GLArrayDataServer.createFixed(GL.GL_NORMAL_ARRAY, null, nComps, nDataType, false, initialSize, glBufferUsage);
          this.texcoordVBO = GLArrayDataServer.createFixed(GL.GL_TEXTURE_COORD_ARRAY, null, tComps, tDataType, false, initialSize, glBufferUsage);
        } else {
          this.vertexVBO   = GLArrayDataServer.createGLSL(GLContext.mgl_Vertex, vComps, vDataType, false, initialSize, glBufferUsage);
          this.colorVBO    = GLArrayDataServer.createGLSL(GLContext.mgl_Color,  cComps, cDataType, false, initialSize, glBufferUsage);
          this.normalVBO   = GLArrayDataServer.createGLSL(GLContext.mgl_Normal, nComps, nDataType, false, initialSize, glBufferUsage);
          this.texcoordVBO = GLArrayDataServer.createGLSL(GLContext.mgl_MultiTexCoord, tComps, tDataType, false, initialSize, glBufferUsage);
        }
        if(!vboUsage) {
            this.vertexVBO.setVBOUsage(vboUsage);
            this.colorVBO.setVBOUsage(vboUsage);
            this.normalVBO.setVBOUsage(vboUsage);
            this.texcoordVBO.setVBOUsage(vboUsage);
        }

        this.sealed=false;
        this.mode = -1;
        this.modeOrig = -1;
    }

    protected final VBOSet regenerate() {
        return new VBOSet(glBufferUsage, initialSize, 
                          vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, useGLSL);
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
                       ",\n\t"+vertexVBO+
                       ",\n\t"+normalVBO+
                       ",\n\t"+colorVBO+
                       ",\n\t"+texcoordVBO+
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

        vertexVBO.seal(gl, true);
        normalVBO.seal(gl, true);
        colorVBO.seal(gl, true);
        texcoordVBO.seal(gl, true);

        if(disableBufferAfterSeal) {
            vertexVBO.enableBuffer(gl, false);
            normalVBO.enableBuffer(gl, false);
            colorVBO.enableBuffer(gl, false);
            texcoordVBO.enableBuffer(gl, false);
        }
    }

    protected void draw(GL gl, Buffer indices, boolean disableBufferAfterDraw, int i)
    {
        if(DEBUG_DRAW) {
            Exception e = new Exception("ImmModeSink.draw["+i+"](disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
            e.printStackTrace();
        }
        vertexVBO.enableBuffer(gl, true);
        normalVBO.enableBuffer(gl, true);
        colorVBO.enableBuffer(gl, true);
        texcoordVBO.enableBuffer(gl, true);

        if (vertexVBO.getBuffer()!=null) {
            if(null==indices) {
                gl.glDrawArrays(mode, 0, vertexVBO.getVerticeNumber());
            } else {
                Class clazz = indices.getClass();
                int type=-1;
                if(GLReflection.instanceOf(clazz, ByteBuffer.class.getName())) {
                    type =  GL.GL_UNSIGNED_BYTE;
                } else if(GLReflection.instanceOf(clazz, ShortBuffer.class.getName())) {
                    type =  GL.GL_UNSIGNED_SHORT;
                }
                if(0>type) {
                    throw new GLException("Given Buffer Class not supported: "+clazz+", should be ubyte or ushort:\n\t"+this);
                }
                gl.glDrawElements(mode, indices.remaining(), type, indices);
            }
        }

        if(disableBufferAfterDraw) {
            vertexVBO.enableBuffer(gl, false);
            normalVBO.enableBuffer(gl, false);
            colorVBO.enableBuffer(gl, false);
            texcoordVBO.enableBuffer(gl, false);
        }
    }

    protected void glVertexv(Buffer v) {
        checkSeal(false);
        vertexVBO.put(v);
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

    protected void glNormalv(Buffer v) {
        checkSeal(false);
        normalVBO.put(v);
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

    protected void glColorv(Buffer v) {
        checkSeal(false);
        colorVBO.put(v);
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
    protected void glColor4f(float x, float y, float z, float a) {
        checkSeal(false);
        colorVBO.putf(x);
        if(colorVBO.getComponents()>1) 
            colorVBO.putf(y);
        if(colorVBO.getComponents()>2) 
            colorVBO.putf(z);
        if(colorVBO.getComponents()>3) 
            colorVBO.putf(a);
        colorVBO.padding(4);
    }

    protected void glTexCoordv(Buffer v) {
        checkSeal(false);
        texcoordVBO.put(v);
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

    protected void glVertex2s(short x, short y) {
        checkSeal(false);
        vertexVBO.puts(x);
        if(vertexVBO.getComponents()>1) 
            vertexVBO.puts(y);
        vertexVBO.padding(2);
    }
    protected void glVertex3s(short x, short y, short z) {
        checkSeal(false);
        vertexVBO.puts(x);
        if(vertexVBO.getComponents()>1) 
            vertexVBO.puts(y);
        if(vertexVBO.getComponents()>2) 
            vertexVBO.puts(z);
        vertexVBO.padding(3);
    }

    protected void glNormal3s(short x, short y, short z) {
        checkSeal(false);
        normalVBO.puts(x);
        if(normalVBO.getComponents()>1) 
            normalVBO.puts(y);
        if(normalVBO.getComponents()>2) 
            normalVBO.puts(z);
        normalVBO.padding(3);
    }

    protected void glColor3s(short x, short y, short z) {
        checkSeal(false);
        colorVBO.puts(x);
        if(colorVBO.getComponents()>1) 
            colorVBO.puts(y);
        if(colorVBO.getComponents()>2) 
            colorVBO.puts(z);
        colorVBO.padding(3);
    }
    protected void glColor4s(short x, short y, short z, short a) {
        checkSeal(false);
        colorVBO.puts(x);
        if(colorVBO.getComponents()>1) 
            colorVBO.puts(y);
        if(colorVBO.getComponents()>2) 
            colorVBO.puts(z);
        if(colorVBO.getComponents()>3) 
            colorVBO.puts(a);
        colorVBO.padding(4);
    }

    protected void glTexCoord2s(short x, short y) {
        checkSeal(false);
        texcoordVBO.puts(x);
        if(texcoordVBO.getComponents()>1) 
            texcoordVBO.puts(y);
        texcoordVBO.padding(2);
    }
    protected void glTexCoord3s(short x, short y, short z) {
        checkSeal(false);
        texcoordVBO.puts(x);
        if(texcoordVBO.getComponents()>1) 
            texcoordVBO.puts(y);
        if(texcoordVBO.getComponents()>2) 
            texcoordVBO.puts(z);
        texcoordVBO.padding(3);
    }

    protected void glVertex2b(byte x, byte y) {
        checkSeal(false);
        vertexVBO.putb(x);
        if(vertexVBO.getComponents()>1) 
            vertexVBO.putb(y);
        vertexVBO.padding(2);
    }
    protected void glVertex3b(byte x, byte y, byte z) {
        checkSeal(false);
        vertexVBO.putb(x);
        if(vertexVBO.getComponents()>1) 
            vertexVBO.putb(y);
        if(vertexVBO.getComponents()>2) 
            vertexVBO.putb(z);
        vertexVBO.padding(3);
    }

    protected void glNormal3b(byte x, byte y, byte z) {
        checkSeal(false);
        normalVBO.putb(x);
        if(normalVBO.getComponents()>1) 
            normalVBO.putb(y);
        if(normalVBO.getComponents()>2) 
            normalVBO.putb(z);
        normalVBO.padding(3);
    }

    protected void glColor3b(byte x, byte y, byte z) {
        checkSeal(false);
        colorVBO.putb(x);
        if(colorVBO.getComponents()>1) 
            colorVBO.putb(y);
        if(colorVBO.getComponents()>2) 
            colorVBO.putb(z);
        colorVBO.padding(3);
    }
    protected void glColor4b(byte x, byte y, byte z, byte a) {
        checkSeal(false);
        colorVBO.putb(x);
        if(colorVBO.getComponents()>1) 
            colorVBO.putb(y);
        if(colorVBO.getComponents()>2) 
            colorVBO.putb(z);
        if(colorVBO.getComponents()>3) 
            colorVBO.putb(a);
        colorVBO.padding(4);
    }
    protected void glTexCoord2b(byte x, byte y) {
        checkSeal(false);
        texcoordVBO.putb(x);
        if(texcoordVBO.getComponents()>1) 
            texcoordVBO.putb(y);
        texcoordVBO.padding(2);
    }

    protected void glTexCoord3b(byte x, byte y, byte z) {
        checkSeal(false);
        texcoordVBO.putb(x);
        if(texcoordVBO.getComponents()>1) 
            texcoordVBO.putb(y);
        if(texcoordVBO.getComponents()>2) 
            texcoordVBO.putb(z);
        texcoordVBO.padding(3);
    }

    GLArrayDataServer vertexVBO;
    GLArrayDataServer normalVBO;
    GLArrayDataServer colorVBO;
    GLArrayDataServer texcoordVBO;
    int mode, modeOrig;
    int glBufferUsage, initialSize;
    int vComps,    cComps,    nComps,    tComps;
    int vDataType, cDataType, nDataType, tDataType;
    boolean sealed, useGLSL;
  }

}

