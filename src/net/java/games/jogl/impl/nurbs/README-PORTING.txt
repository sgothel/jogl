This is a currently incomplete port of SGI's GLU NURBS library from
C++ to Java. There are a few reasons for doing such a port:

 - The C interface is structured around function pointers. It is
   generally difficult to bind such interfaces up to Java.

 - Some people have reported crashes on certain Linux distributions
   when trying to use any routines out of the C GLU library. To date
   we have not been able to diagnose the root cause of these failures.
   Porting the code involved from C++ to Java has solved these
   problems.

The port so far has been started in the internals/ directory. The C++
sources have been gone through roughly alphabetically and
transliterated into the appropriate files. The large Subdivider class
was the current focus of attention at the time of this writing, and a
closer look indicates that at least a few classes were skipped on the
way down to some of the Subdivider's sources like intersect.cc. It may
be a good idea to continue the port in this directory first, since it
looks like the other directories' sources are built on top of these
and it would be good to firm up the Java interfaces for the internals
(and perhaps get the sources to compile) before porting lots of code
built on top of them.

A couple of notes on the translation:

 - All object pool classes have been removed. The intention is to have
   a static allocate() method on the appropriate classes which will
   instantiate populated arrays of these types (not just arrays of
   null references). See uses of TrimVertex.allocate().

 - There are a significant number of places in the original C++ code
   where pointer arithmetic is used. Some of these are not obvious
   until the code has been ported and examined. Bin.java was a good
   example of this where the algorithms needed some restructuring. At
   the time of this writing intersect.cc was in the process of being
   ported and it wasn't clear whether we would need to change some of
   the APIs or add more utility routines to be able to do pointer
   arithmetic on, for example, the TrimVertex arrays returned from the
   allocate() routine.
