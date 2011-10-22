/*
 * This is a modified version of the original header as provided by
 * NVidia; original copyright appears below.
 *
 * Modified by Christopher Kline, May 2003: Stripped down and hacked to get
 * around macro interpretation problems.
 */

/*
 *
 * Copyright (c) 2002-2004, NVIDIA Corporation.
 * 
 *  
 * 
 * NVIDIA Corporation("NVIDIA") supplies this software to you in consideration 
 * of your agreement to the following terms, and your use, installation, 
 * modification or redistribution of this NVIDIA software constitutes 
 * acceptance of these terms.  If you do not agree with these terms, please do 
 * not use, install, modify or redistribute this NVIDIA software.
 * 
 *  
 * 
 * In consideration of your agreement to abide by the following terms, and 
 * subject to these terms, NVIDIA grants you a personal, non-exclusive license,
 * under NVIDIA’s copyrights in this original NVIDIA software (the "NVIDIA 
 * Software"), to use, reproduce, modify and redistribute the NVIDIA 
 * Software, with or without modifications, in source and/or binary forms; 
 * provided that if you redistribute the NVIDIA Software, you must retain the 
 * copyright notice of NVIDIA, this notice and the following text and 
 * disclaimers in all such redistributions of the NVIDIA Software. Neither the 
 * name, trademarks, service marks nor logos of NVIDIA Corporation may be used 
 * to endorse or promote products derived from the NVIDIA Software without 
 * specific prior written permission from NVIDIA.  Except as expressly stated 
 * in this notice, no other rights or licenses express or implied, are granted 
 * by NVIDIA herein, including but not limited to any patent rights that may be 
 * infringed by your derivative works or by other works in which the NVIDIA 
 * Software may be incorporated. No hardware is licensed hereunder. 
 * 
 *  
 * 
 * THE NVIDIA SOFTWARE IS BEING PROVIDED ON AN "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING 
 * WITHOUT LIMITATION, WARRANTIES OR CONDITIONS OF TITLE, NON-INFRINGEMENT, 
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, OR ITS USE AND OPERATION 
 * EITHER ALONE OR IN COMBINATION WITH OTHER PRODUCTS.
 * 
 *  
 * 
 * IN NO EVENT SHALL NVIDIA BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL, 
 * EXEMPLARY, CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, LOST 
 * PROFITS; PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS INTERRUPTION) OR ARISING IN ANY WAY OUT OF THE USE, 
 * REPRODUCTION, MODIFICATION AND/OR DISTRIBUTION OF THE NVIDIA SOFTWARE, 
 * HOWEVER CAUSED AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING 
 * NEGLIGENCE), STRICT LIABILITY OR OTHERWISE, EVEN IF NVIDIA HAS BEEN ADVISED 
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 */ 


#ifndef _cg_h
#define _cg_h


#define CG_VERSION_NUM                1400

// Set up for either Win32 import/export/lib.
#ifndef CGDLL_API
    #ifdef WIN32
        #ifdef CGDLL_EXPORTS
            #define CGDLL_API /*__declspec(dllexport) */
        #elif defined (CG_LIB)
            #define CGDLL_API
        #else
            #define CGDLL_API /* __declspec(dllimport) */
        #endif
    #else
        #define CGDLL_API
    #endif
#endif

/*************************************************************************/
/*** CG Run-Time Library API                                          ***/
/*************************************************************************/

/*************************************************************************/
/*** Data types and enumerants                                         ***/
/*************************************************************************/

typedef int CGbool;

#define CG_FALSE ((CGbool)0)
#define CG_TRUE ((CGbool)1)

typedef struct _CGcontext *CGcontext;
typedef struct _CGprogram *CGprogram;
typedef struct _CGparameter *CGparameter;
typedef struct _CGeffect *CGeffect;
typedef struct _CGtechnique *CGtechnique;
typedef struct _CGpass *CGpass;
typedef struct _CGstate *CGstate;
typedef struct _CGstateassignment *CGstateassignment;
typedef struct _CGannotation *CGannotation;
typedef void *CGhandle;

typedef CGbool (*CGstatecallback)(CGstateassignment);

//!!! PREPROCESS BEGIN

typedef enum
 {
  CG_UNKNOWN_TYPE,
  CG_STRUCT,
  CG_ARRAY,

  CG_TYPE_START_ENUM = 1024,
// # define CG_DATATYPE_MACRO(name, compiler_name, enum_name, base_name, ncols, nrows, pc) \
//   enum_name ,

#include <Cg/cg_datatypes.h>

 } CGtype;

