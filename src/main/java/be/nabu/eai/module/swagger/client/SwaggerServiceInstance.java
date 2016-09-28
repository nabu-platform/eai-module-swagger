package be.nabu.eai.module.swagger.client;

import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.types.api.ComplexContent;

public class SwaggerServiceInstance implements ServiceInstance {

	private SwaggerService service;

	public SwaggerServiceInstance(SwaggerService service) {
		this.service = service;
	}
	
	@Override
	public Service getDefinition() {
		return service;
	}

	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		// TODO Auto-generated method stub
		return null;
	}

}
