package be.nabu.eai.module.swagger.client;

import java.util.List;

import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerParameter.ParameterLocation;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.api.SwaggerResponse;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;
import be.nabu.libs.swagger.api.SwaggerSecuritySetting;
import be.nabu.libs.swagger.parser.SwaggerParser;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.java.BeanResolver;
import be.nabu.libs.types.properties.AliasProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;
import be.nabu.utils.mime.api.Header;

public class SwaggerProxyInterface implements DefinedServiceInterface {

	private SwaggerMethod method;
	private SwaggerDefinition definition;
	private SwaggerPath path;
	private String id;

	private Structure input, output;
	private List<SecurityType> security;
	
	public SwaggerProxyInterface(String id, SwaggerDefinition definition, SwaggerPath path, SwaggerMethod method, List<SecurityType> security) {
		this.id = id;
		this.definition = definition;
		this.path = path;
		this.method = method;
		this.security = security;
		
		this.input = initializeInput();
		this.output = initializeOutput();
	}
	
	protected Structure initializeInput() {
		Structure input = new Structure();
		input.setName("input");

		// TODO: need to put parameters in subsection
		// we need to check the security for this rest service and expose whatever is needed (e.g. api key, username/password, oauth2 token...)
		if (method.getParameters() != null) {
			Structure parameters = new Structure();
			parameters.setName("parameters");
			for (SwaggerParameter parameter : method.getParameters()) {
				if (parameter.getElement() != null) {
					parameters.add(parameter.getElement());
				}
			}
			input.add(new ComplexElementImpl("parameters", parameters, input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
		}
		if (security != null) {
			Structure authentication = new Structure();
			authentication.setName("authentication");
			for (SecurityType type : security) {
				// the api key should not be used unless we add some more configuration options but for now this will do...
				addAuthentication(authentication, "apiKey", ParameterLocation.HEADER, type);
			}
			input.add(new ComplexElementImpl("authentication", authentication, input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
		}
		else if (method.getSecurity() != null && definition.getSecurityDefinitions() != null) {
			Structure authentication = new Structure();
			authentication.setName("authentication");
			for (SwaggerSecuritySetting setting : method.getSecurity()) {
				for (SwaggerSecurityDefinition securityDefinition : definition.getSecurityDefinitions()) {
					if (securityDefinition.getName().equals(setting.getName())) {
						addAuthentication(authentication, setting.getName(), securityDefinition.getLocation(), securityDefinition.getType());
					}
				}
			}
			input.add(new ComplexElementImpl("authentication", authentication, input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
		}
		
		input.add(new ComplexElementImpl("headers", (ComplexType) BeanResolver.getInstance().resolve(Header.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0), new ValueImpl<Integer>(MaxOccursProperty.getInstance(), 0)));
		
		return input;
	}

	private void addAuthentication(Structure authentication, String apiKeyName, ParameterLocation apiKeyLocation, SecurityType securityType) {
		switch(securityType) {
			case apiKey:
				// the api key can appear in both the header and the query
				if (apiKeyLocation == null || apiKeyLocation.equals(ParameterLocation.HEADER)) {
					authentication.add(new SimpleElementImpl<String>("apiHeaderKey", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication, new ValueImpl<String>(AliasProperty.getInstance(), apiKeyName)));
				}
				else {
					authentication.add(new SimpleElementImpl<String>("apiQueryKey", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication, new ValueImpl<String>(AliasProperty.getInstance(), apiKeyName)));
				}
			break;
			case basic:
				authentication.add(new SimpleElementImpl<String>("username", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication));
				authentication.add(new SimpleElementImpl<String>("password", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication));
			break;
			case oauth2:
				authentication.add(new SimpleElementImpl<String>("oauth2Token", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), authentication));
			break;
		}
	}
	
	public static String formattedCode(SwaggerResponse response) {
		String name = response.getCode() == null ? "default" : SwaggerParser.cleanup(HTTPCodes.getMessage(response.getCode()));
		String leadingCapital = name.replaceAll("(^[A-Z]+).*", "$1");
		if (!leadingCapital.isEmpty()) {
			name = name.replaceFirst("^" + leadingCapital, leadingCapital.toLowerCase());
		}
		return name;
	}
	
	protected Structure initializeOutput() {
		Structure output = new Structure();
		output.setName("output");
		
		if (method.getResponses() != null) {
			for (SwaggerResponse response : method.getResponses()) {
				String responseName = formattedCode(response);
				Structure responses = new Structure();
				responses.setName(responseName);
				if (response.getElement() != null) {
					responses.add(response.getElement());
				}
				if (response.getHeaders() != null) {
					Structure headers = new Structure();
					headers.setName("headers");
					for (SwaggerParameter parameter : response.getHeaders()) {
						headers.add(parameter.getElement());
					}
					responses.add(new ComplexElementImpl("headers", headers, responses, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
				}
				output.add(new ComplexElementImpl(responseName, responses, output, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
			}
		}

		return output;
	}
	
	@Override
	public ComplexType getInputDefinition() {
		return input;
	}

	@Override
	public ComplexType getOutputDefinition() {
		return output;
	}

	@Override
	public ServiceInterface getParent() {
		return null;
	}

	public SwaggerMethod getMethod() {
		return method;
	}

	public SwaggerDefinition getDefinition() {
		return definition;
	}

	public SwaggerPath getPath() {
		return path;
	}

	@Override
	public String getId() {
		return id;
	}
}
