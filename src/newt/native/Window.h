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

#define FLAG_CHANGE_VISIBILITY      ( 1 << 31 )
#define FLAG_CHANGE_VISIBILITY_FAST ( 1 << 30 )
#define FLAG_CHANGE_PARENTING       ( 1 << 29 )
#define FLAG_CHANGE_DECORATION      ( 1 << 28 )
#define FLAG_CHANGE_ALWAYSONTOP     ( 1 << 27 )
#define FLAG_CHANGE_ALWAYSONBOTTOM  ( 1 << 26 )
#define FLAG_CHANGE_STICKY          ( 1 << 25 )
#define FLAG_CHANGE_RESIZABLE       ( 1 << 24 )
#define FLAG_CHANGE_MAXIMIZED_VERT  ( 1 << 23 )
#define FLAG_CHANGE_MAXIMIZED_HORZ  ( 1 << 22 )
#define FLAG_CHANGE_FULLSCREEN      ( 1 << 21 )

#define FLAG_IS_VISIBLE             ( 1 <<  0 )
#define FLAG_IS_AUTOPOSITION        ( 1 <<  1 )
#define FLAG_IS_CHILD               ( 1 <<  2 )
#define FLAG_IS_FOCUSED             ( 1 <<  3 )
#define FLAG_IS_UNDECORATED         ( 1 <<  4 )
#define FLAG_IS_ALWAYSONTOP         ( 1 <<  5 )
#define FLAG_IS_ALWAYSONBOTTOM      ( 1 <<  6 )
#define FLAG_IS_STICKY              ( 1 <<  7 )
#define FLAG_IS_RESIZABLE           ( 1 <<  8 )
#define FLAG_IS_MAXIMIZED_VERT      ( 1 <<  9 )
#define FLAG_IS_MAXIMIZED_HORZ      ( 1 << 10 )
#define FLAG_IS_FULLSCREEN          ( 1 << 11 )
#define FLAG_IS_POINTERVISIBLE      ( 1 << 12 )
#define FLAG_IS_POINTERCONFINED     ( 1 << 13 )
#define FLAG_IS_FULLSCREEN_SPAN     ( 1 << 14 )

#define TST_FLAG_CHANGE_VISIBILITY(f)  ( 0 != ( (f) & FLAG_CHANGE_VISIBILITY ) ) 
#define TST_FLAG_CHANGE_VISIBILITY_FAST(f) ( 0 != ( (f) & FLAG_CHANGE_VISIBILITY_FAST ) ) 
#define TST_FLAG_CHANGE_PARENTING(f)   ( 0 != ( (f) & FLAG_CHANGE_PARENTING ) ) 
#define TST_FLAG_CHANGE_DECORATION(f)  ( 0 != ( (f) & FLAG_CHANGE_DECORATION ) ) 
#define TST_FLAG_CHANGE_ALWAYSONTOP(f) ( 0 != ( (f) & FLAG_CHANGE_ALWAYSONTOP ) ) 
#define TST_FLAG_CHANGE_ALWAYSONBOTTOM(f) ( 0 != ( (f) & FLAG_CHANGE_ALWAYSONBOTTOM ) ) 
#define TST_FLAG_CHANGE_ALWAYSONANY(f) ( 0 != ( (f) & ( FLAG_CHANGE_ALWAYSONTOP | FLAG_CHANGE_ALWAYSONBOTTOM ) ) ) 
#define TST_FLAG_CHANGE_STICKY(f)      ( 0 != ( (f) & FLAG_CHANGE_STICKY ) ) 
#define TST_FLAG_CHANGE_RESIZABLE(f)   ( 0 != ( (f) & FLAG_CHANGE_RESIZABLE ) ) 
#define TST_FLAG_CHANGE_MAXIMIZED_VERT(f) ( 0 != ( (f) & FLAG_CHANGE_MAXIMIZED_VERT ) ) 
#define TST_FLAG_CHANGE_MAXIMIZED_HORZ(f) ( 0 != ( (f) & FLAG_CHANGE_MAXIMIZED_HORZ ) ) 
#define TST_FLAG_CHANGE_MAXIMIZED_ANY(f)  ( 0 != ( (f) & ( FLAG_CHANGE_MAXIMIZED_VERT | FLAG_CHANGE_MAXIMIZED_HORZ ) ) ) 
#define TST_FLAG_CHANGE_FULLSCREEN(f)  ( 0 != ( (f) & FLAG_CHANGE_FULLSCREEN ) ) 

#define TST_FLAG_IS_VISIBLE(f)         ( 0 != ( (f) & FLAG_IS_VISIBLE ) ) 
#define TST_FLAG_IS_AUTOPOSITION(f)    ( 0 != ( (f) & FLAG_IS_AUTOPOSITION ) ) 
#define TST_FLAG_IS_CHILD(f)           ( 0 != ( (f) & FLAG_IS_CHILD ) ) 
#define TST_FLAG_IS_FOCUSED(f)         ( 0 != ( (f) & FLAG_IS_FOCUSED ) ) 
#define TST_FLAG_IS_UNDECORATED(f)     ( 0 != ( (f) & FLAG_IS_UNDECORATED ) ) 
#define TST_FLAG_IS_ALWAYSONTOP(f)     ( 0 != ( (f) & FLAG_IS_ALWAYSONTOP ) ) 
#define TST_FLAG_IS_ALWAYSONBOTTOM(f)  ( 0 != ( (f) & FLAG_IS_ALWAYSONBOTTOM ) ) 
#define TST_FLAG_IS_ALWAYSONANY(f)     ( 0 != ( (f) & ( FLAG_IS_ALWAYSONTOP | FLAG_IS_ALWAYSONBOTTOM ) ) ) 
#define TST_FLAG_IS_STICKY(f)          ( 0 != ( (f) & FLAG_IS_STICKY ) ) 
#define TST_FLAG_IS_RESIZABLE(f)       ( 0 != ( (f) & FLAG_IS_RESIZABLE ) ) 
#define TST_FLAG_IS_MAXIMIZED_VERT(f)  ( 0 != ( (f) & FLAG_IS_MAXIMIZED_VERT ) ) 
#define TST_FLAG_IS_MAXIMIZED_HORZ(f)  ( 0 != ( (f) & FLAG_IS_MAXIMIZED_HORZ ) ) 
#define TST_FLAG_IS_MAXIMIZED_ANY(f)   ( 0 != ( (f) & ( FLAG_IS_MAXIMIZED_VERT | FLAG_IS_MAXIMIZED_HORZ ) ) ) 
#define TST_FLAG_IS_FULLSCREEN(f)      ( 0 != ( (f) & FLAG_IS_FULLSCREEN ) ) 
#define TST_FLAG_IS_FULLSCREEN_SPAN(f) ( 0 != ( (f) & FLAG_IS_FULLSCREEN_SPAN ) ) 

#endif
