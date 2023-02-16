package be.nabu.eai.module.swagger.client.api;

import java.nio.charset.Charset;
import java.util.List;

import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;

public interface SwaggerOverride {
	public String getUsername();
	public String getPassword();
	public String getApiHeaderKey();
	public String getApiQueryKey();
	public String getHost();
	public String getBasePath();
	public String getScheme();
	// this _must_ be a subselection of the swagger client security, you can not add new methods, only limit existing ones
	public List<SecurityType> getSecurity();
	public Boolean getThrowException();
	public Charset getCharset();
	public String getSecurityType();
	public String getSecurityContext();
}
