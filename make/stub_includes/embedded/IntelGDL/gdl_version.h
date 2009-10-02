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
//----------------------------------------------------------------------------


/*------------------------------------------------------------------------------
 * GDL VERSION NUMBERS
 *
 * We currently maintain two types of version information:
 *
 * GDL version
 *      This varies with major redefinitions of the user API. It comprises:
 *        - a 4-bit major version number
 *        - a 16-bit minor version number, and
 *        - a 4-bit hotfix number (which will normally be 0).
 *      These numbers are determined by the #define's below.  Hotfix numbers
 *      should be incremented for each hotfix release, and should be reset to 0
 *      when one of the other numbers is rolled.
 *
 *      An application can retrieve the version number via the
 *      gdl_get_driver_info() API.
 *
 * Header version
 *      It is possible for internal changes to be made to shared header files
 *      resulting in incompatibility among the following components if they
 *      are built with different versions of the header files:
 *          - user applications
 *          - libgdl.so
 *          - gdl_mm.ko
 *          - gdl_server
 *
 *      The header version number is composed of 16-bit major and minor numbers
 *      assigned via the #define's below.  THE MINOR NUMBER SHOULD BE ROLLED
 *      whenever a backward-compatible change is made.  THE MAJOR NUMBER SHOULD
 *      BE ROLLED whenever changes are made that are incompatible with previous
 *      versions of the user library.
 *
 *      Compatibility is checked in gdl_init().
 *
 *      The header version is also returned via the gdl_get_driver() API.
 *----------------------------------------------------------------------------*/

#ifndef __GDL_VERSION_H__
#define __GDL_VERSION_H__

#define GDL_VERSION_MAJOR           3
#define GDL_VERSION_MINOR           0
#define GDL_VERSION_HOTFIX          0

#define GDL_HEADER_VERSION_MAJOR    65
#define GDL_HEADER_VERSION_MINOR    4

/* Macro to create a GDL version number from major, minor, and hotfix
   components.  The GDL version number is of type gdl_uint32.
*/
#define MAKE_GDL_VERSION(major,minor,hotfix) \
    ((((major)<<24) & 0xff000000) | (((minor)<<8) & 0xffff00) | (hotfix & 0xff))

/* Macro to return the major/minor/hotfix versions from a GDL version number. */
#define GET_GDL_VERSION_MAJOR(version)  (((version) >> 24) & 0xff)
#define GET_GDL_VERSION_MINOR(version)  (((version) >> 8)  & 0xffff)
#define GET_GDL_VERSION_HOTFIX(version) ((version) & 0xff)

#endif
