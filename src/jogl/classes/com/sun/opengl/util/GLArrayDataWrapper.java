
package com.sun.opengl.util;

import javax.media.opengl.*;

import com.sun.opengl.util.glsl.fixedfunc.impl.*;

import java.nio.*;

public class GLArrayDataWrapper implements GLArrayData {

  public static GLArrayDataWrapper createFixed(GL gl, int index, int comps, int dataType, boolean normalized, 
                                              int stride, Buffer buffer,
                                              int vboName, long bufferOffset)
    throws GLException
  {
      gl.getGLProfile().isValidArrayDataType(index, comps, dataType, false, true);
      GLArrayDataWrapper adc = new GLArrayDataWrapper();
      adc.init(null, index, comps, dataType, normalized, stride, buffer, false, 
               vboName, bufferOffset);
      return adc;
  }

  public static GLArrayDataWrapper createGLSL(GL gl, String name, int comps, int dataType, boolean normalized, 
                                             int stride, Buffer buffer,
                                             int vboName, long bufferOffset)
    throws GLException
  {
      if(!gl.hasGLSL()) {
        throw new GLException("GLArrayDataWrapper.GLSL not supported: "+gl);
      }
      gl.getGLProfile().isValidArrayDataType(-1, comps, dataType, true, true);

      GLArrayDataWrapper adc = new GLArrayDataWrapper();
      adc.init(name, -1, comps, dataType, normalized, stride, buffer, true,
               vboName, bufferOffset);
      return adc;
  }

  // 
  // Data read access
  //

  public final boolean isVertexAttribute() { return isVertexAttribute; }

  public final int getIndex() { return index; }

  public final int getLocation() { return location; }

  public final void setLocation(int v) { location = v; }

  public final String getName() { return name; }

  public final long getOffset() { return vboUsage?bufferOffset:-1; }

  public final int getVBOName() { return vboUsage?vboName:-1; }

  public final boolean isVBO() { return vboUsage; }

  public final Buffer getBuffer() { return buffer; }

  public final int getComponentNumber() { return components; }

  public final int getComponentType() { return dataType; }

  public final int getComponentSize() {
    if(clazz==ByteBuffer.class) {
        return BufferUtil.SIZEOF_BYTE;
    }
    if(clazz==ShortBuffer.class) {
        return BufferUtil.SIZEOF_SHORT;
    }
    if(clazz==IntBuffer.class) {
        return BufferUtil.SIZEOF_INT;
    }
    if(clazz==FloatBuffer.class) {
        return BufferUtil.SIZEOF_FLOAT;
    }
    throw new GLException("Given Buffer Class not supported: "+clazz+":\n\t"+this);
  }

  public final int getElementNumber() {
    if(null==buffer) return 0;
    return ( buffer.position()==0 ) ? ( buffer.limit() / components ) : ( buffer.position() / components ) ;
  }

  public final boolean getNormalized() { return normalized; }

  public final int getStride() { return stride; }

  public final Class getBufferClass() { return clazz; }

  public void destroy(GL gl) {
    this.buffer = null;
    this.components = 0;
    this.stride=0;
    this.strideB=0;
    this.strideL=0;
    this.vboName=0;
    this.vboUsage=false;
    this.bufferOffset=0;
  }

  public String toString() {
    return "GLArrayDataWrapper["+name+
                       ", index "+index+
                       ", location "+location+
                       ", isVertexAttribute "+isVertexAttribute+
                       ", dataType "+dataType+ 
                       ", bufferClazz "+clazz+ 
                       ", elements "+getElementNumber()+
                       ", components "+components+ 
                       ", stride "+stride+"u "+strideB+"b "+strideL+"c"+
                       ", buffer "+buffer+ 
                       ", offset "+bufferOffset+ 
                       ", vboUsage "+vboUsage+ 
                       ", vboName "+vboName+ 
                       "]";
  }

  public static final Class getBufferClass(int dataType) {
    switch(dataType) {
        case GL.GL_BYTE:
        case GL.GL_UNSIGNED_BYTE:
            return ByteBuffer.class;
        case GL.GL_SHORT:
        case GL.GL_UNSIGNED_SHORT:
            return ShortBuffer.class;
        case GL2ES1.GL_FIXED:
            return IntBuffer.class;
        case GL.GL_FLOAT:
            return FloatBuffer.class;
        default:    
            throw new GLException("Given OpenGL data type not supported: "+dataType);
    }
  }

  public void setName(String newName) {
    location = -1;
    name = newName;
  }

  public void    setVBOUsage(boolean vboUsage) {
    this.vboUsage=vboUsage;
  }

  public void    setVBOName(int vboName) {
    this.vboName=vboName;
    setVBOUsage(vboName>0);
  }

  protected void init(String name, int index, int comps, int dataType, boolean normalized, int stride, Buffer data, 
                      boolean isVertexAttribute, 
                      int vboName, long bufferOffset)
    throws GLException
  {
    this.isVertexAttribute = isVertexAttribute;
    this.index = index;
    this.location = -1;
    // We can't have any dependence on the FixedFuncUtil class here for build bootstrapping reasons
    this.name = (null==name)?FixedFuncPipeline.getPredefinedArrayIndexName(index):name;
    if(null==this.name) {
        throw new GLException("Not a valid GL array index: "+index);
    }
    this.dataType = dataType;
    this.clazz = getBufferClass(dataType);
    switch(dataType) {
        case GL.GL_BYTE:
        case GL.GL_UNSIGNED_BYTE:
        case GL.GL_SHORT:
        case GL.GL_UNSIGNED_SHORT:
        case GL2ES1.GL_FIXED:
            this.normalized = normalized;
            break;
        default:    
            this.normalized = false;
    }

    int bpc = getComponentSize();
    if(0<stride && stride<comps*bpc) {
        throw new GLException("stride ("+stride+") lower than component bytes, "+comps+" * "+bpc);
    }
    if(0<stride && stride%bpc!=0) {
        throw new GLException("stride ("+stride+") not a multiple of bpc "+bpc);
    }
    this.buffer = data;
    this.components = comps;
    this.stride=stride;
    this.strideB=(0==stride)?comps*bpc:stride;
    this.strideL=(0==stride)?comps:strideB/bpc;
    this.vboName=vboName;
    this.vboUsage=vboName>0;
    this.bufferOffset=bufferOffset;
  }

  protected GLArrayDataWrapper() { }

  protected int index;
  protected int location;
  protected String name;
  protected int components;
  protected int dataType;
  protected boolean normalized;
  protected int stride;  // user given stride
  protected int strideB; // stride in bytes
  protected int strideL; // stride in logical components
  protected Class clazz;
  protected Buffer buffer;
  protected boolean isVertexAttribute;

  protected long bufferOffset;
  protected int vboName;
  protected boolean vboUsage;
}

