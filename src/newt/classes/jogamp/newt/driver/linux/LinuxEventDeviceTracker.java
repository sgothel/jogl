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

import com.jogamp.common.nio.StructAccessor;
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

public class LinuxEventDeviceTracker implements WindowListener {

	private static final LinuxEventDeviceTracker ledt;


	static {
		ledt = new LinuxEventDeviceTracker();
		final Thread t = new Thread(ledt.eventDeviceManager, "NEWT-LinuxEventDeviceManager");
		t.setDaemon(true);
		t.start();
	}

	public static LinuxEventDeviceTracker getSingleton() {
		return ledt;
	}

	private WindowImpl focusedWindow = null;
    private EventDeviceManager eventDeviceManager = new EventDeviceManager();

    /*
      The devices are in /dev/input:

	crw-r--r--   1 root     root      13,  64 Apr  1 10:49 event0
	crw-r--r--   1 root     root      13,  65 Apr  1 10:50 event1
	crw-r--r--   1 root     root      13,  66 Apr  1 10:50 event2
	crw-r--r--   1 root     root      13,  67 Apr  1 10:50 event3
	...

      And so on up to event31.
     */
	private EventDevicePoller[] eventDevicePollers = new EventDevicePoller[32];

	@Override
	public void windowResized(WindowEvent e) { }

	@Override
	public void windowMoved(WindowEvent e) { }

	@Override
	public void windowDestroyNotify(WindowEvent e) {
		Object s = e.getSource();
		if(focusedWindow == s) {
			focusedWindow = null;
		}
	}

	@Override
	public void windowDestroyed(WindowEvent e) { }

	@Override
	public void windowGainedFocus(WindowEvent e) {
		Object s = e.getSource();
		if(s instanceof WindowImpl) {
			focusedWindow = (WindowImpl) s;
		}
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		Object s = e.getSource();
		if(focusedWindow == s) {
			focusedWindow = null;
		}
	}

