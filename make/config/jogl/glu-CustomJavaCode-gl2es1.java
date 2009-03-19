/*
private static boolean useJavaMipmapCode = true;

static {
  AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        String val = System.getProperty("jogl.glu.nojava");
        if (val != null && !val.toLowerCase().equals("false")) {
          useJavaMipmapCode = false;
        }
        return null;
      }
    });
}
*/

/**
 * Instantiates a new OpenGL Utility Library object. A GLU object may
 * be instantiated at any point in the application and is not
 * inherently tied to any particular OpenGL context; however, the GLU
 * object may only be used when an OpenGL context is current on the
 * current thread. Attempts to call most of the methods in the GLU
 * library when no OpenGL context is current will cause an exception
 * to be thrown.
 *
 * <P>
 *
 * The returned GLU object is not guaranteed to be thread-safe and
 * should only be used from one thread at a time. Multiple GLU objects
 * may be instantiated to be used from different threads
 * simultaneously.
 */

public GLUgl2es1()
{
  super();
}

public void destroy() {
  super.destroy();
}

//----------------------------------------------------------------------
// Utility routines
//

public static final GL2ES1 getCurrentGL2ES1() throws GLException {
  GLContext curContext = GLContext.getCurrent();
  if (curContext == null) {
    throw new GLException("No OpenGL context current on this thread");
  }
  return curContext.getGL().getGL2ES1();
}


/*
public String gluErrorString(int errorCode) {
  return Error.gluErrorString(errorCode);
}
*/

/* extName is an extension name.
 * extString is a string of extensions separated by blank(s). There may or 
 * may not be leading or trailing blank(s) in extString.
 * This works in cases of extensions being prefixes of another like
 * GL_EXT_texture and GL_EXT_texture3D.
 * Returns true if extName is found otherwise it returns false.
 */
/*
public boolean gluCheckExtension(java.lang.String extName, java.lang.String extString) {
  return Registry.gluCheckExtension(extName, extString);
}
*/

/*
public String gluGetString(int name) {
  return Registry.gluGetString(name);
}
*/

//----------------------------------------------------------------------
// Mipmap and image scaling functionality

protected static boolean availableMipmap = false;
protected static boolean checkedMipmap = false;

protected static final void validateMipmap() {
    if(!checkedMipmap) {
        availableMipmap = NWReflection.isClassAvailable("com.sun.opengl.impl.glu.mipmap.Mipmap");
        checkedMipmap = true;
    }
    if(!availableMipmap) {
      throw new GLException("Mipmap not available");
    }
}

private final java.nio.ByteBuffer copyToByteBuffer(java.nio.Buffer buf) {
  if (buf instanceof java.nio.ByteBuffer) {
    if (buf.position() == 0) {
      return (java.nio.ByteBuffer) buf;
    }
    return InternalBufferUtil.copyByteBuffer((java.nio.ByteBuffer) buf);
  } else if (buf instanceof java.nio.ShortBuffer) {
    return InternalBufferUtil.copyShortBufferAsByteBuffer((java.nio.ShortBuffer) buf);
  } else if (buf instanceof java.nio.IntBuffer) {
    return InternalBufferUtil.copyIntBufferAsByteBuffer((java.nio.IntBuffer) buf);
  } else if (buf instanceof java.nio.FloatBuffer) {
    return InternalBufferUtil.copyFloatBufferAsByteBuffer((java.nio.FloatBuffer) buf);
  } else {
    throw new IllegalArgumentException("Unsupported buffer type (must be one of byte, short, int, or float)");
  }
}

/**
 * Optional, throws GLException if not available in profile
 */
