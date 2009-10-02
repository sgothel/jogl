//-----------------------------------------------------------------------------
// This file is provided under a dual BSD/GPLv2 license.  When using or 
// redistributing this file, you may do so under either license.
//
// GPL LICENSE SUMMARY
//
// Copyright(c) 2005-2009 Intel Corporation. All rights reserved.
//
// This program is free software; you can redistribute it and/or modify 
// it under the terms of version 2 of the GNU General Public License as
// published by the Free Software Foundation.
//
// This program is distributed in the hope that it will be useful, but 
// WITHOUT ANY WARRANTY; without even the implied warranty of 
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU 
// General Public License for more details.
//
// You should have received a copy of the GNU General Public License 
// along with this program; if not, write to the Free Software 
// Foundation, Inc., 51 Franklin St - Fifth Floor, Boston, MA 02110-1301 USA.
// The full GNU General Public License is included in this distribution 
// in the file called LICENSE.GPL.
//
// Contact Information:
//      Intel Corporation
//      2200 Mission College Blvd.
//      Santa Clara, CA  97052
//
// BSD LICENSE 
//
// Copyright(c) 2005-2009 Intel Corporation. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without 
// modification, are permitted provided that the following conditions 
// are met:
//
//   - Redistributions of source code must retain the above copyright 
//     notice, this list of conditions and the following disclaimer.
//   - Redistributions in binary form must reproduce the above copyright 
//     notice, this list of conditions and the following disclaimer in 
//     the documentation and/or other materials provided with the 
//     distribution.
//   - Neither the name of Intel Corporation nor the names of its 
//     contributors may be used to endorse or promote products derived 
//     from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR 
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//---------------------------------------------------------------------------*/

#ifndef _GDL_PD_H_
#define _GDL_PD_H_

#include "gdl_types.h"


/** @ingroup disp_pd
 Existing port driver IDs with room for user specified port drivers.
 When communicating with a specific port driver the port id must be passed.
*/
typedef enum
{
    GDL_PD_MIN_SUPPORTED_DRIVERS=0, ///< Port Driver IDs start at this value

    GDL_PD_ID_INTTVENC = GDL_PD_MIN_SUPPORTED_DRIVERS,  ///< CVBS TV encoder
    GDL_PD_ID_INTTVENC_COMPONENT, ///< Component TV encoder
    GDL_PD_ID_HDMI,               ///< HDMI
    GDL_PD_ID_USER_MIN,           ///< Begin user defined drivers
    GDL_PD_ID_USER_MAX = 8,       ///< End user defined drivers

    GDL_PD_MAX_SUPPORTED_DRIVERS  ///< Maximum number of port drivers.
} gdl_pd_id_t;



/** @ingroup disp_pd
Defined port driver attributes.  Which ones are supported (and exactly how)
can vary from one port driver to another.  See the Intel® CE Media Processors
GDL 3.0 Programming Guide for details on the attributes supported by
each port driver.
*/

