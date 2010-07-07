/*
 * Copyright (c) 2007 NVIDIA Corporation.  All rights reserved.
 *
 * NVIDIA Corporation and its licensors retain all intellectual property
 * and proprietary rights in and to this software, related documentation
 * and any modifications thereto.  Any use, reproduction, disclosure or
 * distribution of this software and related documentation without an express
 * license agreement from NVIDIA Corporation is strictly prohibited.
 */


#ifndef __kd_NV_multitouch_h_
#define __kd_NV_multitouch_h_
#include <KD/kd.h>

#ifdef __cplusplus
extern "C" {
#endif



/* KD_IOGROUP_MULTITOUCH: I/O group for Multitouch input devices. */
#define KD_IOGROUP_MULTITOUCH_NV 0x40004000
#define KD_STATE_MULTITOUCH_AVAILABILITY_NV (KD_IOGROUP_MULTITOUCH_NV + 0)
#define KD_INPUT_MULTITOUCH_FINGERS_NV      (KD_IOGROUP_MULTITOUCH_NV + 1)
#define KD_INPUT_MULTITOUCH_WIDTH_NV        (KD_IOGROUP_MULTITOUCH_NV + 2)
#define KD_INPUT_MULTITOUCH_X_NV            (KD_IOGROUP_MULTITOUCH_NV + 3)
#define KD_INPUT_MULTITOUCH_Y_NV            (KD_IOGROUP_MULTITOUCH_NV + 4)
#define KD_INPUT_MULTITOUCH_X2_NV           (KD_IOGROUP_MULTITOUCH_NV + 5)
#define KD_INPUT_MULTITOUCH_Y2_NV           (KD_IOGROUP_MULTITOUCH_NV + 6)
#define KD_INPUT_MULTITOUCH_PRESSURE_NV     (KD_IOGROUP_MULTITOUCH_NV + 7)
#define KD_INPUT_MULTITOUCH_GESTURES_NV     (KD_IOGROUP_MULTITOUCH_NV + 8)
#define KD_INPUT_MULTITOUCH_RELX_NV         (KD_IOGROUP_MULTITOUCH_NV + 9)
#define KD_INPUT_MULTITOUCH_RELY_NV         (KD_IOGROUP_MULTITOUCH_NV + 10)

        

/* KD_EVENT_INPUT_MULTITOUCH_NV: Multitouch event. */
#define KD_EVENT_INPUT_MULTITOUCH_NV 1001
typedef struct KDEventInputMultitouchDataNV {
    KDint32     index;
    KDint8      fingers;
    KDint8      width;
    KDint16     x;
    KDint16     y;
    KDint16     x2;
    KDint16     y2;
    KDint16     pressure;
} KDEventInputMultitouchDataNV;
        

/* KD_EVENT_INPUT_MULTITOUCH_GESTURE_NV: Multitouch gesture event. */
#define KD_EVENT_INPUT_MULTITOUCH_GESTURE_NV 1002
        

/* kdGetEventInputMultitouchDataNV: Get auxiliary event data for multitouch input. */
KD_API KDint KD_APIENTRY kdGetEventInputMultitouchDataNV(const KDEvent * event, KDEventInputMultitouchDataNV * data);

/* kdSetEventInputMultitouchActiveNV: Activate Multitouch input events */
KD_API KDint KD_APIENTRY kdSetEventInputMultitouchActiveNV(KDboolean activate);

/* kdEnableEventInputMultitouchMergeNV: Activate merging of Multitouch input events */
KD_API void KD_APIENTRY kdEnableEventInputMultitouchMergeNV(KDboolean enable);

#ifdef __cplusplus
}
#endif

#endif /* __kd_NV_multitouch_h_ */

