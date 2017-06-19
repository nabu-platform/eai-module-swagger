package be.nabu.eai.module.swagger.provider;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.LargeText;

@XmlRootElement(name = "swaggerProvider")
@XmlType(propOrder = { "version", "basePath", "termsOfService", "path", "host", "scheme", "includeAll" })
public class SwaggerProviderConfiguration {

	private String path, termsOfService, version, basePath, host;
	private Scheme scheme;
	private boolean includeAll;

	@Advanced
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

	@Advanced
	@Comment(title = "Include everything in the application?", description = "By default the swagger provider will only include items from the parent it is in and any children. You can however document the whole web application if you set this to true")
	public boolean isIncludeAll() {
		return includeAll;
	}
	public void setIncludeAll(boolean includeAll) {
		this.includeAll = includeAll;
	}
	
	@Advanced
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	@Advanced
	public Scheme getScheme() {
		return scheme;
	}
	public void setScheme(Scheme scheme) {
		this.scheme = scheme;
	}

	public enum Scheme {
		HTTP, HTTPS
	}
}
