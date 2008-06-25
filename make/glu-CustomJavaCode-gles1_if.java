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
// Utility routines
//

public static final GLU createGLU() throws GLException {
  GLU glu = null;
  GL gl = getCurrentGL();
  if(gl==null) {
    throw new GLException("No current GL");
  }
  return createGLU(gl);
}

public static final GLU createGLU(GL gl) throws GLException {
  GLU glu = null;
  String clazzName;
  if(gl.isGL2()) {
    clazzName="javax.media.opengl.glu.gl2.GLUgl2";
  } else if(gl.isGLES1()) {
    clazzName="javax.media.opengl.glu.es1.GLUes1";
  } else {
    throw new GLException("GLU not supported for GL "+gl);
  }
  return (GLU) GLReflection.createInstance(clazzName);
}

public static final GL getCurrentGL() throws GLException {
  GLContext curContext = GLContext.getCurrent();
  if (curContext == null) {
    throw new GLException("No OpenGL context current on this thread");
  }
  return curContext.getGL();
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


// QuadricDrawStyle
public static final int GLU_POINT = 100010;
public static final int GLU_LINE = 100011;
public static final int GLU_FILL = 100012;
public static final int GLU_SILHOUETTE = 100013;

// QuadricCallback
// GLU_ERROR

// QuadricNormal
public static final int GLU_SMOOTH = 100000;
public static final int GLU_FLAT = 100001;
public static final int GLU_NONE = 100002;

// QuadricOrientation
public static final int GLU_OUTSIDE = 100020;
public static final int GLU_INSIDE = 100021;

// NurbsDisplay
// GLU_FILL
//public static final int GLU_OUTLINE_POLYGON = 100240;
//public static final int GLU_OUTLINE_PATCH = 100241;

// NurbsCallback
//public static final int GLU_NURBS_ERROR = 100103;
public static final int GLU_ERROR = 100103;
//public static final int GLU_NURBS_BEGIN = 100164;
//public static final int GLU_NURBS_BEGIN_EXT = 100164;
//public static final int GLU_NURBS_VERTEX = 100165;
//public static final int GLU_NURBS_VERTEX_EXT = 100165;
//public static final int GLU_NURBS_NORMAL = 100166;
//public static final int GLU_NURBS_NORMAL_EXT = 100166;
//public static final int GLU_NURBS_COLOR = 100167;
//public static final int GLU_NURBS_COLOR_EXT = 100167;
//public static final int GLU_NURBS_TEXTURE_COORD = 100168;
//public static final int GLU_NURBS_TEX_COORD_EXT = 100168;
//public static final int GLU_NURBS_END = 100169;
//public static final int GLU_NURBS_END_EXT = 100169;
//public static final int GLU_NURBS_BEGIN_DATA = 100170;
//public static final int GLU_NURBS_BEGIN_DATA_EXT = 100170;
//public static final int GLU_NURBS_VERTEX_DATA = 100171;
//public static final int GLU_NURBS_VERTEX_DATA_EXT = 100171;
//public static final int GLU_NURBS_NORMAL_DATA = 100172;
//public static final int GLU_NURBS_NORMAL_DATA_EXT = 100172;
//public static final int GLU_NURBS_COLOR_DATA = 100173;
//public static final int GLU_NURBS_COLOR_DATA_EXT = 100173;
//public static final int GLU_NURBS_TEXTURE_COORD_DATA = 100174;
//public static final int GLU_NURBS_TEX_COORD_DATA_EXT = 100174;
//public static final int GLU_NURBS_END_DATA = 100175;
//public static final int GLU_NURBS_END_DATA_EXT = 100175;

// NurbsError
//public static final int GLU_NURBS_ERROR1 = 100251;
//public static final int GLU_NURBS_ERROR2 = 100252;
//public static final int GLU_NURBS_ERROR3 = 100253;
//public static final int GLU_NURBS_ERROR4 = 100254;
//public static final int GLU_NURBS_ERROR5 = 100255;
//public static final int GLU_NURBS_ERROR6 = 100256;
//public static final int GLU_NURBS_ERROR7 = 100257;
//public static final int GLU_NURBS_ERROR8 = 100258;
//public static final int GLU_NURBS_ERROR9 = 100259;
//public static final int GLU_NURBS_ERROR10 = 100260;
//public static final int GLU_NURBS_ERROR11 = 100261;
//public static final int GLU_NURBS_ERROR12 = 100262;
//public static final int GLU_NURBS_ERROR13 = 100263;
//public static final int GLU_NURBS_ERROR14 = 100264;
//public static final int GLU_NURBS_ERROR15 = 100265;
//public static final int GLU_NURBS_ERROR16 = 100266;
//public static final int GLU_NURBS_ERROR17 = 100267;
//public static final int GLU_NURBS_ERROR18 = 100268;
//public static final int GLU_NURBS_ERROR19 = 100269;
//public static final int GLU_NURBS_ERROR20 = 100270;
//public static final int GLU_NURBS_ERROR21 = 100271;
//public static final int GLU_NURBS_ERROR22 = 100272;
//public static final int GLU_NURBS_ERROR23 = 100273;
//public static final int GLU_NURBS_ERROR24 = 100274;
//public static final int GLU_NURBS_ERROR25 = 100275;
//public static final int GLU_NURBS_ERROR26 = 100276;
//public static final int GLU_NURBS_ERROR27 = 100277;
//public static final int GLU_NURBS_ERROR28 = 100278;
//public static final int GLU_NURBS_ERROR29 = 100279;
//public static final int GLU_NURBS_ERROR30 = 100280;
//public static final int GLU_NURBS_ERROR31 = 100281;
//public static final int GLU_NURBS_ERROR32 = 100282;
//public static final int GLU_NURBS_ERROR33 = 100283;
//public static final int GLU_NURBS_ERROR34 = 100284;
//public static final int GLU_NURBS_ERROR35 = 100285;
//public static final int GLU_NURBS_ERROR36 = 100286;
//public static final int GLU_NURBS_ERROR37 = 100287;

// NurbsProperty
//public static final int GLU_AUTO_LOAD_MATRIX = 100200;
//public static final int GLU_CULLING = 100201;
//public static final int GLU_SAMPLING_TOLERANCE = 100203;
//public static final int GLU_DISPLAY_MODE = 100204;
//public static final int GLU_PARAMETRIC_TOLERANCE = 100202;
//public static final int GLU_SAMPLING_METHOD = 100205;
//public static final int GLU_U_STEP = 100206;
//public static final int GLU_V_STEP = 100207;
//public static final int GLU_NURBS_MODE = 100160;
//public static final int GLU_NURBS_MODE_EXT = 100160;
//public static final int GLU_NURBS_TESSELLATOR = 100161;
//public static final int GLU_NURBS_TESSELLATOR_EXT = 100161;
//public static final int GLU_NURBS_RENDERER = 100162;
//public static final int GLU_NURBS_RENDERER_EXT = 100162;

// NurbsSampling
//public static final int GLU_OBJECT_PARAMETRIC_ERROR = 100208;
//public static final int GLU_OBJECT_PARAMETRIC_ERROR_EXT = 100208;
//public static final int GLU_OBJECT_PATH_LENGTH = 100209;
//public static final int GLU_OBJECT_PATH_LENGTH_EXT = 100209;
//public static final int GLU_PATH_LENGTH = 100215;
//public static final int GLU_PARAMETRIC_ERROR = 100216;
//public static final int GLU_DOMAIN_DISTANCE = 100217;

// NurbsTrim
//public static final int GLU_MAP1_TRIM_2 = 100210;
//public static final int GLU_MAP1_TRIM_3 = 100211;

// QuadricCallback
// GLU_ERROR

// TessCallback
public static final int GLU_TESS_BEGIN = 100100;
public static final int GLU_BEGIN = 100100;
public static final int GLU_TESS_VERTEX = 100101;
public static final int GLU_VERTEX = 100101;
public static final int GLU_TESS_END = 100102;
public static final int GLU_END = 100102;
public static final int GLU_TESS_ERROR = 100103;
public static final int GLU_TESS_EDGE_FLAG = 100104;
public static final int GLU_EDGE_FLAG = 100104;
public static final int GLU_TESS_COMBINE = 100105;
public static final int GLU_TESS_BEGIN_DATA = 100106;
public static final int GLU_TESS_VERTEX_DATA = 100107;
public static final int GLU_TESS_END_DATA = 100108;
public static final int GLU_TESS_ERROR_DATA = 100109;
public static final int GLU_TESS_EDGE_FLAG_DATA = 100110;
public static final int GLU_TESS_COMBINE_DATA = 100111;

// TessContour
public static final int GLU_CW = 100120;
public static final int GLU_CCW = 100121;
public static final int GLU_INTERIOR = 100122;
public static final int GLU_EXTERIOR = 100123;
public static final int GLU_UNKNOWN = 100124;

// TessProperty
public static final int GLU_TESS_WINDING_RULE = 100140;
public static final int GLU_TESS_BOUNDARY_ONLY = 100141;
public static final int GLU_TESS_TOLERANCE = 100142;

// TessError
public static final int GLU_TESS_ERROR1 = 100151;
public static final int GLU_TESS_ERROR2 = 100152;
public static final int GLU_TESS_ERROR3 = 100153;
public static final int GLU_TESS_ERROR4 = 100154;
public static final int GLU_TESS_ERROR5 = 100155;
public static final int GLU_TESS_ERROR6 = 100156;
public static final int GLU_TESS_ERROR7 = 100157;
public static final int GLU_TESS_ERROR8 = 100158;
public static final int GLU_TESS_MISSING_BEGIN_POLYGON = 100151;
public static final int GLU_TESS_MISSING_BEGIN_CONTOUR = 100152;
public static final int GLU_TESS_MISSING_END_POLYGON = 100153;
public static final int GLU_TESS_MISSING_END_CONTOUR = 100154;
public static final int GLU_TESS_COORD_TOO_LARGE = 100155;
public static final int GLU_TESS_NEED_COMBINE_CALLBACK = 100156;

// TessWinding
public static final int GLU_TESS_WINDING_ODD = 100130;
public static final int GLU_TESS_WINDING_NONZERO = 100131;
public static final int GLU_TESS_WINDING_POSITIVE = 100132;
public static final int GLU_TESS_WINDING_NEGATIVE = 100133;
public static final int GLU_TESS_WINDING_ABS_GEQ_TWO = 100134;
public static final double GLU_TESS_MAX_COORD = 1.0e150;

public abstract void gluCylinder(GLUquadric quad, double base, double top, double height, int slices, int stacks) ;

/** Interface to C language function: <br> <code> void gluDeleteQuadric(GLUquadric *  quad); </code>    */
public abstract void gluDeleteQuadric(GLUquadric quad) ;

/** Interface to C language function: <br> <code> void gluDisk(GLUquadric *  quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops); </code>    */
public abstract void gluDisk(GLUquadric quad, double inner, double outer, int slices, int loops) ;

/** Interface to C language function: <br> <code> GLUquadric *  gluNewQuadric(void); </code>    */
public abstract GLUquadric gluNewQuadric() ;

/** Interface to C language function: <br> <code> void gluPartialDisk(GLUquadric *  quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops, GLdouble start, GLdouble sweep); </code>    */
public abstract void gluPartialDisk(GLUquadric quad, double inner, double outer, int slices, int loops, double start, double sweep) ;

/** Interface to C language function: <br> <code> void gluQuadricDrawStyle(GLUquadric *  quad, GLenum draw); </code>    */
public abstract void gluQuadricDrawStyle(GLUquadric quad, int draw) ;

/** Interface to C language function: <br> <code> void gluQuadricNormals(GLUquadric *  quad, GLenum normal); </code>    */
public abstract void gluQuadricNormals(GLUquadric quad, int normal) ;

/** Interface to C language function: <br> <code> void gluQuadricOrientation(GLUquadric *  quad, GLenum orientation); </code>    */
public abstract void gluQuadricOrientation(GLUquadric quad, int orientation) ;

/** Interface to C language function: <br> <code> void gluQuadricTexture(GLUquadric *  quad, GLboolean texture); </code>    */
public abstract void gluQuadricTexture(GLUquadric quad, boolean texture) ;

/** Interface to C language function: <br> <code> void gluSphere(GLUquadric *  quad, GLdouble radius, GLint slices, GLint stacks); </code>    */
public abstract void gluSphere(GLUquadric quad, double radius, int slices, int stacks) ;

public abstract void gluOrtho2D(double left, double right, double bottom, double top) ;

public abstract void gluPerspective(double fovy, double aspect, double zNear, double zFar) ;

public abstract void gluLookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) ;

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single array.
 */
