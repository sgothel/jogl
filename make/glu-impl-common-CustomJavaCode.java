/** Indicates whether the given GLU routine is available to be called. */
public boolean isFunctionAvailable(String gluFunctionName)
{
  return (gluProcAddressTable.getAddressFor(gluFunctionName) != 0);
}

private GLUProcAddressTable gluProcAddressTable;

public GLUImpl(GLUProcAddressTable gluProcAddressTable)
{
  this.gluProcAddressTable = gluProcAddressTable;
}

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
