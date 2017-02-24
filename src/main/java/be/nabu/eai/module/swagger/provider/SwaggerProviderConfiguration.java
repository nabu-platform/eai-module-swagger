package be.nabu.eai.module.swagger.provider;

import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.eai.api.LargeText;

@XmlRootElement(name = "swaggerProvider")
public class SwaggerProviderConfiguration {

	private String path, termsOfService, version, basePath;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getBasePath() {
		return basePath;
	}
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
		
	@LargeText
	public String getTermsOfService() {
		return termsOfService;
	}
	public void setTermsOfService(String termsOfService) {
		this.termsOfService = termsOfService;
	}
	
}
