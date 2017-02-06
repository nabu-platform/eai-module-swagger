package be.nabu.eai.module.swagger.provider;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.repository.api.Entry;

public class SwaggerContextMenuProvider implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(Entry entry) {
		if (entry.isNode() && SwaggerProvider.class.isAssignableFrom(entry.getNode().getArtifactClass())) {
			MenuItem item = new MenuItem("Reset swagger cache");
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						((SwaggerProvider) entry.getNode().getArtifact()).refresh();
					}
					catch (Exception e) {
						MainController.getInstance().notify(e);
					}
				}
			});
			return item;
		}
		return null;
	}

}
