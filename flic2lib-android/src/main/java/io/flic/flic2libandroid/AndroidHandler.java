package io.flic.flic2libandroid;

import android.os.Handler;

class AndroidHandler implements HandlerInterface {
    private Handler handler;

    public AndroidHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void post(Runnable r) {
        handler.post(r);
    }

    @Override
    public void postDelayed(Runnable r, long delay) {
        handler.postDelayed(r, delay);
    }

    @Override
    public void removeCallbacks(Runnable r) {
        handler.removeCallbacks(r);
    }

    @Override
    public boolean currentThreadIsHandlerThread() {
        return Thread.currentThread() == handler.getLooper().getThread();
    }
}