// *NOTE* Extend pd_lib.c::__pd_attr_get_name() when adding new entries
typedef enum
{
    GDL_PD_MIN_SUPPORTED_ATTRIBUTES=0,  ///< Attribute ID enum's start at this value

    // HDMI RELATED
    GDL_PD_ATTR_ID_HDCP = GDL_PD_MIN_SUPPORTED_ATTRIBUTES, ///< HDCP control
    GDL_PD_ATTR_ID_HDCP_AUTO_MUTE,      ///< HDCP auto mute
    GDL_PD_ATTR_ID_HDCP_STATUS,         ///< HDCP status
    GDL_PD_ATTR_ID_HDCP_1P1,            ///< HDCP 1.1 Pj check control
    GDL_PD_ATTR_ID_COLOR_SPACE_INPUT,   ///< Input colorspace
    GDL_PD_ATTR_ID_PIXEL_FORMAT_OUTPUT, ///< Output colorspace
    GDL_PD_ATTR_ID_PIXEL_DEPTH,         ///< Depth of outgoing pixels
    GDL_PD_ATTR_ID_BG_COLOR,            ///< Fixed color for HDCP failure
    GDL_PD_ATTR_ID_CABLE_STATUS,        ///< Cable status
    GDL_PD_ATTR_ID_PAR,                 ///< Picture aspect ratio
    GDL_PD_ATTR_ID_FAR,                 ///< Format aspect ratio
    GDL_PD_ATTR_ID_USE_EDID,            ///< TV timings source
    GDL_PD_ATTR_ID_SLOW_DDC,            ///< DDC bus speed
    GDL_PD_ATTR_ID_EQUALIZE,            ///< Equalization level
    GDL_PD_ATTR_ID_TRANSMIT_LEVEL,      ///< Transmit level amplitude
    GDL_PD_ATTR_ID_TERMINATION,         ///< Termination impedance
    GDL_PD_ATTR_ID_AUDIO_CLOCK,         ///< Audio clock
    GDL_PD_ATTR_ID_COLOR_SPACE_EXT,     ///< Extended colorimetry
    GDL_PD_ATTR_ID_SENSE_DELAY,         ///< TV sensing delay after HPD
    GDL_PD_ATTR_ID_OUTPUT_CLAMP,        ///< Clamp the output in (16,235) when 
                                        ///< it is RGB color space. In YCbCr output
                                        ///< this attribute is ignored.

    // ANALOG RELATED
    GDL_PD_ATTR_ID_BRIGHTNESS,          ///< Brightness Level
    GDL_PD_ATTR_ID_CONTRAST,            ///< Contrast Level
    GDL_PD_ATTR_ID_HUE,                 ///< Hue Angle
    GDL_PD_ATTR_ID_SATURATION,          ///< Saturation Level
    GDL_PD_ATTR_ID_ACP,                 ///< Analog Content Protection
    GDL_PD_ATTR_ID_CC,                  ///< Closed Captioning
    GDL_PD_ATTR_ID_UNDERSCAN,           ///< Output scaler
    GDL_PD_ATTR_ID_SHARPNESS_HLUMA,     ///< Horizontal Luma filter
    GDL_PD_ATTR_ID_SHARPNESS_HCHROMA,   ///< Horizontal Chroma Filter
    GDL_PD_ATTR_ID_BLANK_LEVEL,         ///< Sync pulse level
    GDL_PD_ATTR_ID_BLACK_LEVEL,         ///< Black Level
    GDL_PD_ATTR_ID_BURST_LEVEL,         ///< Burst Level
    GDL_PD_ATTR_ID_FLICKER,             ///< Adaptive Flicker Filter
    GDL_PD_ATTR_ID_CHROMA_FILTER,       ///< Pre Chroma filter
    GDL_PD_ATTR_ID_TVOUT_TYPE,          ///< Current DAC configuration
    GDL_PD_ATTR_ID_TESTMODE,            ///< Test pattern generator
    GDL_PD_ATTR_ID_3CH_SYNC,            ///< 3 Channel sync
    GDL_PD_ATTR_ID_SD_OPTION,           ///< Alternate SD mode (e.g.: PAL-M,
                                        ///<    PAL-N, etc.)
    GDL_PD_ATTR_ID_RGB,                 ///< RGB / YPbPr output selection
    GDL_PD_ATTR_ID_CGMS_MODE,           ///< Current Copy Generation mode
    GDL_PD_ATTR_ID_NO_SYNC,             ///< Sync removal from green (y) signal
    GDL_PD_ATTR_ID_YC_DELAY,            ///< Luma vs Chroma delay 

    // COMMON
    GDL_PD_ATTR_ID_POWER,               ///< Disable DAC output
    GDL_PD_ATTR_ID_NAME,                ///< Driver name
    GDL_PD_ATTR_ID_VERSION_MAJOR,       ///< Driver major version
    GDL_PD_ATTR_ID_VERSION_MINOR,       ///< Driver minor version
    GDL_PD_ATTR_ID_DEBUG,               ///< Debug log
    GDL_PD_ATTR_ID_BUILD_DATE,          ///< Driver Build date
    GDL_PD_ATTR_ID_BUILD_TIME,          ///< Driver Build time
    GDL_PD_ATTR_ID_DISPLAY_PIPE,        ///< Display Pipeline assigned

    // ANALOG RELATED
    GDL_PD_ATTR_ID_SVIDEO,              ///< Assignment of component Pb & Pr DACs 
                                        ///<    for S-Video output

    // HDMI RELATED
    GDL_PD_ATTR_ID_AUDIO_STATUS,        ///< Status of audio playback

    // EXTENDED
    GDL_PD_ATTR_ID_USER_MIN,            ///< Start of user defined attributes
    GDL_PD_ATTR_ID_USER_MAX = 100,      ///< Max user defined attribute

    GDL_PD_MAX_SUPPORTED_ATTRIBUTES     ///< End of attribute IDs; must be last
} gdl_pd_attribute_id_t;


/** @ingroup disp_pd

 Attribute usage flags.
*/
// TODO: Move GDL_PD_ATTR_FLAG_SUPPORTED since it's internal to PD */
typedef enum
{
    GDL_PD_ATTR_FLAG_WRITE     = 0x1, /**< Attribute can be written           */
    GDL_PD_ATTR_FLAG_SUPPORTED = 0x2, /**< Attribute is supported on this port
                                           driver. FOR INTERNAL USE ONLY.     */
    GDL_PD_ATTR_FLAG_INTERNAL  = 0x4, /**< Attribute is invisible to outside
                                         world. FOR INTERNAL USE ONLY         */
} gdl_pd_attribute_flag_t;

//------------------------------------------------------------------------------
// Attribute flags used internally to override / extend certain behavior
// NOTE: Make sure values don't collide with gdl_pd_attribute_flag_t
//------------------------------------------------------------------------------
#define GDL_PD_ATTR_FLAG_FORCED 0x8000   // Read only is ignored

/** @ingroup disp_pd
    Attribute types
*/
typedef enum
{
    GDL_PD_ATTR_TYPE_UINT,      /**< Attribute is of type #gdl_uint32.      */
    GDL_PD_ATTR_TYPE_BOOLEAN,   /**< Attribute is of type #gdl_boolean_t.   */
    GDL_PD_ATTR_TYPE_STRING     /**< Attribute is a read-only 0-terminated
                                     ASCII string.
                                */
} gdl_pd_attribute_type_t;

/** @ingroup disp_pd
   Maximum size of PD string attributes
*/ 
#define GDL_PD_MAX_STRING_LENGTH 16

