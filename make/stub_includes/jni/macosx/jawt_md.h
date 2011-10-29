//
//  jawt_md.h
//
//  Copyright (c) 2002 Apple computer Inc. All rights reserved.
//

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

/** 
 * JAWT_DrawingSurfaceInfo.getPlatformInfo()
 *
 * >= 10.6.4 if JAWT_MACOSX_USE_CALAYER is set in JAWT version
 */
typedef struct JAWT_SurfaceLayers
{
    CALayer *layer;
}
JAWT_SurfaceLayers;

#ifdef __cplusplus
}
#endif

#endif /* !_JAVASOFT_JAWT_MD_H_ */
