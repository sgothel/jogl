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

#include "CAEAGLLayered.h"

@implementation CAEAGLUIView
// override
+ (Class)layerClass {
    return CAEAGLLayer.self;
}
/**
override class var layerClass : AnyClass {
    return CAEAGLLayer.self
}
*/
+ (CAEAGLUIView*) findCAEAGLUIView: (UIWindow*) win 
                  startIdx: (int) sidx
{
    NSArray* subviews = [win subviews];
    if(NULL != subviews) {
        for(int i=sidx; i<[subviews count]; i++) {
            UIView* sub = [subviews objectAtIndex: i];
            if( [sub isKindOfClass:[CAEAGLUIView class]] ) {
               return (CAEAGLUIView*)sub; 
            }
        }
    }
    return (CAEAGLUIView*)NULL;
}
+ (UIView*) getUIView: (UIWindow*) win 
                  startIdx: (int) sidx
{
    NSArray* subviews = [win subviews];
    if(NULL != subviews) {
        if( sidx<[subviews count] ) {
            return [subviews objectAtIndex: sidx];
        }
    }
    return (UIView*)NULL;
}
@end /* implementation CAEAGLUIView */
