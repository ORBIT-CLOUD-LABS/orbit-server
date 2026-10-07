package com.orbit.server.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

	String getCode();

	HttpStatus getStatus();

	String getMessage();

}
