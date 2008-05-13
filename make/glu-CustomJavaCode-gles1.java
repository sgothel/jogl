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

public GLU()
{
  this.project = new ProjectES1();
}

//----------------------------------------------------------------------
// Utility routines
//

/**
 * Returns the GL object associated with the OpenGL context current on
 * the current thread. Throws GLException if no OpenGL context is
 * current.
 */

public static GL getCurrentGL() throws GLException {
  GLContext curContext = GLContext.getCurrent();
  if (curContext == null) {
    throw new GLException("No OpenGL context current on this thread");
  }
  return curContext.getGL();
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

/**
 * Returns true if the specified GLU core- or extension-function can be
 * successfully used through this GLU instance. By "successfully" we mean
 * that the function is both <i>callable</i> on the machine running the
 * program and <i>available</i> on the current display.<P>
 *
 * A GLU function is <i>callable</i> if it is a GLU core- or extension-function
 * that is supported by the underlying GLU implementation. The function is
 * <i>available</i> if the OpenGL implementation on the display meets the
 * requirements of the GLU function being called (because GLU functions utilize
 * OpenGL functions). <P>
 *
 * Whether or not a GLU function is <i>callable</i> is determined as follows:
 * <ul>
 *   <li>If the function is a GLU core function (i.e., not an
 *   extension), <code>gluGetString(GLU_VERSION)</code> is used to determine the
 *   version number of the underlying GLU implementation on the host.
 *   then the function name is cross-referenced with that specification to
 *   see if it is part of that version's specification.
 *
 *   <li> If the function is a GLU extension, the function name is
 *   cross-referenced with the list returned by
 *   <code>gluGetString(GLU_EXTENSIONS)</code> to see if the function is one of
 *   the extensions that is supported by the underlying GLU implementation.
 * </ul>
 *
 * Whether or not a GLU function is <i>available</i> is determined as follows:
 * <ul>
 *   <li>If the function is a GLU core function then the function is first
 *   cross-referenced with the GLU specifications to find the minimum GLU
 *   version required to <i>call</i> that GLU function. Then the following table
 *   is consulted to determine the minimum GL version required for that version
 *   of GLU:
 *   <ul>
 *   <li> GLU 1.0 requires OpenGL 1.0
 *   <li> GLU 1.1 requires OpenGL 1.0
 *   <li> GLU 1.2 requires OpenGL 1.1
 *   <li> GLU 1.3 requires OpenGL 1.2
 *   </ul>
 *   Finally, <code>glGetString(GL_VERSION)</code> is used to determine the
 *   highest OpenGL version that both host and display support, and from that it
 *   is possible to determine if the GL facilities required by the GLU function
 *   are <i>available</i> on the display.
 *
 *   <li> If the function is a GLU extension, the function name is
 *   cross-referenced with the list returned by
 *   <code>gluGetString(GLU_EXTENSIONS)</code> to see if the function is one of
 *   the extensions that is supported by the underlying GLU implementation.
 * </ul>
 *
 * <b>NOTE:</b>The availability of a function may change at runtime in
 * response to changes in the display environment. For example, when a window
 * is dragged from one display to another on a multi-display system, or when
 * the properties of the display device are modified (e.g., changing the color
 * depth of the display). Any application that is concerned with handling
 * these situations correctly should confirm availability after a display
 * change before calling a questionable OpenGL function. To detect a change in
 * the display device, please see {@link
 * GLEventListener#displayChanged(GLAutoDrawable,boolean,boolean)}.
 *
 * @param gluFunctionName the name of the OpenGL function (e.g., use
 * "gluNurbsCallbackDataEXT" to check if the <code>
 * gluNurbsCallbackDataEXT(GLUnurbs, GLvoid)</code> extension is available).
 */
public boolean isFunctionAvailable(String gluFunctionName)
{
    //  if (useJavaMipmapCode) {
    // All GLU functions are available in Java port
    return true;
    //  }
  //  return (gluProcAddressTable.getAddressFor(gluFunctionName) != 0);
}

//----------------------------------------------------------------------
// Projection routines
//

private ProjectES1 project;

public void gluOrtho2D(float left, float right, float bottom, float top) {
  project.gluOrtho2D(getCurrentGL(), left, right, bottom, top);
}

public void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
  project.gluPerspective(getCurrentGL(), fovy, aspect, zNear, zFar);
}

