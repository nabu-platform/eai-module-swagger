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

package be.nabu.eai.module.swagger.provider;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import be.nabu.eai.api.Advanced;
import be.nabu.eai.api.Comment;
import be.nabu.eai.api.LargeText;
import be.nabu.eai.repository.jaxb.ArtifactXMLAdapter;
import be.nabu.libs.types.api.DefinedType;

@XmlRootElement(name = "swaggerProvider")
@XmlType(propOrder = { "version", "basePath", "termsOfService", "path", "host", "scheme", "includeAll", "documentedRoles", "allowedRoles", "limitToUser", "additionalTypes" })
public class SwaggerProviderConfiguration {

	private String path, termsOfService, version, basePath, host;
	private Scheme scheme;
	private boolean includeAll;
	private List<String> documentedRoles, allowedRoles;
	// if we set limit to user, we automatically set it to variable
	private boolean limitToUser;
	
	// we can expose additional types
	private List<DefinedType> additionalTypes;

	@Advanced
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getBasePath() {
		return basePath;
	}
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}
		
	@LargeText
	public String getTermsOfService() {
		return termsOfService;
	}
	public void setTermsOfService(String termsOfService) {
		this.termsOfService = termsOfService;
	}

	@Advanced
	@Comment(title = "Include everything in the application?", description = "By default the swagger provider will only include items from the parent it is in and any children. You can however document the whole web application if you set this to true")
	public boolean isIncludeAll() {
		return includeAll;
	}
	public void setIncludeAll(boolean includeAll) {
		this.includeAll = includeAll;
	}
	
	@Advanced
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	@Advanced
	public Scheme getScheme() {
		return scheme;
	}
	public void setScheme(Scheme scheme) {
		this.scheme = scheme;
	}

	public enum Scheme {
		HTTP, HTTPS
	}

	@Comment(title = "If any roles are set here, it will only document fragments belonging to those roles")
	public List<String> getDocumentedRoles() {
		return documentedRoles;
	}
	public void setDocumentedRoles(List<String> documentedRoles) {
		this.documentedRoles = documentedRoles;
	}
	
	@Comment(title = "The roles that are allowed to request this swagger")
	public List<String> getAllowedRoles() {
		return allowedRoles;
	}
	public void setAllowedRoles(List<String> allowedRoles) {
		this.allowedRoles = allowedRoles;
	}
	
	@Comment(title = "If set to true, the swagger will only contain operations that the requesting user can perform")
	public boolean isLimitToUser() {
		return limitToUser;
	}
	public void setLimitToUser(boolean limitToUser) {
		this.limitToUser = limitToUser;
	}

	@XmlJavaTypeAdapter(value = ArtifactXMLAdapter.class)
	public List<DefinedType> getAdditionalTypes() {
		return additionalTypes;
	}
	public void setAdditionalTypes(List<DefinedType> additionalTypes) {
		this.additionalTypes = additionalTypes;
	}
	
}
