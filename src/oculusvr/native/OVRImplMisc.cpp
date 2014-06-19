#include "OVR_CAPI.h"

#include "CAPI_DistortionRenderer.h"

namespace OVR { namespace CAPI {

    /**
     * Index is: apiConfig->Header.API, with
     *   ovrRenderAPIConfig * apiConfig
     *   ovrRenderAPIConfigHeader Header 
     *   ovrRenderAPIType Header.API 
     */
    DistortionRenderer::CreateFunc DistortionRenderer::APICreateRegistry[ovrRenderAPI_Count] =
    {
        0, // None
        0, // None for GL - &GL::DistortionRenderer::Create,
        0, // Android_GLES
        0, // D3D9
        0, // D3D10
        0  // D3D11
    };

}} // namespace OVR::CAPI

//
// TBD: Replace stdc++ for compatibility !
// 
// This is not enough:
// extern "C" void __cxa_pure_virtual() { while (1); }

