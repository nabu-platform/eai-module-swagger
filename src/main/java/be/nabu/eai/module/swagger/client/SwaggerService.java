package be.nabu.eai.module.swagger.client;

import java.util.Set;

import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerPath;

public class SwaggerService implements DefinedService {

	private String id;
	private SwaggerPath path;
	private SwaggerMethod method;
	private SwaggerProxyInterface iface;
	private SwaggerClient client;

	public SwaggerService(String id, SwaggerClient client, SwaggerPath path, SwaggerMethod method) {
		this.id = id;
		this.client = client;
		this.path = path;
		this.method = method;
	}
	
	@Override
	public ServiceInterface getServiceInterface() {
		if (iface != null) {
			synchronized(this) {
				if (iface != null) {
					iface = new SwaggerServiceInterface(id, client.getDefinition(), path, method);
				}
			}
		}
		return iface;
	}

	@Override
	public ServiceInstance newInstance() {
		return new SwaggerServiceInstance(this);
	}

	@Override
	public Set<String> getReferences() {
		return null;
	}

	@Override
	public String getId() {
		return id;
	}

	public SwaggerClient getClient() {
		return client;
	}

	public SwaggerPath getPath() {
		return path;
	}

	public SwaggerMethod getMethod() {
		return method;
	}

}
