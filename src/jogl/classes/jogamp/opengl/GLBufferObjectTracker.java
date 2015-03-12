/**
 * Copyright 2014 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package jogamp.opengl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.*;
import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.nio.Buffers;
import com.jogamp.common.util.IntObjectHashMap;
import com.jogamp.common.util.PropertyAccess;

/**
 * Tracking of {@link GLBufferStorage} instances via GL API callbacks.
 * <p>
 * See {@link GLBufferStorage} for generic details.
 * </p>
 * <p>
 * Buffer storage is created via
 * <ul>
 *   <li>{@link GL#glBufferData(int, long, java.nio.Buffer, int)} - storage recreation with target via {@link #createBufferStorage(GLBufferStateTracker, GL, int, long, Buffer, int, int, CreateStorageDispatch, long)}</li>
 *   <li>{@link GL2#glNamedBufferDataEXT(int, long, java.nio.Buffer, int)} - storage recreation, direct, via {@link #createBufferStorage(GL, int, long, Buffer, int, CreateStorageDispatch, long)}</li>
 *   <li>{@link GL4#glNamedBufferData(int, long, java.nio.Buffer, int)} - storage recreation, direct, via {@link #createBufferStorage(GL, int, long, Buffer, int, CreateStorageDispatch, long)}</li>
 *   <li>{@link GL4#glBufferStorage(int, long, Buffer, int)} - storage creation with target via {@link #createBufferStorage(GLBufferStateTracker, GL, int, long, Buffer, int, int, CreateStorageDispatch, long)}</li>
 *   <li>{@link GL4#glNamedBufferStorage(int, long, Buffer, int)} - storage recreation, direct, via {@link #createBufferStorage(GL, int, long, Buffer, int, CreateStorageDispatch, long)}</li>
 * </ul>
 * Note that storage <i>recreation</i> as mentioned above also invalidate a previous storage instance,
 * i.e. disposed the buffer's current storage if exist and attaches a new storage instance.
 * </p>
 * <p>
 * Buffers storage is disposed via
 * <ul>
 *   <li>{@link GL#glDeleteBuffers(int, IntBuffer)} - explicit, direct, via {@link #notifyBuffersDeleted(int, IntBuffer)} or {@link #notifyBuffersDeleted(int, int[], int)}</li>
 *   <li>{@link GL#glBufferData(int, long, java.nio.Buffer, int)} - storage recreation via target</li>
 *   <li>{@link GL2#glNamedBufferDataEXT(int, long, java.nio.Buffer, int)} - storage recreation, direct</li>
 *   <li>{@link GL4#glNamedBufferData(int, long, java.nio.Buffer, int)} - storage recreation, direct</li>
 *   <li>{@link GL4#glBufferStorage(int, long, Buffer, int)} - storage recreation via target</li>
 *   <li>{@link GL4#glNamedBufferStorage(int, long, Buffer, int)} - storage recreation, direct</li>
 * </ul>
 * </p>
 *
 * <p>
 * Implementation throws a {@link GLException} in all <i>construction</i> methods as listed below,
 * if GL-function constraints are not met. <code>createBufferStorage</code> also throws an exception
 * if the native GL-function fails.
 * <ul>
 *   <li>{@link #createBufferStorage(GLBufferStateTracker, GL, int, long, Buffer, int, int, CreateStorageDispatch, long)}, etc ..</li>
 *   <li>{@link #mapBuffer(GLBufferStateTracker, GL, int, int, MapBufferAllDispatch, long)}, etc ..</li>
 * </ul>
 * In <i>destruction</i> and informal methods, i.e. all others, implementation only issues a WARNING debug message, if enabled.
 * </p>
 *
 * <p>
 * Buffer mapping methods like {@link GL#mapBuffer(int, int)} ...,
 * require knowledge of the buffer's storage size as determined via it's creation with
 * {@link GL#glBufferData(int, long, java.nio.Buffer, int)} ....
 * </p>
 * <p>
 * Hence we track the OpenGL buffer's {@link GLBufferStorage} to be able to
 * access the buffer's storage size. The tracked {@link GLBufferStorage} instances
 * also allow users to conveniently access details of their created and maybe mapped buffer storage.
 * </p>
 * <p>
 * The {@link GLBufferObjectTracker} and it's tracked {@link GLBufferStorage} instances
 * maybe shared across multiple OpenGL context, hence this class is thread safe and employs synchronization.
 * </p>
 * <p>
 * Implementation requires and utilizes a local {@link GLBufferStateTracker}
 * to resolve the actual buffer-name bound to the given target.
 * </p>
 * <p>
 * Note: This tracker requires to be notified about all OpenGL buffer storage operations,
 * as well as the local {@link GLBufferStateTracker} to be notified about all
 * OpenGL buffer binding operations.
 * Hence buffer storage cannot be accessed properly if managed via native code.
 * </p>
 */
