#import <Cocoa/Cocoa.h>
#import <OpenGL/gl.h>
#import <OpenGL/CGLTypes.h>
#import <jni.h>

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) NSLog(@ __VA_ARGS__)
    // #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr)
#else
    #define DBG_PRINT(...)
#endif

#ifndef CGL_VERSION_1_3
    #warning this SDK doesn't support OpenGL profile
#endif

#import "macosx-window-system.h"

