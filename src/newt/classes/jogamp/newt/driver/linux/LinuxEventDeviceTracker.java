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

package jogamp.newt.driver.linux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Integer;
import java.lang.Runnable;
import java.lang.String;
import java.lang.Thread;
import java.nio.ByteBuffer;

import jogamp.newt.WindowImpl;
import jogamp.newt.driver.KeyTracker;

import com.jogamp.common.nio.StructAccessor;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.InputEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.event.KeyEvent;

/**
 * Experimental native event device tracker thread for GNU/Linux
 * just reading <code>/dev/input/event*</code>
 * within it's own polling thread.
 */

public class LinuxEventDeviceTracker implements WindowListener, KeyTracker {

    private static final LinuxEventDeviceTracker ledt;


    static {
        ledt = new LinuxEventDeviceTracker();
        final Thread t = new InterruptSource.Thread(null, ledt.eventDeviceManager, "NEWT-LinuxEventDeviceManager");
        t.setDaemon(true);
        t.start();
    }

    public static LinuxEventDeviceTracker getSingleton() {
        return ledt;
    }

    private WindowImpl focusedWindow = null;
    private final EventDeviceManager eventDeviceManager = new EventDeviceManager();

    /*
      The devices are in /dev/input:

	crw-r--r--   1 root     root      13,  64 Apr  1 10:49 event0
	crw-r--r--   1 root     root      13,  65 Apr  1 10:50 event1
	crw-r--r--   1 root     root      13,  66 Apr  1 10:50 event2
	crw-r--r--   1 root     root      13,  67 Apr  1 10:50 event3
	...

      And so on up to event31.
     */
    private final EventDevicePoller[] eventDevicePollers = new EventDevicePoller[32];

    @Override
    public void windowResized(final WindowEvent e) { }

    @Override
    public void windowMoved(final WindowEvent e) { }

    @Override
    public void windowDestroyNotify(final WindowEvent e) {
        final Object s = e.getSource();
        if(focusedWindow == s) {
            focusedWindow = null;
        }
    }

    @Override
    public void windowDestroyed(final WindowEvent e) { }

    @Override
    public void windowGainedFocus(final WindowEvent e) {
        final Object s = e.getSource();
        if(s instanceof WindowImpl) {
            focusedWindow = (WindowImpl) s;
        }
    }

    @Override
    public void windowLostFocus(final WindowEvent e) {
        final Object s = e.getSource();
        if(focusedWindow == s) {
            focusedWindow = null;
        }
    }

