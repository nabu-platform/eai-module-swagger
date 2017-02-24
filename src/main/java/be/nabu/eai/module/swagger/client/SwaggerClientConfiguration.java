package be.nabu.eai.module.swagger.client;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Enumerator;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.ValueEnumerator;
import be.nabu.eai.module.http.client.HTTPClientArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.jaxb.CharsetAdapter;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;

@XmlRootElement(name = "swaggerClient")
public class SwaggerClientConfiguration {
	
	private HTTPClientArtifact httpClient;
	private Charset charset;
	private String username, password;
	private Boolean supportGzip, sanitizeOutput;
	private SecurityType security;
	private String host, basePath, scheme;
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public HTTPClientArtifact getHttpClient() {
		return httpClient;
	}
	public void setHttpClient(HTTPClientArtifact httpClient) {
		this.httpClient = httpClient;
	}
	
	@XmlJavaTypeAdapter(value = CharsetAdapter.class)
	public Charset getCharset() {
		return charset;
	}
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	@EnvironmentSpecific
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@EnvironmentSpecific
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	@EnvironmentSpecific
	public Boolean getSupportGzip() {
		return supportGzip;
	}
	public void setSupportGzip(Boolean supportGzip) {
		this.supportGzip = supportGzip;
	}

	public Boolean getSanitizeOutput() {
		return sanitizeOutput;
	}
	public void setSanitizeOutput(Boolean sanitizeOutput) {
		this.sanitizeOutput = sanitizeOutput;
	}
	
	public SecurityType getSecurity() {
		return security;
	}
	public void setSecurity(SecurityType security) {
		this.security = security;
	}
	
	@EnvironmentSpecific
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	@EnvironmentSpecific
	public String getBasePath() {
		return basePath;
	}
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	@EnvironmentSpecific
	@ValueEnumerator(enumerator = SchemeEnumerator.class)
	public String getScheme() {
		return scheme;
	}
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	public static class SchemeEnumerator implements Enumerator {

		@Override
		public List<?> enumerate() {
			List<String> schemes = new ArrayList<String>();
			schemes.add("http");
			schemes.add("https");
			return schemes;
		}
		
	}
}
