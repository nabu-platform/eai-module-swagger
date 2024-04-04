package be.nabu.eai.module.swagger.client;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Enumerator;
import be.nabu.eai.api.EnvironmentSpecific;
import be.nabu.eai.api.InterfaceFilter;
import be.nabu.eai.api.ValueEnumerator;
import be.nabu.eai.developer.impl.HTTPAuthenticatorEnumerator;
import be.nabu.eai.module.http.client.HTTPClientArtifact;
import be.nabu.eai.module.rest.client.RESTEndpointArtifact;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.eai.repository.jaxb.CharsetAdapter;
import be.nabu.eai.repository.jaxb.TimeZoneAdapter;
import be.nabu.eai.repository.util.KeyValueMapAdapter;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;
import be.nabu.libs.types.api.annotation.Field;
import be.nabu.libs.types.base.UUIDFormat;

@XmlRootElement(name = "swaggerClient")
public class SwaggerClientConfiguration {
	
	private HTTPClientArtifact httpClient;
	private Charset charset;
	private String username, password, userAgent, apiHeaderKey, apiQueryKey, bearerToken;
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
	private String typeBase;
	private URI lastLoadedUri;
	// if the external system uses a different uuid formatting options, it might be relevant for marshalling!
	// e.g. eucosys only accepts uuids formatted _with_ the dash
	private UUIDFormat uuidFormat;
	// for some reason some people generate as swagger where the format is set to "uuid" but it is actually not a valid uuid...
	// for instance sendgrid for some reason uses a valid uuid but prepends it with "d-" making it invalid...
	private boolean allowUuid = true;
	
	private Map<String, String> operationAliases, typeAliases;
	
	// the type of the security needed (depends on whats available)
	private String securityType;
	// the security context within that type
	private String securityContext;
	
	private RESTEndpointArtifact endpoint;
	
	@Field(comment = "You can opt for using a specific http client, for example if you are working with self-signed certificates for internal infrastructure. If left empty, the default http client will be used.")
	@Advanced
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public HTTPClientArtifact getHttpClient() {
		return httpClient;
	}
	public void setHttpClient(HTTPClientArtifact httpClient) {
		this.httpClient = httpClient;
	}
	
	@Advanced
	@Field(comment = "The charset used to communicate with the swagger provider. The system default will be used if left empty.")
	@XmlJavaTypeAdapter(value = CharsetAdapter.class)
	public Charset getCharset() {
		return charset;
	}
	public void setCharset(Charset charset) {
		this.charset = charset;
	}
	
	@Field(show = "'basic' # security", group = "security")
	@EnvironmentSpecific
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	@Field(show = "'basic' # security", group = "security")
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
	
	@Field(group = "security")
	public List<SecurityType> getSecurity() {
		return security;
	}
	public void setSecurity(List<SecurityType> security) {
		this.security = security;
	}
	
	@Field(comment = "The host name of the target server that will be called. If left empty this is retrieved from the swagger.")
	@EnvironmentSpecific
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	@Field(comment = "The base path for all the calls. If left empty, this is retrieved from the swagger.")
	@EnvironmentSpecific
	public String getBasePath() {
		return basePath;
	}
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	@Field(comment = "The scheme that should be used to communicate with the target server. If left empty, this is retrieved from the swagger.")
	@EnvironmentSpecific
	@ValueEnumerator(enumerator = SchemeEnumerator.class)
	public String getScheme() {
		return scheme;
	}
	public void setScheme(String scheme) {
		this.scheme = scheme;
	}