/** @ingroup disp_pd
     This structure represents port driver attribute
*/
typedef struct
{
    gdl_pd_attribute_id_t   id;     ///<  Global attribute ID.
    gdl_pd_attribute_type_t type;   ///<  Data type of attribute.
    gdl_pd_attribute_flag_t flags;  ///<  Access permissions and internal use

    char                    name[GDL_PD_MAX_STRING_LENGTH+1];
    
    union ///< Attribute data dependent on attribute type
    {
        struct
        {
            gdl_uint32      value_default;  ///<  default value
            gdl_uint32      value_min;      ///<  minimum value
            gdl_uint32      value_max;      ///<  maximum value
            gdl_uint32      value;          ///<  current value
        } _uint;
        
        struct
        {
            gdl_boolean_t   value_default;  ///<  default value
            gdl_boolean_t   value;          ///<  current value
        } _bool;

        struct
        {
            char    value[GDL_PD_MAX_STRING_LENGTH+1]; ///< current value
        } string;

    } content;
} gdl_pd_attribute_t;

/** @ingroup disp_pd
    The Internal TV Encoders can support several different TV standards when
    they are used in Standard Definition (SD) resolutions.  The entries in
    this enumeration are values that can be used to set the
    GDL_PD_ATR_ID_SD_OPTION attribute to specify the standard to be used for SD.
*/
typedef enum 
{
    TV_STD_UNDEFINED   = 0, ///< Use Default per resolution
    TV_STD_NTSC        = 0, ///< Use NTSC for 720x480i mode.
    TV_STD_PAL         = 0, ///< Use PAL for 720x576i mode.
    TV_STD_NTSC_J      = 1, ///< Use NTSC-J (Japan) for 720x480i mode.
    TV_STD_PAL_M       = 2, ///< Use PAL-M (Brazil) for 720x480i mode.
    TV_STD_PAL_N       = 3, ///< Use PAL-N (Argentina) for 720x576i mode.
    TV_STD_MAX              ///< The number of IDs in this enumeration.
} gdl_pd_sd_option_t;

//-----------------------------------------------------------------------------
// Unique IDs for [end user -> port driver] communication
//-----------------------------------------------------------------------------
/** @ingroup disp_pd
    Command codes for the gdl_port_send() function.
*/
typedef enum
{
    GDL_PD_SEND_CC = 0,
        ///< Closed Captioning data; see #gdl_cc_data_t
    GDL_PD_SEND_PAL_WSS,
        ///< Pal Wide Screen signaling; See #gdl_wss_data_t
    GDL_PD_SEND_CGMS_A,
        ///< CGMS-A for NTSC and ATSC formats; See #gdl_cgms_data_t
    GDL_PD_SEND_HDMI_AUDIO_CTRL,
        ///< HDMI audio data; See #gdl_hdmi_audio_ctrl_t
    GDL_PD_SEND_HDMI_HDCP_SRM,
        ///< HDCP System Renewability Message
    GDL_PD_SEND_HDMI_PACKET,
        ///< Generic HDMI packet; See #gdl_hdmi_packet_info_t
    GDL_PD_SEND_USER_MIN,
        /**< External (non-Intel) port drivers may define command codes starting
             with this value.
        */
    GDL_PD_SEND_USER_MAX = 30
        /**< External (non-Intel) port drivers may define command codes up to
             this value.
        */
} gdl_pd_send_t;

//-----------------------------------------------------------------------------
// Unique IDs for [port driver -> end user] communication
//-----------------------------------------------------------------------------
/** @ingroup disp_pd
    Command codes for retrieving port driver extended information 
    via gdl_port_recv().
*/
typedef enum
{
    GDL_PD_RECV_HDMI_AUDIO_CTRL = 0, /**< Audio control information 
                                          see #gdl_hdmi_audio_ctrl_t       */
    GDL_PD_RECV_HDMI_SINK_INFO,      /**< HDMI sink information 
                                          see #gdl_hdmi_sink_info_t        */
    GDL_PD_RECV_HDMI_EDID_BLOCK,     /**< 128 bytes of raw EDID
                                          see #gdl_hdmi_edid_block_t       */
    GDL_PD_RECV_HDMI_HDCP_INFO,      /**< HDCP information        */
    GDL_PD_RECV_HDMI_HDCP_KSVS,      /**< HDCP keys selection vectors      */
    GDL_PD_RECV_USER_MIN,            /**< Begin user defined command codes */
    GDL_PD_RECV_USER_MAX = 30        /**< End user defined command codes   */
} gdl_pd_recv_t;

//-----------------------------------------------------------------------------
// Output pixel format
//-----------------------------------------------------------------------------
/** @ingroup disp_pd
    Attribute values for the HDMI output pixel format.
    See #GDL_PD_ATTR_ID_PIXEL_FORMAT_OUTPUT.
*/
typedef enum
{
    GDL_PD_OPF_RGB444 = 0,          ///< RGB 4:4:4 Output
    GDL_PD_OPF_YUV422,              ///< YUV 4:2:2 Output
    GDL_PD_OPF_YUV444,              ///< YUV 4:4:4 Output
    GDL_PD_OPF_COUNT                ///< Number of output pixel formats + 1
} gdl_pd_output_pixel_format_t;

//-----------------------------------------------------------------------------
// Output pixel depth
//-----------------------------------------------------------------------------
/** @ingroup disp_pd
    Attribute values for the HDMI output pixel depth.
    See #GDL_PD_ATTR_ID_PIXEL_DEPTH.
*/
typedef enum
{
    GDL_PD_OPD_24BIT = 0,       ///< 24 bits per pixel
    GDL_PD_OPD_30BIT,           ///< 30 bits per pixel
    GDL_PD_OPD_36BIT,           ///< 36 bits per pixel
    GDL_PD_PIXEL_DEPTH_COUNT    ///< Number of supported pixel depths + 1
} gdl_pd_output_pixel_depth_t;

