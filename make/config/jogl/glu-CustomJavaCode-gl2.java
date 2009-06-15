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

public GLUgl2()
{
  this.project = new ProjectDouble();
}

public void destroy() {
  if(null!=this.project) {
      this.project.destroy();
      this.project=null;
  }
  super.destroy();
}


//----------------------------------------------------------------------
// Utility routines
//

public static final GL2 getCurrentGL2() throws GLException {
  GLContext curContext = GLContext.getCurrent();
  if (curContext == null) {
    throw new GLException("No OpenGL context current on this thread");
  }
  return curContext.getGL().getGL2();
}

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
public final boolean isFunctionAvailable(String gluFunctionName)
{
  if (useJavaMipmapCode) {
    // All GLU functions are available in Java port
    return true;
  }
  return (gluProcAddressTable.getAddressFor(gluFunctionName) != 0);
}

//----------------------------------------------------------------------
// Projection routines
//

private ProjectDouble project;

public final void gluOrtho2D(float left, float right, float bottom, float top) {
  project.gluOrtho2D(getCurrentGL2(), (double)left, (double)right, (double)bottom, (double)top);

}

public final void gluOrtho2D(double left, double right, double bottom, double top) {
  project.gluOrtho2D(getCurrentGL2(), left, right, bottom, top);
}

public final void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
  project.gluPerspective(getCurrentGL2(), (double)fovy, (double)aspect, (double)zNear, (double)zFar);
}

public final void gluPerspective(double fovy, double aspect, double zNear, double zFar) {
  project.gluPerspective(getCurrentGL2(), fovy, aspect, zNear, zFar);
}

public final void gluLookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
  project.gluLookAt(getCurrentGL2(), (double)eyeX, (double)eyeY, (double)eyeZ, (double)centerX, (double)centerY, (double)centerZ, (double)upX, (double)upY, (double)upZ);
}

public final void gluLookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
  project.gluLookAt(getCurrentGL2(), eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single array.
 */
public final boolean gluProject(double objX, double objY, double objZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] winPos, int winPos_offset) {
  return project.gluProject(objX, objY, objZ, model, model_offset, proj, proj_offset, view, view_offset, winPos, winPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single buffer.
 */
public final boolean gluProject(double objX, double objY, double objZ, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, DoubleBuffer winPos) {
  return project.gluProject(objX, objY, objZ, model, proj, view, winPos);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single array.
 */
public final boolean gluUnProject(double winX, double winY, double winZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] objPos, int objPos_offset) {
  return project.gluUnProject(winX, winY, winZ, model, model_offset, proj, proj_offset, view, view_offset, objPos, objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single buffer.
 */
public final boolean gluUnProject(double winX, double winY, double winZ, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, DoubleBuffer objPos) {
  return project.gluUnProject(winX, winY, winZ, model, proj, view, objPos);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single array.
 */
public final boolean gluUnProject4(double winX, double winY, double winZ, double clipW, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double nearVal, double farVal, double[] objPos, int objPos_offset) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, model_offset, proj, proj_offset, view, view_offset, nearVal, farVal, objPos, objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single buffer.
 */
public final boolean gluUnProject4(double winX, double winY, double winZ, double clipW, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, double nearVal, double farVal, DoubleBuffer objPos) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, proj, view, nearVal, farVal, objPos);
}

public final void gluPickMatrix(double x, double y, double delX, double delY, int[] viewport, int viewport_offset) {
  project.gluPickMatrix(getCurrentGL2(), x, y, delX, delY, viewport, viewport_offset);
}

public final void gluPickMatrix(double x, double y, double delX, double delY, IntBuffer viewport) {
  project.gluPickMatrix(getCurrentGL2(), x, y, delX, delY, viewport);
}

//----------------------------------------------------------------------
// Mipmap and image scaling functionality


private final ByteBuffer copyToByteBuffer(Buffer buf) {
  if (buf instanceof ByteBuffer) {
    if (buf.position() == 0) {
      return (ByteBuffer) buf;
    }
    return InternalBufferUtil.copyByteBuffer((ByteBuffer) buf);
  } else if (buf instanceof ShortBuffer) {
    return InternalBufferUtil.copyShortBufferAsByteBuffer((ShortBuffer) buf);
  } else if (buf instanceof IntBuffer) {
    return InternalBufferUtil.copyIntBufferAsByteBuffer((IntBuffer) buf);
  } else if (buf instanceof FloatBuffer) {
    return InternalBufferUtil.copyFloatBufferAsByteBuffer((FloatBuffer) buf);
  } else {
    throw new IllegalArgumentException("Unsupported buffer type (must be one of byte, short, int, or float)");
  }
}

private final int gluScaleImageJava( int format, int widthin, int heightin,
                               int typein, Buffer datain, int widthout, int heightout,
                               int typeout, Buffer dataout ) {
  ByteBuffer in = null;
  ByteBuffer out = null;
  in = copyToByteBuffer(datain);
  if( dataout instanceof ByteBuffer ) {
    out = (ByteBuffer)dataout;
  } else if( dataout instanceof ShortBuffer ) {
    out = InternalBufferUtil.newByteBuffer(dataout.remaining() * InternalBufferUtil.SIZEOF_SHORT);
  } else if ( dataout instanceof IntBuffer ) {
    out = InternalBufferUtil.newByteBuffer(dataout.remaining() * InternalBufferUtil.SIZEOF_INT);
  } else if ( dataout instanceof FloatBuffer ) {
    out = InternalBufferUtil.newByteBuffer(dataout.remaining() * InternalBufferUtil.SIZEOF_FLOAT);
  } else {
    throw new IllegalArgumentException("Unsupported destination buffer type (must be byte, short, int, or float)");
  }
  int errno = Mipmap.gluScaleImage( getCurrentGL2(), format, widthin, heightin, typein, in, 
            widthout, heightout, typeout, out );
  if( errno == 0 ) {
    out.rewind();
    if (out != dataout) {
      if( dataout instanceof ShortBuffer ) {
        ((ShortBuffer) dataout).put(out.asShortBuffer());
      } else if( dataout instanceof IntBuffer ) {
        ((IntBuffer) dataout).put(out.asIntBuffer());
      } else if( dataout instanceof FloatBuffer ) {
        ((FloatBuffer) dataout).put(out.asFloatBuffer());
      } else {
        throw new RuntimeException("Should not reach here");
      }
    }
  }
  return( errno );
}


private final int gluBuild1DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int format, int type, int userLevel, int baseLevel, int maxLevel,
                                        Buffer data ) {
  ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmapLevels( getCurrentGL2(), target, internalFormat, width,
          format, type, userLevel, baseLevel, maxLevel, buffer ) );
}


private final int gluBuild1DMipmapsJava( int target, int internalFormat, int width,
                                   int format, int type, Buffer data ) {
  ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmaps( getCurrentGL2(), target, internalFormat, width, format,
          type, buffer ) );
}


