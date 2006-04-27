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

public GLU()
{
  this.project = new Project();
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

public String gluErrorString(int errorCode) {
  return Error.gluErrorString(errorCode);
}

/* extName is an extension name.
 * extString is a string of extensions separated by blank(s). There may or 
 * may not be leading or trailing blank(s) in extString.
 * This works in cases of extensions being prefixes of another like
 * GL_EXT_texture and GL_EXT_texture3D.
 * Returns true if extName is found otherwise it returns false.
 */
public boolean gluCheckExtension(java.lang.String extName, java.lang.String extString) {
  return Registry.gluCheckExtension(extName, extString);
}

public String gluGetString(int name) {
  return Registry.gluGetString(name);
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
public boolean isFunctionAvailable(String gluFunctionName)
{
  if (useJavaMipmapCode) {
    // All GLU functions are available in Java port
    return true;
  }
  return (gluProcAddressTable.getAddressFor(gluFunctionName) != 0);
}

//----------------------------------------------------------------------
// Tessellation routines
//

/*****************************************************************************
 * <b>gluNewTess</b> creates and returns a new tessellation object.  This
 * object must be referred to when calling tesselation methods.  A return
 * value of null means that there was not enough memeory to allocate the
 * object.
 *
 * @return A new tessellation object.
 *
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluDeleteTess       gluDeleteTess
 * @see #gluTessCallback     gluTessCallback
 ****************************************************************************/
public GLUtessellator gluNewTess() {
    return GLUtessellatorImpl.gluNewTess();
}

/*****************************************************************************
 * <b>gluDeleteTess</b> destroys the indicated tessellation object (which was
 * created with {@link #gluNewTess gluNewTess}).
 *
 * @param tessellator
 *        Specifies the tessellation object to destroy.
 *
 * @see #gluBeginPolygon gluBeginPolygon
 * @see #gluNewTess      gluNewTess
 * @see #gluTessCallback gluTessCallback
 ****************************************************************************/
public void gluDeleteTess(GLUtessellator tessellator) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluDeleteTess();
}

/*****************************************************************************
 * <b>gluTessProperty</b> is used to control properites stored in a
 * tessellation object.  These properties affect the way that the polygons are
 * interpreted and rendered.  The legal value for <i>which</i> are as
 * follows:<P>
 *
 * <b>GLU_TESS_WINDING_RULE</b>
 * <UL>
 *   Determines which parts of the polygon are on the "interior".
 *   <em>value</em> may be set to one of
 *   <BR><b>GLU_TESS_WINDING_ODD</b>,
 *   <BR><b>GLU_TESS_WINDING_NONZERO</b>,
 *   <BR><b>GLU_TESS_WINDING_POSITIVE</b>, or
 *   <BR><b>GLU_TESS_WINDING_NEGATIVE</b>, or
 *   <BR><b>GLU_TESS_WINDING_ABS_GEQ_TWO</b>.<P>
 *
 *   To understand how the winding rule works, consider that the input
 *   contours partition the plane into regions.  The winding rule determines
 *   which of these regions are inside the polygon.<P>
 *
 *   For a single contour C, the winding number of a point x is simply the
 *   signed number of revolutions we make around x as we travel once around C
 *   (where CCW is positive).  When there are several contours, the individual
 *   winding numbers are summed.  This procedure associates a signed integer
 *   value with each point x in the plane.  Note that the winding number is
 *   the same for all points in a single region.<P>
 *
 *   The winding rule classifies a region as "inside" if its winding number
 *   belongs to the chosen category (odd, nonzero, positive, negative, or
 *   absolute value of at least two).  The previous GLU tessellator (prior to
 *   GLU 1.2) used the "odd" rule.  The "nonzero" rule is another common way
 *   to define the interior.  The other three rules are useful for polygon CSG
 *   operations.
 * </UL>
 * <BR><b>GLU_TESS_BOUNDARY_ONLY</b>
 * <UL>
 *   Is a boolean value ("value" should be set to GL_TRUE or GL_FALSE). When
 *   set to GL_TRUE, a set of closed contours separating the polygon interior
 *   and exterior are returned instead of a tessellation.  Exterior contours
 *   are oriented CCW with respect to the normal; interior contours are
 *   oriented CW. The <b>GLU_TESS_BEGIN</b> and <b>GLU_TESS_BEGIN_DATA</b>
 *   callbacks use the type GL_LINE_LOOP for each contour.
 * </UL>
 * <BR><b>GLU_TESS_TOLERANCE</b>
 * <UL>
 *   Specifies a tolerance for merging features to reduce the size of the
 *   output. For example, two vertices that are very close to each other
 *   might be replaced by a single vertex.  The tolerance is multiplied by the
 *   largest coordinate magnitude of any input vertex; this specifies the
 *   maximum distance that any feature can move as the result of a single
 *   merge operation.  If a single feature takes part in several merge
 *   operations, the toal distance moved could be larger.<P>
 *
 *   Feature merging is completely optional; the tolerance is only a hint.
 *   The implementation is free to merge in some cases and not in others, or
 *   to never merge features at all.  The initial tolerance is 0.<P>
 *
 *   The current implementation merges vertices only if they are exactly
 *   coincident, regardless of the current tolerance.  A vertex is spliced
 *   into an edge only if the implementation is unable to distinguish which
 *   side of the edge the vertex lies on.  Two edges are merged only when both
 *   endpoints are identical.
 * </UL>
 *
 * @param tessellator
 *        Specifies the tessellation object created with
 *        {@link #gluNewTess gluNewTess}
 * @param which
 *        Specifies the property to be set.  Valid values are
 *        <b>GLU_TESS_WINDING_RULE</b>, <b>GLU_TESS_BOUNDARDY_ONLY</b>,
 *        <b>GLU_TESS_TOLERANCE</b>.
 * @param value
 *        Specifices the value of the indicated property.
 *
 * @see #gluGetTessProperty gluGetTessProperty
 * @see #gluNewTess         gluNewTess
 ****************************************************************************/
public void gluTessProperty(GLUtessellator tessellator, int which, double value) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluTessProperty(which, value);
}