typedef enum
 {
// # define CG_BINDLOCATION_MACRO(name,enum_name,compiler_name,\
//                                enum_int,addressable,param_type) \
//   enum_name = enum_int,

#include <Cg/cg_bindlocations.h>

  CG_UNDEFINED,

 } CGresource;

typedef enum
 {
  CG_PROFILE_START = 6144,
  CG_PROFILE_UNKNOWN,

// # define CG_PROFILE_MACRO(name, compiler_id, compiler_id_caps, compiler_opt,int_id,vertex_profile) \
//    CG_PROFILE_##compiler_id_caps = int_id,
  
#include <Cg/cg_profiles.h>

  CG_PROFILE_MAX = 7100,
 } CGprofile;

typedef enum
 {
// # define CG_ERROR_MACRO(code, enum_name, message) \
//    enum_name = code,
# include <Cg/cg_errors.h>
 } CGerror;

typedef enum
 {
  CG_PARAMETERCLASS_UNKNOWN = 0,
  CG_PARAMETERCLASS_SCALAR,
  CG_PARAMETERCLASS_VECTOR,
  CG_PARAMETERCLASS_MATRIX,
  CG_PARAMETERCLASS_STRUCT,
  CG_PARAMETERCLASS_ARRAY,
  CG_PARAMETERCLASS_SAMPLER,
  CG_PARAMETERCLASS_OBJECT
 } CGparameterclass;

//!!! PREPROCESS END

typedef enum
 {
// # define CG_ENUM_MACRO(enum_name, enum_val) \
//    enum_name = enum_val,
# include <Cg/cg_enums.h>
 } CGenum;

#include <stdarg.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void (*CGerrorCallbackFunc)(void);
typedef void (*CGerrorHandlerFunc)(CGcontext ctx, CGerror err, void *data);

/*************************************************************************/
/*** Functions                                                         ***/
/*************************************************************************/

#ifndef CG_EXPLICIT

/*** Context functions ***/

CGDLL_API CGcontext cgCreateContext(void); 
CGDLL_API void cgDestroyContext(CGcontext ctx); 
CGDLL_API CGbool cgIsContext(CGcontext ctx);
CGDLL_API const char *cgGetLastListing(CGcontext ctx);
CGDLL_API void cgSetLastListing(CGhandle handle, const char *listing);
CGDLL_API void cgSetAutoCompile(CGcontext ctx, CGenum flag);
CGDLL_API CGenum cgGetAutoCompile(CGcontext ctx);

/*** Program functions ***/
CGDLL_API CGprogram cgCreateProgram(CGcontext ctx, 
                                    CGenum program_type,
                                    const char *program,
                                    CGprofile profile,
                                    const char *entry,
                                    const char **args);
CGDLL_API CGprogram cgCreateProgramFromFile(CGcontext ctx, 
                                            CGenum program_type,
                                            const char *program_file,
                                            CGprofile profile,
                                            const char *entry,
                                            const char **args);
CGDLL_API CGprogram cgCopyProgram(CGprogram program); 
CGDLL_API void cgDestroyProgram(CGprogram program); 

CGDLL_API CGprogram cgGetFirstProgram(CGcontext ctx);
CGDLL_API CGprogram cgGetNextProgram(CGprogram current);
CGDLL_API CGcontext cgGetProgramContext(CGprogram prog);
CGDLL_API CGbool cgIsProgram(CGprogram program); 

CGDLL_API void cgCompileProgram(CGprogram program); 
CGDLL_API CGbool cgIsProgramCompiled(CGprogram program); 
CGDLL_API const char *cgGetProgramString(CGprogram prog, CGenum pname); 
CGDLL_API CGprofile cgGetProgramProfile(CGprogram prog); 
CGDLL_API char const * const *cgGetProgramOptions(CGprogram prog);
CGDLL_API void cgSetProgramProfile(CGprogram prog, CGprofile profile);

CGDLL_API void cgSetPassProgramParameters(CGprogram);

/*** Parameter functions ***/

CGDLL_API CGparameter cgCreateParameter(CGcontext ctx, CGtype type);
CGDLL_API CGparameter cgCreateParameterArray(CGcontext ctx,
                                             CGtype type, 
                                             int length);
CGDLL_API CGparameter cgCreateParameterMultiDimArray(CGcontext ctx,
                                                     CGtype type,
                                                     int dim, 
                                                     const int *lengths);
