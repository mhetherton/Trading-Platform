# Tecassa tech test for spring booted java 

http://localhost:8080/h2-ui
http://localhost:8080/api/orders



This small application represents a very quick scaffold for a matching engine that accepts new 
order over a rest api.

We have some parts out for you to fill in as you see fit. 

## ID Generator

The system must be able to generate a unique ID for every order request. The id can never be 
repeated, either while running or after a restart.

## Serlialiser/Deserialiser (serdes)

The application includes an artificial ipc/message barrier to simulate handing an order off to another 
processing thread or microservice. Complete the OrderSerdes implementation with a method of your choosing and
include comments to explain that choice.

## OrderMatcher

Complete the OrderMatcher class with a data structure to maintain all open orders and match any new orders
against any existing. More than one order can be matched and it should handle partial fills

## OrderMatcherTest

We have provided a skeleton test class for you to show how you would test your implementation. We are 
not asking for an exhaustive test suite, just a few examples to show how you would approach it.

## OrderApi

The application should be runnable and testable through the OrderApi via curl/postman etc.
