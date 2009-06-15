/* Copyright (c) 2007 NVIDIA Corporation.  All rights reserved.
 *
 * NVIDIA Corporation and its licensors retain all intellectual property
 * and proprietary rights in and to this software, related documentation
 * and any modifications thereto.  Any use, reproduction, disclosure or
 * distribution of this software and related documentation without an
 * express license agreement from NVIDIA Corporation is strictly prohibited.
 */

#ifndef _NVOMX_IndexExtensions_h_
#define _NVOMX_IndexExtensions_h_

#include <OMX_Core.h>
#include <OMX_Component.h>

#include "OMX_Core.h"

/** representation of timeout values, in milliseconds */
typedef OMX_U32 NvxTimeMs;
/** maximum timeout value, used to mean never timeout */
#define NVX_TIMEOUT_NEVER   0xffffffff
/** minimum timeout value */
#define NVX_TIMEOUT_MIN     0

typedef float NVX_F32;

#define NVX_INDEX_PARAM_FILENAME "OMX.Nvidia.index.param.filename" /**< Filename parameter for source, demux, and sink components that read/write files. */
typedef struct NVX_PARAM_FILENAME {
    OMX_U32 nSize;              /**< size of the structure in bytes */
    OMX_VERSIONTYPE nVersion;   /**< NVX extensions specification version information (1.0.0.0)*/
    OMX_STRING pFilename;       /**< name of file as supported by stdio implementation */
} NVX_PARAM_FILENAME;

#define NVX_INDEX_PARAM_DURATION "OMX.Nvidia.index.param.duration"
typedef struct NVX_PARAM_DURATION {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_TICKS nDuration;  /** Duration in us */
} NVX_PARAM_DURATION;

typedef enum
{
    NvxStreamType_NONE,
    NvxStreamType_MPEG4,
    NvxStreamType_H264,
    NvxStreamType_H263,
    NvxStreamType_WMV,
    NvxStreamType_JPG,
    NvxStreamType_MP3,
    NvxStreamType_WAV,
    NvxStreamType_AAC,
    NvxStreamType_AACSBR,
    NvxStreamType_BSAC,
    NvxStreamType_WMA,
    NvxStreamType_WMAPro,
    NvxStreamType_WMALossless,
    NvxStreamType_AMRWB,
    NvxStreamType_AMRNB,
    NvxStreamType_VORBIS
} ENvxStreamType;

#define NVX_INDEX_PARAM_STREAMTYPE "OMX.Nvidia.index.param.streamtype"
typedef struct NVX_PARAM_STREAMTYPE {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPort;
    ENvxStreamType eStreamType;
} NVX_PARAM_STREAMTYPE;

typedef enum
{
    NvxRecordingMode_Enable = 1,
    NvxRecordingMode_EnableExclusive,
    NvxRecordingMode_Disable,
    NvxRecordingMode_Force32 = 0x7FFFFFFF
} ENvxRecordingMode;

#define NVX_INDEX_PARAM_RECORDINGMODE "OMX.Nvidia.index.param.recordingmode"
typedef struct NVX_PARAM_RECORDINGMODE
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    ENvxRecordingMode RecordingMode;
} NVX_PARAM_RECORDINGMODE;

#define NVX_INDEX_PARAM_CHANNELID "OMX.Nvidia.index.param.channelid"
typedef struct NVX_PARAM_CHANNELID
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 ChannelID;
} NVX_PARAM_CHANNELID;

#define NVX_INDEX_PARAM_LOWMEMMODE "OMX.Nvidia.index.param.lowmemmode"
typedef struct NVX_PARAM_LOWMEMMODE {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_BOOL bLowMemMode;
} NVX_PARAM_LOWMEMMODE;

typedef enum
{
    NvxMetadata_Artist,
    NvxMetadata_Album,
    NvxMetadata_Genre,
    NvxMetadata_Title,
    NvxMetadata_Year,
    NvxMetadata_TrackNum,
    NvMMetadata_Encoded,
    NvxMetadata_Comment,
    NvxMetadata_Composer,
    NvxMetadata_Publisher,
    NvxMetadata_OriginalArtist,
    NvxMetadata_AlbumArtist,
    NvxMetadata_Copyright,
    NvxMetadata_Url,
    NvxMetadata_BPM,
    NvxMetadata_CoverArt,
    NvxMetadata_CoverArtURL,
    NvxMetadata_MAX = 0x7FFFFFFF
} ENvxMetadataType;

/* Charset define to extend OMX_METADATACHARSETTYPE for a U32 type */
#define NVOMX_MetadataCharsetU32    0x10000000
#define NVOMX_MetadataFormatJPEG    0x10000001
#define NVOMX_MetadataFormatPNG     0x10000002
#define NVOMX_MetadataFormatBMP     0x10000003
#define NVOMX_MetadataFormatUnknown 0x10000004

/* If specified metadata not found, returns empty string.
 * or OMX_ErrorInsufficientResources if sValueStr is too small */
