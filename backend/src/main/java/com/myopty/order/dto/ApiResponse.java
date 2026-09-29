package com.myopty.order.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard response envelope defined in CONTRIBUTION.md:
 *
 * <pre>
 * { "success": true,  "data": { ... }, "meta": { ... } }
 * { "success": false, "error": { "code": "...", "message": "...", "fieldErrors": { ... } } }
 * </pre>
 *
 * <p>It currently lives in the {@code order} module rather than {@code shared}
 * because changes to {@code com.myopty.shared} need team consensus. Promote it
 * once the module owners agree.
 *
 * @param data  payload, present on success
 * @param error failure detail, present on error
 * @param meta  optional metadata such as pagination, omitted when null
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error, Map<String, Object> meta) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null, null);
	}

	public static <T> ApiResponse<T> ok(T data, Map<String, Object> meta) {
		return new ApiResponse<>(true, data, null, meta);
	}

	public static <T> ApiResponse<T> error(String code, String message) {
		return new ApiResponse<>(false, null, new ApiError(code, message, null), null);
	}

	public static <T> ApiResponse<T> error(String code, String message, Map<String, String> fieldErrors) {
		return new ApiResponse<>(false, null, new ApiError(code, message, fieldErrors), null);
	}

	/**
	 * @param code        stable machine-readable error code, e.g. {@code VALIDATION_FAILED}
	 * @param message     human-readable summary, safe to show the customer
	 * @param fieldErrors one message per field path, omitted when empty
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ApiError(String code, String message, Map<String, String> fieldErrors) {
	}

}
