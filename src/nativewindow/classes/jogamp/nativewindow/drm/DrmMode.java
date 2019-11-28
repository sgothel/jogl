/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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
package jogamp.nativewindow.drm;

import java.io.PrintStream;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.nativewindow.NativeWindowException;

import jogamp.nativewindow.drm.DRMLib;
import jogamp.nativewindow.drm.drmModeConnector;
import jogamp.nativewindow.drm.drmModeEncoder;
import jogamp.nativewindow.drm.drmModeModeInfo;
import jogamp.nativewindow.drm.drmModeRes;

/**
 * Describing a DRM adapter's connected {@link drmModeConnector}
 * and it's {@link drmModeModeInfo}, {@link drmModeEncoder} and CRT index.
 */
public class DrmMode {
    /** DRM file descriptor, valid if >= 0 */
    public final int drmFd;
    /** Number of connected {@link drmModeConnector}s and hence length of all arrays within this instance. */
    public final int count;
    /** Connected {@link drmModeConnector}, multiple outputs supported. Array can be of length zero if none is connected. */
    private final drmModeConnector[] connectors;
    /** Selected current mode {@link drmModeModeInfo}. Array index matches the {@link #connectors}. */
    private final drmModeModeInfo[] modes;
    /** Selected {@link drmModeEncoder}. Array index matches the {@link #connectors}. */
    private final drmModeEncoder[] encoder;
    /** Selected CRT IDs. Array index matches the {@link #connectors}. */
    private final int[] crtc_ids;
    /** Selected CRT indices. Array index matches the {@link #connectors}. */
    private final int[] crtc_indices;
    /** boolean indicating that instance data is valid reflecting native DRM data and has not been {@link #destroy()}ed. */
    private volatile boolean valid;

    private DrmMode(final int drmFd, final int count) {
        this.drmFd = drmFd;
        this.count = count;
        this.connectors = new drmModeConnector[count];
        this.modes = new drmModeModeInfo[count];
        this.encoder = new drmModeEncoder[count];
        this.crtc_ids = new int[count];
        this.crtc_indices = new int[count];
        this.valid = false;
    }

    public void print(final PrintStream out) {
        for(int i=0; i<count; i++) {
            print(out, i);
        }
    }
    public void print(final PrintStream out, final int connectorIdx) {
        final drmModeConnector c = connectors[connectorIdx];
        out.printf("Connector[%d]: id[con 0x%x, enc 0x%x], type %d[id 0x%x], connection %d, dim %dx%x mm, modes %d, encoders %d\n",
                connectorIdx, c.getConnector_id(), c.getEncoder_id(),
                c.getConnector_type(), c.getConnector_type_id(), c.getConnection(), c.getMmWidth(), c.getMmHeight(),
                c.getCount_modes(), c.getCount_encoders());
        final drmModeModeInfo m = modes[connectorIdx];
        System.err.printf( "Connector[%d].Mode: clock %d, %dx%d @ %d Hz, type %d, name <%s>\n",
                connectorIdx, m.getClock(), m.getHdisplay(), m.getVdisplay(), m.getVrefresh(),
                m.getType(), m.getNameAsString());
        final drmModeEncoder e = encoder[connectorIdx];
        System.err.printf( "Connector[%d].Encoder: id 0x%x, type %d, crtc_id 0x%x, possible[crtcs %d, clones %d]\n",
                connectorIdx, e.getEncoder_id(), e.getEncoder_type(), e.getCrtc_id(),
                e.getPossible_crtcs(), e.getPossible_clones());
    }

