package be.nabu.eai.module.swagger.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIRepositoryUtils;
import be.nabu.eai.repository.api.ArtifactRepositoryManager;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ModifiableEntry;
import be.nabu.eai.repository.api.Repository;
import be.nabu.eai.repository.managers.base.JAXBArtifactManager;
import be.nabu.eai.repository.resources.MemoryEntry;
import be.nabu.libs.resources.api.ResourceContainer;
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

	@Override
	public List<Entry> addChildren(ModifiableEntry root, SwaggerClient artifact) throws IOException {
		List<Entry> entries = new ArrayList<Entry>();
		((EAINode) root.getNode()).setLeaf(false);
		if (artifact.getDefinition() != null) {
			MemoryEntry services = new MemoryEntry(root.getRepository(), root, null, root.getId() + ".services", "services");
			
			for (SwaggerPath path : artifact.getDefinition().getPaths()) {
				for (SwaggerMethod method : path.getMethods()) {
					String prettifiedName = SwaggerParser.cleanup(method.getOperationId());
					final String ifaceId = root.getId() + ".interfaces." + prettifiedName;
					addInterface(root, artifact, entries, new SwaggerInterface(ifaceId, artifact.getDefinition(), path, method));
				}
			}
			
			root.addChildren(services);
			
			for (String namespace : artifact.getDefinition().getRegistry().getNamespaces()) {
				for (ComplexType type : artifact.getDefinition().getRegistry().getComplexTypes(namespace)) {
					if (type instanceof DefinedType) {
						addType(root, artifact, entries, (DefinedType) type);
					}
				}
				for (SimpleType<?> type : artifact.getDefinition().getRegistry().getSimpleTypes(namespace)) {
					if (type instanceof DefinedType) {
						addType(root, artifact, entries, (DefinedType) type);
					}
				}
			}
		}
		return entries;
	}
	
	private void addInterface(ModifiableEntry root, SwaggerClient artifact, List<Entry> entries, SwaggerInterface iface) {
		String id = iface.getId();
		if (id.startsWith(artifact.getId() + ".")) {
			String parentId = id.replaceAll("\\.[^.]+$", "");
			ModifiableEntry parent = EAIRepositoryUtils.getParent(root, id.substring(artifact.getId().length() + 1), false);
			EAINode node = new EAINode();
			node.setArtifact(iface);
			node.setLeaf(true);
			MemoryEntry entry = new MemoryEntry(root.getRepository(), parent, node, id, id.substring(parentId.length() + 1));
			node.setEntry(entry);
			parent.addChildren(entry);
			entries.add(entry);
		}
	}
	
	private void addType(ModifiableEntry root, SwaggerClient artifact, List<Entry> entries, DefinedType type) {
		String id = ((DefinedType) type).getId();
		if (id.startsWith(artifact.getId() + ".")) {
			String parentId = id.replaceAll("\\.[^.]+$", "");
			ModifiableEntry parent = EAIRepositoryUtils.getParent(root, id.substring(artifact.getId().length() + 1), false);
			EAINode node = new EAINode();
			node.setArtifact(type);
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
