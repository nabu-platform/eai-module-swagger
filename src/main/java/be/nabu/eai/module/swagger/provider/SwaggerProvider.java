package be.nabu.eai.module.swagger.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import be.nabu.eai.module.rest.WebMethod;
import be.nabu.eai.module.rest.provider.RESTService;
import be.nabu.eai.module.rest.provider.iface.RESTInterfaceArtifact;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.artifacts.jaxb.JAXBArtifact;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.authentication.api.Permission;
import be.nabu.libs.events.api.EventHandler;
import be.nabu.libs.events.api.EventSubscription;
import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.http.HTTPException;
import be.nabu.libs.http.api.HTTPRequest;
import be.nabu.libs.http.api.HTTPResponse;
import be.nabu.libs.http.core.DefaultHTTPResponse;
import be.nabu.libs.http.core.HTTPUtils;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerParameter.ParameterLocation;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.api.SwaggerResponse;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;
import be.nabu.libs.swagger.formatter.SwaggerFormatter;
import be.nabu.libs.swagger.parser.SwaggerDefinitionImpl;
import be.nabu.libs.swagger.parser.SwaggerInfoImpl;
import be.nabu.libs.swagger.parser.SwaggerMethodImpl;
import be.nabu.libs.swagger.parser.SwaggerParameterImpl;
import be.nabu.libs.swagger.parser.SwaggerPathImpl;
import be.nabu.libs.swagger.parser.SwaggerResponseImpl;
import be.nabu.libs.swagger.parser.SwaggerSecurityDefinitionImpl;
import be.nabu.libs.swagger.parser.SwaggerSecuritySettingImpl;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeRegistryImpl;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.ModifiableTypeRegistry;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.impl.MimeHeader;
import be.nabu.utils.mime.impl.PlainMimeContentPart;

public class SwaggerProvider extends JAXBArtifact<SwaggerProviderConfiguration> implements WebFragment {

	private Map<String, EventSubscription<?, ?>> subscriptions = new HashMap<String, EventSubscription<?, ?>>();
	private Map<String, String> swaggers = new HashMap<String, String>();
	
	public SwaggerProvider(String id, ResourceContainer<?> directory, Repository repository) {
		super(id, directory, repository, "swagger-provider.xml", SwaggerProviderConfiguration.class);
	}

	@Override
	public void start(WebApplication artifact, String path) throws IOException {
		if (artifact.getConfig().getWebFragments() != null) {
			final String fullPath = getFullPath(artifact, path);
			artifact.getDispatcher().subscribe(HTTPRequest.class, new EventHandler<HTTPRequest, HTTPResponse>() {
				@Override
				public HTTPResponse handle(HTTPRequest request) {
					try {
						URI uri = HTTPUtils.getURI(request, false);
						String uriPath = URIUtils.normalize(uri.getPath());
						if (fullPath.equals(uriPath)) {
							String swagger = getSwagger(artifact, path);
							if (swagger == null) {
								throw new HTTPException(500, "Could not construct swagger");
							}
							byte [] content = swagger.getBytes(Charset.forName("UTF-8"));
							return new DefaultHTTPResponse(200, HTTPCodes.getMessage(200), new PlainMimeContentPart(null, IOUtils.wrap(content, true), 
								new MimeHeader("Content-Length", "" + content.length),
								new MimeHeader("Content-Type", "application/json; charset=utf-8")
							));
						}
						return null;
					}
					catch (Exception e) {
						throw new HTTPException(500, e);
					}
				}
			});
		}
	}
	
