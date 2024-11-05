package be.nabu.eai.module.swagger.client;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.NotSupportedException;

import nabu.protocols.http.client.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.http.client.HTTPClientArtifact;
import be.nabu.eai.module.rest.WebResponseType;
import be.nabu.eai.module.swagger.client.api.SwaggerOverride;
import be.nabu.eai.module.swagger.client.api.SwaggerOverrideProvider;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.authentication.api.principals.BasicPrincipal;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.http.client.BasicAuthentication;
import be.nabu.libs.http.client.NTLMPrincipalImpl;
import be.nabu.libs.http.core.DefaultHTTPRequest;
import be.nabu.libs.http.core.HTTPRequestAuthenticatorFactory;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.http.glue.GlueListener;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.services.api.ExecutionContext;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceException;
import be.nabu.libs.services.api.ServiceInstance;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.types.base.CollectionFormat;
import be.nabu.libs.swagger.api.SwaggerResponse;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition;
import be.nabu.libs.swagger.api.SwaggerSecuritySetting;
import be.nabu.libs.swagger.api.SwaggerParameter.ParameterLocation;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.TypeBaseUtils;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.api.UnmarshallableBinding;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.form.FormBinding;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.json.JSONUnmarshaller;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.properties.AliasProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.types.structure.StructureGenerator;
import be.nabu.libs.types.structure.StructureInstance;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.ModifiablePart;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.MimeUtils;
import be.nabu.utils.mime.impl.PlainMimeContentPart;
import be.nabu.utils.mime.impl.PlainMimeEmptyPart;

public class SwaggerServiceInstance implements ServiceInstance {

	private SwaggerService service;
	private Logger logger = LoggerFactory.getLogger(getClass());

	public SwaggerServiceInstance(SwaggerService service) {
		this.service = service;
	}
	
	@Override
	public Service getDefinition() {
		return service;
	}

	private WebResponseType getRequestType() {
		List<String> consumes = service.getMethod().getConsumes();
		if (consumes == null || consumes.isEmpty()) {
			consumes = service.getClient().getDefinition().getConsumes();
		}
		if (consumes == null || consumes.isEmpty()) {
			return WebResponseType.JSON;
		}
		if (consumes.contains("application/json")) {
			return WebResponseType.JSON;
		}
		else if (consumes.contains("application/xml") || consumes.contains("text/xml")) {
			return WebResponseType.XML;
		}
		else if (consumes.contains("application/x-www-form-urlencoded")) {
			return WebResponseType.FORM_ENCODED;
		}
		return null;
	}
	
	private WebResponseType getResponseType() {
		List<String> produces = service.getMethod().getProduces();
		if (produces == null || produces.isEmpty()) {
			produces = service.getClient().getDefinition().getProduces();
		}
		if (produces == null || produces.isEmpty()) {
			return WebResponseType.JSON;
		}
		if (produces.contains("application/json")) {
			return WebResponseType.JSON;
		}
		else if (produces.contains("application/xml") || produces.contains("text/xml")) {
			return WebResponseType.XML;
		}
		else if (produces.contains("application/x-www-form-urlencoded")) {
			return WebResponseType.FORM_ENCODED;
		}
		return null;
	}
	
