package uk.co.truenotfalse;

/**
 * An exception to indicate that a request limit has been exceeded.
 */
public class TooManyRequestsException extends RuntimeException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * <p>
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message The detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public TooManyRequestsException(final String message) {
        super(message);
    }


    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.
     * <p>
     * Note that the detail message associated with {@code cause} is not automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message The detail message (which is saved for later retrieval by the {@link #getMessage()} method).
     * @param cause   The cause (which is saved for later retrieval by the {@link #getCause()} method).  (A {@code null}
     *                value is permitted, and indicates that the cause is nonexistent or unknown).
     */
    public TooManyRequestsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