//------------------------------------------------------------------------------
// Picture Aspect Ratio infoframe code
//------------------------------------------------------------------------------
/** @ingroup disp_pd
    Attribute values for the HDMI Picture Aspect Ratio information sent via
    AVI infoframes. See #GDL_PD_ATTR_ID_PAR .
*/
typedef enum
{
    GDL_PD_PAR_NO_DATA = 0x00,      ///< No aspect ratio specified
    GDL_PD_PAR_4_3     = 0x01,      ///< 4:3 aspect ratio
    GDL_PD_PAR_16_9    = 0x02,      ///< 16:9 aspect ratio
} gdl_pd_par_t;

//------------------------------------------------------------------------------
// Format Aspect Ratio infoframe code
//------------------------------------------------------------------------------
/** @ingroup disp_pd
    Attribute values for the HDMI Format Aspect Ratio information sent via
    AVI infoframes. See #GDL_PD_ATTR_ID_FAR.
*/
typedef enum
{
    GDL_PD_FAR_16_9_TOP      = 0x02, ///< box 16:9 (top)
    GDL_PD_FAR_14_9_TOP      = 0x03, ///< box 14:9 (top)
    GDL_PD_FAR_G_14_9_CENTER = 0x04, ///< box > 16:9 (center)
    GDL_PD_FAR_SAME_AS_PAR   = 0x08, ///< As encoded frame
    GDL_PD_FAR_4_3_CENTER    = 0x09, ///< 4:3 center
    GDL_PD_FAR_16_9_CENTER   = 0x0A, ///< 16:9 center
    GDL_PD_FAR_14_9_CENTER   = 0x0B, ///< 14:9 center
    GDL_PD_FAR_4_3_SP_14_9   = 0x0D, ///< 4:3 with s&p 14:9 center
    GDL_PD_FAR_16_9_SP_14_9  = 0x0E, ///< 16:9 with s&p 14:9 center
    GDL_PD_FAR_16_9_SP_4_3   = 0x0F, ///< 4:3 with s&p 4:3 center
} gdl_pd_far_t;


//------------------------------------------------------------------------------
// V B I   S E R V I C E S
//------------------------------------------------------------------------------
/** @ingroup disp_pd
    When inserting VBI information into the analog TV signal, this enumeration
    is used to indicate the field into which the information should be inserted.
*/
typedef enum 
{
    VBI_FIELD_ID_ODD       = 1, /**< Odd field (field 1).   */
    VBI_FIELD_ID_EVEN      = 2, /**< Even field (field 2).  */
    VBI_FIELD_ID_UNDEFINED = 3  /**< This value should be passed when the
                                     display is in a progressive (frame) mode.
                                */
} gdl_pd_vbi_fieldid_t;

/** @ingroup disp_pd

    This enumeration is used to specify values for the #GDL_PD_ATTR_ID_ACP
    attribute (the Analog Copy Protection mode).
*/
typedef enum
{
    ACP_MODE_OFF,           ///<  ACP Off
    ACP_MODE_PSP,           ///<  Pseudo Sync Pulse + No Color Stripes
    ACP_MODE_PSP_CS_2_LINES,///<  Pseudo Sync Pulse + Color Stripes (2 lines)
    ACP_MODE_PSP_CS_4_LINES ///<  Pseudo Sync Pulse + Color Stripes (4 lines)
} gdl_pd_acp_mode_t;

/** @ingroup disp_pd
    This enumeration specifies values for CGMS-A copy permission states to be
    inserted into the analog TV signal. See the #gdl_cgms_data_t data structure.
*/
typedef enum 
{
    CGMS_A_COPY_FREELY     = 1, ///< Unlimited Copies can be made
    CGMS_A_COPY_NOMORE     = 2, ///< Copy has already been made (was reserved)
    CGMS_A_COPY_ONCE       = 3, ///< One copy can be made
    CGMS_A_COPY_NEVER      = 4, ///< No copies can be made
    CGMS_A_NO_DATA         = 5  ///< No data. Word 1 will be 1111
} gdl_pd_cgms_copy_t;

/** @ingroup disp_pd
    
    This enumeration specifies values for CGMS-A aspect ratios to be inserted
    into the analog TV signal. See the #gdl_cgms_data_t data structure.
*/
typedef enum 
{
    CGMS_A_4_3      = 1,    ///< Normal 4:3 aspect ratio
    CGMS_A_4_3_LB   = 2,    ///< 4:3 aspect ratio letterboxed
    CGMS_A_16_9     = 3     ///< 16:9 aspect ratio (Not available at 480i/576i)
} gdl_pd_cgms_aspect_t;

/** @ingroup disp_pd
    This enumeration specifies values for Wide Screen Signalling aspect ration
    information to be inserted into the analog TV signal. See the
    #gdl_wss_data_t data structure.
*/
typedef enum 
{
    /* PAL specific Modes */
    WSS_4_3_FF          = 0,    ///< 4:3 Full Format
    WSS_14_9_LB_C       = 1,    ///< 14:9 Letterbox, Centered
    WSS_14_9_LB_T       = 2,    ///< 14:9 Letterbox, Top
    WSS_16_9_LB_C       = 3,    ///< 16:9 Letterbox, Centered
    WSS_16_9_LB_T       = 4,    ///< 16:9 Letterbox, Top
    WSS_G_16_9_LB_C     = 5,    ///< >16:9 Letterbox, Centered
    WSS_14_9_FF         = 6,    ///< 14:9 Full Format
    WSS_16_9_ANAMORPHIC = 7,    ///< 16:9 Anamorphic
} gdl_pd_wss_aspect_t;

