/*****************************************************************************
 * <b>gluBeginPolygon</b> and {@link net.java.games.jogl.GLU#gluEndPolygon
 * gluEndPolygon} delimit the definition of a nonconvex polygon. To define
 * such a polygon, first call <b>gluBeginPolygon</b>. Then define the contours
 * of the polygon by calling {@link net.java.games.jogl.GLU#gluTessVertex
 * gluTessVertex} for each vertex and
 * {@link net.java.games.jogl.GLU#gluNextContour gluNextContour} to start each
 * new contour. Finally, call {@link net.java.games.jogl.GLU#gluEndPolygon
 * gluEndPolygon} to signal the end of the definition. See the
 * {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex} and
 * {@link net.java.games.jogl.GLU#gluNextContour gluNextContour} reference
 * pages for more details.<P>
 *
 * Once {@link net.java.games.jogl.GLU#gluEndPolygon gluEndPolygon} is called,
 * the polygon is tessellated, and the resulting triangles are described
 * through callbacks. See {@link net.java.games.jogl.GLU#gluTessCallback
 * gluTessCallback} for descriptions of the callback methods.
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 *
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluNextContour      gluNextContour
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 * @see net.java.games.jogl.GLU#gluTessVertex       gluTessVertex
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour
 ****************************************************************************/
public void gluBeginPolygon(GLUtesselator tesselator);


/*****************************************************************************
 * <b>gluDeleteTess</b> destroys the indicated tessellation object (which was
 * created with {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 *
 * @param tesselator
 *        Specifies the tessellation object to destroy.
 *
 * @see net.java.games.jogl.GLU#gluBeginPolygon gluBeginPolygon
 * @see net.java.games.jogl.GLU#gluNewTess      gluNewTess
 * @see net.java.games.jogl.GLU#gluTessCallback gluTessCallback
 ****************************************************************************/
public void gluDeleteTess(GLUtesselator tesselator);


/*****************************************************************************
 * <b>gluEndPolygon</b> and {@link net.java.games.jogl.GLU#gluBeginPolygon
 * gluBeginPolygon} delimit the definition of a nonconvex polygon. To define
 * such a polygon, first call {@link net.java.games.jogl.GLU#gluBeginPolygon
 * gluBeginPolygon}. Then define the contours of the polygon by calling
 * {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex} for each vertex
 * and {@link net.java.games.jogl.GLU#gluNextContour gluNextContour} to start
 * each new contour. Finally, call <b>gluEndPolygon</b> to signal the end of
 * the definition. See the {@link net.java.games.jogl.GLU#gluTessVertex
 * gluTessVertex} and {@link net.java.games.jogl.GLU#gluNextContour
 * gluNextContour} reference pages for more details.<P>
 *
 * Once <b>gluEndPolygon</b> is called, the polygon is tessellated, and the
 * resulting triangles are described through callbacks. See
 * {@link net.java.games.jogl.GLU#gluTessCallback gluTessCallback} for
 * descriptions of the callback methods.
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 *
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluNextContour      gluNextContour
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 * @see net.java.games.jogl.GLU#gluTessVertex       gluTessVertex
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour
 ****************************************************************************/
public void gluEndPolygon(GLUtesselator tesselator);


/*****************************************************************************
 * <b>gluGetTessProperty</b> retrieves properties stored in a tessellation
 * object.  These properties affect the way that tessellation objects are
 * interpreted and rendered.  See the
 * {@link net.java.games.jogl.GLU#gluTessProperty gluTessProperty} reference
 * page for information about the properties and what they do.
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 * @param which
 *        Specifies the property whose value is to be fetched. Valid values
 *        are <b>GLU_TESS_WINDING_RULE</b>, <b>GLU_TESS_BOUNDARY_ONLY</b>,
 *        and <b>GLU_TESS_TOLERANCES</b>.
 * @param value
 *        Specifices an array into which the value of the named property is
 *        written.
 *
 * @see net.java.games.jogl.GLU#gluNewTess      gluNewTess
 * @see net.java.games.jogl.GLU#gluTessProperty gluTessProperty
 ****************************************************************************/
public void gluGetTessProperty(GLUtesselator tesselator, int which, double[] value);


/*****************************************************************************
 * <b>gluNewTess</b> creates and returns a new tessellation object.  This
 * object must be referred to when calling tesselation methods.  A return
 * value of null means that there was not enough memeory to allocate the
 * object.
 *
 * @return A new tessellation object.
 *
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluDeleteTess       gluDeleteTess
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 ****************************************************************************/
public GLUtesselator gluNewTess();


