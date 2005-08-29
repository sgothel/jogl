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

/** Indicates whether the given GLU routine is available to be called. */
public boolean isFunctionAvailable(String gluFunctionName)
{
  return (gluProcAddressTable.getAddressFor(gluFunctionName) != 0);
}

private GLUProcAddressTable gluProcAddressTable;
private GL gl;

public GLUImpl(GLUProcAddressTable gluProcAddressTable)
{
  this.gluProcAddressTable = gluProcAddressTable;
  this.project = new Project();
}

// Used for pure-Java port of GLU
public void setGL(GL gl) {
  this.gl = gl;
}

//----------------------------------------------------------------------
// Tesselator functionality
//

public GLUtesselator gluNewTess() {
    return GLUtesselatorImpl.gluNewTess();
}

public void gluDeleteTess(GLUtesselator tesselator) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluDeleteTess();
}

public void gluTessProperty(GLUtesselator tesselator, int which, double value) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessProperty(which, value);
}

public void gluGetTessProperty(GLUtesselator tesselator, int which, double[] value, int value_offset) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluGetTessProperty(which, value, value_offset);
}

public void gluTessNormal(GLUtesselator tesselator, double x, double y, double z) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessNormal(x, y, z);
}

public void gluTessCallback(GLUtesselator tesselator, int which, GLUtesselatorCallback aCallback) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessCallback(which, aCallback);
}

public void gluTessVertex(GLUtesselator tesselator, double[] coords, int coords_offset, Object data) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessVertex(coords, coords_offset, data);
}

public void gluTessBeginPolygon(GLUtesselator tesselator, Object data) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessBeginPolygon(data);
}

public void gluTessBeginContour(GLUtesselator tesselator) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessBeginContour();
}

public void gluTessEndContour(GLUtesselator tesselator) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessEndContour();
}

public void gluTessEndPolygon(GLUtesselator tesselator) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessEndPolygon();
}

public void gluBeginPolygon(GLUtesselator tesselator) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluBeginPolygon();
}

public void gluNextContour(GLUtesselator tesselator, int type) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluNextContour(type);
}

public void gluEndPolygon(GLUtesselator tesselator) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluEndPolygon();
}

//----------------------------------------------------------------------
// Quadric functionality
//

/** Interface to C language function: <br> <code> void gluCylinder(GLUquadric *  quad, GLdouble base, GLdouble top, GLdouble height, GLint slices, GLint stacks); </code>    */
public void gluCylinder(GLUquadric quad, double base, double top, double height, int slices, int stacks) {
  ((GLUquadricImpl) quad).drawCylinder(gl, (float) base, (float) top, (float) height, slices, stacks);
}

/** Interface to C language function: <br> <code> void gluDeleteQuadric(GLUquadric *  quad); </code>    */
public void gluDeleteQuadric(GLUquadric quad) {
}

/** Interface to C language function: <br> <code> void gluDisk(GLUquadric *  quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops); </code>    */
public void gluDisk(GLUquadric quad, double inner, double outer, int slices, int loops) {
  ((GLUquadricImpl) quad).drawDisk(gl, (float) inner, (float) outer, slices, loops);
}

/** Interface to C language function: <br> <code> GLUquadric *  gluNewQuadric(void); </code>    */
public GLUquadric gluNewQuadric() {
  return new GLUquadricImpl();
}

/** Interface to C language function: <br> <code> void gluPartialDisk(GLUquadric *  quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops, GLdouble start, GLdouble sweep); </code>    */
public void gluPartialDisk(GLUquadric quad, double inner, double outer, int slices, int loops, double start, double sweep) {
  ((GLUquadricImpl) quad).drawPartialDisk(gl, (float) inner, (float) outer, slices, loops, (float) start, (float) sweep);
}

/** Interface to C language function: <br> <code> void gluQuadricDrawStyle(GLUquadric *  quad, GLenum draw); </code>    */
public void gluQuadricDrawStyle(GLUquadric quad, int draw) {
  ((GLUquadricImpl) quad).setDrawStyle(draw);
}

/** Interface to C language function: <br> <code> void gluQuadricNormals(GLUquadric *  quad, GLenum normal); </code>    */
public void gluQuadricNormals(GLUquadric quad, int normal) {
  ((GLUquadricImpl) quad).setNormals(normal);
}