	public static void main(String[] args ){
		LinuxEventDeviceTracker.getSingleton();
		try {
			Thread.sleep(30000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void windowRepaint(WindowUpdateEvent e) { }

    class EventDeviceManager implements Runnable {

        private volatile boolean stop = false;

        @Override
		public void run() {
            File f = new File("/dev/input/");
            int number;
            while(!stop){
                for(String path:f.list()){
                    if(path.startsWith("event")) {
                        String stringNumber = path.substring(5);
                        number = Integer.parseInt(stringNumber);
                        if(number<32&&number>=0) {
                            if(eventDevicePollers[number]==null){
                                eventDevicePollers[number] = new EventDevicePoller(number);
                                Thread t = new Thread(eventDevicePollers[number], "NEWT-LinuxEventDeviceTracker-event"+number);
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
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

	class EventDevicePoller implements Runnable {

        private volatile boolean stop = false;
        private String eventDeviceName;

        public EventDevicePoller(int eventDeviceNumber){
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
			ByteBuffer bb = ByteBuffer.wrap(b);
			StructAccessor s = new StructAccessor(bb);
			final File f = new File(eventDeviceName);
			f.setReadOnly();
			InputStream fis;
			try {
				fis = new FileInputStream(f);
			} catch (FileNotFoundException e) {
                stop=true;
				return;
			}

			int timeSeconds;
			int timeSecondFraction;
			short type;
			short code;
			int value;

			int keyCode=KeyEvent.VK_UNDEFINED;
			char keyChar=' ';
			int eventType=0;
			int modifiers=0;

            loop:
			while(!stop) {
				int remaining=16;
				while(remaining>0) {
					int read = 0;
					try {
						read = fis.read(b, 0, remaining);
					} catch (IOException e) {
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

				if(null != focusedWindow) {
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
						modifiers = 0;
						break;
					case 1: // EV_KEY
						keyCode = LinuxEVKey2NewtVKey(code); // The device independent code.
						keyChar = LinuxEVKey2Unicode(code); // The printable character w/o key modifiers.
						switch(value) {
						case 0:
							modifiers=0;
							eventType=KeyEvent.EVENT_KEY_RELEASED;
							focusedWindow.sendKeyEvent(eventType, modifiers, keyCode, keyChar);
							//Send syntetic typed
							focusedWindow.sendKeyEvent(KeyEvent.EVENT_KEY_TYPED, modifiers, keyCode, (char) keyChar);
							break;
						case 1:
							eventType=KeyEvent.EVENT_KEY_PRESSED;
							focusedWindow.sendKeyEvent(eventType, modifiers, keyCode, keyChar);
							break;
						case 2:
							eventType=KeyEvent.EVENT_KEY_PRESSED;
							modifiers=InputEvent.AUTOREPEAT_MASK;

							//Send syntetic autorepeat release
							focusedWindow.sendKeyEvent(KeyEvent.EVENT_KEY_RELEASED, modifiers, keyCode, keyChar);
							//Send syntetic typed
							focusedWindow.sendKeyEvent(KeyEvent.EVENT_KEY_TYPED, modifiers, keyCode, keyChar);

							focusedWindow.sendKeyEvent(eventType, modifiers, keyCode, keyChar);
							break;
						}
						break;
					case 4: // EV_MSC
						if(code==4) { // MSC_SCAN
							// scancode ignore, linux kernel specific
							// keyCode = value;
						}
						break;
						// TODO: handle joystick events
						// TODO: handle mouse events
						// TODO: handle headphone/hdmi connector events
					}

					//System.out.println("[time "+timeSeconds+":"+timeSecondFraction+"] type "+type+" / code "+code+" = value "+value);

				} else {
					//if(Window.DEBUG_KEY_EVENT)
					System.out.println("[time "+timeSeconds+":"+timeSecondFraction+"] type "+type+" / code "+code+" = value "+value);
				}
			}
			if(null != fis) {
				try {
					fis.close();
				} catch (IOException e) {
				}
			}
            stop=true;
		}

		private char LinuxEVKey2Unicode(short EVKey) {
			// This is the stuff normally mapped by a system keymap

			switch(EVKey){
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

		private int LinuxEVKey2NewtVKey(short EVKey) {
			char vkCode = KeyEvent.VK_UNDEFINED;

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
                    return KeyEvent.VK_DEAD_ACUTE;
                case 41: // grave
                    return KeyEvent.VK_DEAD_GRAVE;

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
                    return 0; // FIXME

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
                    return 0; // FIXME
                case 93: // katakana hiragana
                    return 0; // FIXME
                case 94: // mu henkan
                    return 0; // FIXME
                case 95: // kp jp comma
                    return 0; // FIXME

                case 97: // right ctrl
                    return KeyEvent.VK_CONTROL;
                case 98: // kp slash
                    return KeyEvent.VK_SLASH;

                case 99: // sysrq
                    return 0; // FIXME

                case 100: // right alt
                    return KeyEvent.VK_ALT;
                case 101: // linefeed
                    return 0; // FIXME
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
                    return 0; //FIXME DEAD_MACRON?
                case 113: // mute
                    return 0; //FIXME
                case 114: // vol up
                    return 0; //FIXME
                case 115: // vol down
                    return 0; //FIXME
                case 116: // power
                    return 0; //FIXME

                case 117: // kp equals
                    return KeyEvent.VK_EQUALS;
                case 118: // kp plus minux
                    return 0; // FIXME
                case 119: // pause
                    return KeyEvent.VK_PAUSE;
                case 120: // scale AL compiz scale expose
                    return 0;
                case 121: // kp comma
                    return KeyEvent.VK_COMMA;

                default:
				//System.out.println("LinuxEVKey2NewtVKey: Unmapped EVKey "+EVKey);	
			}

			return vkCode;
		}

		private boolean isLinuxEVKeyWithin(short eVKey, short min,
				short max) {
			if((eVKey>=min) && (eVKey<=max))
				return true;
			return false;
		}
	}    
}
