<style>
table, th, td {
   border: 1px solid black;
}
</style>

# JOGL and OpenGL Divergence 

## Overview
As described in [mapping of OpenGL profiles to interfaces](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/overview-summary.html#overview_description), JOGL is mostly API compatible with the OpenGL API while packaging functionality in [OpenGL profiles](https://jogamp.org/jogl/doc/uml/html/index-withframe.html).

However, there are certain aspects where JOGL diverges due to managed code aspects and the Java™ language.

### OpenGL Debugging
In 2009-2012 the [Debug Output](https://www.khronos.org/opengl/wiki/Debug_Output) extensions
[AMD_debug_output](http://www.opengl.org/registry/specs/AMD/debug_output.txt) and
[ARB_debug_output](https://registry.khronos.org/OpenGL/extensions/ARB/ARB_debug_output.txt) were created,
subsumed to the core via [KHR_debug](https://registry.khronos.org/OpenGL/extensions/KHR/KHR_debug.txt).

They allow applications to [enable and control low-level debugging](https://docs.gl/gl4/glDebugMessageCallback) 
using the OpenGL implementation.

These extensions have been subsumed in [OpenGL 4.3](https://www.khronos.org/opengl/wiki/GLAPI/glDebugMessageCallback)
and in [OpenGL ES 3.2](https://registry.khronos.org/OpenGL-Refpages/es3/html/glDebugMessageCallback.xhtml).

In JOGL a user may attached a [GLDebugListener](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLDebugListener.html)
to the [GLContext](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLContext.html) via [addGLDebugListener(..)](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLContext.html#addGLDebugListener\(com.jogamp.opengl.GLDebugListener\)).
The GLDebugListener receives the native [GLDebugMessage](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLDebugMessage.html) if debugging is enabled.

To enable debugging, the GLContext must be created having [CTX_OPTION_DEBUG](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLContext.html#CTX_OPTION_DEBUG) set via `setContextCreationFlags(int)` on the [GLContext](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLContext.html#setContextCreationFlags\(int\)) or [GLAutoDrawable](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLAutoDrawable.html#setContextCreationFlags(int)).
Alternatively, [GLContext.enableGLDebugMessage(boolean](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLContext.html#enableGLDebugMessage\(boolean\)) may be used.

Test cases
- [TestGLDebug00NEWT](https://jogamp.org/cgit/jogl.git/tree/src/test/com/jogamp/opengl/test/junit/jogl/acore/TestGLDebug00NEWT.java)
- [TestGLDebug01NEWT](https://jogamp.org/cgit/jogl.git/tree/src/test/com/jogamp/opengl/test/junit/jogl/acore/TestGLDebug01NEWT.java)

### Retrieve Mapped Buffer's Data
Since OpenGL 1.5, a mapped buffer object's data can be retrieved via [glGetBufferPointerv(..)](https://www.khronos.org/opengl/wiki/GLAPI/glGetBufferPointer), i.e. the client's memory pointer.

In JOGL, buffers are tracked via [GLBufferStorage](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLBufferStorage.html), covering the storage size, NIO client memory reference if mapped and usage flags.
This ensures lifecycle alignment of the native NIO Java client memory with OpenGL buffer semantics.

To retrieve the bound buffer you may use [getBoundBuffer(..)](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLBase.html#getBoundBuffer\(int\)) and [getBufferStorage(..)](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/opengl/GLBase.html#getBufferStorage\(int\)).

Tracker Implementation
- [GLBufferObjectTracker](https://jogamp.org/cgit/jogl.git/tree/src/jogl/classes/jogamp/opengl/GLBufferObjectTracker.java)
- [GLBufferStateTracker](https://jogamp.org/cgit/jogl.git/tree/src/jogl/classes/jogamp/opengl/GLBufferStateTracker.java)

Test Cases
- [TestMapBufferRead01NEWT](https://jogamp.org/cgit/jogl.git/tree/src/test/com/jogamp/opengl/test/junit/jogl/acore/TestMapBufferRead01NEWT.java)
- [TestMapBufferRead02NEWT](https://jogamp.org/cgit/jogl.git/tree/src/test/com/jogamp/opengl/test/junit/jogl/acore/TestMapBufferRead02NEWT.java)

## References
- [JogAmp Home](https://jogamp.org/)
- [JOGL Git Repo](https://jogamp.org/cgit/jogl.git/about/)
- [JOGL Home](https://jogamp.org/jogl/www/)
- [OpenGL Evolution & JOGL](https://jogamp.org/jogl/doc/Overview-OpenGL-Evolution-And-JOGL.html)
- [Mapping of OpenGL Profiles to Interfaces](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/overview-summary.html#overview_description)
  - [OpenGL API Inclusion Criteria](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/overview-summary.html#GLAPIInclusionCriteria)
  - [UML Diagram](https://jogamp.org/jogl/doc/uml/html/index-withframe.html)
- [How To Build](https://jogamp.org/jogl/doc/HowToBuild.html)