/** Interface to C language function: <br> <code> void gluQuadricOrientation(GLUquadric *  quad, GLenum orientation); </code>    */
public void gluQuadricOrientation(GLUquadric quad, int orientation) {
  ((GLUquadricImpl) quad).setOrientation(orientation);
}

/** Interface to C language function: <br> <code> void gluQuadricTexture(GLUquadric *  quad, GLboolean texture); </code>    */
public void gluQuadricTexture(GLUquadric quad, boolean texture) {
  ((GLUquadricImpl) quad).setTextureFlag(texture);
}

/** Interface to C language function: <br> <code> void gluSphere(GLUquadric *  quad, GLdouble radius, GLint slices, GLint stacks); </code>    */
public void gluSphere(GLUquadric quad, double radius, int slices, int stacks) {
  ((GLUquadricImpl) quad).drawSphere(gl, (float) radius, slices, stacks);
}

//----------------------------------------------------------------------
// Projection functionality
//

private Project project;

public void gluOrtho2D(double left, double right, double bottom, double top) {
  project.gluOrtho2D(gl, left, right, bottom, top);
}

public void gluPerspective(double fovy, double aspect, double zNear, double zFar) {
  project.gluPerspective(gl, fovy, aspect, zNear, zFar);
}

public void gluLookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
  project.gluLookAt(gl, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
}

public boolean gluProject(double objX, double objY, double objZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] winX, int winX_offset, double[] winY, int winY_offset, double[] winZ, int winZ_offset) {
  double[] tmp = new double[3];
  boolean res = project.gluProject(objX, objY, objZ, model, model_offset, proj, proj_offset, view, view_offset, tmp, 0);
  winX[winX_offset] = tmp[0];
  winY[winY_offset] = tmp[1];
  winZ[winZ_offset] = tmp[2];
  return res;
}

public boolean gluProject(double objX, double objY, double objZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] winPos, int winPos_offset) {
  return project.gluProject(objX, objY, objZ, model, model_offset, proj, proj_offset, view, view_offset, winPos, winPos_offset);
}

public boolean gluUnProject(double winX, double winY, double winZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] objX, int objX_offset, double[] objY, int objY_offset, double[] objZ, int objZ_offset) {
  double[] tmp = new double[3];
  boolean res = project.gluUnProject(winX, winY, winZ, model, model_offset, proj, proj_offset, view, view_offset, tmp, 0);
  objX[objX_offset] = tmp[0];
  objY[objY_offset] = tmp[1];
  objZ[objZ_offset] = tmp[2];
  return res;
}

public boolean gluUnProject(double winX, double winY, double winZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] objPos, int objPos_offset) {
  return project.gluUnProject(winX, winY, winZ, model, model_offset, proj, proj_offset, view, view_offset, objPos, objPos_offset);
}

public boolean gluUnProject4(double winX, double winY, double winZ, double clipW, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double nearVal, double farVal, double[] objX, int objX_offset, double[] objY, int objY_offset, double[] objZ, int objZ_offset, double[] objW, int objW_offset) {
  double[] tmp = new double[4];
  boolean res = project.gluUnProject4(winX, winY, winZ, clipW, model, model_offset, proj, proj_offset, 
                    view, view_offset, nearVal, farVal, tmp, 0);
  objX[objX_offset] = tmp[0];
  objY[objY_offset] = tmp[1];
  objZ[objZ_offset] = tmp[2];
  objW[objW_offset] = tmp[3];
  return res;
}

public boolean gluUnProject4(double winX, double winY, double winZ, double clipW, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double nearVal, double farVal, double[] objPos, int objPos_offset) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, model_offset, proj, proj_offset, view, view_offset, nearVal, farVal, objPos, objPos_offset);
}

public void gluPickMatrix(double x, double y, double delX, double delY, int[] viewport, int viewport_offset) {
  project.gluPickMatrix(gl, x, y, delX, delY, viewport, viewport_offset);
}

public void gluPickMatrix(double x, double y, double delX, double delY, IntBuffer viewport) {
  project.gluPickMatrix(gl, x, y, delX, delY, viewport);
}

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

