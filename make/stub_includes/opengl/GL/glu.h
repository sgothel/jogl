/*
 * This file is was based upon the OpenGL 1.2.1 Sample implementation publised
 * by SGI; as such the original license information is preserved below.
 *
 */

/*
** License Applicability. Except to the extent portions of this file are
** made subject to an alternative license as permitted in the SGI Free
** Software License B, Version 1.1 (the "License"), the contents of this
** file are subject only to the provisions of the License. You may not use
** this file except in compliance with the License. You may obtain a copy
** of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
** Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
** 
** http://oss.sgi.com/projects/FreeB
** 
** Note that, as provided in the License, the Software is distributed on an
** "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
** DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
** CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
** PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
** 
** NOTE:  The Original Code (as defined below) has been licensed to Sun
** Microsystems, Inc. ("Sun") under the SGI Free Software License B
** (Version 1.1), shown above ("SGI License").   Pursuant to Section
** 3.2(3) of the SGI License, Sun is distributing the Covered Code to
** you under an alternative license ("Alternative License").  This
** Alternative License includes all of the provisions of the SGI License
** except that Section 2.2 and 11 are omitted.  Any differences between
** the Alternative License and the SGI License are offered solely by Sun
** and not by SGI.
**
** Original Code. The Original Code is: OpenGL Sample Implementation,
** Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
** Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
** Copyright in any portions created by third parties is as indicated
** elsewhere herein. All Rights Reserved.
** 
** Additional Notice Provisions: This software was created using the
** OpenGL(R) version 1.2.1 Sample Implementation published by SGI, but has
** not been independently verified as being compliant with the OpenGL(R)
** version 1.2.1 Specification.
*/

#ifndef __glu_h__
#define __glu_h__

#include <GL/gl.h>

#ifndef APIENTRY
#define APIENTRY
#endif
#ifndef APIENTRYP
#define APIENTRYP APIENTRY*
#endif
#ifndef GLAPI
#define GLAPI extern
#endif

