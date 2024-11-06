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