	@Advanced
	@Field(comment = "If the remote server returns a code indicating something went wrong and this is enabled, an exception will be thrown.")
	public boolean isThrowException() {
		return throwException;
	}
	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}
	
	@Field(comment = "Whether or not we should be lenient when parsing results. If lenient is turned off, any irregularities will lead to exceptions being thrown.")
	@Advanced
	public boolean isLenient() {
		return lenient;
	}
	public void setLenient(boolean lenient) {
		this.lenient = lenient;
	}

	@Field(comment = "Some operation providers require a user agent to be filled in. If left empty, no user agent will be sent along.")
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	// this is currently no longer relevant until we have a use for the interfaces (e.g. proxying)
	@Field(hide = "true")
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

	// example of this is the .NET stack used by cogenius -> they can't handle "date" fields so it must be expressed as "dateTime" but the time component _must_ be midnight _in_ CET.
	@Field(comment = "In some cases, the default timezone used to communicate with the external provider is relevant and can not be correctly sent along in the dateTime string. In that case you can force a specific timezone to be used.")
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
	
	@Field(show = "'basic' # security", comment = "When using NTLM rather then basic authentication, we can provide a domain in the username, enable this to use NTLM-style domain & user", group = "security")
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
	
	@Field(show = "'apiKey' # security && apiQueryName == null", comment = "The name of the header where we inject the API key", group = "security")
	public String getApiHeaderName() {
		return apiHeaderName;
	}
	public void setApiHeaderName(String apiHeaderName) {
		this.apiHeaderName = apiHeaderName;
	}
	
	@Field(show = "'apiKey' # security && apiHeaderName == null", comment = "The name of the query parameter where we inject the API key", group = "security")
	public String getApiQueryName() {
		return apiQueryName;
	}
	public void setApiQueryName(String apiQueryName) {
		this.apiQueryName = apiQueryName;
	}
	
	@Field(comment = "There are a number of ways to structure the operations exposed by this swagger. Once you start using the resulting services, it is highly advised not to change this setting anymore.")
	@Advanced
	public FolderStructure getFolderStructure() {
		return folderStructure;
	}
	public void setFolderStructure(FolderStructure folderStructure) {
		this.folderStructure = folderStructure;
	}
	
	@Field(comment = "In some cases the setup in for example PRD is different from that DEV, the overrider allows you to perform more complex rewriting rules based on configuration.")
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
	@Field(show = "'apiKey' # security && apiHeaderName != null", comment = "The API key we send along in the header", group = "security")
	public String getApiHeaderKey() {
		return apiHeaderKey;
	}
	public void setApiHeaderKey(String apiHeaderKey) {
		this.apiHeaderKey = apiHeaderKey;
	}
	
	@EnvironmentSpecific
	@Field(show = "'apiKey' # security && apiQueryName != null", comment = "The API key we send along in the query parameter", group = "security")
	public String getApiQueryKey() {
		return apiQueryKey;
	}
	public void setApiQueryKey(String apiQueryKey) {
		this.apiQueryKey = apiQueryKey;
	}
	
	@Field(comment = "If the types have long '.' separated names and they all have a same base, you can fill this in and the base will be skipped. Once you start using the types or services, it is highly advised not to change this setting anymore.")
	@Advanced
	public String getTypeBase() {
		return typeBase;
	}
	public void setTypeBase(String typeBase) {
		this.typeBase = typeBase;
	}
	
	// this just keeps track of the last uri that was loaded, making it easier to "reload" without having to look up the uri
	@Field(hide = "true")
	public URI getLastLoadedUri() {
		return lastLoadedUri;
	}
	public void setLastLoadedUri(URI lastLoadedUri) {
		this.lastLoadedUri = lastLoadedUri;
	}
	
	@Field(hide = "!allowUuid")
	@Advanced
	public UUIDFormat getUuidFormat() {
		return uuidFormat;
	}
	public void setUuidFormat(UUIDFormat uuidFormat) {
		this.uuidFormat = uuidFormat;
	}
	
	@Advanced
	public boolean isAllowUuid() {
		return allowUuid;
	}
	public void setAllowUuid(boolean allowUuid) {
		this.allowUuid = allowUuid;
	}
	
	@Field(show = "'bearer' # security", group = "security", comment = "In some cases the bearer token has a fixed value")
	public String getBearerToken() {
		return bearerToken;
	}
	public void setBearerToken(String bearerToken) {
		this.bearerToken = bearerToken;
	}
	
	@Field(group = "security", comment = "Use a pluggable security provider")
	@ValueEnumerator(enumerator = HTTPAuthenticatorEnumerator.class)
	public String getSecurityType() {
		return securityType;
	}
	public void setSecurityType(String securityType) {
		this.securityType = securityType;
	}
	
	@Field(group = "security", comment = "The context for the pluggable security provider")
	public String getSecurityContext() {
		return securityContext;
	}
	public void setSecurityContext(String securityContext) {
		this.securityContext = securityContext;
	}
	
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getOperationAliases() {
		if (operationAliases == null) {
			operationAliases = new LinkedHashMap<String, String>();
		}
		return operationAliases;
	}
	public void setOperationAliases(Map<String, String> operationAliases) {
		this.operationAliases = operationAliases;
	}
	
	@Field(group = "typeMapping")
	@XmlJavaTypeAdapter(value = KeyValueMapAdapter.class)
	public Map<String, String> getTypeAliases() {
		if (typeAliases == null) {
			typeAliases = new LinkedHashMap<String, String>();
		}
		return typeAliases;
	}
	public void setTypeAliases(Map<String, String> typeAliases) {
		this.typeAliases = typeAliases;
	}
	
	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public RESTEndpointArtifact getEndpoint() {
		return endpoint;
	}
	public void setEndpoint(RESTEndpointArtifact endpoint) {
		this.endpoint = endpoint;
	}
}