public class GLBufferObjectTracker {
    protected static final boolean DEBUG;

    static {
        Debug.initSingleton();
        DEBUG = PropertyAccess.isPropertyDefined("jogl.debug.GLBufferObjectTracker", true);
    }

    static final class GLBufferStorageImpl extends GLBufferStorage {
        GLBufferStorageImpl(final int name, final long size, final int mutableUsage, final int immutableFlags) {
            super(name, size, mutableUsage, immutableFlags);
        }
        protected final void reset(final long size, final int mutableUsage, final int immutableFlags) {
            super.reset(size, mutableUsage, immutableFlags);
        }
        protected final void setMappedBuffer(final ByteBuffer buffer) {
            super.setMappedBuffer(buffer);
        }
    }

    /**
     * Map from buffer names to GLBufferObject.
     */
    private final IntObjectHashMap bufferName2StorageMap;

    public GLBufferObjectTracker() {
        bufferName2StorageMap = new IntObjectHashMap();
        bufferName2StorageMap.setKeyNotFoundValue(null);
    }

    public static interface CreateStorageDispatch {
        void create(final int targetOrBufferName, final long size, final Buffer data, final int mutableUsageOrImmutableFlags);
    }

    /**
     * Must be called when [re]creating the GL buffer object via {@link GL#glBufferData(int, long, java.nio.Buffer, int)}
     * and {@link GL4#glBufferStorage(int, long, Buffer, int)},
     * i.e. implies destruction of the buffer.
     *
     * @param bufferStateTracker
     * @param caller
     * @param target
     * @param size
     * @param mutableUsage <code>glBufferData</code>, <code>glNamedBufferData</code> usage
     * @param immutableFlags <code>glBufferStorage</code> flags
     * @throws GLException if buffer is not bound to target
     * @throws GLException if size is less-or-eqaul zero for <code>glBufferStorage</code>, or size is less-than zero otherwise
     * @throws GLException if a native GL-Error occurs
     */
    public synchronized final void createBufferStorage(final GLBufferStateTracker bufferStateTracker, final GL caller,
                                                       final int target, final long size, final Buffer data, final int mutableUsage, final int immutableFlags,
                                                       final CreateStorageDispatch dispatch) throws GLException {
        final int glerrPre = caller.glGetError(); // clear
        if (DEBUG && GL.GL_NO_ERROR != glerrPre) {
            System.err.printf("%s.%s glerr-pre 0x%X%n", msgClazzName, msgCreateBound, glerrPre);
        }
        final int bufferName = bufferStateTracker.getBoundBufferObject(target, caller);
        if ( 0 == bufferName ) {
            throw new GLException(String.format("%s: Buffer for target 0x%X not bound", GL_INVALID_OPERATION, target));
        }
        final boolean mutableBuffer = 0 != mutableUsage;
        final boolean invalidSize = (  mutableBuffer && 0 > size )   // glBufferData, glNamedBufferData
                                 || ( !mutableBuffer && 0 >= size ); // glBufferStorage
        if( invalidSize ) {
            throw new GLException(String.format("%s: Invalid size %d for buffer %d on target 0x%X", GL_INVALID_VALUE, size, bufferName, target));
        }

        dispatch.create(target, size, data, mutableBuffer ? mutableUsage : immutableFlags);
        final int glerrPost = caller.glGetError(); // be safe, catch failure!
        if(GL.GL_NO_ERROR != glerrPost) {
            throw new GLException(String.format("GL-Error 0x%X while creating %s storage for target 0x%X -> buffer %d of size %d with data %s",
                    glerrPost, mutableBuffer ? "mutable" : "immutable", target, bufferName, size, data));
        }
        final GLBufferStorageImpl objOld = (GLBufferStorageImpl) bufferName2StorageMap.get(bufferName);
        if( null != objOld ) {
            objOld.reset(size, mutableUsage, immutableFlags);
            if (DEBUG) {
                System.err.printf("%s.%s target: 0x%X -> reset %d: %s%n", msgClazzName, msgCreateBound, target, bufferName, objOld);
            }
        } else {
            final GLBufferStorageImpl objNew = new GLBufferStorageImpl(bufferName, size, mutableUsage, immutableFlags);
            bufferName2StorageMap.put(bufferName, objNew);
            if (DEBUG) {
                System.err.printf("%s.%s target: 0x%X -> new %d: %s%n", msgClazzName, msgCreateBound, target, bufferName, objNew);
            }
        }
    }

