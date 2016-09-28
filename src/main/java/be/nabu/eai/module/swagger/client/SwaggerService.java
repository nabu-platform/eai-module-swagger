package be.nabu.eai.module.swagger.client;

import java.util.Set;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerPath;

public class SwaggerService implements DefinedService {

	private String id;
	private SwaggerDefinition definition;
	private SwaggerPath path;
	private SwaggerMethod method;
	private SwaggerInterface iface;

	public SwaggerService(String id, SwaggerDefinition definition, SwaggerPath path, SwaggerMethod method) {
		this.id = id;
		this.definition = definition;
		this.path = path;
		this.method = method;
	}
	
	@Override
	public ServiceInterface getServiceInterface() {
		if (iface != null) {
			synchronized(this) {
				if (iface != null) {
					iface = new SwaggerInterface(id, definition, path, method);
				}
			}
		}
		return iface;
	}

	@Override
	public ServiceInstance newInstance() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getReferences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

}