/** @ingroup disp_pd
    This enumeration specifies values for Wide Screen Signalling camera mode
    information to be inserted into the analog TV signal. See the
    #gdl_wss_data_t data structure.
*/
typedef enum 
{
    WSS_CAMERA_MODE = 0, ///< Camera Mode
    WSS_FILM_MODE   = 1, ///< Film Mode
} gdl_pd_wss_camera_t;

/** @ingroup disp_pd
    This enumeration specifies values for Wide Screen Signalling color encoding
    information to be inserted into the analog TV signal. See the
    #gdl_wss_data_t data structure.
*/
typedef enum 
{
    WSS_CE_NORMAL_PAL = 10, ///< Normal PAL Colors
    WSS_CE_COLOR_PLUS = 11, ///< Motion Adaptive Color Plus
} gdl_pd_wss_ce_t;

/** @ingroup disp_pd
    This enumeration specifies values to indicate  Wide Screen Signalling
    helpers state, to be inserted into the analog TV signal. See the 
    #gdl_wss_data_t data structure.
*/
typedef enum 
{
    WSS_HELPERS_NOT_PRESENT = 1, ///< No Helper
    WSS_HELPERS_PRESENT     = 2, ///< Modulated helper

} gdl_pd_wss_helpers_t;

/** @ingroup disp_pd
    This enumeration specifies values for Wide Screen Signalling open subtitles
    state, to be inserted into the analog TV signal. See the #gdl_wss_data_t
    data structure.
*/
typedef enum 
{
    WSS_OPEN_SUBTITLES_NO       = 1,    ///< No open subtitles
    WSS_OPEN_SUBTITLES_INSIDE   = 2,    ///< Subtitles in active image area
    WSS_OPEN_SUBTITLES_OUTSIDE  = 3,    ///< Subtitles out of active image area
} gdl_pd_wss_opensub_t;

/** @ingroup disp_pd
    This enumeration specifies values for Wide Screen Signalling surround sound
    state, to be inserted into the analog TV signal. See the #gdl_wss_data_t
    data structure.
*/
typedef enum 
{
    WSS_SURROUND_NO     = 1,    ///< No surround sound information
    WSS_SURROUND_YES    = 2,    ///< Surround sound present
} gdl_pd_wss_surround_t;

/** @ingroup disp_pd
    This enumeration contains the data type identifier for the WSS information
    to pass to the TV encoder to be inserted into the analog TV signal.
*/
typedef enum 
{
    WSS_NO_COPYRIGHT    = 1,    ///< No Copyright asserted or status unknown
    WSS_COPYRIGHT       = 2,    ///< Copyright Asserted
} gdl_pd_wss_copyright_t;

/** @ingroup disp_pd
    This enumeration specifies values for Wide Screen Signalling copy
    restriction state, to be inserted into the analog TV signal. See the
    #gdl_wss_data_t data structure.
*/
typedef enum 
{
    WSS_COPY_NO_REST    = 1,    ///< Copying not restricted
    WSS_COPY_RESTRICTED = 2     ///< Copying Restricted
} gdl_pd_wss_copy_t;


/** @ingroup disp_pd

    This data structure is used to pass closed captioning information to the
    display driver.  The driver will pass this information to the TV encoder to
    be inserted into the analog TV signal.
*/
typedef struct
{
   gdl_pd_vbi_fieldid_t pd_vbi_field_id;
                                /**< Field ID identifier; See
                                     #gdl_pd_vbi_fieldid_t.
                                */
   unsigned char        data_length;
                                /**< Number of valid closed caption data bytes
                                     passed; must be an even number, with a
                                     maximum value of 8.
                                */
   unsigned char        ccdata[8];
                                /**< Array containing the closed caption data
                                     to be inserted.
                                */
} gdl_cc_data_t;

/** @ingroup disp_pd

    This data structure is used to pass PAL Wide Screen signaling from an
    application to the display driver.  The driver will pass this
    information to the TV encoder to be inserted into the PAL analog TV signal.

    Teletext is not supported in silicon. Teletext in subtitle always is 0.

    Standard in use:
    ETSI EN 300 294 V1.4.1 2003-2004 
*/
typedef struct
{
    gdl_boolean_t           enabled;     ///< GDL_TRUE => Enabled
    gdl_pd_wss_aspect_t     aspect;      ///< Aspect Ratio
    gdl_pd_wss_camera_t     cam_mode;    ///< Camera Mode
    gdl_pd_wss_ce_t         color_enc;   ///< Color Encoding
    gdl_pd_wss_helpers_t    helpers;     ///< Helpers Present
    gdl_pd_wss_opensub_t    open_sub;    ///< Open Subtitles
    gdl_pd_wss_surround_t   surround;    ///< Surround sound
    gdl_pd_wss_copyright_t  copyright;   ///< Copyright assertion
    gdl_pd_wss_copy_t       copy_rest;   ///< Copy Restriction
} gdl_wss_data_t;

/** @ingroup disp_pd

    This data structure is used to pass Copy Generation Management System
    (Analog) information from the application to the display driver.  The driver
    will pass this information to the TV encoder to be inserted into the analog
    TV signal.

    XDS CEA-608 based CGMS-A should be passed using the Closed Captioning API.
    See #gdl_cc_data_t
 
    Standard is use: IEC 61880 480i Line20, EIA/CEA-805 480p Line 41, 
                     720p Line 24 , and 1080i Line 19 
*/
typedef struct
{
    gdl_boolean_t         enabled;   ///< GDL_TRUE => Enabled
    gdl_pd_cgms_copy_t    copyGen;   ///< CGMS-A data see #gdl_pd_cgms_copy_t
    gdl_pd_cgms_aspect_t  aspect;    ///< Wide Screen signaling.
    gdl_pd_acp_mode_t     mv;        ///< APS
    gdl_boolean_t         analog_src;///< Analog Source Bit
} gdl_cgms_data_t;

