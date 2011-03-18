/*
 * @(#)jni_md.h	1.15 05/11/17
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

#ifndef _JAVASOFT_JNI_MD_H_
#define _JAVASOFT_JNI_MD_H_

#define JNIEXPORT __declspec(dllexport)
#define JNIIMPORT __declspec(dllimport)
#define JNICALL __stdcall

typedef long jint;
typedef __int64 jlong;
typedef signed char jbyte;

#endif /* !_JAVASOFT_JNI_MD_H_ */