CGDLL_API void cgDestroyParameter(CGparameter param);
CGDLL_API void cgConnectParameter(CGparameter from, CGparameter to);
CGDLL_API void cgDisconnectParameter(CGparameter param);
CGDLL_API CGparameter cgGetConnectedParameter(CGparameter param);

CGDLL_API int cgGetNumConnectedToParameters(CGparameter param);
CGDLL_API CGparameter cgGetConnectedToParameter(CGparameter param, int index);

CGDLL_API CGparameter cgGetNamedParameter(CGprogram prog, const char *name);
CGDLL_API CGparameter cgGetNamedProgramParameter(CGprogram prog, 
                                                 CGenum name_space, 
                                                 const char *name);

CGDLL_API CGparameter cgGetFirstParameter(CGprogram prog, CGenum name_space);
CGDLL_API CGparameter cgGetNextParameter(CGparameter current);
CGDLL_API CGparameter cgGetFirstLeafParameter(CGprogram prog, CGenum name_space);
CGDLL_API CGparameter cgGetNextLeafParameter(CGparameter current);

CGDLL_API CGparameter cgGetFirstStructParameter(CGparameter param);
CGDLL_API CGparameter cgGetNamedStructParameter(CGparameter param, 
                                                const char *name);

CGDLL_API CGparameter cgGetFirstDependentParameter(CGparameter param);

CGDLL_API CGparameter cgGetArrayParameter(CGparameter aparam, int index);
CGDLL_API int cgGetArrayDimension(CGparameter param);
CGDLL_API CGtype cgGetArrayType(CGparameter param);
CGDLL_API int cgGetArraySize(CGparameter param, int dimension);
CGDLL_API int cgGetArrayTotalSize(CGparameter param);
CGDLL_API void cgSetArraySize(CGparameter param, int size);
CGDLL_API void cgSetMultiDimArraySize(CGparameter param, const int *sizes);

CGDLL_API CGprogram cgGetParameterProgram(CGparameter param);
CGDLL_API CGcontext cgGetParameterContext(CGparameter param);
CGDLL_API CGbool cgIsParameter(CGparameter param);
CGDLL_API const char *cgGetParameterName(CGparameter param);
CGDLL_API CGtype cgGetParameterType(CGparameter param);
CGDLL_API CGtype cgGetParameterBaseType(CGparameter param);
CGDLL_API CGparameterclass cgGetParameterClass(CGparameter param);
CGDLL_API int cgGetParameterRows(CGparameter param);
CGDLL_API int cgGetParameterColumns(CGparameter param);
CGDLL_API CGtype cgGetParameterNamedType(CGparameter param);
CGDLL_API const char *cgGetParameterSemantic(CGparameter param);
CGDLL_API CGresource cgGetParameterResource(CGparameter param);
CGDLL_API CGresource cgGetParameterBaseResource(CGparameter param);
CGDLL_API unsigned long cgGetParameterResourceIndex(CGparameter param);
CGDLL_API CGenum cgGetParameterVariability(CGparameter param);
CGDLL_API CGenum cgGetParameterDirection(CGparameter param);
CGDLL_API CGbool cgIsParameterReferenced(CGparameter param);
CGDLL_API CGbool cgIsParameterUsed(CGparameter param, CGhandle handle);
CGDLL_API const double *cgGetParameterValues(CGparameter param, 
                                             CGenum value_type,
                                             int *nvalues);
CGDLL_API void cgSetParameterValuedr(CGparameter param, int n, const double *vals);
CGDLL_API void cgSetParameterValuedc(CGparameter param, int n, const double *vals);
CGDLL_API void cgSetParameterValuefr(CGparameter param, int n, const float *vals);
CGDLL_API void cgSetParameterValuefc(CGparameter param, int n, const float *vals);
CGDLL_API void cgSetParameterValueir(CGparameter param, int n, const int *vals);
CGDLL_API void cgSetParameterValueic(CGparameter param, int n, const int *vals);
CGDLL_API int cgGetParameterValuedr(CGparameter param, int n, double *vals);
CGDLL_API int cgGetParameterValuedc(CGparameter param, int n, double *vals);
CGDLL_API int cgGetParameterValuefr(CGparameter param, int n, float *vals);
CGDLL_API int cgGetParameterValuefc(CGparameter param, int n, float *vals);
CGDLL_API int cgGetParameterValueir(CGparameter param, int n, int *vals);
CGDLL_API int cgGetParameterValueic(CGparameter param, int n, int *vals);
CGDLL_API const char *cgGetStringParameterValue(CGparameter param);
CGDLL_API void cgSetStringParameterValue(CGparameter param, const char *str);