    public static void main(final String[] args ){
        System.setProperty("newt.debug.Window.KeyEvent", "true");
        LinuxEventDeviceTracker.getSingleton();
        try {
            while(true) {
                Thread.sleep(1000);
            }
        } catch (final InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    public void windowRepaint(final WindowUpdateEvent e) { }

    class EventDeviceManager implements Runnable {

        private volatile boolean stop = false;

        @Override
        public void run() {
            final File f = new File("/dev/input/");
            int number;
            while(!stop){
                for(final String path:f.list()){
                    if(path.startsWith("event")) {
                        final String stringNumber = path.substring(5);
                        number = Integer.parseInt(stringNumber);
                        if(number<32&&number>=0) {
                            if(eventDevicePollers[number]==null){
                                eventDevicePollers[number] = new EventDevicePoller(number);
                                final Thread t = new InterruptSource.Thread(null, eventDevicePollers[number], "NEWT-LinuxEventDeviceTracker-event"+number);
                                t.setDaemon(true);
                                t.start();
                            } else if(eventDevicePollers[number].stop) {
                                eventDevicePollers[number]=null;
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (final InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

    class EventDevicePoller implements Runnable {

        private volatile boolean stop = false;
        private final String eventDeviceName;

        public EventDevicePoller(final int eventDeviceNumber){
            this.eventDeviceName="/dev/input/event"+eventDeviceNumber;
        }

        @Override
        public void run() {
            final byte[] b = new byte[16];
            /**
             * The Linux input event interface.
             * http://www.kernel.org/doc/Documentation/input/input.txt
             *
             * struct input_event {
             *  struct timeval time;
             *	unsigned short type;
             *	unsigned short code;
             *	unsigned int value;
             * };
             */
            final ByteBuffer bb = ByteBuffer.wrap(b);
            final StructAccessor s = new StructAccessor(bb);
            final File f = new File(eventDeviceName);
            f.setReadOnly();
            InputStream fis;
            try {
                fis = new FileInputStream(f);
            } catch (final FileNotFoundException e) {
                stop=true;
                return;
            }

            int timeSeconds;
            int timeSecondFraction;
            short type;
            short code;
            int value;

            short keyCode=KeyEvent.VK_UNDEFINED;
            char keyChar=' ';
            short eventType=0;
            int modifiers=0;

            loop:
                while(!stop) {
                    int remaining=16;
                    while(remaining>0) {
                        int read = 0;
                        try {
                            read = fis.read(b, 0, remaining);
                        } catch (final IOException e) {
                            stop = true;
                            break loop;
                        }
                        if(read<0) {
                            stop = true; // EOF of event device file !?
                            break loop;
                        } else {
                            remaining -= read;
                        }
                    }

                    timeSeconds = s.getIntAt(0);
                    timeSecondFraction = s.getShortAt(4);
                    type = s.getShortAt(8);
                    code = s.getShortAt(10);
                    value = s.getIntAt(12);


                    /*
                     * Linux sends Keyboard events in the following order:
                     * EV_MSC (optional, contains scancode)
                     * EV_KEY
                     * SYN_REPORT (sent before next key)
                     */

                    switch(type) {
                    case 0: // SYN_REPORT
                        // Clear
                        eventType = 0;
                        keyCode = KeyEvent.VK_UNDEFINED;
                        keyChar = 0; // Print null for unprintable char.
                        if(Window.DEBUG_KEY_EVENT) {
                            System.out.println("[SYN_REPORT----]");
                        }
                        break;
                    case 1: // EV_KEY
                        keyCode = LinuxEVKey2NewtVKey(code); // The device independent code.
                        keyChar = NewtVKey2Unicode(keyCode, modifiers); // The printable character w/ key modifiers.
                        if(Window.DEBUG_KEY_EVENT) {
                            System.out.println("[EV_KEY: [time "+timeSeconds+":"+timeSecondFraction+"] type "+type+" / code "+code+" = value "+value);
                        }

                        switch(value) {
                        case 0:
                            eventType=KeyEvent.EVENT_KEY_RELEASED;

                            switch(keyCode) {
                            case KeyEvent.VK_SHIFT:
                                modifiers &= ~InputEvent.SHIFT_MASK;
                                break;
                            case KeyEvent.VK_ALT:
                                modifiers &= ~InputEvent.ALT_MASK;
                                break;
                            case KeyEvent.VK_ALT_GRAPH:
                                modifiers &= ~InputEvent.ALT_GRAPH_MASK;
                                break;
                            case KeyEvent.VK_CONTROL:
                                modifiers &= ~InputEvent.CTRL_MASK;
                                break;
                            }

                            if(null != focusedWindow) {
                                focusedWindow.sendKeyEvent(eventType, modifiers, keyCode, keyCode, keyChar);
                            }
                            if(Window.DEBUG_KEY_EVENT) {
                                System.out.println("[event released] keyCode: "+keyCode+" keyChar: "+keyChar+ " modifiers: "+modifiers);
                            }
                            break;
                        case 1:
                            eventType=KeyEvent.EVENT_KEY_PRESSED;

                            switch(keyCode) {
                            case KeyEvent.VK_SHIFT:
                                modifiers |= InputEvent.SHIFT_MASK;
                                break;
                            case KeyEvent.VK_ALT:
                                modifiers |= InputEvent.ALT_MASK;
                                break;
                            case KeyEvent.VK_ALT_GRAPH:
                                modifiers |= InputEvent.ALT_GRAPH_MASK;
                                break;
                            case KeyEvent.VK_CONTROL:
                                modifiers |= InputEvent.CTRL_MASK;
                                break;
                            }

                            if(null != focusedWindow) {
                                focusedWindow.sendKeyEvent(eventType, modifiers, keyCode, keyCode, keyChar);
                            }
                            if(Window.DEBUG_KEY_EVENT) {
                                System.out.println("[event pressed] keyCode: "+keyCode+" keyChar: "+keyChar+ " modifiers: "+modifiers);
                            }
                            break;
                        case 2:
                            eventType=KeyEvent.EVENT_KEY_PRESSED;
                            modifiers |= InputEvent.AUTOREPEAT_MASK;

                            switch(keyCode) {
                            case KeyEvent.VK_SHIFT:
                                modifiers |= InputEvent.SHIFT_MASK;
                                break;
                            case KeyEvent.VK_ALT:
                                modifiers |= InputEvent.ALT_MASK;
                                break;
                            case KeyEvent.VK_ALT_GRAPH:
                                modifiers |= InputEvent.ALT_GRAPH_MASK;
                                break;
                            case KeyEvent.VK_CONTROL:
                                modifiers |= InputEvent.CTRL_MASK;
                                break;
                            }

                            if(null != focusedWindow) {
                                //Send syntetic autorepeat release
                                focusedWindow.sendKeyEvent(KeyEvent.EVENT_KEY_RELEASED, modifiers, keyCode, keyCode, keyChar);

                                focusedWindow.sendKeyEvent(eventType, modifiers, keyCode, keyCode, keyChar);
                            }
                            if(Window.DEBUG_KEY_EVENT) {
                                System.out.println("[event released auto] keyCode: "+keyCode+" keyChar: "+keyChar+ " modifiers: "+modifiers);
                                System.out.println("[event pressed auto] keyCode: "+keyCode+" keyChar: "+keyChar+ " modifiers: "+modifiers);
                            }
                            modifiers &= ~InputEvent.AUTOREPEAT_MASK;
                            break;
                        }
                        break;
                    case 4: // EV_MSC
                        if(code==4) { // MSC_SCAN
                            // scancode ignore, linux kernel specific
                        }
                        break;
                        // TODO: handle joystick events
                        // TODO: handle mouse events
                        // TODO: handle headphone/hdmi connector events
                    default: // Print number.
                        if(Window.DEBUG_KEY_EVENT) {
                            System.out.println("TODO EventDevicePoller: [time "+timeSeconds+":"+timeSecondFraction+"] type "+type+" / code "+code+" = value "+value);
                        }
                    }
                }

            if(null != fis) {
                try {
                    fis.close();
                } catch (final IOException e) {
                }
            }
            stop=true;
        }

        private char NewtVKey2Unicode(final short VK, final int modifiers) {
            if( KeyEvent.isPrintableKey(VK, true) ) {
                if((modifiers & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK) {
                    return (char)VK;
                } else {
                    return String.valueOf((char)VK).toLowerCase().charAt(0);
                }
            }
            return 0;
        }

        @SuppressWarnings("unused")
        private char LinuxEVKey2Unicode(final short EVKey) {
            // This is the stuff normally mapped by a system keymap

            switch(EVKey) {
            case 17: // w
                return 'w';
            case 31: // s
                return 's';
            case 30: // a
                return 'a';
            case 32: // d
                return 'd';
            case 1: // ESC
                return 27;
            case 28: // Enter
            case 96: // Keypad Enter
                return '\n';
            case 57: // Space
                return ' ';
            case 11: // 0
            case 82: // Numpad 0
                return '0';
            case 2: // 1
            case 79: // Numpad 1
                return '1';
            case 3: // 2
            case 80: // Numpad 1
                return '2';
            case 4: // 3
            case 81: // Numpad 3
                return '3';
            case 5: // 4
            case 75: // Numpad 4
                return '4';
            case 6: // 5
            case 76: // Numpad 5
                return '5';
            case 7: // 6
            case 77: // Numpad 6
                return '6';
            case 8: // 7
            case 71: // Numpad 7
                return '7';
            case 9: // 8
            case 72: // Numpad 8
                return '8';
            case 10: // 9
            case 73: // Numpad 9
                return '9';

            default:
            }

            return 0;
        }

        private short LinuxEVKey2NewtVKey(final short EVKey) {

            switch(EVKey) {
            case 1: // ESC
                return KeyEvent.VK_ESCAPE;
            case 2: // 1
                return KeyEvent.VK_1;
            case 79: // Numpad 1
                return KeyEvent.VK_NUMPAD1;
            case 3: // 2
                return KeyEvent.VK_2;
            case 80: // Numpad 2
                return KeyEvent.VK_NUMPAD2;
            case 4: // 3
                return KeyEvent.VK_3;
            case 81: // Numpad 3
                return KeyEvent.VK_NUMPAD3;
            case 5: // 4
                return KeyEvent.VK_4;
            case 75: // Numpad 4
                return KeyEvent.VK_NUMPAD4;
            case 6: // 5
                return KeyEvent.VK_5;
            case 76: // Numpad 5
                return KeyEvent.VK_NUMPAD5;
            case 7: // 6
                return KeyEvent.VK_6;
            case 77: // Numpad 6
                return KeyEvent.VK_NUMPAD6;
            case 8: // 7
                return KeyEvent.VK_7;
            case 71: // Numpad 7
                return KeyEvent.VK_NUMPAD7;
            case 9: // 8
                return KeyEvent.VK_8;
            case 72: // Numpad 8
                return KeyEvent.VK_NUMPAD8;
            case 10: // 9
                return KeyEvent.VK_9;
            case 73: // Numpad 9
                return KeyEvent.VK_NUMPAD9;
            case 11: // 0
                return KeyEvent.VK_0;
            case 82: // Numpad 0
                return KeyEvent.VK_NUMPAD0;
            case 12:
                return KeyEvent.VK_MINUS;
            case 13:
                return KeyEvent.VK_EQUALS;
            case 14: // Backspace
                return KeyEvent.VK_BACK_SPACE;

            case 15:
                return KeyEvent.VK_TAB;
            case 16:
                return KeyEvent.VK_Q;
            case 17: // w
                return KeyEvent.VK_W;
            case 18:
                return KeyEvent.VK_E;
            case 19:
                return KeyEvent.VK_R;
            case 20:
                return KeyEvent.VK_T;
            case 21:
                return KeyEvent.VK_Y;
            case 22:
                return KeyEvent.VK_U;
            case 23:
                return KeyEvent.VK_I;
            case 24:
                return KeyEvent.VK_O;
            case 25:
                return KeyEvent.VK_P;
            case 26: // left brace
                return KeyEvent.VK_LEFT_PARENTHESIS;
            case 27: // right brace
                return KeyEvent.VK_RIGHT_PARENTHESIS;
            case 28: // Enter
            case 96: // Keypad Enter
                return KeyEvent.VK_ENTER;

            case 29: // left ctrl
                return KeyEvent.VK_CONTROL;
            case 30: // a
                return KeyEvent.VK_A;
            case 31: // s
                return KeyEvent.VK_S;
            case 32: // d
                return KeyEvent.VK_D;
            case 33:
                return KeyEvent.VK_F;
            case 34:
                return KeyEvent.VK_G;
            case 35:
                return KeyEvent.VK_H;
            case 36:
                return KeyEvent.VK_J;
            case 37:
                return KeyEvent.VK_K;
            case 38:
                return KeyEvent.VK_L;
            case 39:
                return KeyEvent.VK_SEMICOLON;
            case 40: // apostrophe
                return KeyEvent.VK_QUOTE;
            case 41: // grave
                return KeyEvent.VK_BACK_QUOTE;

            case 42: // left shift
                return KeyEvent.VK_SHIFT;
            case 43:
                return KeyEvent.VK_BACK_SLASH;
            case 44:
                return KeyEvent.VK_Z;
            case 45:
                return KeyEvent.VK_X;
            case 46:
                return KeyEvent.VK_C;
            case 47:
                return KeyEvent.VK_V;
            case 48:
                return KeyEvent.VK_B;
            case 49:
                return KeyEvent.VK_N;
            case 50:
                return KeyEvent.VK_M;
            case 51:
                return KeyEvent.VK_COMMA;
            case 52: // dot
                return KeyEvent.VK_PERIOD;
            case 53:
                return KeyEvent.VK_SLASH;
            case 54:
                return KeyEvent.VK_SHIFT;
            case 55: // kp asterisk
                return KeyEvent.VK_ASTERISK;
            case 56: // left alt
                return KeyEvent.VK_ALT;
            case 57: // Space
                return KeyEvent.VK_SPACE;
            case 58:
                return KeyEvent.VK_CAPS_LOCK;

            case 59:
                return KeyEvent.VK_F1;
            case 60:
                return KeyEvent.VK_F2;
            case 61:
                return KeyEvent.VK_F3;
            case 62:
                return KeyEvent.VK_F4;
            case 63:
                return KeyEvent.VK_F5;
            case 64:
                return KeyEvent.VK_F6;
            case 65:
                return KeyEvent.VK_F7;
            case 66:
                return KeyEvent.VK_F8;
            case 67:
                return KeyEvent.VK_F9;
            case 68:
                return KeyEvent.VK_F10;

            case 69:
                return KeyEvent.VK_NUM_LOCK;
            case 70:
                return KeyEvent.VK_SCROLL_LOCK;

            case 74: // kp minus
                return KeyEvent.VK_MINUS;
            case 78: // kp plus
                return KeyEvent.VK_PLUS;
            case 83: // kp dot
                return KeyEvent.VK_PERIOD;

            // TODO: add mappings for japanese special buttons
            case 85: // zenkakuhankaku
            case 86: // 102nd
                break; // FIXME

            case 87:
                return KeyEvent.VK_F11;
            case 88:
                return KeyEvent.VK_F12;

            case 89: // ro
                return KeyEvent.VK_ROMAN_CHARACTERS;
            case 90: // Katakana
                return KeyEvent.VK_KATAKANA;
            case 91:
                return KeyEvent.VK_HIRAGANA;

            case 92: // kenkan
                break; // FIXME
            case 93: // katakana hiragana
                break; // FIXME
            case 94: // mu henkan
                break; // FIXME
            case 95: // kp jp comma
                break; // FIXME

            case 97: // right ctrl
                return KeyEvent.VK_CONTROL;
            case 98: // kp slash
                return KeyEvent.VK_SLASH;

            case 99: // sysrq
                break; // FIXME

            case 100: // right alt
                return KeyEvent.VK_ALT;
            case 101: // linefeed
                break; // FIXME
            case 102: // home
                return KeyEvent.VK_HOME;
            case 103: // KEY_UP
                return KeyEvent.VK_UP;
            case 104:
                return KeyEvent.VK_PAGE_UP;
            case 105: // KEY_LEFT
                return KeyEvent.VK_LEFT;
            case 106: // KEY_RIGHT
                return KeyEvent.VK_RIGHT;
            case 107:
                return KeyEvent.VK_END;
            case 108: // KEY_DOWN
                return KeyEvent.VK_DOWN;
            case 109:
                return KeyEvent.VK_PAGE_DOWN;
            case 110:
                return KeyEvent.VK_INSERT;
            case 111: // del
                return KeyEvent.VK_DELETE;

            case 112: // macro
                break; // FIXME DEAD_MACRON?
            case 113: // mute
                break; // FIXME
            case 114: // vol up
                break; // FIXME
            case 115: // vol down
                break; // FIXME
            case 116: // power
                break; // FIXME

            case 117: // kp equals
                return KeyEvent.VK_EQUALS;
            case 118: // kp plus minux
                break; // FIXME
            case 119: // pause
                return KeyEvent.VK_PAUSE;
            case 120: // scale AL compiz scale expose
                break; // FIXME
            case 121: // kp comma
                return KeyEvent.VK_COMMA;
            case 122: // hangeul
                break; // FIXME
            case 123: // hanja
                break; // FIXME
            case 124: // yen
                break; // FIXME

            case 125: // left meta
            case 126: // right meta
                return KeyEvent.VK_META;
            case 127: // compose
                return KeyEvent.VK_COMPOSE;

            case 128: // stop
                return KeyEvent.VK_STOP;
            case 129: // again
                return KeyEvent.VK_AGAIN;
            case 130: // properties
                return KeyEvent.VK_PROPS;
            case 131: // undo
                return KeyEvent.VK_UNDO;
            case 132: // front
                break; // FIXME
            case 133: // copy
                return KeyEvent.VK_COPY;
            case 134: // open
                break; // FIXME
            case 135: // paste
                return KeyEvent.VK_PASTE;
            case 136: // find
                return KeyEvent.VK_FIND;
            case 137: // cut
                return KeyEvent.VK_CUT;
            case 138: // help
                return KeyEvent.VK_HELP;
            case 139: // menu
                break; // FIXME
            case 140: // calc
                break; // FIXME
            case 141: // setup
                break; // FIXME
            case 142: // sleep
                break; // FIXME
            case 143: // wakeup
                break; // FIXME
            case 144: // file
                break; // FIXME
            case 145: // send file
                break; // FIXME
            case 146: // delete file
                break; // FIXME
            case 147: // xfer
                break; // FIXME
            case 148: // prog1
                break; // FIXME
            case 149: // prog2
                break; // FIXME
            case 150: // www
                break; // FIXME
            case 151: // msdos
                break; // FIXME
            case 152: // coffee
                break; // FIXME
            case 153: // direction
                break; // FIXME
            case 154: // cycle windows
                break; // FIXME
            case 155: // mail
                break; // FIXME
            case 156: // bookmarks
                break; // FIXME
            case 157: // computer
                break; // FIXME
            case 158: // back
                break; // FIXME
            case 159: // forward
                break; // FIXME
            case 160: // close cd
                break; // FIXME
            case 161: // eject cd
                break; // FIXME
            case 162: // eject close cd
                break; // FIXME
            case 163: // next song
                break; // FIXME
            case 164: // play pause
                break; // FIXME
            case 165: // previous song
                break; // FIXME
            case 166: // stop cd
                break; // FIXME
            case 167: // record
                break; // FIXME
            case 168: // rewind
                break; // FIXME
            case 169: // phone
                break; // FIXME
            case 170: // ISO
                break; // FIXME
            case 171: // config
                break; // FIXME
            case 172: // home page
                break; // FIXME
            case 173: // refresh
                break; // FIXME
            case 174: // exit
                break; // FIXME
            case 175: // move
                break; // FIXME
            case 176: // edit
                break; // FIXME
            case 177: // scroll up
                break; // FIXME PAGE_UP?
            case 178: // scroll down
                break; // FIXME PAGE_DOWN?
            case 179: // kp left paren
                return KeyEvent.VK_LEFT_PARENTHESIS;
            case 180: // kp right paren
                return KeyEvent.VK_RIGHT_PARENTHESIS;
            case 181: // new
                break; // FIXME
            case 182: // redo
                break; // FIXME

            case 183: // F13
                return KeyEvent.VK_F13;
            case 184: // F14
                return KeyEvent.VK_F14;
            case 185: // F15
                return KeyEvent.VK_F15;
            case 186: // F16
                return KeyEvent.VK_F16;
            case 187: // F17
                return KeyEvent.VK_F17;
            case 188: // F18
                return KeyEvent.VK_F18;
            case 189: // F19
                return KeyEvent.VK_F19;
            case 190: // F20
                return KeyEvent.VK_F20;
            case 191: // F21
                return KeyEvent.VK_F21;
            case 192: // F22
                return KeyEvent.VK_F22;
            case 193: // F23
                return KeyEvent.VK_F23;
            case 194: // F24
                return KeyEvent.VK_F24;

            case 200: // play cd
                break; // FIXME
            case 201: // pause cd
                break; // FIXME
            case 202: // prog 3
                break; // FIXME
            case 203: // prog 4
                break; // FIXME
            case 204: // dashboard
                break; // FIXME
            case 205: // suspend
                break; // FIXME
            case 206: // close
                break; // FIXME
            case 207: // play
                break; // FIXME
            case 208: // fast forward
                break; // FIXME
            case 210: // print
                return KeyEvent.VK_PRINTSCREEN; // FIXME ?
            case 211: // HP
                break; // FIXME
            case 212: // camera
                break; // FIXME
            case 213: // sound
                break; // FIXME
            case 214: // question
                break; // FIXME
            case 215: // email
                break; // FIXME
            case 216: // chat
                break; // FIXME
            case 217: // search
                break; // FIXME
            case 218: // connect
                break; // FIXME
            case 219: // finance
                break; // FIXME
            case 220: // sport
                break; // FIXME
            case 221: // shop
                break; // FIXME
            case 222: // alt erase
                break; // FIXME
            case 223: // cancel
                break; // FIXME
            case 224: // brightness down
                break; // FIXME
            case 225: // brightness up
                break; // FIXME
            case 226: // media
                break; // FIXME
            case 227: // switch video mode
                break; // FIXME
            case 228: // kb dillum toggle
                break; // FIXME
            case 229: // kb dillum down
                break; // FIXME
            case 230: // kb dillum up
                break; // FIXME
            case 231: // send
                break; // FIXME
            case 232: // reply
                break; // FIXME
            case 233: // forward mail
                break; // FIXME
            case 234: // save
                break; // FIXME
            case 235: // documents
                break; // FIXME
            case 236: // battery
                break; // FIXME
            case 237: // bluetooth
                break; // FIXME
            case 238: // wlan
                break; // FIXME
            case 239: // UWB
                break; // FIXME
            case 240: // unknown
                return KeyEvent.VK_UNDEFINED;
            case 241: // video next
                break; // FIXME
            case 242: // video prev
                break; // FIXME
            case 243: // brightness cycle
                break; // FIXME
            case 244: // brightness zero
                break; // FIXME
            case 245: // display off
                break; // FIXME
            case 246: // wimax
                break; // FIXME
            case 247: // rf kill radio off
                break; // FIXME
            case 248: // mic mute
                break; // FIXME

            default:
            }

            if(Window.DEBUG_KEY_EVENT) {
                System.out.println("TODO LinuxEVKey2NewtVKey: Unmapped EVKey "+EVKey);
            }

            return KeyEvent.VK_UNDEFINED;
        }
    }
}
