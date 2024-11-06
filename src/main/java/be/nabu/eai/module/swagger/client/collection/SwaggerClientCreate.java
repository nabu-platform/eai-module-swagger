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

package be.nabu.eai.module.swagger.client.collection;

import be.nabu.libs.types.api.annotation.ComplexTypeDescriptor;
import be.nabu.libs.types.api.annotation.Field;

@ComplexTypeDescriptor(propOrder = {"name", "uri"})
public class SwaggerClientCreate {
	
	private String name, uri;
	
	@Field(minOccurs = 1)
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Field(comment = "This should point to a 'swagger.json' file, either on a publically available HTTP endpoint or a local file path.", minOccurs = 1)
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
}