// NurbsDisplay
// GLU_FILL
public static final int GLU_OUTLINE_POLYGON = 100240;
public static final int GLU_OUTLINE_PATCH = 100241;

// NurbsCallback
public static final int GLU_NURBS_ERROR = 100103;
public static final int GLU_ERROR = 100103;
public static final int GLU_NURBS_BEGIN = 100164;
public static final int GLU_NURBS_BEGIN_EXT = 100164;
public static final int GLU_NURBS_VERTEX = 100165;
public static final int GLU_NURBS_VERTEX_EXT = 100165;
public static final int GLU_NURBS_NORMAL = 100166;
public static final int GLU_NURBS_NORMAL_EXT = 100166;
public static final int GLU_NURBS_COLOR = 100167;
public static final int GLU_NURBS_COLOR_EXT = 100167;
public static final int GLU_NURBS_TEXTURE_COORD = 100168;
public static final int GLU_NURBS_TEX_COORD_EXT = 100168;
public static final int GLU_NURBS_END = 100169;
public static final int GLU_NURBS_END_EXT = 100169;
public static final int GLU_NURBS_BEGIN_DATA = 100170;
public static final int GLU_NURBS_BEGIN_DATA_EXT = 100170;
public static final int GLU_NURBS_VERTEX_DATA = 100171;
public static final int GLU_NURBS_VERTEX_DATA_EXT = 100171;
public static final int GLU_NURBS_NORMAL_DATA = 100172;
public static final int GLU_NURBS_NORMAL_DATA_EXT = 100172;
public static final int GLU_NURBS_COLOR_DATA = 100173;
public static final int GLU_NURBS_COLOR_DATA_EXT = 100173;
public static final int GLU_NURBS_TEXTURE_COORD_DATA = 100174;
public static final int GLU_NURBS_TEX_COORD_DATA_EXT = 100174;
public static final int GLU_NURBS_END_DATA = 100175;
public static final int GLU_NURBS_END_DATA_EXT = 100175;

// NurbsError
public static final int GLU_NURBS_ERROR1 = 100251;
public static final int GLU_NURBS_ERROR2 = 100252;
public static final int GLU_NURBS_ERROR3 = 100253;
public static final int GLU_NURBS_ERROR4 = 100254;
public static final int GLU_NURBS_ERROR5 = 100255;
public static final int GLU_NURBS_ERROR6 = 100256;
public static final int GLU_NURBS_ERROR7 = 100257;
public static final int GLU_NURBS_ERROR8 = 100258;
public static final int GLU_NURBS_ERROR9 = 100259;
public static final int GLU_NURBS_ERROR10 = 100260;
public static final int GLU_NURBS_ERROR11 = 100261;
public static final int GLU_NURBS_ERROR12 = 100262;
public static final int GLU_NURBS_ERROR13 = 100263;
public static final int GLU_NURBS_ERROR14 = 100264;
public static final int GLU_NURBS_ERROR15 = 100265;
public static final int GLU_NURBS_ERROR16 = 100266;
public static final int GLU_NURBS_ERROR17 = 100267;
public static final int GLU_NURBS_ERROR18 = 100268;
public static final int GLU_NURBS_ERROR19 = 100269;
public static final int GLU_NURBS_ERROR20 = 100270;
public static final int GLU_NURBS_ERROR21 = 100271;
public static final int GLU_NURBS_ERROR22 = 100272;
public static final int GLU_NURBS_ERROR23 = 100273;
public static final int GLU_NURBS_ERROR24 = 100274;
public static final int GLU_NURBS_ERROR25 = 100275;
public static final int GLU_NURBS_ERROR26 = 100276;
public static final int GLU_NURBS_ERROR27 = 100277;
public static final int GLU_NURBS_ERROR28 = 100278;
public static final int GLU_NURBS_ERROR29 = 100279;
public static final int GLU_NURBS_ERROR30 = 100280;
public static final int GLU_NURBS_ERROR31 = 100281;
public static final int GLU_NURBS_ERROR32 = 100282;
public static final int GLU_NURBS_ERROR33 = 100283;
public static final int GLU_NURBS_ERROR34 = 100284;
public static final int GLU_NURBS_ERROR35 = 100285;
public static final int GLU_NURBS_ERROR36 = 100286;
public static final int GLU_NURBS_ERROR37 = 100287;

