package org.sunbird.workflow.exception;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
@ResponseBody
@Getter
@Setter
public class InvalidDataInputException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private String code;
	private Object[] params;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Object[] getParams() {
		return params;
	}

	public void setParams(Object[] params) {
		this.params = params;
	}

	public InvalidDataInputException(String code, Object[] params) {
		super();
		this.code = code;
		this.params = params;
	}

	public InvalidDataInputException(String code) {
		super(code);
		this.code = code;
	}

	public InvalidDataInputException(String code, Throwable e) {
		super(code, e);
		this.code = code;
	}

}
