/*
 * Copyright (c) 2007 NVIDIA Corporation.  All rights reserved.
 *
 * NVIDIA Corporation and its licensors retain all intellectual property
 * and proprietary rights in and to this software, related documentation
 * and any modifications thereto.  Any use, reproduction, disclosure or
 * distribution of this software and related documentation without an express
 * license agreement from NVIDIA Corporation is strictly prohibited.
 */

/*
* Copyright (c) 2007 The Khronos Group Inc.
*
* Permission is hereby granted, free of charge, to any person obtaining
* a copy of this software and associated documentation files (the
* "Software"), to deal in the Software without restriction, including
* without limitation the rights to use, copy, modify, merge, publish,
* distribute, sublicense, and/or sell copies of the Software, and to
* permit persons to whom the Software is furnished to do so, subject
* to the following conditions:
* The above copyright notice and this permission notice shall be included
* in all copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
* IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
* CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
* TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
* SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
*/


/* NVIDIA KD/kdplatform.h for OpenKODE Core 1.0 Final candidate (draft 5070) */

#ifndef __kdplatform_h_
#define __kdplatform_h_

#define KD_API
#define KD_APIENTRY

typedef int KDint32;
typedef unsigned int KDuint32;
#if defined(_MSC_VER)
typedef signed __int64 KDint64;
typedef unsigned __int64 KDuint64;
#else
typedef long long KDint64;
typedef unsigned long long KDuint64;
#endif
typedef short KDint16;
typedef unsigned short KDuint16;
typedef unsigned long KDuintptr;
typedef unsigned long KDsize;
typedef long KDssize;
#define KDINT_MIN (-0x7fffffff-1)
#define KDINT_MAX 0x7fffffff
#define KDUINT_MAX 0xffffffffU
#define KDINT64_MIN (-0x7fffffffffffffffLL-1)
#define KDINT64_MAX 0x7fffffffffffffffLL
#define KDUINT64_MAX 0xffffffffffffffffULL
#define KDSIZE_MAX (~(unsigned long)0)
#define KDSSIZE_MAX ((long)KDSIZE_MAX/2)
#define KDSSIZE_MIN (-1 - KDSSIZE_MAX)
#define KDUINTPTR_MAX KDSIZE_MAX
#define KD_NORETURN
#define KD_WINDOW_SUPPORTED
#ifdef KD_NDEBUG
#define kdAssert(c)
#else
#define kdAssert(c) ((void)( (c) ? 0 : (kdHandleAssertion(#c, __FILE__, __LINE__), 0)))
#endif

#define KD_INFINITY_BITS 0x7f800000u
#define KD_NEGATIVE_ZERO_BITS 0x80000000u
#define KD_INFINITY (kdBitsToFloatNV(KD_INFINITY_BITS))
#define KD_NEGATIVE_ZERO (kdBitsToFloatNV(KD_NEGATIVE_ZERO_BITS))

KD_API float KD_APIENTRY kdBitsToFloatNV(KDuint32 x);

#endif /* __kdplatform_h_ */

