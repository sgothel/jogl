/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
 
package com.jogamp.test.junit.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

public class SingletonInstance {

    static final boolean DEBUG = true;
    static final String temp_file_path;

    static {
        String s = null;
        try {
            File tmpFile = File.createTempFile("TEST", "tst");
            String absTmpFile = tmpFile.getCanonicalPath();
            tmpFile.delete();
            s = absTmpFile.substring(0, absTmpFile.lastIndexOf(File.separator));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        temp_file_path = s;
    }

    public static String getCanonicalTempPath() {
        return temp_file_path;
    }

    public static String getCanonicalTempLockFilePath(String basename) {
        return getCanonicalTempPath() + File.separator + basename;
    }

    public SingletonInstance(String lockFileBasename) {
        this.file = new File ( getCanonicalTempLockFilePath ( lockFileBasename ) );
    }

    public SingletonInstance(File lockFile) {
        this.file = lockFile ;
    }

    public synchronized void lock(long timeout_ms, long poll_ms) {
        long i=0;
        try {
            do {
                if(tryLock()) {
                    return;
                }
                if(DEBUG && 0==i) {
                    System.err.println("Wait for lock " + file);
                }
                i++;
                Thread.sleep(poll_ms);
            } while ( i < timeout_ms / poll_ms ) ;
        } catch ( InterruptedException ie ) {
            throw new  RuntimeException(ie);
        }
        throw new RuntimeException("SingletonInstance couldn't get lock within "+timeout_ms+"ms");
    }

    public synchronized boolean tryLock() {
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileLock = randomAccessFile.getChannel().tryLock();

            if (fileLock != null) {
                //final File f_file = file;
                //final RandomAccessFile f_randomAccessFile = randomAccessFile;
                //final FileLock f_fileLock = fileLock;
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        unlock();
                    }
                });
                locked = true;
                if(DEBUG) {
                    System.err.println("Locked " + file);
                }
                return true;
            }
        } catch (Exception e) {
            System.err.println("Unable to create and/or lock file: " + file);
            e.printStackTrace();
        }
        return false;
    }

    public synchronized boolean unlock() {
        if(locked) {
            try {
                fileLock.release();
                randomAccessFile.close();
                file.delete();
                return true;
            } catch (Exception e) {
                System.err.println("Unable to remove lock file: " + file);
                e.printStackTrace();
            } finally {
                fileLock = null;
                randomAccessFile = null;
                locked = false;
            }
        }
        return false;
    }

    public synchronized boolean isLocked() {
        return locked;
    }

    File file = null;
    RandomAccessFile randomAccessFile = null;
    FileLock fileLock = null;
    boolean locked = false;
}