/*****************************************************************************
 * <b>gluGetTessProperty</b> retrieves properties stored in a tessellation
 * object.  These properties affect the way that tessellation objects are
 * interpreted and rendered.  See the
 * {@link #gluTessProperty gluTessProperty} reference
 * page for information about the properties and what they do.
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 * @param which
 *        Specifies the property whose value is to be fetched. Valid values
 *        are <b>GLU_TESS_WINDING_RULE</b>, <b>GLU_TESS_BOUNDARY_ONLY</b>,
 *        and <b>GLU_TESS_TOLERANCES</b>.
 * @param value
 *        Specifices an array into which the value of the named property is
 *        written.
 *
 * @see #gluNewTess      gluNewTess
 * @see #gluTessProperty gluTessProperty
 ****************************************************************************/
public void gluGetTessProperty(GLUtessellator tessellator, int which, double[] value, int value_offset) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluGetTessProperty(which, value, value_offset);
}

/*****************************************************************************
 * <b>gluTessNormal</b> describes a normal for a polygon that the program is
 * defining. All input data will be projected onto a plane perpendicular to
 * the one of the three coordinate axes before tessellation and all output
 * triangles will be oriented CCW with repsect to the normal (CW orientation
 * can be obtained by reversing the sign of the supplied normal).  For
 * example, if you know that all polygons lie in the x-y plane, call
 * <b>gluTessNormal</b>(tess, 0.0, 0.0, 0.0) before rendering any polygons.<P>
 *
 * If the supplied normal is (0.0, 0.0, 0.0)(the initial value), the normal
 * is determined as follows.  The direction of the normal, up to its sign, is
 * found by fitting a plane to the vertices, without regard to how the
 * vertices are connected.  It is expected that the input data lies
 * approximately in the plane; otherwise, projection perpendicular to one of
 * the three coordinate axes may substantially change the geometry.  The sign
 * of the normal is chosen so that the sum of the signed areas of all input
 * contours is nonnegative (where a CCW contour has positive area).<P>
 *
 * The supplied normal persists until it is changed by another call to
 * <b>gluTessNormal</b>.
 *
 * @param tessellator
 *        Specifies the tessellation object (created by
 *        {@link #gluNewTess gluNewTess}).
 * @param x
 *        Specifies the first component of the normal.
 * @param y
 *        Specifies the second component of the normal.
 * @param z
 *        Specifies the third component of the normal.
 *
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessNormal(GLUtessellator tessellator, double x, double y, double z) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluTessNormal(x, y, z);
}

/*****************************************************************************
 * <b>gluTessCallback</b> is used to indicate a callback to be used by a
 * tessellation object. If the specified callback is already defined, then it
 * is replaced. If <i>aCallback</i> is null, then the existing callback
 * becomes undefined.<P>
 *
 * These callbacks are used by the tessellation object to describe how a
 * polygon specified by the user is broken into triangles. Note that there are
 * two versions of each callback: one with user-specified polygon data and one
 * without. If both versions of a particular callback are specified, then the
 * callback with user-specified polygon data will be used. Note that the
 * polygonData parameter used by some of the methods is a copy of the
 * reference that was specified when
 * {@link #gluTessBeginPolygon gluTessBeginPolygon}
 * was called. The legal callbacks are as follows:<P>
 *
 * <b>GLU_TESS_BEGIN</b>
 * <UL>
 *   The begin callback is invoked like {@link javax.media.opengl.GL#glBegin
 *   glBegin} to indicate the start of a (triangle) primitive. The method
 *   takes a single argument of type int. If the
 *   <b>GLU_TESS_BOUNDARY_ONLY</b> property is set to <b>GL_FALSE</b>, then
 *   the argument is set to either <b>GL_TRIANGLE_FAN</b>,
 *   <b>GL_TRIANGLE_STRIP</b>, or <b>GL_TRIANGLES</b>. If the
 *   <b>GLU_TESS_BOUNDARY_ONLY</b> property is set to <b>GL_TRUE</b>, then the
 *   argument will be set to <b>GL_LINE_LOOP</b>. The method prototype for
 *   this callback is:
 * </UL>
 *
 * <PRE>
 *         void begin(int type);</PRE><P>
 *
 * <b>GLU_TESS_BEGIN_DATA</b>
 * <UL>
 *   The same as the <b>GLU_TESS_BEGIN</b> callback except
 *   that it takes an additional reference argument. This reference is
 *   identical to the opaque reference provided when
 *   {@link #gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void beginData(int type, Object polygonData);</PRE>
 *
 * <b>GLU_TESS_EDGE_FLAG</b>
 * <UL>
 *   The edge flag callback is similar to
 *   {@link javax.media.opengl.GL#glEdgeFlag glEdgeFlag}. The method takes
 *   a single boolean boundaryEdge that indicates which edges lie on the
 *   polygon boundary. If the boundaryEdge is <b>GL_TRUE</b>, then each vertex
 *   that follows begins an edge that lies on the polygon boundary, that is,
 *   an edge that separates an interior region from an exterior one. If the
 *   boundaryEdge is <b>GL_FALSE</b>, then each vertex that follows begins an
 *   edge that lies in the polygon interior. The edge flag callback (if
 *   defined) is invoked before the first vertex callback.<P>
 *
 *   Since triangle fans and triangle strips do not support edge flags, the
 *   begin callback is not called with <b>GL_TRIANGLE_FAN</b> or
 *   <b>GL_TRIANGLE_STRIP</b> if a non-null edge flag callback is provided.
 *   (If the callback is initialized to null, there is no impact on
 *   performance). Instead, the fans and strips are converted to independent
 *   triangles. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void edgeFlag(boolean boundaryEdge);</PRE>
 *
 * <b>GLU_TESS_EDGE_FLAG_DATA</b>
 * <UL>
 *   The same as the <b>GLU_TESS_EDGE_FLAG</b> callback except that it takes
 *   an additional reference argument. This reference is identical to the
 *   opaque reference provided when
 *   {@link #gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void edgeFlagData(boolean boundaryEdge, Object polygonData);</PRE>
 *
 * <b>GLU_TESS_VERTEX</b>
 * <UL>
 *   The vertex callback is invoked between the begin and end callbacks. It is
 *   similar to {@link javax.media.opengl.GL#glVertex3f glVertex3f}, and it
 *   defines the vertices of the triangles created by the tessellation
 *   process. The method takes a reference as its only argument. This
 *   reference is identical to the opaque reference provided by the user when
 *   the vertex was described (see
 *   {@link #gluTessVertex gluTessVertex}). The method
 *   prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void vertex(Object vertexData);</PRE>
 *
 * <b>GLU_TESS_VERTEX_DATA</b>
 * <UL>
 *   The same as the <b>GLU_TESS_VERTEX</b> callback except that it takes an
 *   additional reference argument. This reference is identical to the opaque
 *   reference provided when
 *   {@link #gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void vertexData(Object vertexData, Object polygonData);</PRE>
 *
 * <b>GLU_TESS_END</b>
 * <UL>
 *   The end callback serves the same purpose as
 *   {@link javax.media.opengl.GL#glEnd glEnd}. It indicates the end of a
 *   primitive and it takes no arguments. The method prototype for this
 *   callback is:
 * </UL>
 *
 * <PRE>
 *         void end();</PRE>
 *
 * <b>GLU_TESS_END_DATA</b>
 * <UL>
 *   The same as the <b>GLU_TESS_END</b> callback except that it takes an
 *   additional reference argument. This reference is identical to the opaque
 *   reference provided when
 *   {@link #gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void endData(Object polygonData);</PRE>
 *
 * <b>GLU_TESS_COMBINE</b>
 * <UL>
 *   The combine callback is called to create a new vertex when the
 *   tessellation detects an intersection, or wishes to merge features. The
 *   method takes four arguments: an array of three elements each of type
 *   double, an array of four references, an array of four elements each of
 *   type float, and a reference to a reference. The prototype is:
 * </UL>
 *
 * <PRE>
 *         void combine(double[] coords, Object[] data,
 *                      float[] weight, Object[] outData);</PRE>
 *
 * <UL>
 *   The vertex is defined as a linear combination of up to four existing
 *   vertices, stored in <i>data</i>. The coefficients of the linear
 *   combination are given by <i>weight</i>; these weights always add up to 1.
 *   All vertex pointers are valid even when some of the weights are 0.
 *   <i>coords</i> gives the location of the new vertex.<P>
 *
 *   The user must allocate another vertex, interpolate parameters using
 *   <i>data</i> and <i>weight</i>, and return the new vertex pointer
 *   in <i>outData</i>. This handle is supplied during rendering callbacks.
 *   The user is responsible for freeing the memory some time after
 *   {@link #gluTessEndPolygon gluTessEndPolygon} is
 *   called.<P>
 *
 *   For example, if the polygon lies in an arbitrary plane in 3-space, and a
 *   color is associated with each vertex, the <b>GLU_TESS_COMBINE</b>
 *   callback might look like this:
 * </UL>
 * <PRE>
 *         void myCombine(double[] coords, Object[] data,
 *                        float[] weight, Object[] outData)
 *         {
 *            MyVertex newVertex = new MyVertex();
 *
 *            newVertex.x = coords[0];
 *            newVertex.y = coords[1];
 *            newVertex.z = coords[2];
 *            newVertex.r = weight[0]*data[0].r +
 *                          weight[1]*data[1].r +
 *                          weight[2]*data[2].r +
 *                          weight[3]*data[3].r;
 *            newVertex.g = weight[0]*data[0].g +
 *                          weight[1]*data[1].g +
 *                          weight[2]*data[2].g +
 *                          weight[3]*data[3].g;
 *            newVertex.b = weight[0]*data[0].b +
 *                          weight[1]*data[1].b +
 *                          weight[2]*data[2].b +
 *                          weight[3]*data[3].b;
 *            newVertex.a = weight[0]*data[0].a +
 *                          weight[1]*data[1].a +
 *                          weight[2]*data[2].a +
 *                          weight[3]*data[3].a;
 *            outData = newVertex;
 *         }</PRE>
 *
 * <UL>
 *   If the tessellation detects an intersection, then the
 *   <b>GLU_TESS_COMBINE</b> or <b>GLU_TESS_COMBINE_DATA</b> callback (see
 *   below) must be defined, and it must write a non-null reference into
 *   <i>outData</i>. Otherwise the <b>GLU_TESS_NEED_COMBINE_CALLBACK</b> error
 *   occurs, and no output is generated.
 * </UL>
 *
 * <b>GLU_TESS_COMBINE_DATA</b>
 * <UL>
 *   The same as the <b>GLU_TESS_COMBINE</b> callback except that it takes an
 *   additional reference argument. This reference is identical to the opaque
 *   reference provided when
 *   {@link #gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void combineData(double[] coords, Object[] data,
                            float[] weight, Object[] outData,
                            Object polygonData);</PRE>
 *
 * <b>GLU_TESS_ERROR</b>
 * <UL>
 *   The error callback is called when an error is encountered. The one
 *   argument is of type int; it indicates the specific error that occurred
 *   and will be set to one of <b>GLU_TESS_MISSING_BEGIN_POLYGON</b>,
 *   <b>GLU_TESS_MISSING_END_POLYGON</b>,
 *   <b>GLU_TESS_MISSING_BEGIN_CONTOUR</b>,
 *   <b>GLU_TESS_MISSING_END_CONTOUR</b>, <b>GLU_TESS_COORD_TOO_LARGE</b>,
 *   <b>GLU_TESS_NEED_COMBINE_CALLBACK</b> or <b>GLU_OUT_OF_MEMORY</b>.
 *   Character strings describing these errors can be retrieved with the
 *   {@link #gluErrorString gluErrorString} call. The
 *   method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void error(int errnum);</PRE>
 *
 * <UL>
 *   The GLU library will recover from the first four errors by inserting the
 *   missing call(s). <b>GLU_TESS_COORD_TOO_LARGE</b> indicates that some
 *   vertex coordinate exceeded the predefined constant
 *   <b>GLU_TESS_MAX_COORD</b> in absolute value, and that the value has been
 *   clamped. (Coordinate values must be small enough so that two can be
 *   multiplied together without overflow.)
 *   <b>GLU_TESS_NEED_COMBINE_CALLBACK</b> indicates that the tessellation
 *   detected an intersection between two edges in the input data, and the
 *   <b>GLU_TESS_COMBINE</b> or <b>GLU_TESS_COMBINE_DATA</b> callback was not
 *   provided. No output is generated. <b>GLU_OUT_OF_MEMORY</b> indicates that
 *   there is not enough memory so no output is generated.
 * </UL>
 *
 * <b>GLU_TESS_ERROR_DATA</b>
 * <UL>
 *   The same as the GLU_TESS_ERROR callback except that it takes an
 *   additional reference argument. This reference is identical to the opaque
 *   reference provided when
 *   {@link #gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void errorData(int errnum, Object polygonData);</PRE>
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 * @param which
 *        Specifies the callback being defined. The following values are
 *        valid: <b>GLU_TESS_BEGIN</b>, <b>GLU_TESS_BEGIN_DATA</b>,
 *        <b>GLU_TESS_EDGE_FLAG</b>, <b>GLU_TESS_EDGE_FLAG_DATA</b>,
 *        <b>GLU_TESS_VERTEX</b>, <b>GLU_TESS_VERTEX_DATA</b>,
 *        <b>GLU_TESS_END</b>, <b>GLU_TESS_END_DATA</b>,
 *        <b>GLU_TESS_COMBINE</b>,  <b>GLU_TESS_COMBINE_DATA</b>,
 *        <b>GLU_TESS_ERROR</b>, and <b>GLU_TESS_ERROR_DATA</b>.
 * @param aCallback
 *        Specifies the callback object to be called.
 *
 * @see javax.media.opengl.GL#glBegin              glBegin
 * @see javax.media.opengl.GL#glEdgeFlag           glEdgeFlag
 * @see javax.media.opengl.GL#glVertex3f           glVertex3f
 * @see #gluNewTess          gluNewTess
 * @see #gluErrorString      gluErrorString
 * @see #gluTessVertex       gluTessVertex
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluTessBeginContour gluTessBeginContour
 * @see #gluTessProperty     gluTessProperty
 * @see #gluTessNormal       gluTessNormal
 ****************************************************************************/
