/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

#ifndef _INPUT_EVENT_H_
#define _INPUT_EVENT_H_

#define EVENT_SHIFT_MASK      (1 <<  0)
#define EVENT_CTRL_MASK       (1 <<  1)
#define EVENT_META_MASK       (1 <<  2)
#define EVENT_ALT_MASK        (1 <<  3)
#define EVENT_ALT_GRAPH_MASK  (1 <<  4)

#define EVENT_BUTTON1_MASK    (1 <<  5)
#define EVENT_BUTTON2_MASK    (1 <<  6)
#define EVENT_BUTTON3_MASK    (1 <<  7)
#define EVENT_BUTTON4_MASK    (1 <<  8)
#define EVENT_BUTTON5_MASK    (1 <<  9)
#define EVENT_BUTTON6_MASK    (1 << 10)
#define EVENT_BUTTON7_MASK    (1 << 11)
#define EVENT_BUTTON8_MASK    (1 << 12)
#define EVENT_BUTTON9_MASK    (1 << 13)

/** 16 buttons */
#define EVENT_BUTTONLAST_MASK (1 << 20)

/** 16 buttons */
#define EVENT_BUTTONALL_MASK ( 0xffff << 5 )

#define EVENT_AUTOREPEAT_MASK (1 << 29)
#define EVENT_CONFINED_MASK   (1 << 30)
#define EVENT_INVISIBLE_MASK  (1 << 31)

#endif
