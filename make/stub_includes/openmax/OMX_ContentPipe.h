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

/** OMX_ContentPipe.h - OpenMax IL version 1.1.1
 *  The OMX_ContentPipe header file contains the definitions used to define
 *  the public interface for content piples.  This header file is intended to
 *  be used by the component.
 */

#ifndef OMX_CONTENTPIPE_H
#define OMX_CONTENTPIPE_H

#ifndef KD_EACCES
/* OpenKODE error codes. CPResult values may be zero (indicating success
   or one of the following values) */
#define KD_EACCES (13)
#define KD_EADDRINUSE (98)
#define KD_EAGAIN (11)
#define KD_EBADF (9)
#define KD_EBUSY (16)
#define KD_ECANCELED (125)
#define KD_ECONNABORTED (103)
#define KD_ECONNREFUSED (111)
#define KD_ECONNRESET (104)
#define KD_EDEADLK (35)
#define KD_EDESTADDRREQ (89)
#define KD_EDOM (33)
#define KD_ERANGE (34)
#define KD_EEXIST (17)
#define KD_EFAULT (14)
#define KD_EFBIG (27)
#define KD_EHOSTUNREACH (113)
#define KD_EINPROGRESS (115)
#define KD_EINTR (4)
#define KD_EINVAL (22)
#define KD_EIO (5)
#define KD_EISCONN (106)
#define KD_EISDIR (21)
#define KD_EMFILE (24)
#define KD_ENAMETOOLONG (36)
#define KD_ENETDOWN (100)
#define KD_ENETRESET (102)
#define KD_ENETUNREACH (101)
#define KD_ENOBUFS (105)
#define KD_ENOENT (2)
#define KD_ENOMEM (12)
#define KD_ENOSPC (28)
#define KD_ENOSYS (28)
#define KD_ENOTCONN (107)
#define KD_ENOTDIR (20)
#define KD_ENOTEMPTY (39)
#define KD_ENOTSOCK (88)
#define KD_ENOTSUP (95)
#define KD_EPERM (1)
#define KD_EPROTO (71)
#define KD_ETIMEDOUT (110)
#define KD_EILSEQ (84)
#endif

/** Map types from OMX standard types only here so interface is as generic as possible. */
typedef OMX_U32    CPresult;
typedef char *     CPstring;  
typedef void *     CPhandle;
typedef OMX_U32    CPuint;
typedef OMX_S32    CPint;  
typedef char       CPbyte;  
typedef OMX_BOOL   CPbool;

/** enumeration of origin types used in the CP_PIPETYPE's Seek function 
 * @ingroup cp
 */
typedef enum CP_ORIGINTYPE {
    CP_OriginBegin,      
    CP_OriginCur,      
    CP_OriginEnd,      
    CP_OriginMax = 0X7FFFFFFF
} CP_ORIGINTYPE;

/** enumeration of contact access types used in the CP_PIPETYPE's Open function 
 * @ingroup cp
 */
typedef enum CP_ACCESSTYPE {
    CP_AccessRead,      
    CP_AccessWrite,  
    CP_AccessReadWrite ,  
    CP_AccessMax = 0X7FFFFFFF
} CP_ACCESSTYPE;

/** enumeration of results returned by the CP_PIPETYPE's CheckAvailableBytes function 
 * @ingroup cp
 */
typedef enum CP_CHECKBYTESRESULTTYPE
{
    CP_CheckBytesOk,                    /**< There are at least the request number 
                                              of bytes available */
    CP_CheckBytesNotReady,              /**< The pipe is still retrieving bytes 
                                              and presently lacks sufficient bytes. 
                                              Client will be called when they are 
                                              sufficient bytes are available. */
    CP_CheckBytesInsufficientBytes  ,     /**< The pipe has retrieved all bytes 
                                              but those available are less than those 
                                              requested */
    CP_CheckBytesAtEndOfStream,         /**< The pipe has reached the end of stream
                                              and no more bytes are available. */
    CP_CheckBytesOutOfBuffers,          /**< All read/write buffers are currently in use. */
    CP_CheckBytesMax = 0X7FFFFFFF
} CP_CHECKBYTESRESULTTYPE;