#define NVX_INDEX_CONFIG_QUERYMETADATA "OMX.Nvidia.index.config.querymetadata"
typedef struct NVX_CONFIG_QUERYMETADATA {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    ENvxMetadataType eType;
    OMX_STRING sValueStr;
    OMX_U32 nValueLen;
    OMX_METADATACHARSETTYPE eCharSet;
} NVX_CONFIG_QUERYMETADATA;

#define NVX_INDEX_CONFIG_KEEPASPECT "OMX.Nvidia.index.config.keepaspect"
typedef struct NVX_CONFIG_KEEPASPECT {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_BOOL bKeepAspect;
} NVX_CONFIG_KEEPASPECT;

#define NVX_INDEX_CONFIG_ULPMODE "OMX.Nvidia.index.config.ulpmode"
typedef struct NVX_CONFIG_ULPMODE {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_BOOL enableUlpMode;
    OMX_U32 kpiMode;
} NVX_CONFIG_ULPMODE;

#define NVX_INDEX_CONFIG_AUDIO_OUTPUT "OMX.Nvidia.index.config.audio.output"
typedef enum NVX_AUDIO_OUTPUTTYPE {
    NVX_AUDIO_OutputI2S = 0,
    NVX_AUDIO_OutputHdmi,
    NVX_AUDIO_Force32 = 0x7FFFFFFF
} NVX_AUDIO_OUTPUTTYPE;

typedef struct NVX_CONFIG_AUDIOOUTPUT {
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    NVX_AUDIO_OUTPUTTYPE eOutputType;
} NVX_CONFIG_AUDIOOUTPUT;

#define NVX_INDEX_CONFIG_WHITEBALANCEOVERRIDE "OMX.Nvidia.index.config.whitebalanceoverride"
typedef struct NVX_CONFIG_WHITEBALANCEOVERRIDE
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    OMX_S32  wbIlluminantOverride;
    OMX_S32  wbGainAndColorCorrectionOverride[13];
} NVX_CONFIG_WHITEBALANCEOVERRIDE;

#define NVX_INDEX_CONFIG_PREVIEWENABLE "OMX.Nvidia.index.config.previewenable"

#define NVX_INDEX_CONFIG_CAPTUREPAUSE "OMX.Nvidia.index.config.capturepause"

#define NVX_INDEX_CONFIG_CONVERGEANDLOCK "OMX.Nvidia.index.config.convergeandlock"
typedef struct NVX_CONFIG_CONVERGEANDLOCK
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    OMX_BOOL bUnlock;
    OMX_BOOL bAutoFocus;
    OMX_BOOL bAutoExposure;
    OMX_BOOL bAutoWhiteBalance;
    OMX_U32  nTimeOutMS;
} NVX_CONFIG_CONVERGEANDLOCK;

#define NVX_INDEX_CONFIG_PRECAPTURECONVERGE "OMX.Nvidia.index.config.precaptureconverge"
typedef struct NVX_CONFIG_PRECAPTURECONVERGE
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    OMX_BOOL bPrecaptureConverge;
    OMX_BOOL bContinueDuringCapture;
    OMX_U32  nTimeOutMS;
} NVX_CONFIG_PRECAPTURECONVERGE;

#define NVX_INDEX_CONFIG_AUTOFRAMERATE "OMX.Nvidia.index.config.autoframerate"
typedef struct NVX_CONFIG_AUTOFRAMERATE
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    OMX_BOOL bEnabled;
    OMX_S32  low;
    OMX_S32  high;
} NVX_CONFIG_AUTOFRAMERATE;

#define NVX_INDEX_CONFIG_CAMERARAWCAPTURE "OMX.Nvidia.index.config.camera.rawcapture"
typedef struct NVX_CONFIG_CAMERARAWCAPTURE
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    OMX_U32 nCaptureCount;
    OMX_STRING Filename;
} NVX_CONFIG_CAMERARAWCAPTURE;

typedef enum
{
    NvxFlicker_Off,
    NvxFlicker_Auto,
    NvxFlicker_50HZ,
    NvxFlicker_60HZ
} ENvxFlickerType;
#define NVX_INDEX_CONFIG_FLICKER "OMX.Nvidia.index.config.flicker"
typedef struct NVX_CONFIG_FLICKER
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    ENvxFlickerType eFlicker;
} NVX_CONFIG_FLICKER;

// this is used for isp data dump
#define NVX_INDEX_CONFIG_ISPDATA "OMX.Nvidia.index.config.ispdata"
typedef struct NVX_CONFIG_ISPDATA
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    NVX_F32 ilData;
    NVX_F32 ilData2;
} NVX_CONFIG_ISPDATA;

#define NVX_INDEX_PARAM_VIDEO_ENCODE_PROPERTY "OMX.Nvidia.index.param.video.encode.prop"
typedef enum NVX_VIDEO_ERROR_RESILIENCY_LEVEL_TYPE {
    NVX_VIDEO_ErrorResiliency_None = 0,
    NVX_VIDEO_ErrorResiliency_Low,
    NVX_VIDEO_ErrorResiliency_High,
    NVX_VIDEO_ErrorResiliency_Invalid = 0x7FFFFFFF
} NVX_VIDEO_ERROR_RESILIENCY_LEVEL_TYPE;