    /**
     * Must be called when [re]creating the GL buffer object
     * via {@link GL2#glNamedBufferDataEXT(int, long, java.nio.Buffer, int) glNamedBufferDataEXT},
     * {@link GL4#glNamedBufferData(int, long, java.nio.Buffer, int) glNamedBufferData},
     * and {@link GL4#glNamedBufferStorage(int, long, Buffer, int)},
     * i.e. implies destruction of the buffer.
     *
     * @param bufferName
     * @param size
     * @param mutableUsage
     * @throws GLException if size is less-than zero
     * @throws GLException if a native GL-Error occurs
     */
    public synchronized final void createBufferStorage(final GL caller,
                                                       final int bufferName, final long size, final Buffer data, final int mutableUsage, final int immutableFlags,
                                                       final CreateStorageDispatch dispatch) throws GLException {
        final int glerrPre = caller.glGetError(); // clear
        if (DEBUG && GL.GL_NO_ERROR != glerrPre) {
            System.err.printf("%s.%s glerr-pre 0x%X%n", msgClazzName, msgCreateNamed, glerrPre);
        }
        if ( 0 > size ) { // glBufferData, glNamedBufferData
            throw new GLException(String.format("%s: Invalid size %d for buffer %d", GL_INVALID_VALUE, size, bufferName));
        }
        final boolean mutableBuffer = 0 != mutableUsage;
        if( !mutableBuffer ) {
            throw new InternalError("Immutable glNamedBufferStorage not supported yet");
        }
        dispatch.create(bufferName, size, data, mutableUsage);
        final int glerrPost = caller.glGetError(); // be safe, catch failure!
        if(GL.GL_NO_ERROR != glerrPost) {
            throw new GLException(String.format("GL-Error 0x%X while creating %s storage for buffer %d of size %d with data %s",
                                                glerrPost, "mutable", bufferName, size, data));
        }
        final GLBufferStorageImpl objOld = (GLBufferStorageImpl) bufferName2StorageMap.get(bufferName);
        if( null != objOld ) {
            objOld.reset(size, mutableUsage, immutableFlags);
            if (DEBUG) {
                System.err.printf("%s.%s direct: reset %d: %s%n", msgClazzName, msgCreateNamed, bufferName, objOld);
            }
        } else {
            final GLBufferStorageImpl objNew = new GLBufferStorageImpl(bufferName, size, mutableUsage, immutableFlags);
            bufferName2StorageMap.put(bufferName, objNew);
            if (DEBUG) {
                System.err.printf("%s.%s direct: new %d: %s%n", msgClazzName, msgCreateNamed, bufferName, objNew);
            }
        }
    }

    /**
     * Must be called when deleting GL buffer objects vis <code>glDeleteBuffers</code>.
     * @param count
     * @param bufferNames
     * @param offset
     */
    public synchronized final void notifyBuffersDeleted(final int count, final int[] bufferNames, final int offset) {
        for(int i=0; i<count; i++) {
            notifyBufferDeleted(bufferNames[i+offset], i, count);
        }
    }
    /**
     * Must be called when deleting GL buffer objects vis <code>glDeleteBuffers</code>.
     * @param n
     * @param bufferNames
     */
    public synchronized final void notifyBuffersDeleted(final int n, final IntBuffer bufferNames) {
        final int offset = bufferNames.position();
        for(int i=0; i<n; i++) {
            notifyBufferDeleted(bufferNames.get(i+offset), i, n);
        }
    }
    /**
     * Must be called when deleting GL buffer objects vis {@link GL#glDeleteBuffers(int, IntBuffer)}.
     * @param bufferName
     * @param i
     * @param count
     */
    private synchronized final void notifyBufferDeleted(final int bufferName, final int i, final int count) {
        final GLBufferStorageImpl objOld = (GLBufferStorageImpl) bufferName2StorageMap.put(bufferName, null);
        if (DEBUG) {
            System.err.printf("%s.notifyBuffersDeleted()[%d/%d]: %d: %s -> null%n", msgClazzName, i+1, count, bufferName, objOld);
        }
        if( null == objOld ) {
            if (DEBUG) {
                System.err.printf("%s: %s.notifyBuffersDeleted()[%d/%d]: Buffer %d not tracked%n", warning, msgClazzName, i+1, count, bufferName);
                ExceptionUtils.dumpStack(System.err);
            }
            return;
        }
        objOld.setMappedBuffer(null);
    }

