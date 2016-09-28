package be.nabu.eai.module.swagger.client;

import java.io.IOException;
import java.io.InputStream;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.swagger.SwaggerParser;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.utils.io.IOUtils;

public class SwaggerClient extends JAXBArtifact<SwaggerClientConfiguration> {
	
	private SwaggerDefinition definition;

	public SwaggerClient(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "swagger-client.xml", SwaggerClientConfiguration.class);
	}
	
	public SwaggerDefinition getDefinition() throws IOException {
		if (definition == null) {
			Resource child = getDirectory().getChild("swagger.json");
			if (child != null) {
				synchronized(this) {
					if (definition == null) {
						InputStream input = IOUtils.toInputStream(((ReadableResource) child).getReadable());
						try {
							definition = SwaggerParser.parse(getId(), input);
						}
						finally {
							input.close();
						}
					}
				}
			}
		}
		return definition;
	}

}
