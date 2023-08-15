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

JOGL contains [Graph](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/graph/curve/OutlineShape.html), [a resolution-independent GPU NURBS curve renderer](https://jausoft.com/blog/2023/02/22/reimagine-java-on-desktop-bare-metal-devices/) suitable for desktop and embedded devices and supporting [text type rendering](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/com/jogamp/graph/curve/opengl/TextRegionUtil.html) \[ [paper](https://jogamp.org/doc/gpunurbs2011/p70-santina.pdf), [slides](https://jogamp.org/doc/gpunurbs2011/graphicon2011-slides.pdf) \].
Graph is used in the contained [GraphUI, enabling immersive UI within the 3D scene](https://jausoft.com/blog/tag/graph_type_rendering/).

JOGL is part of [the JogAmp project](https://jogamp.org).

[List of proposed work items & use-cases](https://jogamp.org/wiki/index.php?title=SW_Tracking_Report_Feature_Objectives_Overview)
and [current blog entries](https://jausoft.com/blog/tag/jogamp/).

**The JogAmp project needs funding and we offer [commercial support](https://jogamp.org/wiki/index.php?title=Maintainer_and_Contacts#Commercial_Support)!**<br/>
Please contact [Göthel Software (Jausoft)](https://jausoft.com/).

## Organization of the JOGL source tree
```
doc/                Build and user documentation
make/               Ant build scripts
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
- Forum/Mailinglist  [http://forum.jogamp.org/](http://forum.jogamp.org/)
- Maintainer         [https://jogamp.org/wiki/index.php/Maintainer_and_Contacts](https://jogamp.org/wiki/index.php/Maintainer_and_Contacts)
- Sven's Blog        [https://jausoft.com/blog/tag/jogamp/](https://jausoft.com/blog/tag/jogamp/)
- Email              sgothel _at_ jausoft _dot_ com

## References
- [JogAmp Home](https://jogamp.org/)
- [JOGL Home](https://jogamp.org/jogl/www/)
- [Git Repository](https://jogamp.org/cgit/jogl.git/about/)
- [OpenGL Evolution & JOGL](https://jogamp.org/jogl/doc/Overview-OpenGL-Evolution-And-JOGL.html)
- [Mapping of OpenGL Profiles to Interfaces](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/overview-summary.html#overview_description)
  - [OpenGL API Inclusion Criteria](https://jogamp.org/deployment/jogamp-next/javadoc/jogl/javadoc/overview-summary.html#GLAPIInclusionCriteria)
  - [UML Diagram](https://jogamp.org/jogl/doc/uml/html/index-withframe.html)
- [JOGL and OpenGL Divergence](https://jogamp.org/jogl/doc/OpenGL_API_Divergence.html)
- [How To Build](https://jogamp.org/jogl/doc/HowToBuild.html)
- [Wiki](https://jogamp.org/wiki/)


## JogAmp History & Milestones
Bottom line, too much work has been performed to be listed here.    
However, let's have a few sentimental points listed and we may add a few more as we go.

### *OpenGL™ for Java™ (GL4Java)*
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