/** @ingroup disp_pd
    This enumeration is used to specify values for the #GDL_PD_ATTR_ID_SVIDEO
    attribute
*/
typedef enum
{
    GDL_PD_TVOUT_TYPE_COMPOSITE, ///< Composite only
    GDL_PD_TVOUT_TYPE_SVIDEO,    ///< S-Video only
    GDL_PD_TVOUT_TYPE_COMPONENT, ///< Reserved for internal use
    GDL_PD_TVOUT_TYPE_CVBSSV,    ///< Composite and S-video
} gdl_pd_tvout_type_t;

//------------------------------------------------------------------------------
//    H D M I   S P E C I F I C   D A T A   T Y P E S
//------------------------------------------------------------------------------

/** @ingroup disp_pd
   This structure defines the HDMI audio data blocks.
*/
typedef struct
{
    gdl_uint32 format;
    gdl_uint32 max_channels;
    gdl_uint32 fs;
    gdl_uint32 ss_bitrate;
} gdl_hdmi_audio_cap_t;

/** @ingroup disp_pd
    A CEC Source Physical Address.
*/
typedef struct
{
    gdl_uint8 a;
    gdl_uint8 b;
    gdl_uint8 c;
    gdl_uint8 d;
} gdl_src_phys_addr_t;

/** @ingroup disp_pd
    This data structure represents additional sink details not-available through
    port attributes
*/
typedef struct
{
    gdl_uint16          manufac_id;   ///< Sink manufacturer ID
    gdl_uint16          product_code; ///< Sink product code
    gdl_boolean_t       hdmi;         ///< Sink is HDMI
    gdl_boolean_t       ycbcr444;     ///< Sink supports YCbCr444
    gdl_boolean_t       ycbcr422;     ///< Sink supports YCbCr422
    gdl_src_phys_addr_t spa;          ///< CEC source physical address a.b.c.d
    gdl_uint32          speaker_map;  ///< Speaker allocation map
    gdl_boolean_t       dc_30;        ///< Sink supports 30-bit color
    gdl_boolean_t       dc_36;        ///< Sink supports 36-bit color
    gdl_boolean_t       dc_y444;      ///< Sink supports YCbCr444 in supported DC modes
    gdl_boolean_t       xvycc601;     ///< Sink supports xvYCC BT601 Colorimetry
    gdl_boolean_t       xvycc709;     ///< Sink supports xvYCC BT709 Colorimetry
    gdl_boolean_t       supports_ai;  ///< Sink supports aux audio information
} gdl_hdmi_sink_info_t;

/** @ingroup disp_pd
    This data structure represents 128 byte EDID block
*/
typedef struct
{
    gdl_uint8 index;     ///< Block number to read
    gdl_uint8 data[128]; ///< Block contents
} gdl_hdmi_edid_block_t;

/** @ingroup disp_pd
    This data structure represents HDCP topology information
*/
typedef struct
{
    gdl_boolean_t hdcp_1p1;             ///< Sink supports HDCP 1.1
    gdl_boolean_t repeater;             ///< Sink is a repeater
    gdl_boolean_t max_cascade_exceeded; ///< Maximum allowed depth exceeded
    gdl_uint32    depth;                ///< Topology depth
    gdl_boolean_t max_devs_exceeded;    ///< Maximum allowed device number exceeded
    gdl_uint32    device_count;         ///< Number of devices connected to the repeater
} gdl_hdmi_hdcp_info_t;

/** @ingroup disp_pd
    This enumeration defines the command IDs for the HDMI audio commands.
    See #gdl_hdmi_audio_ctrl_t.
*/
typedef enum
{
    GDL_HDMI_AUDIO_START,           ///< Start audio playback
    GDL_HDMI_AUDIO_STOP,            ///< Stop audio playback
    GDL_HDMI_AUDIO_SET_FORMAT,      ///< Set audio format
    GDL_HDMI_AUDIO_GET_CAPS,        ///< Retrieve descriptor of audio blocks
    GDL_HDMI_AUDIO_WRITE,           ///< For driver internal use only
} gdl_hdmi_audio_cmd_id_t;


/** @ingroup disp_pd
    This enumeration defines IDs for different HDMI audio formats.
*/
// IMPORTANT: DO NOT change order!!!
typedef enum
{
    GDL_HDMI_AUDIO_FORMAT_UNDEFINED = 0x00,
    GDL_HDMI_AUDIO_FORMAT_PCM,
    GDL_HDMI_AUDIO_FORMAT_AC3,
    GDL_HDMI_AUDIO_FORMAT_MPEG1,
    GDL_HDMI_AUDIO_FORMAT_MP3,
    GDL_HDMI_AUDIO_FORMAT_MPEG2,
    GDL_HDMI_AUDIO_FORMAT_AAC,
    GDL_HDMI_AUDIO_FORMAT_DTS,
    GDL_HDMI_AUDIO_FORMAT_ATRAC,
    GDL_HDMI_AUDIO_FORMAT_OBA,
    GDL_HDMI_AUDIO_FORMAT_DDP,
    GDL_HDMI_AUDIO_FORMAT_DTSHD,
    GDL_HDMI_AUDIO_FORMAT_MLP,
    GDL_HDMI_AUDIO_FORMAT_DST,
    GDL_HDMI_AUDIO_FORMAT_WMA_PRO,
} gdl_hdmi_audio_fmt_t;

