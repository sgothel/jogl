
package com.jogamp.opengl.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES1;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLException;
import javax.media.opengl.fixedfunc.GLPointerFunc;

import com.jogamp.opengl.util.glsl.ShaderState;

/**
 * <p>
 * Immediate mode sink, implementing OpenGL fixed function subset of immediate mode operations, i.e.
 * <pre>
 *   glBegin();
 *     glVertex3f(1f, 1f, 1f);
 *     glColor4f(1f, 1f, 1f, 1f);
 *     ...
 *   glEnd();
 * </pre>
 * Implementation buffers all vertex, colors, normal and texture-coord elements in their respective buffers
 * to be either rendered directly via {@link #glEnd(GL)} or to be added to an internal display list
 * via {@link #glEnd(GL, boolean) glEnd(gl, false)} for deferred rendering via {@link #draw(GL, boolean)}.
 * </p>
 * <a name="storageDetails"><h5>Buffer storage and it's creation via {@link #createFixed(int, int, int, int, int, int, int, int, int, int) createFixed(..)} 
 * and {@link #createGLSL(int, int, int, int, int, int, int, int, int, int) createGLSL(..)}</h5></a> 
 * <p>
 * If unsure whether <i>colors</i>, <i>normals</i> and <i>textures</i> will be used, 
 * simply add them with an expected component count.
 * This implementation will only render buffers which are being filled.<br/>
 * The buffer growing implementation will only grow the exceeded buffers, unused buffers are not resized.
 * </p>
 * <p>
 * Note: Optional types, i.e. color, must be either not used or used w/ the same element count as vertex, etc. 
 * This is a semantic constraint, same as in the original OpenGL spec.
 * </p>
 */
public class ImmModeSink {

  public static final boolean DEBUG_BEGIN_END = false;
  public static final boolean DEBUG_DRAW = false;
  public static final boolean DEBUG_BUFFER = false;

  public static final int GL_QUADS      = 0x0007; // Needs data manipulation on ES1/ES2
  public static final int GL_QUAD_STRIP = 0x0008;
  public static final int GL_POLYGON    = 0x0009;

  /**
   * Uses a GL2ES1, or ES2 fixed function emulation immediate mode sink
   * <p>
   * See <a href="#storageDetails"> buffer storage details</a>.
   * </p>
   * 
   * @param initialElementCount initial buffer size, if subsequent mutable operations are about to exceed the buffer size, the buffer will grow about the initial size.
   * @param vComps mandatory vertex component count, should be 2, 3 or 4.
   * @param vDataType mandatory vertex data type, e.g. {@link GL#GL_FLOAT}
   * @param cComps optional color component count, may be 0, 3 or 4
   * @param cDataType optional color data type, e.g. {@link GL#GL_FLOAT}
   * @param nComps optional normal component count, may be 0, 3 or 4
   * @param nDataType optional normal data type, e.g. {@link GL#GL_FLOAT}
   * @param tComps optional texture-coordinate  component count, may be 0, 2 or 3
   * @param tDataType optional texture-coordinate data type, e.g. {@link GL#GL_FLOAT}
   * @param glBufferUsage VBO <code>usage</code> parameter for {@link GL#glBufferData(int, long, Buffer, int)}, e.g. {@link GL#GL_STATIC_DRAW}, 
   *                      set to <code>0</code> for no VBO usage
   */
  public static ImmModeSink createFixed(int initialElementCount, 
                                        int vComps, int vDataType,
                                        int cComps, int cDataType,
                                        int nComps, int nDataType, 
                                        int tComps, int tDataType, 
                                        int glBufferUsage) {
    return new ImmModeSink(initialElementCount, vComps, 
                           vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, false, glBufferUsage);
  }

