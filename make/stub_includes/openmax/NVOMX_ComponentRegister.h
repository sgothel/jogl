/* Copyright (c) 2007 NVIDIA Corporation.  All rights reserved.
 *
 * NVIDIA Corporation and its licensors retain all intellectual property 
 * and proprietary rights in and to this software, related documentation 
 * and any modifications thereto.  Any use, reproduction, disclosure or 
 * distribution of this software and related documentation without an 
 * express license agreement from NVIDIA Corporation is strictly prohibited.
 */

/** 
 * @file
 * <b>NVIDIA Tegra ODM Kit:
 *         OMX Component Register Interface</b>
 *
 * @b Description: Defines the interface to register/deregister OMX components.
 * 
 */

#ifndef NVOMX_ComponentRegister_h
#define NVOMX_ComponentRegister_h

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#include <OMX_Core.h>

/**
 * @defgroup nv_omx_il_comp_reg NVIDIA OMX Component Register Interface
 *   
 * This is the NVIDIA OMX component register interface.
 *
 * @ingroup nv_omx_il_core 
 * @{
 */

/** 
 * Dynamically registers a component with the core.
 * The core should return from this call in 20 msec.
 *
 * @pre OMX_Init()
 *
 * @param [in] pComponentReg A pointer to a component register
 *  structure. Both \c pName and \c pInitialize values of this structure
 *  shall be non-null. The \c pName value must be unique, otherwise an
 *  error is returned.
 * @retval OMX_ErrorNone If successful, or the appropriate OMX error code.
 * @ingroup core
 */
OMX_API OMX_ERRORTYPE OMX_APIENTRY NVOMX_RegisterComponent(
    OMX_IN  OMX_COMPONENTREGISTERTYPE *pComponentReg);

/** 
 * Deregisters a component with the core. 
 * A dynamically registered component is not required to be deregistered
 * with a call to this function. OMX_DeInit() will deregister
 * all components.
 * The core should return from this call in 20 msec.
 * 
 * @pre OMX_Init()
 *
 * @param [in] pComponentName The name of the component to deregister.
 * @retval OMX_ErrorNone If successful, or the appropriate OMX error code.
 * @ingroup core
 */
OMX_API OMX_ERRORTYPE OMX_APIENTRY NVOMX_DeRegisterComponent(
    OMX_IN OMX_STRING pComponentName);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif
/** @} */
/* File EOF */