/** enumeration of content pipe events sent to the client callback. 
 * @ingroup cp
 */
typedef enum CP_EVENTTYPE{
    CP_BytesAvailable,      	    /** bytes requested in a CheckAvailableBytes call are now available*/
    CP_Overflow,  		           /** enumeration of content pipe events sent to the client callback*/
    CP_PipeDisconnected  ,  		    /** enumeration of content pipe events sent to the client callback*/
    CP_EventMax = 0X7FFFFFFF
} CP_EVENTTYPE;

/** content pipe definition 
 * @ingroup cp
 */
typedef struct CP_PIPETYPE
{
    /** Open a content stream for reading or writing. */ 
    CPresult (*Open)( CPhandle* hContent, CPstring szURI, CP_ACCESSTYPE eAccess );

    /** Close a content stream. */ 
    CPresult (*Close)( CPhandle hContent );

    /** Create a content source and open it for writing. */ 
    CPresult (*Create)( CPhandle *hContent, CPstring szURI );

    /** Check the that specified number of bytes are available for reading or writing (depending on access type).*/
    CPresult (*CheckAvailableBytes)( CPhandle hContent, CPuint nBytesRequested, CP_CHECKBYTESRESULTTYPE *eResult );

    /** Seek to certain position in the content relative to the specified origin. */
    CPresult (*SetPosition)( CPhandle  hContent, CPint nOffset, CP_ORIGINTYPE eOrigin);

    /** Retrieve the current position relative to the start of the content. */
    CPresult (*GetPosition)( CPhandle hContent, CPuint *pPosition);

    /** Retrieve data of the specified size from the content stream (advance content pointer by size of data).
       Note: pipe client provides pointer. This function is appropriate for small high frequency reads. */
    CPresult (*Read)( CPhandle hContent, CPbyte *pData, CPuint nSize); 

    /** Retrieve a buffer allocated by the pipe that contains the requested number of bytes. 
       Buffer contains the next block of bytes, as specified by nSize, of the content. nSize also
       returns the size of the block actually read. Content pointer advances the by the returned size. 
       Note: pipe provides pointer. This function is appropriate for large reads. The client must call 
       ReleaseReadBuffer when done with buffer. 

       In some cases the requested block may not reside in contiguous memory within the
       pipe implementation. For instance if the pipe leverages a circular buffer then the requested 
       block may straddle the boundary of the circular buffer. By default a pipe implementation 
       performs a copy in this case to provide the block to the pipe client in one contiguous buffer.
       If, however, the client sets bForbidCopy, then the pipe returns only those bytes preceding the memory 
       boundary. Here the client may retrieve the data in segments over successive calls. */
    CPresult (*ReadBuffer)( CPhandle hContent, CPbyte **ppBuffer, CPuint *nSize, CPbool bForbidCopy);

    /** Release a buffer obtained by ReadBuffer back to the pipe. */
    CPresult (*ReleaseReadBuffer)(CPhandle hContent, CPbyte *pBuffer);

    /** Write data of the specified size to the content (advance content pointer by size of data).
       Note: pipe client provides pointer. This function is appropriate for small high frequency writes. */
    CPresult (*Write)( CPhandle hContent, CPbyte *data, CPuint nSize); 

    /** Retrieve a buffer allocated by the pipe used to write data to the content. 
       Client will fill buffer with output data. Note: pipe provides pointer. This function is appropriate
       for large writes. The client must call WriteBuffer when done it has filled the buffer with data.*/
    CPresult (*GetWriteBuffer)( CPhandle hContent, CPbyte **ppBuffer, CPuint nSize);

    /** Deliver a buffer obtained via GetWriteBuffer to the pipe. Pipe will write the 
       the contents of the buffer to content and advance content pointer by the size of the buffer */
    CPresult (*WriteBuffer)( CPhandle hContent, CPbyte *pBuffer, CPuint nFilledSize);

    /** Register a per-handle client callback with the content pipe. */
    CPresult (*RegisterCallback)( CPhandle hContent, CPresult (*ClientCallback)(CP_EVENTTYPE eEvent, CPuint iParam));

} CP_PIPETYPE;

#endif

