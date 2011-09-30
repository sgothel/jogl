/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
package com.jogamp.android.launcher;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import android.content.Context;

public class LauncherTempFileCache {
    private static final boolean DEBUG = true;

    // Lifecycle: For all JVMs, ClassLoader and times.
    private static final String tmpDirPrefix = "jogamp.tmp.cache";
    
    // Get the value of the tmproot system property
    // Lifecycle: For all JVMs and ClassLoader
    /* package */ static final String tmpRootPropName = "jnlp.jogamp.tmp.cache.root";

    // Flag indicating that we got a fatal error in the static initializer.
    private static boolean staticInitError = false;
    
    private static File tmpBaseDir;

    // String representing the name of the temp root directory relative to the
    // tmpBaseDir. Its value is "jlnNNNNN", which is the unique filename created
    // by File.createTempFile() without the ".tmp" extension.
    //
    // Lifecycle: For all JVMs and ClassLoader
    //
    private static String tmpRootPropValue;

    private static File tmpRootDir;

    // Flag indicating that we got a fatal error in the initializer.
    private boolean initError = false;

    private File individualTmpDir;
    
    /**
     * Documented way to kick off static initialization
     * @return true is static initialization was successful
     */
    public static synchronized boolean initSingleton(Context ctx) {
        if(null == tmpRootDir && !staticInitError) {
            // Create / initialize the temp root directory, starting the Reaper
            // thread to reclaim old installations if necessary. If we get an
            // exception, set an error code.
            try {
                initTmpRoot(ctx);
            } catch (Exception ex) {
                ex.printStackTrace();
                staticInitError = true;
            }
        }
        return !staticInitError;        
    }
    