// NurbsProperty
public static final int GLU_AUTO_LOAD_MATRIX = 100200;
public static final int GLU_CULLING = 100201;
public static final int GLU_SAMPLING_TOLERANCE = 100203;
public static final int GLU_DISPLAY_MODE = 100204;
public static final int GLU_PARAMETRIC_TOLERANCE = 100202;
public static final int GLU_SAMPLING_METHOD = 100205;
public static final int GLU_U_STEP = 100206;
public static final int GLU_V_STEP = 100207;
public static final int GLU_NURBS_MODE = 100160;
public static final int GLU_NURBS_MODE_EXT = 100160;
public static final int GLU_NURBS_TESSELLATOR = 100161;
public static final int GLU_NURBS_TESSELLATOR_EXT = 100161;
public static final int GLU_NURBS_RENDERER = 100162;
public static final int GLU_NURBS_RENDERER_EXT = 100162;

// NurbsSampling
public static final int GLU_OBJECT_PARAMETRIC_ERROR = 100208;
public static final int GLU_OBJECT_PARAMETRIC_ERROR_EXT = 100208;
public static final int GLU_OBJECT_PATH_LENGTH = 100209;
public static final int GLU_OBJECT_PATH_LENGTH_EXT = 100209;
public static final int GLU_PATH_LENGTH = 100215;
public static final int GLU_PARAMETRIC_ERROR = 100216;
public static final int GLU_DOMAIN_DISTANCE = 100217;

// NurbsTrim
public static final int GLU_MAP1_TRIM_2 = 100210;
public static final int GLU_MAP1_TRIM_3 = 100211;

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
public static final int GLU_TESS_BOUNDRY_ONLY = 100141;
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


public int gluScaleImageJava( int format, int widthin, int heightin,
          int typein, Object datain, int widthout, int heightout,
          int typeout, Object dataout ) {
  ByteBuffer in = null;
  ByteBuffer out = null;
  if( datain instanceof ByteBuffer ) {
    in = (ByteBuffer)datain;
  } else if( datain instanceof byte[] ) {
    in = ByteBuffer.allocateDirect( ((byte[])datain).length ).order( ByteOrder.nativeOrder() );
    in.put((byte[]) datain).rewind();
  } else if( datain instanceof short[] ) {
    in = ByteBuffer.allocateDirect( ((byte[])datain).length * 2 ).order( ByteOrder.nativeOrder() );
    in.asShortBuffer().put((short[]) datain).rewind();
  } else if( datain instanceof int[] ) {
    in = ByteBuffer.allocateDirect( ((byte[])datain).length * 4 ).order( ByteOrder.nativeOrder() );
    in.asIntBuffer().put((int[]) datain).rewind();
  } else if( datain instanceof float[] ) {
    in = ByteBuffer.allocateDirect( ((byte[])datain).length * 4 ).order( ByteOrder.nativeOrder() );
    in.asFloatBuffer().put((float[]) datain).rewind();
  } else {
    throw new IllegalArgumentException( "Input data must be a primitive array or a ByteBuffer" );
  }
  if( dataout instanceof ByteBuffer ) {
    out = (ByteBuffer)dataout;
  } else if( dataout instanceof byte[] ) {
    out = ByteBuffer.wrap( ((byte[])dataout) );
  } else if( dataout instanceof short[] ) {
    out = ByteBuffer.allocate( ((short[])dataout).length * 2 );
  } else if( dataout instanceof int[] ) {
    out = ByteBuffer.allocate( ((int[])dataout).length * 4 );
  } else if( dataout instanceof float[] ) {
    out = ByteBuffer.allocate( ((float[])dataout).length * 4 );
  } else {
    throw new IllegalArgumentException( "Output data must be a primitive array or a ByteBuffer" );
  }
  int errno = Mipmap.gluScaleImage( gl, format, widthin, heightin, typein, in, 
            widthout, heightout, typeout, out );
  if( errno == 0 ) {
    if( dataout instanceof short[] ) {
      out.asShortBuffer().get( (short[])dataout );
    } else if( dataout instanceof int[] ) {
      out.asIntBuffer().get( (int[])dataout );
    } else if( dataout instanceof float[] ) {
      out.asFloatBuffer().get( (float[])dataout );
    }
  }
  return( errno );
}