/** @ingroup disp_pd
    This enumeration defines IDs for different HDMI audio sampling frequencies.
*/
typedef enum
{
    GDL_HDMI_AUDIO_FS_32_KHZ    = 0x01,
    GDL_HDMI_AUDIO_FS_44_1_KHZ  = 0x02,
    GDL_HDMI_AUDIO_FS_48_KHZ    = 0x04,
    GDL_HDMI_AUDIO_FS_88_2_KHZ  = 0x08,
    GDL_HDMI_AUDIO_FS_96_KHZ    = 0x10,
    GDL_HDMI_AUDIO_FS_176_4_KHZ = 0x20,
    GDL_HDMI_AUDIO_FS_192_KHZ   = 0x40,
} gdl_hdmi_audio_fs_t;

/** @ingroup disp_pd
    This enumeration defines IDs for different HDMI audio sample sizes.
*/
typedef enum
{
    GDL_HDMI_AUDIO_SS_UNDEFINED  = 0x00, ///< Undefined value
    GDL_HDMI_AUDIO_SS_16         = 0x01, ///< 16 bits
    GDL_HDMI_AUDIO_SS_20         = 0x02, ///< 20 bits
    GDL_HDMI_AUDIO_SS_24         = 0x04, ///< 24 bits
} gdl_hdmi_audio_ss_t;

/** @ingroup disp_pd
    Enumeration of the different audio speaker allocation options defined in the
    CEA-861D specification.
*/
typedef enum
{
    GDL_HDMI_AUDIO_SPEAKER_MAP_FLFR   = 0x0001,
    GDL_HDMI_AUDIO_SPEAKER_MAP_LFE    = 0x0002,
    GDL_HDMI_AUDIO_SPEAKER_MAP_FC     = 0x0004,
    GDL_HDMI_AUDIO_SPEAKER_MAP_RLRR   = 0x0008,
    GDL_HDMI_AUDIO_SPEAKER_MAP_RC     = 0x0010,
    GDL_HDMI_AUDIO_SPEAKER_MAP_FLCFRC = 0x0020,
    GDL_HDMI_AUDIO_SPEAKER_MAP_RLCRRC = 0x0040,
    GDL_HDMI_AUDIO_SPEAKER_MAP_FLWFRW = 0x0080,
    GDL_HDMI_AUDIO_SPEAKER_MAP_FLHFRH = 0x0100,
    GDL_HDMI_AUDIO_SPEAKER_MAP_TC     = 0x0200,
    GDL_HDMI_AUDIO_SPEAKER_MAP_FCH    = 0x0400,
} gdl_hdmi_audio_speaker_map_t;

/** @ingroup disp_pd
    This structure represents different audio commands
*/
typedef struct
{
    gdl_hdmi_audio_cmd_id_t cmd_id;  ///< Audio command type
    
    union  ///< Audio command details
    {
        struct   ///< Arguments for #GDL_HDMI_AUDIO_SET_FORMAT command.
        {
            gdl_hdmi_audio_fmt_t fmt;   ///< Audio format
            gdl_hdmi_audio_fs_t  fs;    ///< Sampling frequency
            unsigned int         ch;    ///< Number of channels
            gdl_hdmi_audio_ss_t  ss;    ///< Sample size [in bits]
            gdl_hdmi_audio_speaker_map_t map;
                                        ///< Speaker allocation map
        } _set_config;
        
        struct   ///< Arguments for #GDL_HDMI_AUDIO_GET_CAPS command.
        {
            unsigned int    index;      ///< Capability number
            gdl_hdmi_audio_cap_t cap;   ///< Capability content
        } _get_caps;
        
        struct   ///< Arguments for #GDL_HDMI_AUDIO_WRITE command
        {
            unsigned int  samples;      ///< Audio samples buffer address
            unsigned int  silence;      ///< Audio silence buffer address
            unsigned int  size;         ///< Audio data buffer size
            unsigned int  id;           ///< Audio buffer ID
            gdl_boolean_t sync;         ///< Type of write operation
        } _write;
        
        struct   ///< Arguments for #GDL_HDMI_AUDIO_STOP command
        {
            gdl_boolean_t sync;          ///< Type of stop request
        } _stop;
    
    } data;
    
} gdl_hdmi_audio_ctrl_t;

/** @ingroup disp_pd
    This structure represents generic HDMI packet
*/
typedef struct
{
    unsigned char header[3];
    unsigned char data[28];
} gdl_hdmi_packet_t;

/** @ingroup disp_pd
    This structure represents HDMI packet slot number
*/
typedef enum
{
    GDL_HDMI_PACKET_SLOT_0,
    GDL_HDMI_PACKET_SLOT_1,
} gdl_hdmi_packet_slot_t;

/** @ingroup disp_pd
    This structure is used to submit data via #GDL_PD_SEND_HDMI_PACKET service
    provided by #gdl_port_send
*/
typedef struct
{
    gdl_hdmi_packet_t      packet;
    gdl_hdmi_packet_slot_t slot;
} gdl_hdmi_packet_info_t;

/** @ingroup disp_pd
*   This enumeration represents YC Delay amounts
*/
typedef enum
{
    GDL_YC_DELAY_NONE,    ///< No YC delay 
    GDL_YC_DELAY_ADVANCE, ///< Y 0.5 Pixel Advance delay
    GDL_YC_DELAY_MINUS    ///< Y 1.0 Pixel delay
} gdl_yc_delay_t;

