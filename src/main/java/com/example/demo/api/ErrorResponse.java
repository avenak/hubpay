package com.example.demo.api;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Models an API error response.
 */
@Data
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
}
