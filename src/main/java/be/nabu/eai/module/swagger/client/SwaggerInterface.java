package be.nabu.eai.module.swagger.client;

import be.nabu.libs.http.HTTPCodes;
import be.nabu.libs.services.api.DefinedServiceInterface;
import be.nabu.libs.services.api.ServiceInterface;
import be.nabu.libs.swagger.SwaggerDefinitionImpl;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.api.SwaggerResponse;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class SwaggerInterface implements DefinedServiceInterface {

	private SwaggerMethod method;
	private SwaggerDefinition definition;
	private SwaggerPath path;
	private String id;

	private Structure input, output;
	
	public SwaggerInterface(String id, SwaggerDefinition definition, SwaggerPath path, SwaggerMethod method) {
		this.id = id;
		this.definition = definition;
		this.path = path;
		this.method = method;
		
		initializeInput();
		initializeOutput();
	}
	
	private void initializeInput() {
		Structure input = new Structure();
		input.setName("input");

		if (method.getParameters() != null) {
			for (SwaggerParameter parameter : method.getParameters()) {
				if (parameter.getElement() != null) {
					input.add(parameter.getElement());
				}
			}
		}
		
		this.input = input;
	}
	
	public static String formattedCode(SwaggerResponse response) {
		String name = response.getCode() == null ? "default" : SwaggerDefinitionImpl.cleanup(HTTPCodes.getMessage(response.getCode()));
		String leadingCapital = name.replaceAll("(^[A-Z]+).*", "$1");
		if (!leadingCapital.isEmpty()) {
			name = name.replaceFirst("^" + leadingCapital, leadingCapital.toLowerCase());
		}
		return name;
	}
	
	private void initializeOutput() {
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
			
		this.output = output;
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
