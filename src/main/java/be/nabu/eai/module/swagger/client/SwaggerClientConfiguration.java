package be.nabu.eai.module.swagger.client;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Enumerator;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.api.ValueEnumerator;
import be.nabu.eai.module.http.client.HTTPClientArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.jaxb.CharsetAdapter;
import be.nabu.eai.repository.jaxb.TimeZoneAdapter;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;
import be.nabu.libs.types.api.annotation.Field;

@XmlRootElement(name = "swaggerClient")
public class SwaggerClientConfiguration {
	
	private HTTPClientArtifact httpClient;
	private Charset charset;
	private String username, password, userAgent, apiHeaderKey, apiQueryKey;
	private Boolean supportGzip, sanitizeOutput;
	private boolean allowDomain;
	private List<SecurityType> security;
	private String host, basePath, scheme;
	private boolean throwException, lenient = true, showInterfaces;
	private TimeZone timezone;
	// only these operationids are exposed
	private List<String> operationIds = new ArrayList<String>();
	private Boolean exposeAllServices;
	private String apiHeaderName, apiQueryName;
	private FolderStructure folderStructure;
	private DefinedService overrideProvider;
	
	@Advanced
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
	
	@Field(show = "'basic' # security")
	@EnvironmentSpecific
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Field(show = "'basic' # security")
	@EnvironmentSpecific
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	@Advanced
	@EnvironmentSpecific
	public Boolean getSupportGzip() {
		return supportGzip;
	}
	public void setSupportGzip(Boolean supportGzip) {
		this.supportGzip = supportGzip;
	}

	@Advanced
	public Boolean getSanitizeOutput() {
		return sanitizeOutput;
	}
	public void setSanitizeOutput(Boolean sanitizeOutput) {
		this.sanitizeOutput = sanitizeOutput;
	}
	
	public List<SecurityType> getSecurity() {
		return security;
	}
	public void setSecurity(List<SecurityType> security) {
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

	public boolean isThrowException() {
		return throwException;
	}
	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}
	
	@Advanced
	public boolean isLenient() {
		return lenient;
	}
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}
	
	@Advanced
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	@Advanced
	public boolean isShowInterfaces() {
		return showInterfaces;
	}
	public void setShowInterfaces(boolean showInterfaces) {
		this.showInterfaces = showInterfaces;
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

	@Advanced
	@XmlJavaTypeAdapter(value = TimeZoneAdapter.class)
	public TimeZone getTimezone() {
		return timezone;
	}
	public void setTimezone(TimeZone timezone) {
		this.timezone = timezone;
	}
	
	public Boolean getExposeAllServices() {
		return exposeAllServices;
	}
	public void setExposeAllServices(Boolean exposeAllServices) {
		this.exposeAllServices = exposeAllServices;
	}
	
	@Field(show = "'basic' # security", comment = "When using NTLM rather then basic authentication, we can provide a domain in the username, enable this to use NTLM-style domain & user")
	public boolean isAllowDomain() {
		return allowDomain;
	}
	public void setAllowDomain(boolean allowDomain) {
		this.allowDomain = allowDomain;
	}
	
	public List<String> getOperationIds() {
		return operationIds;
	}
	public void setOperationIds(List<String> operationIds) {
		this.operationIds = operationIds;
	}
	
	@Field(show = "'apiKey' # security && apiQueryName == null", comment = "The name of the header where we inject the API key")
	public String getApiHeaderName() {
		return apiHeaderName;
	}
	public void setApiHeaderName(String apiHeaderName) {
		this.apiHeaderName = apiHeaderName;
	}
	
	@Field(show = "'apiKey' # security && apiHeaderName == null", comment = "The name of the query parameter where we inject the API key")
	public String getApiQueryName() {
		return apiQueryName;
	}
	public void setApiQueryName(String apiQueryName) {
		this.apiQueryName = apiQueryName;
	}
	
	@Advanced
	public FolderStructure getFolderStructure() {
		return folderStructure;
	}
	public void setFolderStructure(FolderStructure folderStructure) {
		this.folderStructure = folderStructure;
	}
	
	@Advanced
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	@InterfaceFilter(implement = "be.nabu.eai.module.swagger.client.api.SwaggerOverrideProvider.overrideFor")
	public DefinedService getOverrideProvider() {
		return overrideProvider;
	}
	public void setOverrideProvider(DefinedService overrideProvider) {
		this.overrideProvider = overrideProvider;
	}
	
	@EnvironmentSpecific
	@Field(show = "'apiKey' # security", comment = "The API header key value")
	public String getApiHeaderKey() {
		return apiHeaderKey;
	}
	public void setApiHeaderKey(String apiHeaderKey) {
		this.apiHeaderKey = apiHeaderKey;
	}
	
	@EnvironmentSpecific
	@Field(show = "'apiKey' # security", comment = "The API query key value")
	public String getApiQueryKey() {
		return apiQueryKey;
	}
	public void setApiQueryKey(String apiQueryKey) {
		this.apiQueryKey = apiQueryKey;
	}
	

}
