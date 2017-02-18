package be.nabu.eai.module.swagger.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.WritableContainer;

public class SwaggerClientGUIManager extends BaseJAXBGUIManager<SwaggerClientConfiguration, SwaggerClient> {

	public SwaggerClientGUIManager() {
		super("Swagger Client", SwaggerClient.class, new SwaggerClientManager(), SwaggerClientConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		return null;
	}

	@Override
	protected SwaggerClient newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		return new SwaggerClient(entry.getId(), entry.getContainer(), entry.getRepository());
	}

	public String getCategory() {
		return "Services";
	}
	
	@Override
	public void display(MainController controller, AnchorPane pane, SwaggerClient instance) {
		ScrollPane scroll = new ScrollPane();
		AnchorPane.setBottomAnchor(scroll, 0d);
		AnchorPane.setTopAnchor(scroll, 0d);
		AnchorPane.setLeftAnchor(scroll, 0d);
		AnchorPane.setRightAnchor(scroll, 0d);
		VBox vbox = new VBox();
		HBox buttons = new HBox();
		vbox.getChildren().add(buttons);
		Button upload = new Button("Update from file");
		upload.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void handle(ActionEvent arg0) {
				SimpleProperty<File> fileProperty = new SimpleProperty<File>("File", File.class, true);
				fileProperty.setInput(true);
				final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet(Arrays.asList(new Property [] { fileProperty })));
				EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Swagger File", new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						try {
							if (instance.getDirectory().getChild("swagger.json") != null) {
								((ManageableContainer<?>) instance.getDirectory()).delete("swagger.json");
							}
							File file = updater.getValue("File");
							if (file != null) {
								Resource child = instance.getDirectory().getChild("swagger.json");
								if (child == null) {
									child = ((ManageableContainer<?>) instance.getDirectory()).create("swagger.json", "application/json");
								}
								WritableContainer<ByteBuffer> writable = ((WritableResource) child).getWritable();
								try {
									InputStream input = new BufferedInputStream(new FileInputStream(file));
									try {
										IOUtils.copyBytes(IOUtils.wrap(input), writable);
									}
									finally {
										input.close();
									}
								}
								finally {
									writable.close();
								}
							}
							MainController.getInstance().setChanged();
							MainController.getInstance().refresh(instance.getId());
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
			}
		});
		Button download = new Button("Update from URI");
		download.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			@Override
			public void handle(ActionEvent arg0) {
				SimpleProperty<URI> fileProperty = new SimpleProperty<URI>("URI", URI.class, true);
				final SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet(Arrays.asList(new Property [] { fileProperty })));
				EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Swagger URI", new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						try {
							if (instance.getDirectory().getChild("swagger.json") != null) {
								((ManageableContainer<?>) instance.getDirectory()).delete("swagger.json");
							}
							URI uri = updater.getValue("URI");
							if (uri != null) {
								Resource child = instance.getDirectory().getChild("swagger.json");
								if (child == null) {
									child = ((ManageableContainer<?>) instance.getDirectory()).create("swagger.json", "application/json");
								}
								WritableContainer<ByteBuffer> writable = ((WritableResource) child).getWritable();
								try {
									InputStream stream = uri.toURL().openStream();
									try {
										IOUtils.copyBytes(IOUtils.wrap(stream), writable);
									}
									finally {
										stream.close();
									}
								}
								finally {
									writable.close();
								}
							}
							MainController.getInstance().setChanged();
							MainController.getInstance().refresh(instance.getId());
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				});
			}
		});
		buttons.getChildren().addAll(upload, download);
		vbox.prefWidthProperty().bind(scroll.widthProperty());
		scroll.setContent(vbox);
		AnchorPane anchorPane = new AnchorPane();
		display(instance, anchorPane);
		vbox.getChildren().add(anchorPane);
		pane.getChildren().add(scroll);
	}
}
