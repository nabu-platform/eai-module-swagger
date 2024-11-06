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

package nabu.web.swagger;

import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import be.nabu.eai.module.swagger.provider.SwaggerProvider;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.swagger.formatter.SwaggerFormatter;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.api.DefinedType;

@WebService
public class Services {
	
	public void resetCache(@WebParam(name = "swaggerProviderId") String swaggerProviderId) {
		if (swaggerProviderId != null) {
			Artifact resolve = EAIResourceRepository.getInstance().resolve(swaggerProviderId);
			if (resolve instanceof SwaggerProvider) {
				((SwaggerProvider) resolve).refresh();
			}
			else {
				throw new IllegalArgumentException("Not a swagger provider: " + swaggerProviderId);
			}
		}
	}
	
	@WebResult(name = "json")
	public String formatAsJsonSchema(@WebParam(name = "typeId") String typeId) {
		DefinedType resolved = DefinedTypeResolverFactory.getInstance().getResolver().resolve(typeId);
		if (resolved == null) {
			throw new IllegalArgumentException("Not a type: " + typeId);
		}
		return SwaggerFormatter.formatTypeAsJSON(resolved);	
	}
	
}