public void gluTessCallback(GLUtessellator tessellator, int which, GLUtessellatorCallback aCallback) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluTessCallback(which, aCallback);
}

/*****************************************************************************
 * <b>gluTessVertex</b> describes a vertex on a polygon that the program
 * defines. Successive <b>gluTessVertex</b> calls describe a closed contour.
 * For example, to describe a quadrilateral <b>gluTessVertex</b> should be
 * called four times. <b>gluTessVertex</b> can only be called between
 * {@link #gluTessBeginContour gluTessBeginContour} and
 * {@link #gluTessBeginContour gluTessEndContour}.<P>
 *
 * <b>data</b> normally references to a structure containing the vertex
 * location, as well as other per-vertex attributes such as color and normal.
 * This reference is passed back to the user through the
 * <b>GLU_TESS_VERTEX</b> or <b>GLU_TESS_VERTEX_DATA</b> callback after
 * tessellation (see the {@link #gluTessCallback
 * gluTessCallback} reference page).
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 * @param coords
 *        Specifies the coordinates of the vertex.
 * @param data
 *        Specifies an opaque reference passed back to the program with the
 *        vertex callback (as specified by
 *        {@link #gluTessCallback gluTessCallback}).
 *
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluNewTess          gluNewTess
 * @see #gluTessBeginContour gluTessBeginContour
 * @see #gluTessCallback     gluTessCallback
 * @see #gluTessProperty     gluTessProperty
 * @see #gluTessNormal       gluTessNormal
 * @see #gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessVertex(GLUtessellator tessellator, double[] coords, int coords_offset, Object data) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluTessVertex(coords, coords_offset, data);
}

/*****************************************************************************
 * <b>gluTessBeginPolygon</b> and
 * {@link #gluTessEndPolygon gluTessEndPolygon} delimit
 * the definition of a convex, concave or self-intersecting polygon. Within
 * each <b>gluTessBeginPolygon</b>/
 * {@link #gluTessEndPolygon gluTessEndPolygon} pair,
 * there must be one or more calls to
 * {@link #gluTessBeginContour gluTessBeginContour}/
 * {@link #gluTessEndContour gluTessEndContour}. Within
 * each contour, there are zero or more calls to
 * {@link #gluTessVertex gluTessVertex}. The vertices
 * specify a closed contour (the last vertex of each contour is automatically
 * linked to the first). See the {@link #gluTessVertex
 * gluTessVertex}, {@link #gluTessBeginContour
 * gluTessBeginContour}, and {@link #gluTessEndContour
 * gluTessEndContour} reference pages for more details.<P>
 *
 * <b>data</b> is a reference to a user-defined data structure. If the
 * appropriate callback(s) are specified (see
 * {@link #gluTessCallback gluTessCallback}), then this
 * reference is returned to the callback method(s). Thus, it is a convenient
 * way to store per-polygon information.<P>
 *
 * Once {@link #gluTessEndPolygon gluTessEndPolygon} is
 * called, the polygon is tessellated, and the resulting triangles are
 * described through callbacks. See
 * {@link #gluTessCallback gluTessCallback} for
 * descriptions of the callback methods.
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 * @param data
 *        Specifies a reference to user polygon data.
 *
 * @see #gluNewTess          gluNewTess
 * @see #gluTessBeginContour gluTessBeginContour
 * @see #gluTessVertex       gluTessVertex
 * @see #gluTessCallback     gluTessCallback
 * @see #gluTessProperty     gluTessProperty
 * @see #gluTessNormal       gluTessNormal
 * @see #gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessBeginPolygon(GLUtessellator tessellator, Object data) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluTessBeginPolygon(data);
}

/*****************************************************************************
 * <b>gluTessBeginContour</b> and
 * {@link #gluTessEndContour gluTessEndContour} delimit
 * the definition of a polygon contour. Within each
 * <b>gluTessBeginContour</b>/
 * {@link #gluTessEndContour gluTessEndContour} pair,
 * there can be zero or more calls to
 * {@link #gluTessVertex gluTessVertex}. The vertices
 * specify a closed contour (the last vertex of each contour is automatically
 * linked to the first). See the {@link #gluTessVertex
 * gluTessVertex} reference page for more details. <b>gluTessBeginContour</b>
 * can only be called between
 * {@link #gluTessBeginPolygon gluTessBeginPolygon} and
 * {@link #gluTessEndPolygon gluTessEndPolygon}.
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 *
 * @see #gluNewTess          gluNewTess
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluTessVertex       gluTessVertex
 * @see #gluTessCallback     gluTessCallback
 * @see #gluTessProperty     gluTessProperty
 * @see #gluTessNormal       gluTessNormal
 * @see #gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessBeginContour(GLUtessellator tessellator) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluTessBeginContour();
}

/*****************************************************************************
 *  <b>gluTessEndContour</b> and
 * {@link #gluTessBeginContour gluTessBeginContour}
 * delimit the definition of a polygon contour. Within each
 * {@link #gluTessBeginContour gluTessBeginContour}/
 * <b>gluTessEndContour</b> pair, there can be zero or more calls to
 * {@link #gluTessVertex gluTessVertex}. The vertices
 * specify a closed contour (the last vertex of each contour is automatically
 * linked to the first). See the {@link #gluTessVertex
 * gluTessVertex} reference page for more details.
 * {@link #gluTessBeginContour gluTessBeginContour} can
 * only be called between {@link #gluTessBeginPolygon
 * gluTessBeginPolygon} and
 * {@link #gluTessEndPolygon gluTessEndPolygon}.
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 *
 * @see #gluNewTess          gluNewTess
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluTessVertex       gluTessVertex
 * @see #gluTessCallback     gluTessCallback
 * @see #gluTessProperty     gluTessProperty
 * @see #gluTessNormal       gluTessNormal
 * @see #gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessEndContour(GLUtessellator tessellator) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluTessEndContour();
}

/*****************************************************************************
 * <b>gluTessEndPolygon</b> and
 * {@link #gluTessBeginPolygon gluTessBeginPolygon}
 * delimit the definition of a convex, concave or self-intersecting polygon.
 * Within each {@link #gluTessBeginPolygon
 * gluTessBeginPolygon}/<b>gluTessEndPolygon</b> pair, there must be one or
 * more calls to {@link #gluTessBeginContour
 * gluTessBeginContour}/{@link #gluTessEndContour
 * gluTessEndContour}. Within each contour, there are zero or more calls to
 * {@link #gluTessVertex gluTessVertex}. The vertices
 * specify a closed contour (the last vertex of each contour is automatically
 * linked to the first). See the {@link #gluTessVertex
 * gluTessVertex}, {@link #gluTessBeginContour
 * gluTessBeginContour} and {@link #gluTessEndContour
 * gluTessEndContour} reference pages for more details.<P>
 *
 * Once <b>gluTessEndPolygon</b> is called, the polygon is tessellated, and
 * the resulting triangles are described through callbacks. See
 * {@link #gluTessCallback gluTessCallback} for
 * descriptions of the callback functions.
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 *
 * @see #gluNewTess          gluNewTess
 * @see #gluTessBeginContour gluTessBeginContour
 * @see #gluTessVertex       gluTessVertex
 * @see #gluTessCallback     gluTessCallback
 * @see #gluTessProperty     gluTessProperty
 * @see #gluTessNormal       gluTessNormal
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 ****************************************************************************/
public void gluTessEndPolygon(GLUtessellator tessellator) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluTessEndPolygon();
}

