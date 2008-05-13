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