/*****************************************************************************
 * <b>gluNextContour</b> is used to describe polygons with multiple
 * contours. After you describe the first contour through a series of
 * {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex} calls, a
 * <b>gluNextContour</b> call indicates that the previous contour is complete
 * and that the next contour is about to begin. Perform another series of
 * {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex} calls to
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
 *   function is mapped to {@link net.java.games.jogl.GLU#gluTessEndContour
 *   gluTessEndContour} followed by
 *   {@link net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour}.
 * </UL>
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 * @param type
 *        The type of the contour being defined.
 *
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 * @see net.java.games.jogl.GLU#gluTessEndContour   gluTessEndContour
 * @see net.java.games.jogl.GLU#gluTessVertex       gluTessVertex
 ****************************************************************************/
public void gluNextContour(GLUtesselator tesselator, int type);


/*****************************************************************************
 * <b>gluTessBeginContour</b> and
 * {@link net.java.games.jogl.GLU#gluTessEndContour gluTessEndContour} delimit
 * the definition of a polygon contour. Within each
 * <b>gluTessBeginContour</b>/
 * {@link net.java.games.jogl.GLU#gluTessEndContour gluTessEndContour} pair,
 * there can be zero or more calls to
 * {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex}. The vertices
 * specify a closed contour (the last vertex of each contour is automatically
 * linked to the first). See the {@link net.java.games.jogl.GLU#gluTessVertex
 * gluTessVertex} reference page for more details. <b>gluTessBeginContour</b>
 * can only be called between
 * {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon} and
 * {@link net.java.games.jogl.GLU#gluTessEndPolygon gluTessEndPolygon}.
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 *
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluTessVertex       gluTessVertex
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 * @see net.java.games.jogl.GLU#gluTessProperty     gluTessProperty
 * @see net.java.games.jogl.GLU#gluTessNormal       gluTessNormal
 * @see net.java.games.jogl.GLU#gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessBeginContour(GLUtesselator tesselator);


/*****************************************************************************
 * <b>gluTessBeginPolygon</b> and
 * {@link net.java.games.jogl.GLU#gluTessEndPolygon gluTessEndPolygon} delimit
 * the definition of a convex, concave or self-intersecting polygon. Within
 * each <b>gluTessBeginPolygon</b>/
 * {@link net.java.games.jogl.GLU#gluTessEndPolygon gluTessEndPolygon} pair,
 * there must be one or more calls to
 * {@link net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour}/
 * {@link net.java.games.jogl.GLU#gluTessEndContour gluTessEndContour}. Within
 * each contour, there are zero or more calls to
 * {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex}. The vertices
 * specify a closed contour (the last vertex of each contour is automatically
 * linked to the first). See the {@link net.java.games.jogl.GLU#gluTessVertex
 * gluTessVertex}, {@link net.java.games.jogl.GLU#gluTessBeginContour
 * gluTessBeginContour}, and {@link net.java.games.jogl.GLU#gluTessEndContour
 * gluTessEndContour} reference pages for more details.<P>
 *
 * <b>data</b> is a reference to a user-defined data structure. If the
 * appropriate callback(s) are specified (see
 * {@link net.java.games.jogl.GLU#gluTessCallback gluTessCallback}), then this
 * reference is returned to the callback method(s). Thus, it is a convenient
 * way to store per-polygon information.<P>
 *
 * Once {@link net.java.games.jogl.GLU#gluTessEndPolygon gluTessEndPolygon} is
 * called, the polygon is tessellated, and the resulting triangles are
 * described through callbacks. See
 * {@link net.java.games.jogl.GLU#gluTessCallback gluTessCallback} for
 * descriptions of the callback methods.
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 * @param data
 *        Specifies a reference to user polygon data.
 *
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour
 * @see net.java.games.jogl.GLU#gluTessVertex       gluTessVertex
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 * @see net.java.games.jogl.GLU#gluTessProperty     gluTessProperty
 * @see net.java.games.jogl.GLU#gluTessNormal       gluTessNormal
 * @see net.java.games.jogl.GLU#gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessBeginPolygon(GLUtesselator tesselator, Object data);


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
 * {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon}
 * was called. The legal callbacks are as follows:<P>
 *
 * <b>GLU_TESS_BEGIN</b>
 * <UL>
 *   The begin callback is invoked like {@link net.java.games.jogl.GL#glBegin
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
 *   {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void beginData(int type, Object polygonData);</PRE>
 *
 * <b>GLU_TESS_EDGE_FLAG</b>
 * <UL>
 *   The edge flag callback is similar to
 *   {@link net.java.games.jogl.GL#glEdgeFlag glEdgeFlag}. The method takes
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
 *   {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void edgeFlagData(boolean boundaryEdge, Object polygonData);</PRE>
 *
 * <b>GLU_TESS_VERTEX</b>
 * <UL>
 *   The vertex callback is invoked between the begin and end callbacks. It is
 *   similar to {@link net.java.games.jogl.GL#glVertex glVertex}, and it
 *   defines the vertices of the triangles created by the tessellation
 *   process. The method takes a reference as its only argument. This
 *   reference is identical to the opaque reference provided by the user when
 *   the vertex was described (see
 *   {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex}). The method
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
 *   {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void vertexData(Object vertexData, Object polygonData);</PRE>
 *
 * <b>GLU_TESS_END</b>
 * <UL>
 *   The end callback serves the same purpose as
 *   {@link net.java.games.jogl.GL#glEnd glEnd}. It indicates the end of a
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
 *   {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon}
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
 *   {@link net.java.games.jogl.GLU#gluTessEndPolygon gluTessEndPolygon} is
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
 *   {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon}
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
 *   {@link net.java.games.jogl.GLU#gluErrorString gluErrorString} call. The
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
 *   {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon}
 *   was called. The method prototype for this callback is:
 * </UL>
 *
 * <PRE>
 *         void errorData(int errnum, Object polygonData);</PRE>
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
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
 * @see net.java.games.jogl.GL#glBegin              glBegin
 * @see net.java.games.jogl.GL#glEdgeFlag           glEdgeFlag
 * @see net.java.games.jogl.GL#glVertex             glVertex
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluErrorString      gluErrorString
 * @see net.java.games.jogl.GLU#gluTessVertex       gluTessVertex
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour
 * @see net.java.games.jogl.GLU#gluTessProperty     gluTessProperty
 * @see net.java.games.jogl.GLU#gluTessNormal       gluTessNormal
 ****************************************************************************/