    /**
     * Collecting all connected {@link drmModeConnector}
     * and it's {@link drmModeModeInfo}, {@link drmModeEncoder} and CRT index.
     *
     * @param drmFd the DRM file descriptor
     * @param preferNativeMode chose {@link DRMLib#DRM_MODE_TYPE_PREFERRED}
     */
    public static DrmMode create(final int drmFd, final boolean preferNativeMode) {
        final drmModeRes resources = DRMLib.drmModeGetResources(drmFd);
        if( null == resources ) {
            throw new NativeWindowException("drmModeGetResources failed");
        }
        DrmMode res = null;
        try {
            {
                final List<drmModeConnector> _connectors = new ArrayList<drmModeConnector>();
                final IntBuffer _connectorIDs = resources.getConnectors();
                if(DRMUtil.DEBUG) {
                    for(int i=0; i<_connectorIDs.limit(); i++) {
                        final drmModeConnector c = DRMLib.drmModeGetConnector(drmFd, _connectorIDs.get(i));
                        final boolean chosen = DRMLib.DRM_MODE_CONNECTED == c.getConnection();
                        System.err.printf("Connector %d/%d chosen %b,: id[con 0x%x, enc 0x%x], type %d[id 0x%x], connection %d, dim %dx%x mm, modes %d, encoders %d\n",
                                i, _connectorIDs.limit(), chosen, c.getConnector_id(), c.getEncoder_id(),
                                c.getConnector_type(), c.getConnector_type_id(), c.getConnection(), c.getMmWidth(), c.getMmHeight(),
                                c.getCount_modes(), c.getCount_encoders());
                        DRMLib.drmModeFreeConnector(c);
                    }
                }
                drmModeConnector con = null;
                for(int i=0; i<_connectorIDs.limit(); i++) {
                    con = DRMLib.drmModeGetConnector(drmFd, _connectorIDs.get(i));
                    if( DRMLib.DRM_MODE_CONNECTED == con.getConnection() ) {
                        _connectors.add(con);
                    } else {
                        DRMLib.drmModeFreeConnector(con);
                        con = null;
                    }
                }
                res = new DrmMode(drmFd, _connectors.size());
                _connectors.toArray(res.connectors);
            }
            for(int k=0; k<res.count; k++) {
                final drmModeModeInfo _modes[] = res.connectors[k].getModes(0, new drmModeModeInfo[res.connectors[k].getCount_modes()]);
                drmModeModeInfo _mode = null;
                {
                    int maxArea = 0;
                    int j=0;
                    for(int i=0; i<_modes.length; i++) {
                        final drmModeModeInfo m = _modes[i];
                        final int area = m.getHdisplay() * m.getVdisplay();
                        if( preferNativeMode && m.getType() == DRMLib.DRM_MODE_TYPE_PREFERRED ) {
                            _mode = m;
                            maxArea = Integer.MAX_VALUE;
                            j = i;
                            // only continue loop for DEBUG verbosity
                        } else if( area > maxArea ) {
                            _mode = m;
                            maxArea = area;
                            j = i;
                        }
                        if( DRMUtil.DEBUG ) {
                            System.err.printf( "Connector[%d].Mode %d/%d (max-chosen %d): clock %d, %dx%d @ %d Hz, type %d, name <%s>\n",
                                    k, i, _modes.length, j,
                                    m.getClock(), m.getHdisplay(), m.getVdisplay(), m.getVrefresh(),
                                    m.getType(), m.getNameAsString());
                        }
                    }
                }
                if( null == _mode ) {
                    throw new NativeWindowException("could not find mode");
                }
                res.modes[k] = _mode;
            }
            {
                final IntBuffer encoderIDs = resources.getEncoders();
                for(int k=0; k<res.count; k++) {
                    if( DRMUtil.DEBUG ) {
                        for (int i = 0; i < encoderIDs.limit(); i++) {
                            final drmModeEncoder e = DRMLib.drmModeGetEncoder(drmFd, encoderIDs.get(i));
                            final boolean chosen = e.getEncoder_id() == res.connectors[k].getEncoder_id();
                            System.err.printf( "Connector[%d].Encoder %d/%d chosen %b: id 0x%x, type %d, crtc_id 0x%x, possible[crtcs %d, clones %d]\n",
                                k, i, encoderIDs.limit(), chosen,
                                e.getEncoder_id(), e.getEncoder_type(), e.getCrtc_id(),
                                e.getPossible_crtcs(), e.getPossible_clones());
                            DRMLib.drmModeFreeEncoder(e);
                        }
                    }
                    drmModeEncoder e = null;
                    for (int i = 0; i < encoderIDs.limit(); i++) {
                        e = DRMLib.drmModeGetEncoder(drmFd, encoderIDs.get(i));
                        if( e.getEncoder_id() == res.connectors[k].getEncoder_id() ) {
                            break;
                        } else {
                            DRMLib.drmModeFreeEncoder(e);
                            e = null;
                        }
                    }
                    if( null == e ) {
                        throw new NativeWindowException("could not find encoder");
                    }
                    res.encoder[k] = e;
                }
            }
            {
                final IntBuffer crtcs = resources.getCrtcs();
                for(int k=0; k<res.count; k++) {
                    int idx = -1;
                    for(int i=0; i<crtcs.limit(); i++) {
                        if( crtcs.get(i) == res.encoder[k].getCrtc_id() ) {
                            idx = i;
                            break;
                        }
                    }
                    if( 0 > idx ) {
                        throw new NativeWindowException("could not find crtc index");
                    }
                    res.crtc_ids[k] = crtcs.get(idx);
                    res.crtc_indices[k] = idx;
                }
            }
        } catch (final Throwable t) {
            if( null != res ) {
                res.destroy();
                res = null;
            }
            throw t;
        } finally {
            DRMLib.drmModeFreeResources(resources);
        }
        res.valid = true;
        return res;
    }

