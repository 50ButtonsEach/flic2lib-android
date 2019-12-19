package io.flic.flic2libandroid;

/**
 * Logger interface.
 *
 * <p>For debugging purposes only.</p>
 */
public interface LoggerInterface {
    /**
     * Log an event.
     *
     * <p>This method is not allowed to call back into the library, or throw exceptions.</p>
     *
     * @param bdAddr the address
     * @param action an action text
     * @param text a message
     */
    void log(final String bdAddr, final String action, final String text);
}
