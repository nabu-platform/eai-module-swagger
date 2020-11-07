package be.nabu.eai.module.swagger.client;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Accordion;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.ArtifactGUIManager;
import be.nabu.eai.developer.api.PortableArtifactGUIManager;
import be.nabu.eai.developer.impl.CustomTooltip;
import be.nabu.eai.developer.managers.base.BaseJAXBGUIManager;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.ace.AceEditor;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.parser.SwaggerDefinitionImpl;
import be.nabu.libs.swagger.parser.SwaggerParser;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.io.api.WritableContainer;

public class SwaggerClientGUIManager extends BaseJAXBGUIManager<SwaggerClientConfiguration, SwaggerClient> {

	public SwaggerClientGUIManager() {
		super("Swagger Client", SwaggerClient.class, new SwaggerClientManager(), SwaggerClientConfiguration.class);
	}

	@Override
	protected List<Property<?>> getCreateProperties() {
		//return Arrays.asList(new SimpleProperty<Boolean>("Expose All Services", Boolean.class, false));
		return null;
	}

	@Override
	protected SwaggerClient newInstance(MainController controller, RepositoryEntry entry, Value<?>... values) throws IOException {
		SwaggerClient swaggerClient = new SwaggerClient(entry.getId(), entry.getContainer(), entry.getRepository());
		// all new ones should be limited by default
		swaggerClient.getConfig().setExposeAllServices(false);
		// we usually want exceptions to be thrown
		swaggerClient.getConfig().setThrowException(true);
		// by default we also use tags to group
		swaggerClient.getConfig().setFolderStructure(FolderStructure.PATH);
		for (Value<?> value : values) {
			if (value.getProperty().getName().equals("Expose All Services")) {
				Boolean result = (Boolean) value.getValue();
				if (result != null && result) {
					swaggerClient.getConfig().setExposeAllServices(true);
				}
			}
		}
		return swaggerClient;
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
		buttons.setPadding(new Insets(10));
		buttons.setAlignment(Pos.CENTER);
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
		Button view = new Button("View Swagger");
		view.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				Resource swagger = instance.getDirectory().getChild("swagger.json");
				if (swagger != null) {
					try {
						ReadableContainer<ByteBuffer> readable = ((ReadableResource) swagger).getReadable();
						try {
							String tabId = instance.getId() + " (swagger.json)";
							Tab tab = MainController.getInstance().getTab(tabId);
							if (tab == null) {
								tab = MainController.getInstance().newTab(tabId);
								AceEditor editor = new AceEditor();
								editor.setContent("application/javascript", new String(IOUtils.toBytes(readable), "UTF-8"));
								editor.setReadOnly(true);
								tab.setContent(editor.getWebView());
							}
							MainController.getInstance().activate(tabId);
						}
						finally {
							readable.close();
						}
					}
					catch (IOException e) {
						MainController.getInstance().notify(e);
					}
				}
			}
		});
		buttons.getChildren().addAll(upload, download, view);
		scroll.setContent(vbox);
		scroll.setFitToWidth(true);
		
		AnchorPane anchorPane = new AnchorPane();
		Accordion accordion = displayWithAccordion(instance, anchorPane);
		
		// this disappears on redraw!