    public static interface MapBufferDispatch {
        ByteBuffer allocNioByteBuffer(final long addr, final long length);
    }
    public static interface MapBufferRangeDispatch extends MapBufferDispatch {
        long mapBuffer(final int targetOrBufferName, final long offset, final long length, final int access);
    }
    public static interface MapBufferAllDispatch extends MapBufferDispatch {
        long mapBuffer(final int targetOrBufferName, final int access);
    }

    private static final String GL_INVALID_OPERATION = "GL_INVALID_OPERATION";
    private static final String GL_INVALID_VALUE = "GL_INVALID_VALUE";

    /**
     * Must be called when mapping GL buffer objects via {@link GL#mapBuffer(int, int)}.
     * @throws GLException if buffer is not bound to target
     * @throws GLException if buffer is not tracked
     * @throws GLException if buffer is already mapped
     * @throws GLException if buffer has invalid store size, i.e. less-than zero
     */
    public synchronized final GLBufferStorage mapBuffer(final GLBufferStateTracker bufferStateTracker,
                                                        final GL caller, final int target, final int access,
                                                        final MapBufferAllDispatch dispatch) throws GLException {
        return this.mapBufferImpl(bufferStateTracker, caller, target, false /* useRange */, 0 /* offset */, 0 /* length */, access, dispatch);
    }
    /**
     * Must be called when mapping GL buffer objects via {@link GL#mapBufferRange(int, long, long, int)}.
     * @throws GLException if buffer is not bound to target
     * @throws GLException if buffer is not tracked
     * @throws GLException if buffer is already mapped
     * @throws GLException if buffer has invalid store size, i.e. less-than zero
     * @throws GLException if buffer mapping range does not fit, incl. offset
     */
    public synchronized final GLBufferStorage mapBuffer(final GLBufferStateTracker bufferStateTracker,
                                                        final GL caller, final int target, final long offset, final long length, final int access,
                                                        final MapBufferRangeDispatch dispatch) throws GLException {
        return this.mapBufferImpl(bufferStateTracker, caller, target, true /* useRange */, offset, length, access, dispatch);
    }
    /**
     * Must be called when mapping GL buffer objects via {@link GL2#mapNamedBufferEXT(int, int)}.
     * @throws GLException if buffer is not tracked
     * @throws GLException if buffer is already mapped
     * @throws GLException if buffer has invalid store size, i.e. less-than zero
     */
    public synchronized final GLBufferStorage mapBuffer(final int bufferName, final int access, final MapBufferAllDispatch dispatch) throws GLException {
        return this.mapBufferImpl(0 /* target */, bufferName, true /* isNamedBuffer */, false /* useRange */, 0 /* offset */, 0 /* length */, access, dispatch);
    }
    /**
     * Must be called when mapping GL buffer objects via {@link GL2#mapNamedBufferRangeEXT(int, long, long, int)}.
     * @throws GLException if buffer is not tracked
     * @throws GLException if buffer is already mapped
     * @throws GLException if buffer has invalid store size, i.e. less-than zero
     * @throws GLException if buffer mapping range does not fit, incl. offset
     */
    public synchronized final GLBufferStorage mapBuffer(final int bufferName, final long offset, final long length, final int access, final MapBufferRangeDispatch dispatch) throws GLException {
        return this.mapBufferImpl(0 /* target */, bufferName, true /* isNamedBuffer */, true /* useRange */, offset, length, access, dispatch);
    }
    /**
     * @throws GLException if buffer is not bound to target
     * @throws GLException if buffer is not tracked
     * @throws GLException if buffer is already mapped
     * @throws GLException if buffer has invalid store size, i.e. less-than zero
     * @throws GLException if buffer mapping range does not fit, incl. optional offset
     */
    private synchronized final GLBufferStorage mapBufferImpl(final GLBufferStateTracker bufferStateTracker,
                                                             final GL caller, final int target, final boolean useRange,
                                                             final long offset, final long length, final int access,
                                                             final MapBufferDispatch dispatch) throws GLException {
        final int bufferName = bufferStateTracker.getBoundBufferObject(target, caller);
        if( 0 == bufferName ) {
            throw new GLException(String.format("%s.%s: %s Buffer for target 0x%X not bound", msgClazzName, msgMapBuffer, GL_INVALID_OPERATION, target));
        }
        return this.mapBufferImpl(target, bufferName, false /* isNamedBuffer */, useRange, offset, length, access, dispatch);
    }
    /**
     * <p>
     * A zero store size will avoid a native call and returns the unmapped {@link GLBufferStorage}.
     * </p>
     * <p>
     * A null native mapping result indicating an error will
     * not cause a GLException but returns the unmapped {@link GLBufferStorage}.
     * This allows the user to handle this case.
     * </p>
     * @throws GLException if buffer is not tracked
     * @throws GLException if buffer is already mapped
     * @throws GLException if buffer has invalid store size, i.e. less-than zero
     * @throws GLException if buffer mapping range does not fit, incl. optional offset
     */
    private synchronized final GLBufferStorage mapBufferImpl(final int target, final int bufferName, final boolean isNamedBuffer, final boolean useRange, long offset,
                                                             long length, final int access, final MapBufferDispatch dispatch) throws GLException {
        final GLBufferStorageImpl store = (GLBufferStorageImpl)bufferName2StorageMap.get(bufferName);
        if ( null == store ) {
            throw new GLException("Buffer with name "+bufferName+" not tracked");
        }
        if( null != store.getMappedBuffer() ) {
            throw new GLException(String.format("%s.%s: %s Buffer storage of target 0x%X -> %d: %s is already mapped", msgClazzName, msgMapBuffer, GL_INVALID_OPERATION, target, bufferName, store));
        }
        final long storeSize = store.getSize();
        if ( 0 > storeSize ) {
            throw new GLException(String.format("%s.%s: %s Buffer storage of target 0x%X -> %d: %s is of less-than zero", msgClazzName, msgMapBuffer, GL_INVALID_OPERATION, target, bufferName, store));
        }
        if( !useRange ) {
            length = storeSize;
            offset = 0;
        }
        if( length + offset > storeSize ) {
            throw new GLException(String.format("%s.%s: %s Out of range: offset %d, length %d, buffer storage of target 0x%X -> %d: %s", msgClazzName, msgMapBuffer, GL_INVALID_VALUE, offset, length, target, bufferName, store));
        }
        if( 0 >= length || 0 > offset ) {
            throw new GLException(String.format("%s.%s: %s Invalid values: offset %d, length %d, buffer storage of target 0x%X -> %d: %s", msgClazzName, msgMapBuffer, GL_INVALID_VALUE, offset, length, target, bufferName, store));
        }
        if( 0 == storeSize ) {
            return store;
        }
        final long addr;
        if( isNamedBuffer ) {
            if( useRange ) {
                addr = ((MapBufferRangeDispatch)dispatch).mapBuffer(bufferName, offset, length, access);
            } else {
                addr = ((MapBufferAllDispatch)dispatch).mapBuffer(bufferName, access);
            }
        } else {
            if( useRange ) {
                addr = ((MapBufferRangeDispatch)dispatch).mapBuffer(target, offset, length, access);
            } else {
                addr = ((MapBufferAllDispatch)dispatch).mapBuffer(target, access);
            }
        }
        // GL's map-buffer implementation always returns NULL on error,
        // user shall validate the result and the corresponding getGLError() value!
        if ( 0 == addr ) {
            if( DEBUG ) {
                System.err.printf("%s.%s: %s MapBuffer null result for target 0x%X -> %d: %s, off %d, len %d, acc 0x%X%n", msgClazzName, msgMapBuffer, warning, target, bufferName, store, offset, length, access);
                ExceptionUtils.dumpStack(System.err);
            }
            // User shall handle the glError !
        } else {
            final ByteBuffer buffer = dispatch.allocNioByteBuffer(addr, length);
            Buffers.nativeOrder(buffer);
            if( DEBUG ) {
                System.err.printf("%s.%s: Target 0x%X -> %d: %s, off %d, len %d, acc 0x%X%n", msgClazzName, msgClazzName, target, bufferName, store.toString(false), offset, length, access);
            }
            store.setMappedBuffer(buffer);
        }
        return store;
    }