/*****************************************************************************

 * <b>gluBeginPolygon</b> and {@link #gluEndPolygon gluEndPolygon}
 * delimit the definition of a nonconvex polygon. To define such a
 * polygon, first call <b>gluBeginPolygon</b>. Then define the
 * contours of the polygon by calling {@link #gluTessVertex
 * gluTessVertex} for each vertex and {@link #gluNextContour
 * gluNextContour} to start each new contour. Finally, call {@link
 * #gluEndPolygon gluEndPolygon} to signal the end of the
 * definition. See the {@link #gluTessVertex gluTessVertex} and {@link
 * #gluNextContour gluNextContour} reference pages for more
 * details.<P>

 *
 * Once {@link #gluEndPolygon gluEndPolygon} is called,
 * the polygon is tessellated, and the resulting triangles are described
 * through callbacks. See {@link #gluTessCallback
 * gluTessCallback} for descriptions of the callback methods.
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 *
 * @see #gluNewTess          gluNewTess
 * @see #gluNextContour      gluNextContour
 * @see #gluTessCallback     gluTessCallback
 * @see #gluTessVertex       gluTessVertex
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluTessBeginContour gluTessBeginContour
 ****************************************************************************/
public void gluBeginPolygon(GLUtessellator tessellator) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluBeginPolygon();
}

