package be.nabu.eai.module.swagger.client;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.ArtifactGUIInstance;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.libs.validator.api.ValidationMessage.Severity;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;

public class SwaggerClientMenu implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(Entry entry) {
		if (entry.isNode() && SwaggerClient.class.isAssignableFrom(entry.getNode().getArtifactClass()) && entry instanceof ResourceEntry) {
			MenuItem item = new MenuItem("Limit to used services");
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					MainController.getInstance().open(entry.getId());
					// for now the lock is synchronous, so run immediately (timeout 0)
					MainController.getInstance().runIn(new Runnable() {
						public void run() {
							try {
								if (MainController.getInstance().hasLock(entry.getId()).get()) {
									ArtifactGUIInstance artifactInstance = MainController.getInstance().getArtifactInstance(entry.getId());
									// if i resolve it from the node at this point, i get an out of sync version
									SwaggerClient artifact = (SwaggerClient) artifactInstance.getArtifact();
									List<String> operationIds = new ArrayList<String>();
									for (SwaggerService service : artifact.getRepository().getArtifacts(SwaggerService.class)) {
										// don't check on instance, just id (to prevent reload problems)
										if (service.getClient().getId().equals(artifact.getId())) {
											List<String> dependencies = artifact.getRepository().getDependencies(service.getId());
											// if we have dependencies, we are using it
											if (dependencies != null && !dependencies.isEmpty()) {
												operationIds.add(service.getMethod().getOperationId());
											}
										}
									}
									artifact.getConfig().setOperationIds(operationIds);
									artifact.getConfig().setExposeAllServices(false);
									MainController.getInstance().redraw(artifact.getId());
									MainController.getInstance().setChanged(artifact.getId());
								}
								else {
									MainController.getInstance().showNotification(Severity.WARNING, "Could not lock swagger client", "Can't update the swagger because the lock can not be obtained");
								}
							}
							catch (Exception e) {
								MainController.getInstance().notify(e);
							}
						}
					}, 0);
				}
			});
			return item;
		}
		return null;
	}

}
