package be.snife.sbms.util.http;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import be.snife.sbms.api.exceptions.InvalidInputException;
import be.snife.sbms.api.exceptions.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@RestControllerAdvice
@Slf4j
class GlobalControllerExceptionHandler {

	// private static final Logger LOG =
	// LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

	@ResponseStatus(NOT_FOUND)
	@ExceptionHandler(NotFoundException.class)
	public @ResponseBody HttpErrorInfo handleNotFoundExceptions(ServerHttpRequest request, NotFoundException ex) {

		return createHttpErrorInfo(NOT_FOUND, request, ex);
	}

	@ResponseStatus(UNPROCESSABLE_ENTITY)
	@ExceptionHandler(InvalidInputException.class)
	public @ResponseBody HttpErrorInfo handleInvalidInputException(ServerHttpRequest request,
			InvalidInputException ex) {

		final String path = request.getPath().pathWithinApplication().value();
		final String message = ex.getMessage();

		log.debug("Returning HTTP status: {} for path: {}, message: {}", UNPROCESSABLE_ENTITY, path, message);

		return createHttpErrorInfo(UNPROCESSABLE_ENTITY, request, ex);
	}

	private HttpErrorInfo createHttpErrorInfo(HttpStatus httpStatus, ServerHttpRequest request, Exception ex) {

		final String path = request.getPath().pathWithinApplication().value();
		final String message = ex.getMessage();

		log.debug("Returning HTTP status: {} for path: {}, message: {}", httpStatus, path, message);
		return new HttpErrorInfo(httpStatus, path, message);
	}
}