public abstract boolean gluProject(double objX, double objY, double objZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] winPos, int winPos_offset) ;

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single buffer.
 */
public abstract boolean gluProject(double objX, double objY, double objZ, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, DoubleBuffer winPos) ;

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single array.
 */
public abstract boolean gluUnProject(double winX, double winY, double winZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] objPos, int objPos_offset) ;

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single buffer.
 */
public abstract boolean gluUnProject(double winX, double winY, double winZ, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, DoubleBuffer objPos) ;

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single array.
 */
public abstract boolean gluUnProject4(double winX, double winY, double winZ, double clipW, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double nearVal, double farVal, double[] objPos, int objPos_offset) ;

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single buffer.
 */
public abstract boolean gluUnProject4(double winX, double winY, double winZ, double clipW, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, double nearVal, double farVal, DoubleBuffer objPos) ;

public abstract void gluPickMatrix(double x, double y, double delX, double delY, int[] viewport, int viewport_offset) ;

public abstract void gluPickMatrix(double x, double y, double delX, double delY, IntBuffer viewport) ;

public abstract int gluScaleImage( int format, int widthin, int heightin,
                               int typein, java.nio.Buffer datain, int widthout, int heightout,
                               int typeout, java.nio.Buffer dataout ) ;

public abstract int gluBuild1DMipmapLevels( int target, int internalFormat, int width,
                                        int format, int type, int userLevel, int baseLevel, int maxLevel,
                                        java.nio.Buffer data ) ;
public abstract int gluBuild1DMipmaps( int target, int internalFormat, int width,
                                   int format, int type, java.nio.Buffer data ) ;

public abstract int gluBuild2DMipmapLevels( int target, int internalFormat, int width,
                                        int height, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, java.nio.Buffer data ) ;

public abstract int gluBuild2DMipmaps( int target, int internalFormat, int width,
                                   int height, int format, int type, java.nio.Buffer data ) ;

public abstract int gluBuild3DMipmapLevels( int target, int internalFormat, int width,
                                        int height, int depth, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, java.nio.Buffer data) ;

public abstract int gluBuild3DMipmaps( int target, int internalFormat, int width,
                                   int height, int depth, int format, int type, java.nio.Buffer data ) ;

