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

public GLUes1()
{
  this.project = new ProjectFloat();
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
// Quadric functionality
//

/** Interface to C language function: <br> <code> void gluCylinder(GLUquadric *  quad, GLdouble base, GLdouble top, GLdouble height, GLint slices, GLint stacks); </code>    */
public final void gluCylinder(GLUquadric quad, double base, double top, double height, int slices, int stacks) {
  ((GLUquadricImpl) quad).drawCylinder(getCurrentGL2ES1(), (float) base, (float) top, (float) height, slices, stacks);
}

/** Interface to C language function: <br> <code> void gluDeleteQuadric(GLUquadric *  quad); </code>    */
public final void gluDeleteQuadric(GLUquadric quad) {
}

/** Interface to C language function: <br> <code> void gluDisk(GLUquadric *  quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops); </code>    */
public final void gluDisk(GLUquadric quad, double inner, double outer, int slices, int loops) {
  ((GLUquadricImpl) quad).drawDisk(getCurrentGL2ES1(), (float) inner, (float) outer, slices, loops);
}

/** Interface to C language function: <br> <code> GLUquadric *  gluNewQuadric(void); </code>    */
public final GLUquadric gluNewQuadric() {
  return new GLUquadricImpl();
}

/** Interface to C language function: <br> <code> void gluPartialDisk(GLUquadric *  quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops, GLdouble start, GLdouble sweep); </code>    */
public final void gluPartialDisk(GLUquadric quad, double inner, double outer, int slices, int loops, double start, double sweep) {
  ((GLUquadricImpl) quad).drawPartialDisk(getCurrentGL2ES1(), (float) inner, (float) outer, slices, loops, (float) start, (float) sweep);
}

/** Interface to C language function: <br> <code> void gluQuadricDrawStyle(GLUquadric *  quad, GLenum draw); </code>    */
public final void gluQuadricDrawStyle(GLUquadric quad, int draw) {
  ((GLUquadricImpl) quad).setDrawStyle(draw);
}

/** Interface to C language function: <br> <code> void gluQuadricNormals(GLUquadric *  quad, GLenum normal); </code>    */
public final void gluQuadricNormals(GLUquadric quad, int normal) {
  ((GLUquadricImpl) quad).setNormals(normal);
}

/** Interface to C language function: <br> <code> void gluQuadricOrientation(GLUquadric *  quad, GLenum orientation); </code>    */
public final void gluQuadricOrientation(GLUquadric quad, int orientation) {
  ((GLUquadricImpl) quad).setOrientation(orientation);
}

/** Interface to C language function: <br> <code> void gluQuadricTexture(GLUquadric *  quad, GLboolean texture); </code>    */
public final void gluQuadricTexture(GLUquadric quad, boolean texture) {
  ((GLUquadricImpl) quad).setTextureFlag(texture);
}

/** Interface to C language function: <br> <code> void gluSphere(GLUquadric *  quad, GLdouble radius, GLint slices, GLint stacks); </code>    */
public final void gluSphere(GLUquadric quad, double radius, int slices, int stacks) {
  ((GLUquadricImpl) quad).drawSphere(getCurrentGL2ES1(), (float) radius, slices, stacks);
}

//----------------------------------------------------------------------
// Projection routines
//

private ProjectFloat project;

public final void gluOrtho2D(float left, float right, float bottom, float top) {
  project.gluOrtho2D(getCurrentGL2ES1(), left, right, bottom, top);
}

public final void gluPerspective(float fovy, float aspect, float zNear, float zFar) {
  project.gluPerspective(getCurrentGL2ES1(), fovy, aspect, zNear, zFar);
}

public final void gluLookAt(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
  project.gluLookAt(getCurrentGL2ES1(), eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single array.
 */
public final boolean gluProject(float objX, float objY, float objZ, float[] model, int model_offset, float[] proj, int proj_offset, int[] view, int view_offset, float[] winPos, int winPos_offset) {
  return project.gluProject(objX, objY, objZ, model, model_offset, proj, proj_offset, view, view_offset, winPos, winPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single buffer.
 */
public final boolean gluProject(float objX, float objY, float objZ, java.nio.FloatBuffer model, java.nio.FloatBuffer proj, java.nio.IntBuffer view, java.nio.FloatBuffer winPos) {
  return project.gluProject(objX, objY, objZ, model, proj, view, winPos);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single array.
 */
public final boolean gluUnProject(float winX, float winY, float winZ, float[] model, int model_offset, float[] proj, int proj_offset, int[] view, int view_offset, float[] objPos, int objPos_offset) {
  return project.gluUnProject(winX, winY, winZ, model, model_offset, proj, proj_offset, view, view_offset, objPos, objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single buffer.
 */
public final boolean gluUnProject(float winX, float winY, float winZ, java.nio.FloatBuffer model, java.nio.FloatBuffer proj, java.nio.IntBuffer view, java.nio.FloatBuffer objPos) {
  return project.gluUnProject(winX, winY, winZ, model, proj, view, objPos);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single array.
 */
public final boolean gluUnProject4(float winX, float winY, float winZ, float clipW, float[] model, int model_offset, float[] proj, int proj_offset, int[] view, int view_offset, float nearVal, float farVal, float[] objPos, int objPos_offset) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, model_offset, proj, proj_offset, view, view_offset, nearVal, farVal, objPos, objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single buffer.
 */
public final boolean gluUnProject4(float winX, float winY, float winZ, float clipW, java.nio.FloatBuffer model, java.nio.FloatBuffer proj, java.nio.IntBuffer view, float nearVal, float farVal, java.nio.FloatBuffer objPos) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, proj, view, nearVal, farVal, objPos);
}

public final void gluPickMatrix(float x, float y, float delX, float delY, int[] viewport, int viewport_offset) {
  project.gluPickMatrix(getCurrentGL2ES1(), x, y, delX, delY, viewport, viewport_offset);
}

public final void gluPickMatrix(float x, float y, float delX, float delY, java.nio.IntBuffer viewport) {
  project.gluPickMatrix(getCurrentGL2ES1(), x, y, delX, delY, viewport);
}

public final void gluOrtho2D(double left, double right, double bottom, double top) {
  project.gluOrtho2D(getCurrentGL2ES1(), (float)left, (float)right, (float)bottom, (float)top);
}

public final void gluPerspective(double fovy, double aspect, double zNear, double zFar) {
  project.gluPerspective(getCurrentGL2ES1(), (float)fovy, (float)aspect, (float)zNear, (float)zFar);
}

public final void gluLookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
  project.gluLookAt(getCurrentGL2ES1(), (float)eyeX, (float)eyeY, (float)eyeZ, (float)centerX, (float)centerY, (float)centerZ, (float)upX, (float)upY, (float)upZ);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single array.
 */
public final boolean gluProject(double objX, double objY, double objZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] winPos, int winPos_offset) {
  return project.gluProject((float)objX, (float)objY, (float)objZ, BufferUtil.getFloatArray(model), model_offset, BufferUtil.getFloatArray(proj), proj_offset, view, view_offset, BufferUtil.getFloatArray(winPos), winPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single array.
 */
public final boolean gluUnProject(double winX, double winY, double winZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] objPos, int objPos_offset) {
  return project.gluUnProject((float)winX, (float)winY, (float)winZ, BufferUtil.getFloatArray(model), model_offset, BufferUtil.getFloatArray(proj), proj_offset, view, view_offset, BufferUtil.getFloatArray(objPos), objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single array.
 */
public final boolean gluUnProject4(double winX, double winY, double winZ, double clipW, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double nearVal, double farVal, double[] objPos, int objPos_offset) {
  return project.gluUnProject4((float)winX, (float)winY, (float)winZ, (float)clipW, BufferUtil.getFloatArray(model), model_offset, BufferUtil.getFloatArray(proj), proj_offset, view, view_offset, (float)nearVal, (float)farVal, BufferUtil.getFloatArray(objPos), objPos_offset);
}

public final void gluPickMatrix(double x, double y, double delX, double delY, int[] viewport, int viewport_offset) {
  project.gluPickMatrix(getCurrentGL2ES1(), (float)x, (float)y, (float)delX, (float)delY, viewport, viewport_offset);
}

public final void gluPickMatrix(double x, double y, double delX, double delY, IntBuffer viewport) {
  project.gluPickMatrix(getCurrentGL2ES1(), (float)x, (float)y, (float)delX, (float)delY, viewport);
}

//----------------------------------------------------------------------
// Mipmap and image scaling functionality

private final java.nio.ByteBuffer copyToByteBuffer(java.nio.Buffer buf) {
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

public final int gluScaleImage( int format, int widthin, int heightin,
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


public final int gluBuild1DMipmapLevels( int target, int internalFormat, int width,
                                        int format, int type, int userLevel, int baseLevel, int maxLevel,
                                        java.nio.Buffer data ) {
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmapLevels( getCurrentGL2ES1(), target, internalFormat, width,
          format, type, userLevel, baseLevel, maxLevel, buffer ) );
}


public final int gluBuild1DMipmaps( int target, int internalFormat, int width,
                                   int format, int type, java.nio.Buffer data ) {
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmaps( getCurrentGL2ES1(), target, internalFormat, width, format,
          type, buffer ) );
}


public final int gluBuild2DMipmapLevels( int target, int internalFormat, int width,
                                        int height, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, java.nio.Buffer data ) {
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmapLevels( getCurrentGL2ES1(), target, internalFormat, width,
          height, format, type, userLevel, baseLevel, maxLevel, data ) );
}

public final int gluBuild2DMipmaps( int target, int internalFormat, int width,
                                   int height, int format, int type, java.nio.Buffer data ) {
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmaps( getCurrentGL2ES1(), target, internalFormat, width, height,
          format, type, data) );
}

public final int gluBuild3DMipmapLevels( int target, int internalFormat, int width,
                                        int height, int depth, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, java.nio.Buffer data) {
  java.nio.ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmapLevels( getCurrentGL2ES1(), target, internalFormat, width,
          height, depth, format, type, userLevel, baseLevel, maxLevel, buffer) );
}

public final int gluBuild3DMipmaps( int target, int internalFormat, int width,
                                   int height, int depth, int format, int type, java.nio.Buffer data ) {
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