  /**
   * Uses a GL2ES2 GLSL shader immediate mode sink.
   * To issue the draw() command,
   * a ShaderState must be current, using ShaderState.glUseProgram().
   * <p>
   * See <a href="#storageDetails"> buffer storage details</a>.
   * </p>
   * 
   * @param initialElementCount initial buffer size, if subsequent mutable operations are about to exceed the buffer size, the buffer will grow about the initial size.
   * @param vComps mandatory vertex component count, should be 2, 3 or 4.
   * @param vDataType mandatory vertex data type, e.g. {@link GL#GL_FLOAT}
   * @param cComps optional color component count, may be 0, 3 or 4
   * @param cDataType optional color data type, e.g. {@link GL#GL_FLOAT}
   * @param nComps optional normal component count, may be 0, 3 or 4
   * @param nDataType optional normal data type, e.g. {@link GL#GL_FLOAT}
   * @param tComps optional texture-coordinate  component count, may be 0, 2 or 3
   * @param tDataType optional texture-coordinate data type, e.g. {@link GL#GL_FLOAT}
   * @param glBufferUsage VBO <code>usage</code> parameter for {@link GL#glBufferData(int, long, Buffer, int)}, e.g. {@link GL#GL_STATIC_DRAW}, 
   *                      set to <code>0</code> for no VBO usage
   * 
   * @see #draw(GL, boolean)
   * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
   * @see com.jogamp.opengl.util.glsl.ShaderState#getCurrentShaderState()
   */
  public static ImmModeSink createGLSL(int initialElementCount, 
                                       int vComps, int vDataType,
                                       int cComps, int cDataType,
                                       int nComps, int nDataType, 
                                       int tComps, int tDataType, 
                                       int glBufferUsage) {
    return new ImmModeSink(initialElementCount, vComps, 
                           vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, true, glBufferUsage);
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
    StringBuilder sb = new StringBuilder("ImmModeSink[");
    sb.append(",\n\tVBO list: "+vboSetList.size()+" [");
    for(Iterator<VBOSet> i=vboSetList.iterator(); i.hasNext() ; ) {
        sb.append("\n\t");
        sb.append( i.next() );
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
        System.err.println("ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
    }
    int n=0;
    for(int i=0; i<vboSetList.size(); i++, n++) {
        vboSetList.get(i).draw(gl, null, disableBufferAfterDraw, n);
    }
  }

  public void draw(GL gl, Buffer indices, boolean disableBufferAfterDraw) {
    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
    }
    int n=0;
    for(int i=0; i<vboSetList.size(); i++, n++) {
        vboSetList.get(i).draw(gl, indices, disableBufferAfterDraw, n);
    }
  }

