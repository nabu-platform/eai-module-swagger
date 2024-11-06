/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.swagger.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import be.nabu.libs.artifacts.ExternalDependencyImpl;
import be.nabu.libs.artifacts.api.ExternalDependency;
import be.nabu.libs.artifacts.api.ExternalDependencyArtifact;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerPath;

public class SwaggerService implements DefinedService, ExternalDependencyArtifact {

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
		if (iface == null) {
			synchronized(this) {
				if (iface == null) {
					iface = new SwaggerServiceInterface(id, client.getDefinition(), path, method, client.getConfig().getSecurity());
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

	public String getScheme() {
		String scheme = client.getConfig().getScheme();
		if (scheme == null && getMethod().getSchemes() != null && !getMethod().getSchemes().isEmpty()) {
			scheme = getMethod().getSchemes().get(0);
		}
		if (scheme == null && client.getDefinition().getSchemes() != null && !client.getDefinition().getSchemes().isEmpty()) {
			scheme = client.getDefinition().getSchemes().get(0);
		}
		return scheme == null ? "http" : scheme;
	}
	
	public String getHost() {
		String host = client.getConfig().getHost();
		if (host == null) {
			host = client.getDefinition().getHost();
		}
		return host;
	}
	
	public String getFullPath() {
		String path = client.getConfig().getBasePath();
		if (path == null) {
			path = client.getDefinition().getBasePath();
		}
		return (path == null ? getPath().getPath() : path + "/" + getPath().getPath()).replaceAll("[/]{2,}", "/");
	}
	
	@Override
	public List<ExternalDependency> getExternalDependencies() {
		List<ExternalDependency> dependencies = new ArrayList<ExternalDependency>();
		ExternalDependencyImpl dependency = new ExternalDependencyImpl();
		try {
			dependency.setEndpoint(new URI(
				getScheme(),
				// authority, this includes the port
				getHost(),
				getFullPath(),
				// query is only filled in at runtime
				null,
				// fragment is irrelevant?
				null));
		}
		catch (URISyntaxException e) {
			// can't help it...
		}
		dependency.setArtifactId(getId());
		dependency.setMethod(getMethod().getMethod().toUpperCase());
		dependency.setId(getMethod().getOperationId());
		dependency.setDescription(getMethod().getSummary() == null ? getMethod().getDescription() : getMethod().getSummary());
		dependency.setGroup(client.getId());
		dependency.setType("REST");
		dependency.setCredentials(client.getConfig().getUsername());
		dependencies.add(dependency);
		return dependencies;
	}

}
