/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 * 
 */

#import <AppKit/AppKit.h>
#import "jni.h"

@interface NewtView : NSView
{
    jobject javaWindowObject;

    // This is set while messages are being dispatched and cleared afterward
    JNIEnv* env;
}

/* Set during event dispatching cycle */
- (void) setJNIEnv: (JNIEnv*) env;
- (JNIEnv*) getJNIEnv;

/* Register or deregister (NULL) the java Window object, 
   ie, if NULL, no events are send */
- (void) setJavaWindowObject: (jobject) javaWindowObj;
- (jobject) getJavaWindowObject;

- (void) rightMouseDown: (NSEvent*) theEvent;

@end

@interface NewtMacWindow : NSWindow
{
}

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jobject) clazz;

- (id) initWithContentRect: (NSRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSBackingStoreType) bufferingType
       screen:(NSScreen *)screen;

@end
