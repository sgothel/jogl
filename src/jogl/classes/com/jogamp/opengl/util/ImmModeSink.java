
package com.jogamp.opengl.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Iterator;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES1;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.fixedfunc.GLPointerFunc;

import jogamp.opengl.Debug;

import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.PropertyAccess;
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
 * and {@link #createGLSL(int, int, int, int, int, int, int, int, int, int, ShaderState) createGLSL(..)}</h5></a>
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
  protected static final boolean DEBUG_BEGIN_END;
  protected static final boolean DEBUG_DRAW;
  protected static final boolean DEBUG_BUFFER;

  static {
      Debug.initSingleton();
      DEBUG_BEGIN_END = PropertyAccess.isPropertyDefined("jogl.debug.ImmModeSink.BeginEnd", true);
      DEBUG_DRAW = PropertyAccess.isPropertyDefined("jogl.debug.ImmModeSink.Draw", true);
      DEBUG_BUFFER = PropertyAccess.isPropertyDefined("jogl.debug.ImmModeSink.Buffer", true);
  }

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
  public static ImmModeSink createFixed(final int initialElementCount,
                                        final int vComps, final int vDataType,
                                        final int cComps, final int cDataType,
                                        final int nComps, final int nDataType,
                                        final int tComps, final int tDataType,
                                        final int glBufferUsage) {
    return new ImmModeSink(initialElementCount,
                           vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType,
                           false, glBufferUsage, null, 0);
  }

  /**
   * Uses a GL2ES2 GLSL shader immediate mode sink, utilizing the given ShaderState.
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
   * @param st ShaderState to locate the vertex attributes
   * @see #draw(GL, boolean)
   * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
   * @see com.jogamp.opengl.util.glsl.ShaderState#getCurrentShaderState()
   */
  public static ImmModeSink createGLSL(final int initialElementCount,
                                       final int vComps, final int vDataType,
                                       final int cComps, final int cDataType,
                                       final int nComps, final int nDataType,
                                       final int tComps, final int tDataType,
                                       final int glBufferUsage, final ShaderState st) {
    return new ImmModeSink(initialElementCount,
                           vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType,
                           true, glBufferUsage, st, 0);
  }

  /**
   * Uses a GL2ES2 GLSL shader immediate mode sink, utilizing the given shader-program.
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
   * @param shaderProgram shader-program name to locate the vertex attributes
   * @see #draw(GL, boolean)
   * @see com.jogamp.opengl.util.glsl.ShaderState#useProgram(GL2ES2, boolean)
   * @see com.jogamp.opengl.util.glsl.ShaderState#getCurrentShaderState()
   */
  public static ImmModeSink createGLSL(final int initialElementCount,
                                       final int vComps, final int vDataType,
                                       final int cComps, final int cDataType,
                                       final int nComps, final int nDataType,
                                       final int tComps, final int tDataType,
                                       final int glBufferUsage, final int shaderProgram) {
    return new ImmModeSink(initialElementCount,
                           vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType,
                           true, glBufferUsage, null, shaderProgram);
  }

  public void destroy(final GL gl) {
    destroyList(gl);

    vboSet.destroy(gl);
  }

  public void reset() {
    reset(null);
  }

  public void reset(final GL gl) {
    destroyList(gl);
    vboSet.reset(gl);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ImmModeSink[");
    sb.append(",\n\tVBO list: "+vboSetList.size()+" [");
    for(final Iterator<VBOSet> i=vboSetList.iterator(); i.hasNext() ; ) {
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

  public void draw(final GL gl, final boolean disableBufferAfterDraw) {
    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
    }
    int n=0;
    for(int i=0; i<vboSetList.size(); i++, n++) {
        vboSetList.get(i).draw(gl, null, disableBufferAfterDraw, n);
    }
  }

  public void draw(final GL gl, final Buffer indices, final boolean disableBufferAfterDraw) {
    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.draw(disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
    }
    int n=0;
    for(int i=0; i<vboSetList.size(); i++, n++) {
        vboSetList.get(i).draw(gl, indices, disableBufferAfterDraw, n);
    }
  }

  public void glBegin(int mode) {
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
    if(DEBUG_BEGIN_END) {
        System.err.println("ImmModeSink.glBegin("+vboSet.modeOrig+" -> "+vboSet.mode+")");
    }
    vboSet.checkSeal(false);
  }

  public final void glEnd(final GL gl) {
      glEnd(gl, null, true);
  }

  public void glEnd(final GL gl, final boolean immediateDraw) {
      glEnd(gl, null, immediateDraw);
  }

  public final void glEnd(final GL gl, final Buffer indices) {
      glEnd(gl, indices, true);
  }

  private void glEnd(final GL gl, final Buffer indices, final boolean immediateDraw) {
    if(DEBUG_BEGIN_END) {
        System.err.println("ImmModeSink START glEnd(immediate: "+immediateDraw+")");
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
        System.err.println("ImmModeSink END glEnd(immediate: "+immediateDraw+")");
    }
  }

  public void glVertexv(final Buffer v) {
    vboSet.glVertexv(v);
  }
  public void glNormalv(final Buffer v) {
    vboSet.glNormalv(v);
  }
  public void glColorv(final Buffer v) {
    vboSet.glColorv(v);
  }
  public void glTexCoordv(final Buffer v) {
    vboSet.glTexCoordv(v);
  }

  public final void glVertex2f(final float x, final float y) {
    vboSet.glVertex2f(x,y);
  }

  public final void glVertex3f(final float x, final float y, final float z) {
    vboSet.glVertex3f(x,y,z);
  }

  public final void glNormal3f(final float x, final float y, final float z) {
    vboSet.glNormal3f(x,y,z);
  }

  public final void glColor3f(final float x, final float y, final float z) {
    vboSet.glColor3f(x,y,z);
  }

  public final void glColor4f(final float x, final float y, final float z, final float a) {
    vboSet.glColor4f(x,y,z, a);
  }

  public final void glTexCoord2f(final float x, final float y) {
    vboSet.glTexCoord2f(x,y);
  }

  public final void glTexCoord3f(final float x, final float y, final float z) {
    vboSet.glTexCoord3f(x,y,z);
  }

  public final void glVertex2s(final short x, final short y) {
    vboSet.glVertex2s(x,y);
  }

  public final void glVertex3s(final short x, final short y, final short z) {
    vboSet.glVertex3s(x,y,z);
  }

  public final void glNormal3s(final short x, final short y, final short z) {
    vboSet.glNormal3s(x,y,z);
  }

  public final void glColor3s(final short x, final short y, final short z) {
    vboSet.glColor3s(x,y,z);
  }

  public final void glColor4s(final short x, final short y, final short z, final short a) {
    vboSet.glColor4s(x,y,z,a);
  }

  public final void glTexCoord2s(final short x, final short y) {
    vboSet.glTexCoord2s(x,y);
  }

  public final void glTexCoord3s(final short x, final short y, final short z) {
    vboSet.glTexCoord3s(x,y,z);
  }

  public final void glVertex2b(final byte x, final byte y) {
    vboSet.glVertex2b(x,y);
  }

  public final void glVertex3b(final byte x, final byte y, final byte z) {
    vboSet.glVertex3b(x,y,z);
  }

  public final void glNormal3b(final byte x, final byte y, final byte z) {
    vboSet.glNormal3b(x,y,z);
  }

  public final void glColor3b(final byte x, final byte y, final byte z) {
    vboSet.glColor3b(x,y,z);
  }

  public final void glColor3ub(final byte x, final byte y, final byte z) {
    vboSet.glColor3ub(x,y,z);
  }

  public final void glColor4b(final byte x, final byte y, final byte z, final byte a) {
    vboSet.glColor4b(x,y,z,a);
  }

  public final void glColor4ub(final byte x, final byte y, final byte z, final byte a) {
    vboSet.glColor4ub(x,y,z,a);
  }

  public final void glTexCoord2b(final byte x, final byte y) {
    vboSet.glTexCoord2b(x,y);
  }

  public final void glTexCoord3b(final byte x, final byte y, final byte z) {
    vboSet.glTexCoord3b(x,y,z);
  }

  protected ImmModeSink(final int initialElementCount,
                        final int vComps, final int vDataType,
                        final int cComps, final int cDataType,
                        final int nComps, final int nDataType,
                        final int tComps, final int tDataType,
                        final boolean useGLSL, final int glBufferUsage, final ShaderState st, final int shaderProgram) {
    vboSet = new VBOSet(initialElementCount,
                        vComps, vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType,
                        useGLSL, glBufferUsage, st, shaderProgram);
    this.vboSetList   = new ArrayList<VBOSet>();
  }

  public boolean getUseVBO() { return vboSet.getUseVBO(); }

  /**
   * Returns the additional element count if buffer resize is required.
   * @see #setResizeElementCount(int)
   */
  public int getResizeElementCount() { return vboSet.getResizeElementCount(); }

  /**
   * Sets the additional element count if buffer resize is required,
   * defaults to <code>initialElementCount</code> of factory method.
   * @see #createFixed(int, int, int, int, int, int, int, int, int, int)
   * @see #createGLSL(int, int, int, int, int, int, int, int, int, int, ShaderState)
   */
  public void setResizeElementCount(final int v) { vboSet.setResizeElementCount(v); }

  private void destroyList(final GL gl) {
    for(int i=0; i<vboSetList.size(); i++) {
        vboSetList.get(i).destroy(gl);
    }
    vboSetList.clear();
  }

  private VBOSet vboSet;
  private final ArrayList<VBOSet> vboSetList;

  protected static class VBOSet {
    protected VBOSet (final int initialElementCount,
                      final int vComps, final int vDataType,
                      final int cComps, final int cDataType,
                      final int nComps, final int nDataType,
                      final int tComps, final int tDataType,
                      final boolean useGLSL, final int glBufferUsage, final ShaderState st, final int shaderProgram) {
        // final ..
        this.glBufferUsage=glBufferUsage;
        this.initialElementCount=initialElementCount;
        this.useVBO = 0 != glBufferUsage;
        this.useGLSL=useGLSL;
        this.shaderState = st;
        this.shaderProgram = shaderProgram;

        if(useGLSL && null == shaderState && 0 == shaderProgram) {
            throw new IllegalArgumentException("Using GLSL but neither a valid shader-program nor ShaderState has been passed!");
        }
        // variable ..
        this.resizeElementCount=initialElementCount;
        this.vDataType=vDataType;
        this.vDataTypeSigned=GLBuffers.isSignedGLType(vDataType);
        this.vComps=vComps;
        this.vCompsBytes=vComps * GLBuffers.sizeOfGLType(vDataType);
        this.cDataType=cDataType;
        this.cDataTypeSigned=GLBuffers.isSignedGLType(cDataType);
        this.cComps=cComps;
        this.cCompsBytes=cComps * GLBuffers.sizeOfGLType(cDataType);
        this.nDataType=nDataType;
        this.nDataTypeSigned=GLBuffers.isSignedGLType(nDataType);
        this.nComps=nComps;
        this.nCompsBytes=nComps * GLBuffers.sizeOfGLType(nDataType);
        this.tDataType=tDataType;
        this.tDataTypeSigned=GLBuffers.isSignedGLType(tDataType);
        this.tComps=tComps;
        this.tCompsBytes=tComps * GLBuffers.sizeOfGLType(tDataType);
        this.vboName = 0;

        this.vCount=0;
        this.cCount=0;
        this.nCount=0;
        this.tCount=0;
        this.vElems=0;
        this.cElems=0;
        this.nElems=0;
        this.tElems=0;

        this.pageSize = Platform.getMachineDataInfo().pageSizeInBytes();

        reallocateBuffer(initialElementCount);
        rewind();

        this.sealed=false;
        this.sealedGL=false;
        this.mode = 0;
        this.modeOrig = 0;
        this.bufferEnabled=false;
        this.bufferWritten=false;
        this.bufferWrittenOnce=false;
        this.glslLocationSet = false;
    }

    protected int getResizeElementCount() { return resizeElementCount; }
    protected void setResizeElementCount(final int v) { resizeElementCount=v; }

    protected boolean getUseVBO() { return useVBO; }

    protected final VBOSet regenerate(final GL gl) {
        return new VBOSet(initialElementCount, vComps,
                          vDataType, cComps, cDataType, nComps, nDataType, tComps, tDataType,
                          useGLSL, glBufferUsage, shaderState, shaderProgram);
    }

    protected void checkSeal(final boolean test) throws GLException {
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

    private boolean usingShaderProgram = false;

    protected void useShaderProgram(final GL2ES2 gl, final boolean force) {
        if( force || !usingShaderProgram ) {
            if(null != shaderState) {
                shaderState.useProgram(gl, true);
            } else /* if( 0 != shaderProgram) */ {
                gl.glUseProgram(shaderProgram);
            }
            usingShaderProgram = true;
        }
    }

    protected void draw(final GL gl, final Buffer indices, final boolean disableBufferAfterDraw, final int i)
    {
        enableBuffer(gl, true);

        if(null != shaderState || 0 != shaderProgram) {
            useShaderProgram(gl.getGL2ES2(), false);
        }

        if(DEBUG_DRAW) {
            System.err.println("ImmModeSink.draw["+i+"].0 (disableBufferAfterDraw: "+disableBufferAfterDraw+"):\n\t"+this);
        }

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
                // FIXME: Impl. VBO usage .. or unroll.
                if( !gl.getContext().isCPUDataSourcingAvail() ) {
                    throw new GLException("CPU data sourcing n/a w/ "+gl.getContext());
                }
                final int type;
                if(indices instanceof ByteBuffer) {
                    type =  GL.GL_UNSIGNED_BYTE;
                } else if(indices instanceof ShortBuffer) {
                    type =  GL.GL_UNSIGNED_SHORT;
                } else if(indices instanceof IntBuffer) {
                    type =  GL.GL_UNSIGNED_INT;
                } else {
                    throw new GLException("Given Buffer Class not supported: "+indices.getClass()+", should be ubyte, ushort or uint:\n\t"+this);
                }
                final int idxLen = indices.remaining();
                final int idx0 = indices.position();

                if ( GL_QUADS == mode && !gl.isGL2() ) {
                    if( GL.GL_UNSIGNED_BYTE == type ) {
                        final ByteBuffer b = (ByteBuffer) indices;
                        for (int j = 0; j < idxLen; j++) {
                            gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0x000000ff & b.get(idx0+j), 4);
                        }
                    } else if( GL.GL_UNSIGNED_SHORT == type ){
                        final ShortBuffer b = (ShortBuffer) indices;
                        for (int j = 0; j < idxLen; j++) {
                            gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0x0000ffff & b.get(idx0+j), 4);
                        }
                    } else {
                        final IntBuffer b = (IntBuffer) indices;
                        for (int j = 0; j < idxLen; j++) {
                            gl.glDrawArrays(GL.GL_TRIANGLE_FAN, 0xffffffff & b.get(idx0+j), 4);
                        }
                    }
                } else {
                    ((GL2ES1)gl).glDrawElements(mode, idxLen, type, indices);
                    // GL2: gl.glDrawRangeElements(mode, 0, idxLen-1, idxLen, type, indices);
                }
            }
        }

        if(disableBufferAfterDraw) {
            enableBuffer(gl, false);
        }

        if(DEBUG_DRAW) {
            System.err.println("ImmModeSink.draw["+i+"].X (disableBufferAfterDraw: "+disableBufferAfterDraw+")");
        }
    }

    public void glVertexv(final Buffer v) {
        checkSeal(false);
        Buffers.put(vertexArray, v);
    }
    public void glNormalv(final Buffer v) {
        checkSeal(false);
        Buffers.put(normalArray, v);
    }
    public void glColorv(final Buffer v) {
        checkSeal(false);
        Buffers.put(colorArray, v);
    }
    public void glTexCoordv(final Buffer v) {
        checkSeal(false);
        Buffers.put(textCoordArray, v);
    }

    public void glVertex2b(final byte x, final byte y) {
        checkSeal(false);
        growBuffer(VERTEX);
        if(vComps>0)
            Buffers.putNb(vertexArray, vDataTypeSigned, x, true);
        if(vComps>1)
            Buffers.putNb(vertexArray, vDataTypeSigned, y, true);
        countAndPadding(VERTEX, vComps-2);
    }
    public void glVertex3b(final byte x, final byte y, final byte z) {
        checkSeal(false);
        growBuffer(VERTEX);
        if(vComps>0)
            Buffers.putNb(vertexArray, vDataTypeSigned, x, true);
        if(vComps>1)
            Buffers.putNb(vertexArray, vDataTypeSigned, y, true);
        if(vComps>2)
            Buffers.putNb(vertexArray, vDataTypeSigned, z, true);
        countAndPadding(VERTEX, vComps-3);
    }
    public void glVertex2s(final short x, final short y) {
        checkSeal(false);
        growBuffer(VERTEX);
        if(vComps>0)
            Buffers.putNs(vertexArray, vDataTypeSigned, x, true);
        if(vComps>1)
            Buffers.putNs(vertexArray, vDataTypeSigned, y, true);
        countAndPadding(VERTEX, vComps-2);
    }
    public void glVertex3s(final short x, final short y, final short z) {
        checkSeal(false);
        growBuffer(VERTEX);
        if(vComps>0)
            Buffers.putNs(vertexArray, vDataTypeSigned, x, true);
        if(vComps>1)
            Buffers.putNs(vertexArray, vDataTypeSigned, y, true);
        if(vComps>2)
            Buffers.putNs(vertexArray, vDataTypeSigned, z, true);
        countAndPadding(VERTEX, vComps-3);
    }
    public void glVertex2f(final float x, final float y) {
        checkSeal(false);
        growBuffer(VERTEX);
        if(vComps>0)
            Buffers.putNf(vertexArray, vDataTypeSigned, x);
        if(vComps>1)
            Buffers.putNf(vertexArray, vDataTypeSigned, y);
        countAndPadding(VERTEX, vComps-2);
    }
    public void glVertex3f(final float x, final float y, final float z) {
        checkSeal(false);
        growBuffer(VERTEX);
        if(vComps>0)
            Buffers.putNf(vertexArray, vDataTypeSigned, x);
        if(vComps>1)
            Buffers.putNf(vertexArray, vDataTypeSigned, y);
        if(vComps>2)
            Buffers.putNf(vertexArray, vDataTypeSigned, z);
        countAndPadding(VERTEX, vComps-3);
    }

    public void glNormal3b(final byte x, final byte y, final byte z) {
        checkSeal(false);
        growBuffer(NORMAL);
        if(nComps>0)
            Buffers.putNb(normalArray, nDataTypeSigned, x, true);
        if(nComps>1)
            Buffers.putNb(normalArray, nDataTypeSigned, y, true);
        if(nComps>2)
            Buffers.putNb(normalArray, nDataTypeSigned, z, true);
        countAndPadding(NORMAL, nComps-3);
    }
    public void glNormal3s(final short x, final short y, final short z) {
        checkSeal(false);
        growBuffer(NORMAL);
        if(nComps>0)
            Buffers.putNs(normalArray, nDataTypeSigned, x, true);
        if(nComps>1)
            Buffers.putNs(normalArray, nDataTypeSigned, y, true);
        if(nComps>2)
            Buffers.putNs(normalArray, nDataTypeSigned, z, true);
        countAndPadding(NORMAL, nComps-3);
    }
    public void glNormal3f(final float x, final float y, final float z) {
        checkSeal(false);
        growBuffer(NORMAL);
        if(nComps>0)
            Buffers.putNf(normalArray, nDataTypeSigned, x);
        if(nComps>1)
            Buffers.putNf(normalArray, nDataTypeSigned, y);
        if(nComps>2)
            Buffers.putNf(normalArray, nDataTypeSigned, z);
        countAndPadding(NORMAL, nComps-3);
    }

    public void glColor3b(final byte r, final byte g, final byte b) {
        checkSeal(false);
        growBuffer(COLOR);
        if(cComps>0)
            Buffers.putNb(colorArray, cDataTypeSigned, r, true);
        if(cComps>1)
            Buffers.putNb(colorArray, cDataTypeSigned, g, true);
        if(cComps>2)
            Buffers.putNb(colorArray, cDataTypeSigned, b, true);
        countAndPadding(COLOR, cComps-3);
    }
    public void glColor3ub(final byte r, final byte g, final byte b) {
        checkSeal(false);
        growBuffer(COLOR);
        if(cComps>0)
            Buffers.putNb(colorArray, cDataTypeSigned, r, false);
        if(cComps>1)
            Buffers.putNb(colorArray, cDataTypeSigned, g, false);
        if(cComps>2)
            Buffers.putNb(colorArray, cDataTypeSigned, b, false);
        countAndPadding(COLOR, cComps-3);
    }
    public void glColor4b(final byte r, final byte g, final byte b, final byte a) {
        checkSeal(false);
        growBuffer(COLOR);
        if(cComps>0)
            Buffers.putNb(colorArray, cDataTypeSigned, r, true);
        if(cComps>1)
            Buffers.putNb(colorArray, cDataTypeSigned, g, true);
        if(cComps>2)
            Buffers.putNb(colorArray, cDataTypeSigned, b, true);
        if(cComps>3)
            Buffers.putNb(colorArray, cDataTypeSigned, a, true);
        countAndPadding(COLOR, cComps-4);
    }
    public void glColor4ub(final byte r, final byte g, final byte b, final byte a) {
        checkSeal(false);
        growBuffer(COLOR);
        if(cComps>0)
            Buffers.putNb(colorArray, cDataTypeSigned, r, false);
        if(cComps>1)
            Buffers.putNb(colorArray, cDataTypeSigned, g, false);
        if(cComps>2)
            Buffers.putNb(colorArray, cDataTypeSigned, b, false);
        if(cComps>3)
            Buffers.putNb(colorArray, cDataTypeSigned, a, false);
        countAndPadding(COLOR, cComps-4);
    }
    public void glColor3s(final short r, final short g, final short b) {
        checkSeal(false);
        growBuffer(COLOR);
        if(cComps>0)
            Buffers.putNs(colorArray, cDataTypeSigned, r, true);
        if(cComps>1)
            Buffers.putNs(colorArray, cDataTypeSigned, g, true);
        if(cComps>2)
            Buffers.putNs(colorArray, cDataTypeSigned, b, true);
        countAndPadding(COLOR, cComps-3);
    }
    public void glColor4s(final short r, final short g, final short b, final short a) {
        checkSeal(false);
        growBuffer(COLOR);
        if(cComps>0)
            Buffers.putNs(colorArray, cDataTypeSigned, r, true);
        if(cComps>1)
            Buffers.putNs(colorArray, cDataTypeSigned, g, true);
        if(cComps>2)
            Buffers.putNs(colorArray, cDataTypeSigned, b, true);
        if(cComps>3)
            Buffers.putNs(colorArray, cDataTypeSigned, a, true);
        countAndPadding(COLOR, cComps-4);
    }
    public void glColor3f(final float r, final float g, final float b) {
        checkSeal(false);
        growBuffer(COLOR);
        if(cComps>0)
            Buffers.putNf(colorArray, cDataTypeSigned, r);
        if(cComps>1)
            Buffers.putNf(colorArray, cDataTypeSigned, g);
        if(cComps>2)
            Buffers.putNf(colorArray, cDataTypeSigned, b);
        countAndPadding(COLOR, cComps-3);
    }
    public void glColor4f(final float r, final float g, final float b, final float a) {
        checkSeal(false);
        growBuffer(COLOR);
        if(cComps>0)
            Buffers.putNf(colorArray, cDataTypeSigned, r);
        if(cComps>1)
            Buffers.putNf(colorArray, cDataTypeSigned, g);
        if(cComps>2)
            Buffers.putNf(colorArray, cDataTypeSigned, b);
        if(cComps>3)
            Buffers.putNf(colorArray, cDataTypeSigned, a);
        countAndPadding(COLOR, cComps-4);
    }

    public void glTexCoord2b(final byte x, final byte y) {
        checkSeal(false);
        growBuffer(TEXTCOORD);
        if(tComps>0)
            Buffers.putNb(textCoordArray, tDataTypeSigned, x, true);
        if(tComps>1)
            Buffers.putNb(textCoordArray, tDataTypeSigned, y, true);
        countAndPadding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3b(final byte x, final byte y, final byte z) {
        checkSeal(false);
        growBuffer(TEXTCOORD);
        if(tComps>0)
            Buffers.putNb(textCoordArray, tDataTypeSigned, x, true);
        if(tComps>1)
            Buffers.putNb(textCoordArray, tDataTypeSigned, y, true);
        if(tComps>2)
            Buffers.putNb(textCoordArray, tDataTypeSigned, z, true);
        countAndPadding(TEXTCOORD, tComps-3);
    }
    public void glTexCoord2s(final short x, final short y) {
        checkSeal(false);
        growBuffer(TEXTCOORD);
        if(tComps>0)
            Buffers.putNs(textCoordArray, tDataTypeSigned, x, true);
        if(tComps>1)
            Buffers.putNs(textCoordArray, tDataTypeSigned, y, true);
        countAndPadding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3s(final short x, final short y, final short z) {
        checkSeal(false);
        growBuffer(TEXTCOORD);
        if(tComps>0)
            Buffers.putNs(textCoordArray, tDataTypeSigned, x, true);
        if(tComps>1)
            Buffers.putNs(textCoordArray, tDataTypeSigned, y, true);
        if(tComps>2)
            Buffers.putNs(textCoordArray, tDataTypeSigned, z, true);
        countAndPadding(TEXTCOORD, tComps-3);
    }
    public void glTexCoord2f(final float x, final float y) {
        checkSeal(false);
        growBuffer(TEXTCOORD);
        if(tComps>0)
            Buffers.putNf(textCoordArray, tDataTypeSigned, x);
        if(tComps>1)
            Buffers.putNf(textCoordArray, tDataTypeSigned, y);
        countAndPadding(TEXTCOORD, tComps-2);
    }
    public void glTexCoord3f(final float x, final float y, final float z) {
        checkSeal(false);
        growBuffer(TEXTCOORD);
        if(tComps>0)
            Buffers.putNf(textCoordArray, tDataTypeSigned, x);
        if(tComps>1)
            Buffers.putNf(textCoordArray, tDataTypeSigned, y);
        if(tComps>2)
            Buffers.putNf(textCoordArray, tDataTypeSigned, z);
        countAndPadding(TEXTCOORD, tComps-3);
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

    public void setShaderProgram(final int program) {
        if(null == shaderState && 0 == program) {
            throw new IllegalArgumentException("Not allowed to zero shader program if no ShaderState is set");
        }
        shaderProgram = program;
        glslLocationSet = false; // enforce location reset!
    }

    /**
     * @param gl
     * @return true if all locations for all used arrays are found (min 1 array), otherwise false.
     *         Also sets 'glslLocationSet' to the return value!
     */
    private boolean resetGLSLArrayLocation(final GL2ES2 gl) {
        int iA = 0;
        int iL = 0;

        if(null != vArrayData) {
            iA++;
            if( vArrayData.setLocation(gl, shaderProgram) >= 0 ) {
                iL++;
            }
        }
        if(null != cArrayData) {
            iA++;
            if( cArrayData.setLocation(gl, shaderProgram) >= 0 ) {
                iL++;
            }
        }
        if(null != nArrayData) {
            iA++;
            if( nArrayData.setLocation(gl, shaderProgram) >= 0 ) {
                iL++;
            }
        }
        if(null != tArrayData) {
            iA++;
            if( tArrayData.setLocation(gl, shaderProgram) >= 0 ) {
                iL++;
            }
        }
        glslLocationSet = iA == iL;
        return glslLocationSet;
    }

    public void destroy(final GL gl) {
        reset(gl);

        vCount=0; cCount=0; nCount=0; tCount=0;
        vertexArray=null; colorArray=null; normalArray=null; textCoordArray=null;
        vArrayData=null; cArrayData=null; nArrayData=null; tArrayData=null;
        buffer=null;
    }

    public void reset(final GL gl) {
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

    public void seal(final GL glObj, final boolean seal)
    {
        seal(seal);
        if(sealedGL==seal) return;
        sealedGL = seal;
        final GL gl = glObj.getGL();
        if(seal) {
            if(useVBO) {
                if(0 == vboName) {
                    final int[] tmp = new int[1];
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

    public void seal(final boolean seal)
    {
        if(sealed==seal) return;
        sealed = seal;
        if(seal) {
            bufferWritten=false;
            rewind();
        }
    }

  public void enableBuffer(final GL gl, final boolean enable) {
    if( bufferEnabled != enable && vElems>0 ) {
        if(enable) {
            checkSeal(true);
        }
        bufferEnabled = enable;
        if(useGLSL) {
            useShaderProgram(gl.getGL2ES2(), true);
            if(null != shaderState) {
                enableBufferGLSLShaderState(gl, enable);
            } else {
                enableBufferGLSLSimple(gl, enable);
            }
        } else {
            enableBufferFixed(gl, enable);
        }
    }
  }

  private final void writeBuffer(final GL gl) {
    final int vBytes  = vElems * vCompsBytes;
    final int cBytes  = cElems * cCompsBytes;
    final int nBytes  = nElems * nCompsBytes;
    final int tBytes  = tElems * tCompsBytes;
    final int delta = buffer.limit() - (vBytes+cBytes+nBytes+tBytes);
    if( bufferWrittenOnce && delta > pageSize ) {
        if(0 < vBytes) {
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, vOffset, vBytes, vertexArray);
        }
        if(0 < cBytes) {
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, cOffset, cBytes, colorArray);
        }
        if(0 < nBytes) {
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, nOffset, nBytes, normalArray);
        }
        if(0 < tBytes) {
            gl.glBufferSubData(GL.GL_ARRAY_BUFFER, tOffset, tBytes, textCoordArray);
        }
    } else {
        gl.glBufferData(GL.GL_ARRAY_BUFFER, buffer.limit(), buffer, glBufferUsage);
        bufferWrittenOnce = true;
    }
  }

  private void enableBufferFixed(final GL gl, final boolean enable) {
    final GL2ES1 glf = gl.getGL2ES1();

    final boolean useV = vComps>0 && vElems>0 ;
    final boolean useC = cComps>0 && cElems>0 ;
    final boolean useN = nComps>0 && nElems>0 ;
    final boolean useT = tComps>0 && tElems>0 ;

    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableFixed.0 "+enable+": use [ v "+useV+", c "+useC+", n "+useN+", t "+useT+"], "+getElemUseCountStr()+", "+buffer);
    }

    if(enable) {
        if(useVBO) {
            if(0 == vboName) {
                throw new InternalError("Using VBO but no vboName");
            }
            glf.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);

            if(!bufferWritten) {
                writeBuffer(gl);
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
        System.err.println("ImmModeSink.enableFixed.X ");
    }
  }

  private void enableBufferGLSLShaderState(final GL gl, final boolean enable) {
    final GL2ES2 glsl = gl.getGL2ES2();

    final boolean useV = vComps>0 && vElems>0 ;
    final boolean useC = cComps>0 && cElems>0 ;
    final boolean useN = nComps>0 && nElems>0 ;
    final boolean useT = tComps>0 && tElems>0 ;

    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableGLSL.A.0 "+enable+": use [ v "+useV+", c "+useC+", n "+useN+", t "+useT+"], "+getElemUseCountStr()+", "+buffer);
    }

    if(enable) {
        if(useVBO) {
            if(0 == vboName) {
                throw new InternalError("Using VBO but no vboName");
            }
            glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);
            if(!bufferWritten) {
                writeBuffer(gl);
            }
        }
        bufferWritten=true;
    }

    if(useV) {
       if(enable) {
           shaderState.enableVertexAttribArray(glsl, vArrayData);
           shaderState.vertexAttribPointer(glsl, vArrayData);
       } else {
           shaderState.disableVertexAttribArray(glsl, vArrayData);
       }
    }
    if(useC) {
       if(enable) {
           shaderState.enableVertexAttribArray(glsl, cArrayData);
           shaderState.vertexAttribPointer(glsl, cArrayData);
       } else {
           shaderState.disableVertexAttribArray(glsl, cArrayData);
       }
    }
    if(useN) {
       if(enable) {
           shaderState.enableVertexAttribArray(glsl, nArrayData);
           shaderState.vertexAttribPointer(glsl, nArrayData);
       } else {
           shaderState.disableVertexAttribArray(glsl, nArrayData);
       }
    }
    if(useT) {
       if(enable) {
           shaderState.enableVertexAttribArray(glsl, tArrayData);
           shaderState.vertexAttribPointer(glsl, tArrayData);
       } else {
           shaderState.disableVertexAttribArray(glsl, tArrayData);
       }
    }
    glslLocationSet = true; // ShaderState does set the location implicit

    if(enable && useVBO) {
        glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableGLSL.A.X ");
    }
  }

  private void enableBufferGLSLSimple(final GL gl, final boolean enable) {
    final GL2ES2 glsl = gl.getGL2ES2();

    final boolean useV = vComps>0 && vElems>0 ;
    final boolean useC = cComps>0 && cElems>0 ;
    final boolean useN = nComps>0 && nElems>0 ;
    final boolean useT = tComps>0 && tElems>0 ;

    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableGLSL.B.0 "+enable+": use [ v "+useV+", c "+useC+", n "+useN+", t "+useT+"], "+getElemUseCountStr()+", "+buffer);
    }

    if(!glslLocationSet) {
        if( !resetGLSLArrayLocation(glsl) ) {
            if(DEBUG_DRAW) {
                final int vLoc = null != vArrayData ? vArrayData.getLocation() : -1;
                final int cLoc = null != cArrayData ? cArrayData.getLocation() : -1;
                final int nLoc = null != nArrayData ? nArrayData.getLocation() : -1;
                final int tLoc = null != tArrayData ? tArrayData.getLocation() : -1;
                System.err.println("ImmModeSink.enableGLSL.B.X attribute locations in shader program "+shaderProgram+", incomplete ["+vLoc+", "+cLoc+", "+nLoc+", "+tLoc+"] - glslLocationSet "+glslLocationSet);
            }
            return;
        }
    }

    if(enable) {
        if(useVBO) {
            if(0 == vboName) {
                throw new InternalError("Using VBO but no vboName");
            }
            glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboName);
            if(!bufferWritten) {
                writeBuffer(gl);
            }
        }
        bufferWritten=true;
    }

    if(useV) {
       if(enable) {
           glsl.glEnableVertexAttribArray(vArrayData.getLocation());
           glsl.glVertexAttribPointer(vArrayData);
       } else {
           glsl.glDisableVertexAttribArray(vArrayData.getLocation());
       }
    }
    if(useC) {
       if(enable) {
           glsl.glEnableVertexAttribArray(cArrayData.getLocation());
           glsl.glVertexAttribPointer(cArrayData);
       } else {
           glsl.glDisableVertexAttribArray(cArrayData.getLocation());
       }
    }
    if(useN) {
       if(enable) {
           glsl.glEnableVertexAttribArray(nArrayData.getLocation());
           glsl.glVertexAttribPointer(nArrayData);
       } else {
           glsl.glDisableVertexAttribArray(nArrayData.getLocation());
       }
    }
    if(useT) {
       if(enable) {
           glsl.glEnableVertexAttribArray(tArrayData.getLocation());
           glsl.glVertexAttribPointer(tArrayData);
       } else {
           glsl.glDisableVertexAttribArray(tArrayData.getLocation());
       }
    }

    if(enable && useVBO) {
        glsl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    if(DEBUG_DRAW) {
        System.err.println("ImmModeSink.enableGLSL.B.X ");
    }
  }

    @Override
    public String toString() {
        final String glslS = useGLSL ?
                       ", useShaderState "+(null!=shaderState)+
                       ", shaderProgram "+shaderProgram+
                       ", glslLocationSet "+glslLocationSet : "";

        return "VBOSet[mode "+mode+
                       ", modeOrig "+modeOrig+
                       ", use/count "+getElemUseCountStr()+
                       ", sealed "+sealed+
                       ", sealedGL "+sealedGL+
                       ", bufferEnabled "+bufferEnabled+
                       ", bufferWritten "+bufferWritten+" (once "+bufferWrittenOnce+")"+
                       ", useVBO "+useVBO+", vboName "+vboName+
                       ", useGLSL "+useGLSL+
                       glslS+
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

    protected boolean fitElementInBuffer(final int type) {
        final int addElems = 1;
        switch (type) {
            case VERTEX:
                return ( vCount - vElems ) >= addElems ;
            case COLOR:
                return ( cCount - cElems ) >= addElems ;
            case NORMAL:
                return ( nCount - nElems ) >= addElems ;
            case TEXTCOORD:
                return ( tCount - tElems ) >= addElems ;
            default:
                throw new InternalError("XXX");
        }
    }

    protected boolean reallocateBuffer(final int addElems) {
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

        final int vBytes  = vCount * vCompsBytes;
        final int cBytes  = cCount * cCompsBytes;
        final int nBytes  = nCount * nCompsBytes;
        final int tBytes  = tCount * tCompsBytes;

        buffer = Buffers.newDirectByteBuffer( vBytes + cBytes + nBytes + tBytes );
        vOffset = 0;

        if(vBytes>0) {
            vertexArray = GLBuffers.sliceGLBuffer(buffer, vOffset, vBytes, vDataType);
        } else {
            vertexArray = null;
        }
        cOffset=vOffset+vBytes;

        if(cBytes>0) {
            colorArray = GLBuffers.sliceGLBuffer(buffer, cOffset, cBytes, cDataType);
        } else {
            colorArray = null;
        }
        nOffset=cOffset+cBytes;

        if(nBytes>0) {
            normalArray = GLBuffers.sliceGLBuffer(buffer, nOffset, nBytes, nDataType);
        } else {
            normalArray = null;
        }
        tOffset=nOffset+nBytes;

        if(tBytes>0) {
            textCoordArray = GLBuffers.sliceGLBuffer(buffer, tOffset, tBytes, tDataType);
        } else {
            textCoordArray = null;
        }

        buffer.position(tOffset+tBytes);
        buffer.flip();

        if(vComps>0) {
            vArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_VERTEX_ARRAY, vComps,
                                                        vDataType, GLBuffers.isGLTypeFixedPoint(vDataType), 0,
                                                        vertexArray, 0, vOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        } else {
            vArrayData = null;
        }
        if(cComps>0) {
            cArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_COLOR_ARRAY, cComps,
                                                        cDataType, GLBuffers.isGLTypeFixedPoint(cDataType), 0,
                                                        colorArray, 0, cOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        } else {
            cArrayData = null;
        }
        if(nComps>0) {
            nArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_NORMAL_ARRAY, nComps,
                                                        nDataType, GLBuffers.isGLTypeFixedPoint(nDataType), 0,
                                                        normalArray, 0, nOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        } else {
            nArrayData = null;
        }
        if(tComps>0) {
            tArrayData = GLArrayDataWrapper.createFixed(GLPointerFunc.GL_TEXTURE_COORD_ARRAY, tComps,
                                                        tDataType, GLBuffers.isGLTypeFixedPoint(tDataType), 0,
                                                        textCoordArray, 0, tOffset, GL.GL_STATIC_DRAW, GL.GL_ARRAY_BUFFER);
        } else {
            tArrayData = null;
        }

        bufferWrittenOnce = false; // new buffer data storage size!

        if(DEBUG_BUFFER) {
            System.err.println("ImmModeSink.realloc.X: "+this.toString());
            ExceptionUtils.dumpStack(System.err);
        }
        return true;
    }

    /** grow buffer by initialElementCount if there is no space for one more element in the designated buffer */
    protected final boolean growBuffer(final int type) {
        if( null !=buffer && !sealed ) {
            if( !fitElementInBuffer(type) ) {
                // save olde values ..
                final Buffer _vertexArray=vertexArray, _colorArray=colorArray, _normalArray=normalArray, _textCoordArray=textCoordArray;

                if ( reallocateBuffer(resizeElementCount) ) {
                    if(null!=_vertexArray) {
                        _vertexArray.flip();
                        Buffers.put(vertexArray, _vertexArray);
                    }
                    if(null!=_colorArray) {
                        _colorArray.flip();
                        Buffers.put(colorArray, _colorArray);
                    }
                    if(null!=_normalArray) {
                        _normalArray.flip();
                        Buffers.put(normalArray, _normalArray);
                    }
                    if(null!=_textCoordArray) {
                        _textCoordArray.flip();
                        Buffers.put(textCoordArray, _textCoordArray);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Fourth element default value for color (alpha), vertex (w) is '1',
     * as specified w/ VertexAttributes (ES2/GL3).
     * <p>
     * vec4 v = vec4(0, 0, 0, 1);
     * vec4 c = vec4(0, 0, 0, 1);
     * </p>
     *
     * @param type
     * @param fill
     */
    private void countAndPadding(final int type, int fill) {
        if ( sealed ) return;

        final Buffer dest;
        final boolean dSigned;
        final int e; // either 0 or 1

        switch (type) {
            case VERTEX:
                dest = vertexArray;
                dSigned = vDataTypeSigned;
                e = 4 == vComps ? 1 : 0;
                vElems++;
                break;
            case COLOR:
                dest = colorArray;
                dSigned = cDataTypeSigned;
                e = 4 == cComps ? 1 : 0;
                cElems++;
                break;
            case NORMAL:
                dest = normalArray;
                dSigned = nDataTypeSigned;
                e = 0;
                nElems++;
                break;
            case TEXTCOORD:
                dest = textCoordArray;
                dSigned = tDataTypeSigned;
                e = 0;
                tElems++;
                break;
            default: throw new InternalError("Invalid type "+type);
        }

        if ( null==dest ) return;

        while( fill > e ) {
            fill--;
            Buffers.putNf(dest, dSigned, 0f);
        }
        if( fill > 0 ) { // e == 1, add missing '1f end component'
            Buffers.putNf(dest, dSigned, 1f);
        }
    }

    final private int glBufferUsage, initialElementCount;
    final private boolean useVBO, useGLSL;
    final private ShaderState shaderState;
    private int shaderProgram;
    private int mode, modeOrig, resizeElementCount;

    private ByteBuffer buffer;
    private int vboName;

    private static final int VERTEX = 0;
    private static final int COLOR = 1;
    private static final int NORMAL = 2;
    private static final int TEXTCOORD = 3;

    private int vCount,    cCount,    nCount,    tCount;       // number of elements fit in each buffer
    private int vOffset,   cOffset,   nOffset,   tOffset;      // offset of specific array in common buffer
    private int vElems,    cElems,    nElems,    tElems;       // number of used elements in each buffer
    private final int vComps,    cComps,    nComps,    tComps; // number of components for each elements [2, 3, 4]
    private final int vCompsBytes, cCompsBytes, nCompsBytes, tCompsBytes; // byte size of all components
    private final int vDataType, cDataType, nDataType, tDataType;
    private final boolean vDataTypeSigned, cDataTypeSigned, nDataTypeSigned, tDataTypeSigned;
    private final int pageSize;
    private Buffer vertexArray, colorArray, normalArray, textCoordArray;
    private GLArrayDataWrapper vArrayData, cArrayData, nArrayData, tArrayData;

    private boolean sealed, sealedGL;
    private boolean bufferEnabled, bufferWritten, bufferWrittenOnce;
    private boolean glslLocationSet;
  }

}

