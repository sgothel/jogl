Organization of the JOGL source tree
------------------------------------

doc/   Build and user documentation
make/  Ant build scripts
       Configuration files for glue code generation
       Header files for glue code generation
       See top of build.xml for brief invocation instructions
src/   Java and native source code for JOGL
       (Currently also contains source code for GlueGen tool; in
        process of being split into its own project)
www/   Web pages and older Java Web Start binaries for JOGL

Acknowledgments
---------------

Sun Microsystems, Inc. gratefully acknowledges that the initial
version of JOGL was authored and developed by Kenneth Bradley Russell
and Christopher John Kline.

Sun and the JOGL development team are grateful for the contributions
of all of the individuals who have advanced the project. Please
contact the project owners if your name is missing from this list.

Gerard Ziemski contributed the original port of JOGL to Mac OS X.

Rob Grzywinski and Artur Biesiadowski contributed the Ant build
support. Alex Radeski contributed the cpptasks support in the build
process.

Pepijn Van Eeckhoudt and Nathan Parker Burg contributed the Java port
of the GLU tessellator. Pepijn also contributed the initial version of
the FPSAnimator utility class.

User GKW on the javagaming.org forums contributed the substantial port
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

The following individuals made significant contributions to various
areas of the project:

Alban Cousinié
Athomas Goldberg
Yuri Vladimir Gushchin
Gregory Pierce
Carsten Weisse

Sun and the JOGL development team are grateful for the support of the
javagaming.org community, from where dozens, if not hundreds, of
individuals have contributed discussions, bug reports, bug fixes, and
other forms of support.
