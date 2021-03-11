package be.nabu.eai.module.swagger.provider;

import java.io.IOException;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;

public class SwaggerProviderGUIManager extends BaseJAXBGUIManager<SwaggerProviderConfiguration, SwaggerProvider> {

	public SwaggerProviderGUIManager() {
		super("Swagger Provider", SwaggerProvider.class, new SwaggerProviderManager(), SwaggerProviderConfiguration.class);
	}

	@Override
	public String getCategory() {
		return "REST";
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected SwaggerProvider newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new SwaggerProvider(entry.getId(), entry.getContainer(), entry.getRepository());
	}

}
