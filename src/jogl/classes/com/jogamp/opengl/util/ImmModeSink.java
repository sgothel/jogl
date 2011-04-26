
package com.jogamp.opengl.util;

import com.jogamp.common.util.*;
import com.jogamp.opengl.util.glsl.ShaderState;

import javax.media.opengl.*;
import javax.media.opengl.fixedfunc.*;
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
  public static ImmModeSink createFixed(GL gl, int glBufferUsage, int initialSize,
                                        int vComps, int vDataType,
                                        int cComps, int cDataType, 
                                        int nComps, int nDataType, 
                                        int tComps, int tDataType) {
    return new ImmModeSink(gl, glBufferUsage, initialSize, 
                           vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, false);
  }

  /**
   * Uses a GL2ES2 GLSL shader immediate mode sink.
   * To issue the draw() command,
   * a ShaderState must be current, using ShaderState.glUseProgram().
   *
   * @see #draw(GL, boolean)
   * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
   * @see com.jogamp.opengl.util.glsl.ShaderState#getCurrentShaderState()
   */
  public static ImmModeSink createGLSL(GL gl, int glBufferUsage, int initialSize,
                                       int vComps, int vDataType,
                                       int cComps, int cDataType, 
                                       int nComps, int nDataType, 
                                       int tComps, int tDataType) {
    return new ImmModeSink(gl, glBufferUsage, initialSize, 
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
        Exception e = new Exception("Info: ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
        e.printStackTrace();
    }
    int n=0;
    for(Iterator i=vboSetList.iterator(); i.hasNext() ; n++) {
        ((VBOSet)i.next()).draw(gl, null, disableBufferAfterDraw, n);
    }
  }

  public void draw(GL gl, Buffer indices, boolean disableBufferAfterDraw) {
    if(DEBUG_DRAW) {
        Exception e = new Exception("Info: ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
        e.printStackTrace();
    }
    int n=0;
    for(Iterator i=vboSetList.iterator(); i.hasNext() ; n++) {
        ((VBOSet)i.next()).draw(gl, indices, disableBufferAfterDraw, n);
    }
  }

  public void glBegin(int mode) {
    if(DEBUG_BEGIN_END) {
        Exception e = new Exception("Info: ImmModeSink.glBegin("+vboSet.mode+"):\n\t"+this);
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
        Exception e = new Exception("Info: ImmModeSink START glEnd(immediate: "+immediateDraw+"):\n\t"+this);
        e.printStackTrace();
    }
    if(immediateDraw) {
        vboSet.seal(gl, true);
        vboSet.draw(gl, indices, true, -1);
        reset(gl);
    } else {
        vboSet.seal(gl, true);
        vboSet.enableBuffer(gl, false);
        vboSetList.add(vboSet);
        vboSet = vboSet.regenerate();
    }
  }

  public void glVertexv(Buffer v) {
    vboSet.glVertexv(v);
  }
  public void glNormalv(Buffer v) {
    vboSet.glNormalv(v);
  }
  public void glColorv(Buffer v) {
    vboSet.glColorv(v);
  }
  public void glTexCoordv(Buffer v) {
    vboSet.glTexCoordv(v);
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

  public final void glColor4f(float x, float y, float z, float a) {
    vboSet.glColor4f(x,y,z, a);
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

  protected ImmModeSink(GL gl, int glBufferUsage, int initialSize,
                        int vComps, int vDataType,
                        int cComps, int cDataType, 
                        int nComps, int nDataType, 
                        int tComps, int tDataType, boolean useGLSL) {
    if(useGLSL && !gl.hasGLSL()) {
        throw new GLException("ImmModeSink GLSL usage not supported: "+gl);
    }
    vboSet = new  VBOSet(gl, glBufferUsage, initialSize, 
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
    protected VBOSet (GL gl, int glBufferUsage, int initialSize,
                      int vComps, int vDataType,
                      int cComps, int cDataType, 
                      int nComps, int nDataType, 
                      int tComps, int tDataType, boolean useGLSL) {
        this.gl=gl;
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

        allocateBuffer(initialSize);
        rewind();

        this.sealed=false;
        this.sealedGL=false;
        this.mode = -1;
        this.modeOrig = -1;
        this.bufferEnabled=false;
        this.bufferWritten=false;
    }

    protected final VBOSet regenerate() {
        return new VBOSet(gl, glBufferUsage, initialSize, 
                          vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, useGLSL);
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

    protected void draw(GL gl, Buffer indices, boolean disableBufferAfterDraw, int i)
    {
        if(DEBUG_DRAW) {
            Exception e = new Exception("Info: ImmModeSink.draw["+i+"](disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
            e.printStackTrace();
        }
        enableBuffer(gl, true);

        if (buffer!=null) {
            GL2ES1 glf = gl.getGL2ES1();

            if(null==indices) {
                glf.glDrawArrays(mode, 0, count);
            } else {
                Class clazz = indices.getClass();
                int type=-1;
                if(ReflectionUtil.instanceOf(clazz, ByteBuffer.class.getName())) {
                    type =  GL.GL_UNSIGNED_BYTE;
                } else if(ReflectionUtil.instanceOf(clazz, ShortBuffer.class.getName())) {
                    type =  GL.GL_UNSIGNED_SHORT;
                }
                if(0>type) {
                    throw new GLException("Given Buffer Class not supported: "+clazz+", should be ubyte or ushort:\n\t"+this);
                }
                glf.glDrawElements(mode, indices.remaining(), type, indices);
                // GL2: gl.glDrawRangeElements(mode, 0, indices.remaining()-1, indices.remaining(), type, indices);
            }
        }

        if(disableBufferAfterDraw) {
            enableBuffer(gl, false);
        }
    }

    public void glVertexv(Buffer v) {
        checkSeal(false);
        GLBuffers.put(vertexArray, v);
    }
    public void glNormalv(Buffer v) {
        checkSeal(false);
        GLBuffers.put(normalArray, v);
    }
    public void glColorv(Buffer v) {
        checkSeal(false);
        GLBuffers.put(colorArray, v);
    }
    public void glTexCoordv(Buffer v) {
        checkSeal(false);
        GLBuffers.put(textCoordArray, v);
    }

    public void glVertex2b(byte x, byte y) {
        checkSeal(false);
        growBufferIfNecessary(VERTEX, 2);
        if(vComps>0) 
            GLBuffers.putb(vertexArray, x);
        if(vComps>1) 
            GLBuffers.putb(vertexArray, y);
        padding(VERTEX, vComps-2);
    }
    public void glVertex3b(byte x, byte y, byte z) {
        checkSeal(false);
        growBufferIfNecessary(VERTEX, 3);
        if(vComps>0) 
            GLBuffers.putb(vertexArray, x);
        if(vComps>1) 
            GLBuffers.putb(vertexArray, y);
        if(vComps>2) 
            GLBuffers.putb(vertexArray, z);
        padding(VERTEX, vComps-3);
    }
    public void glVertex2s(short x, short y) {
        checkSeal(false);
        growBufferIfNecessary(VERTEX, 2);
        if(vComps>0) 
            GLBuffers.puts(vertexArray, x);
        if(vComps>1) 
            GLBuffers.puts(vertexArray, y);
        padding(VERTEX, vComps-2);
    }
    public void glVertex3s(short x, short y, short z) {
        checkSeal(false);
        growBufferIfNecessary(VERTEX, 3);
        if(vComps>0) 
            GLBuffers.puts(vertexArray, x);
        if(vComps>1) 
            GLBuffers.puts(vertexArray, y);
        if(vComps>2) 
            GLBuffers.puts(vertexArray, z);
        padding(VERTEX, vComps-3);
    }
    public void glVertex2f(float x, float y) {
        checkSeal(false);
        growBufferIfNecessary(VERTEX, 2);
        if(vComps>0) 
            GLBuffers.putf(vertexArray, x);
        if(vComps>1) 
            GLBuffers.putf(vertexArray, y);
        padding(VERTEX, vComps-2);
    }
    public void glVertex3f(float x, float y, float z) {
        checkSeal(false);
        growBufferIfNecessary(VERTEX, 3);
        if(vComps>0) 
            GLBuffers.putf(vertexArray, x);
        if(vComps>1) 
            GLBuffers.putf(vertexArray, y);
        if(vComps>2) 
            GLBuffers.putf(vertexArray, z);
        padding(VERTEX, vComps-3);
    }

    public void glNormal3b(byte x, byte y, byte z) {
        checkSeal(false);
        growBufferIfNecessary(NORMAL, 3);
        if(nComps>0) 
            GLBuffers.putb(normalArray, x);
        if(nComps>1) 
            GLBuffers.putb(normalArray, y);
        if(nComps>2) 
            GLBuffers.putb(normalArray, z);
        padding(NORMAL, nComps-3);
    }
    public void glNormal3s(short x, short y, short z) {
        checkSeal(false);
        growBufferIfNecessary(NORMAL, 3);
        if(nComps>0) 
            GLBuffers.puts(normalArray, x);
        if(nComps>1) 
            GLBuffers.puts(normalArray, y);
        if(nComps>2) 
            GLBuffers.puts(normalArray, z);
        padding(NORMAL, nComps-3);
    }
    public void glNormal3f(float x, float y, float z) {
        checkSeal(false);
        growBufferIfNecessary(NORMAL, 3);
        if(nComps>0) 
            GLBuffers.putf(normalArray, x);
        if(nComps>1) 
            GLBuffers.putf(normalArray, y);
        if(nComps>2) 
            GLBuffers.putf(normalArray, z);
        padding(NORMAL, nComps-3);
    }

    public void glColor3b(byte r, byte g, byte b) {
        checkSeal(false);
        growBufferIfNecessary(COLOR, 3);
        if(cComps>0) 
            GLBuffers.putb(colorArray, r);
        if(cComps>1) 
            GLBuffers.putb(colorArray, g);
        if(cComps>2) 
            GLBuffers.putb(colorArray, b);
        padding(COLOR, cComps-3);
    }
    public void glColor4b(byte r, byte g, byte b, byte a) {
        checkSeal(false);
        growBufferIfNecessary(COLOR, 4);
        if(cComps>0) 
            GLBuffers.putb(colorArray, r);
        if(cComps>1) 
            GLBuffers.putb(colorArray, g);
        if(cComps>2) 
            GLBuffers.putb(colorArray, b);
        if(cComps>3) 
            GLBuffers.putb(colorArray, a);
        padding(COLOR, cComps-4);
    }
    public void glColor3s(short r, short g, short b) {
        checkSeal(false);
        growBufferIfNecessary(COLOR, 3);
        if(cComps>0) 
            GLBuffers.puts(colorArray, r);
        if(cComps>1) 
            GLBuffers.puts(colorArray, g);
        if(cComps>2) 
            GLBuffers.puts(colorArray, b);
        padding(COLOR, cComps-3);
    }
    public void glColor4s(short r, short g, short b, short a) {
        checkSeal(false);
        growBufferIfNecessary(COLOR, 4);
        if(cComps>0) 
            GLBuffers.puts(colorArray, r);
        if(cComps>1) 
            GLBuffers.puts(colorArray, g);
        if(cComps>2) 
            GLBuffers.puts(colorArray, b);
        if(cComps>3) 
            GLBuffers.puts(colorArray, a);
        padding(COLOR, cComps-4);
    }
    public void glColor3f(float r, float g, float b) {
        checkSeal(false);
        growBufferIfNecessary(COLOR, 3);
        if(cComps>0) 
            GLBuffers.putf(colorArray, r);
        if(cComps>1) 
            GLBuffers.putf(colorArray, g);
        if(cComps>2) 
            GLBuffers.putf(colorArray, b);
        padding(COLOR, cComps-3);
    }
    public void glColor4f(float r, float g, float b, float a) {
        checkSeal(false);
        growBufferIfNecessary(COLOR, 4);
        if(cComps>0) 
            GLBuffers.putf(colorArray, r);
        if(cComps>1) 
            GLBuffers.putf(colorArray, g);
        if(cComps>2) 
            GLBuffers.putf(colorArray, b);
        if(cComps>3) 
            GLBuffers.putf(colorArray, a);
        padding(COLOR, cComps-4);
    }

    public void glTexCoord2b(byte x, byte y) {
        checkSeal(false);
        growBufferIfNecessary(TEXTCOORD, 2);
        if(tComps>0) 
            GLBuffers.putb(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.putb(textCoordArray, y);
        padding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3b(byte x, byte y, byte z) {
        checkSeal(false);
        growBufferIfNecessary(TEXTCOORD, 3);
        if(tComps>0) 
            GLBuffers.putb(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.putb(textCoordArray, y);
        if(tComps>2) 
            GLBuffers.putb(textCoordArray, z);
        padding(TEXTCOORD, tComps-3);
    }
    public void glTexCoord2s(short x, short y) {
        checkSeal(false);
        growBufferIfNecessary(TEXTCOORD, 2);
        if(tComps>0) 
            GLBuffers.puts(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.puts(textCoordArray, y);
        padding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3s(short x, short y, short z) {
        checkSeal(false);
        growBufferIfNecessary(TEXTCOORD, 3);
        if(tComps>0) 
            GLBuffers.puts(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.puts(textCoordArray, y);
        if(tComps>2) 
            GLBuffers.puts(textCoordArray, z);
        padding(TEXTCOORD, tComps-3);
    }
    public void glTexCoord2f(float x, float y) {
        checkSeal(false);
        growBufferIfNecessary(TEXTCOORD, 2);
        if(tComps>0) 
            GLBuffers.putf(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.putf(textCoordArray, y);
        padding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3f(float x, float y, float z) {
        checkSeal(false);
        growBufferIfNecessary(TEXTCOORD, 3);
        if(tComps>0) 
            GLBuffers.putf(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.putf(textCoordArray, y);
        if(tComps>2) 
            GLBuffers.putf(textCoordArray, z);
        padding(TEXTCOORD, tComps-3);
    }

    public void rewind() {
        if(null!=vertexArray) {
            vertexArray.rewind();
        }
        if(null!=colorArray) {
            colorArray.rewind();
        }
        if(null!=normalArray) {
            normalArray.rewind();
        }
        if(null!=textCoordArray) {
            textCoordArray.rewind();
        }
    }

    public void destroy(GL gl) {
        reset(gl);

        vertexArray=null; colorArray=null; normalArray=null; textCoordArray=null;
        vArrayData=null; cArrayData=null; nArrayData=null; tArrayData=null;
        buffer=null;
        bSize=0; count=0;
    }

    public void reset(GL gl) {
        enableBuffer(gl, false);
        reset();
    }

    public void reset() {
        if(buffer!=null) {
            buffer.clear();
        }
        rewind();

        this.mode = -1;
        this.modeOrig = -1;
        this.sealed=false;
        this.bufferEnabled=false;
        this.bufferWritten=false;
    }

    public void seal(GL glObj, boolean seal)
    {
        seal(seal);
        if(sealedGL==seal) return;
        sealedGL = seal;
        GL gl = glObj.getGL();
        if(seal) {
            if(vboUsage && vboName==0) {
                int[] tmp = new int[1];
                gl.glGenBuffers(1, tmp, 0);
                vboName = tmp[0];
            }
            if(null!=vArrayData)
                vArrayData.setVBOName(vboName);
            if(null!=cArrayData)
                cArrayData.setVBOName(vboName);
            if(null!=nArrayData)
                nArrayData.setVBOName(vboName);
            if(null!=tArrayData)
                tArrayData.setVBOName(vboName);
            enableBuffer(gl, true);
        } else {
            enableBuffer(gl, false);
        }
    }

    public void seal(boolean seal)
    {
        if(sealed==seal) return;
        sealed = seal;
        if(seal) {
            bufferWritten=false;
        }
    }

  public void enableBuffer(GL gl, boolean enable) {
    /* if(enableBufferAlways && enable) {
        bufferEnabled = false;
    } */
    if( bufferEnabled != enable && count>0 ) {
        if(enable) {
            checkSeal(true);
        }
        if(useGLSL) { 
            enableBufferGLSL(gl, enable);
        } else {
            enableBufferFixed(gl, enable);
        }
        bufferEnabled = enable;
    }
  }

  public void enableBufferFixed(GL gl, boolean enable) {
    GL2ES1 glf = gl.getGL2ES1();

    if(enable) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);

        if(!bufferWritten) {
            gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit(), buffer, GL.GL_STATIC_DRAW);
            bufferWritten=true;
        }

        if(vComps>0) {
           glf.glEnableClientState(glf.GL_VERTEX_ARRAY);
           glf.glVertexPointer(vArrayData);
        }
        if(cComps>0) {
           glf.glEnableClientState(glf.GL_COLOR_ARRAY);
           glf.glColorPointer(cArrayData);
        }
        if(nComps>0) {
           glf.glEnableClientState(glf.GL_NORMAL_ARRAY);
           glf.glNormalPointer(nArrayData);
        }
        if(tComps>0) {
           glf.glEnableClientState(glf.GL_TEXTURE_COORD_ARRAY);
           glf.glTexCoordPointer(tArrayData);
        }

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    } else {
        if(vComps>0) {
           glf.glDisableClientState(glf.GL_VERTEX_ARRAY);
        }
        if(cComps>0) {
           glf.glDisableClientState(glf.GL_COLOR_ARRAY);
        }
        if(nComps>0) {
           glf.glDisableClientState(glf.GL_NORMAL_ARRAY);
        }
        if(tComps>0) {
           glf.glDisableClientState(glf.GL_TEXTURE_COORD_ARRAY);
        }
    }
  }

  public void enableBufferGLSL(GL gl, boolean enable) {
    ShaderState st = ShaderState.getShaderState(gl);
    if(null==st) {
        throw new GLException("No ShaderState in "+gl);
    }      
    GL2ES2 glsl = gl.getGL2ES2();
 
    if(enable) {
        glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);

        if(!bufferWritten) {
            glsl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit(), buffer, GL.GL_STATIC_DRAW);
            bufferWritten=true;
        }

        if(vComps>0) {
           st.enableVertexAttribArray(glsl, vArrayData);
           st.vertexAttribPointer(glsl, vArrayData);
        }
        if(cComps>0) {
           st.enableVertexAttribArray(glsl, cArrayData);
           st.vertexAttribPointer(glsl, cArrayData);
        }
        if(nComps>0) {
           st.enableVertexAttribArray(glsl, nArrayData);
           st.vertexAttribPointer(glsl, nArrayData);
        }
        if(tComps>0) {
           st.enableVertexAttribArray(glsl, tArrayData);
           st.vertexAttribPointer(glsl, tArrayData);
        }

        glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    } else {
        if(vComps>0) {
           st.disableVertexAttribArray(glsl, vArrayData);
        }
        if(cComps>0) {
           st.disableVertexAttribArray(glsl, cArrayData);
        }
        if(nComps>0) {
           st.disableVertexAttribArray(glsl, nArrayData);
        }
        if(tComps>0) {
           st.disableVertexAttribArray(glsl, tArrayData);
        }
    }
  }

    public String toString() {
        return "VBOSet[mode "+mode+ 
                       ", modeOrig "+modeOrig+ 
                       ", sealed "+sealed+ 
                       ", bufferEnabled "+bufferEnabled+ 
                       ", bufferWritten "+bufferWritten+ 
                       ",\n\t"+vArrayData+
                       ",\n\t"+cArrayData+
                       ",\n\t"+nArrayData+
                       ",\n\t"+tArrayData+
                       "]";
    }

    // non public matters

    protected void allocateBuffer(int elements) {
        int vWidth = vComps * GLBuffers.sizeOfGLType(vDataType);
        int cWidth = cComps * GLBuffers.sizeOfGLType(cDataType);
        int nWidth = nComps * GLBuffers.sizeOfGLType(nDataType);
        int tWidth = tComps * GLBuffers.sizeOfGLType(tDataType);

        count  = elements;
        bSize  = count * ( vWidth + cWidth + nWidth + tWidth ) ;

        buffer = GLBuffers.newDirectByteBuffer(bSize);

        int pos = 0;
        int size= count * vWidth ;
        if(size>0) {
            vertexArray = GLBuffers.sliceGLBuffer(buffer, pos, size, vDataType);
        } else {
            vertexArray = null;
        }
        vOffset = pos;
        pos+=size;

        size= count * cWidth ;
        if(size>0) {
            colorArray = GLBuffers.sliceGLBuffer(buffer, pos, size, cDataType);
        } else {
            colorArray = null;
        }
        cOffset = pos;
        pos+=size;

        size= count * nWidth ;
        if(size>0) {
            normalArray = GLBuffers.sliceGLBuffer(buffer, pos, size, nDataType);
        } else {
            normalArray = null;
        }
        nOffset = pos;
        pos+=size;

        size= count * tWidth ;
        if(size>0) {
            textCoordArray = GLBuffers.sliceGLBuffer(buffer, pos, size, tDataType);
        } else {
            textCoordArray = null;
        }
        tOffset = pos;
        pos+=size;

        buffer.position(pos);
        buffer.flip();

        if(vComps>0) {
            vArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_VERTEX_ARRAY, vComps, vDataType, false, 0,
                                                        vertexArray, 0, vOffset, GL.GL_STATIC_DRAW);
        } else {
            vArrayData = null;
        }
        if(cComps>0) {
            cArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_COLOR_ARRAY, cComps, cDataType, false, 0,
                                                        colorArray, 0, cOffset, GL.GL_STATIC_DRAW);
        } else {
            cArrayData = null;
        }
        if(nComps>0) {
            nArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_NORMAL_ARRAY, nComps, nDataType, false, 0,
                                                        normalArray, 0, nOffset, GL.GL_STATIC_DRAW);
        } else {
            nArrayData = null;
        }
        if(tComps>0) {
            tArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_TEXTURE_COORD_ARRAY, tComps, tDataType, false, 0,
                                                        textCoordArray, 0, tOffset, GL.GL_STATIC_DRAW);
        } else {
            tArrayData = null;
        }

    }

    protected final boolean growBufferIfNecessary(int type, int spare) {
        if(buffer==null || count < spare) { 
            growBuffer(type, initialSize);
            return true;
        }
        return false;
    }

    protected final void growBuffer(int type, int additional) {
        if(sealed || 0==additional) return;

        // save olde values ..
        Buffer _vertexArray=vertexArray, _colorArray=colorArray, _normalArray=normalArray, _textCoordArray=textCoordArray;
        ByteBuffer _buffer = buffer;

        allocateBuffer(count+additional);

        if(null!=_vertexArray) {
            _vertexArray.flip();
            GLBuffers.put(vertexArray, _vertexArray);
        }
        if(null!=_colorArray) {
            _colorArray.flip();
            GLBuffers.put(colorArray, _colorArray);
        }
        if(null!=_normalArray) {
            _normalArray.flip();
            GLBuffers.put(normalArray, _normalArray);
        }
        if(null!=_textCoordArray) {
            _textCoordArray.flip();
            GLBuffers.put(textCoordArray, _textCoordArray);
        }
    }

    protected void padding(int type, int fill) {
        if ( sealed ) return;

        Buffer dest = null;

        switch (type) {
            case VERTEX:
                dest = vertexArray;
                break;
            case COLOR:
                dest = colorArray;
                break;
            case NORMAL:
                dest = normalArray;
                break;
            case TEXTCOORD:
                dest = textCoordArray;
                break;
        }

        if ( null==dest ) return;

        while((fill--)>0) {
            GLBuffers.putb(dest, (byte)0);
        }
    }

    protected int mode, modeOrig;
    protected int glBufferUsage, initialSize;

    protected ByteBuffer buffer;
    protected int bSize, count, vboName;

    public static final int VERTEX = 0;
    public static final int COLOR = 1;
    public static final int NORMAL = 2;
    public static final int TEXTCOORD = 3;

    protected int vOffset, cOffset, nOffset, tOffset;
    protected int vComps,    cComps,    nComps,    tComps;
    protected int vDataType, cDataType, nDataType, tDataType;
    protected Buffer vertexArray, colorArray, normalArray, textCoordArray;
    protected GLArrayDataWrapper vArrayData, cArrayData, nArrayData, tArrayData;

    protected boolean sealed, sealedGL, useGLSL;
    protected boolean bufferEnabled, bufferWritten;
    protected GL gl;
  }

}

