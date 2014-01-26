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
package com.jogamp.graph.font;

import java.io.IOException;


public interface FontSet {

    /** Font family REGULAR **/
    public static final int FAMILY_REGULAR    = 0;

    /** Font family LIGHT **/
    public static final int FAMILY_LIGHT      = 1;

    /** Font family MEDIUM **/
    public static final int FAMILY_MEDIUM     = 2;

    /** Font family CONDENSED **/
    public static final int FAMILY_CONDENSED  = 3;

    /** Font family MONO **/
    public static final int FAMILY_MONOSPACED = 4;

    /** SERIF style/family bit flag. Fallback to Sans Serif. */
    public static final int STYLE_SERIF       = 1 << 1;

    /** BOLD style bit flag */
    public static final int STYLE_BOLD        = 1 << 2;

    /** ITALIC style bit flag */
    public static final int STYLE_ITALIC      = 1 << 3;

    Font getDefault() throws IOException ;

    Font get(int family, int stylebits) throws IOException ;
}
