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

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Group;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.validator.api.Validator;

public class ComplexTypeWrapper implements ComplexType, DefinedType {

	private ComplexType parent;
	private String namespace;
	private String name;

	public ComplexTypeWrapper(ComplexType parent, String namespace, String name) {
		this.parent = parent;
		this.namespace = namespace;
		this.name = name;
	}
	
	@Override
	public String getName(Value<?>... values) {
		return name;
	}

	@Override
	public String getNamespace(Value<?>... values) {
		return namespace;
	}

	@Override
	public boolean isList(Value<?>... properties) {
		return parent.isList(properties);
	}

	@Override
	public Validator<?> createValidator(Value<?>... properties) {
		return parent.createValidator(properties);
	}

	@Override
	public Validator<Collection<?>> createCollectionValidator(Value<?>... properties) {
		return parent.createCollectionValidator(properties);
	}

	@Override
	public Set<Property<?>> getSupportedProperties(Value<?>... properties) {
		return parent.getSupportedProperties(properties);
	}

	@Override
	public Type getSuperType() {
		return parent.getSuperType();
	}

	@Override
	public Value<?>[] getProperties() {
		return parent.getProperties();
	}

	@Override
	public Iterator<Element<?>> iterator() {
		return parent.iterator();
	}

	@Override
	public Element<?> get(String path) {
		return parent.get(path);
	}

	@Override
	public ComplexContent newInstance() {
		return parent.newInstance();
	}

	@Override
	public Boolean isAttributeQualified(Value<?>... values) {
		return parent.isAttributeQualified(values);
	}

	@Override
	public Boolean isElementQualified(Value<?>... values) {
		return parent.isElementQualified(values);
	}

	@Override
	public Group[] getGroups() {
		return parent.getGroups();
	}

	@Override
	public String getId() {
		return ((DefinedType) parent).getId();
	}

}
