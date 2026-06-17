package com.trading.tech.serdes;

public interface Serdes<T> {

    byte[] serialise(T object);

    T deserialise(byte[] bytes);
}
