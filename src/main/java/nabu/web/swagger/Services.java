package nabu.web.swagger;

import javax.jws.WebParam;
import javax.jws.WebService;

import be.nabu.eai.module.swagger.provider.SwaggerProvider;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.artifacts.api.Artifact;

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
	
}
