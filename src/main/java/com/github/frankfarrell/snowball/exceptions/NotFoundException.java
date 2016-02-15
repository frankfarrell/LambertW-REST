package com.github.frankfarrell.snowball.exceptions;

/**
 * Created by Frank on 15/02/2016.
 */
public class NotFoundException extends RuntimeException{

    private String field;
    private Object value;  //Where T is type of field being searched on

    public NotFoundException(String field, Object value){
        this.field = field;
        this.value = value;
    }

    public String getField(){
        return field;
    }

    public Object getValue(){
        return value;
    }

}