	private boolean isSecure() {
		if (service.getClient().getConfig().getScheme() != null) {
			return service.getClient().getConfig().getScheme().equals("https");
		}
		List<String> schemes = service.getMethod().getSchemes();
		if (schemes == null || schemes.isEmpty()) {
			schemes = service.getClient().getDefinition().getSchemes();
		}
		if ((schemes == null || schemes.isEmpty()) && service.getClient().getConfig().getEndpoint() != null) {
			return service.getClient().getConfig().getEndpoint().getConfig().getSecure() != null && service.getClient().getConfig().getEndpoint().getConfig().getSecure();
		}
		return schemes != null && !schemes.isEmpty() && schemes.contains("https");
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		try {
			SwaggerOverrideProvider overrideProvider = service.getClient().getOverrideProvider();
			SwaggerOverride override = overrideProvider == null ? null : overrideProvider.overrideFor(service.getMethod().getOperationId(), service.getMethod().getMethod(), service.getPath().getPath());
			// runtime wins
			String host = input == null ? null : (String) input.get("host");
			// check specific override
			if (host == null && override != null) {
				host = override.getHost();
			}
			if (host == null) {
				// otherwise something you explicitly configured for the whole client
				host = service.getClient().getConfig().getHost();
				if (host == null) {
					// otherwise the default provided host
					host = service.getClient().getDefinition().getHost();
				}
			}
			if (host == null && service.getClient().getConfig().getEndpoint() != null) {
				host = service.getClient().getConfig().getEndpoint().getConfig().getHost();
			}
			if (host == null) {
				throw new ServiceException("SWAGGER-CLIENT-1", "No host found for: " + service.getId());
			}
			
			String basePath = input == null ? null : (String) input.get("basePath");
			if (basePath == null && override != null) {
				basePath = override.getBasePath();
			}
			if (basePath == null) {
				basePath = service.getClient().getConfig().getBasePath();
				if (basePath == null) {
					basePath = service.getClient().getDefinition().getBasePath();
				}
			}
			if (basePath == null && service.getClient().getConfig().getEndpoint() != null) {
				basePath = service.getClient().getConfig().getEndpoint().getConfig().getBasePath();
			}
			
			// we assume the root path
			if (basePath == null) {
				basePath = "/";
//				throw new ServiceException("SWAGGER-CLIENT-2", "No basePath configured for: " + service.getId());
			}

			Charset charset = service.getClient().getConfig().getCharset();
			if (charset == null && override != null) {
				charset = override.getCharset();
			}
			if (charset == null && service.getClient().getConfig().getEndpoint() != null) {
				charset = service.getClient().getConfig().getEndpoint().getConfig().getCharset();
			}
			if (charset == null) {
				charset = Charset.defaultCharset();
			}
					
			String path = service.getPath().getPath();
			if (path == null) {
				path = "/";
			}
			List<Header> additionalHeaders = new ArrayList<Header>();
			
			// add custom headers if necessary
			if (input != null && input.get("headers") != null) {
				for (Object object : (List) input.get("headers")) {
					if (object instanceof ComplexContent) {
						object = TypeUtils.getAsBean((ComplexContent) object, Header.class);
					}
					additionalHeaders.add((Header) object);
				}
			}
			
			// you can set additional query data (e.g. labels) that do not have keys
			// this was added at a later point, not sure why this diverges so much from the rest client formatting logic
			String additionalQueryData = null;
			Map<String, List<String>> queryParameters = new HashMap<String, List<String>>();
			Map<String, List<String>> formParameters = new HashMap<String, List<String>>();

			ModifiablePart part;
			Object parameters = input == null ? null : input.get("parameters");
			if (parameters instanceof ComplexContent) {
				byte [] content = null;
				WebResponseType requestType = getRequestType();
				String contentType = requestType == null ? "application/octet-stream" : requestType.getMimeType();
				
				// check if there is a body content
				if (service.getMethod().getParameters() != null) {
					for (SwaggerParameter parameter : service.getMethod().getParameters()) {
						if (parameter.getElement() == null) {
							continue;
						}
						Object value = ((ComplexContent) parameters).get(parameter.getElement().getName());
						
						if (parameter.getLocation() == ParameterLocation.BODY && value == null && parameter.getElement().getType() instanceof ComplexType) {
							// check if its mandatory
							Value<Integer> minOccurs = parameter.getElement().getProperty(MinOccursProperty.getInstance());
							if (minOccurs == null || minOccurs.getValue() > 0) {
								if (parameter.getElement().getType().isList()) {
									value = new ArrayList();
								}
								else {
									value = ((ComplexType) parameter.getElement().getType()).newInstance();
								}
							}
						}
						
						if (value != null) {
							switch (parameter.getLocation()) {
								case BODY:
									if (!formParameters.isEmpty()) {
										throw new ServiceException("SWAGGER-1", "Input parameters of type 'body' and 'formData' can not co-exist in the same operation");
									}
									if (value instanceof InputStream) {
										try {
											content = IOUtils.toBytes(IOUtils.wrap((InputStream) value));
										}
										finally {
											((InputStream) value).close();
										}
									}
									else if (value instanceof byte[]) {
										content = (byte[]) value;
									}
									// @2024-08-14: we noticed that simple type arrays at the root were failing in the _next_ else if (so not being detected as list)
									// we updated list detection but reduced it to only capture simple types because complex types already seem to have support for root arrays
									else if (parameter.getElement().getType().isList(parameter.getElement().getProperties()) && parameter.getElement().getType() instanceof SimpleType) {
										// we have array support in json
										if (requestType == null || requestType == WebResponseType.JSON) {
											Structure wrapper = new Structure();
											wrapper.setName("wrapper");
											wrapper.add(TypeBaseUtils.clone(parameter.getElement(), wrapper));
											StructureInstance wrapperInstance = wrapper.newInstance();
											wrapperInstance.set(parameter.getElement().getName(), value);
											JSONBinding binding = new JSONBinding(wrapper, charset);
											binding.setIgnoreRootIfArrayWrapper(true);
											ByteArrayOutputStream output = new ByteArrayOutputStream();
											binding.marshal(output, wrapperInstance);
											content = output.toByteArray();
											if (requestType == null) {
												requestType = WebResponseType.JSON;
											}
											contentType = requestType.getMimeType();
										}
										else {
											throw new RuntimeException("Array inputs are not yet supported");
										}
									}
									else if (parameter.getElement().getType() instanceof SimpleType) {
										SimpleType<?> type = (SimpleType<?>) parameter.getElement().getType();
										if (type instanceof Marshallable) {
											String marshal = ((Marshallable) type).marshal(value, parameter.getElement().getProperties());
											value = marshal.getBytes(charset);
										}
										else {
											throw new RuntimeException("Non-marshallable simple type inputs are not yet supported");
										}
									}
									else {
										ComplexContent bodyContent = value instanceof ComplexContent ? (ComplexContent) value : ComplexContentWrapperFactory.getInstance().getWrapper().wrap(value);
		
										MarshallableBinding binding;
										if (requestType == null) {
											requestType = WebResponseType.JSON;
										}
										contentType = requestType.getMimeType();
										switch(requestType) {
											case FORM_ENCODED: 
												binding = new FormBinding(bodyContent.getType()); 
											break;
											case XML: 
												XMLBinding xmlBinding = new XMLBinding(bodyContent.getType(), charset);
												binding = xmlBinding;
											break;
											default:
												JSONBinding jsonBinding = new JSONBinding(bodyContent.getType(), charset);
												// if the maxOccurs is _on_ the type itself, we have a root array
												jsonBinding.setIgnoreRootIfArrayWrapper(ValueUtils.contains(MaxOccursProperty.getInstance(), parameter.getElement().getType().getProperties()));
												binding = jsonBinding;
												break;
										}
										ByteArrayOutputStream output = new ByteArrayOutputStream();
										binding.marshal(output, bodyContent);
										content = output.toByteArray();
									}
								break;
								case HEADER: 
									String headerStringVariable;
									if (parameter.getElement().getType() instanceof Marshallable) {
										headerStringVariable = ((Marshallable) parameter.getElement().getType()).marshal(value, parameter.getElement().getProperties());
									}
									else {
										headerStringVariable = (String) (value instanceof String ? value : ConverterFactory.getInstance().getConverter().convert(value, String.class));
									}
									if ("Content-Type".equalsIgnoreCase(parameter.getName())) {
										contentType = headerStringVariable;
									}
									else {
										additionalHeaders.add(new MimeHeader(parameter.getName(), headerStringVariable));
									}
								break;
								case PATH:
									String pathStringVariable;
									if (parameter.getElement().getType() instanceof Marshallable) {
										pathStringVariable = ((Marshallable) parameter.getElement().getType()).marshal(value, parameter.getElement().getProperties());
									}
									else {
										pathStringVariable = value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class);
									}
									path = path.replaceAll("\\{[\\s]*" + Pattern.quote(parameter.getName()) + "\\b[^}]*\\}",
										URIUtils.encodeURIComponent(Matcher.quoteReplacement(pathStringVariable), false));
								break;
								case FORMDATA:
									if (content != null) {
										throw new ServiceException("SWAGGER-1", "Input parameters of type 'body' and 'formData' can not co-exist in the same operation");
									}
								case QUERY:
									if (value instanceof ComplexContent) {
										throw new NotSupportedException("Complex content in query parameters is not yet supported");
									}
									else if (value instanceof Iterable) {
										StringBuilder builder = new StringBuilder();
										boolean first = true;
										CollectionFormat format = parameter.getCollectionFormat();
										if (format == null) {
											format = CollectionFormat.CSV;
										}
										if (format == CollectionFormat.MATRIX_EXPLODE) {
											additionalQueryData = ";";
											format = CollectionFormat.MULTI;
										}
										else if (format == CollectionFormat.MATRIX_IMPLODE) {
											additionalQueryData = ";";
											format = CollectionFormat.CSV;
										}
										if (format == CollectionFormat.MULTI) {
											List<String> values = new ArrayList<String>();
											for (Object child : (Iterable<?>) value) {
												String queryStringVariable;
												if (parameter.getElement().getType() instanceof Marshallable) {
													queryStringVariable = ((Marshallable) parameter.getElement().getType()).marshal(child, parameter.getElement().getProperties());
												}
												else {
													queryStringVariable = (String) (child instanceof String ? child : ConverterFactory.getInstance().getConverter().convert(child, String.class));
												}
												values.add(queryStringVariable);
											}
											if (values.isEmpty()) {
												continue;
											}
											if (parameter.getLocation() == ParameterLocation.FORMDATA) {
												formParameters.put(parameter.getName(), values);
											}
											else {
												queryParameters.put(parameter.getName(), values);
											}
										}
										else {
											for (Object child : (Iterable<?>) value) {
												if (child == null) {
													continue;
												}
												else if (child instanceof ComplexContent) {
													throw new NotSupportedException("Complex content in query parameters is not yet supported");
												}
												if (first) {
													first = false;
												}
												else {
													builder.append(format.getCharacter());
												}
												String queryStringVariable;
												if (parameter.getElement().getType() instanceof Marshallable) {
													queryStringVariable = ((Marshallable) parameter.getElement().getType()).marshal(child, parameter.getElement().getProperties());
												}
												else {
													queryStringVariable = (String) (child instanceof String ? child : ConverterFactory.getInstance().getConverter().convert(child, String.class));
												}
												builder.append(queryStringVariable);
											}
											// nothing in iterable
											if (first) {
												continue;
											}
											if (parameter.getLocation() == ParameterLocation.FORMDATA) {
												formParameters.put(parameter.getName(), Arrays.asList(builder.toString()));
											}
											else {
												if (format == CollectionFormat.LABEL) {
													additionalQueryData = "." + builder.toString();
												}
												else {
													queryParameters.put(parameter.getName(), Arrays.asList(builder.toString()));
												}
											}
										}
									}
									else if (parameter.getLocation() == ParameterLocation.FORMDATA) {
										String formStringVariable;
										if (parameter.getElement().getType() instanceof Marshallable) {
											formStringVariable = ((Marshallable) parameter.getElement().getType()).marshal(value, parameter.getElement().getProperties());
										}
										else {
											formStringVariable = value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class);
										}
										formParameters.put(parameter.getName(), Arrays.asList(formStringVariable));
									}
									else {
										String otherStringVariable;
										if (parameter.getElement().getType() instanceof Marshallable) {
											otherStringVariable = ((Marshallable) parameter.getElement().getType()).marshal(value, parameter.getElement().getProperties());
										}
										else {
											otherStringVariable = value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class);
										}
										queryParameters.put(parameter.getName(), Arrays.asList(otherStringVariable));
									}
								break;
							}
						}
					}
				}
				
