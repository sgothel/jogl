/**
 * This C header file is derived from Apple's Java SDK provided C header file
 * with the following copyright notice:
 *
 *   Copyright (c) 2002 Apple computer Inc. All rights reserved.
 * 
 * This version has complex comments removed and does not contain inlined algorithms etc, if any existed.
 *
 * The original C header file was included to JOGL on Mon Jun 15 22:57:38 2009
 * (commit cbc45e816f4ee81031bffce19a99550681462a24) by Sun Microsystem's staff and were approved. 
 *
 * This C header file is included due to ensure compatibility with - and invocation of the JAWT protocol.
 * They are processed by GlueGen to create a Java binding for JAWT invocation only.
 * 
 * http://ftp.resource.org/courts.gov/c/F3/387/387.F3d.522.03-5400.html (36)
 * "Atari Games Corp. v. Nintendo of Am., Inc., Nos. 88-4805 & 89-0027, 1993 WL 207548, at *1 (N.D.Cal. May 18, 1993) ("Atari III") 
 * ("Program code that is strictly necessary to achieve current compatibility presents a merger problem, almost by definition, 
 * and is thus excluded from the scope of any copyright.")."
 *
 * http://eur-lex.europa.eu/LexUriServ/LexUriServ.do?uri=OJ:L:2009:111:0016:0022:EN:PDF
 * L 111/17 (10) and (15)
 */

#ifndef _JAVASOFT_JAWT_MD_H_
#define _JAVASOFT_JAWT_MD_H_

#include <jawt.h>
#include <AppKit/NSView.h>
#include <QuartzCore/CALayer.h>

#ifdef __cplusplus
extern "C" {
#endif

/** 
 * JAWT_DrawingSurfaceInfo.getPlatformInfo()
 *
 * Only if not JAWT_SurfaceLayers, see below!
 */
typedef struct JAWT_MacOSXDrawingSurfaceInfo
{
    /** the view is guaranteed to be valid only for the duration of Component.paint method */
    NSView *cocoaViewRef; 
}
JAWT_MacOSXDrawingSurfaceInfo;

#ifndef __GLUEGEN__

    #define JAWT_MACOSX_USE_CALAYER 0x80000000

    /** Java7 and Java6 (OSX >= 10.6.4) CALayer surface if provided (Bit JAWT_MACOSX_USE_CALAYER set in the JAWT version) */
    @protocol JAWT_SurfaceLayers
    @property (readwrite, retain) CALayer *layer;
    @property (readonly) CALayer *windowLayer;
    @end

#endif __GLUEGEN__

#ifdef __cplusplus
}
#endif

#endif /* !_JAVASOFT_JAWT_MD_H_ */
