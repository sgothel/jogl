//
//  jawt_md.m
//
//  Copyright (c) 2002 Apple computer Inc. All rights reserved.
//

#ifndef _JAVASOFT_JAWT_MD_H_
#define _JAVASOFT_JAWT_MD_H_

#include "jawt.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct JAWT_MacOSXDrawingSurfaceInfo
{
	int	cgWindowID; // CGSConnectionID
	int	cgConnectionID; // CGSWindowID
	int	cgContextRef; // CGContextRef
    
    int cocoaWindowRef; // NSWindow*
    
	int	carbonWindowRef; // WindowRef (HIToolbox/MacWindows.h)
    
	int	windowX; // int
	int	windowY; // int
	int	windowWidth; // int
	int	windowHeight; // int
}
JAWT_MacOSXDrawingSurfaceInfo;

#ifdef __cplusplus
}
#endif

#endif /* !_JAVASOFT_JAWT_MD_H_ */