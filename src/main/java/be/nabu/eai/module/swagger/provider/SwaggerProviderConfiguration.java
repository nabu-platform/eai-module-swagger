package be.nabu.eai.module.swagger.provider;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "swaggerProvider")
public class SwaggerProviderConfiguration {

	private String path, title, version, basePath;

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
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
	
}
