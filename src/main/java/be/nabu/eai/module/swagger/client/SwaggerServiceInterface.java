package be.nabu.eai.module.swagger.client;

import java.util.List;

import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.structure.Structure;

public class SwaggerServiceInterface extends SwaggerProxyInterface {

	public SwaggerServiceInterface(String id, SwaggerDefinition definition, SwaggerPath path, SwaggerMethod method, List<SecurityType> securityType) {
		super(id, definition, path, method, securityType);
	}

	@Override
	protected Structure initializeInput() {
		Structure input = super.initializeInput();
		// allow dynamic overriding of host at runtime
		input.add(new SimpleElementImpl<String>("host", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
		// allow dynamic overriding of the basepath
		input.add(new SimpleElementImpl<String>("basePath", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0)));
		return input;
	}

	@Override
	protected Structure initializeOutput() {
		Structure output = super.initializeOutput();
		// anything here?
		return output;
	}

	
}
