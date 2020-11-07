package be.nabu.eai.module.swagger.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import be.nabu.eai.module.types.structure.StructureManager;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.resources.api.ResourceContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.parser.SwaggerParser;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.SimpleType;

public class SwaggerClientManager extends JAXBArtifactManager<SwaggerClientConfiguration, SwaggerClient> implements ArtifactRepositoryManager<SwaggerClient> {

	public SwaggerClientManager() {
		super(SwaggerClient.class);
	}

	/**
	 * This does not do recursive resolving so if type A is referenced and it itself references B, it will _not_ be picked up
	 */
	private Set<String> getUsedTypes(DefinedService service) {
		Set<String> set = new HashSet<String>();
		set.addAll(StructureManager.getComplexReferences(service.getServiceInterface().getInputDefinition(), true));
		set.addAll(StructureManager.getComplexReferences(service.getServiceInterface().getOutputDefinition(), true));
		return set;
	}
	
	@Override
	public List<Entry> addChildren(ModifiableEntry root, SwaggerClient artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		((EAINode) root.getNode()).setLeaf(false);
		if (artifact.getDefinition() != null) {
			boolean exposeAll = artifact.getConfig().getExposeAllServices() == null || artifact.getConfig().getExposeAllServices();
			List<String> operationIds = artifact.getConfig().getOperationIds();
			Set<String> set = new HashSet<String>();
			for (SwaggerPath path : artifact.getDefinition().getPaths()) {
				for (SwaggerMethod method : path.getMethods()) {
					String prettifiedName;
					FolderStructure folderStructure = artifact.getConfig().getFolderStructure();
					// backwards compatibility I'm afraid... :(
					if (folderStructure == null) {
						folderStructure = FolderStructure.HIERARCHY;
					}
					switch(folderStructure) {
						// the default as it was originally
						case HIERARCHY:
							prettifiedName = SwaggerParser.cleanup(method.getOperationId() == null ? method.getMethod() + path.getPath(): method.getOperationId(), true);
						break;
						case FLAT:
							prettifiedName = SwaggerParser.cleanup(method.getOperationId() == null ? method.getMethod() + path.getPath(): method.getOperationId(), false);
						break;
						case PATH:
							StringBuilder builder = new StringBuilder();
							StringBuilder by = new StringBuilder();
							builder.append(method.getMethod().toLowerCase());
							String resourcePath = path.getPath();
							// this should always be the case?
							if (resourcePath.startsWith(artifact.getDefinition().getBasePath())) {
								resourcePath = resourcePath.substring(artifact.getDefinition().getBasePath().length());
							}
							boolean firstBy = true;
							for (String part : resourcePath.split("/")) {
								if (part.isEmpty()) {
									continue;
								}
								boolean variable = false;
								// it's a variable part, we do some additional stuff
								if (part.startsWith("{") && part.endsWith("}")) {
									part = part.substring(1, part.length() - 1);
									// if you have multiple ids, we do an "and" in between them
									by.append(firstBy ? "By" : "And");
									part = part.substring(0, 1).toUpperCase() + part.substring(1);
									variable = true;
									firstBy = false;
								}
								// we want to skip any "." as they will mess up the structure
								// and replace underscore notation with upper camelcase
								for (String subPart : part.replace(".", "").split("_")) {
									if (subPart.isEmpty()) {
										continue;
									}
									(variable ? by : builder).append(subPart.substring(0, 1).toUpperCase() + subPart.substring(1));
								}
							}
							// we want the by at the end, for example if you have GET /accounts/{accountId}/billing/charges
							// we want it to be "getAccountsBillingChargesByAccountId" rather than "getAccountsByAccountIdBillingCharges"
							// in extreme cases this can lead to conflicts, in that case, you probably can't use this naming method
							builder.append(by.toString());
							// we remove any dots, they can only mess things up
							prettifiedName = builder.toString();
						break;
						default:
							prettifiedName = SwaggerParser.cleanup(method.getOperationId() == null ? method.getMethod() + path.getPath(): method.getOperationId(), false);
							if (method.getTags() != null && !method.getTags().isEmpty()) {
								String namespace = SwaggerParser.cleanup(method.getTags().get(0), false);
								prettifiedName = namespace + "." + prettifiedName;
							}
					}
					// regardless of the folderstructure, we use these operation ids
					String fixedOperationId = SwaggerParser.cleanup(method.getOperationId() == null ? method.getMethod() + path.getPath(): method.getOperationId(), true);
					if (!exposeAll && (operationIds == null || operationIds.isEmpty() || operationIds.indexOf(fixedOperationId) < 0)) {
						continue;
					}
					SwaggerProxyInterface iface = new SwaggerProxyInterface(root.getId() + ".interfaces." + prettifiedName, artifact.getDefinition(), path, method, artifact.getConfig().getSecurity());
					if (artifact.getConfig().isShowInterfaces()) {
						addChild(root, artifact, entries, iface);
					}
					SwaggerService child = new SwaggerService(root.getId() + ".services." + prettifiedName, artifact, path, method);
					addChild(root, artifact, entries, child);
					if (!exposeAll) {
						set.addAll(getUsedTypes(child));
					}
				}
			}
			
			for (String namespace : artifact.getDefinition().getRegistry().getNamespaces()) {
				for (ComplexType type : artifact.getDefinition().getRegistry().getComplexTypes(namespace)) {
					if (type instanceof DefinedType) {
						if (exposeAll || set.contains(((DefinedType) type).getId())) {
							addChild(root, artifact, entries, (DefinedType) type);
						}
					}
				}
				for (SimpleType<?> type : artifact.getDefinition().getRegistry().getSimpleTypes(namespace)) {
					if (type instanceof DefinedType) {
						if (exposeAll || set.contains(((DefinedType) type).getId())) {
							addChild(root, artifact, entries, (DefinedType) type);
						}
					}
				}
			}
		}
		return entries;
	}
	
	private void addChild(ModifiableEntry root, SwaggerClient artifact, List<Entry> entries, Artifact child) {
		String id = child.getId();
		if (id.startsWith(artifact.getId() + ".")) {
			String parentId = id.replaceAll("\\.[^.]+$", "");
			ModifiableEntry parent = EAIRepositoryUtils.getParent(root, id.substring(artifact.getId().length() + 1), false);
			EAINode node = new EAINode();
			node.setArtifact(child);
			node.setLeaf(true);
			MemoryEntry entry = new MemoryEntry(artifact.getId(), root.getRepository(), parent, node, id, id.substring(parentId.length() + 1));
			node.setEntry(entry);
			parent.addChildren(entry);
			entries.add(entry);
		}
	}
	
	@Override
	public List<Entry> removeChildren(ModifiableEntry parent, SwaggerClient artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		Entry services = parent.getChild("services");
		if (services != null) {
			for (Entry service : services) {
				entries.add(service);
			}
			parent.removeChildren("services");
		}
		Entry types = parent.getChild("types");
		if (types != null) {
			for (Entry type : types) {
				entries.add(type);
			}
			parent.removeChildren("types");
		}
		Entry interfaces = parent.getChild("interfaces");
		if (interfaces != null) {
			for (Entry type : interfaces) {
				entries.add(type);
			}
			parent.removeChildren("interfaces");
		}
		return entries;
	}

	@Override
	protected SwaggerClient newInstance(String id, ResourceContainer<?> container, Repository repository) {
		return new SwaggerClient(id, container, repository);
	}

}
