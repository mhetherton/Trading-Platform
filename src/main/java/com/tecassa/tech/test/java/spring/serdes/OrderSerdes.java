package com.tecassa.tech.test.java.spring.serdes;

import com.tecassa.tech.test.java.spring.domain.Order;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import org.springframework.stereotype.Component;

/**
 * TODO - Fill in this serialiser/deserialiser with an implementation
 * of your choosing and explain your decision.
 * 
 * I'm using ByteArray Stream and Data Stream for the following reasons:
 * 1. No Manual Byte Counting: Using Data Stream and Data Stream
 * completely removes the need to track array positions or specify maximum
 * string lengths.
 * 2. Built-in String Handling: writeUTF and readUTF automatically
 * manage string lengths and encoding behind the scenes.
 * 3. Direct Enum Conversion: The code converts the enum to its raw string name
 * ("BUY" or "SELL") and reads it back using Order.Side.valueOf().
 * 4. The try with resource will automatically close the input and output
 * streams and ByteArray Streams.
 * 5. Its very simple and easy to read.
 * 
 */
@Slf4j
@Component
public class OrderSerdes implements Serdes<Order> {

    @Override
    public byte[] serialise(Order object) {
        if (object == null) {
            return new byte[0];
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos)) {

            dos.writeLong(object.getVolume());
            // Convert BigDecimal to string and write it UTF-encoded
            dos.writeUTF(object.getPrice().toString());
            dos.writeUTF(object.getSide() != null ? object.getSide().name() : "");

            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Serialization failed: {}", e);
            throw new RuntimeException("Serialization failed", e);
        }
    }

    @Override
    public Order deserialise(byte[] bytes) {
        if (bytes == null || bytes.length == 0)
            return null;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                DataInputStream dis = new DataInputStream(bais)) {

            Order order = new Order();
            order.setVolume(dis.readLong());
            order.setPrice(new BigDecimal(dis.readUTF()));
            String sideStr = dis.readUTF();
            order.setSide(sideStr.isEmpty() ? null : Order.Side.valueOf(sideStr));

            return order;
        } catch (IOException | IllegalArgumentException e) {
            log.error("Deserialization failed: {}", e);
            throw new RuntimeException("Deserialization failed", e);
        }
    }
}
