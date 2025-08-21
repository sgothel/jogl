<!---
We convert markdown using pandoc using `markdown+lists_without_preceding_blankline` as source format
and `html5+smart` with a custom template as the target.

Recipe:
```
  ~/pandoc-buttondown-cgit/pandoc_md2html_local.sh GlueGen_Mapping.md > GlueGen_Mapping.html
```  

Git repos:
- https://jausoft.com/cgit/users/sgothel/pandoc-buttondown-cgit.git/about/
- https://github.com/sgothel/pandoc-buttondown-cgit
-->

<style>
table, th, td {
   border: 1px solid black;
}
</style>

# JOGL Specification Overview {#overview_description}

## Preface

This specification, an optional set of packages, describes the Java(TM)
bindings to the native OpenGL(R) 3D graphics library profiles:

-   OpenGL \[ 1.0 .. 4.6 \], compatibility- and core profiles
-   OpenGL ES \[ 1.0 .. 3.2 \]
-   EGL \[ 1.0 .. 1.5 \]

[Inclusion Criteria](#GLAPIInclusionCriteria) explains the OpenGL
profile separation.

See [OpenGL Runtime Requirements](#GLRuntimeVersion).

An implementation is available as [JOGL, a JogAmp
module](http://jogl.jogamp.org).

Other API bindings are available as JogAmp modules:

-   OpenCL(R) [as JOCL](http://jocl.jogamp.org)
-   OpenAL(R) [as JOAL](http://joal.jogamp.org)

## Dependencies

This binding has dependencies to the following:

-   Either of the following Java implementations:
    -   [Java SE 1.6 or later](http://docs.oracle.com/javase/6/docs/api/)
    -   A mobile JavaVM with language 1.6 support, ie:
        -   [Android API Level 9 (Version 2.3)](http://developer.android.com/reference/packages.html)
        -   [JamVM](http://jamvm.sourceforge.net/)

        with
        -   [Java 1.4 *java.nio*
            implementation](http://docs.oracle.com/javase/1.4.2/docs/api/java/nio/package-summary.html)

-   {@linkplain com.jogamp.nativewindow NativeWindow Protocol}

    The *NativeWindow Protocol* is included in JogAmp\'s implementation

## OpenGL Profile Model

OpenGL today is not just a single set of functions, it offers many
profiles for different purposes, e.g. ES1, ES2 and ES3 for mobile, GL \[
3.1 .. 4.6 \] core for a programmable shader application, etc.

JOGL reflects these profiles [with an OO abstraction model](http://jogamp.org/jogl/doc/uml/html/), 
specifying interfaces encapsulating common subsets.

## Package Structure

The packages defined by this specification include:

-   The **com.jogamp.opengl** package

    This package contains all Java bindings for all OpenGL profiles.

    See [Inclusion Criteria](#GLAPIInclusionCriteria) explaining the
    OpenGL profile seperation.

    See [OpenGL Runtime Requirements](#GLRuntimeVersion).

    The main OpenGL profile interfaces are:

    -   {@link com.jogamp.opengl.GL2 com.jogamp.opengl.GL2} interface

        This interface contains all OpenGL \[ 1.0 .. 3.0 \] methods, as
        well as most of it\'s extensions defined at the time of this
        specification.

        OpenGL extensions whose functionality was incorporated into core
        OpenGL ≤ 3.0, are subsumed into the core namespace.

        See [GL2 Inclusion Criteria](#GL2InclusionCriteria).

        See [GL2 Runtime Requirements](#GL2RuntimeVersion).

        Future extensions will be added with a [maintenance update](#maintenanceupdates)

    -   {@link com.jogamp.opengl.GL3 com.jogamp.opengl.GL3} interface

        This interface contains all OpenGL \[ 3.1 .. 3.3 \] *core*
        methods, as well as most of it\'s extensions defined at the time
        of this specification.

        Note: OpenGL \[ 3.1 .. 3.3 \] core profile does not includes
        fixed point functionality.

        See [GL3 Inclusion Criteria](#GL3InclusionCriteria).

        See [GL3 Runtime Requirements](#GL3RuntimeVersion).

        Future extensions will be added with a [maintenance
        update](#maintenanceupdates)

    -   {@link com.jogamp.opengl.GL3bc com.jogamp.opengl.GL3bc} interface

        This interface contains all OpenGL \[ 3.1 .. 3.3 \]
        *compatibility* methods, as well as most of it\'s extensions
        defined at the time of this specification.

        Note: OpenGL \[ 3.1 .. 3.3 \] compatibility profile does
        includes fixed point functionality.

        Future extensions will be added with a [maintenance
        update](#maintenanceupdates)

    -   {@link com.jogamp.opengl.GL4 com.jogamp.opengl.GL4} interface

        This interface contains all OpenGL \[ 4.0 .. 4.6 \] *core*
        methods, as well as most of it\'s extensions defined at the time
        of this specification.

        Note: OpenGL \[ 4.0 .. 4.6 \] core profile does not includes
        fixed point functionality.

        Future extensions will be added with a [maintenance
        update](#maintenanceupdates)

    -   {@link com.jogamp.opengl.GL4bc com.jogamp.opengl.GL4bc} interface

        This interface contains all OpenGL \[ 4.0 .. 4.6 \]
        *compatibility* profile, as well as most of it\'s extensions
        defined at the time of this specification.

        Note: OpenGL \[ 4.0 .. 4.6 \] compatibility profile does
        includes fixed point functionality.

        Future extensions will be added with a [maintenance
        update](#maintenanceupdates)

    -   {@link com.jogamp.opengl.GLES1 com.jogamp.opengl.GLES1} interface

        This interface contains all OpenGL ES \[ 1.0 .. 1.1 \] methods,
        as well as most of it\'s extensions defined at the time of this
        specification.

        Future extensions will be added with a [maintenance
        update](#maintenanceupdates)

    -   {@link com.jogamp.opengl.GLES2 com.jogamp.opengl.GLES2} interface

        This interface contains all OpenGL ES 2.0 methods, as well as
        most of it\'s extensions defined at the time of this
        specification.

        Future extensions will be added with a [maintenance
        update](#maintenanceupdates)

    -   {@link com.jogamp.opengl.GLES3 com.jogamp.opengl.GLES3} interface

        This interface contains all OpenGL ES \[ 3.0 .. 3.2 \] methods,
        as well as most of it\'s extensions defined at the time of this
        specification.

        Future extensions will be added with a [maintenance
        update](#maintenanceupdates)

    Additionally the packages contains interfaces resembling
    intersecting *common profiles*. These *common profiles* may be
    utilize for cross profile code supposed to either run on desktop and
    mobile devices, or across GL profiles themselves:

    -   {@link com.jogamp.opengl.GLBase com.jogamp.opengl.GLBase} interface

        Common interface containing the profile type identification and
        conversion methods.

        Used to query which specialized profile class an instance of
        this object actually is and offering a protocol to convert it to
        these types.

    -   {@link com.jogamp.opengl.GL com.jogamp.opengl.GL} interface

        Common interface containing the subset of all profiles, GL4bc,
        GL4, GL3bc, GL3, GL2, GLES1, GLES2 and GLES3.

        This interface reflects common data types, texture and
        framebuffer functionality.

    -   {@link com.jogamp.opengl.GL2ES1 com.jogamp.opengl.GL2ES1} interface

        Interface containing the common subset of GL2 and GLES1.

        This interface reflects the fixed functionality of OpenGL,
        without the immediate mode API.

    -   {@link com.jogamp.opengl.GL2ES2 com.jogamp.opengl.GL2ES2} interface

        Interface containing the common subset of GL2 and GLES2.
        Interface is almost GLES2 complete.

        This interface reflects the programmable shader functionality of
        desktop and embedded OpenGL up until GLES2.

    -   {@link com.jogamp.opengl.GL3ES3 com.jogamp.opengl.GL3ES3} interface

        Interface containing the common subset of core GL3 and GLES3.
        Interface is almost GLES3 complete, lacking
        `GL_ARB_ES3_compatibility` extension.

        This interface reflects the programmable shader functionality of
        desktop and embedded OpenGL up until GLES3.

    -   {@link com.jogamp.opengl.GL4ES3 com.jogamp.opengl.GL4ES3} interface

        Interface containing the common subset of core GL4 and GLES3.
        Interface is GLES3 complete w/o vendor extensions.

        This interface reflects the programmable shader functionality of
        desktop and embedded OpenGL up until GLES3.

    -   {@link com.jogamp.opengl.GL2GL3 com.jogamp.opengl.GL2GL3} interface

        Interface containing the common subset of core GL3 (OpenGL 3.1+)
        and GL2 (OpenGL 3.0), also known as the OpenGL 3.0 forward
        compatible, non deprecated subset.

        This interface reflects only the programmable shader
        functionality of desktop OpenGL

-   The **com.jogamp.opengl.glu** package

    This package contains bindings for the OpenGL Graphics System
    Utility (GLU) Library version 1.3, inclusive, with the exception of
    the GLU NURBS routines which are not exposed.


## API Binding Conventions

The Java language bindings to the pre-existing C APIs in these packages
have been created using a consistent set of rules. Vendor-defined
extensions should make use of the same rules in order to provide a
consistent developer experience.

The rules for creating the Java language binding are described in the
following sections. These rules should be followed as closely as
possible for all future APIs that share the com.jogamp.opengl namespace.

### Function Naming

Functions are named in the same way as in the C binding. That is, an
OpenGL API function glClear is bound to Java method GL.glClear. Although
it would be possible to drop the gl prefix (since it is redundant with
the interface name GL), the resulting code was deemed to look too
foreign to experienced OpenGL developers. For the same reason, we have
also carried over all type suffixes like 3f and 3fv from methods such as
glColor3f and glColor3fv, respectively.

Extension suffixes, such as EXT, ARB, and vendor-specific suffixes, are
retained so as to match C conventions.

### Mapping of Constants

Constants are named in the same way as in the C binding. For instance,
the OpenGL constant GL_RGB is bound to Java constant GL.GL_RGB.

### Mapping of Primitive Types

All 8-bit integral types become byte, all 16-bit integral types become
short, and all 32-bit integral types become int. All 32-bit
floating-point types become float and all 64-bit floating-point types
become double.

Integer return values that can only be GL_TRUE or GL_FALSE are mapped to
boolean.

### Mapping of Pointer Arguments

OpenGL functions that take pointer arguments fall into several
categories:

-   Functions that take an untyped pointer argument for immediate use
-   Functions that take a typed pointer argument for immediate use
-   Functions that take an untyped pointer argument for deferred use
-   Functions that take a typed pointer argument for deferred use

Functions that take an untyped (void\*) pointer argument for immediate
use are given a single binding that takes a New I/O (NIO) Buffer object.
The Buffer may be of any type allowable by the function (and compatible
with the other arguments to the function) and may be direct or indirect.
An example of an OpenGL API in this category is glTexImage2D.

Functions that take a typed pointer (e.g., GLfloat \*) argument for
immediate use are given two bindings. The first takes a Java primitive
array with a type that matches the C pointer type (i.e., GLfloat\* maps
to float\[\]). The second takes a typed Buffer object (i.e., GLfloat\*
maps to FloatBuffer). An example of an OpenGL API in this category is
glColor3fv.

Functions that take an untyped (void\*) pointer argument for deferred
use are given a single binding that takes a Buffer object. The Buffer
may be of any type allowable by the function (and compatible with the
other arguments to the function), but must be direct. That is, it may
not have been created from a Java primitive array using the wrap method.
The functions that fall into this category generally have names ending
with the suffix \"pointer.\" An example of an OpenGL API in this
category is glVertexPointer. Because these functions do not consume the
data located at the given pointer immediately, but only at some
unspecified later time, it is not possible to use a Java primitive array
whose memory location may change.

Functions that take a typed (e.g., GLfloat\*) pointer argument for
deferred use are given a single binding that takes a typed Buffer object
(i.e., GLfloat\* maps to FloatBuffer). The Buffer must be direct. That
is, it may not have been created from a Java primitive array using the
wrap method. An example of an OpenGL API in this category is
glFeedbackBuffer.

Methods that read or write a specific number of values from an array or
Buffer argument do not read or write any subsequent elements of the
array or Buffer.

An outgoing C char\* pointer, if representing a null-terminated,
read-only C string, maps to a Java String. An outgoing C char\*\*
pointer, if similarly representing an array of read-only C strings, maps
to a Java String\[\] (array of String objects). All other char\*
pointers, including those representing mutable C strings as used in some
Get methods, are mapped to byte\[\] and ByteBuffer.

### Index Parameter for Arrays

Each C method argument that is mapped to a primitive array in Java is
actually mapped to two separate parameters: the appropriate primitive
array type in Java and an integer offset parameter. The value of the
integer offset is the index which the method will start reading from
within the array. Earlier indices will be ignored. This mapping provides
more congruity with existing Java APIs and allows reuse of a single
array across multiple Java method calls by changing the index in much
the same way that C pointers permit for C arrays.

### Reduction of Method Explosions

Since there are two ways to expand a given C method pointer parameter,
it would be possible for C methods with multiple pointer arguments to
expand to many Java methods if one was to consider every possible
combination of mappings (the C method would expand to the number of
pointer parameters to the power of 2). In order to avoid an API
explosion, we restrict a given Java method to like kind mappings only.
In other words, a given C method with N typed pointer parameters for
immediate use, where N \>= 1, will map to exactly two Java methods: One
with all primitive arrays and one with all Buffer types.

Also, methods that accept multiple Buffer arguments require all direct
or all non-direct Buffers. Direct and non-direct buffers should never be
mixed within an API call by an application.

### Byte ordering of Buffers

When allocating a New I/O Buffer (in particular, a direct ByteBuffer) to
be passed to the APIs in these packages, it is essential to set the
*byte ordering* of the newly-allocated ByteBuffer to the *native* byte
ordering of the platform: e.g.
`ByteBuffer.allocateDirect(...).order(ByteOrder.nativeOrder());`. The
byte order of the ByteBuffer indicates how multi-byte values such as int
and float are stored in the Buffer either using methods like putInt and
putFloat or views such as IntBuffer or FloatBuffer. The Java bindings
perform no conversion or byte swapping on the outgoing data to OpenGL,
and the native OpenGL implementation expects data in the host CPU\'s
byte order, so it is essential to always match the byte order of the
underlying platform when filling Buffers with data.

### Auto-slicing of Buffers

When a Buffer object is passed to an OpenGL function binding, the actual
pointer argument that is passed down to the OpenGL C implementation is
equal to the starting pointer of the Buffer data, plus an offset given
by the Buffer.position() function, multiplied by the data type size in
bytes (1 for a ByteBuffer, 2 for a ShortBuffer, 4 for a IntBuffer or
FloatBuffer, and 8 for DoubleBuffer). The array offset given by
Buffer\<type\>.arrayOffset() is also added in the offset for wrapped
arrays.

This feature is known as \"auto-slicing,\" as it mimics the effect of
calling slice() on the Buffer object without the overhead of explicit
object creation.

### Errors and Exceptions

For performance reasons, OpenGL functions do not return error values
directly. Instead, applications must query for errors using functions
such as glGetError. This behavior is largely preserved in the Java
language bindings, as described below.

In the interest of efficiency, the Java API does not generally throw
exceptions. However, running an application with the DebugGL composable
pipeline, which is part of the API, will force an exception to be thrown
at the point of failure.

Many errors are defined by OpenGL merely to set the error code, rather
than throwing an exception. For example, passing a bad enumerated
parameter value may result in the error flag being set to
GL.GL_INVALID_VALUE. Attempting to check for such errors in the binding
layer would require either replicating the error-checking logic of the
underlying engine, or querying the error state after every function.
This would greatly impact performance by inhibiting the ability of the
hardware to pipeline work.

### Security

Exception behavior is defined in cases that could otherwise lead to
illegal memory accesses in the underlying OpenGL engine. Implementations
should take necessary steps to prevent the GL from accessing or
overwriting memory except for properly allocated Buffers and array
method arguments.

An implementation should take care to validate arguments correctly
before invoking native methods that could potentially access memory
illegally. In particular, methods that validate the contents of an array
(such as a list of GL attributes) or a Buffer should take precautions
against exploits in which a separate thread attempts to alter the
contents of the argument during the time interval following validation
but preceding passage of the argument to the underlying native engine.

## Sharing of Server-Side OpenGL Objects between GLContexts {#sharing}

Sharing of server-side OpenGL objects such as buffer objects, e.g. VBOs,
and textures among OpenGL contexts is supported in this specification.

See {@link com.jogamp.opengl.GLSharedContextSetter GLSharedContextSetter} interface for details.

## Criteria Used for Inclusion of APIs into the Java Bindings

### OpenGL API Inclusion Criteria {#GLAPIInclusionCriteria}

OpenGL functions and OpenGL extensions have been included in the Java
bindings according the following rules:

#### GL3 interface {#GL3InclusionCriteria}
{@link com.jogamp.opengl.GL3 com.jogamp.opengl.GL3} interface:
    -   All functions in core, forward compatible, OpenGL \[ 3.1 - 3.3
        \], inclusive, have been included, as described in the header
        files `GL/glcorearb.h`.
    -   Reason for starting a new profile beginning with 3.1 are:
        -   OpenGL 3.1 requires a new native context, incompatible with
            prior versions.
        -   OpenGL 3.1 core profile drops fixed functionality.
    -   Forward compatibility, aka core, ie a context without
        `GL_ARB_compatibility`, is chosen because:
        -   It shares a common subset with ES2.x
        -   It is not guaranteed to be provided by all vendors.
        -   It is not guaranteed to be provided in future versions.
        -   OpenGL 3.2 core profile is compatible with OpenGL 3.1
            forward compatible spec.
        -   OpenGL 3.2 Spec Appendix E.1: It is not possible to
            implement both core and compatibility profiles in a single
            GL context, ..

#### GL2 interface {#GL2InclusionCriteria}
{@link com.jogamp.opengl.GL2 com.jogamp.opengl.GL2} interface
    -   All functions in core OpenGL 3.0, inclusive, have been included.
    -   Reason for making the *cut* at OpenGL 3.0 are:
        -   Availability of 3.0 with the same native context.
        -   Availability of 3.0 via extensions.
    -   If the functionality of the OpenGL extension was subsumed into
        core OpenGL by version 3.0, then the extension was dropped from
        the Java bindings. However, if the core function name is not
        available in the native OpenGL implementation, the extension
        named equivalent is used instead, e.g.
        *GL_ARB_framebuffer_object*.
    -   In general the native method name will be looked up as follows
        -   Try the interface name
        -   Try the extension name: ARB, EXT, ..

#### General Inclusion Rules
-   Functions that deal with explicit pointer values in such a way that
    they cannot be properly implemented in Java have been excluded. 
    This includes retrieval methods with a C void \*\* in the OpenGL
    signature like glGetBufferPointerv, glGetPointerv,
    glGetVertexAttribPointerv, as well as functions that require
    persistent pointer to pointer storage across function calls like
    vertex array lists.
-   If the extension is registered in the official OpenGL extension
    registry but the specification was never completed or was
    discontinued (as indicated in the specification and/or lack of
    inclusion in SGI\'s official OpenGL header files), then the
    extension was not included.  Using these criteria, ARB extensions
    through number 42 (GL_ARB_pixel_buffer_object), inclusive, and
    non-ARB extensions through number 311 (GL_REMEDY_string_marker),
    inclusive, have been included in the Java bindings according to the
    numbering scheme found in the official OpenGL extension registry.
-   Some bindings to several vendor-specific extensions have been
    included that are not found in the OpenGL extension registry.  These
    extensions were deemed popular enough and/or were specifically
    requested by users.
-   Platform-specific extensions, such as those that begin with WGL,
    GLX, CGL, etc., have been excluded from the public API.  See the
    section \"Accessing platform-specific extensions\" for more
    information about accessing these functions on certain
    implementations.

### OpenGL GLU API Inclusion Criteria

Bindings for all core GLU APIs have been included with the exception of
the GLU NURBS APIs.  These APIs may be included in a future maintenance
release of the Java bindings.

## OpenGL Extensions

### Creating New Extensions

While the Java APIs for OpenGL extensions are unconditionally exposed,
the underlying functions may not be present. A program can query whether
a potentially unavailable function is actually available at runtime by
using the method GL.isFunctionAvailable.

Bindings for OpenGL extensions not covered in this specification may be
supplied by individual vendors or groups. Such bindings may be
considered for inclusion in a future version of this specification. In
order to avoid fragmentation, vendors creating extension bindings should
expose new extensions using the method GL.getExtension. This method is
intended to provide a mechanism for vendors who wish to provide access
to new OpenGL extensions without changing the public API of the core
package.

Names for added extension methods and extension-defined constants and
Java bindings for C parameters should follow the guidelines set forth in
this specification.

### Accessing Platform-Specific Extensions

Platform-specific extensions such as those that begin with WGL, GLX,
CGL, etc. are not included in the API.  Each implementation can choose
to export all, some, or none of these APIs via the
GL.getPlatformGLExtensions API which returns an Object whose underlying
data type is specific to a given implementation.

Therefore, any usage of these APIs is both platform and implementation
specific.

## OpenGL Version on Runtime System {#GLRuntimeVersion}

### GL4 Desktop Requirements {#GL4RuntimeVersion}

An OpenGL ≥ 4.0 version is required to instantiate a {@link com.jogamp.opengl.GL4 GL4} context.

### GL3 Desktop Requirements {#GL3RuntimeVersion}

An OpenGL ≥ 3.1 version is required to instantiate a {@link com.jogamp.opengl.GL3 GL3} context.

### GL2 Desktop Requirements {#GL2RuntimeVersion}

An OpenGL ≥ 1.5 version is required to instantiate a {@link com.jogamp.opengl.GL2 GL2} context.

Even though OpenGL extensions whose functionality was included into core
OpenGL by version 3.0, inclusive, are not included in the bindings, it
should be noted that OpenGL version 3.0 is not an absolute requirement
on the runtime system. This is because a user could query whether any
particular function is available before calling certain core APIs that
might not be present. Also, if the core function name is not available
in the native OpenGL implementation, the extension named equivalent is
used instead, e.g. *GL_ARB_framebuffer_object*. However, in general, it
is reasonable to expect at least OpenGL 1.5 to be installed on the
runtime system and an implementor of the API is free to require the
presence of at least OpenGL 1.5 on the target system.

**The JOGL reference implementation require at least OpenGL version 1.1**, 
due to it's dynamical function binding starting with OpenGL 1.2.

In future revisions of the API, this minimum standard may be raised.

### GLES3 Requirements {#GLES3RuntimeVersion}

An OpenGL ES ≥ 3.0 version is required to instantiate an {@link com.jogamp.opengl.GLES3 GLES3} context.

### GLES2 Requirements {#GLES2RuntimeVersion}

An OpenGL ES ≥ 2.0 version is required to instantiate an {@link com.jogamp.opengl.GLES2 GLES2} context.

### GLES1 Requirements {#GLES1RuntimeVersion}

An OpenGL ES \[ 1.0 .. 1.1 \] version is required to instantiate an {@link com.jogamp.opengl.GLES1 GLES1} context.

## Runtime Version Information {#RuntimeVersionInformation}

Any Java Bindings for OpenGL implementation should include version
information in its jar manifest file. This information can then easily
be accessed at runtime via the java.lang.Package API. At least the
following information is included in the Reference Implementation jar
file manifest: Specification Title, Specification Vendor, Specification
Version, Implementation Vendor, and Implementation Version.

JOGL provides {@link com.jogamp.opengl.JoglVersion} implementing 
{@link com.jogamp.common.util.JogampVersion}, which provides users access to
the specification and implementation version, the build date and source
code repository branch name and it\'s latest commit identifier.

## Future Maintenance Updates {#maintenanceupdates}

New core APIs found in future versions of OpenGL, as well as new OpenGL
extensions, are expected to be added to the bindings and included into
the com.jogamp.opengl namespace via future maintenance updates to the
API.

## Revision History

-   Early Draft Review, October/November 2005
-   Public Review, December/January 2005
-   Proposed Final Draft Review, February/March 2006
-   1.0.0 Final Release, September 2006
-   1.1.0 Maintenance Release, April 2007
-   2.0.0 Maintenance Release, July 2009
-   2.0.1 Java Dependency Update to 1.5, February 2011
-   2.0.2 Major Release, July 18th 2013
-   2.3.0 Major Release, March 11th 2015

# Further Information {#readme}
The following is an excerpt of our [README](https://jogamp.org/cgit/jogl.git/about/).

## Contact Us
- Forum/Mailinglist  [http://forum.jogamp.org/](http://forum.jogamp.org/)
- Maintainer         [https://jogamp.org/wiki/index.php/Maintainer_and_Contacts](https://jogamp.org/wiki/index.php/Maintainer_and_Contacts)
- Sven's Blog        [https://jausoft.com/blog/tag/jogamp/](https://jausoft.com/blog/tag/jogamp/)
- Email              sgothel _at_ jausoft _dot_ com

## References
- [JogAmp Home](https://jogamp.org/)
- [JOGL Home](https://jogamp.org/jogl/www/)
- [Git Repository](https://jogamp.org/cgit/jogl.git/about/)
- [OpenGL Evolution & JOGL](https://jogamp.org/jogl/doc/Overview-OpenGL-Evolution-And-JOGL.html)
- [Mapping of OpenGL Profiles to Interfaces](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/index.html#overview_description)
  - [OpenGL API Inclusion Criteria](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/index.html#GLAPIInclusionCriteria)
  - [UML Diagram](https://jogamp.org/jogl/doc/uml/html/index-withframe.html)
- [JOGL and OpenGL Divergence](https://jogamp.org/jogl/doc/OpenGL_API_Divergence.html)
- [How To Build](https://jogamp.org/jogl/doc/HowToBuild.html)
- [Wiki](https://jogamp.org/wiki/)
- [**Khronos Registry**](http://www.khronos.org/registry/)
  -   [ES 3.1 Ref Pages](http://www.khronos.org/opengles/sdk/docs/man31/)
  -   [ES 3.2 Spec](https://registry.khronos.org/OpenGL/specs/es/3.2/es_spec_3.2.withchanges.pdf)
  -   [GLSL ES 3.20 Spec](https://registry.khronos.org/OpenGL/specs/es/3.2/GLSL_ES_Specification_3.20.pdf)
- [**OpenGL Registry**](http://www.opengl.org/registry/)
  -   [GL 4 Ref Pages](http://www.opengl.org/sdk/docs/man4/)
  -   [GLSL Ref Pages](http://www.opengl.org/sdk/docs/manglsl/)
  -   [GL 4.6 Core Spec](http://www.opengl.org/registry/doc/glspec46.core.withchanges.pdf)
  -   [GL 4.6 Compat.  Spec](http://www.opengl.org/registry/doc/glspec46.compatibility.withchanges.pdf)
  -   [GLSL 4.60 spec](http://www.opengl.org/registry/doc/GLSLangSpec.4.60.diff.pdf)

## JogAmp History & Milestones
Bottom line, too much work has been performed to be listed here.    
However, let's have a few sentimental points listed and we may add a few more as we go.

### OpenGL™ for Java™ (GL4Java)
[*OpenGL™ for Java™ (GL4Java)*](https://jogamp.org/cgit/gl4java.git/about/) was developed [from March 1997](https://jausoft.com/gl4java/docs/overview/history.html) 
until [March 2003](https://jogamp.org/cgit/gl4java.git/log/).    
Its concepts were reused in the subsequently launched [JOGL project](https://jogamp.org/jogl/www/)    
initially [lead by Sun Microsystems](#gluegen-joal-and-jogl-at-sun-microsystems--co) and later by [the JogAmp community](https://jogamp.org/),    
rendering *GL4Java* effectively *JOGL's* predecessor.  A few of the concepts reused were:
- C-Header Compiler to JNI glue code: C2J -> GlueGen
- AWT integration: GLCanvas, GLJPanel (swing)
- WinHandleAccess -> NativeWindow
- GLDrawableFactory, GLDrawable, GLContext, GLEvenListener

### GlueGen, JOAL and JOGL at Sun Microsystems &amp; Co
- 2003-06-06 [Initial JOGL code commit](https://jogamp.org/cgit/jogl.git/commit/?id=d49fd968963909f181423eae46c613189468fac3) 
- 2003-06-07 [Initial JOAL code commit](https://jogamp.org/cgit/joal.git/commit/?id=5f9e58c5b1a23119a63dfb1e76e73349858439db)
- 2004-02-18 [JOGL version 1.0.0-b01](https://jogamp.org/cgit/jogl.git/commit/?id=9b0fa9dad2b196d8f86e5e6b575deadac059e877)
- 2006-01-15 [GlueGen separation from JOGL, own project + repo](https://jogamp.org/cgit/gluegen.git/commit/?id=df0f5636884b212bcc7a2d9b1b61c195bba79621)
- 2007-03-13 [JSR-231](https://jcp.org/aboutJava/communityprocess/final/jsr231/index.html) [1st Maintencance Release](https://jcp.org/aboutJava/communityprocess/maintenance/jsr231/231ChangeLog.html)
- 2007-04-19 [JOGL Version 1.1.0](https://jogamp.org/cgit/jogl.git/commit/?id=7fe08dd72d240c8b74617424595bcc65334b78db)
- 2008-04-29 [JSR-231](https://jcp.org/aboutJava/communityprocess/final/jsr231/index.html) [2nd Maintencance Release](https://jcp.org/aboutJava/communityprocess/maintenance/jsr231/231ChangeLog.html)
- 2008-04-30 [JOGL Version 1.1.1](https://jogamp.org/cgit/jogl.git/commit/?id=547be0683b524e35a82f9461a0b93d95b8e74849)
- 2008-05-22 [JOGL on an embedded Nvidia APX 2500 (Tegra1), JavaOne 2008](https://www.youtube.com/watch?v=DeupVAMnvFA)
- 2008-06-01 [JOGL 2 Start: NEWT, NativeWindow abstraction, OpenGL profiles, ...](https://jogamp.org/cgit/jogl.git/commit/?id=806564c9599510db2bb0e2d0e441ca6ad8068aa0)
- 2008-12-xx [OpenMAX and JOGL GL ES2 on embedded Nvidia APX 2500 (Tegra1)](https://www.youtube.com/watch?v=D6Lkw3eZK1w)
- 2009-06-16 [Merged JOGL 2 Branch: NEWT, NativeWindow abstraction, OpenGL profiles, ...](https://jogamp.org/cgit/jogl.git/log/?h=JOGL_2_SANDBOX)
- 2009-07-09 [completed git migration](https://jogamp.org/cgit/jogl.git/commit/?id=9d910cf21fb8a61e3a1604f6258364c3b725964d), see also [this blog](https://jausoft.com/blog/2009/07/08/svn-to-git-migration-1/) 
- 2009-07-24 [JOCL initiation](https://jogamp.org/cgit/jocl.git/commit/?id=1737ee672c05d956a99a91d9894556230f6363bc), independent from Sun
- 2009-10-02 [Adding embedded Intel-GDL support (NEWT, EGL, ES2) to JOGL](https://jogamp.org/cgit/jogl.git/commit/?id=52c3caf07ad07fcb029ea584d7e5f4c5031f84c2)
- 2009-10-10 [JOGL Plugin3 Integration](https://jogamp.org/cgit/jogl.git/commit/?id=2268a6ce8a900ae7aa9f20d5f595f811185574a9)

### JogAmp Period
- 2009-11-10 [Away from Sun Microsystems](https://jogamp.org/cgit/jogl.git/commit/?id=87eb12f5846ccef587c5945ced99b778bcd67ba6), see also [this blog](https://jausoft.com/blog/2009/11/09/jogl-is-dead-long-live-jogl/) 
- 2010-05-07 [JogAmp launch ...](https://jausoft.com/blog/2010/05/07/jogamp-org-wip/) 
- 2010-10-01 [NEWT/AWT Reparenting](https://jogamp.org/cgit/jogl.git/commit/?id=fd87de826f391bf490fdb1e4b8b659348d21324b)
- 2010-11-23 [JogAmp RC v2.0-rc1](https://jogamp.org/cgit/jogl.git/commit/?id=ce3508aa66b9a40974cce2988094d0edc68b30f4)
- 2011-02-20 [JogAmp Production Home in Germany](https://jausoft.com/blog/2011/02/20/jogamp-production-lifecycle-fun/) 
- Resolution Independent NURBS @ GPU (the essential _GraphUI toolkit_) 
  - 2011-04-01 [First artifacts](https://jausoft.com/blog/2011/04/01/resolution-independent-gpu-accelerated-curve-font-rendering/) 
  - 2011-10-05 [Paper and software release](https://jausoft.com/blog/2011/10/05/jogljogamp-red-square-moscow-nurbs-graphicon2011/) 
- 2011-08-17 [Work on embedded devices and Android](https://jausoft.com/blog/2011/08/17/jogl-embedded-device-status-p1/) 
- 2012-04-19 Added [streaming audio/video player, JOGL on desktop & mobile status](https://jausoft.com/blog/2012/04/19/jogljogamp-status-update/) 
- 2013-02-20 [Java3D Continuation](https://jogamp.org/wiki/index.php?title=Java3D_Overview) and [its git repo](https://jogamp.org/cgit/java3d/)
- 2013-07-24 First [JogAmp Release 2.0.2](https://jogamp.org/wiki/index.php?title=SW_Tracking_Report_Objectives_for_the_release_2.0.2_of_JOGL) 
- 2015-03-11 [JogAmp Release 2.3.0](https://jogamp.org/wiki/index.php?title=SW_Tracking_Report_Objectives_for_the_release_2.3.0) 
- 2015-10-10 [JogAmp Release 2.3.2](https://jogamp.org/wiki/index.php?title=SW_Tracking_Report_Objectives_for_the_release_2.3.2) 
- 2015-11-08 [JOCL: OpenCL 2.0 Support](https://jogamp.org/cgit/jocl.git/commit/?id=edd9720fbb570e0fe177cc41d3612084ea8a7b17)
- 2018-01-28 [Ardor3D Continuation](https://jogamp.org/wiki/index.php?title=Ardor3D_Overview) and [its git repo](https://jogamp.org/cgit/ardor3d.git/)
- JogAmp on iOS 
  - 2019-06-23 [First iOS Visuals](https://jausoft.com/blog/2019/06/23/jogamp-ios-arm64-port-first-visuals/), running on my [own custom OpenJDK build](https://jausoft.com/blog/2019/06/17/jogamp-ios-arm64-bring-up/) 
  - 2019-07-08 Fully working [NEWT + JOGL on iOS](https://jausoft.com/blog/2019/07/08/jogamp-ios-arm64-port-newt/) 
- 2019-11-30 [Java11, support for DRM/GBM and iOS etc](https://jausoft.com/blog/2019/11/30/jogamp-2-4-0-release-feature-freeze-complete/) 
- 2023-01-15 Added MacOS aarch64 support
- 2023-01-22 Added SWT 4.26 support (JOGL)
- 2023-01-31 Added NEWT Windows/X11 (Soft) PixelScale support (JOGL)
- 2023-02-01 [JogAmp Release 2.4.0](https://jogamp.org/wiki/index.php/SW_Tracking_Report_Objectives_for_the_release_2.4.0)
- 2023-02-24 [FFmpeg Binding Update](https://jausoft.com/blog/2023/02/24/jogamps-jogl-ffmpeg-binding-update/)
- [Revamp Graph Type Rendering and Graph UI](https://jausoft.com/blog/tag/graph_type_rendering/)
  - 2023-02-22 [Reimagine Java on Desktop & Bare-Metal Devices](https://jausoft.com/blog/2023/02/22/reimagine-java-on-desktop-bare-metal-devices/)
  - 2023-04-10 [Type Animation Update 2](https://jausoft.com/blog/2023/04/10/graphui-type-animation-update-2/)
  - 2023-04-14 [FontView App (Micro FontForge)](https://jausoft.com/blog/2023/04/14/graphui-fontview-app-micro-fontforge/)
  - 2023-05-23 [JOAL/OpenAL + GraphUI](https://jausoft.com/blog/2023/05/23/joal-openal-graphui-spatial-sound-in-your-ui-jogamp-v2-5-0-notes/)
- 2023-05-06 [Supported MacOS Version](https://jogamp.org/cgit/gluegen.git/tree/doc/JogAmpMacOSVersions.md)
- 2023-05-15 Fixed [DPI Scaling with AWT and AWT+NEWT](https://forum.jogamp.org/DPI-scaling-not-working-tp4042206p4042603.html) (JOGL)
- 2023-05-20 [JOAL: OpenAL-Soft v1.23.1](https://openal-soft.org/), [git about](https://jogamp.org/cgit/joal.git/about/), [www face](https://jogamp.org/joal/www/).
- 2023-06-16 [GlueGen Updates](https://jogamp.org/gluegen/doc/GlueGen_Mapping.html)
  - 2023-06-16 [GlueGen Revised Struct Mapping](https://jogamp.org/gluegen/doc/GlueGen_Mapping.html#struct-mapping)
  - 2023-06-16 Added [GlueGen git-about](https://jogamp.org/cgit/gluegen.git/about/) and updated [www face](https://jogamp.org/gluegen/www/)
  - 2023-07-10 Added [GlueGen JavaCallback](https://jogamp.org/gluegen/doc/GlueGen_Mapping.html#java-callback) w/ `AL_SOFT_events` support in JOAL

### Conferences 
- JavaOne [2002 (GL4Java)](https://jogamp.org/jogl/www/3167.pdf), [2003](https://jogamp.org/jogl/www/2125.pdf), [2004](https://jogamp.org/jogl/www/ts1361.pdf), [2006](https://jogamp.org/jogl/www/bof0899.pdf), [2007](https://jogamp.org/jogl/www/BOF-3908-JOGL-slides.pdf), [2008](https://www.youtube.com/watch?v=DeupVAMnvFA)
- Siggraph [2010](https://jogamp.org/doc/siggraph2010/jogamp-siggraph2010.pdf), [2011](https://jogamp.org/doc/siggraph2011/jogamp-siggraph2011.pdf), [2012](https://jogamp.org/doc/siggraph2012/), [2013](https://jogamp.org/doc/siggraph2013/), [2014](https://jogamp.org/doc/siggraph2014/)
- Fosdem [2013](https://jogamp.org/doc/fosdem2013/), [2014](https://jogamp.org/doc/fosdem2014/)

### Papers
Santina, Rami. Resolution Independent NURBS Curve Rendering using Programmable Graphics Pipeline. GraphiCon'2011. [Paper](https://jogamp.org/doc/gpunurbs2011/p70-santina.pdf), [Slides](https://jogamp.org/doc/gpunurbs2011/graphicon2011-slides.pdf), [Initial Blog](https://jausoft.com/blog/2011/10/05/jogljogamp-red-square-moscow-nurbs-graphicon2011/), [Blog Series](https://jausoft.com/blog/tag/graph_type_rendering/)

## Acknowledgments
The JogAmp Community is grateful for all the contributions
of all of the individuals who have advanced the project. 
For sure we are not able to list all of them here.
Please contact us if you like to be added to this list.

This list can hardly cover all contributors and their contributions.

Since roughly 2010, JOGL development has been continued
by individuals of the JogAmp community, see git log for details.

### Chronological
Sven Gothel created [*OpenGL™ for Java™ (GL4Java)*](#opengl-for-java-gl4java)
in March 1997 and maintained it up until March 2003.

Kenneth Bradley Russell and Christopher John Kline 
[developed JOGL, acquired by Sun Microsystems](#gluegen-joal-and-jogl-at-sun-microsystems--co)
and released the first public version in 2003.

Gerard Ziemski contributed the original port of JOGL to Mac OS X.

Rob Grzywinski and Artur Biesiadowski contributed the Ant build
support. Alex Radeski contributed the cpptasks support in the build
process.

Pepijn Van Eeckhoudt and Nathan Parker Burg contributed the Java port
of the GLU tessellator. Pepijn also contributed the initial version of
the FPSAnimator utility class.

James Walsh (GKW) contributed the substantial port
of the GLU mipmap generation code to Java, as well as robustness fixes
in the Windows implementation and other areas.

The JSR-231 expert group as a whole provided valuable discussions and
guidance in the design of the current APIs. In particular, Kevin
Rushforth, Daniel Rice and Travis Bryson were instrumental in the
design of the current APIs.

Travis Bryson did extensive work on the GlueGen tool to make it
conform to the desired API design. He also shepherded JSR-231 through
the standardization process, doing extensive cross-validation of the
APIs and implementation along the way, and authored JOGL's nightly
build system.

Lilian Chamontin contributed the JOGLAppletLauncher, opening new ways
of deploying 3D over the web.

Christopher Campbell collaborated closely with the JOGL development
team to enable interoperability between Sun's OpenGL pipeline for
Java2D and JOGL in Java SE 6, and also co-authored the TextureIO
subsystem.

Sven Gothel refactored the windowing subsystem layer to be generic,
introduced the support for multiple GL profiles, realized NEWT etc.
He teamed up with Rami Santina, realizing the new graph package
exposing generic curve, text and UI support.

Rami Santina researched and implemented the math behind the new 
graph package [RSantina](https://jausoft.com/blog/2011/10/05/jogljogamp-red-square-moscow-nurbs-graphicon2011/), etc.

The following individuals made significant contributions to various
areas of the project (Alphabetical):

- Michael Bien
- Artur Biesiadowski
- Travis Bryson
- Nathan Parker Burg
- Lilian Chamontin
- Alban Cousinié
- Pepijn Van Eeckhoudt
- Mathieu Féry
- Athomas Goldberg
- Sven Gothel
- Julien Gouesse
- Rob Grzywinski 
- Yuri Vladimir Gushchin
- Harvey Harrison
- Christopher John Kline 
- Martin Pernollet
- Gregory Pierce
- Emmanuel Puybaret
- Xerxes Rånby
- Alex Radeski
- Daniel Rice
- Kevin Rushforth
- Kenneth Bradley Russell
- Rami Santina
- Dominik Ströhlein (DemoscenePassivist)
- Dmitri Trembovetski
- Wade Walker
- James Walsh (GKW)
- Carsten Weisse
- Gerard Ziemski

The JogAmp Community is grateful for the support of the
javagaming.org community and it's [own JogAmp forum](http://forum.jogamp.org/), 
from where dozens, if not hundreds, of individuals have 
contributed discussions, bug reports, bug fixes, and other forms of support.


