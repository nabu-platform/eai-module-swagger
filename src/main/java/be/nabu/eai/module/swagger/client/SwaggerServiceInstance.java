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

import nabu.protocols.http.client.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.nabu.eai.module.rest.WebResponseType;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.authentication.api.principals.BasicPrincipal;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.api.client.HTTPClient;
import be.nabu.libs.http.client.BasicAuthentication;
import be.nabu.libs.http.client.NTLMPrincipalImpl;
import be.nabu.libs.http.core.DefaultHTTPRequest;
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
import be.nabu.libs.types.ComplexContentWrapperFactory;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.binding.api.MarshallableBinding;
import be.nabu.libs.types.binding.api.UnmarshallableBinding;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.form.FormBinding;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.binding.json.JSONUnmarshaller;
import be.nabu.libs.types.binding.xml.XMLBinding;
import be.nabu.libs.types.properties.AliasProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.structure.Structure;
import be.nabu.libs.types.structure.StructureGenerator;
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
		return schemes != null && !schemes.isEmpty() && schemes.contains("https");
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public ComplexContent execute(ExecutionContext executionContext, ComplexContent input) throws ServiceException {
		try {
			// runtime wins
			String host = input == null ? null : (String) input.get("host");
			if (host == null) {
				// otherwise something you explicitly configured
				host = service.getClient().getConfig().getHost();
				if (host == null) {
					// otherwise the default provided host
					host = service.getClient().getDefinition().getHost();
				}
			}
			if (host == null) {
				throw new ServiceException("SWAGGER-CLIENT-1", "No host found for: " + service.getId());
			}
			
			String basePath = input == null ? null : (String) input.get("basePath");
			if (basePath == null) {
				basePath = service.getClient().getConfig().getBasePath();
				if (basePath == null) {
					basePath = service.getClient().getDefinition().getBasePath();
				}
			}
			if (basePath == null) {
				throw new ServiceException("SWAGGER-CLIENT-2", "No basePath configured for: " + service.getId());
			}

			Charset charset = service.getClient().getConfig().getCharset();
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
									else if (parameter.getElement().getType().isList()) {
										throw new RuntimeException("Array inputs are not yet supported");
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
									if ("Content-Type".equalsIgnoreCase(parameter.getName())) {
										contentType = value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class);
									}
									else {
										additionalHeaders.add(new MimeHeader(parameter.getName(), value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class)));
									}
								break;
								case PATH:
									path = path.replaceAll("\\{[\\s]*" + Pattern.quote(parameter.getName()) + "\\b[^}]*\\}",
										URIUtils.encodeURIComponent(Matcher.quoteReplacement(value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class)), false));
								break;
								case FORMDATA:
									if (content != null) {
										throw new ServiceException("SWAGGER-1", "Input parameters of type 'body' and 'formData' can not co-exist in the same operation");
									}
								case QUERY:
									if (value instanceof Iterable) {
										StringBuilder builder = new StringBuilder();
										boolean first = true;
										CollectionFormat format = parameter.getCollectionFormat();
										if (format == null) {
											format = CollectionFormat.CSV;
										}
										if (format == CollectionFormat.MULTI) {
											List<String> values = new ArrayList<String>();
											for (Object child : (Iterable<?>) value) {
												values.add(child instanceof String ? (String) child : ConverterFactory.getInstance().getConverter().convert(child, String.class));
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
												if (first) {
													first = false;
												}
												else {
													builder.append(format.getCharacter());
												}
												builder.append(child instanceof String ? (String) child : ConverterFactory.getInstance().getConverter().convert(child, String.class));
											}
											// nothing in iterable
											if (first) {
												continue;
											}
											if (parameter.getLocation() == ParameterLocation.FORMDATA) {
												formParameters.put(parameter.getName(), Arrays.asList(builder.toString()));
											}
											else {
												queryParameters.put(parameter.getName(), Arrays.asList(builder.toString()));
											}
										}
									}
									else if (parameter.getLocation() == ParameterLocation.FORMDATA) {
										formParameters.put(parameter.getName(), Arrays.asList(value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class)));
									}
									else {
										queryParameters.put(parameter.getName(), Arrays.asList(value instanceof String ? (String) value : ConverterFactory.getInstance().getConverter().convert(value, String.class)));
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
				part.setHeader(new MimeHeader("Content-Encoding", "gzip"));
				part.setHeader(new MimeHeader("Accept-Encoding", "gzip"));
			}
			part.setHeader(new MimeHeader("Host", host));
			
			// github requires a user agent for its api access...
			if (service.getClient().getConfig().getUserAgent() != null) {
				part.setHeader(new MimeHeader("User-Agent", service.getClient().getConfig().getUserAgent()));
			}
			
			final String oauth2Token = input == null ? null : (String) input.get("authentication/oauth2Token");
			if (oauth2Token != null) {
				part.setHeader(new MimeHeader("Authorization", "Bearer " + oauth2Token));
			}

			final String apiHeaderKey = input == null ? null : (String) input.get("authentication/apiHeaderKey");
			if (apiHeaderKey != null) {
				Element<?> element = ((ComplexType) input.getType().get("authentication").getType()).get("apiHeaderKey");
				Value<String> property = element.getProperty(AliasProperty.getInstance());
				String name = property == null ? "apiKey" : property.getValue();
				part.setHeader(new MimeHeader(name, apiHeaderKey));
			}
			
			final String username = input == null || input.get("authentication/username") == null ? service.getClient().getConfig().getUsername() : (String) input.get("authentication/username");
			final String password = input == null || input.get("authentication/password") == null ? service.getClient().getConfig().getPassword() : (String) input.get("authentication/password");

			BasicPrincipal principal = null;
			if (username != null) {
				int index = username.indexOf('/');
				if (index < 0) {
					index = username.indexOf('\\');
				}
				if (index < 0) {
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

			final String apiQueryKey = input == null ? null : (String) input.get("authentication/apiQueryKey");
			if (apiQueryKey != null) {
				Element<?> element = ((ComplexType) input.getType().get("authentication").getType()).get("apiQueryKey");
				Value<String> property = element.getProperty(AliasProperty.getInstance());
				String name = property == null ? "apiKey" : property.getValue();
				if (first) {
					path += "?";
				}
				else {
					path += "&";
				}
				path += name + "=" + apiQueryKey;
			}
			
			HTTPRequest request = new DefaultHTTPRequest(
				service.getMethod().getMethod() == null ? "GET" : service.getMethod().getMethod().toUpperCase(),
				path,
				part
			);
			
			if (service.getMethod().getSecurity() != null && service.getClient().getDefinition().getSecurityDefinitions() != null && principal != null) {
				security: for (SwaggerSecuritySetting setting : service.getMethod().getSecurity()) {
					for (SwaggerSecurityDefinition definition : service.getClient().getDefinition().getSecurityDefinitions()) {
						if (definition.getName().equals(setting.getName())) {
							switch(definition.getType()) {
								case basic:
									request.getContent().setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, new BasicAuthentication().authenticate(principal, "basic")));
								break;
								case apiKey:
									request.getContent().setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, definition.getFieldName() + " " + principal.getName()));
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
			else if (input != null && input.get("authentication/username") != null) {
				request.getContent().setHeader(new MimeHeader(HTTPUtils.SERVER_AUTHENTICATE_RESPONSE, new BasicAuthentication().authenticate(principal, "basic")));
			}

			HTTPClient client = Services.getTransactionable(executionContext, input == null ? null : (String) input.get("transactionId"), service.getClient().getConfig().getHttpClient()).getClient();
			HTTPResponse response = client.execute(request, principal, isSecure(), true);
			
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
			
			if (chosenResponse == null || ((response.getCode() < 200 || response.getCode() >= 300) && service.getClient().getConfig().isThrowException())) {
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
				
				String responseContentType = MimeUtils.getContentType(response.getContent().getHeaders());
				if (response.getContent() instanceof ContentPart && chosenResponse.getElement() != null) {
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
						else if ("application/json".equalsIgnoreCase(responseContentType) || "application/javascript".equalsIgnoreCase(responseContentType) || "application/x-javascript".equalsIgnoreCase(responseContentType)) {
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
						else if ("application/xml".equalsIgnoreCase(responseContentType) || "text/xml".equalsIgnoreCase(responseContentType)) {
							XMLBinding xmlBinding = new XMLBinding(unmarshalType, charset);
							// lenient by default
							xmlBinding.setIgnoreUndefined(true);
							binding = xmlBinding;
						}
						else {
							throw new ServiceException("SWAGGER-5", "Unexpected response content type: " + responseContentType);
						}
						
						ComplexContent unmarshal = binding.unmarshal(IOUtils.toInputStream(((ContentPart) response.getContent()).getReadable()), new Window[0]);
						if (service.getClient().getConfig().getSanitizeOutput() != null && service.getClient().getConfig().getSanitizeOutput()) {
							unmarshal = (ComplexContent) GlueListener.sanitize(unmarshal);
						}
						output.set(SwaggerProxyInterface.formattedCode(chosenResponse) + "/" + chosenResponse.getElement().getName(), wrapped ? unmarshal.get("array") : unmarshal);
					}
				}
				
				if (chosenResponse.getHeaders() != null) {
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
		catch (Exception e) {
			throw new ServiceException(e);
		}
	}

}
