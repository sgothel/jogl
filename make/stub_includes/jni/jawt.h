/*
 * @(#)jawt.h	1.11 05/11/17
 *
 * This C header file is derived from Sun Microsystem's Java SDK provided C header file
 * with the following copyright notice:
 *
 *   Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 *   SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * This version has complex comments removed and does not contain inlined algorithms etc, if any existed.
 * 
 * The original C header file was included to JOGL on Sat Jun 21 02:10:30 2008
 * (commit cbc45e816f4ee81031bffce19a99550681462a24) by Sun Microsystem's staff and were approved. 
 *
 * This C header file is included due to ensure compatibility with - and invocation of the JAWT protocol.
 * They are processed by GlueGen to create a Java binding for JAWT invocation only.
 * 
 * http://ftp.resource.org/courts.gov/c/F3/387/387.F3d.522.03-5400.html (36)
 * "Atari Games Corp. v. Nintendo of Am., Inc., Nos. 88-4805 & 89-0027, 1993 WL 207548, at *1 (N.D.Cal. May 18, 1993) ("Atari III") 
 * ("Program code that is strictly necessary to achieve current compatibility presents a merger problem, almost by definition, 
 * and is thus excluded from the scope of any copyright.")."
 *
 * http://eur-lex.europa.eu/LexUriServ/LexUriServ.do?uri=OJ:L:2009:111:0016:0022:EN:PDF
 * L 111/17 (10) and (15)
 */

#ifndef _JAVASOFT_JAWT_H_
#define _JAVASOFT_JAWT_H_

#include "jni.h"

#ifdef __cplusplus
extern "C" {
#endif

/*
 * AWT native interface (new in JDK 1.3)
 */

typedef struct jawt_Rectangle {
    jint x;
    jint y;
    jint width;
    jint height;
} JAWT_Rectangle;

struct jawt_DrawingSurface;

typedef struct jawt_DrawingSurfaceInfo {
    void* platformInfo;
    struct jawt_DrawingSurface* ds;
    JAWT_Rectangle bounds;
    jint clipSize;
    JAWT_Rectangle* clip;
} JAWT_DrawingSurfaceInfo;

#define JAWT_LOCK_ERROR                 0x00000001
#define JAWT_LOCK_CLIP_CHANGED          0x00000002
#define JAWT_LOCK_BOUNDS_CHANGED        0x00000004
#define JAWT_LOCK_SURFACE_CHANGED       0x00000008

typedef struct jawt_DrawingSurface {
    JNIEnv* env;
    jobject target;
    jint (JNICALL *Lock)
        (struct jawt_DrawingSurface* ds);
    JAWT_DrawingSurfaceInfo* (JNICALL *GetDrawingSurfaceInfo)
        (struct jawt_DrawingSurface* ds);
    void (JNICALL *FreeDrawingSurfaceInfo)
        (JAWT_DrawingSurfaceInfo* dsi);
    void (JNICALL *Unlock)
        (struct jawt_DrawingSurface* ds);
} JAWT_DrawingSurface;

typedef struct jawt {
    jint version;
    JAWT_DrawingSurface* (JNICALL *GetDrawingSurface)
        (JNIEnv* env, jobject target);
    void (JNICALL *FreeDrawingSurface)
        (JAWT_DrawingSurface* ds);
    /*
     * Since 1.4
     */
    void (JNICALL *Lock)(JNIEnv* env);
    /*
     * Since 1.4
     */
    void (JNICALL *Unlock)(JNIEnv* env);
    /*
     * Since 1.4
     */
    jobject (JNICALL *GetComponent)(JNIEnv* env, void* platformInfo);

} JAWT;

_JNI_IMPORT_OR_EXPORT_
jboolean JNICALL JAWT_GetAWT(JNIEnv* env, JAWT* awt);

#define JAWT_VERSION_1_3 0x00010003
#define JAWT_VERSION_1_4 0x00010004

#ifdef __cplusplus
} /* extern "C" */
#endif

#endif /* !_JAVASOFT_JAWT_H_ */