/*****************************************************************************
 * <b>gluNextContour</b> is used to describe polygons with multiple
 * contours. After you describe the first contour through a series of
 * {@link #gluTessVertex gluTessVertex} calls, a
 * <b>gluNextContour</b> call indicates that the previous contour is complete
 * and that the next contour is about to begin. Perform another series of
 * {@link #gluTessVertex gluTessVertex} calls to
 * describe the new contour. Repeat this process until all contours have been
 * described.<P>
 *
 * The type parameter defines what type of contour follows. The following
 * values are valid. <P>
 *
 * <b>GLU_EXTERIOR</b>
 * <UL>
 *   An exterior contour defines an exterior boundary of the polygon.
 * </UL>
 * <b>GLU_INTERIOR</b>
 * <UL>
 *   An interior contour defines an interior boundary of the polygon (such as
 *   a hole).
 * </UL>
 * <b>GLU_UNKNOWN</b>
 * <UL>
 *   An unknown contour is analyzed by the library to determine whether it is
 *   interior or exterior.
 * </UL>
 * <b>GLU_CCW, GLU_CW</b>
 * <UL>
 *   The first <b>GLU_CCW</b> or <b>GLU_CW</b> contour defined is considered
 *   to be exterior. All other contours are considered to be exterior if they
 *   are oriented in the same direction (clockwise or counterclockwise) as the
 *   first contour, and interior if they are not. If one contour is of type
 *   <b>GLU_CCW</b> or <b>GLU_CW</b>, then all contours must be of the same
 *   type (if they are not, then all <b>GLU_CCW</b> and <b>GLU_CW</b> contours
 *   will be changed to <b>GLU_UNKNOWN</b>). Note that there is no
 *   real difference between the <b>GLU_CCW</b> and <b>GLU_CW</b> contour
 *   types.
 * </UL><P>
 *
 * To define the type of the first contour, you can call <b>gluNextContour</b>
 * before describing the first contour. If you do not call
 * <b>gluNextContour</b> before the first contour, the first contour is marked
 * <b>GLU_EXTERIOR</b>.<P>
 *
 * <UL>
 *   <b>Note:</b>  The <b>gluNextContour</b> function is obsolete and is
 *   provided for backward compatibility only. The <b>gluNextContour</b>
 *   function is mapped to {@link #gluTessEndContour
 *   gluTessEndContour} followed by
 *   {@link #gluTessBeginContour gluTessBeginContour}.
 * </UL>
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 * @param type
 *        The type of the contour being defined.
 *
 * @see #gluNewTess          gluNewTess
 * @see #gluTessBeginContour gluTessBeginContour
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluTessCallback     gluTessCallback
 * @see #gluTessEndContour   gluTessEndContour
 * @see #gluTessVertex       gluTessVertex
 ****************************************************************************/
