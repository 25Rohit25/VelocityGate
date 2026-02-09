package com.gateway.apigateway.circuitbreaker;

public enum CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}