    /**
     * Returns whether instance data is valid reflecting native DRM data and has not been {@link #destroy()}ed.
     */
    public final boolean isValid() {
        return valid;
    }
    private final void checkValid() {
        if( !valid ) {
            throw new IllegalStateException("Instance is invalid");
        }
    }


    /**
     * Frees all native DRM resources collected by one of the static methods like {@link #create(int, boolean)}.
     * <p>
     * Method should be issued before shutting down or releasing the {@link #drmFd} via {@link DRMLib#drmClose(int)}.
     * </p>
     */
    public final void destroy() {
        if( valid ) {
            synchronized( this ) {
                if( valid ) {
                    valid = false;
                    for(int i=0; i<count; i++) {
                        if( null != encoder[i] ) {
                            DRMLib.drmModeFreeEncoder(encoder[i]);
                        }
                        if( null != connectors[i]) {
                            DRMLib.drmModeFreeConnector(connectors[i]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns an array for each connected {@link drmModeConnector}s.
     * <p>
     * Returned array length is zero if no {@link drmModeConnector} is connected.
     * </p>
     * @throws IllegalStateException if instance is not {@link #isValid()}.
     */
    public final drmModeConnector[] getConnectors() throws IllegalStateException
    { checkValid(); return connectors; }

    /**
     * Returns an array of {@link drmModeModeInfo} for each connected {@link #getConnectors()}'s current mode.
     * @throws IllegalStateException if instance is not {@link #isValid()}.
     */
    public final drmModeModeInfo[] getModes() throws IllegalStateException
    { checkValid(); return modes; }

    /**
     * Returns an array of {@link drmModeEncoder} for each connected {@link #getConnectors()}.
     * @throws IllegalStateException if instance is not {@link #isValid()}.
     */
    public final drmModeEncoder[] getEncoder() throws IllegalStateException
    {  checkValid(); return encoder; }

    /**
     * Returns an array of selected CRT IDs for each connected {@link #getConnectors()}.
     * @throws IllegalStateException if instance is not {@link #isValid()}.
     */
    public final int[] getCrtcIDs() throws IllegalStateException
    { checkValid(); return crtc_ids; }

    /**
     * Returns an array of selected CRT indices for each connected {@link #getConnectors()}.
     * @throws IllegalStateException if instance is not {@link #isValid()}.
     */
    public final int[] getCrtcIndices() throws IllegalStateException
    { checkValid(); return crtc_indices; }
}