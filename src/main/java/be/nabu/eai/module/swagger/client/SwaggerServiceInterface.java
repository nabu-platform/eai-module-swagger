/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.module.swagger.client;

import java.util.List;

import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.base.Scope;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.properties.ScopeProperty;
import be.nabu.libs.types.structure.Structure;

public class SwaggerServiceInterface extends SwaggerProxyInterface {

	public SwaggerServiceInterface(String id, SwaggerDefinition definition, SwaggerPath path, SwaggerMethod method, List<SecurityType> securityType) {
		super(id, definition, path, method, securityType);
	}

	@Override
	protected Structure initializeInput() {
		Structure input = super.initializeInput();
		// allow dynamic overriding of host at runtime
		input.add(new SimpleElementImpl<String>("host", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0),
			new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
		// allow dynamic overriding of the basepath
		input.add(new SimpleElementImpl<String>("basePath", SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class), input, new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0),
			new ValueImpl<Scope>(ScopeProperty.getInstance(), Scope.PRIVATE)));
		return input;
	}

	@Override
	protected Structure initializeOutput() {
		Structure output = super.initializeOutput();
		// anything here?
		return output;
	}

	
}
