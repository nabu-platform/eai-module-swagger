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