public void gluTessCallback(GLUtesselator tesselator, int which,
                            GLUtesselatorCallback aCallback);


/*****************************************************************************
 *  <b>gluTessEndContour</b> and
 * {@link net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour}
 * delimit the definition of a polygon contour. Within each
 * {@link net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour}/
 * <b>gluTessEndContour</b> pair, there can be zero or more calls to
 * {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex}. The vertices
 * specify a closed contour (the last vertex of each contour is automatically
 * linked to the first). See the {@link net.java.games.jogl.GLU#gluTessVertex
 * gluTessVertex} reference page for more details.
 * {@link net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour} can
 * only be called between {@link net.java.games.jogl.GLU#gluTessBeginPolygon
 * gluTessBeginPolygon} and
 * {@link net.java.games.jogl.GLU#gluTessEndPolygon gluTessEndPolygon}.
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 *
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluTessVertex       gluTessVertex
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 * @see net.java.games.jogl.GLU#gluTessProperty     gluTessProperty
 * @see net.java.games.jogl.GLU#gluTessNormal       gluTessNormal
 * @see net.java.games.jogl.GLU#gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessEndContour(GLUtesselator tesselator);


/*****************************************************************************
 * <b>gluTessEndPolygon</b> and
 * {@link net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon}
 * delimit the definition of a convex, concave or self-intersecting polygon.
 * Within each {@link net.java.games.jogl.GLU#gluTessBeginPolygon
 * gluTessBeginPolygon}/<b>gluTessEndPolygon</b> pair, there must be one or
 * more calls to {@link net.java.games.jogl.GLU#gluTessBeginContour
 * gluTessBeginContour}/{@link net.java.games.jogl.GLU#gluTessEndContour
 * gluTessEndContour}. Within each contour, there are zero or more calls to
 * {@link net.java.games.jogl.GLU#gluTessVertex gluTessVertex}. The vertices
 * specify a closed contour (the last vertex of each contour is automatically
 * linked to the first). See the {@link net.java.games.jogl.GLU#gluTessVertex
 * gluTessVertex}, {@link net.java.games.jogl.GLU#gluTessBeginContour
 * gluTessBeginContour} and {@link net.java.games.jogl.GLU#gluTessEndContour
 * gluTessEndContour} reference pages for more details.<P>
 *
 * Once <b>gluTessEndPolygon</b> is called, the polygon is tessellated, and
 * the resulting triangles are described through callbacks. See
 * {@link net.java.games.jogl.GLU#gluTessCallback gluTessCallback} for
 * descriptions of the callback functions.
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 *
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour
 * @see net.java.games.jogl.GLU#gluTessVertex       gluTessVertex
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 * @see net.java.games.jogl.GLU#gluTessProperty     gluTessProperty
 * @see net.java.games.jogl.GLU#gluTessNormal       gluTessNormal
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 ****************************************************************************/
public void gluTessEndPolygon(GLUtesselator tesselator);


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
 * @param tesselator
 *        Specifies the tessellation object (created by
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 * @param x
 *        Specifies the first component of the normal.
 * @param y
 *        Specifies the second component of the normal.
 * @param z
 *        Specifies the third component of the normal.
 *
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessNormal(GLUtesselator tesselator, double x, double y, double z);


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
 *   absolute value of at least two).  The previous GLU tesselator (prior to
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
 * @param tesselator
 *        Specifies the tessellation object created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}
 * @param which
 *        Specifies the property to be set.  Valid values are
 *        <b>GLU_TESS_WINDING_RULE</b>, <b>GLU_TESS_BOUNDARDY_ONLY</b>,
 *        <b>GLU_TESS_TOLERANCE</b>.
 * @param value
 *        Specifices the value of the indicated property.
 *
 * @see net.java.games.jogl.GLU#gluGetTessProperty gluGetTessProperty
 * @see net.java.games.jogl.GLU#gluNewTess         gluNewTess
 ****************************************************************************/