/** @ingroup disp_pd
*   This enumeration represents vswing equalization values
*/
typedef enum
{
    GDL_HDMI_EQUALIZE_NONE, ///< Equalization disabled
    GDL_HDMI_EQUALIZE_10,   ///< Equalization 10%, not supported on CE3100
    GDL_HDMI_EQUALIZE_20,   ///< Equalization 20%
    GDL_HDMI_EQUALIZE_30,   ///< Equalization 30%, not supported on CE3100
    GDL_HDMI_EQUALIZE_40,   ///< Equalization 40%
    GDL_HDMI_EQUALIZE_50,   ///< Equalization 50%, not supported on CE3100
    GDL_HDMI_EQUALIZE_60,   ///< Equalization 60%
    GDL_HDMI_EQUALIZE_70,   ///< Equalization 70%, not supported on CE3100
    GDL_HDMI_EQUALIZE_80,   ///< Equalization 80%
} gdl_hdmi_equalize_t;

/** @ingroup disp_pd
*   This enumeration represents transmit level amplitude values
*/
typedef enum
{
    GDL_HDMI_TRANSMIT_LEVEL_300, ///< 300 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_325, ///< 325 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_350, ///< 350 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_375, ///< 375 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_400, ///< 400 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_425, ///< 425 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_450, ///< 450 mV
    GDL_HDMI_TRANSMIT_LEVEL_475, ///< 475 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_500, ///< 500 mV
    GDL_HDMI_TRANSMIT_LEVEL_525, ///< 525 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_550, ///< 550 mV
    GDL_HDMI_TRANSMIT_LEVEL_575, ///< 575 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_600, ///< 600 mV
    GDL_HDMI_TRANSMIT_LEVEL_625, ///< 625 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_650, ///< 650 mV, not supported on CE3100
    GDL_HDMI_TRANSMIT_LEVEL_675, ///< 675 mV, not supported on CE3100
} gdl_hdmi_transmit_level_t;

/** @ingroup disp_pd
*   This enumeration represents termination impedance values
*/
typedef enum
{
    GDL_HDMI_TERMINATION_OPEN, ///< Open
    GDL_HDMI_TERMINATION_677,  ///< 677 Ohm, not supported on CE3100
    GDL_HDMI_TERMINATION_398,  ///< 398 Ohm, not supported on CE3100
    GDL_HDMI_TERMINATION_250,  ///< 250 Ohm, not supported on CE3100
    GDL_HDMI_TERMINATION_200,  ///< 200 Ohm
    GDL_HDMI_TERMINATION_100,  ///< 100 Ohm
    GDL_HDMI_TERMINATION_88,   ///<  88 Ohm, not supported on CE3100
    GDL_HDMI_TERMINATION_78,   ///<  78 Ohm, not supported on CE3100
    GDL_HDMI_TERMINATION_72,   ///<  72 Ohm, not supported on CE3100
    GDL_HDMI_TERMINATION_67,   ///<  67 Ohm
    GDL_HDMI_TERMINATION_65,   ///<  65 Ohm, not supported on CE3100
    GDL_HDMI_TERMINATION_50,   ///<  50 Ohm
} gdl_hdmi_termination_t;

/** @ingroup disp_pd
*   This enumeration represents band gap resistor values
*/
typedef enum
{
    GDL_HDMI_BGLVL_788,  ///< 0.788v not supported on Sodaville
    GDL_HDMI_BGLVL_818,  ///< 0.818v not supported on Sodaville
    GDL_HDMI_BGLVL_854,  ///< 0.854v not supported on Sodaville [CE3100 default]
    GDL_HDMI_BGLVL_891,  ///< 0.891v not supported on Sodaville
    GDL_HDMI_BGLVL_820,  ///< 0.82v  not supported on CE3100 [Sodaville default]
    GDL_HDMI_BGLVL_800,  ///< 0.80v  not supported on CE3100
    GDL_HDMI_BGLVL_780,  ///< 0.78v  not supported on CE3100
    GDL_HDMI_BGLVL_760,  ///< 0.76v  not supported on CE3100
    GDL_HDMI_BGLVL_750,  ///< 0.75v  not supported on CE3100
    GDL_HDMI_BGLVL_720,  ///< 0.72v  not supported on CE3100
    GDL_HDMI_BGLVL_660,  ///< 0.66v  not supported on CE3100
    GDL_HDMI_BGLVL_600,  ///< 0.60v  not supported on CE3100
} gdl_hdmi_bglvl_t;

/** @ingroup disp_pd
*   This enumeration represents different HDCP states
*/
typedef enum
{
    GDL_HDCP_STATUS_OFF,         ///< HDCP is disabled
    GDL_HDCP_STATUS_IN_PROGRESS, ///< HDCP is enabled but not authenticated yet
    GDL_HDCP_STATUS_ON,          ///< HDCP is enabled and is authenticated
} gdl_hdcp_status_t;

/** @ingroup disp_pd
*   This enumeration represents audio clock values with respect to which
*   internal audio divisor value is chosen
*/
typedef enum
{
    GDL_HDMI_AUDIO_CLOCK_24, ///< Audio clock is running at 24MHz
    GDL_HDMI_AUDIO_CLOCK_36, ///< Audio clock is running at 36Mhz
} gdl_hdmi_audio_clock_t;


#endif // _GDL_PD_H_
