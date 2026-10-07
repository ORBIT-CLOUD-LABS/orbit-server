package com.orbit.server.common.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

	String getCode();

	HttpStatus getStatus();

	String getMessage();

}
