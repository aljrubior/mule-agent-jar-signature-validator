package com.mycompany.agent.exceptions;

import com.mulesoft.agent.exception.ArtifactValidationException;

public class SignatureVerificationFailedException extends ArtifactValidationException {

    public SignatureVerificationFailedException() {
        super();
    }

    /**
     * Creates an instance of the exception with a message explaining the cause.
     *
     * @param message The message explaining the cause of the exception.
     */
    public SignatureVerificationFailedException(String message) {
        super(message);
    }

    /**
     * Creates an instance of the exception with a message explaining the cause and the exception that triggered it.
     *
     * @param message       The message explaining the cause of the exception.
     * @param originalCause The exception that triggered this exception.
     */
    public SignatureVerificationFailedException(String message, Exception originalCause) {
        super(message, originalCause);
    }
}