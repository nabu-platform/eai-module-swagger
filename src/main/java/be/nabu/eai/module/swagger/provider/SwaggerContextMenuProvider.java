package be.nabu.eai.module.swagger.provider;

import java.util.concurrent.Future;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.developer.util.Confirm;
import be.nabu.eai.developer.util.Confirm.ConfirmType;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.util.SystemPrincipal;
import be.nabu.libs.services.api.Service;
import be.nabu.libs.services.api.ServiceResult;
import be.nabu.libs.types.api.ComplexContent;

public class SwaggerContextMenuProvider implements EntryContextMenuProvider {

	@Override
	public MenuItem getContext(Entry entry) {
		if (entry.isNode() && SwaggerProvider.class.isAssignableFrom(entry.getNode().getArtifactClass())) {
			MenuItem item = new MenuItem("Reset swagger cache");
			item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					try {
						Service service = (Service) EAIResourceRepository.getInstance().resolve("nabu.web.swagger.Services.resetCache");
						if (service != null) {
							ComplexContent input = service.getServiceInterface().getInputDefinition().newInstance();
							input.set("swaggerProviderId", entry.getId());
							Future<ServiceResult> run = EAIResourceRepository.getInstance().getServiceRunner().run(service, EAIResourceRepository.getInstance().newExecutionContext(SystemPrincipal.ROOT), input);
							ServiceResult serviceResult = run.get();
							if (serviceResult.getException() != null) {
								MainController.getInstance().notify(serviceResult.getException());
							}
							else {
								Confirm.confirm(ConfirmType.INFORMATION, "Cache reset", "Reset swagger cache for: " + entry.getId(), null);
							}
						}
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