CGDLL_API int cgGetParameterOrdinalNumber(CGparameter param);
CGDLL_API CGbool cgIsParameterGlobal(CGparameter param);
CGDLL_API int cgGetParameterIndex(CGparameter param);

CGDLL_API void cgSetParameterVariability(CGparameter param, CGenum vary);
CGDLL_API void cgSetParameterSemantic(CGparameter param, const char *semantic);

CGDLL_API void cgSetParameter1f(CGparameter param, float x);
CGDLL_API void cgSetParameter2f(CGparameter param, float x, float y);
CGDLL_API void cgSetParameter3f(CGparameter param, float x, float y, float z);
CGDLL_API void cgSetParameter4f(CGparameter param, 
                                float x, 
                                float y, 
                                float z,
                                float w);
CGDLL_API void cgSetParameter1d(CGparameter param, double x);
CGDLL_API void cgSetParameter2d(CGparameter param, double x, double y);
CGDLL_API void cgSetParameter3d(CGparameter param, 
                                double x, 
                                double y, 
                                double z);
CGDLL_API void cgSetParameter4d(CGparameter param, 
                                double x, 
                                double y, 
                                double z,
                                double w);
CGDLL_API void cgSetParameter1i(CGparameter param, int x);
CGDLL_API void cgSetParameter2i(CGparameter param, int x, int y);
CGDLL_API void cgSetParameter3i(CGparameter param, int x, int y, int z);
CGDLL_API void cgSetParameter4i(CGparameter param, 
                                int x, 
                                int y, 
                                int z,
                                int w);


CGDLL_API void cgSetParameter1iv(CGparameter param, const int *v);
CGDLL_API void cgSetParameter2iv(CGparameter param, const int *v);
CGDLL_API void cgSetParameter3iv(CGparameter param, const int *v);
CGDLL_API void cgSetParameter4iv(CGparameter param, const int *v);
CGDLL_API void cgSetParameter1fv(CGparameter param, const float *v);
CGDLL_API void cgSetParameter2fv(CGparameter param, const float *v);
CGDLL_API void cgSetParameter3fv(CGparameter param, const float *v);
CGDLL_API void cgSetParameter4fv(CGparameter param, const float *v);
CGDLL_API void cgSetParameter1dv(CGparameter param, const double *v);
CGDLL_API void cgSetParameter2dv(CGparameter param, const double *v);
CGDLL_API void cgSetParameter3dv(CGparameter param, const double *v);
CGDLL_API void cgSetParameter4dv(CGparameter param, const double *v);

CGDLL_API void cgSetMatrixParameterir(CGparameter param, const int *matrix);
CGDLL_API void cgSetMatrixParameterdr(CGparameter param, const double *matrix);
CGDLL_API void cgSetMatrixParameterfr(CGparameter param, const float *matrix);
CGDLL_API void cgSetMatrixParameteric(CGparameter param, const int *matrix);
CGDLL_API void cgSetMatrixParameterdc(CGparameter param, const double *matrix);
CGDLL_API void cgSetMatrixParameterfc(CGparameter param, const float *matrix);

CGDLL_API void cgGetMatrixParameterir(CGparameter param, int *matrix);
CGDLL_API void cgGetMatrixParameterdr(CGparameter param, double *matrix);
CGDLL_API void cgGetMatrixParameterfr(CGparameter param, float *matrix);
CGDLL_API void cgGetMatrixParameteric(CGparameter param, int *matrix);
CGDLL_API void cgGetMatrixParameterdc(CGparameter param, double *matrix);
CGDLL_API void cgGetMatrixParameterfc(CGparameter param, float *matrix);

/*** Type Functions ***/

CGDLL_API const char *cgGetTypeString(CGtype type);
CGDLL_API CGtype cgGetType(const char *type_string);

CGDLL_API CGtype cgGetNamedUserType(CGhandle handle, const char *name);

CGDLL_API int cgGetNumUserTypes(CGhandle handle);
CGDLL_API CGtype cgGetUserType(CGhandle handle, int index);

CGDLL_API int cgGetNumParentTypes(CGtype type);
CGDLL_API CGtype cgGetParentType(CGtype type, int index);

CGDLL_API CGbool cgIsParentType(CGtype parent, CGtype child);
CGDLL_API CGbool cgIsInterfaceType(CGtype type);

/*** Resource Functions ***/

