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

package be.nabu.eai.module.swagger.client.collection;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.developer.CollectionActionImpl;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.CollectionAction;
import be.nabu.eai.developer.api.CollectionManager;
import be.nabu.eai.developer.api.CollectionManagerFactory;
import be.nabu.eai.developer.api.EntryAcceptor;
import be.nabu.eai.developer.collection.EAICollectionUtils;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.module.swagger.client.FolderStructure;
import be.nabu.eai.module.swagger.client.SwaggerClient;
import be.nabu.eai.module.swagger.client.SwaggerClientManager;
import be.nabu.eai.repository.CollectionImpl;
import be.nabu.eai.repository.api.Collection;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.resources.URIUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class SwaggerClientCollectionFactory implements CollectionManagerFactory {

	private static final String FOLDER_NAME = "integrations"; // used to be: connectors
	
	@Override
	public CollectionManager getCollectionManager(Entry entry) {
		for (Entry child : entry) {
			if (child.isNode() && SwaggerClient.class.equals(child.getNode().getArtifactClass())) {
				return new SwaggerClientCollection(entry);
			}
		}
		return null;
	}

	@Override
	public List<CollectionAction> getActionsFor(Entry entry) {
		List<CollectionAction> actions = new ArrayList<CollectionAction>();
		if (EAICollectionUtils.isProject(entry)) {
			actions.add(new CollectionActionImpl(EAICollectionUtils.newActionTile("swaggerclient-large.png", "Add Swagger", "Load a third party Swagger."), new javafx.event.EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent arg0) {
					SwaggerClientCreate swaggerClientCreate = new SwaggerClientCreate();
					EAIDeveloperUtils.buildPopup("Connect via Swagger", "Swagger", swaggerClientCreate, new EventHandler<ActionEvent>() {
						@Override
						public void handle(ActionEvent arg0) {
							String uri = swaggerClientCreate.getUri();
							String name = swaggerClientCreate.getName();
							// only proceed if we actually have a URI
							if (uri != null && !uri.trim().isEmpty() && name != null && !name.trim().isEmpty()) {
								try {
									URI result = new URI(URIUtils.encodeURI(uri));
									RepositoryEntry connectorEntry = getSwaggerEntry((RepositoryEntry) entry, swaggerClientCreate.getName());
									SwaggerClient swaggerClient = new SwaggerClient(connectorEntry.getId(), connectorEntry.getContainer(), connectorEntry.getRepository());
									// all new ones should be limited by default
									swaggerClient.getConfig().setExposeAllServices(false);
									// we usually want exceptions to be thrown
									swaggerClient.getConfig().setThrowException(true);
									// by default we also use tags to group
									swaggerClient.getConfig().setFolderStructure(FolderStructure.PATH);
									
									new SwaggerClientManager().save(connectorEntry, swaggerClient);
									swaggerClient.load(result);
									EAIDeveloperUtils.created(swaggerClient.getId());
									
									MainController.getInstance().open(swaggerClient.getId());
								}
								catch (Exception e) {
									MainController.getInstance().notify(e);
								}
							}
						}
					}, false).show();
				}
			}, new EntryAcceptor() {
				@Override
				public boolean accept(Entry entry) {
					Collection collection = entry.getCollection();
					return collection != null && "folder".equals(collection.getType()) && "integrations".equals(collection.getSubType());
				}
			}));
		}
		return actions;
	}

	public static Entry getConnectorsEntry(RepositoryEntry project) throws IOException {
		Entry child = EAIDeveloperUtils.mkdir(project, FOLDER_NAME);
		if (!child.isCollection()) {
			CollectionImpl collection = new CollectionImpl();
			collection.setType("folder");
			collection.setSubType("integrations");
			collection.setName("Integrations");
			collection.setSmallIcon("integration/integration-small.png");
			collection.setMediumIcon("integration/integration-medium.png");
			collection.setLargeIcon("integration/integration-large.png");
			((RepositoryEntry) child).setCollection(collection);
			((RepositoryEntry) child).saveCollection();
			EAIDeveloperUtils.updated(child.getId());
		}
		return child;
	}
	
	public static Entry getConnectorEntry(RepositoryEntry project, String name) throws IOException {
		String normalize = EAICollectionUtils.normalize(name);
		Entry child = EAIDeveloperUtils.mkdir((RepositoryEntry) getConnectorsEntry(project), normalize);
		if (!child.isCollection()) {
			CollectionImpl collection = new CollectionImpl();
			collection.setType("integration");
			if (!normalize.equals(name)) {
				collection.setName(name);
			}
			((RepositoryEntry) child).setCollection(collection);
			((RepositoryEntry) child).saveCollection();
			EAIDeveloperUtils.updated(child.getId());
		}
		return child;
	}
	
	public static RepositoryEntry getSwaggerEntry(RepositoryEntry project, String name) throws IOException {
		Entry connectorEntry = getConnectorEntry(project, name);
		return ((RepositoryEntry) connectorEntry).createNode("swagger", new SwaggerClientManager(), true);
	}
}