public int gluBuild1DMipmapLevelsJava( int target, int internalFormat, int width,
          int format, int type, int userLevel, int baseLevel, int maxLevel,
          Object data ) {
  ByteBuffer buffer = null;
  if( data instanceof ByteBuffer ) {
    buffer = (ByteBuffer)data;
  } else if( data instanceof byte[] ) {
    buffer = ByteBuffer.allocateDirect( ((byte[])data).length ).order( ByteOrder.nativeOrder() );
    buffer.put( (byte[])data );
  } else if( data instanceof short[] ) {
    buffer = ByteBuffer.allocateDirect( ((short[])data).length * 2 ).order( ByteOrder.nativeOrder() );
    buffer.asShortBuffer().put( (short[])data );
  } else if( data instanceof int[] ) {
    buffer = ByteBuffer.allocateDirect( ((int[])data).length * 4 ).order( ByteOrder.nativeOrder() );
    buffer.asIntBuffer().put( (int[])data );
  } else if( data instanceof float[] ) {
    buffer = ByteBuffer.allocateDirect( ((float[])data).length * 4 ).order( ByteOrder.nativeOrder() );
    buffer.asFloatBuffer().put( (float[])data );
  } else {
    throw new IllegalArgumentException( "Input data must be a primitive array or a ByteBuffer" );
  }
  return( Mipmap.gluBuild1DMipmapLevels( gl, target, internalFormat, width,
          format, type, userLevel, baseLevel, maxLevel, buffer ) );
}


public int gluBuild1DMipmapsJava( int target, int internalFormat, int width,
          int format, int type, Object data ) {
  ByteBuffer buffer = null;
  if( data instanceof ByteBuffer ) {
    buffer = (ByteBuffer)data;
  } else if( data instanceof byte[] ) {
    buffer = ByteBuffer.allocateDirect( ((byte[])data).length ).order( ByteOrder.nativeOrder() );
    buffer.put( (byte[])data );
  } else if( data instanceof short[] ) {
    buffer = ByteBuffer.allocateDirect( ((short[])data).length * 2 ).order( ByteOrder.nativeOrder() );
    buffer.asShortBuffer().put( (short[])data );
  } else if( data instanceof int[] ) {
    buffer = ByteBuffer.allocateDirect( ((int[])data).length * 4 ).order( ByteOrder.nativeOrder() );
    buffer.asIntBuffer().put( (int[])data );
  } else if( data instanceof float[] ) {
    buffer = ByteBuffer.allocateDirect( ((float[])data).length * 4 ).order( ByteOrder.nativeOrder() );
    buffer.asFloatBuffer().put( (float[])data );
  } else {
    throw new IllegalArgumentException( "Input data must be a primitive array or a ByteBuffer" );
  }
  return( Mipmap.gluBuild1DMipmaps( gl, target, internalFormat, width, format,
          type, buffer ) );
}


public int gluBuild2DMipmapLevelsJava( int target, int internalFormat, int width,
        int height, int format, int type, int userLevel, int baseLevel,
        int maxLevel, Object data ) {
  return( Mipmap.gluBuild2DMipmapLevels( gl, target, internalFormat, width,
          height, format, type, userLevel, baseLevel, maxLevel, data ) );
}

public int gluBuild2DMipmapsJava( int target, int internalFormat, int width,
        int height, int format, int type, Object data ) {
  return( Mipmap.gluBuild2DMipmaps( gl, target, internalFormat, width, height,
          format, type, data) );
}

