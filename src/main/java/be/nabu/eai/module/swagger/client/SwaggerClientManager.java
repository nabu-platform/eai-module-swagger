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
					String prettifiedName = SwaggerParser.cleanup(method.getOperationId() == null ? method.getMethod() + path.getPath(): method.getOperationId());
					if (!exposeAll && (operationIds == null || operationIds.isEmpty() || operationIds.indexOf(prettifiedName) < 0)) {
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
			MemoryEntry entry = new MemoryEntry(root.getRepository(), parent, node, id, id.substring(parentId.length() + 1));
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