    public static interface UnmapBufferDispatch {
        boolean unmap(final int targetOrBufferName);
    }

    /**
     * Must be called when unmapping GL buffer objects via {@link GL#glUnmapBuffer(int)}.
     * <p>
     * Only clear mapped buffer reference of {@link GLBufferStorage}
     * if native unmapping was successful.
     * </p>
     */
    public synchronized final boolean unmapBuffer(final GLBufferStateTracker bufferStateTracker, final GL caller,
                                                  final int target,
                                                  final UnmapBufferDispatch dispatch) {
        final int bufferName = bufferStateTracker.getBoundBufferObject(target, caller);
        final GLBufferStorageImpl store;
        if( 0 == bufferName ) {
            if (DEBUG) {
                System.err.printf("%s: %s.%s: Buffer for target 0x%X not bound%n", warning, msgClazzName, msgUnmapped, target);
                ExceptionUtils.dumpStack(System.err);
            }
            store = null;
        } else {
            store = (GLBufferStorageImpl) bufferName2StorageMap.get(bufferName);
            if( DEBUG && null == store ) {
                System.err.printf("%s: %s.%s: Buffer %d not tracked%n", warning, msgClazzName, msgUnmapped, bufferName);
                ExceptionUtils.dumpStack(System.err);
            }
        }
        final boolean res = dispatch.unmap(target);
        if( res && null != store ) {
            store.setMappedBuffer(null);
        }
        if( DEBUG ) {
            System.err.printf("%s.%s %s target: 0x%X -> %d: %s%n", msgClazzName, msgUnmapped, res ? "OK" : "Failed", target, bufferName, store.toString(false));
            if(!res) {
                ExceptionUtils.dumpStack(System.err);
            }
        }
        return res;
    }
    /**
     * Must be called when unmapping GL buffer objects via {@link GL2#glUnmapNamedBuffer(int)}.
     * <p>
     * Only clear mapped buffer reference of {@link GLBufferStorage}
     * if native unmapping was successful.
     * </p>
     */
    public synchronized final boolean unmapBuffer(final int bufferName,
                                                  final UnmapBufferDispatch dispatch) {
        final GLBufferStorageImpl store = (GLBufferStorageImpl) bufferName2StorageMap.get(bufferName);
        if (DEBUG && null == store ) {
            System.err.printf("%s: %s.%s: Buffer %d not tracked%n", warning, msgClazzName, msgUnmapped, bufferName);
            ExceptionUtils.dumpStack(System.err);
        }
        final boolean res = dispatch.unmap(bufferName);
        if( res && null != store ) {
            store.setMappedBuffer(null);
        }
        if (DEBUG) {
            System.err.printf("%s.%s %s %d: %s%n", msgClazzName, msgUnmapped, res ? "OK" : "Failed", bufferName, store.toString(false));
            if(!res) {
                ExceptionUtils.dumpStack(System.err);
            }
        }
        return res;
    }

    public synchronized final GLBufferStorage getBufferStorage(final int bufferName) {
        return (GLBufferStorageImpl)bufferName2StorageMap.get(bufferName);
    }

    /**
     * Clear all tracked buffer object knowledge.
     * <p>
     * Shall only be called at GLContext destruction <i>iff</i>
     * there are no other shared GLContext instances left.
     * </p>
     */
    public synchronized final void clear() {
        if (DEBUG) {
          System.err.printf("%s.clear() - Thread %s%n", msgClazzName, Thread.currentThread().getName());
          // ExceptionUtils.dumpStackTrace(System.err);
        }
        bufferName2StorageMap.clear();
    }

    private static final String warning  = "WARNING";
    private static final String msgClazzName = "GLBufferObjectTracker";
    private static final String msgUnmapped = "notifyBufferUnmapped()";
    private static final String msgCreateBound = "createBoundBufferStorage()";
    private static final String msgCreateNamed = "createNamedBufferStorage()";
    private static final String msgMapBuffer = "mapBuffer()";
}