  public void glBegin(int mode) {
    if(DEBUG_BEGIN_END) {
        System.err.println("ImmModeSink.glBegin("+vboSet.mode+"):\n\t"+this);
    }
    vboSet.modeOrig = mode;
    switch(mode) {
        case GL_QUAD_STRIP:
            mode=GL.GL_TRIANGLE_STRIP;
            break;
        case GL_POLYGON:
            mode=GL.GL_TRIANGLE_FAN;
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
        System.err.println("ImmModeSink START glEnd(immediate: "+immediateDraw+"):\n\t"+this);
    }
    if(immediateDraw) {
        vboSet.seal(gl, true);
        vboSet.draw(gl, indices, true, -1);
        reset(gl);
    } else {
        vboSet.seal(gl, true);
        vboSet.enableBuffer(gl, false);
        vboSetList.add(vboSet);
        vboSet = vboSet.regenerate(gl);
    }
    if(DEBUG_BEGIN_END) {
        System.err.println("ImmModeSink END glEnd(immediate: "+immediateDraw+"):\n\t"+this);
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

  protected ImmModeSink(int initialElementCount, int vComps,
                        int vDataType, int cComps,
                        int cDataType, int nComps, 
                        int nDataType, int tComps, 
                        int tDataType, boolean useGLSL, int glBufferUsage) {
    vboSet = new  VBOSet(initialElementCount, vComps, 
                         vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, useGLSL, glBufferUsage);
    this.vboSetList   = new ArrayList<VBOSet>();
  }
  
  public boolean getUseVBO() { return vboSet.getUseVBO(); }
  
  private void destroyList(GL gl) {
    for(int i=0; i<vboSetList.size(); i++) {
        vboSetList.get(i).destroy(gl);
    }
    vboSetList.clear();
  }

  private VBOSet vboSet;
  private final ArrayList<VBOSet> vboSetList;

  protected static class VBOSet {
    protected VBOSet (int initialElementCount, int vComps,
                      int vDataType, int cComps,
                      int cDataType, int nComps, 
                      int nDataType, int tComps, 
                      int tDataType, boolean useGLSL, int glBufferUsage) {
        this.glBufferUsage=glBufferUsage;
        this.initialElementCount=initialElementCount;
        this.vDataType=vDataType;
        this.vComps=vComps;
        this.cDataType=cDataType;
        this.cComps=cComps;
        this.nDataType=nDataType;
        this.nComps=nComps;
        this.tDataType=tDataType;
        this.tComps=tComps;
        this.useGLSL=useGLSL;
        this.useVBO = 0 != glBufferUsage;
        this.vboName = 0;
        
        this.vCount=0;
        this.cCount=0;
        this.nCount=0;
        this.tCount=0;
        this.vElems=0;
        this.cElems=0;
        this.nElems=0;
        this.tElems=0;
        
        reallocateBuffer(initialElementCount);
        rewind();

        this.sealed=false;
        this.sealedGL=false;
        this.mode = 0;
        this.modeOrig = 0;
        this.bufferEnabled=false;
        this.bufferWritten=false;
    }

    protected boolean getUseVBO() { return useVBO; }
    
    protected final VBOSet regenerate(GL gl) {
        return new VBOSet(initialElementCount, vComps, 
                          vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType, useGLSL, glBufferUsage);
    }

    protected void checkSeal(boolean test) throws GLException {
        if(0==mode) {
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
            System.err.println("ImmModeSink.draw["+i+"].0 (disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
        }
        enableBuffer(gl, true);

        if (buffer!=null) {
            if(null==indices) {
                if ( GL_QUADS == mode && !gl.isGL2() ) {
                    for (int j = 0; j < vElems - 3; j += 4) {
                        gl.glDrawArrays(GL.GL_TRIANGLE_FAN, j, 4);
                    }
                } else {
                    gl.glDrawArrays(mode, 0, vElems);
                }
            } else {
                int type=-1;
                if(indices instanceof ByteBuffer) {
                    type =  GL.GL_UNSIGNED_BYTE;
                } else if(indices instanceof ShortBuffer) {
                    type =  GL.GL_UNSIGNED_SHORT;
                } else {
                    throw new GLException("Given Buffer Class not supported: "+indices.getClass()+", should be ubyte or ushort:\n\t"+this);
                }
                
                if ( GL_QUADS == mode && !gl.isGL2() ) {
                    if( GL.GL_UNSIGNED_BYTE == type ) {
                        final ByteBuffer b = (ByteBuffer) indices;
                        for (int j = b.position(); j < b.remaining(); j++) {
                            gl.glDrawArrays(GL.GL_TRIANGLE_FAN, (int)(0x000000ff & b.get(j)), 4);
                        }                        
                    } else {
                        final ShortBuffer b = (ShortBuffer) indices;
                        for (int j = b.position(); j < b.remaining(); j++) {
                            gl.glDrawArrays(GL.GL_TRIANGLE_FAN, (int)(0x0000ffff & b.get(j)), 4);
                        }                                                
                    }
                } else {
                    gl.glDrawElements(mode, indices.remaining(), type, indices);
                    // GL2: gl.glDrawRangeElements(mode, 0, indices.remaining()-1, indices.remaining(), type, indices);
                }
            }
        }

        if(disableBufferAfterDraw) {
            enableBuffer(gl, false);
        }
        
        if(DEBUG_DRAW) {
            System.err.println("ImmModeSink.draw["+i+"].X (disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
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
        growBuffer(VERTEX, 2);
        if(vComps>0) 
            GLBuffers.putb(vertexArray, x);
        if(vComps>1) 
            GLBuffers.putb(vertexArray, y);
        padding(VERTEX, vComps-2);
    }
    public void glVertex3b(byte x, byte y, byte z) {
        checkSeal(false);
        growBuffer(VERTEX, 3);
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
        growBuffer(VERTEX, 2);
        if(vComps>0) 
            GLBuffers.puts(vertexArray, x);
        if(vComps>1) 
            GLBuffers.puts(vertexArray, y);
        padding(VERTEX, vComps-2);
    }
    public void glVertex3s(short x, short y, short z) {
        checkSeal(false);
        growBuffer(VERTEX, 3);
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
        growBuffer(VERTEX, 2);
        if(vComps>0) 
            GLBuffers.putf(vertexArray, x);
        if(vComps>1) 
            GLBuffers.putf(vertexArray, y);
        padding(VERTEX, vComps-2);
    }
    public void glVertex3f(float x, float y, float z) {
        checkSeal(false);
        growBuffer(VERTEX, 3);
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
        growBuffer(NORMAL, 3);
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
        growBuffer(NORMAL, 3);
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
        growBuffer(NORMAL, 3);
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
        growBuffer(COLOR, 3);
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
        growBuffer(COLOR, 4);
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
        growBuffer(COLOR, 3);
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
        growBuffer(COLOR, 4);
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
        growBuffer(COLOR, 3);
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
        growBuffer(COLOR, 4);
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
        growBuffer(TEXTCOORD, 2);
        if(tComps>0) 
            GLBuffers.putb(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.putb(textCoordArray, y);
        padding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3b(byte x, byte y, byte z) {
        checkSeal(false);
        growBuffer(TEXTCOORD, 3);
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
        growBuffer(TEXTCOORD, 2);
        if(tComps>0) 
            GLBuffers.puts(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.puts(textCoordArray, y);
        padding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3s(short x, short y, short z) {
        checkSeal(false);
        growBuffer(TEXTCOORD, 3);
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
        growBuffer(TEXTCOORD, 2);
        if(tComps>0) 
            GLBuffers.putf(textCoordArray, x);
        if(tComps>1) 
            GLBuffers.putf(textCoordArray, y);
        padding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3f(float x, float y, float z) {
        checkSeal(false);
        growBuffer(TEXTCOORD, 3);
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

        vCount=0; cCount=0; nCount=0; tCount=0;
        vertexArray=null; colorArray=null; normalArray=null; textCoordArray=null;
        vArrayData=null; cArrayData=null; nArrayData=null; tArrayData=null;
        buffer=null;
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

        this.mode = 0;
        this.modeOrig = 0;
        this.sealed=false;
        this.sealedGL=false;
        this.bufferEnabled=false;
        this.bufferWritten=false;
        this.vElems=0;
        this.cElems=0;
        this.nElems=0;
        this.tElems=0;        
    }

    public void seal(GL glObj, boolean seal)
    {
        seal(seal);
        if(sealedGL==seal) return;
        sealedGL = seal;
        GL gl = glObj.getGL();
        if(seal) {
            if(useVBO) {
                if(0 == vboName) {
                    int[] tmp = new int[1];
                    gl.glGenBuffers(1, tmp, 0);
                    vboName = tmp[0];
                }
                if(null!=vArrayData) {
                    vArrayData.setVBOName(vboName);
                }
                if(null!=cArrayData) {
                    cArrayData.setVBOName(vboName);
                }
                if(null!=nArrayData) {
                    nArrayData.setVBOName(vboName);
                }
                if(null!=tArrayData) {
                    tArrayData.setVBOName(vboName);
                }
            }
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
            rewind();
        }
    }

  public void enableBuffer(GL gl, boolean enable) {
    if( bufferEnabled != enable && vElems>0 ) {
        if(enable) {
            checkSeal(true);
        }
        bufferEnabled = enable;
        if(useGLSL) { 
            enableBufferGLSL(gl, enable);
        } else {
            enableBufferFixed(gl, enable);
        }
    }
  }

  public void enableBufferFixed(GL gl, boolean enable) {
    GL2ES1 glf = gl.getGL2ES1();
    
    final boolean useV = vComps>0 && vElems>0 ;
    final boolean useC = cComps>0 && cElems>0 ;
    final boolean useN = nComps>0 && nElems>0 ;
    final boolean useT = tComps>0 && tElems>0 ;
    
    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableFixed.0 "+enable+": use [ v "+useV+", c "+useC+", n "+useN+", t "+useT+"]");        
    }

    if(enable) {
        if(useVBO) {
            if(0 == vboName) {
                throw new InternalError("Using VBO but no vboName");
            }
            glf.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);
            
            if(!bufferWritten) {
                glf.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit(), buffer, glBufferUsage);
            }
        }
        bufferWritten=true;
    }

    if(useV) {
       if(enable) {
           glf.glEnableClientState(GLPointerFunc.GL_VERTEX_ARRAY);
           glf.glVertexPointer(vArrayData);
       } else {
           glf.glDisableClientState(GLPointerFunc.GL_VERTEX_ARRAY);               
       }
    }
    if(useC) {
       if(enable) {
           glf.glEnableClientState(GLPointerFunc.GL_COLOR_ARRAY);
           glf.glColorPointer(cArrayData);
       } else {
           glf.glDisableClientState(GLPointerFunc.GL_COLOR_ARRAY);
       }
    }
    if(useN) {
       if(enable) {
           glf.glEnableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
           glf.glNormalPointer(nArrayData);
       } else {
           glf.glDisableClientState(GLPointerFunc.GL_NORMAL_ARRAY);
       }
    }
    if(useT) {
       if(enable) {
           glf.glEnableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
           glf.glTexCoordPointer(tArrayData);
       } else {
           glf.glDisableClientState(GLPointerFunc.GL_TEXTURE_COORD_ARRAY);
       }
    }

    if(enable && useVBO) {
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }
    
    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableFixed.X "+this);        
    }
  }

  public void enableBufferGLSL(GL gl, boolean enable) {
    ShaderState st = ShaderState.getShaderState(gl);
    if(null==st) {
        throw new GLException("No ShaderState in "+gl);
    }      
    GL2ES2 glsl = gl.getGL2ES2();
 
    final boolean useV = vComps>0 && vElems>0 ;
    final boolean useC = cComps>0 && cElems>0 ;
    final boolean useN = nComps>0 && nElems>0 ;
    final boolean useT = tComps>0 && tElems>0 ;
    
    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableGLSL.0 "+enable+": use [ v "+useV+", c "+useC+", n "+useN+", t "+useT+"]");        
    }
    
    if(enable) {
        if(useVBO) {
            if(0 == vboName) {
                throw new InternalError("Using VBO but no vboName");
            }
            glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);
            if(!bufferWritten) {
                glsl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit(), buffer, GL.GL_STATIC_DRAW);
            }
        }
        bufferWritten=true;
    }

    if(useV) {
       if(enable) {
           st.enableVertexAttribArray(glsl, vArrayData);
           st.vertexAttribPointer(glsl, vArrayData);
       } else {
           st.disableVertexAttribArray(glsl, vArrayData);
       }
    }
    if(useC) {
       if(enable) {
           st.enableVertexAttribArray(glsl, cArrayData);
           st.vertexAttribPointer(glsl, cArrayData);
       } else {
           st.disableVertexAttribArray(glsl, cArrayData);
       }
    }
    if(useN) {
       if(enable) {
           st.enableVertexAttribArray(glsl, nArrayData);
           st.vertexAttribPointer(glsl, nArrayData);
       } else {
           st.disableVertexAttribArray(glsl, nArrayData);
       }
    }
    if(useT) {
       if(enable) {
           st.enableVertexAttribArray(glsl, tArrayData);
           st.vertexAttribPointer(glsl, tArrayData);
       } else {
           st.disableVertexAttribArray(glsl, tArrayData);
       }
    }
    
    if(enable && useVBO) {
        glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }
    
    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableGLSL.X "+this);        
    }
  }

    public String toString() {
        return "VBOSet[mode "+mode+ 
                       ", modeOrig "+modeOrig+ 
                       ", use/count "+getElemUseCountStr()+
                       ", sealed "+sealed+ 
                       ", sealedGL "+sealedGL+
                       ", bufferEnabled "+bufferEnabled+ 
                       ", bufferWritten "+bufferWritten+
                       ", useVBO "+useVBO+", vboName "+vboName+
                       ",\n\t"+vArrayData+
                       ",\n\t"+cArrayData+
                       ",\n\t"+nArrayData+
                       ",\n\t"+tArrayData+
                       "]";
    }

    // non public matters

    protected String getElemUseCountStr() {
        return "[v "+vElems+"/"+vCount+", c "+cElems+"/"+cCount+", n "+nElems+"/"+nCount+", t "+tElems+"/"+tCount+"]";
    }
    
    protected boolean fitElemsInBuffers(int addElems) {
        final int vAdd = addElems - ( vCount - vElems );
        final int cAdd = addElems - ( cCount - cElems );
        final int nAdd = addElems - ( nCount - nElems );
        final int tAdd = addElems - ( tCount - tElems );
        final boolean res = 0>=vAdd && 0>=cAdd && 0>=nAdd && 0>=tAdd;
        /* if(DEBUG_BUFFER) {
            System.err.println("ImmModeSink.fitElemsInBuffer: "+getElemUseCountStr()+" + "+addElems+" -> "+res);
        } */
        return res;
    }
    
    protected boolean reallocateBuffer(int addElems) {
        final int vAdd = addElems - ( vCount - vElems );
        final int cAdd = addElems - ( cCount - cElems );
        final int nAdd = addElems - ( nCount - nElems );
        final int tAdd = addElems - ( tCount - tElems );
        
        if( 0>=vAdd && 0>=cAdd && 0>=nAdd && 0>=tAdd) {
            if(DEBUG_BUFFER) {
                System.err.println("ImmModeSink.realloc: "+getElemUseCountStr()+" + "+addElems+" -> NOP");
            }
            return false;
        }
        
        if(DEBUG_BUFFER) {
            System.err.println("ImmModeSink.realloc: "+getElemUseCountStr()+" + "+addElems);
        }
        vCount += vAdd;
        cCount += cAdd;
        nCount += nAdd;
        tCount += tAdd;
        
        final int vWidth = vComps * GLBuffers.sizeOfGLType(vDataType);
        final int cWidth = cComps * GLBuffers.sizeOfGLType(cDataType);
        final int nWidth = nComps * GLBuffers.sizeOfGLType(nDataType);
        final int tWidth = tComps * GLBuffers.sizeOfGLType(tDataType);

        final int bSizeV  = vCount * vWidth;
        final int bSizeC  = cCount * cWidth;
        final int bSizeN  = nCount * nWidth;
        final int bSizeT  = tCount * tWidth;
        
        buffer = GLBuffers.newDirectByteBuffer( bSizeV + bSizeC + bSizeN + bSizeT );
        vOffset = 0;
        
        if(bSizeV>0) {
            vertexArray = GLBuffers.sliceGLBuffer(buffer, vOffset, bSizeV, vDataType);
        } else {
            vertexArray = null;
        }        
        cOffset=vOffset+bSizeV;

        if(bSizeC>0) {
            colorArray = GLBuffers.sliceGLBuffer(buffer, cOffset, bSizeC, cDataType);
        } else {
            colorArray = null;
        }
        nOffset=cOffset+bSizeC;

        if(bSizeN>0) {
            normalArray = GLBuffers.sliceGLBuffer(buffer, nOffset, bSizeN, nDataType);
        } else {
            normalArray = null;
        }
        tOffset=nOffset+bSizeN;

        if(bSizeT>0) {
            textCoordArray = GLBuffers.sliceGLBuffer(buffer, tOffset, bSizeT, tDataType);
        } else {
            textCoordArray = null;
        }

        buffer.position(tOffset+bSizeT);
        buffer.flip();

        if(vComps>0) {
            vArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_VERTEX_ARRAY, vComps, vDataType, false, 0,
                                                        vertexArray, 0, vOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        } else {
            vArrayData = null;
        }
        if(cComps>0) {
            cArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_COLOR_ARRAY, cComps, cDataType, false, 0,
                                                        colorArray, 0, cOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        } else {
            cArrayData = null;
        }
        if(nComps>0) {
            nArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_NORMAL_ARRAY, nComps, nDataType, false, 0,
                                                        normalArray, 0, nOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        } else {
            nArrayData = null;
        }
        if(tComps>0) {
            tArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_TEXTURE_COORD_ARRAY, tComps, tDataType, false, 0,
                                                        textCoordArray, 0, tOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        } else {
            tArrayData = null;
        }
        
        if(DEBUG_BUFFER) {
            System.err.println("ImmModeSink.realloc.X: "+this.toString());
            Thread.dumpStack();
        }
        return true;
    }

    protected final boolean growBuffer(int type, int additional) {
        if( null !=buffer && !sealed && 0<additional) {
            if( !fitElemsInBuffers(additional) ) {
                // save olde values ..
                final Buffer _vertexArray=vertexArray, _colorArray=colorArray, _normalArray=normalArray, _textCoordArray=textCoordArray;
        
                if ( reallocateBuffer(initialElementCount) ) {
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
                    return true;
                }
            }
        }
        return false;
    }

    protected void padding(int type, int fill) {
        if ( sealed ) return;

        Buffer dest = null;

        switch (type) {
            case VERTEX:
                dest = vertexArray;
                vElems++;
                break;
            case COLOR:
                dest = colorArray;
                cElems++;
                break;
            case NORMAL:
                dest = normalArray;
                nElems++;
                break;
            case TEXTCOORD:
                dest = textCoordArray;
                tElems++;
                break;
        }

        if ( null==dest ) return;

        while((fill--)>0) {
            GLBuffers.putb(dest, (byte)0);
        }
    }

    final protected int glBufferUsage, initialElementCount;
    final protected boolean useVBO;
    protected int mode, modeOrig;

    protected ByteBuffer buffer;
    protected int vboName;

    public static final int VERTEX = 0;
    public static final int COLOR = 1;
    public static final int NORMAL = 2;
    public static final int TEXTCOORD = 3;

    protected int vCount,    cCount,    nCount,    tCount;
    protected int vOffset,   cOffset,   nOffset,   tOffset;
    protected int vComps,    cComps,    nComps,    tComps;
    protected int vElems,    cElems,    nElems,    tElems;
    protected int vDataType, cDataType, nDataType, tDataType;
    protected Buffer vertexArray, colorArray, normalArray, textCoordArray;
    protected GLArrayDataWrapper vArrayData, cArrayData, nArrayData, tArrayData;

    protected boolean sealed, sealedGL, useGLSL;
    protected boolean bufferEnabled, bufferWritten;
  }

}

