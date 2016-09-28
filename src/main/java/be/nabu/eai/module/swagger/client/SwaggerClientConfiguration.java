package be.nabu.eai.module.swagger.client;

import java.nio.charset.Charset;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.module.http.client.HTTPClientArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.jaxb.CharsetAdapter;

@XmlRootElement(name = "swaggerClient")
public class SwaggerClientConfiguration {
	private HTTPClientArtifact httpClient;
	private Charset charset;
	private String username, password;
	
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
	
}
