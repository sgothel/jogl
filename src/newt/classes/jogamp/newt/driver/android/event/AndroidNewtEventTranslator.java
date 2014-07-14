package jogamp.newt.driver.android.event;

import jogamp.newt.driver.android.WindowDriver;
import android.view.View;

public class AndroidNewtEventTranslator implements View.OnKeyListener, View.OnTouchListener, View.OnFocusChangeListener, View.OnGenericMotionListener {
        private final WindowDriver newtWindow;
        private final AndroidNewtEventFactory factory;

        public AndroidNewtEventTranslator(final WindowDriver newtWindow, final android.content.Context context, final android.os.Handler handler) {
            this.newtWindow = newtWindow;
            this.factory = new AndroidNewtEventFactory(context, handler);
        }

        private final boolean processTouchMotionEvents(final View v, final android.view.MotionEvent event, final boolean isOnTouchEvent) {
            final boolean eventSent = factory.sendPointerEvent(true /*enqueue*/, false /*wait*/, true /*setFocusOnDown*/,
                                                               isOnTouchEvent, event, newtWindow);
            if( eventSent ) {
                try { Thread.sleep((long) (100.0F/3.0F)); } // 33 ms - FIXME ??
                catch(final InterruptedException e) { }
                return true; // consumed/handled, further interest in events
            }
            return false; // no mapping, no further interest in the event!
        }

        @Override
        public boolean onTouch(final View v, final android.view.MotionEvent event) {
            return processTouchMotionEvents(v, event, true);
        }

        @Override
        public boolean onGenericMotion(final View v, final android.view.MotionEvent event) {
            return processTouchMotionEvents(v, event, false);
        }

        @Override
        public boolean onKey(final View v, final int keyCode, final android.view.KeyEvent event) {
            final com.jogamp.newt.event.KeyEvent newtEvent = AndroidNewtEventFactory.createKeyEvent(event, newtWindow, false /* no system keys */);
            if(null != newtEvent) {
                newtWindow.enqueueEvent(false, newtEvent);
                return true;
            }
            return false;
        }

        @Override
        public void onFocusChange(final View v, final boolean hasFocus) {
            newtWindow.focusChanged(false, hasFocus);
        }
}
