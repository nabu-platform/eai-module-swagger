package be.nabu.eai.module.swagger.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import be.nabu.eai.developer.api.InterfaceLister;
import be.nabu.eai.developer.util.InterfaceDescriptionImpl;

public class SwaggerClientInterfaceLister implements InterfaceLister {
	
	private static Collection<InterfaceDescription> descriptions = null;
	
	@Override
	public Collection<InterfaceDescription> getInterfaces() {
		if (descriptions == null) {
			synchronized(SwaggerClientInterfaceLister.class) {
				if (descriptions == null) {
					List<InterfaceDescription> descriptions = new ArrayList<InterfaceDescription>();
					descriptions.add(new InterfaceDescriptionImpl("Swagger", "Swagger Override Provider", "be.nabu.eai.module.swagger.client.api.SwaggerOverrideProvider.overrideFor"));
					SwaggerClientInterfaceLister.descriptions = descriptions;
				}
			}
		}
		return descriptions;
	}

}