public void gluTessProperty(GLUtesselator tesselator, int which, double value);


/*****************************************************************************
 * <b>gluTessVertex</b> describes a vertex on a polygon that the program
 * defines. Successive <b>gluTessVertex</b> calls describe a closed contour.
 * For example, to describe a quadrilateral <b>gluTessVertex</b> should be
 * called four times. <b>gluTessVertex</b> can only be called between
 * {@link net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour} and
 * {@link net.java.games.jogl.GLU#gluTessBeginContour gluTessEndContour}.<P>
 *
 * <b>data</b> normally references to a structure containing the vertex
 * location, as well as other per-vertex attributes such as color and normal.
 * This reference is passed back to the user through the
 * <b>GLU_TESS_VERTEX</b> or <b>GLU_TESS_VERTEX_DATA</b> callback after
 * tessellation (see the {@link net.java.games.jogl.GLU#gluTessCallback
 * gluTessCallback} reference page).
 *
 * @param tesselator
 *        Specifies the tessellation object (created with
 *        {@link net.java.games.jogl.GLU#gluNewTess gluNewTess}).
 * @param coords
 *        Specifies the coordinates of the vertex.
 * @param data
 *        Specifies an opaque reference passed back to the program with the
 *        vertex callback (as specified by
 *        {@link net.java.games.jogl.GLU#gluTessCallback gluTessCallback}).
 *
 * @see net.java.games.jogl.GLU#gluTessBeginPolygon gluTessBeginPolygon
 * @see net.java.games.jogl.GLU#gluNewTess          gluNewTess
 * @see net.java.games.jogl.GLU#gluTessBeginContour gluTessBeginContour
 * @see net.java.games.jogl.GLU#gluTessCallback     gluTessCallback
 * @see net.java.games.jogl.GLU#gluTessProperty     gluTessProperty
 * @see net.java.games.jogl.GLU#gluTessNormal       gluTessNormal
 * @see net.java.games.jogl.GLU#gluTessEndPolygon   gluTessEndPolygon
 ****************************************************************************/
public void gluTessVertex(GLUtesselator tesselator, double[] coords, Object data);


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
 * GLEventListener#displayChanged(GLDrawable,boolean,boolean)}.
 *
 * @param gluFunctionName the name of the OpenGL function (e.g., use
 * "gluNurbsCallbackDataEXT" to check if the <code>
 * gluNurbsCallbackDataEXT(GLUnurbs, GLvoid)</code> extension is available).
 */
public boolean isFunctionAvailable(String gluFunctionName);