	public String getSwagger(WebApplication artifact, String path) {
		String key = getKey(artifact, path);
		if (!swaggers.containsKey(key)) {
			synchronized(this) {
				try {
					if (!swaggers.containsKey(key)) {
						SwaggerDefinitionImpl definition = new SwaggerDefinitionImpl(getId());
						SwaggerInfoImpl info = new SwaggerInfoImpl();
						// title and version are mandatory
						info.setTitle(getConfig().getTitle() == null ? getId() : getConfig().getTitle());
						info.setVersion(getConfig().getVersion() == null ? "1.0" : getConfig().getVersion());
						definition.setInfo(info);
						definition.setRegistry(new TypeRegistryImpl());
						definition.setBasePath(getConfig().getBasePath() == null ? (path == null ? "/" : path) : getConfig().getBasePath());
						definition.setConsumes(Arrays.asList("application/xml", "application/json"));
						definition.setProduces(definition.getConsumes());
						Integer port = artifact.getConfig().getVirtualHost().getConfig().getServer().getConfig().getPort();
						definition.setHost(artifact.getConfig().getVirtualHost().getConfig().getHost() + (port == null ? null : ":" + port));
						boolean isSecure = artifact.getConfig().getVirtualHost().getConfig().getKeyAlias() != null
								&& artifact.getConfig().getVirtualHost().getConfig().getServer().getConfig().getKeystore() != null;
						definition.setSchemes(Arrays.asList(isSecure ? "https" : "http"));
						definition.setVersion("2.0");
						
						if (artifact.getConfig().getPasswordAuthenticationService() != null) {
							SwaggerSecurityDefinitionImpl security = new SwaggerSecurityDefinitionImpl();
							security.setType(SecurityType.basic);
							security.setName("basic");
							definition.setSecurityDefinitions(Arrays.asList(security));
						}
						
						if (artifact.getConfig().getWebFragments() != null) {
							definition.setPaths(analyzePaths((ModifiableTypeRegistry) definition.getRegistry(), artifact.getConfig().getWebFragments(), getRelativePath(artifact.getServerPath(), path)));
						}
						
						ByteArrayOutputStream output = new ByteArrayOutputStream();
						SwaggerFormatter swaggerFormatter = new SwaggerFormatter();
						swaggerFormatter.setExpandInline(true);
						swaggerFormatter.setAllowDefinedTypeReferences(true);
						swaggerFormatter.format(definition, output);
						swaggers.put(key, new String(output.toByteArray(), "UTF-8"));
					}
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return swaggers.get(key);
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<SwaggerPath> analyzePaths(ModifiableTypeRegistry registry, List<WebFragment> fragments, String path) {
		List<SwaggerPath> paths = new ArrayList<SwaggerPath>();
		for (WebFragment fragment : fragments) {
			if (fragment instanceof RESTService) {
				RESTService rest = (RESTService) fragment;
				for (Artifact child : rest.getContainedArtifacts()) {
					if (child instanceof RESTInterfaceArtifact) {
						RESTInterfaceArtifact iface = (RESTInterfaceArtifact) child;
						SwaggerPathImpl swaggerPath = null;
						String fullPath = getRelativePath(path, iface.getConfig().getPath());
						for (SwaggerPath possible : paths) {
							if (possible.getPath().equals(fullPath)) {
								swaggerPath = (SwaggerPathImpl) possible;
								break;
							}
						}
						if (swaggerPath == null) {
							swaggerPath = new SwaggerPathImpl();
							swaggerPath.setPath(fullPath);
							paths.add(swaggerPath);
						}
						
						SwaggerMethodImpl method = new SwaggerMethodImpl();
						method.setMethod(iface.getConfig().getMethod().toString().toLowerCase());
						method.setConsumes(Arrays.asList("application/xml", "application/json"));
						method.setProduces(Arrays.asList("application/xml", "application/json", "text/html"));
						method.setOperationId(rest.getId());
						if (iface.getConfig().getRoles() != null && !iface.getConfig().getRoles().isEmpty() && !iface.getConfig().getRoles().equals(Arrays.asList("guest"))) {
							SwaggerSecuritySettingImpl security = new SwaggerSecuritySettingImpl();
							security.setName("basic");
							security.setScopes(new ArrayList<String>());
							method.setSecurity(Arrays.asList(security));
						}
						List<SwaggerParameter> parameters = new ArrayList<SwaggerParameter>();
						String headerParams = iface.getConfig().getHeaderParameters();
						if (headerParams != null) {
							for (String headerParam : headerParams.split("[\\s]*,[\\s]*")) {
								SwaggerParameterImpl parameter = new SwaggerParameterImpl();
								parameter.setName(headerParam);
								parameter.setLocation(ParameterLocation.HEADER);
								parameter.setElement(new SimpleElementImpl<String>(headerParam, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), null,
									new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
								parameters.add(parameter);
							}
						}
						String queryParams = iface.getConfig().getQueryParameters();
						if (queryParams != null) {
							for (String queryParam : queryParams.split("[\\s]*,[\\s]*")) {
								SwaggerParameterImpl parameter = new SwaggerParameterImpl();
								parameter.setName(queryParam);
								parameter.setLocation(ParameterLocation.QUERY);
								parameter.setElement(new SimpleElementImpl<String>(queryParam, SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), null,
									new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
								parameters.add(parameter);
							}
						}
						if (iface.getConfig().getPath() != null) {
							Pattern pattern = Pattern.compile("\\{([^}]+)\\}");
							Matcher matcher = pattern.matcher(iface.getConfig().getPath());
							while (matcher.find()) {
								SwaggerParameterImpl parameter = new SwaggerParameterImpl();
								parameter.setName(matcher.group(1).replaceAll("[\\s]*:.*$", ""));
								parameter.setLocation(ParameterLocation.PATH);
								parameter.setElement(new SimpleElementImpl<String>(parameter.getName(), SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), null));
								parameters.add(parameter);
							}
						}
						if (iface.getConfig().getInputAsStream() != null && iface.getConfig().getInputAsStream()) {
							SwaggerParameterImpl parameter = new SwaggerParameterImpl();
							parameter.setName("body");
							parameter.setLocation(ParameterLocation.BODY);
							SimpleType<?> bytesType = registry.getSimpleType(getId(), "byte");
							if (bytesType == null) {
								bytesType = new SimpleTypeWrapper<byte[]>((Marshallable<byte[]>) SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(byte[].class), getId(), "byte");
								registry.register(bytesType);
							}
							parameter.setElement(new SimpleElementImpl("body", bytesType, null));
							parameters.add(parameter);
						}
						else if (iface.getConfig().getInput() != null) {
							SwaggerParameterImpl parameter = new SwaggerParameterImpl();
							parameter.setName("body");
							parameter.setLocation(ParameterLocation.BODY);
							ComplexType complexType = registry.getComplexType(getId(), iface.getConfig().getInput().getId());
							if (complexType == null) {
								complexType = new ComplexTypeWrapper((ComplexType) iface.getConfig().getInput(), getId(), iface.getConfig().getInput().getId());
								registry.register(complexType);
							}
							parameter.setElement(new ComplexElementImpl("body", complexType, (ComplexType) null));
							parameters.add(parameter);
						}
						method.setParameters(parameters);
						
						List<SwaggerResponse> responses = new ArrayList<SwaggerResponse>();
						
						// if we have security, we can send back a 401 and 403
						if (method.getSecurity() != null) {
							SwaggerResponseImpl c401 = new SwaggerResponseImpl();
							c401.setCode(401);
							c401.setDescription("The user is not authenticated, after authentication the request can be retried");
							responses.add(c401);
							
							SwaggerResponseImpl c403 = new SwaggerResponseImpl();
							c403.setCode(403);
							c403.setDescription("The user is authenticated but does not have permission to run this service");
							responses.add(c403);
						}
						
						// there is some content
						if ((iface.getConfig().getOutputAsStream() != null && iface.getConfig().getOutputAsStream()) || iface.getConfig().getOutput() != null) {
							SwaggerResponseImpl c200 = new SwaggerResponseImpl();
							c200.setCode(200);
							c200.setDescription("The request was successful");
							if (iface.getConfig().getOutputAsStream() != null && iface.getConfig().getOutputAsStream()) {
								SimpleType<?> bytesType = registry.getSimpleType(getId(), "byte");
								if (bytesType == null) {
									bytesType = new SimpleTypeWrapper<byte[]>((Marshallable<byte[]>) SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(byte[].class), getId(), "byte");
									registry.register(bytesType);
								}
								c200.setElement(new SimpleElementImpl("body", bytesType, null));
							}
							else if (iface.getConfig().getOutput() != null) {
								ComplexType complexType = registry.getComplexType(getId(), iface.getConfig().getOutput().getId());
								if (complexType == null) {
									complexType = new ComplexTypeWrapper((ComplexType) iface.getConfig().getOutput(), getId(), iface.getConfig().getOutput().getId());
									registry.register(complexType);
								}
								c200.setElement(new ComplexElementImpl("body", complexType, (ComplexType) null));
							}
							responses.add(c200);
						}
						
						// every response can lead to an empty return (theoretically)
						SwaggerResponseImpl c204 = new SwaggerResponseImpl();
						c204.setCode(204);
						c204.setDescription("The request was successful but no content will be returned");
						responses.add(c204);
						
						// if it is a get, we can return a 304
						if (iface.getConfig().getMethod() == WebMethod.GET) {
							SwaggerResponseImpl c304 = new SwaggerResponseImpl();
							c304.setCode(304);
							c304.setDescription("The resource has not changed since the last modified");
							responses.add(c304);
						}
						
						method.setResponses(responses);
						
						// TODO: make sure we recurse if is a web component
						
						if (swaggerPath.getMethods() == null) {
							swaggerPath.setMethods(new ArrayList<SwaggerMethod>());
						}
						swaggerPath.getMethods().add(method);
					}
				}
			}
		}
		return paths;
	}
	
	@Override
	public void stop(WebApplication artifact, String path) {
		String key = getKey(artifact, path);
		if (subscriptions.containsKey(key)) {
			synchronized(subscriptions) {
				if (subscriptions.containsKey(key)) {
					subscriptions.get(key).unsubscribe();
					subscriptions.remove(key);
				}
			}
		}
	}

	@Override
	public List<Permission> getPermissions(WebApplication artifact, String path) {
		return new ArrayList<Permission>();
	}

	@Override
	public boolean isStarted(WebApplication artifact, String path) {
		return subscriptions.containsKey(getKey(artifact, path));
	}

	private String getKey(WebApplication artifact, String path) {
		return artifact.getId() + ":" + path;
	}
	
	String getFullPath(WebApplication artifact, String path) {
		try {
			String parentPath = getRelativePath(artifact.getServerPath(), path);
			return getRelativePath(parentPath, getConfig().getPath() == null ? "swagger.json" : getConfig().getPath());
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private String getRelativePath(String root, String path) {
		if (root == null) {
			root = "/";
		}
		if (path != null && !path.isEmpty() && !path.equals("/")) {
			if (!root.endsWith("/")) {
				root += "/";
			}
			root += path.replaceFirst("^[/]+", "");
		}
		return root.replace("//", "/");
	}
}
