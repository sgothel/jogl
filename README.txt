Organization of the JOGL source tree
------------------------------------

doc/                Build and user documentation
make/               Ant build scripts,
                    see top of build.xml for brief invocation instructions
make/config         Configuration files for glue code generation
make/stub_includes  Header files for glue code generation

src/                Java and native source code for:
src/jogl            - JOGL
src/nativewindow    - NativeWindow Interface
src/newt            - NEWT
src/junit           - Unit test cases

www/                Web pages

NativeWindow and NEWT may be build seperately.

Contact Us
---------------

Web                http://jogamp.org/
Forum/Mailinglist  http://forum.jogamp.org/
JogAmp Channel     server: conference.jabber.org room: jogamp
Repository         http://jogamp.org/git/
Email              mediastream _at_ jogamp _dot_ org

Acknowledgments
---------------

The JogAmp Community is gratefully acknowledges that the initial
version of JOGL was authored and developed by Kenneth Bradley Russell
and Christopher John Kline.

The JogAmp Community is grateful for all the contributions
of all of the individuals who have advanced the project. 
For sure we are not able to list all of them here.
Please contact us if you like to be added to this list.

This list can hardly cover all contributors and their contributions.
You may like to check the author field of our SCM.

(Chronological)

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
Teamed up with Rami Santina, the new graph package was realized,
exposing generic curve, text and UI support.

Rami Santina researched and implemented the math behind the new 
graph package [RSantina], etc.

The following individuals made significant contributions to various
areas of the project (Alphabetical):

Michael Bien
Alban Cousinié
Athomas Goldberg
Yuri Vladimir Gushchin
Gregory Pierce
Dominik Ströhlein (DemoscenePassivist)
Wade Walker
Carsten Weisse

The JogAmp Community is grateful for the support of the
javagaming.org community and it's own JogAmp forum, 
from where dozens, if not hundreds, of individuals have 
contributed discussions, bug reports, bug fixes, and other forms of support.

+++

[RSantina]: Resolution Independent NURBS Curve Rendering using Programmable Graphics Pipeline

