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