    /**
     * This method is called by the static initializer to create / initialize
     * the temp root directory that will hold the temp directories for this
     * instance of the JVM. This is done as follows:
     *
     *     1. Synchronize on a global lock. Note that for this purpose we will
     *        use System.out in the absence of a true global lock facility.
     *        We are careful not to hold this lock too long.
     *
     *     2. Check for the existence of the "jnlp.applet.launcher.tmproot"
     *        system property.
     *
     *         a. If set, then some other thread in a different ClassLoader has
     *            already created the tmprootdir, so we just need to
     *            use it. The remaining steps are skipped.
     *
     *         b. If not set, then we are the first thread in this JVM to run,
     *            and we need to create the the tmprootdir.
     *
     *     3. Create the tmprootdir, along with the appropriate locks.
     *        Note that we perform the operations in the following order,
     *        prior to creating tmprootdir itself, to work around the fact that
     *        the file creation and file lock steps are not atomic, and we need
     *        to ensure that a newly-created tmprootdir isn't reaped by a
     *        concurrently running JVM.
     *
     *            create jlnNNNN.tmp using File.createTempFile()
     *            lock jlnNNNN.tmp
     *            create jlnNNNN.lck while holding the lock on the .tmp file
     *            lock jlnNNNN.lck
     *
     *        Since the Reaper thread will enumerate the list of *.lck files
     *        before starting, we can guarantee that if there exists a *.lck file
     *        for an active process, then the corresponding *.tmp file is locked
     *        by that active process. This guarantee lets us avoid reaping an
     *        active process' files.
     *
     *     4. Set the "jnlp.applet.launcher.tmproot" system property.
     *
     *     5. Add a shutdown hook to cleanup jlnNNNN.lck and jlnNNNN.tmp. We
     *        don't actually expect that this shutdown hook will ever be called,
     *        but the act of doing this, ensures that the locks never get
     *        garbage-collected, which is necessary for correct behavior when
     *        the first ClassLoader is later unloaded, while subsequent Applets
     *        are still running.
     *
     *     6. Start the Reaper thread to cleanup old installations.
     */
    private static void initTmpRoot(Context ctx) throws IOException {
        if (DEBUG) {
            System.err.println("TempFileCache: Static Initialization ----------------------------------------------");
            System.err.println("TempFileCache: Thread: "+Thread.currentThread().getName()+", CL 0x"+Integer.toHexString(LauncherTempFileCache.class.getClassLoader().hashCode()));
        }

        synchronized (System.out) {
            // Get the name of the tmpbase directory.
            {
                final File tmpRoot = ctx.getDir("temp", Context.MODE_WORLD_READABLE);
                tmpBaseDir = new File(tmpRoot, tmpDirPrefix);
            }
            tmpRootPropValue = System.getProperty(tmpRootPropName);

            if (tmpRootPropValue == null) {
                // Create the tmpbase directory if it doesn't already exist
                tmpBaseDir.mkdir();
                if (!tmpBaseDir.isDirectory()) {
                    throw new IOException("Cannot create directory " + tmpBaseDir);
                }

                // Create ${tmpbase}/jlnNNNN.tmp then lock the file
                File tmpFile = File.createTempFile("jln", ".tmp", tmpBaseDir);
                if (DEBUG) {
                    System.err.println("TempFileCache: tmpFile = " + tmpFile.getAbsolutePath());
                }
                final FileOutputStream tmpOut = new FileOutputStream(tmpFile);
                final FileChannel tmpChannel = tmpOut.getChannel();
                final FileLock tmpLock = tmpChannel.lock();

                // Strip off the ".tmp" to get the name of the tmprootdir
                String tmpFileName = tmpFile.getAbsolutePath();
                String tmpRootName = tmpFileName.substring(0, tmpFileName.lastIndexOf(".tmp"));

                // create ${tmpbase}/jlnNNNN.lck then lock the file
                String lckFileName = tmpRootName + ".lck";
                File lckFile = new File(lckFileName);
                if (DEBUG) {
                    System.err.println("TempFileCache: lckFile = " + lckFile.getAbsolutePath());
                }
                lckFile.createNewFile();
                final FileOutputStream lckOut = new FileOutputStream(lckFile);
                final FileChannel lckChannel = lckOut.getChannel();
                final FileLock lckLock = lckChannel.lock();

                // Create tmprootdir
                tmpRootDir = new File(tmpRootName);
                if (DEBUG) {
                    System.err.println("TempFileCache: tmpRootDir = " + tmpRootDir.getAbsolutePath());
                }
                if (!tmpRootDir.mkdir()) {
                    throw new IOException("Cannot create " + tmpRootDir);
                }

                // Add shutdown hook to cleanup the OutputStream, FileChannel,
                // and FileLock for the jlnNNNN.lck and jlnNNNN.lck files.
                // We do this so that the locks never get garbage-collected.
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    /* @Override */
                    public void run() {
                        // NOTE: we don't really expect that this code will ever
                        // be called. If it does, we will close the output
                        // stream, which will in turn close the channel.
                        // We will then release the lock.
                        try {
                            tmpOut.close();
                            tmpLock.release();
                            lckOut.close();
                            lckLock.release();
                        } catch (IOException ex) {
                            // Do nothing
                        }
                    }
                });

                // Set the system property...
                tmpRootPropValue = tmpRootName.substring(tmpRootName.lastIndexOf(File.separator) + 1);
                System.setProperty(tmpRootPropName, tmpRootPropValue);
                if (DEBUG) {
                    System.err.println("TempFileCache: Setting " + tmpRootPropName + "=" + tmpRootPropValue);
                }

                // Start a new Reaper thread to do stuff...
                Thread reaperThread = new Thread() {
                    /* @Override */
                    public void run() {
                        deleteOldTempDirs();
                    }
                };
                reaperThread.setName("TempFileCache-Reaper");
                reaperThread.start();
            } else {
                // Make sure that the property is not set to an illegal value
                if (tmpRootPropValue.indexOf('/') >= 0 ||
                        tmpRootPropValue.indexOf(File.separatorChar) >= 0) {
                    throw new IOException("Illegal value of: " + tmpRootPropName);
                }

                // Set tmpRootDir = ${tmpbase}/${jnlp.applet.launcher.tmproot}
                if (DEBUG) {
                    System.err.println("TempFileCache: Using existing value of: " +
                            tmpRootPropName + "=" + tmpRootPropValue);
                }
                tmpRootDir = new File(tmpBaseDir, tmpRootPropValue);
                if (DEBUG) {
                    System.err.println("TempFileCache: tmpRootDir = " + tmpRootDir.getAbsolutePath());
                }
                if (!tmpRootDir.isDirectory()) {
                    throw new IOException("Cannot access " + tmpRootDir);
                }
            }
        }
        if (DEBUG) {
            System.err.println("------------------------------------------------------------------ (static ok: "+(!staticInitError)+")");
        }
    }

    /**
     * Called by the Reaper thread to delete old temp directories
     * Only one of these threads will run per JVM invocation.
     */
    private static void deleteOldTempDirs() {
        if (DEBUG) {
            System.err.println("TempFileCache: *** Reaper: deleteOldTempDirs in " +
                    tmpBaseDir.getAbsolutePath());
        }

        // enumerate list of jnl*.lck files, ignore our own jlnNNNN file
        final String ourLockFile = tmpRootPropValue + ".lck";
        FilenameFilter lckFilter = new FilenameFilter() {
            /* @Override */
            public boolean accept(File dir, String name) {
                return name.endsWith(".lck") && !name.equals(ourLockFile);
            }
        };

        // For each file <file>.lck in the list we will first try to lock
        // <file>.tmp if that succeeds then we will try to lock <file>.lck
        // (which should always succeed unless there is a problem). If we can
        // get the lock on both files, then it must be an old installation, and
        // we will delete it.
        String[] fileNames = tmpBaseDir.list(lckFilter);
        if (fileNames != null) {
            for (int i = 0; i < fileNames.length; i++) {
                String lckFileName = fileNames[i];
                String tmpDirName = lckFileName.substring(0, lckFileName.lastIndexOf(".lck"));
                String tmpFileName = tmpDirName + ".tmp";

                File lckFile = new File(tmpBaseDir, lckFileName);
                File tmpFile = new File(tmpBaseDir, tmpFileName);
                File tmpDir = new File(tmpBaseDir, tmpDirName);

                if (lckFile.exists() && tmpFile.exists() && tmpDir.isDirectory()) {
                    FileOutputStream tmpOut = null;
                    FileChannel tmpChannel = null;
                    FileLock tmpLock = null;

                    try {
                        tmpOut = new FileOutputStream(tmpFile);
                        tmpChannel = tmpOut.getChannel();
                        tmpLock = tmpChannel.tryLock();
                    } catch (Exception ex) {
                        // Ignore exceptions
                        if (DEBUG) {
                            ex.printStackTrace();
                        }
                    }

                    if (tmpLock != null) {
                        FileOutputStream lckOut = null;
                        FileChannel lckChannel = null;
                        FileLock lckLock = null;

                        try {
                            lckOut = new FileOutputStream(lckFile);
                            lckChannel = lckOut.getChannel();
                            lckLock = lckChannel.tryLock();
                        } catch (Exception ex) {
                            if (DEBUG) {
                                ex.printStackTrace();
                            }
                        }

                        if (lckLock != null) {
                            // Recursively remove the old tmpDir and all of
                            // its contents
                            removeAll(tmpDir);

                            // Close the streams and delete the .lck and .tmp
                            // files. Note that there is a slight race condition
                            // in that another process could open a stream at
                            // the same time we are trying to delete it, which will
                            // prevent deletion, but we won't worry about it, since
                            // the worst that will happen is we might have an
                            // occasional 0-byte .lck or .tmp file left around
                            try {
                                lckOut.close();
                            } catch (IOException ex) {
                            }
                            lckFile.delete();
                            try {
                                tmpOut.close();
                            } catch (IOException ex) {
                            }
                            tmpFile.delete();
                        } else {
                            try {
                                // Close the file and channel for the *.lck file
                                if (lckOut != null) {
                                    lckOut.close();
                                }
                                // Close the file/channel and release the lock
                                // on the *.tmp file
                                tmpOut.close();
                                tmpLock.release();
                            } catch (IOException ex) {
                                if (DEBUG) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    if (DEBUG) {
                        System.err.println("TempFileCache: Skipping: " + tmpDir.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Remove the specified file or directory. If "path" is a directory, then
     * recursively remove all entries, then remove the directory itself.
     */
    private static void removeAll(File path) {
        if (DEBUG) {
            System.err.println("TempFileCache: removeAll(" + path + ")");
        }

        if (path.isDirectory()) {
            // Recursively remove all files/directories in this directory
            File[] list = path.listFiles();
            if (list != null) {
                for (int i = 0; i < list.length; i++) {
                    removeAll(list[i]);
                }
            }
        }

        path.delete();
    }

    public LauncherTempFileCache () {
        if (DEBUG) {
            System.err.println("TempFileCache: new TempFileCache() --------------------- (static ok: "+(!staticInitError)+")");
            System.err.println("TempFileCache: Thread: "+Thread.currentThread().getName()+", CL 0x"+Integer.toHexString(LauncherTempFileCache.class.getClassLoader().hashCode())+", this 0x"+Integer.toHexString(hashCode()));
        }
        if(!staticInitError) { 
            try {
                createTmpDir();
            } catch (Exception ex) {
                ex.printStackTrace();
                initError = true;
            }
        }
        if (DEBUG) {
            System.err.println("tempDir: "+individualTmpDir+" (ok: "+(!initError)+")");
            System.err.println("----------------------------------------------------------");
        }        
    }
    
    /**
     * @return true is static and object initialization was successful
     */
    public boolean isValid() { return !staticInitError && !initError; }
    
    /**
     * Base temp directory used by TempFileCache. 
     * Lifecycle: For all JVMs, ClassLoader and times.
     * 
     * This is set to:
     *
     * ${java.io.tmpdir}/<tmpDirPrefix>
     *
     * 
     * @return
     */
    public File getBaseDir() { return tmpBaseDir; }

    /**
     * Root temp directory for this JVM instance. Used to store individual
     * directories.
     *
     * Lifecycle: For all JVMs and ClassLoader
     *
     * <tmpBaseDir>/<tmpRootPropValue>
     *
     * Use Case: Per ClassLoader files, eg. native libraries. 
     *
     * Old temp directories are cleaned up the next time a JVM is launched that
     * uses TempFileCache.
     *
     * 
     * @return
     */
    public File getRootDir() { return tmpRootDir; }
    
    /**
     * Temporary directory for individual files (eg. native libraries of one ClassLoader instance).
     * The directory name is:
     *
     * Lifecycle: Within each JVM .. use case dependent, ie. per ClassLoader
     *
     * <tmpRootDir>/jlnMMMMM
     *
     * where jlnMMMMM is the unique filename created by File.createTempFile()
     * without the ".tmp" extension.
     *
     * 
     * @return
     */
    public File getTempDir() { return individualTmpDir; }
    
    
    /**
     * Create the temp directory in tmpRootDir. To do this, we create a temp
     * file with a ".tmp" extension, and then create a directory of the
     * same name but without the ".tmp". The temp file, directory, and all
     * files in the directory will be reaped the next time this is started.
     * We avoid deleteOnExit, because it doesn't work reliably.
     */
    private void createTmpDir() throws IOException {
        File tmpFile = File.createTempFile("jln", ".tmp", tmpRootDir);
        String tmpFileName = tmpFile.getAbsolutePath();
        String tmpDirName = tmpFileName.substring(0, tmpFileName.lastIndexOf(".tmp"));
        individualTmpDir = new File(tmpDirName);
        if (!individualTmpDir.mkdir()) {
            throw new IOException("Cannot create " + individualTmpDir);
        }
    }
}
