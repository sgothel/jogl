# JOGL, High-Performance Graphics Binding for Java™

[Original document location](https://jogamp.org/cgit/jogl.git/about/)

## Git Repository
This project's canonical repositories is hosted on [JogAmp](https://jogamp.org/cgit/jogl.git/).

## Overview
The [*JOGL project*](https://jogamp.org/jogl/www/) hosts the development of high-performance graphics binding for Java™, and is designed to provide hardware-supported 3D graphics and multimedia to applications written in Java™.

JOGL provides full access to the APIs in the OpenGL® [ 1.0 .. 4.5 ], ES [ 1.0 .. 3.2 ] and EGL [ 1.0 .. 1.5 ] specification as well as nearly all vendor extensions.
[OpenGL Evolution & JOGL](https://jogamp.org/jogl/doc/Overview-OpenGL-Evolution-And-JOGL.html) and this API Specification may give you a brief overview.

JOGL also embraces multimedia technology and binds to FFMpeg as well as to other media libraries providing a unified access API with JOAL. Further, stereo devices are supported in a generic fashion as well as for early OculusVR.

JOGL integrates with the AWT, Swing, OpenJFX and SWT widget sets, as well as with custom windowing toolkits using the NativeWindow API.

JOGL also provides its own [native windowing toolkit, NEWT](https://jogamp.org/jogl/doc/NEWT-Overview.html), running on top of X11, Windows, MacOS and even on bare-metal console mode without a windowing system.

JOGL is part of [the JogAmp project](https://jogamp.org).

[List of proposed work items & use-cases](https://jogamp.org/wiki/index.php?title=SW_Tracking_Report_Feature_Objectives_Overview)
and [current blog entries](https://jausoft.com/blog/tag/jogamp/).

**The JogAmp project needs funding and we offer [commercial support](https://jogamp.org/wiki/index.php?title=Maintainer_and_Contacts#Commercial_Support)!**<br/>
Please contact [Göthel Software (Jausoft)](https://jausoft.com/).

## Organization of the JOGL source tree
```
doc/                Build and user documentation
make/               Ant build scripts,
                    see top of build.xml for brief invocation instructions
make/config         Configuration files for glue code generation
make/stub_includes  Header files for glue code generation

src/                Java and native source code for:
src/jogl            - JOGL
src/nativewindow    - NativeWindow Interface
src/newt            - NEWT
src/graphui         - GraphUI
src/demos           - Demos
src/test            - Unit tests

www/                Web pages
```

NativeWindow, NEWT and GraphUI might be build seperately.

## Contact Us
- JogAmp             [http://jogamp.org/](http://jogamp.org/)
- JOGL Web           [http://jogamp.org/](http://jogamp.org/jogl/)
- Forum/Mailinglist  [http://forum.jogamp.org/](http://forum.jogamp.org/)
- Repository         [http://jogamp.org/git/](http://jogamp.org/git/)
- Wiki               [https://jogamp.org/wiki/](https://jogamp.org/wiki/)
- Maintainer         [https://jogamp.org/wiki/index.php/Maintainer_and_Contacts](https://jogamp.org/wiki/index.php/Maintainer_and_Contacts)
- Email              sgothel _at_ jausoft _dot_ com


## Acknowledgments
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
He teamed up with Rami Santina, realizing the new graph package
exposing generic curve, text and UI support.

Rami Santina researched and implemented the math behind the new 
graph package [RSantina](https://jausoft.com/blog/2011/10/05/jogljogamp-red-square-moscow-nurbs-graphicon2011/), etc.

The following individuals made significant contributions to various
areas of the project (Alphabetical):

- Michael Bien
- Alban Cousinié
- Athomas Goldberg
- Julien Gouesse
- Yuri Vladimir Gushchin
- Gregory Pierce
- Dominik Ströhlein (DemoscenePassivist)
- Wade Walker
- Carsten Weisse

The JogAmp Community is grateful for the support of the
javagaming.org community and it's own JogAmp forum, 
from where dozens, if not hundreds, of individuals have 
contributed discussions, bug reports, bug fixes, and other forms of support.

+++

*RSantina:* [Resolution Independent NURBS Curve Rendering using Programmable Graphics Pipeline](https://jausoft.com/blog/2011/10/05/jogljogamp-red-square-moscow-nurbs-graphicon2011/)