//		for (TitledPane singlePane : accordion.getPanes()) {
//			EAIDeveloperUtils.setPrompt(singlePane.getContent(), "#basePath", instance.getDefinition().getBasePath());
//			EAIDeveloperUtils.setPrompt(singlePane.getContent(), "#host", instance.getDefinition().getHost());
//			if (instance.getDefinition().getSchemes() != null && !instance.getDefinition().getSchemes().isEmpty()) {
//				EAIDeveloperUtils.setPrompt(singlePane.getContent(), "#scheme", instance.getDefinition().getSchemes().contains("https") ? "https" : "http");
//			}
//		}
		
		vbox.getChildren().add(anchorPane);
		
		try {
			VBox drawOperationIds = drawOperationIds(instance);
		
			// allow choosing of exposed operations
			TitledPane operations = new TitledPane("Operations", drawOperationIds);
			accordion.getPanes().add(0, operations);
			
			// we want this as the default
			accordion.setExpandedPane(operations);
		}
		catch (Exception e) {
			MainController.getInstance().notify(e);
		}
		if (instance.getDefinition() instanceof SwaggerDefinitionImpl) {
			List<ValidationMessage> validationMessages = ((SwaggerDefinitionImpl) instance.getDefinition()).getValidationMessages();
			if (validationMessages != null && !validationMessages.isEmpty()) {
				VBox messages = new VBox();
				messages.setPadding(new Insets(10));
				
				StringBuilder builder = new StringBuilder();
				for (ValidationMessage message : validationMessages) {
					if (!builder.toString().isEmpty()) {
						builder.append("\n");
					}
					builder.append("[" + message.getSeverity() + "] " + message.getMessage());
				}
				TextArea area = new TextArea();
				area.setText(builder.toString());
				area.setEditable(false);
				messages.getChildren().addAll(area);
				TitledPane titledPane = new TitledPane("Validation Messages", messages);
				accordion.getPanes().add(titledPane);
			}
		}
		

		pane.getChildren().add(scroll);
	}
	
	@Override
	protected String getDefaultValue(SwaggerClient client, String property) {
		if (client.getDefinition() != null) {
			if ("basePath".equals(property)) {
				return client.getDefinition().getBasePath();
			}
			else if ("host".equals(property)) {
				return client.getDefinition().getHost();
			}
			else if ("scheme".equals(property)) {
				return client.getDefinition().getSchemes().contains("https") ? "https" : "http";
			}
		}
		return super.getDefaultValue(client, property);
	}

	public static VBox drawOperationIds(SwaggerClient instance) {
		VBox operationIds = new VBox();
		
		CheckBox exposeAll = new CheckBox("Expose all operations");
		exposeAll.setSelected(instance.getConfig().getExposeAllServices() == null || instance.getConfig().getExposeAllServices());
		new CustomTooltip("Check this to expose all operations. If left unchecked, you can pick and choose the operations you want to expose").install(exposeAll);
		operationIds.getChildren().add(exposeAll);
		VBox.setMargin(exposeAll, new Insets(10));
		
		TextField filter = new TextField();
		filter.setPromptText("Search");
		operationIds.getChildren().add(filter);
		VBox.setMargin(filter, new Insets(10));
		Map<SwaggerMethod, BooleanProperty> map = new HashMap<SwaggerMethod, BooleanProperty>();
		Map<String, CheckBox> checkboxMap = new HashMap<String, CheckBox>();
		if (instance.getDefinition() != null) {
			for (SwaggerPath path : instance.getDefinition().getPaths()) {
				for (SwaggerMethod method : path.getMethods()) {
					String prettifiedName = SwaggerParser.cleanup(method.getOperationId() == null ? method.getMethod() + path.getPath(): method.getOperationId());
					HBox box = new HBox();
					box.setAlignment(Pos.CENTER_LEFT);
					CheckBox checkBox = new CheckBox();
					
					Label methodLabel = new Label(method.getMethod().toUpperCase());
					Label pathLabel = new Label(path.getPath());
					HBox.setMargin(methodLabel, new Insets(0, 10, 0, 10));
					
					if (method.getMethod().equalsIgnoreCase("post")) {
						methodLabel.setStyle("-fx-background-color: #49cc90; -fx-text-fill: white; -fx-font-weight: bold;");
						methodLabel.setPadding(new Insets(5, 10, 5, 10));
						pathLabel.setStyle("-fx-background-color: #ecfaf4; -fx-border-width: 1px; -fx-border-color: #49cc90");
						pathLabel.setPadding(new Insets(5, 10, 5, 10));
					}
					else if (method.getMethod().equalsIgnoreCase("get")) {
						methodLabel.setStyle("-fx-background-color: #61affe; -fx-text-fill: white; -fx-font-weight: bold;");
						methodLabel.setPadding(new Insets(5, 10, 5, 10));
						pathLabel.setStyle("-fx-background-color: #eff7ff; -fx-border-width: 1px; -fx-border-color: #61affe");
						pathLabel.setPadding(new Insets(5, 10, 5, 10));
					}
					else if (method.getMethod().equalsIgnoreCase("put")) {
						methodLabel.setStyle("-fx-background-color: #fca130; -fx-text-fill: white; -fx-font-weight: bold;");
						methodLabel.setPadding(new Insets(5, 10, 5, 10));
						pathLabel.setStyle("-fx-background-color: #fff5ea; -fx-border-width: 1px; -fx-border-color: #fca130");
						pathLabel.setPadding(new Insets(5, 10, 5, 10));
					}
					else if (method.getMethod().equalsIgnoreCase("delete")) {
						methodLabel.setStyle("-fx-background-color: #f93e3e; -fx-text-fill: white; -fx-font-weight: bold;");
						methodLabel.setPadding(new Insets(5, 10, 5, 10));
						pathLabel.setStyle("-fx-background-color: #feebeb; -fx-border-width: 1px; -fx-border-color: #f93e3e");
						pathLabel.setPadding(new Insets(5, 10, 5, 10));
					}
					// any other method
					else {
						methodLabel.setStyle("-fx-background-color: #b03ef9; -fx-text-fill: white; -fx-font-weight: bold;");
						methodLabel.setPadding(new Insets(5, 10, 5, 10));
						pathLabel.setStyle("-fx-background-color: #fceaff; -fx-border-width: 1px; -fx-border-color: #b03ef9");
						pathLabel.setPadding(new Insets(5, 10, 5, 10));
					}
					
					checkBox.setSelected(instance.getConfig().getOperationIds() != null && instance.getConfig().getOperationIds().indexOf(prettifiedName) >= 0);
					checkboxMap.put(prettifiedName, checkBox);
					
					if (exposeAll.isSelected()) {
						checkBox.setSelected(true);
						checkBox.setDisable(true);
					}
					
					checkBox.selectedProperty().addListener(new ChangeListener<Boolean>() {
						@Override
						public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
							// if it is disabled, we might be manipulating it from somewhere else, don't persist the value!
							if (!checkBox.isDisabled()) {
								// if we didn't set it or set it to true, we expose all, makes no sense to change it
								if (newValue != null && newValue) {
									if (instance.getConfig().getOperationIds() == null) {
										instance.getConfig().setOperationIds(new ArrayList<String>());
									}
									if (instance.getConfig().getOperationIds().indexOf(prettifiedName) < 0) {
										instance.getConfig().getOperationIds().add(prettifiedName);
										MainController.getInstance().setChanged();
									}
								}
								else if (instance.getConfig().getOperationIds() != null) {
									int indexOf = instance.getConfig().getOperationIds().indexOf(prettifiedName);
									if (indexOf >= 0) {
										instance.getConfig().getOperationIds().remove(indexOf);
										MainController.getInstance().setChanged();
									}
								}
							}
						}
					});
					
					Label operationId = new Label(" (" + prettifiedName + ")");
					operationId.setStyle("-fx-text-fill: #aaa");
					box.getChildren().addAll(checkBox, methodLabel, pathLabel, operationId);
					if (method.getDescription() != null && !method.getDescription().trim().isEmpty()) {
						MainController.getInstance().attachTooltip(operationId, method.getDescription());
					}
					HBox spacer = new HBox();
					HBox.setHgrow(spacer, Priority.ALWAYS);
					box.getChildren().add(spacer);
					Button button = new Button();
					button.setGraphic(MainController.loadFixedSizeGraphic("right-chevron.png", 12));
					button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
						@SuppressWarnings({ "unchecked", "rawtypes" })
						@Override
						public void handle(ActionEvent event) {
							SwaggerService swaggerService = new SwaggerService(instance.getId() + ".services." + prettifiedName, instance, path, method);
							ArtifactGUIManager<?> guiManager = MainController.getInstance().getGUIManager(swaggerService.getClass());
							if (guiManager instanceof PortableArtifactGUIManager) {
								AnchorPane pane = new AnchorPane();
								try {
									((PortableArtifactGUIManager) guiManager).display(MainController.getInstance(), pane, swaggerService);
									Tab newTab = MainController.getInstance().newTab(swaggerService.getId());
									newTab.setContent(pane);
								}
								catch (Exception e) {
									MainController.getInstance().notify(e);
								}
							}
						}
					});
					box.getChildren().add(button);
					
					box.setPadding(new Insets(10));
					operationIds.getChildren().add(box);
					map.put(method, new SimpleBooleanProperty(true));
					box.visibleProperty().bind(map.get(method));
					box.managedProperty().bind(box.visibleProperty());
				}
			}
		}
		
		filter.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue == null || newValue.trim().isEmpty()) {
					for (BooleanProperty property : map.values()) {
						property.set(true);
					}
				}
				else {
					String regex = newValue.contains("*") ? "(?i)(?s).*" + newValue.replace("*", ".*") + ".*" : null;
					for (SwaggerPath path : instance.getDefinition().getPaths()) {
						for (SwaggerMethod method : path.getMethods()) {
							if (regex != null) {
								map.get(method).set(path.getPath().matches(regex) || (method.getDescription() != null && method.getDescription().matches(regex)));
							}
							else {
								map.get(method).set(path.getPath().toLowerCase().indexOf(newValue.toLowerCase()) >= 0 || (method.getDescription() != null && method.getDescription().toLowerCase().indexOf(newValue.toLowerCase()) >= 0));
							}
						}
					}
				}
			}
		});
		
		exposeAll.selectedProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				if (arg2 != null && arg2) {
					for (Map.Entry<String, CheckBox> entry : checkboxMap.entrySet()) {
						entry.getValue().setDisable(true);
						entry.getValue().setSelected(true);
					}
				}
				else {
					for (Map.Entry<String, CheckBox> entry : checkboxMap.entrySet()) {
						entry.getValue().setSelected(instance.getConfig().getOperationIds() != null && instance.getConfig().getOperationIds().indexOf(entry.getKey()) >= 0);
						entry.getValue().setDisable(false);
					}
				}
				instance.getConfig().setExposeAllServices(arg2 != null && arg2);
				MainController.getInstance().setChanged();
			}
		});
		return operationIds;
	}

	@Override
	protected List<String> getBlacklistedProperties() {
		return Arrays.asList("operationIds", "exposeAllServices");
	}
	
	
}
