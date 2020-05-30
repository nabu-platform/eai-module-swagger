package be.nabu.eai.module.swagger.client.api;

import javax.jws.WebParam;
import javax.jws.WebResult;

public interface SwaggerOverrideProvider {
	@WebResult(name = "override")
	public SwaggerOverride overrideFor(
		@WebParam(name = "operationId") String operationId, 
		@WebParam(name = "method") String method, 
		@WebParam(name = "path") String path);
}
