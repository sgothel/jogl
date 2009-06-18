/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 */

package com.sun.nativewindow.impl.jvm;

import java.nio.ByteBuffer;
import com.sun.nativewindow.impl.*;

/**
 * Currently this tool works around the Hotspot race condition bugs:
 <PRE>
     4395095 JNI access to java.nio DirectBuffer constructor/accessor
     6852404 Race condition in JNI Direct Buffer access and creation routines
 </PRE>
 *
 * Make sure to initialize this class as soon as possible,
 * before doing any multithreading work.
 *
 */
public class JVMUtil {
    private static final boolean DEBUG = Debug.debug("JVMUtil");

    static {
        NativeLibLoaderBase.loadNativeWindow("jvm");

        ByteBuffer buffer = InternalBufferUtil.newByteBuffer(64);
        if( ! initialize(buffer) ) {
            throw new RuntimeException("Failed to initialize the JVMUtil "+Thread.currentThread().getName());
        }
        if(DEBUG) {
            Exception e = new Exception("JVMUtil.initSingleton() .. initialized "+Thread.currentThread().getName());
            e.printStackTrace();
        }
    }

    public static void initSingleton() {
    }

    private JVMUtil() {}

    private static native boolean initialize(java.nio.ByteBuffer buffer);
}

