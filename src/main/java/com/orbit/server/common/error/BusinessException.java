package com.orbit.server.common.error;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getCode());
		this.errorCode = errorCode;
	}

}
