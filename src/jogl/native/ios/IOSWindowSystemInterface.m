/**
 * Copyright 2019 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
#include <AvailabilityMacros.h>

#import "IOSWindowSystemInterface.h"

EAGLContext * eaglCreateContext(EAGLRenderingAPI api) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    DBG_PRINT("createEAGLContext.0: api %d\n", api);

    EAGLContext* ctx = [[EAGLContext alloc] initWithAPI:api];
        
    DBG_PRINT("createEAGLContext.X: ctx: %p\n", ctx);
    [pool release];
    return ctx;
}

EAGLContext * eaglCreateContextShared(EAGLRenderingAPI api, EAGLSharegroup* sharegroup) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    DBG_PRINT("createEAGLContext.0: api %d, sharegroup %p\n", api, sharegroup);

    EAGLContext* ctx = [[EAGLContext alloc] initWithAPI:api sharegroup:sharegroup];
        
    DBG_PRINT("createEAGLContext.X: ctx: %p\n", ctx);
    [pool release];
    return ctx;
}

Bool eaglDeleteContext(EAGLContext *ctx, Bool releaseOnMainThread) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    DBG_PRINT("deleteEAGLContext.0: ctx %p, releaseOnMainThread %d\n", ctx, releaseOnMainThread);
    if(releaseOnMainThread && NO == [NSThread isMainThread]) {
        [ctx performSelectorOnMainThread:@selector(release) withObject:nil waitUntilDone:NO];
    } else {
        // ??? On OSX would hangs for ~10s for 1 of a shared context set or offscreen context, set releaseOnMainThread=true
        [ctx release]; 
    }
    [pool release];
    return true;
}

EAGLRenderingAPI eaglGetRenderingAPI(EAGLContext* ctx) {
    return [ctx API];
}
EAGLSharegroup * eaglGetSharegroup(EAGLContext *ctx) {
    return [ctx sharegroup];
}
Bool eaglIsContextMultiThreaded(EAGLContext* ctx) {
    return [ctx isMultiThreaded];
}
void eaglSetContextMultiThreaded(EAGLContext* ctx, Bool v) {
    [ctx setMultiThreaded: v];
}

EAGLContext* eaglGetCurrentContext(void) {
    return [EAGLContext currentContext];
}
Bool eaglMakeCurrentContext(EAGLContext* ctx) {
    return [EAGLContext setCurrentContext: ctx];
}
Bool eaglBindDrawableStorageToRenderbuffer(EAGLContext* ctx, int renderbufferTarget, CAEAGLLayer /* EAGLDrawable */ * drawable) {
    return [ctx renderbufferStorage: renderbufferTarget fromDrawable: drawable];
}
Bool eaglPresentRenderbuffer(EAGLContext* ctx, int renderbufferTarget) {
    return [ctx presentRenderbuffer: renderbufferTarget];
}

#include <dlfcn.h>
Bool imagesInitialized = false;
static char libGLESStr[] = "/System/Library/Frameworks/OpenGLES.framework/Libraries/libGLES.dylib";
static void * *libGLESImage;
void* getProcAddress(const char *procname) {
  if (imagesInitialized == false) {
    imagesInitialized = true;
    libGLESImage = dlopen(libGLESStr, RTLD_LAZY | RTLD_GLOBAL);
  }
  if(NULL == libGLESImage) {
    return NULL;
  }
    
  char underscoreName[512] = "_";
  void * res = NULL;
  strcat(underscoreName, procname);
    
  res = dlsym(libGLESImage, underscoreName);
  if( NULL == res ) {
    // try smth else ..
  }
  return res;
}