private final int gluBuild2DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int height, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, Buffer data ) {
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmapLevels( getCurrentGL2(), target, internalFormat, width,
          height, format, type, userLevel, baseLevel, maxLevel, data ) );
}

private final int gluBuild2DMipmapsJava( int target, int internalFormat, int width,
                                   int height, int format, int type, Buffer data ) {
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmaps( getCurrentGL2(), target, internalFormat, width, height,
          format, type, data) );
}

private final int gluBuild3DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int height, int depth, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, Buffer data) {
  ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmapLevels( getCurrentGL2(), target, internalFormat, width,
          height, depth, format, type, userLevel, baseLevel, maxLevel, buffer) );
}

private final int gluBuild3DMipmapsJava( int target, int internalFormat, int width,
                                   int height, int depth, int format, int type, Buffer data ) {
  ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmaps( getCurrentGL2(), target, internalFormat, width, height,
          depth, format, type, buffer ) );
}


//----------------------------------------------------------------------
// Wrappers for mipmap and image scaling entry points which dispatch either
// to the Java or C versions.
//

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public final int gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapLevelsJava(target, internalFormat, width, format, type, level, base, max, data);
  } else {
    return gluBuild1DMipmapLevelsC(target, internalFormat, width, format, type, level, base, max, data);
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
public final int gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapsJava(target, internalFormat, width, format, type, data);
  } else {
    return gluBuild1DMipmapsC(target, internalFormat, width, format, type, data);
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public final int gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapLevelsJava(target, internalFormat, width, height, format, type, level, base, max, data);
  } else {
    return gluBuild2DMipmapLevelsC(target, internalFormat, width, height, format, type, level, base, max, data);
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
public final int gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapsJava(target, internalFormat, width, height, format, type, data);
  } else {
    return gluBuild2DMipmapsC(target, internalFormat, width, height, format, type, data);
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public final int gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapLevelsJava(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  } else {
    return gluBuild3DMipmapLevelsC(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
public final int gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapsJava(target, internalFormat, width, height, depth, format, type, data);
  } else {
    return gluBuild3DMipmapsC(target, internalFormat, width, height, depth, format, type, data);
  }
}


/** Interface to C language function: <br> <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
public final int gluScaleImage(int format, int wIn, int hIn, int typeIn, java.nio.Buffer dataIn, int wOut, int hOut, int typeOut, java.nio.Buffer dataOut) {
  if (useJavaMipmapCode) {
    return gluScaleImageJava(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  } else {
    return gluScaleImageC(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  }
}

//----------------------------------------------------------------------
// NURBS functionality
//

/**
 * Sets a property on a NURBS object. (NOTE: this function is not currently implemented.)
 * 
 * @param r
 *            GLUnurbs object holding NURBS to which a property should be
 *            set
 * @param property
 *            property id
 * @param value
 *            property value
 */
public final void gluNurbsProperty(GLUnurbs r, int property, float value) {
  // TODO glunurbsproperty
  float nurbsValue;
  switch (property) {
  default:
    //				System.out.println("TODO gluwnurbs.glunurbsproperty");
    break;
  }
}

/**
 * Creates a new GLUnurbs object.
 * 
 * @return GLUnurbs object
 */
public final GLUnurbs gluNewNurbsRenderer() {
  // DONE
  return new GLUgl2nurbsImpl();
}

/**
 * Begins a curve definition.
 * 
 * @param r
 *            GLUnurbs object to specify curve to
 */
public final void gluBeginCurve(GLUnurbs r) {
  // DONE
  ((GLUgl2nurbsImpl) r).bgncurve();
}

/**
 * Begins a surface definition.
 * 
 * @param r
 *            GLUnurbs object to specify surface to
 */
public final void gluBeginSurface(GLUnurbs r) {
  // DONE
  ((GLUgl2nurbsImpl) r).bgnsurface();
}

/**
 * Ends a surface.
 * 
 * @param r
 *            GLUnurbs object holding surface
 */
public final void gluEndSurface(GLUnurbs r) {
  // DONE
  ((GLUgl2nurbsImpl) r).endsurface();
}

/**
 * Makes a NURBS surface.
 * 
 * @param r
 *            GLUnurbs object holding the surface
 * @param sknot_count
 *            number of knots in s direction
 * @param sknot
 *            knots in s direction
 * @param tknot_count
 *            number of knots in t direction
 * @param tknot
 *            knots in t direction
 * @param s_stride
 *            number of control points coordinates in s direction
 * @param t_stride
 *            number of control points coordinates in t direction
 * @param ctlarray
 *            control points
 * @param sorder
 *            order of surface in s direction
 * @param torder
 *            order of surface in t direction
 * @param type
 *            surface type
 */
public final void gluNurbsSurface(GLUnurbs r, int sknot_count, float[] sknot,
                            int tknot_count, float[] tknot, int s_stride, int t_stride,
                            float[] ctlarray, int sorder, int torder, int type) {
  // DONE
  ((GLUgl2nurbsImpl) r).nurbssurface(sknot_count, sknot, tknot_count, tknot, s_stride,
                                  t_stride, ctlarray, sorder, torder, type);
}

/**
 * Make a NURBS curve.
 * 
 * @param r
 *            GLUnurbs object holding the curve
 * @param nknots
 *            number of knots
 * @param knot
 *            knot vector
 * @param stride
 *            number of control point coordinates
 * @param ctlarray
 *            control points
 * @param order
 *            order of the curve
 * @param type
 *            curve type
 */
public final void gluNurbsCurve(GLUnurbs r, int nknots, float[] knot, int stride,
                          float[] ctlarray, int order, int type) {
  int realType;
  switch (type) {
    // TODO GLU_MAP1_TRIM_2 etc.
  default:
    realType = type;
    break;
  }
  ((GLUgl2nurbsImpl) r).nurbscurve(nknots, knot, stride, ctlarray, order, realType);
}

/**
 * Ends a curve definition.
 * 
 * @param r
 *            GLUnurbs object holding the curve
 */
public final void gluEndCurve(GLUnurbs r) {
  //DONE
  ((GLUgl2nurbsImpl) r).endcurve();
}

//----------------------------------------------------------------------
// GLUProcAddressTable handling
//

private static GLUgl2ProcAddressTable gluProcAddressTable;
private static volatile boolean gluLibraryLoaded;

private static final GLUgl2ProcAddressTable getGLUProcAddressTable() {
  if (!gluLibraryLoaded) {
    loadGLULibrary();
  }
  if (gluProcAddressTable == null) {
    GLContext curContext = GLContext.getCurrent();
    if (curContext == null) {
        throw new GLException("No OpenGL context current on this thread");
    }
    GLUgl2ProcAddressTable tmp = new GLUgl2ProcAddressTable();
    GLProcAddressHelper.resetProcAddressTable(tmp, ((GLDrawableImpl)curContext.getGLDrawable()).getDynamicLookupHelper());
    gluProcAddressTable = tmp;
  }
  return gluProcAddressTable;
}

private static final synchronized void loadGLULibrary() {
  if (!gluLibraryLoaded) {
    GLDrawableFactoryImpl.getFactoryImpl(null).loadGLULibrary();
    gluLibraryLoaded = true;
  }
}
