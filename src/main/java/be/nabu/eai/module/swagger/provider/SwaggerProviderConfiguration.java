package be.nabu.eai.module.swagger.provider;

import javax.xml.bind.annotation.XmlRootElement;

import be.nabu.eai.api.Comment;
import be.nabu.eai.api.LargeText;

@XmlRootElement(name = "swaggerProvider")
public class SwaggerProviderConfiguration {

	private String path, termsOfService, version, basePath;
	private boolean includeAll;

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

	@Comment(title = "Include everything in the application?", description = "By default the swagger provider will only include items from the parent it is in and any children. You can however document the whole web application if you set this to true")
	public boolean isIncludeAll() {
		return includeAll;
	}
	public void setIncludeAll(boolean includeAll) {
		this.includeAll = includeAll;
	}
	
}
