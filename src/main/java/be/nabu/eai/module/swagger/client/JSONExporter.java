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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.repository.api.Entry;
import be.nabu.libs.swagger.formatter.SwaggerFormatter;
import be.nabu.libs.types.api.ComplexType;

public class JSONExporter implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(final Entry entry) {
		if (entry.isNode() && ComplexType.class.isAssignableFrom(entry.getNode().getArtifactClass())) {
			MenuItem item = new MenuItem("Export as JSON Schema");
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					try {
						ComplexType complex = (ComplexType) entry.getNode().getArtifact();
						String formatTypeAsJSON = SwaggerFormatter.formatTypeAsJSON(complex);
						Confirm.confirm(ConfirmType.INFORMATION, "XML Schema: " + entry.getId(), formatTypeAsJSON, null);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
			return item;
		}
		return null;
	}
		
}
