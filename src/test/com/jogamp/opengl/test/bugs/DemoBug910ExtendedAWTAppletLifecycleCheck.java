/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.bugs;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("serial")
public class DemoBug910ExtendedAWTAppletLifecycleCheck extends Applet {

    private static String currentThreadName() { return "["+Thread.currentThread().getName()+", isAWT-EDT "+EventQueue.isDispatchThread()+"]"; }

    private static void invoke(final boolean wait, final Runnable r) {
        if(EventQueue.isDispatchThread()) {
            r.run();
        } else {
          try {
            if(wait) {
                EventQueue.invokeAndWait(r);
            } else {
                EventQueue.invokeLater(r);
            }
          } catch (final InvocationTargetException e) {
            throw new RuntimeException(e.getTargetException());
          } catch (final InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
    }

    private static final String comp2Str(final Component c) {
        return c.getClass().getSimpleName()+"[visible "+c.isVisible()+", showing "+c.isShowing()+", valid "+c.isValid()+
                ", displayable "+c.isDisplayable()+", "+c.getX()+"/"+c.getY()+" "+c.getWidth()+"x"+c.getHeight()+"]";
    }

    private void println(final String msg) {
        System.err.println(msg);
    }

    private final void checkComponentState(final String msg, final boolean expIsContained, final int expAddNotifyCount, final int expRemoveNotifyCount) {
        final int compCount = getComponentCount();
        final Component c = 1 <= compCount ? getComponent(0) : null;
        final String clazzName = null != c ? c.getName() : "n/a";
        final boolean isContained = c == myCanvas;
        final String okS = ( expIsContained == isContained &&
                             expAddNotifyCount == myCanvas.addNotifyCount &&
                             expRemoveNotifyCount == myCanvas.removeNotifyCount ) ? "OK" : "ERROR";
        println("Component-State @ "+msg+": "+okS+
                ", contained[exp "+expIsContained+", has "+isContained+"]"+(expIsContained!=isContained?"*":"")+
                ", addNotify[exp "+expAddNotifyCount+", has "+myCanvas.addNotifyCount+"]"+(expAddNotifyCount!=myCanvas.addNotifyCount?"*":"")+
                ", removeNotify[exp "+expRemoveNotifyCount+", has "+myCanvas.removeNotifyCount+"]"+(expRemoveNotifyCount!=myCanvas.removeNotifyCount?"*":"")+
                ", compCount "+compCount+", compClazz "+clazzName);
    }

    AtomicInteger initCount = new AtomicInteger(0);
    AtomicInteger startCount = new AtomicInteger(0);
    AtomicInteger stopCount = new AtomicInteger(0);
    AtomicInteger destroyCount = new AtomicInteger(0);

    private final void checkAppletState(final String msg, final boolean expIsActive,
                                        final int expInitCount, final int expStartCount, final int expStopCount, final boolean startStopCountEquals, final int expDestroyCount) {
        final boolean isActive = this.isActive();
        final String okS = ( expInitCount == initCount.get() &&
                             expIsActive == isActive &&
                             expStartCount == startCount.get() &&
                             expStopCount == stopCount.get() &&
                             expDestroyCount == destroyCount.get() &&
                             ( !startStopCountEquals || startCount == stopCount ) ) ? "OK" : "ERROR";
        println("Applet-State @ "+msg+": "+okS+
                ", active[exp "+expIsActive+", has "+isActive+"]"+(expIsActive!=isActive?"*":"")+
                ", init[exp "+expInitCount+", has "+initCount+"]"+(expInitCount!=initCount.get()?"*":"")+
                ", start[exp "+expStartCount+", has "+startCount+"]"+(expStartCount!=startCount.get()?"*":"")+
                ", stop[exp "+expStopCount+", has "+stopCount+"]"+(expStopCount!=stopCount.get()?"*":"")+
                ", start==stop[exp "+startStopCountEquals+", start "+startCount+", stop "+stopCount+"]"+(( startStopCountEquals && startCount != stopCount )?"*":"")+
                ", destroy[exp "+expDestroyCount+", has "+destroyCount+"]"+(expDestroyCount!=destroyCount.get()?"*":""));
    }

    private class MyCanvas extends Canvas {
        int addNotifyCount = 0;
        int removeNotifyCount = 0;
        int paintCount = 0;

        MyCanvas() {
            setBackground( new Color( 200, 200, 255 ) );
        }

        public String toString() {
            return comp2Str(this)+", add/remove[addNotify "+addNotifyCount+", removeCount "+removeNotifyCount+"]";
        }

        @Override
        public void addNotify() {
            addNotifyCount++;
            println("Applet.Canvas.addNotify() - "+currentThreadName());
            if( !EventQueue.isDispatchThread() ) {
                println("Applet.Canvas.addNotify() ERROR: Not on AWT-EDT");
            }
            // Thread.dumpStack();
            super.addNotify();
            println("Applet.Canvas.addNotify(): "+this);
        }

        @Override
        public void removeNotify() {
            removeNotifyCount++;
            println("Applet.Canvas.removeNotify() - "+currentThreadName());
            println("Applet.Canvas.removeNotify(): "+this);
            if( !EventQueue.isDispatchThread() ) {
                println("Applet.Canvas.removeNotify() ERROR: Not on AWT-EDT");
            }
            // Thread.dumpStack();
            super.removeNotify();
        }

        @Override
        public void paint(final Graphics g) {
            super.paint(g);
            paintCount++;
            final int width = getWidth();
            final int height = getHeight();
            final String msg = "The payload Canvas. Paint "+width+"x"+height+" #"+paintCount;
            g.setColor(Color.black);
            g.drawString(msg, 64, 64);
        }
    }
    MyCanvas myCanvas = null;

    @Override
    public void init() {
        final java.awt.Dimension aSize = getSize();
        println("Applet.init() START - applet.size "+aSize+" - "+currentThreadName());
        initCount.incrementAndGet();
        checkAppletState("init", false /* expIsActive */, 1 /* expInitCount */,
                         0 /* expStartCount */, 0 /* expStopCount */, true /* startStopCountEquals */,
                         0 /* expDestroyCount */);
        invoke(true, new Runnable() {
            public void run() {
                setLayout(new BorderLayout());
                myCanvas = new MyCanvas();
                println("Applet.init(): self   "+comp2Str(DemoBug910ExtendedAWTAppletLifecycleCheck.this));
                println("Applet.init(): canvas "+comp2Str(myCanvas));
                checkComponentState("init-add.pre", false, 0, 0);
                add(myCanvas, BorderLayout.CENTER);
                validate();
                checkComponentState("init-add.post", true, 1, 0);
                println("Applet.init(): canvas "+comp2Str(myCanvas));
            } } );
        println("Applet.init() END - "+currentThreadName());
    }

    @Override
    public void start() {
        println("Applet.start() START (isVisible "+isVisible()+", isDisplayable "+isDisplayable()+") - "+currentThreadName());
        startCount.incrementAndGet();
        checkAppletState("start", true /* expIsActive */, 1 /* expInitCount */,
                         startCount.get() /* expStartCount */, startCount.get()-1 /* expStopCount */, false /* startStopCountEquals */,
                         0 /* expDestroyCount */);
        invoke(true, new Runnable() {
            public void run() {
                checkComponentState("start-visible.pre", true, 1, 0);
                if( null != myCanvas ) {
                    myCanvas.setFocusable(true);
                    myCanvas.requestFocus();
                }
                checkComponentState("start-visible.post", true, 1, 0);
                println("Applet.start(): self   "+comp2Str(DemoBug910ExtendedAWTAppletLifecycleCheck.this));
                println("Applet.start(): canvas "+comp2Str(myCanvas));
            }
        });
        println("Applet.start() END - "+currentThreadName());
    }

    @Override
    public void stop() {
        println("Applet.stop() START - "+currentThreadName());
        stopCount.incrementAndGet();
        checkAppletState("stop", false /* expIsActive */, 1 /* expInitCount */,
                         stopCount.get() /* expStartCount */, stopCount.get() /* expStopCount */, true /* startStopCountEquals */,
                         0 /* expDestroyCount */);
        invoke(true, new Runnable() {
            public void run() {
                checkComponentState("stop", true, 1, 0);
            } } );
        println("Applet.stop() END - "+currentThreadName());
    }

    @Override
    public void destroy() {
        println("Applet.destroy() START - "+currentThreadName());
        destroyCount.incrementAndGet();
        checkAppletState("destroy", false /* expIsActive */, 1 /* expInitCount */,
                         startCount.get() /* expStartCount */, stopCount.get() /* expStopCount */, true /* startStopCountEquals */,
                         1 /* expDestroyCount */);
        invoke(true, new Runnable() {
            public void run() {
                checkComponentState("destroy-remove.pre", true, 1, 0);
                remove(myCanvas);
                checkComponentState("destroy-remove.post", false, 1, 1);
            } } );
        println("Applet.destroy() END - "+currentThreadName());
    }
}