public void gluNextContour(GLUtessellator tessellator, int type) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluNextContour(type);
}

/*****************************************************************************
 * <b>gluEndPolygon</b> and {@link #gluBeginPolygon
 * gluBeginPolygon} delimit the definition of a nonconvex polygon. To define
 * such a polygon, first call {@link #gluBeginPolygon
 * gluBeginPolygon}. Then define the contours of the polygon by calling
 * {@link #gluTessVertex gluTessVertex} for each vertex
 * and {@link #gluNextContour gluNextContour} to start
 * each new contour. Finally, call <b>gluEndPolygon</b> to signal the end of
 * the definition. See the {@link #gluTessVertex
 * gluTessVertex} and {@link #gluNextContour
 * gluNextContour} reference pages for more details.<P>
 *
 * Once <b>gluEndPolygon</b> is called, the polygon is tessellated, and the
 * resulting triangles are described through callbacks. See
 * {@link #gluTessCallback gluTessCallback} for
 * descriptions of the callback methods.
 *
 * @param tessellator
 *        Specifies the tessellation object (created with
 *        {@link #gluNewTess gluNewTess}).
 *
 * @see #gluNewTess          gluNewTess
 * @see #gluNextContour      gluNextContour
 * @see #gluTessCallback     gluTessCallback
 * @see #gluTessVertex       gluTessVertex
 * @see #gluTessBeginPolygon gluTessBeginPolygon
 * @see #gluTessBeginContour gluTessBeginContour
 ****************************************************************************/
