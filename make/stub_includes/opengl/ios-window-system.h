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
#include <UIKit/UIKit.h>
#include <OpenGLES/EAGLDrawable.h>
#include <QuartzCore/CAEAGLLayer.h>
#include <OpenGLES/EAGL.h>
#include <gluegen_stdint.h>

typedef int Bool;

EAGLContext * eaglCreateContext(EAGLRenderingAPI api);
EAGLContext * eaglCreateContextShared(EAGLRenderingAPI api, EAGLSharegroup* sharegroup);
Bool eaglDeleteContext(EAGLContext *ctx, Bool releaseOnMainThread);

EAGLRenderingAPI eaglGetRenderingAPI(EAGLContext* ctx);
EAGLSharegroup * eaglGetSharegroup(EAGLContext *ctx);
Bool eaglIsContextMultiThreaded(EAGLContext* ctx);
void eaglSetContextMultiThreaded(EAGLContext* ctx, Bool v); /* spawn off load to new GL worker thread if true */

EAGLContext* eaglGetCurrentContext(void);
Bool eaglMakeCurrentContext(EAGLContext* ctx);

Bool eaglBindDrawableStorageToRenderbuffer(EAGLContext* ctx, int renderbufferTarget, CAEAGLLayer /* EAGLDrawable */ * drawable);
Bool eaglPresentRenderbuffer(EAGLContext* ctx, int renderbufferTarget);

void* getProcAddress(const char *procName);
