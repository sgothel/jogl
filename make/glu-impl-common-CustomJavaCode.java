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

public void gluGetTessProperty(GLUtesselator tesselator, int which, double[] value) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluGetTessProperty(which, value);
}

public void gluTessNormal(GLUtesselator tesselator, double x, double y, double z) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessNormal(x, y, z);
}

public void gluTessCallback(GLUtesselator tesselator, int which, GLUtesselatorCallback aCallback) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessCallback(which, aCallback);
}

public void gluTessVertex(GLUtesselator tesselator, double[] coords, Object data) {
    GLUtesselatorImpl tess = (GLUtesselatorImpl) tesselator;
    tess.gluTessVertex(coords, data);
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

public boolean gluProject(double objX, double objY, double objZ, double[] model, double[] proj, int[] view, double[] winX, double[] winY, double[] winZ) {
  double[] tmp = new double[3];
  boolean res = project.gluProject(objX, objY, objZ, model, proj, view, tmp);
  winX[0] = tmp[0];
  winY[0] = tmp[1];
  winZ[0] = tmp[2];
  return res;
}

public boolean gluProject(double objX, double objY, double objZ, double[] model, double[] proj, int[] view, double[] winPos) {
  return project.gluProject(objX, objY, objZ, model, proj, view, winPos);
}

public boolean gluUnProject(double winX, double winY, double winZ, double[] model, double[] proj, int[] view, double[] objX, double[] objY, double[] objZ) {
  double[] tmp = new double[3];
  boolean res = project.gluUnProject(winX, winY, winZ, model, proj, view, tmp);
  objX[0] = tmp[0];
  objY[0] = tmp[1];
  objZ[0] = tmp[2];
  return res;
}

public boolean gluUnProject(double winX, double winY, double winZ, double[] model, double[] proj, int[] view, double[] objPos) {
  return project.gluUnProject(winX, winY, winZ, model, proj, view, objPos);
}

public boolean gluUnProject4(double winX, double winY, double winZ, double clipW, double[] model, double[] proj, int[] view, double nearVal, double farVal, double[] objX, double[] objY, double[] objZ, double[] objW) {
  double[] tmp = new double[4];
  boolean res = project.gluUnProject4(winX, winY, winZ, clipW, model, proj, view, nearVal, farVal, tmp);
  objX[0] = tmp[0];
  objY[0] = tmp[1];
  objZ[0] = tmp[2];
  objW[0] = tmp[3];
  return res;
}

public boolean gluUnProject4(double winX, double winY, double winZ, double clipW, double[] model, double[] proj, int[] view, double nearVal, double farVal, double[] objPos) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, proj, view, nearVal, farVal, objPos);
}

public void gluPickMatrix(double x, double y, double delX, double delY, int[] viewport) {
  project.gluPickMatrix(gl, x, y, delX, delY, viewport);
}