#ifdef __cplusplus
extern "C" {
#endif

/*************************************************************/

/* Version */
#define GLU_VERSION_1_1                    1
#define GLU_VERSION_1_2                    1
#define GLU_VERSION_1_3                    1

#ifndef GLU_VERSION_1_X

/* Extensions */
#define GLU_EXT_object_space_tess          1
#define GLU_EXT_nurbs_tessellator          1

/* Boolean */
#define GLU_FALSE                          0
#define GLU_TRUE                           1

/* StringName */
#define GLU_VERSION                        100800
#define GLU_EXTENSIONS                     100801

/* ErrorCode */
#define GLU_INVALID_ENUM                   100900
#define GLU_INVALID_VALUE                  100901
#define GLU_OUT_OF_MEMORY                  100902
#define GLU_INVALID_OPERATION              100904

/* NurbsDisplay */
/*      GLU_FILL */
#define GLU_OUTLINE_POLYGON                100240
#define GLU_OUTLINE_PATCH                  100241

/* NurbsCallback */
#define GLU_NURBS_ERROR                    100103
#define GLU_ERROR                          100103
#define GLU_NURBS_BEGIN                    100164
#define GLU_NURBS_BEGIN_EXT                100164
#define GLU_NURBS_VERTEX                   100165
#define GLU_NURBS_VERTEX_EXT               100165
#define GLU_NURBS_NORMAL                   100166
#define GLU_NURBS_NORMAL_EXT               100166
#define GLU_NURBS_COLOR                    100167
#define GLU_NURBS_COLOR_EXT                100167
#define GLU_NURBS_TEXTURE_COORD            100168
#define GLU_NURBS_TEX_COORD_EXT            100168
#define GLU_NURBS_END                      100169
#define GLU_NURBS_END_EXT                  100169
#define GLU_NURBS_BEGIN_DATA               100170
#define GLU_NURBS_BEGIN_DATA_EXT           100170
#define GLU_NURBS_VERTEX_DATA              100171
#define GLU_NURBS_VERTEX_DATA_EXT          100171
#define GLU_NURBS_NORMAL_DATA              100172
#define GLU_NURBS_NORMAL_DATA_EXT          100172
#define GLU_NURBS_COLOR_DATA               100173
#define GLU_NURBS_COLOR_DATA_EXT           100173
#define GLU_NURBS_TEXTURE_COORD_DATA       100174
#define GLU_NURBS_TEX_COORD_DATA_EXT       100174
#define GLU_NURBS_END_DATA                 100175
#define GLU_NURBS_END_DATA_EXT             100175

/* NurbsError */
#define GLU_NURBS_ERROR1                   100251
#define GLU_NURBS_ERROR2                   100252
#define GLU_NURBS_ERROR3                   100253
#define GLU_NURBS_ERROR4                   100254
#define GLU_NURBS_ERROR5                   100255
#define GLU_NURBS_ERROR6                   100256
#define GLU_NURBS_ERROR7                   100257
#define GLU_NURBS_ERROR8                   100258
#define GLU_NURBS_ERROR9                   100259
#define GLU_NURBS_ERROR10                  100260
#define GLU_NURBS_ERROR11                  100261
#define GLU_NURBS_ERROR12                  100262
#define GLU_NURBS_ERROR13                  100263
#define GLU_NURBS_ERROR14                  100264
#define GLU_NURBS_ERROR15                  100265
#define GLU_NURBS_ERROR16                  100266
#define GLU_NURBS_ERROR17                  100267
#define GLU_NURBS_ERROR18                  100268
#define GLU_NURBS_ERROR19                  100269
#define GLU_NURBS_ERROR20                  100270
#define GLU_NURBS_ERROR21                  100271
#define GLU_NURBS_ERROR22                  100272
#define GLU_NURBS_ERROR23                  100273
#define GLU_NURBS_ERROR24                  100274
#define GLU_NURBS_ERROR25                  100275
#define GLU_NURBS_ERROR26                  100276
#define GLU_NURBS_ERROR27                  100277
#define GLU_NURBS_ERROR28                  100278
#define GLU_NURBS_ERROR29                  100279
#define GLU_NURBS_ERROR30                  100280
#define GLU_NURBS_ERROR31                  100281
#define GLU_NURBS_ERROR32                  100282
#define GLU_NURBS_ERROR33                  100283
#define GLU_NURBS_ERROR34                  100284
#define GLU_NURBS_ERROR35                  100285
#define GLU_NURBS_ERROR36                  100286
#define GLU_NURBS_ERROR37                  100287

/* NurbsProperty */
#define GLU_AUTO_LOAD_MATRIX               100200
#define GLU_CULLING                        100201
#define GLU_SAMPLING_TOLERANCE             100203
#define GLU_DISPLAY_MODE                   100204
#define GLU_PARAMETRIC_TOLERANCE           100202
#define GLU_SAMPLING_METHOD                100205
#define GLU_U_STEP                         100206
#define GLU_V_STEP                         100207
#define GLU_NURBS_MODE                     100160
#define GLU_NURBS_MODE_EXT                 100160
#define GLU_NURBS_TESSELLATOR              100161
#define GLU_NURBS_TESSELLATOR_EXT          100161
#define GLU_NURBS_RENDERER                 100162
#define GLU_NURBS_RENDERER_EXT             100162

/* NurbsSampling */
#define GLU_OBJECT_PARAMETRIC_ERROR        100208
#define GLU_OBJECT_PARAMETRIC_ERROR_EXT    100208
#define GLU_OBJECT_PATH_LENGTH             100209
#define GLU_OBJECT_PATH_LENGTH_EXT         100209
#define GLU_PATH_LENGTH                    100215
#define GLU_PARAMETRIC_ERROR               100216
#define GLU_DOMAIN_DISTANCE                100217

/* NurbsTrim */
#define GLU_MAP1_TRIM_2                    100210
#define GLU_MAP1_TRIM_3                    100211

/* QuadricDrawStyle */
#define GLU_POINT                          100010
#define GLU_LINE                           100011
#define GLU_FILL                           100012
#define GLU_SILHOUETTE                     100013

/* QuadricCallback */
/*      GLU_ERROR */

/* QuadricNormal */
#define GLU_SMOOTH                         100000
#define GLU_FLAT                           100001
#define GLU_NONE                           100002

/* QuadricOrientation */
#define GLU_OUTSIDE                        100020
#define GLU_INSIDE                         100021

/* TessCallback */
#define GLU_TESS_BEGIN                     100100
#define GLU_BEGIN                          100100
#define GLU_TESS_VERTEX                    100101
#define GLU_VERTEX                         100101
#define GLU_TESS_END                       100102
#define GLU_END                            100102
#define GLU_TESS_ERROR                     100103
#define GLU_TESS_EDGE_FLAG                 100104
#define GLU_EDGE_FLAG                      100104
#define GLU_TESS_COMBINE                   100105
#define GLU_TESS_BEGIN_DATA                100106
#define GLU_TESS_VERTEX_DATA               100107
#define GLU_TESS_END_DATA                  100108
#define GLU_TESS_ERROR_DATA                100109
#define GLU_TESS_EDGE_FLAG_DATA            100110
#define GLU_TESS_COMBINE_DATA              100111

/* TessContour */
#define GLU_CW                             100120
#define GLU_CCW                            100121
#define GLU_INTERIOR                       100122
#define GLU_EXTERIOR                       100123
#define GLU_UNKNOWN                        100124

/* TessProperty */
#define GLU_TESS_WINDING_RULE              100140
#define GLU_TESS_BOUNDARY_ONLY             100141
#define GLU_TESS_TOLERANCE                 100142

/* TessError */
#define GLU_TESS_ERROR1                    100151
#define GLU_TESS_ERROR2                    100152
#define GLU_TESS_ERROR3                    100153
#define GLU_TESS_ERROR4                    100154
#define GLU_TESS_ERROR5                    100155
#define GLU_TESS_ERROR6                    100156
#define GLU_TESS_ERROR7                    100157
#define GLU_TESS_ERROR8                    100158
#define GLU_TESS_MISSING_BEGIN_POLYGON     100151
#define GLU_TESS_MISSING_BEGIN_CONTOUR     100152
#define GLU_TESS_MISSING_END_POLYGON       100153
#define GLU_TESS_MISSING_END_CONTOUR       100154
#define GLU_TESS_COORD_TOO_LARGE           100155
#define GLU_TESS_NEED_COMBINE_CALLBACK     100156

/* TessWinding */
#define GLU_TESS_WINDING_ODD               100130
#define GLU_TESS_WINDING_NONZERO           100131
#define GLU_TESS_WINDING_POSITIVE          100132
#define GLU_TESS_WINDING_NEGATIVE          100133
#define GLU_TESS_WINDING_ABS_GEQ_TWO       100134

/*************************************************************/


#ifdef __cplusplus
class GLUnurbs;
class GLUquadric;
class GLUtesselator;
#else
typedef struct GLUnurbs GLUnurbs;
typedef struct GLUquadric GLUquadric;
typedef struct GLUtesselator GLUtesselator;
#endif

typedef GLUnurbs GLUnurbsObj;
typedef GLUquadric GLUquadricObj;
typedef GLUtesselator GLUtesselatorObj;
typedef GLUtesselator GLUtriangulatorObj;

#define GLU_TESS_MAX_COORD 1.0e150

/* Internal convenience typedefs */
typedef void (APIENTRY *_GLUfuncptr)();

#endif  /* GLX_VERSION_1_X */

#ifndef GLU_VERSION_1_X
#define GLU_VERSION_1_X 1

GLAPI void APIENTRY gluBeginCurve (GLUnurbs* nurb);
GLAPI void APIENTRY gluBeginPolygon (GLUtesselator* tess);
GLAPI void APIENTRY gluBeginSurface (GLUnurbs* nurb);
GLAPI void APIENTRY gluBeginTrim (GLUnurbs* nurb);
GLAPI GLint APIENTRY gluBuild1DMipmapLevels (GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *data);
GLAPI GLint APIENTRY gluBuild1DMipmaps (GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *data);
GLAPI GLint APIENTRY gluBuild2DMipmapLevels (GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *data);
GLAPI GLint APIENTRY gluBuild2DMipmaps (GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *data);
GLAPI GLint APIENTRY gluBuild3DMipmapLevels (GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *data);
GLAPI GLint APIENTRY gluBuild3DMipmaps (GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *data);
GLAPI GLboolean APIENTRY gluCheckExtension (const GLubyte *extName, const GLubyte *extString);
GLAPI void APIENTRY gluCylinder (GLUquadric* quad, GLdouble base, GLdouble top, GLdouble height, GLint slices, GLint stacks);
GLAPI void APIENTRY gluDeleteNurbsRenderer (GLUnurbs* nurb);
GLAPI void APIENTRY gluDeleteQuadric (GLUquadric* quad);
GLAPI void APIENTRY gluDeleteTess (GLUtesselator* tess);
GLAPI void APIENTRY gluDisk (GLUquadric* quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops);
GLAPI void APIENTRY gluEndCurve (GLUnurbs* nurb);
GLAPI void APIENTRY gluEndPolygon (GLUtesselator* tess);
GLAPI void APIENTRY gluEndSurface (GLUnurbs* nurb);
GLAPI void APIENTRY gluEndTrim (GLUnurbs* nurb);
GLAPI const GLubyte * APIENTRY gluErrorString (GLenum error);
GLAPI void APIENTRY gluGetNurbsProperty (GLUnurbs* nurb, GLenum property, GLfloat* data);
GLAPI const GLubyte * APIENTRY gluGetString (GLenum name);
GLAPI void APIENTRY gluGetTessProperty (GLUtesselator* tess, GLenum which, GLdouble* data);
GLAPI void APIENTRY gluLoadSamplingMatrices (GLUnurbs* nurb, const GLfloat *model, const GLfloat *perspective, const GLint *view);
GLAPI void APIENTRY gluLookAt (GLdouble eyeX, GLdouble eyeY, GLdouble eyeZ, GLdouble centerX, GLdouble centerY, GLdouble centerZ, GLdouble upX, GLdouble upY, GLdouble upZ);
GLAPI GLUnurbs* APIENTRY gluNewNurbsRenderer (void);
GLAPI GLUquadric* APIENTRY gluNewQuadric (void);
GLAPI GLUtesselator* APIENTRY gluNewTess (void);
GLAPI void APIENTRY gluNextContour (GLUtesselator* tess, GLenum type);
GLAPI void APIENTRY gluNurbsCallback (GLUnurbs* nurb, GLenum which, _GLUfuncptr CallBackFunc);
GLAPI void APIENTRY gluNurbsCallbackData (GLUnurbs* nurb, GLvoid* userData);
GLAPI void APIENTRY gluNurbsCallbackDataEXT (GLUnurbs* nurb, GLvoid* userData);
GLAPI void APIENTRY gluNurbsCurve (GLUnurbs* nurb, GLint knotCount, GLfloat *knots, GLint stride, GLfloat *control, GLint order, GLenum type);
GLAPI void APIENTRY gluNurbsProperty (GLUnurbs* nurb, GLenum property, GLfloat value);
GLAPI void APIENTRY gluNurbsSurface (GLUnurbs* nurb, GLint sKnotCount, GLfloat* sKnots, GLint tKnotCount, GLfloat* tKnots, GLint sStride, GLint tStride, GLfloat* control, GLint sOrder, GLint tOrder, GLenum type);
GLAPI void APIENTRY gluOrtho2D (GLdouble left, GLdouble right, GLdouble bottom, GLdouble top);
GLAPI void APIENTRY gluPartialDisk (GLUquadric* quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops, GLdouble start, GLdouble sweep);
GLAPI void APIENTRY gluPerspective (GLdouble fovy, GLdouble aspect, GLdouble zNear, GLdouble zFar);
GLAPI void APIENTRY gluPickMatrix (GLdouble x, GLdouble y, GLdouble delX, GLdouble delY, GLint *viewport);
GLAPI GLint APIENTRY gluProject (GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *model, const GLdouble *proj, const GLint *view, GLdouble* winX, GLdouble* winY, GLdouble* winZ);
GLAPI void APIENTRY gluPwlCurve (GLUnurbs* nurb, GLint count, GLfloat* data, GLint stride, GLenum type);
GLAPI void APIENTRY gluQuadricCallback (GLUquadric* quad, GLenum which, _GLUfuncptr CallBackFunc);
GLAPI void APIENTRY gluQuadricDrawStyle (GLUquadric* quad, GLenum draw);
GLAPI void APIENTRY gluQuadricNormals (GLUquadric* quad, GLenum normal);
GLAPI void APIENTRY gluQuadricOrientation (GLUquadric* quad, GLenum orientation);
GLAPI void APIENTRY gluQuadricTexture (GLUquadric* quad, GLboolean texture);
GLAPI GLint APIENTRY gluScaleImage (GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid* dataOut);
GLAPI void APIENTRY gluSphere (GLUquadric* quad, GLdouble radius, GLint slices, GLint stacks);
GLAPI void APIENTRY gluTessBeginContour (GLUtesselator* tess);
GLAPI void APIENTRY gluTessBeginPolygon (GLUtesselator* tess, GLvoid* data);
GLAPI void APIENTRY gluTessCallback (GLUtesselator* tess, GLenum which, _GLUfuncptr CallBackFunc);
GLAPI void APIENTRY gluTessEndContour (GLUtesselator* tess);
GLAPI void APIENTRY gluTessEndPolygon (GLUtesselator* tess);
GLAPI void APIENTRY gluTessNormal (GLUtesselator* tess, GLdouble valueX, GLdouble valueY, GLdouble valueZ);
GLAPI void APIENTRY gluTessProperty (GLUtesselator* tess, GLenum which, GLdouble data);
GLAPI void APIENTRY gluTessVertex (GLUtesselator* tess, GLdouble *location, GLvoid* data);
GLAPI GLint APIENTRY gluUnProject (GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *model, const GLdouble *proj, const GLint *view, GLdouble* objX, GLdouble* objY, GLdouble* objZ);
GLAPI GLint APIENTRY gluUnProject4 (GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *model, const GLdouble *proj, const GLint *view, GLdouble nearVal, GLdouble farVal, GLdouble* objX, GLdouble* objY, GLdouble* objZ, GLdouble* objW);


typedef void (APIENTRYP PFNGLUBEGINCURVEPROC) (GLUnurbs* nurb);
typedef void (APIENTRYP PFNGLUBEGINPOLYGONPROC) (GLUtesselator* tess);
typedef void (APIENTRYP PFNGLUBEGINSURFACEPROC) (GLUnurbs* nurb);
typedef void (APIENTRYP PFNGLUBEGINTRIMPROC) (GLUnurbs* nurb);
typedef GLint (APIENTRYP PFNGLUBUILD1DMIPMAPLEVELSPROC) (GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *data);
typedef GLint (APIENTRYP PFNGLUBUILD1DMIPMAPSPROC) (GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *data);
typedef GLint (APIENTRYP PFNGLUBUILD2DMIPMAPLEVELSPROC) (GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *data);
typedef GLint (APIENTRYP PFNGLUBUILD2DMIPMAPSPROC) (GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *data);
typedef GLint (APIENTRYP PFNGLUBUILD3DMIPMAPLEVELSPROC) (GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *data);
typedef GLint (APIENTRYP PFNGLUBUILD3DMIPMAPSPROC) (GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *data);
typedef GLboolean (APIENTRYP PFNGLUCHECKEXTENSIONPROC) (const GLubyte *extName, const GLubyte *extString);
typedef void (APIENTRYP PFNGLUCYLINDERPROC) (GLUquadric* quad, GLdouble base, GLdouble top, GLdouble height, GLint slices, GLint stacks);
typedef void (APIENTRYP PFNGLUDELETENURBSRENDERERPROC) (GLUnurbs* nurb);
typedef void (APIENTRYP PFNGLUDELETEQUADRICPROC) (GLUquadric* quad);
typedef void (APIENTRYP PFNGLUDELETETESSPROC) (GLUtesselator* tess);
typedef void (APIENTRYP PFNGLUDISKPROC) (GLUquadric* quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops);
typedef void (APIENTRYP PFNGLUENDCURVEPROC) (GLUnurbs* nurb);
typedef void (APIENTRYP PFNGLUENDPOLYGONPROC) (GLUtesselator* tess);
typedef void (APIENTRYP PFNGLUENDSURFACEPROC) (GLUnurbs* nurb);
typedef void (APIENTRYP PFNGLUENDTRIMPROC) (GLUnurbs* nurb);
typedef const GLubyte * (APIENTRYP PFNGLUERRORSTRINGPROC) (GLenum error);
typedef void (APIENTRYP PFNGLUGETNURBSPROPERTYPROC) (GLUnurbs* nurb, GLenum property, GLfloat* data);
typedef const GLubyte * (APIENTRYP PFNGLUGETSTRINGPROC) (GLenum name);
typedef void (APIENTRYP PFNGLUGETTESSPROPERTYPROC) (GLUtesselator* tess, GLenum which, GLdouble* data);
typedef void (APIENTRYP PFNGLULOADSAMPLINGMATRICESPROC) (GLUnurbs* nurb, const GLfloat *model, const GLfloat *perspective, const GLint *view);
typedef void (APIENTRYP PFNGLULOOKATPROC) (GLdouble eyeX, GLdouble eyeY, GLdouble eyeZ, GLdouble centerX, GLdouble centerY, GLdouble centerZ, GLdouble upX, GLdouble upY, GLdouble upZ);
typedef GLUnurbs* (APIENTRYP PFNGLUNEWNURBSRENDERERPROC) (void);
typedef GLUquadric* (APIENTRYP PFNGLUNEWQUADRICPROC) (void);
typedef GLUtesselator* (APIENTRYP PFNGLUNEWTESSPROC) (void);
typedef void (APIENTRYP PFNGLUNEXTCONTOURPROC) (GLUtesselator* tess, GLenum type);
typedef void (APIENTRYP PFNGLUNURBSCALLBACKPROC) (GLUnurbs* nurb, GLenum which, _GLUfuncptr CallBackFunc);
typedef void (APIENTRYP PFNGLUNURBSCALLBACKDATAPROC) (GLUnurbs* nurb, GLvoid* userData);
typedef void (APIENTRYP PFNGLUNURBSCALLBACKDATAEXTPROC) (GLUnurbs* nurb, GLvoid* userData);
typedef void (APIENTRYP PFNGLUNURBSCURVEPROC) (GLUnurbs* nurb, GLint knotCount, GLfloat *knots, GLint stride, GLfloat *control, GLint order, GLenum type);
typedef void (APIENTRYP PFNGLUNURBSPROPERTYPROC) (GLUnurbs* nurb, GLenum property, GLfloat value);
typedef void (APIENTRYP PFNGLUNURBSSURFACEPROC) (GLUnurbs* nurb, GLint sKnotCount, GLfloat* sKnots, GLint tKnotCount, GLfloat* tKnots, GLint sStride, GLint tStride, GLfloat* control, GLint sOrder, GLint tOrder, GLenum type);
typedef void (APIENTRYP PFNGLUORTHO2DPROC) (GLdouble left, GLdouble right, GLdouble bottom, GLdouble top);
typedef void (APIENTRYP PFNGLUPARTIALDISKPROC) (GLUquadric* quad, GLdouble inner, GLdouble outer, GLint slices, GLint loops, GLdouble start, GLdouble sweep);
typedef void (APIENTRYP PFNGLUPERSPECTIVEPROC) (GLdouble fovy, GLdouble aspect, GLdouble zNear, GLdouble zFar);
typedef void (APIENTRYP PFNGLUPICKMATRIXPROC) (GLdouble x, GLdouble y, GLdouble delX, GLdouble delY, GLint *viewport);
typedef GLint (APIENTRYP PFNGLUPROJECTPROC) (GLdouble objX, GLdouble objY, GLdouble objZ, const GLdouble *model, const GLdouble *proj, const GLint *view, GLdouble* winX, GLdouble* winY, GLdouble* winZ);
typedef void (APIENTRYP PFNGLUPWLCURVEPROC) (GLUnurbs* nurb, GLint count, GLfloat* data, GLint stride, GLenum type);
typedef void (APIENTRYP PFNGLUQUADRICCALLBACKPROC) (GLUquadric* quad, GLenum which, _GLUfuncptr CallBackFunc);
typedef void (APIENTRYP PFNGLUQUADRICDRAWSTYLEPROC) (GLUquadric* quad, GLenum draw);
typedef void (APIENTRYP PFNGLUQUADRICNORMALSPROC) (GLUquadric* quad, GLenum normal);
typedef void (APIENTRYP PFNGLUQUADRICORIENTATIONPROC) (GLUquadric* quad, GLenum orientation);
typedef void (APIENTRYP PFNGLUQUADRICTEXTUREPROC) (GLUquadric* quad, GLboolean texture);
typedef GLint (APIENTRYP PFNGLUSCALEIMAGEPROC) (GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid* dataOut);
typedef void (APIENTRYP PFNGLUSPHEREPROC) (GLUquadric* quad, GLdouble radius, GLint slices, GLint stacks);
typedef void (APIENTRYP PFNGLUTESSBEGINCONTOURPROC) (GLUtesselator* tess);
typedef void (APIENTRYP PFNGLUTESSBEGINPOLYGONPROC) (GLUtesselator* tess, GLvoid* data);
typedef void (APIENTRYP PFNGLUTESSCALLBACKPROC) (GLUtesselator* tess, GLenum which, _GLUfuncptr CallBackFunc);
typedef void (APIENTRYP PFNGLUTESSENDCONTOURPROC) (GLUtesselator* tess);
typedef void (APIENTRYP PFNGLUTESSENDPOLYGONPROC) (GLUtesselator* tess);
typedef void (APIENTRYP PFNGLUTESSNORMALPROC) (GLUtesselator* tess, GLdouble valueX, GLdouble valueY, GLdouble valueZ);
typedef void (APIENTRYP PFNGLUTESSPROPERTYPROC) (GLUtesselator* tess, GLenum which, GLdouble data);
typedef void (APIENTRYP PFNGLUTESSVERTEXPROC) (GLUtesselator* tess, GLdouble *location, GLvoid* data);
typedef GLint (APIENTRYP PFNGLUUNPROJECTPROC) (GLdouble winX, GLdouble winY, GLdouble winZ, const GLdouble *model, const GLdouble *proj, const GLint *view, GLdouble* objX, GLdouble* objY, GLdouble* objZ);
typedef GLint (APIENTRYP PFNGLUUNPROJECT4PROC) (GLdouble winX, GLdouble winY, GLdouble winZ, GLdouble clipW, const GLdouble *model, const GLdouble *proj, const GLint *view, GLdouble nearVal, GLdouble farVal, GLdouble* objX, GLdouble* objY, GLdouble* objZ, GLdouble* objW);

#endif  /* GLU_VERSION_1_X */

#ifdef __cplusplus
}
#endif

#endif /* __glu_h__ */