CGDLL_API const char *cgGetResourceString(CGresource resource);
CGDLL_API CGresource cgGetResource(const char *resource_string);

/*** Enum Functions ***/

CGDLL_API const char *cgGetEnumString(CGenum en);
CGDLL_API CGenum cgGetEnum(const char *enum_string);

/*** Profile Functions ***/

CGDLL_API const char *cgGetProfileString(CGprofile profile);
CGDLL_API CGprofile cgGetProfile(const char *profile_string);

/*** Error Functions ***/

CGDLL_API CGerror cgGetError(void);
CGDLL_API CGerror cgGetFirstError(void);
CGDLL_API const char *cgGetErrorString(CGerror error);
CGDLL_API const char *cgGetLastErrorString(CGerror *error);
CGDLL_API void cgSetErrorCallback(CGerrorCallbackFunc func);
CGDLL_API CGerrorCallbackFunc cgGetErrorCallback(void);
CGDLL_API void cgSetErrorHandler(CGerrorHandlerFunc func, void *data);
CGDLL_API CGerrorHandlerFunc cgGetErrorHandler(void **data);

/*** Misc Functions ***/

CGDLL_API const char *cgGetString(CGenum sname);


/*** CgFX Functions ***/

CGDLL_API CGeffect cgCreateEffect(CGcontext, const char *code, const char **args);
CGDLL_API CGeffect cgCreateEffectFromFile(CGcontext, const char *filename,
                                          const char **args);
CGDLL_API void cgDestroyEffect(CGeffect);
CGDLL_API CGcontext cgGetEffectContext(CGeffect);
CGDLL_API CGbool cgIsEffect(CGeffect effect);

CGDLL_API CGeffect cgGetFirstEffect(CGcontext);
CGDLL_API CGeffect cgGetNextEffect(CGeffect);

CGDLL_API CGprogram cgCreateProgramFromEffect(CGeffect effect,
                                              CGprofile profile,
                                              const char *entry,
                                              const char **args);

CGDLL_API CGtechnique cgGetFirstTechnique(CGeffect);
CGDLL_API CGtechnique cgGetNextTechnique(CGtechnique);
CGDLL_API CGtechnique cgGetNamedTechnique(CGeffect, const char *name);
CGDLL_API const char *cgGetTechniqueName(CGtechnique);
CGDLL_API CGbool cgIsTechnique(CGtechnique);
CGDLL_API CGbool cgValidateTechnique(CGtechnique);
CGDLL_API CGbool cgIsTechniqueValidated(CGtechnique);
CGDLL_API CGeffect cgGetTechniqueEffect(CGtechnique);

CGDLL_API CGpass cgGetFirstPass(CGtechnique);
CGDLL_API CGpass cgGetNamedPass(CGtechnique, const char *name);
CGDLL_API CGpass cgGetNextPass(CGpass);
CGDLL_API CGbool cgIsPass(CGpass);
CGDLL_API const char *cgGetPassName(CGpass); 
CGDLL_API CGtechnique cgGetPassTechnique(CGpass);

CGDLL_API void cgSetPassState(CGpass);
CGDLL_API void cgResetPassState(CGpass);

CGDLL_API CGstateassignment cgGetFirstStateAssignment(CGpass);
CGDLL_API CGstateassignment cgGetNamedStateAssignment(CGpass, const char *name);
CGDLL_API CGstateassignment cgGetNextStateAssignment(CGstateassignment);
CGDLL_API CGbool cgIsStateAssignment(CGstateassignment);
CGDLL_API CGbool cgCallStateSetCallback(CGstateassignment);
CGDLL_API CGbool cgCallStateValidateCallback(CGstateassignment);
CGDLL_API CGbool cgCallStateResetCallback(CGstateassignment);
CGDLL_API CGpass cgGetStateAssignmentPass(CGstateassignment);
CGDLL_API CGparameter cgGetSamplerStateAssignmentParameter(CGstateassignment);

CGDLL_API const float *cgGetFloatStateAssignmentValues(CGstateassignment, int *nVals);
CGDLL_API const int *cgGetIntStateAssignmentValues(CGstateassignment, int *nVals);
CGDLL_API const CGbool *cgGetBoolStateAssignmentValues(CGstateassignment, int *nVals);
CGDLL_API const char *cgGetStringStateAssignmentValue(CGstateassignment);
CGDLL_API CGprogram cgGetProgramStateAssignmentValue(CGstateassignment);
CGDLL_API CGparameter cgGetTextureStateAssignmentValue(CGstateassignment);
CGDLL_API CGparameter cgGetSamplerStateAssignmentValue(CGstateassignment);
CGDLL_API int cgGetStateAssignmentIndex(CGstateassignment);