public int gluBuild3DMipmapLevelsJava( int target, int internalFormat, int width,
        int height, int depth, int format, int type, int userLevel, int baseLevel,
        int maxLevel, Object data) {
  ByteBuffer buffer = null;
  if( data instanceof ByteBuffer ) {
    buffer = (ByteBuffer)data;
  } else if( data instanceof byte[] ) {
    buffer = ByteBuffer.allocateDirect( ((byte[])data).length ).order( ByteOrder.nativeOrder() );
    buffer.put( (byte[])data );
  } else if( data instanceof short[] ) {
    buffer = ByteBuffer.allocateDirect( ((short[])data).length * 2 ).order( ByteOrder.nativeOrder() );
    buffer.asShortBuffer().put( (short[])data );
  } else if( data instanceof int[] ) {
    buffer = ByteBuffer.allocateDirect( ((int[])data).length * 4 ).order( ByteOrder.nativeOrder() );
    buffer.asIntBuffer().put( (int[])data );
  } else if( data instanceof float[] ) {
    buffer = ByteBuffer.allocateDirect( ((float[])data).length * 4 ).order( ByteOrder.nativeOrder() );
    buffer.asFloatBuffer().put( (float[])data );
  } else {
    throw new IllegalArgumentException( "Input data must be a primitive array or a ByteBuffer" );
  }
  return( Mipmap.gluBuild3DMipmapLevels( gl, target, internalFormat, width,
          height, depth, format, type, userLevel, baseLevel, maxLevel, buffer) );
}

public int gluBuild3DMipmapsJava( int target, int internalFormat, int width,
        int height, int depth, int format, int type, Object data ) {
  ByteBuffer buffer = null;
  if( data instanceof ByteBuffer ) {
    buffer = (ByteBuffer)data;
  } else if( data instanceof byte[] ) {
    buffer = ByteBuffer.allocateDirect( ((byte[])data).length ).order( ByteOrder.nativeOrder() );
    buffer.put( (byte[])data );
  } else if( data instanceof short[] ) {
    buffer = ByteBuffer.allocateDirect( ((short[])data).length * 2 ).order( ByteOrder.nativeOrder() );
    buffer.asShortBuffer().put( (short[])data );
  } else if( data instanceof int[] ) {
    buffer = ByteBuffer.allocateDirect( ((int[])data).length * 4 ).order( ByteOrder.nativeOrder() );
    buffer.asIntBuffer().put( (int[])data );
  } else if( data instanceof float[] ) {
    buffer = ByteBuffer.allocateDirect( ((float[])data).length * 4 ).order( ByteOrder.nativeOrder() );
    buffer.asFloatBuffer().put( (float[])data );
  } else {
    throw new IllegalArgumentException( "Input data must be a primitive array or a ByteBuffer" );
  }
  return( Mipmap.gluBuild3DMipmaps( gl, target, internalFormat, width, height,
          depth, format, type, buffer ) );
}


//----------------------------------------------------------------------
// Wrappers for mipmap and image scaling entry points which dispatch either
// to the Java or C versions.
//


