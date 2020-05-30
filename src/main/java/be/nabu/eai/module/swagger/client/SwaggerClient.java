package be.nabu.eai.module.swagger.client;

import java.io.IOException;
import java.io.InputStream;

import be.nabu.eai.module.swagger.client.api.SwaggerOverrideProvider;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.parser.SwaggerParser;
import be.nabu.utils.io.IOUtils;

public class SwaggerClient extends JAXBArtifact<SwaggerClientConfiguration> {
	
	private SwaggerDefinition definition;
	private SwaggerOverrideProvider overrideProvider;
	private boolean overrideProviderResolved;

	public SwaggerClient(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "swagger-client.xml", SwaggerClientConfiguration.class);
	}
	
	public SwaggerDefinition getDefinition() {
		if (definition == null) {
			Resource child = getDirectory().getChild("swagger.json");
			if (child != null) {
				synchronized(this) {
					if (definition == null) {
						try {
							InputStream input = IOUtils.toInputStream(((ReadableResource) child).getReadable());
							try {
								SwaggerParser swaggerParser = new SwaggerParser();
								swaggerParser.setTimezone(getConfig().getTimezone());
								definition = swaggerParser.parse(getId(), input);
							}
							finally {
								input.close();
							}
						}
						catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
		return definition;
	}

	public SwaggerOverrideProvider getOverrideProvider() {
		if (!overrideProviderResolved) {
			synchronized(this) {
				if (!overrideProviderResolved) {
					if (getConfig().getOverrideProvider() != null) {
						overrideProvider = POJOUtils.newProxy(SwaggerOverrideProvider.class, getRepository(), SystemPrincipal.ROOT, getConfig().getOverrideProvider());
					}
					overrideProviderResolved = true;
				}
			}
		}
		return overrideProvider;
	}
}