public void gluEndPolygon(GLUtessellator tessellator) {
    GLUtessellatorImpl tess = (GLUtessellatorImpl) tessellator;
    tess.gluEndPolygon();
}

//----------------------------------------------------------------------
// Quadric functionality
//

/** Interface to C language function: <br> <code> void gluCylinder(GLUquadric *  quad, GLdouble base, GLdouble top, GLdouble height, GLint slices, GLint stacks); </code>    */
public void gluCylinder(GLUquadric quad, double base, double top, double height, int slices, int stacks) {
  ((GLUquadricImpl) quad).drawCylinder(getCurrentGL(), (float) base, (float) top, (float) height, slices, stacks);
}

/** Interface to C language function: <br> <code> void gluDeleteQuadric(GLUquadric *  quad); </code>    */
public void gluDeleteQuadric(GLUquadric quad) {
}

/** Interface to C language function: <br> <code> void gluDisk(GLUquadric *  quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops); </code>    */
public void gluDisk(GLUquadric quad, double inner, double outer, int slices, int loops) {
  ((GLUquadricImpl) quad).drawDisk(getCurrentGL(), (float) inner, (float) outer, slices, loops);
}

/** Interface to C language function: <br> <code> GLUquadric *  gluNewQuadric(void); </code>    */
public GLUquadric gluNewQuadric() {
  return new GLUquadricImpl();
}

/** Interface to C language function: <br> <code> void gluPartialDisk(GLUquadric *  quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops, GLdouble start, GLdouble sweep); </code>    */
public void gluPartialDisk(GLUquadric quad, double inner, double outer, int slices, int loops, double start, double sweep) {
  ((GLUquadricImpl) quad).drawPartialDisk(getCurrentGL(), (float) inner, (float) outer, slices, loops, (float) start, (float) sweep);
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
  ((GLUquadricImpl) quad).drawSphere(getCurrentGL(), (float) radius, slices, stacks);
}

//----------------------------------------------------------------------
// Projection routines
//

private Project project;

public void gluOrtho2D(double left, double right, double bottom, double top) {
  project.gluOrtho2D(getCurrentGL(), left, right, bottom, top);
}

public void gluPerspective(double fovy, double aspect, double zNear, double zFar) {
  project.gluPerspective(getCurrentGL(), fovy, aspect, zNear, zFar);
}

