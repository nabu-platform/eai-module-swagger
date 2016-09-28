package be.nabu.eai.module.swagger.provider;

import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.libs.resources.api.ResourceContainer;

public class SwaggerProviderManager extends JAXBArtifactManager<SwaggerProviderConfiguration, SwaggerProvider> {

	public SwaggerProviderManager() {
		super(SwaggerProvider.class);
	}

	@Override
	protected SwaggerProvider newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new SwaggerProvider(id, container, repository);
	}

}