CGDLL_API int cgGetNumDependentStateAssignmentParameters(CGstateassignment);
CGDLL_API CGparameter cgGetDependentStateAssignmentParameter(CGstateassignment, int index);

CGDLL_API CGstate cgGetStateAssignmentState(CGstateassignment);
CGDLL_API CGstate cgGetSamplerStateAssignmentState(CGstateassignment);

CGDLL_API CGstate cgCreateState(CGcontext, const char *name, CGtype);
CGDLL_API CGstate cgCreateArrayState(CGcontext, const char *name, CGtype, int nelems);
CGDLL_API void cgSetStateCallbacks(CGstate, CGstatecallback set, CGstatecallback reset,
                                   CGstatecallback validate);
CGDLL_API CGstatecallback cgGetStateSetCallback(CGstate);
CGDLL_API CGstatecallback cgGetStateResetCallback(CGstate);
CGDLL_API CGstatecallback cgGetStateValidateCallback(CGstate);
CGDLL_API CGtype cgGetStateType(CGstate);
CGDLL_API const char *cgGetStateName(CGstate);
CGDLL_API CGstate cgGetNamedState(CGcontext, const char *name);
CGDLL_API CGstate cgGetFirstState(CGcontext);
CGDLL_API CGstate cgGetNextState(CGstate);
CGDLL_API CGbool cgIsState(CGstate);
CGDLL_API void cgAddStateEnumerant(CGstate, const char *name, int value);

CGDLL_API CGstate cgCreateSamplerState(CGcontext, const char *name, CGtype);
CGDLL_API CGstate cgCreateArraySamplerState(CGcontext, const char *name, CGtype, int nelems);
CGDLL_API CGstate cgGetNamedSamplerState(CGcontext, const char *name);
CGDLL_API CGstate cgGetFirstSamplerState(CGcontext);

CGDLL_API CGstateassignment cgGetFirstSamplerStateAssignment(CGparameter);
CGDLL_API CGstateassignment cgGetNamedSamplerStateAssignment(CGparameter, const char *);
CGDLL_API void cgSetSamplerState(CGparameter);

CGDLL_API CGparameter cgGetNamedEffectParameter(CGeffect, const char *);
CGDLL_API CGparameter cgGetFirstLeafEffectParameter(CGeffect);
CGDLL_API CGparameter cgGetFirstEffectParameter(CGeffect);
CGDLL_API CGparameter cgGetEffectParameterBySemantic(CGeffect, const char *);

CGDLL_API CGannotation cgGetFirstTechniqueAnnotation(CGtechnique);
CGDLL_API CGannotation cgGetFirstPassAnnotation(CGpass);
CGDLL_API CGannotation cgGetFirstParameterAnnotation(CGparameter);
CGDLL_API CGannotation cgGetFirstProgramAnnotation(CGprogram);
CGDLL_API CGannotation cgGetNextAnnotation(CGannotation);

CGDLL_API CGannotation cgGetNamedTechniqueAnnotation(CGtechnique, const char *);
CGDLL_API CGannotation cgGetNamedPassAnnotation(CGpass, const char *);
CGDLL_API CGannotation cgGetNamedParameterAnnotation(CGparameter, const char *);
CGDLL_API CGannotation cgGetNamedProgramAnnotation(CGprogram, const char *);

CGDLL_API CGbool cgIsAnnotation(CGannotation);

CGDLL_API const char *cgGetAnnotationName(CGannotation);
CGDLL_API CGtype cgGetAnnotationType(CGannotation);

CGDLL_API const float *cgGetFloatAnnotationValues(CGannotation, int *nvalues);
CGDLL_API const int *cgGetIntAnnotationValues(CGannotation, int *nvalues);
CGDLL_API const char *cgGetStringAnnotationValue(CGannotation);
CGDLL_API const int *cgGetBooleanAnnotationValues(CGannotation, int *nvalues);

CGDLL_API int cgGetNumDependentAnnotationParameters(CGannotation);
CGDLL_API CGparameter cgGetDependentAnnotationParameter(CGannotation, int index);

CGDLL_API void cgEvaluateProgram(CGprogram, float *, int ncomps, int nx, int ny, int nz);

#endif

#ifdef __cplusplus
}
#endif

#endif
