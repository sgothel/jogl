/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

#ifndef _WINDOW_H_
#define _WINDOW_H_

#define FLAG_CHANGE_PARENTING       ( 1 <<  0 )
#define FLAG_CHANGE_DECORATION      ( 1 <<  1 )
#define FLAG_CHANGE_FULLSCREEN      ( 1 <<  2 )
#define FLAG_CHANGE_ALWAYSONTOP     ( 1 <<  3 )
#define FLAG_CHANGE_VISIBILITY      ( 1 <<  4 )

#define FLAG_HAS_PARENT             ( 1 <<  8 )
#define FLAG_IS_UNDECORATED         ( 1 <<  9 )
#define FLAG_IS_FULLSCREEN          ( 1 << 10 )
#define FLAG_IS_ALWAYSONTOP         ( 1 << 11 )
#define FLAG_IS_VISIBLE             ( 1 << 12 )

#define TST_FLAG_CHANGE_PARENTING(f)   ( 0 != ( (f) & FLAG_CHANGE_PARENTING ) ) 
#define TST_FLAG_CHANGE_DECORATION(f)  ( 0 != ( (f) & FLAG_CHANGE_DECORATION ) ) 
#define TST_FLAG_CHANGE_FULLSCREEN(f)  ( 0 != ( (f) & FLAG_CHANGE_FULLSCREEN ) ) 
#define TST_FLAG_CHANGE_ALWAYSONTOP(f) ( 0 != ( (f) & FLAG_CHANGE_ALWAYSONTOP ) ) 
#define TST_FLAG_CHANGE_VISIBILITY(f)  ( 0 != ( (f) & FLAG_CHANGE_VISIBILITY ) ) 

#define TST_FLAG_HAS_PARENT(f)        ( 0 != ( (f) & FLAG_HAS_PARENT ) ) 
#define TST_FLAG_IS_UNDECORATED(f)    ( 0 != ( (f) & FLAG_IS_UNDECORATED ) ) 
#define TST_FLAG_IS_FULLSCREEN(f)     ( 0 != ( (f) & FLAG_IS_FULLSCREEN ) ) 
#define TST_FLAG_IS_FULLSCREEN(f)     ( 0 != ( (f) & FLAG_IS_FULLSCREEN ) ) 
#define TST_FLAG_IS_ALWAYSONTOP(f)    ( 0 != ( (f) & FLAG_IS_ALWAYSONTOP ) ) 
#define TST_FLAG_IS_VISIBLE(f)        ( 0 != ( (f) & FLAG_IS_VISIBLE ) ) 

#endif
