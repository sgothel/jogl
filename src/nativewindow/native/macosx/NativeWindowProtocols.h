/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

#ifndef NATIVEWINDOWPROTOCOLS_H
#define NATIVEWINDOWPROTOCOLS_H 1
 
/** 
 * CALayer size needs to be set using the AWT component size. 
 * See detailed description in JAWTUtil.java and sync w/ changed. 
 */
#define NW_DEDICATEDFRAME_QUIRK_SIZE      ( 1 << 0 )

/** 
 * CALayer position needs to be set to zero.
 * See detailed description in JAWTUtil.java and sync w/ changed. 
 */
#define NW_DEDICATEDFRAME_QUIRK_POSITION  ( 1 << 1 )

/** 
 * CALayer position needs to be derived from AWT position.
 * in relation to super CALayer.
 * See detailed description in JAWTUtil.java and sync w/ changed.
 */
#define NW_DEDICATEDFRAME_QUIRK_LAYOUT ( 1 << 2 )

#import <Foundation/NSGeometry.h>

@protocol NWDedicatedFrame
- (void)setDedicatedFrame:(CGRect)dFrame quirks:(int)quirks;
@end

#endif /* NATIVEWINDOWPROTOCOLS_H_ */