public void gluLookAt(double eyeX, double eyeY, double eyeZ, double centerX, double centerY, double centerZ, double upX, double upY, double upZ) {
  project.gluLookAt(getCurrentGL(), eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single array.
 */
public boolean gluProject(double objX, double objY, double objZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] winPos, int winPos_offset) {
  return project.gluProject(objX, objY, objZ, model, model_offset, proj, proj_offset, view, view_offset, winPos, winPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluProject(GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  winX, GLdouble *  winY, GLdouble *  winZ); </code>
 * <P> Accepts the outgoing window coordinates as a single buffer.
 */
public boolean gluProject(double objX, double objY, double objZ, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, DoubleBuffer winPos) {
  return project.gluProject(objX, objY, objZ, model, proj, view, winPos);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single array.
 */
public boolean gluUnProject(double winX, double winY, double winZ, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double[] objPos, int objPos_offset) {
  return project.gluUnProject(winX, winY, winZ, model, model_offset, proj, proj_offset, view, view_offset, objPos, objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject(GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ); </code>
 * <P> Accepts the outgoing object coordinates (a 3-vector) as a single buffer.
 */
public boolean gluUnProject(double winX, double winY, double winZ, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, DoubleBuffer objPos) {
  return project.gluUnProject(winX, winY, winZ, model, proj, view, objPos);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single array.
 */
public boolean gluUnProject4(double winX, double winY, double winZ, double clipW, double[] model, int model_offset, double[] proj, int proj_offset, int[] view, int view_offset, double nearVal, double farVal, double[] objPos, int objPos_offset) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, model_offset, proj, proj_offset, view, view_offset, nearVal, farVal, objPos, objPos_offset);
}

/** Interface to C language function: <br> <code> GLint gluUnProject4(GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *  model, const GLdouble *  proj, const GLint *  view, GLdouble nearVal, GLdouble farVal, GLdouble *  objX, GLdouble *  objY, GLdouble *  objZ, GLdouble *  objW); </code>
 * <P> Accepts the outgoing object coordinates (a 4-vector) as a single buffer.
 */
public boolean gluUnProject4(double winX, double winY, double winZ, double clipW, DoubleBuffer model, DoubleBuffer proj, IntBuffer view, double nearVal, double farVal, DoubleBuffer objPos) {
  return project.gluUnProject4(winX, winY, winZ, clipW, model, proj, view, nearVal, farVal, objPos);
}

public void gluPickMatrix(double x, double y, double delX, double delY, int[] viewport, int viewport_offset) {
  project.gluPickMatrix(getCurrentGL(), x, y, delX, delY, viewport, viewport_offset);
}

public void gluPickMatrix(double x, double y, double delX, double delY, IntBuffer viewport) {
  project.gluPickMatrix(getCurrentGL(), x, y, delX, delY, viewport);
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

private ByteBuffer copyToByteBuffer(Buffer buf) {
  if (buf instanceof ByteBuffer) {
    if (buf.position() == 0) {
      return (ByteBuffer) buf;
    }
    return BufferUtil.copyByteBuffer((ByteBuffer) buf);
  } else if (buf instanceof ShortBuffer) {
    return BufferUtil.copyShortBufferAsByteBuffer((ShortBuffer) buf);
  } else if (buf instanceof IntBuffer) {
    return BufferUtil.copyIntBufferAsByteBuffer((IntBuffer) buf);
  } else if (buf instanceof FloatBuffer) {
    return BufferUtil.copyFloatBufferAsByteBuffer((FloatBuffer) buf);
  } else {
    throw new IllegalArgumentException("Unsupported buffer type (must be one of byte, short, int, or float)");
  }
}

private int gluScaleImageJava( int format, int widthin, int heightin,
                               int typein, Buffer datain, int widthout, int heightout,
                               int typeout, Buffer dataout ) {
  ByteBuffer in = null;
  ByteBuffer out = null;
  in = copyToByteBuffer(datain);
  if( dataout instanceof ByteBuffer ) {
    out = (ByteBuffer)dataout;
  } else if( dataout instanceof ShortBuffer ) {
    out = BufferUtil.newByteBuffer(dataout.remaining() * BufferUtil.SIZEOF_SHORT);
  } else if ( dataout instanceof IntBuffer ) {
    out = BufferUtil.newByteBuffer(dataout.remaining() * BufferUtil.SIZEOF_INT);
  } else if ( dataout instanceof FloatBuffer ) {
    out = BufferUtil.newByteBuffer(dataout.remaining() * BufferUtil.SIZEOF_FLOAT);
  } else {
    throw new IllegalArgumentException("Unsupported destination buffer type (must be byte, short, int, or float)");
  }
  int errno = Mipmap.gluScaleImage( getCurrentGL(), format, widthin, heightin, typein, in, 
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


private int gluBuild1DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int format, int type, int userLevel, int baseLevel, int maxLevel,
                                        Buffer data ) {
  ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmapLevels( getCurrentGL(), target, internalFormat, width,
          format, type, userLevel, baseLevel, maxLevel, buffer ) );
}


private int gluBuild1DMipmapsJava( int target, int internalFormat, int width,
                                   int format, int type, Buffer data ) {
  ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild1DMipmaps( getCurrentGL(), target, internalFormat, width, format,
          type, buffer ) );
}


private int gluBuild2DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int height, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, Buffer data ) {
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmapLevels( getCurrentGL(), target, internalFormat, width,
          height, format, type, userLevel, baseLevel, maxLevel, data ) );
}

private int gluBuild2DMipmapsJava( int target, int internalFormat, int width,
                                   int height, int format, int type, Buffer data ) {
  // While the code below handles other data types, it doesn't handle non-ByteBuffers
  data = copyToByteBuffer(data);
  return( Mipmap.gluBuild2DMipmaps( getCurrentGL(), target, internalFormat, width, height,
          format, type, data) );
}

private int gluBuild3DMipmapLevelsJava( int target, int internalFormat, int width,
                                        int height, int depth, int format, int type, int userLevel, int baseLevel,
                                        int maxLevel, Buffer data) {
  ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmapLevels( getCurrentGL(), target, internalFormat, width,
          height, depth, format, type, userLevel, baseLevel, maxLevel, buffer) );
}

private int gluBuild3DMipmapsJava( int target, int internalFormat, int width,
                                   int height, int depth, int format, int type, Buffer data ) {
  ByteBuffer buffer = copyToByteBuffer(data);
  return( Mipmap.gluBuild3DMipmaps( getCurrentGL(), target, internalFormat, width, height,
          depth, format, type, buffer ) );
}


//----------------------------------------------------------------------
// Wrappers for mipmap and image scaling entry points which dispatch either
// to the Java or C versions.
//

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data); </code>    */
public int gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapLevelsJava(target, internalFormat, width, format, type, level, base, max, data);
  } else {
    return gluBuild1DMipmapLevelsC(target, internalFormat, width, format, type, level, base, max, data);
  }
}

/** Interface to C language function: <br> <code> GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data); </code>    */
public int gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild1DMipmapsJava(target, internalFormat, width, format, type, data);
  } else {
    return gluBuild1DMipmapsC(target, internalFormat, width, format, type, data);
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
public int gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild2DMipmapsJava(target, internalFormat, width, height, format, type, data);
  } else {
    return gluBuild2DMipmapsC(target, internalFormat, width, height, format, type, data);
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
public int gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, java.nio.Buffer data) {
  if (useJavaMipmapCode) {
    return gluBuild3DMipmapsJava(target, internalFormat, width, height, depth, format, type, data);
  } else {
    return gluBuild3DMipmapsC(target, internalFormat, width, height, depth, format, type, data);
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
// GLUProcAddressTable handling
//

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