public final int gluScaleImage( int format, int widthin, int heightin,
                               int typein, java.nio.Buffer datain, int widthout, int heightout,
                               int typeout, java.nio.Buffer dataout ) {
  validateMipmap();
  java.nio.ByteBuffer in = null;
  java.nio.ByteBuffer out = null;
  in = copyToByteBuffer(datain);
  if( dataout instanceof java.nio.ByteBuffer ) {
    out = (java.nio.ByteBuffer)dataout;
  } else if( dataout instanceof java.nio.ShortBuffer ) {
    out = InternalBufferUtil.newByteBuffer(dataout.remaining() * InternalBufferUtil.SIZEOF_SHORT);
  } else if ( dataout instanceof java.nio.IntBuffer ) {
    out = InternalBufferUtil.newByteBuffer(dataout.remaining() * InternalBufferUtil.SIZEOF_INT);
  } else if ( dataout instanceof java.nio.FloatBuffer ) {
    out = InternalBufferUtil.newByteBuffer(dataout.remaining() * InternalBufferUtil.SIZEOF_FLOAT);
  } else {
    throw new IllegalArgumentException("Unsupported destination buffer type (must be byte, short, int, or float)");
  }
  int errno = Mipmap.gluScaleImage( getCurrentGL2ES1(), format, widthin, heightin, typein, in, 
            widthout, heightout, typeout, out );
  if( errno == 0 ) {
    out.rewind();
    if (out != dataout) {
      if( dataout instanceof java.nio.ShortBuffer ) {
        ((java.nio.ShortBuffer) dataout).put(out.asShortBuffer());
      } else if( dataout instanceof java.nio.IntBuffer ) {
        ((java.nio.IntBuffer) dataout).put(out.asIntBuffer());
      } else if( dataout instanceof java.nio.FloatBuffer ) {
        ((java.nio.FloatBuffer) dataout).put(out.asFloatBuffer());
      } else {
        throw new RuntimeException("Should not reach here");
      }
    }
  }
  return( errno );
}


/**
 * Optional, throws GLException if not available in profile
 */
public final int gluBuild1DMipmapLevels( int target, int internalFormat, int width,
                                        int format, int type, int userLevel, int baseLevel, int maxLevel,
                                        java.nio.Buffer data ) {
  validateMipmap();
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmapLevels( getCurrentGL2ES1(), target, internalFormat, width,
          format, type, userLevel, baseLevel, maxLevel, buffer ) );
}


/**
 * Optional, throws GLException if not available in profile
 */
public final int gluBuild1DMipmaps( int target, int internalFormat, int width,
                                   int format, int type, java.nio.Buffer data ) {
  validateMipmap();
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmaps( getCurrentGL2ES1(), target, internalFormat, width, format,
          type, buffer ) );
}


/**
 * Optional, throws GLException if not available in profile
 */
public final int gluBuild2DMipmapLevels( int target, int internalFormat, int width,
                                        int height, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, java.nio.Buffer data ) {
  validateMipmap();
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmapLevels( getCurrentGL2ES1(), target, internalFormat, width,
          height, format, type, userLevel, baseLevel, maxLevel, data ) );
}

/**
 * Optional, throws GLException if not available in profile
 */
public final int gluBuild2DMipmaps( int target, int internalFormat, int width,
                                   int height, int format, int type, java.nio.Buffer data ) {
  validateMipmap();
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmaps( getCurrentGL2ES1(), target, internalFormat, width, height,
          format, type, data) );
}

/**
 * Optional, throws GLException if not available in profile
 */
public final int gluBuild3DMipmapLevels( int target, int internalFormat, int width,
                                        int height, int depth, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, java.nio.Buffer data) {
  validateMipmap();
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmapLevels( getCurrentGL2ES1(), target, internalFormat, width,
          height, depth, format, type, userLevel, baseLevel, maxLevel, buffer) );
}

/**
 * Optional, throws GLException if not available in profile
 */
public final int gluBuild3DMipmaps( int target, int internalFormat, int width,
                                   int height, int depth, int format, int type, java.nio.Buffer data ) {
  validateMipmap();
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmaps( getCurrentGL2ES1(), target, internalFormat, width, height,
          depth, format, type, buffer ) );
}

//----------------------------------------------------------------------
// GLUProcAddressTable handling
//

/*
private static GLUProcAddressTable gluProcAddressTable;
private static volatile boolean gluLibraryLoaded;

private static GLUProcAddressTable getGLUProcAddressTable() {
  if (!gluLibraryLoaded) {
    loadGLULibrary();
  }
  if (gluProcAddressTable == null) {
    GLUProcAddressTable tmp = new GLUProcAddressTable();
    GLProcAddressHelper.resetProcAddressTable(tmp, GLDrawableFactoryImpl.getFactoryImpl());
    gluProcAddressTable = tmp;
  }
  return gluProcAddressTable;
}

private static synchronized void loadGLULibrary() {
  if (!gluLibraryLoaded) {
    GLDrawableFactoryImpl.getFactoryImpl().loadGLULibrary();
    gluLibraryLoaded = true;
  }
}
*/
