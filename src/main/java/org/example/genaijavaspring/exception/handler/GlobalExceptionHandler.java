package org.example.genaijavaspring.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ResponseBody
    @ExceptionHandler(value = {Exception.class})
    public ErrorDto handleException(Exception exception) {
        System.err.println("Exception occurred: " + exception.getMessage());
        return new ErrorDto(HttpStatus.BAD_REQUEST.getReasonPhrase(), exception.getMessage());
    }

}
