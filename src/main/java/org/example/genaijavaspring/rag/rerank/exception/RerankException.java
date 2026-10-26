package org.example.genaijavaspring.rag.rerank.exception;

public class RerankException extends RuntimeException{

    public RerankException(String message, Throwable cause) {
        super(message, cause);
    }

    public RerankException(String message) {
        super(message);
    }
}