				// if we have a form and we have formData parameters, use that to build the body content
				if (content == null && !formParameters.isEmpty()) {
					String formContent = "";
					boolean first = true;
					for (String key : formParameters.keySet()) {
						for (String value : formParameters.get(key)) {
							if (first) {
								first = false;
							}
							else {
								formContent += "&";
							}
							formContent += URIUtils.encodeURIComponent(key, false) + "=" + URIUtils.encodeURIComponent(value, false);
						}
					}
					if (!formContent.isEmpty()) {
						content = formContent.getBytes(Charset.forName("UTF-8"));
						contentType = WebResponseType.FORM_ENCODED.getMimeType();
					}
				}
				
				part = content == null ? new PlainMimeEmptyPart(null, new MimeHeader("Content-Length", "0")) : new PlainMimeContentPart(null, IOUtils.wrap(content, true), 
					new MimeHeader("Content-Length", Integer.valueOf(content.length).toString()),
					new MimeHeader("Content-Type", contentType)
				);
				// if we have a content part, we can reopen it
				if (part instanceof PlainMimeContentPart) {
					((PlainMimeContentPart) part).setReopenable(true);
				}
			}
			else if (parameters == null) {
				part = new PlainMimeEmptyPart(null, new MimeHeader("Content-Length", "0"));
			}
			else {
				throw new ServiceException("SWAGGER-CLIENT-3", "Invalid content");
			}
			
			// request the proper type
			WebResponseType responseType = getResponseType();
			if (responseType != null) {
				part.setHeader(new MimeHeader("Accept", responseType.getMimeType()));
			}
			
			// add whatever headers were requested
			part.setHeader(additionalHeaders.toArray(new Header[additionalHeaders.size()]));
			
			// never use gzip if the server is in development mode
			if (service.getClient().getConfig().getSupportGzip() != null && service.getClient().getConfig().getSupportGzip() && !EAIResourceRepository.isDevelopment()) {
				Long contentLength = MimeUtils.getContentLength(part.getHeaders());
				// if we don't have a content length, we are already chunking and want to gzip as well
				// if we have a content length and it is non-zero, we have content that needs gzipping as well
				if (contentLength == null || contentLength > 0) {
					part.setHeader(new MimeHeader("Content-Encoding", "gzip"));
				}
				// if we have a content-length at this point, replace it with chunked
				if (contentLength != null && contentLength > 0) {
					part.removeHeader("Content-Length");
					part.setHeader(new MimeHeader("Transfer-Encoding", "Chunked"));
				}
				// always accept gzip in this case
				part.setHeader(new MimeHeader("Accept-Encoding", "gzip"));
			}
			part.setHeader(new MimeHeader("Host", host));
			
			// github requires a user agent for its api access...
			if (service.getClient().getConfig().getUserAgent() != null) {
				part.setHeader(new MimeHeader("User-Agent", service.getClient().getConfig().getUserAgent()));
			}
			
			// we support bearer tokens for oauth2
			String bearerToken = input == null ? null : (String) input.get("authentication/oauth2Token");
			// and in general
			if (bearerToken == null) {
				bearerToken = input == null ? null : (String) input.get("authentication/bearerToken");
			}
			// if we don't have a bearer token yet but you explicitly configured it as security option, check any configured static bearer token
			if (bearerToken == null && service.getClient().getConfig().getSecurity() != null && service.getClient().getConfig().getSecurity().contains(SecurityType.bearer)) {
				bearerToken = service.getClient().getConfig().getBearerToken();
			}
			if (bearerToken != null) {
				part.setHeader(new MimeHeader("Authorization", "Bearer " + bearerToken));
			}
			
			final String username = input == null || input.get("authentication/username") == null ? (override == null || override.getUsername() == null ? service.getClient().getConfig().getUsername() : override.getUsername()) : (String) input.get("authentication/username");
			final String password = input == null || input.get("authentication/password") == null ? (override == null || override.getPassword() == null ? service.getClient().getConfig().getPassword() : override.getPassword()) : (String) input.get("authentication/password");

			BasicPrincipal principal = null;
			if (username != null) {
				int index = username.indexOf('/');
				if (index < 0) {
					index = username.indexOf('\\');
				}
				if (index < 0 || !service.getClient().getConfig().isAllowDomain()) {
					principal = new BasicPrincipal() {
						private static final long serialVersionUID = 1L;
						@Override
						public String getName() {
							return username;
						}
						@Override
						public String getPassword() {
							return password;
						}
					};
				}
				// create an NTLM principal
				else if (username != null) {
					principal = new NTLMPrincipalImpl(username.substring(0, index), username.substring(index + 1), password);
				}
			}
			
			if (!basePath.equals("/")) {
				path = basePath + "/" + path.replaceFirst("^[/]+", "");
			}
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			
			boolean first = true;
			for (String key : queryParameters.keySet()) {
				for (String value : queryParameters.get(key)) {
					if (first) {
						first = false;
						path += "?";
					}
					else {
						path += "&";
					}
					path += URIUtils.encodeURIComponent(key, false) + "=" + URIUtils.encodeURIComponent(value, false);
				}
			}
			if (additionalQueryData != null) {
				if (first) {
					path += "?";
					first = false;
				}
				path += additionalQueryData;
			}
			
			// we support api keys in the header
			String apiHeaderKey = input == null ? null : (String) input.get("authentication/apiHeaderKey");
			if (apiHeaderKey == null && override != null) {
				apiHeaderKey = override.getApiHeaderKey();
			}
			if (apiHeaderKey == null) {
				apiHeaderKey = service.getClient().getConfig().getApiHeaderKey();
			}
			if (apiHeaderKey != null) {
				Element<?> element = null;
				if (input != null && input.getType().get("authentication") != null) {
					element = ((ComplexType) input.getType().get("authentication").getType()).get("apiHeaderKey");
				}
				String name = service.getClient().getConfig().getApiHeaderName();
				// the alias is only set if it is defined in the swagger itself
				if (name == null && element != null) {
					Value<String> property = element.getProperty(AliasProperty.getInstance());
					if (property != null) {
						name = property.getValue();
					}
				}
				part.setHeader(new MimeHeader(name == null ? "apiKey" : name, apiHeaderKey));
			}
			
			// we support api keys in the query
			String apiQueryKey = input == null ? null : (String) input.get("authentication/apiQueryKey");
			if (apiQueryKey == null && override != null) {
				apiQueryKey = override.getApiQueryKey();
			}
			if (apiQueryKey == null) {
				apiQueryKey = service.getClient().getConfig().getApiQueryKey();
			}
			if (apiQueryKey != null) {
				Element<?> element = ((ComplexType) input.getType().get("authentication").getType()).get("apiQueryKey");
				String name = service.getClient().getConfig().getApiQueryName();
				// the alias is only set if it is defined in the swagger itself
				if (name == null && element != null) {
					Value<String> property = element.getProperty(AliasProperty.getInstance());
					if (property != null) {
						name = property.getValue();
					}
				}
				if (first) {
					path += "?";
				}
				else {
					path += "&";
				}
				path += (name == null ? "apiKey" : name) + "=" + apiQueryKey;
			}
			
			List<SecurityType> security = override == null ? null : override.getSecurity();
			if (security == null) {
				security = service.getClient().getConfig().getSecurity();
			}
			
			boolean basicAuthenticated = false;
			if (principal != null && security != null && security.contains(SecurityType.basic)) {
				part.setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, new BasicAuthentication().authenticate(principal, "basic")));
				basicAuthenticated = true;
			}
			
			if (service.getMethod().getSecurity() != null && !service.getMethod().getSecurity().isEmpty() 
					&& service.getClient().getDefinition().getSecurityDefinitions() != null && !service.getClient().getDefinition().getSecurityDefinitions().isEmpty() 
					&& principal != null) {
				security: for (SwaggerSecuritySetting setting : service.getMethod().getSecurity()) {
					for (SwaggerSecurityDefinition definition : service.getClient().getDefinition().getSecurityDefinitions()) {
						if (definition.getName().equals(setting.getName())) {
							switch(definition.getType()) {
								case basic:
									if (!basicAuthenticated) {
										part.setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, new BasicAuthentication().authenticate(principal, "basic")));
									}
								break;
								case apiKey:
									// if this was filled in, api key is already done
									if (apiHeaderKey == null && apiQueryKey == null) {
										part.setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, definition.getFieldName() + " " + principal.getName()));
									}
								break;
								default:
									logger.warn("Currently no support for authentication type: " + definition.getType());
							}
							break security;
						}
					}
				}
			}
			// if you explicitly passed in authentication, we use that
			else if (input != null && input.get("authentication/username") != null && !basicAuthenticated) {
				part.setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, new BasicAuthentication().authenticate(principal, "basic")));
			}

			HTTPRequest request = new DefaultHTTPRequest(
				service.getMethod().getMethod() == null ? "GET" : service.getMethod().getMethod().toUpperCase(),
				path,
				part
			);
			
			if (override != null && override.getSecurityType() != null) {
				if (!HTTPRequestAuthenticatorFactory.getInstance().getAuthenticator(override.getSecurityType())
						.authenticate(request, override.getSecurityContext(), null, false)) {
					throw new IllegalStateException("Could not authenticate the request");
				}
			}
			else if (service.getClient().getConfig().getSecurityType() != null) {
				if (!HTTPRequestAuthenticatorFactory.getInstance().getAuthenticator(service.getClient().getConfig().getSecurityType())
						.authenticate(request, service.getClient().getConfig().getSecurityContext(), null, false)) {
					throw new IllegalStateException("Could not authenticate the request");
				}
			}
			else if (service.getClient().getConfig().getEndpoint() != null && service.getClient().getConfig().getEndpoint().getConfig().getSecurityType() != null) {
				if (!HTTPRequestAuthenticatorFactory.getInstance().getAuthenticator(service.getClient().getConfig().getEndpoint().getConfig().getSecurityType())
						.authenticate(request, service.getClient().getConfig().getEndpoint().getConfig().getSecurityContext(), null, false)) {
					throw new IllegalStateException("Could not authenticate the request");
				}
			}
			
			HTTPClientArtifact httpClient = service.getClient().getConfig().getHttpClient();
			if (httpClient == null && service.getClient().getConfig().getEndpoint() != null) {
				httpClient = service.getClient().getConfig().getEndpoint().getConfig().getHttpClient();
			}
			HTTPClient client = Services.getTransactionable(executionContext, input == null ? null : (String) input.get("transactionId"), httpClient).getClient();
			
			boolean secure = override != null && override.getScheme() != null ? override.getScheme().equals("https") : isSecure();
			HTTPResponse response = client.execute(request, principal, secure, true);
			
			SwaggerResponse chosenResponse = null;
			if (service.getMethod().getResponses() != null) {
				SwaggerResponse defaultResponse = null;
				for (SwaggerResponse possibleResponse : service.getMethod().getResponses()) {
					if (possibleResponse.getCode() == null) {
						defaultResponse = possibleResponse;
					}
					else if (possibleResponse.getCode() == response.getCode()) {
						chosenResponse = possibleResponse;
						break;
					}
				}
				if (chosenResponse == null) {
					chosenResponse = defaultResponse;
				}
			}
			
			boolean throwException = override != null && override.getThrowException() != null ? override.getThrowException() : service.getClient().getConfig().isThrowException();

			// if we have a response code out of range and we want to throw an exception, we fail
			boolean fail = (response.getCode() < 200 || response.getCode() >= 300) && throwException;
			// if we are allowed to proceed but we have no chosen response, check if we are lenient
			if (!fail && chosenResponse == null) {
				// if we are lenient AND the response code is 204 (at which point we don't want a response), it is allowed to pass
				// otherwise, we fail
				fail = !(service.getClient().getConfig().isLenient() && response.getCode() == 204);
			}
			// if we are lenient AND the response code is 204, we don't need a chosen response type
			if (fail) {
				byte [] content = null;
				if (response.getContent() instanceof ContentPart) {
					ReadableContainer<ByteBuffer> readable = ((ContentPart) response.getContent()).getReadable();
					if (readable != null) {
						try {
							content = IOUtils.toBytes(readable);
						}
						finally {
							readable.close();
						}
					}
				}
				throw new ServiceException("REST-CLIENT-3", "Invalid response object returned from the server: [" + response.getCode() + "] " + response.getMessage() + (content == null ? "" : "\n" + new String(content)));
			}
			
			ComplexContent output = service.getServiceInterface().getOutputDefinition().newInstance();
			if (response.getContent() != null) {
				// always stream back all the headers...
				output.set("headers", new ArrayList<Header>(Arrays.asList(response.getContent().getHeaders())));
				
				Header contentLength = MimeUtils.getHeader("Content-Length", response.getContent().getHeaders());
				boolean isEmpty = contentLength != null && contentLength.getValue() != null && contentLength.getValue().trim().equals("0");
				String responseContentType = MimeUtils.getContentType(response.getContent().getHeaders());
				if (response.getContent() instanceof ContentPart && chosenResponse != null && chosenResponse.getElement() != null && !isEmpty) {
					if (chosenResponse.getElement().getType() instanceof SimpleType) {
						Object unmarshal;
						if (((SimpleType<?>) chosenResponse.getElement().getType()).getInstanceClass().equals(InputStream.class)) {
							unmarshal = IOUtils.toInputStream(((ContentPart) response.getContent()).getReadable());
						}
						else {
							ReadableContainer<ByteBuffer> readable = ((ContentPart) response.getContent()).getReadable();
							try {
								String content = new String(IOUtils.toBytes(readable), charset);
								// instead of returning the base64 as is with content type "application/octet-stream" or the like
								// some applications send it back with content type "application/json" and wrap the base64 in quotes to indicate a string
								// return of non-object and non-arrays in json is only recently allowed and because it is by definition not a complex type, not directly supported by the binding API
								if (((SimpleType<?>) chosenResponse.getElement().getType()).getInstanceClass().equals(byte[].class) && content.startsWith("\"") && content.endsWith("\"")) {
									content = JSONUnmarshaller.unescape(content.substring(1, content.length() - 1));
								}
								// only numbers and booleans are allowed as "native" types without quotes
								// the rest should have quotes
								else if (!Number.class.isAssignableFrom(((SimpleType<?>) chosenResponse.getElement().getType()).getInstanceClass()) && !Boolean.class.isAssignableFrom(((SimpleType<?>) chosenResponse.getElement().getType()).getInstanceClass())) {
									if (content.startsWith("\"") && content.endsWith("\"")) {
										content = content.substring(1, content.length() - 1);
									}
								}
								unmarshal = ((Unmarshallable<?>) chosenResponse.getElement().getType()).unmarshal(content, chosenResponse.getElement().getProperties());
							}
							finally {
								readable.close();
							}
						}
						output.set(SwaggerProxyInterface.formattedCode(chosenResponse) + "/" + chosenResponse.getElement().getName(), unmarshal);
					}
					else {
						if (responseContentType == null) {
							if (responseType != null) {
								responseContentType = responseType.getMimeType();
							}
						}
						
						boolean wrapped = false;
						ComplexType unmarshalType = null;
						// if the type itself is a list, wrap something around it for parsing
						if (chosenResponse.getElement().getType().isList(chosenResponse.getElement().getProperties())) {
							Structure structure = new Structure();
							structure.add(new ComplexElementImpl("array", (ComplexType) chosenResponse.getElement().getType(), structure));
							unmarshalType = structure;
							wrapped = true;
						}
						else {
							unmarshalType = (ComplexType) chosenResponse.getElement().getType();
						}

						UnmarshallableBinding binding;
						if ("application/x-www-form-urlencoded".equalsIgnoreCase(responseContentType)) {
							binding = new FormBinding(unmarshalType, charset);
						}
						else if ("application/json".equalsIgnoreCase(responseContentType) || "application/javascript".equalsIgnoreCase(responseContentType) || "application/x-javascript".equalsIgnoreCase(responseContentType)
								|| "application/problem+json".equalsIgnoreCase(responseContentType) || (responseContentType != null && responseContentType.matches("application/[\\w]+\\+json"))) {
							JSONBinding jsonBinding = new JSONBinding(unmarshalType, charset);
//							jsonBinding.setIgnoreRootIfArrayWrapper(ValueUtils.contains(MaxOccursProperty.getInstance(), chosenResponse.getElement().getType().getProperties()));
							jsonBinding.setIgnoreRootIfArrayWrapper(true);
							jsonBinding.setAllowRaw(true);
							jsonBinding.setIgnoreEmptyStrings(true);
							// lenient by default
							jsonBinding.setIgnoreUnknownElements(true);
							// also allow dynamic generation by default
							jsonBinding.setAllowDynamicElements(true);
							// but don't add it to the original type
							jsonBinding.setAddDynamicElementDefinitions(false);
							// use structures for dynamic parsing
							jsonBinding.setComplexTypeGenerator(new StructureGenerator());
							binding = jsonBinding;
						}
						else if ("application/xml".equalsIgnoreCase(responseContentType) || "text/xml".equalsIgnoreCase(responseContentType)
								|| "application/problem+xml".equalsIgnoreCase(responseContentType)) {
							XMLBinding xmlBinding = new XMLBinding(unmarshalType, charset);
							// lenient by default
							xmlBinding.setIgnoreUndefined(true);
							binding = xmlBinding;
						}
						else {
							String content = "";
							if (responseContentType != null && responseContentType.startsWith("text/")) {
								ReadableContainer<ByteBuffer> readable = ((ContentPart) response.getContent()).getReadable();
								if (readable != null) {
									try {
										byte[] bytes = IOUtils.toBytes(readable);
										content = "\n" + new String(bytes, Charset.forName("UTF-8"));
									}
									catch (Exception e) {
										logger.warn("Could not serialize response", e);
									}
									finally {
										try {
											readable.close();
										}
										catch (Exception e) {
											logger.warn("Could not close readable", e);
										}
									}
								}
							}
							throw new ServiceException("SWAGGER-5", "Unexpected response content type: " + responseContentType + content);
						}
						
						ComplexContent unmarshal = binding.unmarshal(IOUtils.toInputStream(((ContentPart) response.getContent()).getReadable()), new Window[0]);
						if (service.getClient().getConfig().getSanitizeOutput() != null && service.getClient().getConfig().getSanitizeOutput()) {
							unmarshal = (ComplexContent) GlueListener.sanitize(unmarshal);
						}
						output.set(SwaggerProxyInterface.formattedCode(chosenResponse) + "/" + chosenResponse.getElement().getName(), wrapped ? unmarshal.get("array") : unmarshal);
					}
				}
				
				if (chosenResponse != null && chosenResponse.getHeaders() != null) {
					for (SwaggerParameter header : chosenResponse.getHeaders()) {
						Header responseHeader = MimeUtils.getHeader(header.getName(), response.getContent().getHeaders());
						if (responseHeader != null && responseHeader.getValue() != null) {
							output.set(SwaggerProxyInterface.formattedCode(chosenResponse) + "/headers/" + header.getElement().getName(), responseHeader.getValue());
						}
					}
				}
			}
			return output;
		}
		catch (ServiceException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ServiceException("SWAGGER-CLIENT-4", "Unexpected error occurred", e);
		}
	}

}
