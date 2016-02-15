package com.github.frankfarrell.snowball.controller;

import com.github.frankfarrell.snowball.exceptions.AlreadyExistsException;
import com.github.frankfarrell.snowball.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by Frank
 *
 * Generic Exception Handler.
 * Intercepts Exceptions and returns http error codes and JSON Responses.
 * To use, inject bean into service application class as follows
 *      @Bean
 *      public static RestExceptionHandler restExceptionHandler(){
 *          return new RestExceptionHandler();
 *      }
 *
 */
@ControllerAdvice()
public class RestExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Return 404 when an attempt to read an entity that does not exist is made
     * @param req
     * @param ex
     * @return
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(value= HttpStatus.NOT_FOUND)
    @ResponseBody
    public NotFoundResponse handleMethodArgumentNotValid(HttpServletRequest req, NotFoundException ex) {

        String errorURL = req.getRequestURL().toString();

        logger.info(HttpStatus.NOT_FOUND+"|"+ errorURL);
        NotFoundResponse response = new NotFoundResponse(errorURL, ex.getField(), ex.getValue());
        return response;
    }

    protected class NotFoundResponse{

        public final String url;
        public final String field;
        public final Object value;

        public NotFoundResponse(String url, String field, Object value){
            this.url =url;
            this.field = field;
            this.value = value;
        }
    }

    /**
     * Return 409 When an attempt to create a resource that already exists is made
     * @param req
     * @param ex
     * @return
     */
    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseStatus(value= HttpStatus.CONFLICT)
    @ResponseBody
    public AlreadyExistsResponse handleMethodArgumentNotValid(HttpServletRequest req, AlreadyExistsException ex) {

        String errorURL = req.getRequestURL().toString();

        //For instance if a user is searched for by UUID, type would be User field wouuld be UUID and value  the value
        logger.info(HttpStatus.CONFLICT+"|"+ errorURL);
        AlreadyExistsResponse response = new AlreadyExistsResponse(errorURL, ex.field, ex.value);
        return response;
    }

    protected class AlreadyExistsResponse{

        public final String url;
        public final String field;
        public final Object value;

        public AlreadyExistsResponse(String url, String field, Object value){
            this.url =url;
            this.field = field;
            this.value = value;
        }
    }

    /**
     * Return 400 When a JSON With invlaid types (eg a boolean where an array is expected) is passed in body for create/update
     * @param req
     * @param ex
     * @return
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(value= HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ExceptionMessageResponse handleMethodArgumentNotValid(HttpServletRequest req, HttpMessageNotReadableException ex) {
        String errorURL = req.getRequestURL().toString();
        logger.info(HttpStatus.BAD_REQUEST + "|" + errorURL);
        return new ExceptionMessageResponse("The HTTP Request was unreadable");
    }

    public class ExceptionMessageResponse {
        public final String message;

        public ExceptionMessageResponse(String message) {
            this.message = message;
        }
    }
    //TODO Catch all 500
}