/** Interface to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, byte[] data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapLevelsJava(target, internalFormat, width, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, short[] data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapLevelsJava(target, internalFormat, width, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, int[] data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapLevelsJava(target, internalFormat, width, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, float[] data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapLevelsJava(target, internalFormat, width, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/* Todo travis: change 0 to offset for buffer here */
/** Interface to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapLevelsJava(target, internalFormat, width, format, type, level, base, max, data);
  } else {
    return gluBuild1DMipmapLevelsC(target, internalFormat, width, format, type, level, base, max, data);
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, byte[] data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapsJava(target, internalFormat, width, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, short[] data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapsJava(target, internalFormat, width, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, int[] data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapsJava(target, internalFormat, width, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, float[] data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapsJava(target, internalFormat, width, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, byte[] data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapLevelsJava(target, internalFormat, width, height, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, short[] data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapLevelsJava(target, internalFormat, width, height, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, int[] data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapLevelsJava(target, internalFormat, width, height, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, float[] data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapLevelsJava(target, internalFormat, width, height, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapLevelsJava(target, internalFormat, width, height, format, type, level, base, max, data);
  } else {
    return gluBuild2DMipmapLevelsC(target, internalFormat, width, height, format, type, level, base, max, data);
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, byte[] data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapsJava(target, internalFormat, width, height, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, short[] data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapsJava(target, internalFormat, width, height, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, int[] data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapsJava(target, internalFormat, width, height, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, float[] data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapsJava(target, internalFormat, width, height, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapsJava(target, internalFormat, width, height, format, type, data);
  } else {
    return gluBuild2DMipmapsC(target, internalFormat, width, height, format, type, data);
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, byte[] data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapLevelsJava(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, short[] data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapLevelsJava(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, int[] data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapLevelsJava(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, float[] data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapLevelsJava(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapLevelsJava(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  } else {
    return gluBuild3DMipmapLevelsC(target, internalFormat, width, height, depth, format, type, level, base, max, data);
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, byte[] data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapsJava(target, internalFormat, width, height, depth, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, short[] data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapsJava(target, internalFormat, width, height, depth, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, int[] data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapsJava(target, internalFormat, width, height, depth, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, float[] data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapsJava(target, internalFormat, width, height, depth, format, type, data);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapsJava(target, internalFormat, width, height, depth, format, type, data);
  } else {
    return gluBuild3DMipmapsC(target, internalFormat, width, height, depth, format, type, data);
  }
}


/** Interface to C language function: <br> <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
public int gluScaleImage(int format, int wIn, int hIn, int typeIn, byte[] dataIn, int wOut, int hOut, int typeOut, byte[] dataOut) {
  if (useJavaMipmapCode) {
    return gluScaleImageJava(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
public int gluScaleImage(int format, int wIn, int hIn, int typeIn, short[] dataIn, int wOut, int hOut, int typeOut, short[] dataOut) {
  if (useJavaMipmapCode) {
    return gluScaleImageJava(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
public int gluScaleImage(int format, int wIn, int hIn, int typeIn, int[] dataIn, int wOut, int hOut, int typeOut, int[] dataOut) {
  if (useJavaMipmapCode) {
    return gluScaleImageJava(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}


/** Interface to C language function: <br> <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
public int gluScaleImage(int format, int wIn, int hIn, int typeIn, float[] dataIn, int wOut, int hOut, int typeOut, float[] dataOut) {
  if (useJavaMipmapCode) {
    return gluScaleImageJava(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  } else {
    throw new GLException("Primitive array data no longer supported by C GLU implementation");
  }
}

/** Interface to C language function: <br> <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
public int gluScaleImage(int format, int wIn, int hIn, int typeIn, java.nio.Buffer dataIn, int wOut, int hOut, int typeOut, java.nio.Buffer dataOut) {
  if (useJavaMipmapCode) {
    return gluScaleImageJava(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  } else {
    return gluScaleImageC(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut);
  }
}



//----------------------------------------------------------------------
// Wrappers for C entry points for mipmap and scaling functionality.
// (These are only used as a fallback and will be removed in a future release)


  /** Entry point to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
  public int gluBuild1DMipmapLevelsC(int target, int internalFormat, int width, int format, int type, int level, int base, int max, java.nio.Buffer data)
  {
    if (!BufferFactory.isDirect(data))
      throw new GLException("Argument \"data\" was not a direct buffer");
    final long __addr_ = gluProcAddressTable._addressof_gluBuild1DMipmapLevels;
    if (__addr_ == 0) {
      throw new GLException("Method \"gluBuild1DMipmapLevels\" not available");
    }
    return dispatch_gluBuild1DMipmapLevels(target, internalFormat, width, format, type, level, base, max, data, __addr_);
  }

  /** Encapsulates function pointer for OpenGL function <br>: <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
  native private int dispatch_gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, java.nio.Buffer data, long glProcAddress);



  /** Entry point to C language function: <br> <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
  public int gluBuild1DMipmapsC(int target, int internalFormat, int width, int format, int type, java.nio.Buffer data)
  {
    if (!BufferFactory.isDirect(data))
      throw new GLException("Argument \"data\" was not a direct buffer");
    final long __addr_ = gluProcAddressTable._addressof_gluBuild1DMipmaps;
    if (__addr_ == 0) {
      throw new GLException("Method \"gluBuild1DMipmaps\" not available");
    }
    return dispatch_gluBuild1DMipmaps(target, internalFormat, width, format, type, data, __addr_);
  }

  /** Encapsulates function pointer for OpenGL function <br>: <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
  native private int dispatch_gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, java.nio.Buffer data, long glProcAddress);




  /** Entry point to C language function: <br> <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
  public int gluBuild2DMipmapLevelsC(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, java.nio.Buffer data)
  {
    if (!BufferFactory.isDirect(data))
      throw new GLException("Argument \"data\" was not a direct buffer");
    final long __addr_ = gluProcAddressTable._addressof_gluBuild2DMipmapLevels;
    if (__addr_ == 0) {
      throw new GLException("Method \"gluBuild2DMipmapLevels\" not available");
    }
    return dispatch_gluBuild2DMipmapLevels(target, internalFormat, width, height, format, type, level, base, max, data, __addr_);
  }

  /** Encapsulates function pointer for OpenGL function <br>: <code> GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
  native private int dispatch_gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, java.nio.Buffer data, long glProcAddress);




  /** Entry point to C language function: <br> <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
  public int gluBuild2DMipmapsC(int target, int internalFormat, int width, int height, int format, int type, java.nio.Buffer data)
  {
    if (!BufferFactory.isDirect(data))
      throw new GLException("Argument \"data\" was not a direct buffer");
    final long __addr_ = gluProcAddressTable._addressof_gluBuild2DMipmaps;
    if (__addr_ == 0) {
      throw new GLException("Method \"gluBuild2DMipmaps\" not available");
    }
    return dispatch_gluBuild2DMipmaps(target, internalFormat, width, height, format, type, data, __addr_);
  }

  /** Encapsulates function pointer for OpenGL function <br>: <code> GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data); </code>    */
  native private int dispatch_gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, java.nio.Buffer data, long glProcAddress);


  /** Entry point to C language function: <br> <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
  public int gluBuild3DMipmapLevelsC(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, java.nio.Buffer data)
  {
    if (!BufferFactory.isDirect(data))
      throw new GLException("Argument \"data\" was not a direct buffer");
    final long __addr_ = gluProcAddressTable._addressof_gluBuild3DMipmapLevels;
    if (__addr_ == 0) {
      throw new GLException("Method \"gluBuild3DMipmapLevels\" not available");
    }
    return dispatch_gluBuild3DMipmapLevels(target, internalFormat, width, height, depth, format, type, level, base, max, data, __addr_);
  }

  /** Encapsulates function pointer for OpenGL function <br>: <code> GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
  native private int dispatch_gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, java.nio.Buffer data, long glProcAddress);






  /** Entry point to C language function: <br> <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
  public int gluBuild3DMipmapsC(int target, int internalFormat, int width, int height, int depth, int format, int type, java.nio.Buffer data)
  {
    if (!BufferFactory.isDirect(data))
      throw new GLException("Argument \"data\" was not a direct buffer");
    final long __addr_ = gluProcAddressTable._addressof_gluBuild3DMipmaps;
    if (__addr_ == 0) {
      throw new GLException("Method \"gluBuild3DMipmaps\" not available");
    }
    return dispatch_gluBuild3DMipmaps(target, internalFormat, width, height, depth, format, type, data, __addr_);
  }

  /** Encapsulates function pointer for OpenGL function <br>: <code> GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data); </code>    */
  native private int dispatch_gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, java.nio.Buffer data, long glProcAddress);




  /** Entry point to C language function: <br> <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
  public int gluScaleImageC(int format, int wIn, int hIn, int typeIn, java.nio.Buffer dataIn, int wOut, int hOut, int typeOut, java.nio.Buffer dataOut)
  {
    if (!BufferFactory.isDirect(dataIn))
      throw new GLException("Argument \"dataIn\" was not a direct buffer");
    if (!BufferFactory.isDirect(dataOut))
      throw new GLException("Argument \"dataOut\" was not a direct buffer");
    final long __addr_ = gluProcAddressTable._addressof_gluScaleImage;
    if (__addr_ == 0) {
      throw new GLException("Method \"gluScaleImage\" not available");
    }
    return dispatch_gluScaleImage(format, wIn, hIn, typeIn, dataIn, wOut, hOut, typeOut, dataOut, __addr_);
  }

  /** Encapsulates function pointer for OpenGL function <br>: <code> GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut); </code>    */
  native private int dispatch_gluScaleImage(int format, int wIn, int hIn, int typeIn, java.nio.Buffer dataIn, int wOut, int hOut, int typeOut, java.nio.Buffer dataOut, long glProcAddress);



