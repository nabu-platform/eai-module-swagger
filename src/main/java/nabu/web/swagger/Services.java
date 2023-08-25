package nabu.web.swagger;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import be.nabu.eai.module.swagger.provider.SwaggerProvider;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.swagger.formatter.SwaggerFormatter;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.api.DefinedType;

@WebService
public class Services {
	
	public void resetCache(@WebParam(name = "swaggerProviderId") String swaggerProviderId) {
		if (swaggerProviderId != null) {
			Artifact resolve = EAIResourceRepository.getInstance().resolve(swaggerProviderId);
			if (resolve instanceof SwaggerProvider) {
				((SwaggerProvider) resolve).refresh();
			}
			else {
				throw new IllegalArgumentException("Not a swagger provider: " + swaggerProviderId);
			}
		}
	}
	
	@WebResult(name = "json")
	public String formatAsJsonSchema(@WebParam(name = "typeId") String typeId) {
		DefinedType resolved = DefinedTypeResolverFactory.getInstance().getResolver().resolve(typeId);
		if (resolved == null) {
			throw new IllegalArgumentException("Not a type: " + typeId);
		}
		return SwaggerFormatter.formatTypeAsJSON(resolved);	
	}
	
}
