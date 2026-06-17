package com.tecassa.tech.test.java.spring.serdes;

public interface Serdes<T> {

    byte[] serialise(T object);

    T deserialise(byte[] bytes);
}
