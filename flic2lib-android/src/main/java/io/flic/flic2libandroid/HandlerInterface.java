package io.flic.flic2libandroid;

import android.content.Context;

/**
 * Interface for defining a custom handler. Normally not used.
 *
 * @see Flic2Manager#init(Context, HandlerInterface, LoggerInterface)
 */
public interface HandlerInterface {
    /**
     * Causes the Runnable r to be added to the message queue.
     * The runnable will be run on the thread to which this handler is
     * attached.
     *
     * @param r The Runnable that will be executed.
     */

    void post(Runnable r);
    /**
     * Causes the Runnable r to be added to the message queue, to be run
     * after the specified amount of time elapses.
     * The runnable will be run on the thread to which this handler
     * is attached.
     * <b>The time-base is {@link android.os.SystemClock#uptimeMillis}.</b>
     * Time spent in deep sleep will add an additional delay to execution.
     *
     * @param r The Runnable that will be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *        will be executed.
     */
    void postDelayed(Runnable r, long delayMillis);

    /**
     * Remove any pending posts of Runnable r that are in the message queue.
     *
     * @param r The Runnable that will be removed.
     */
    void removeCallbacks(Runnable r);

    /**
     * Checks if the calling thread is the same as the thread the handler is attached to.
     *
     * @return true if the calling thread is the same as the handler thread.
     */
    boolean currentThreadIsHandlerThread();
}
