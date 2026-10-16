package org.example.genaijavaspring.exception.handler;

public record ErrorDto(
        String code,
        String message
) {
}
