package be.nabu.eai.module.swagger.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import be.nabu.eai.module.swagger.client.api.SwaggerOverrideProvider;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.openapi.OpenApiParserv3;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.services.pojo.POJOUtils;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.parser.SwaggerParser;
import be.nabu.libs.types.map.MapContent;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

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
								MapContent parsed = SwaggerParser.parseJson(input);
								if (parsed.get("openapi") != null) {
									OpenApiParserv3 swaggerParser = new OpenApiParserv3();
									swaggerParser.setAllowUuid(getConfig().isAllowUuid());
									swaggerParser.setTimezone(getConfig().getTimezone());
									swaggerParser.setUuidFormat(getConfig().getUuidFormat());
									definition = swaggerParser.parse(getId(), parsed);
								}
//								else if (parsed.get("swagger") != null) {
								else {
									SwaggerParser swaggerParser = new SwaggerParser();
									swaggerParser.setTypeMapping(getConfig().getTypeAliases());
									swaggerParser.setAllowUuid(getConfig().isAllowUuid());
									swaggerParser.setTypeBase(getConfig().getTypeBase());
									swaggerParser.setTimezone(getConfig().getTimezone());
									swaggerParser.setUuidFormat(getConfig().getUuidFormat());
									definition = swaggerParser.parse(getId(), parsed);
								}
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
	
	public void load(URI uri) throws IOException {
		Resource child = getDirectory().getChild("swagger.json");
		if (child == null) {
			child = ((ManageableContainer<?>) getDirectory()).create("swagger.json", "application/json");
		}
		try {
			WritableContainer<ByteBuffer> writable = ((WritableResource) child).getWritable();
			try {
				InputStream stream = uri.toURL().openStream();
				try {
					IOUtils.copyBytes(IOUtils.wrap(stream), writable);
				}
				finally {
					stream.close();
				}
			}
			finally {
				writable.close();
			}
		}
		// if it fails to load, we remove the swagger.json so it doesn't stay in an empty state
		catch (Exception e) {
			((ManageableContainer<?>) getDirectory()).delete("swagger.json");
			throw new RuntimeException(e);
		}
	}
}
