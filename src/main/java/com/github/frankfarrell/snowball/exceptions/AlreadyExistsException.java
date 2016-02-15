package com.github.frankfarrell.snowball.exceptions;

import com.github.frankfarrell.snowball.controller.RestExceptionHandler;

/**
 * Created by Frank on 15/02/2016.
 */
public class AlreadyExistsException extends RuntimeException{

    public final String field;
    public final Object value;

    public AlreadyExistsException(final String field, final Object value){
        this.field = field;
        this.value = value;
    }

}