typedef enum NVX_VIDEO_APPLICATION_TYPE {
    NVX_VIDEO_Application_Camcorder = 0,
    NVX_VIDEO_Application_VideoTelephony,
    NVX_VIDEO_Application_Invalid = 0x7FFFFFFF
} NVX_VIDEO_APPLICATION_TYPE;

/*new parameter to fine tune video encoder configuration*/
typedef struct NVX_PARAM_VIDENCPROPERTY
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;
    
    NVX_VIDEO_APPLICATION_TYPE eApplicationType;
    NVX_VIDEO_ERROR_RESILIENCY_LEVEL_TYPE eErrorResiliencyLevel;
} NVX_PARAM_VIDENCPROPERTY;

#define NVX_INDEX_PARAM_VIDEO_ENCODE_STRINGENTBITRATE "OMX.Nvidia.index.param.video.encode.stringentbitrate"

#define NVX_INDEX_PARAM_OTHER_3GPMUX_BUFFERCONFIG  "OMX.Nvidia.index.param.other.3gpmux.bufferconfig"
typedef struct NVX_PARAM_OTHER_3GPMUX_BUFFERCONFIG
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    OMX_BOOL bUseCache;
    OMX_U32 nBufferSize;
    OMX_U32 nPageSize;
} NVX_PARAM_OTHER_3GPMUX_BUFFERCONFIG;

#define NVX_INDEX_CONFIG_THUMBNAIL "OMX.Nvidia.index.config.thumbnail"
typedef struct NVX_CONFIG_THUMBNAIL
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    OMX_BOOL bEnabled;
    OMX_U32 nWidth;
    OMX_U32 nHeight;
} NVX_CONFIG_THUMBNAIL;

#define NVX_INDEX_CONFIG_PROFILE "OMX.Nvidia.index.config.profile"
typedef struct NVX_CONFIG_PROFILE
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    OMX_BOOL bProfile;
    OMX_BOOL bVerbose;
    OMX_BOOL bStubOutput;
    OMX_U32  nForceLocale; // 0 - no, 1 -cpu, 2 - avp
    OMX_U32  nNvMMProfile;
    OMX_BOOL bNoAVSync;
    OMX_BOOL enableUlpMode;
    OMX_U32 ulpkpiMode;
    OMX_S32  nAVSyncOffset;
    OMX_BOOL bFlip;
    OMX_U32  nFrameDrop;

    OMX_BOOL bSanity;
    OMX_U32  nAvgFPS;
    OMX_U32  nTotFrameDrops;

    // for camera
    OMX_U64 nTSPreviewStart;
    OMX_U64 nTSCaptureStart;
    OMX_U64 nTSCaptureEnd;
    OMX_U64 nTSPreviewEnd;
    OMX_U32 nPreviewStartFrameCount;
    OMX_U32 nPreviewEndFrameCount;
    OMX_U32 nCaptureStartFrameCount;
    OMX_U32 nCaptureEndFrameCount;

} NVX_CONFIG_PROFILE;


// put event extension here for now;
typedef enum NVX_EVENTTYPE_ENUM {
    NVX_EventVendorStartUnused = 0x70000000,
    
    NVX_EventCameraStart = (NVX_EventVendorStartUnused | 0xD00000),
    NVX_EventCamera_AlgorithmsLocked           = NVX_EventCameraStart,
    NVX_EventCamera_AutoFocusAchieved,
    NVX_EventCamera_AutoExposureAchieved,
    NVX_EventCamera_AutoWhiteBalanceAchieved,
    NVX_EventCamera_AutoFocusTimedOut,
    NVX_EventCamera_AutoExposureTimedOut,
    NVX_EventCamera_AutoWhiteBalanceTimedOut,
    NVX_EventCamera_CaptureAborted,
    NVX_EventCamera_CaptureStarted,

    NVX_EventCamera_StillCaptureReady,

    NVX_EventMax = OMX_EventMax,
} NVX_EVENTTYPE;


// Allow manual override on WinA to permit power optimizations from client side
typedef enum
{
    NvxWindow_A = 0,
    NvxWindow_MAX = 0x7FFFFFFF
} ENvxWindowType;

typedef enum
{
    NvxWindowAction_TurnOn = 0,
    NvxWindowAction_TurnOff,
    NvxWindowAction_MAX = 0x7FFFFFFF
} ENvxWindowDispActionType;

#define NVX_INDEX_CONFIG_WINDOW_DISP_OVERRIDE "OMX.Nvidia.index.config.windisp"
typedef struct NVX_CONFIG_WINDOWOVERRIDE
{
    OMX_U32 nSize;
    OMX_VERSIONTYPE nVersion;
    OMX_U32 nPortIndex;

    ENvxWindowType eWindow;
    ENvxWindowDispActionType eAction;
} NVX_CONFIG_WINDOWOVERRIDE;

#endif

