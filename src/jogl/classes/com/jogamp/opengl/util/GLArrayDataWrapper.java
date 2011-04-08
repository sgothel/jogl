
package com.jogamp.opengl.util;

import javax.media.opengl.*;

import jogamp.opengl.util.glsl.fixedfunc.*;

import java.nio.*;

public class GLArrayDataWrapper implements GLArrayData {

  /**
   * Create a VBO, using a predefined fixed function array index, wrapping the given data.
   * 
   * @param gl the current GL instance
   * @param index The GL array index
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   * @param vboName
   * @param vboOffset
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget either {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @return the new create instance
   * @throws GLException
   */
  public static GLArrayDataWrapper createFixed(GL gl, int index, int comps, int dataType, boolean normalized, 
                                              int stride, Buffer buffer,
                                              int vboName, long vboOffset, int vboUsage, int vboTarget)
    throws GLException
  {
      gl.getGLProfile().isValidArrayDataType(index, comps, dataType, false, true);
      GLArrayDataWrapper adc = new GLArrayDataWrapper();
      adc.init(null, index, comps, dataType, normalized, stride, buffer, false, 
               vboName, vboOffset, vboUsage, vboTarget);
      return adc;
  }

  /**
   * Create a VBO, using a custom GLSL array attribute name, wrapping the given data.
   * 
   * @param gl the current GL instance
   * @param name  The custom name for the GL attribute, maybe null if gpuBufferTarget is {@link GL#GL_ELEMENT_ARRAY_BUFFER}    
   * @param comps The array component number
   * @param dataType The array index GL data type
   * @param normalized Whether the data shall be normalized
   * @param stride
   * @param buffer the user define data
   * @param vboName
   * @param vboOffset
   * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
   * @param vboTarget either {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   * @return the new create instance
   * @throws GLException
   */
  public static GLArrayDataWrapper createGLSL(GL gl, String name, int comps, int dataType, boolean normalized, 
                                             int stride, Buffer buffer,                                             
                                             int vboName, long vboOffset, int vboUsage, int vboTarget)
    throws GLException
  {
      if(!gl.hasGLSL()) {
        throw new GLException("GLArrayDataWrapper.GLSL not supported: "+gl);
      }
      gl.getGLProfile().isValidArrayDataType(-1, comps, dataType, true, true);

      GLArrayDataWrapper adc = new GLArrayDataWrapper();
      adc.init(name, -1, comps, dataType, normalized, stride, buffer, true,
              vboName, vboOffset, vboUsage, vboTarget);
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

  public final long getVBOOffset() { return vboEnabled?vboOffset:-1; }

  public final int getVBOName() { return vboEnabled?vboName:-1; }

  public final boolean isVBO() { return vboEnabled; }

  public final int getVBOUsage() { return vboEnabled?vboUsage:-1; }
  
  public final int getVBOTarget() { return vboEnabled?vboTarget:-1; }
  
  public final Buffer getBuffer() { return buffer; }

  public final int getComponentNumber() { return components; }

  public final int getComponentType() { return dataType; }

  public final int getComponentSize() {
    if(clazz==ByteBuffer.class) {
        return GLBuffers.SIZEOF_BYTE;
    }
    if(clazz==ShortBuffer.class) {
        return GLBuffers.SIZEOF_SHORT;
    }
    if(clazz==IntBuffer.class) {
        return GLBuffers.SIZEOF_INT;
    }
    if(clazz==FloatBuffer.class) {
        return GLBuffers.SIZEOF_FLOAT;
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
    buffer = null;
    vboName=0;
    vboEnabled=false;
    vboOffset=-1;
    valid = false;
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
                       ", offset "+vboOffset+
                       ", vboUsage 0x"+Integer.toHexString(vboUsage)+ 
                       ", vboTarget 0x"+Integer.toHexString(vboTarget)+ 
                       ", vboEnabled "+vboEnabled+ 
                       ", vboName "+vboName+ 
                       ", valid "+valid+                       
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

  /**
   * Enable or disable use of VBO.
   * Only possible if a VBO buffer name is defined.
   * @see #setVBOName(int)
   */  
  public void setVBOEnabled(boolean vboEnabled) {
    this.vboEnabled=vboEnabled;
  }

  /**
   * Set the VBO buffer name, if valid (>0) enable use of VBO
   * @see #setVBOEnabled(boolean)
   */  
  public void    setVBOName(int vboName) {
    this.vboName=vboName;
    setVBOEnabled(vboName>0);
  }

 /**  
  * @param vboUsage {@link GL2ES2#GL_STREAM_DRAW}, {@link GL#GL_STATIC_DRAW} or {@link GL#GL_DYNAMIC_DRAW}
  */  
  public void setVBOUsage(int vboUsage) { 
      this.vboUsage = vboUsage; 
  }
  
  /**  
   * @param vboTarget either {@link GL#GL_ARRAY_BUFFER} or {@link GL#GL_ELEMENT_ARRAY_BUFFER}
   */  
  public void setVBOTarget(int vboTarget) {
      this.vboTarget = vboTarget;
  }  

  protected void init(String name, int index, int comps, int dataType, boolean normalized, int stride, Buffer data, 
                      boolean isVertexAttribute, 
                      int vboName, long vboOffset, int vboUsage, int vboTarget)
    throws GLException
  {
    this.isVertexAttribute = isVertexAttribute;
    this.index = index;
    this.location = -1;
    // We can't have any dependence on the FixedFuncUtil class here for build bootstrapping reasons
    
    if( GL.GL_ELEMENT_ARRAY_BUFFER == vboTarget ) {
        // ok ..
    } else if( GL.GL_ARRAY_BUFFER == vboTarget ) {
        // check name ..
        this.name = ( null == name ) ? FixedFuncPipeline.getPredefinedArrayIndexName(index) : name ;
        if(null == this.name ) {
            throw new GLException("Not a valid array buffer index: "+index);
        }        
    } else if( 0 <= vboTarget ) {
        throw new GLException("Invalid GPUBuffer target: 0x"+Integer.toHexString(vboTarget));
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
    if(0 >= comps) {
        throw new GLException("Invalid number of components: " + comps);
    }
    this.components = comps;
    this.stride=stride;
    this.strideB=(0==stride)?comps*bpc:stride;
    this.strideL=(0==stride)?comps:strideB/bpc;
    this.vboName=vboName;
    this.vboEnabled=vboName>0;
    this.vboOffset=vboOffset;
    
    switch(vboUsage) {
        case -1: // nop
        case GL.GL_STATIC_DRAW:
        case GL.GL_DYNAMIC_DRAW:
        case GL2ES2.GL_STREAM_DRAW:
            break;
        default:
            throw new GLException("invalid gpuBufferUsage: "+vboUsage+":\n\t"+this); 
    }
    switch(vboTarget) {
        case -1: // nop
        case GL.GL_ARRAY_BUFFER:
        case GL.GL_ELEMENT_ARRAY_BUFFER:
            break;
        default:
            throw new GLException("invalid gpuBufferTarget: "+vboTarget+":\n\t"+this);
    }
    this.vboUsage=vboUsage;
    this.vboTarget=vboTarget;    
    this.valid=true;
  }

  protected GLArrayDataWrapper() { }

  protected boolean valid;
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

  protected long vboOffset;
  protected int vboName;
  protected boolean vboEnabled;
  protected int vboUsage;
  protected int vboTarget;  
}

