
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