public void gluLookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
  project.gluLookAt(getCurrentGL(), eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single array.
 */
public boolean gluProject(float objX, float objY, float objZ, float[] model, int model_offset, float[] proj, int proj_offset, int[] view, int view_offset, float[] winPos, int winPos_offset) {
  return project.gluProject(objX, objY, objZ, model, model_offset, proj, proj_offset, view, view_offset, winPos, winPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single buffer.
 */
public boolean gluProject(float objX, float objY, float objZ, java.nio.FloatBuffer model, java.nio.FloatBuffer proj, java.nio.IntBuffer view, java.nio.FloatBuffer winPos) {
  return project.gluProject(objX, objY, objZ, model, proj, view, winPos);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single array.
 */
public boolean gluUnProject(float winX, float winY, float winZ, float[] model, int model_offset, float[] proj, int proj_offset, int[] view, int view_offset, float[] objPos, int objPos_offset) {
  return project.gluUnProject(winX, winY, winZ, model, model_offset, proj, proj_offset, view, view_offset, objPos, objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single buffer.
 */
public boolean gluUnProject(float winX, float winY, float winZ, java.nio.FloatBuffer model, java.nio.FloatBuffer proj, java.nio.IntBuffer view, java.nio.FloatBuffer objPos) {
  return project.gluUnProject(winX, winY, winZ, model, proj, view, objPos);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single array.
 */
public boolean gluUnProject4(float winX, float winY, float winZ, float clipW, float[] model, int model_offset, float[] proj, int proj_offset, int[] view, int view_offset, float nearVal, float farVal, float[] objPos, int objPos_offset) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, model_offset, proj, proj_offset, view, view_offset, nearVal, farVal, objPos, objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single buffer.
 */
public boolean gluUnProject4(float winX, float winY, float winZ, float clipW, java.nio.FloatBuffer model, java.nio.FloatBuffer proj, java.nio.IntBuffer view, float nearVal, float farVal, java.nio.FloatBuffer objPos) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, proj, view, nearVal, farVal, objPos);
}

public void gluPickMatrix(float x, float y, float delX, float delY, int[] viewport, int viewport_offset) {
  project.gluPickMatrix(getCurrentGL(), x, y, delX, delY, viewport, viewport_offset);
}

public void gluPickMatrix(float x, float y, float delX, float delY, java.nio.IntBuffer viewport) {
  project.gluPickMatrix(getCurrentGL(), x, y, delX, delY, viewport);
}

// Boolean
public static final int GLU_FALSE = 0;
public static final int GLU_TRUE = 1;

// String Name
public static final int GLU_VERSION = 100800;
public static final int GLU_EXTENSIONS = 100801;

// Extensions
public static final String versionString = "1.3";
public static final String extensionString = "GLU_EXT_nurbs_tessellator " +
                                             "GLU_EXT_object_space_tess ";

// ErrorCode
public static final int GLU_INVALID_ENUM = 100900;
public static final int GLU_INVALID_VALUE = 100901;
public static final int GLU_OUT_OF_MEMORY = 100902;
public static final int GLU_INVALID_OPERATION = 100904;

/*************

//----------------------------------------------------------------------
// Mipmap and image scaling functionality


// Boolean
public static final int GLU_FALSE = 0;
public static final int GLU_TRUE = 1;

// String Name
public static final int GLU_VERSION = 100800;
public static final int GLU_EXTENSIONS = 100801;

// Extensions
public static final String versionString = "1.3";
public static final String extensionString = "GLU_EXT_nurbs_tessellator " +
                                             "GLU_EXT_object_space_tess ";

// ErrorCode
public static final int GLU_INVALID_ENUM = 100900;
public static final int GLU_INVALID_VALUE = 100901;
public static final int GLU_OUT_OF_MEMORY = 100902;
public static final int GLU_INVALID_OPERATION = 100904;

private java.nio.ByteBuffer copyToByteBuffer(java.nio.Buffer buf) {
  if (buf instanceof java.nio.ByteBuffer) {
    if (buf.position() == 0) {
      return (java.nio.ByteBuffer) buf;
    }
    return BufferUtil.copyByteBuffer((java.nio.ByteBuffer) buf);
  } else if (buf instanceof java.nio.ShortBuffer) {
    return BufferUtil.copyShortBufferAsByteBuffer((java.nio.ShortBuffer) buf);
  } else if (buf instanceof java.nio.IntBuffer) {
    return BufferUtil.copyIntBufferAsByteBuffer((java.nio.IntBuffer) buf);
  } else if (buf instanceof java.nio.FloatBuffer) {
    return BufferUtil.copyFloatBufferAsByteBuffer((java.nio.FloatBuffer) buf);
  } else {
    throw new IllegalArgumentException("Unsupported buffer type (must be one of byte, short, int, or float)");
  }
}

private int gluScaleImageJava( int format, int widthin, int heightin,
                               int typein, java.nio.Buffer datain, int widthout, int heightout,
                               int typeout, java.nio.Buffer dataout ) {
  java.nio.ByteBuffer in = null;
  java.nio.ByteBuffer out = null;
  in = copyToByteBuffer(datain);
  if( dataout instanceof java.nio.ByteBuffer ) {
    out = (java.nio.ByteBuffer)dataout;
  } else if( dataout instanceof java.nio.ShortBuffer ) {
    out = BufferUtil.newByteBuffer(dataout.remaining() * BufferUtil.SIZEOF_SHORT);
  } else if ( dataout instanceof java.nio.IntBuffer ) {
    out = BufferUtil.newByteBuffer(dataout.remaining() * BufferUtil.SIZEOF_INT);
  } else if ( dataout instanceof java.nio.FloatBuffer ) {
    out = BufferUtil.newByteBuffer(dataout.remaining() * BufferUtil.SIZEOF_FLOAT);
  } else {
    throw new IllegalArgumentException("Unsupported destination buffer type (must be byte, short, int, or float)");
  }
  int errno = Mipmap.gluScaleImage( getCurrentGL(), format, widthin, heightin, typein, in, 
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


private int gluBuild1DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int format, int type, int userLevel, int baseLevel, int maxLevel,
                                        java.nio.Buffer data ) {
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmapLevels( getCurrentGL(), target, internalFormat, width,
          format, type, userLevel, baseLevel, maxLevel, buffer ) );
}


private int gluBuild1DMipmapsJava( int target, int internalFormat, int width,
                                   int format, int type, java.nio.Buffer data ) {
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmaps( getCurrentGL(), target, internalFormat, width, format,
          type, buffer ) );
}


private int gluBuild2DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int height, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, java.nio.Buffer data ) {
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmapLevels( getCurrentGL(), target, internalFormat, width,
          height, format, type, userLevel, baseLevel, maxLevel, data ) );
}

private int gluBuild2DMipmapsJava( int target, int internalFormat, int width,
                                   int height, int format, int type, java.nio.Buffer data ) {
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmaps( getCurrentGL(), target, internalFormat, width, height,
          format, type, data) );
}

private int gluBuild3DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int height, int depth, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, java.nio.Buffer data) {
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmapLevels( getCurrentGL(), target, internalFormat, width,
          height, depth, format, type, userLevel, baseLevel, maxLevel, buffer) );
}

private int gluBuild3DMipmapsJava( int target, int internalFormat, int width,
                                   int height, int depth, int format, int type, java.nio.Buffer data ) {
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmaps( getCurrentGL(), target, internalFormat, width, height,
          depth, format, type, buffer ) );
}

*********************/

//----------------------------------------------------------------------
// Wrappers for mipmap and image scaling entry points which dispatch either
// to the Java or C versions.
//

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
/*
public int gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapLevelsJava(target, internalFormat, width, format, type, level, base, max, data);
  } else {
    return gluBuild1DMipmapLevelsC(target, internalFormat, width, format, type, level, base, max, data);
  }
}
*/

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
/*
public int gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapsJava(target, internalFormat, width, format, type, data);
  } else {
    return gluBuild1DMipmapsC(target, internalFormat, width, format, type, data);
  }
}
*/

/** Interface to C language function: <br> <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
/*
public int gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapLevelsJava(target, internalFormat, width, height, format, type, level, base, max, data);
  } else {
    return gluBuild2DMipmapLevelsC(target, internalFormat, width, height, format, type, level, base, max, data);
  }
}
*/

/** Interface to C language function: <br> <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
/*
public int gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapsJava(target, internalFormat, width, height, format, type, data);
  } else {
    return gluBuild2DMipmapsC(target, internalFormat, width, height, format, type, data);
  }
}
*/

/** Interface to C language function: <br> <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
/*
public int gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapLevelsJava(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  } else {
    return gluBuild3DMipmapLevelsC(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  }
}
*/

/** Interface to C language function: <br> <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
/*
public int gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapsJava(target, internalFormat, width, height, depth, format, type, data);
  } else {
    return gluBuild3DMipmapsC(target, internalFormat, width, height, depth, format, type, data);
  }
}
*/

/** Interface to C language function: <br> <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
/*
public int gluScaleImage(int format, int wIn, int hIn, int typeIn, java.nio.Buffer dataIn, int wOut, int hOut, int typeOut, java.nio.Buffer dataOut) {
  if (useJavaMipmapCode) {
    return gluScaleImageJava(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  } else {
    return gluScaleImageC(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  }
}
*/

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
    ProcAddressHelper.resetProcAddressTable(tmp, GLDrawableFactoryImpl.getFactoryImpl());
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
