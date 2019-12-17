package io.flic.flic2libandroid;

/**
 * Logger interface.
 *
 * For debugging purposes only.
 */
public interface LoggerInterface {
    /**
     * Log an event.
     *
     * This method is not allowed to call back into the library, or throw exceptions.
     *
     * @param bdAddr the address
     * @param action an action text
     * @param text a message
     */
    void log(final String bdAddr, final String action, final String text);
}
