package be.nabu.eai.module.swagger.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.impl.BaseNodeMetadataArtifactFragmentManager;
import be.nabu.libs.resources.ResourceReadableContainer;
import be.nabu.libs.resources.ResourceWritableContainer;
import be.nabu.libs.resources.api.ReadableResource;
import be.nabu.libs.resources.api.Resource;
import be.nabu.libs.resources.api.WritableResource;
import be.nabu.libs.swagger.parser.SwaggerParser;
import be.nabu.libs.validator.api.Validation;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.utils.io.IOUtils;

public class SwaggerClientArtifactFragmentManager extends BaseNodeMetadataArtifactFragmentManager<SwaggerClient> {

	private static final String CONFIGURATION_PATH = "swagger-client.xml";
	private static final String DEFINITION_PATH = "swagger.json";
	private static final String XML_CONTENT_TYPE = "application/xml";
	private static final String JSON_CONTENT_TYPE = "application/json";
	private static final String ARTIFACT_TYPE = "swaggerClient";
	private static final String ARTIFACT_CATEGORY = "service";

	@Override
	public List<ArtifactFragment> listFragments(SwaggerClient artifact) {
		List<ArtifactFragment> fragments = new ArrayList<ArtifactFragment>(getSharedFragments(artifact));
		Entry entry = EAIResourceRepository.getInstance().getEntry(artifact.getId());
		boolean editable = entry instanceof ResourceEntry && entry.isEditable();
		fragments.add(new ResourceFragment(artifact, CONFIGURATION_PATH, XML_CONTENT_TYPE, ARTIFACT_TYPE, editable));
		if (artifact.getDirectory().getChild(DEFINITION_PATH) instanceof ReadableResource) {
			fragments.add(new ResourceFragment(artifact, DEFINITION_PATH, JSON_CONTENT_TYPE, "definition", editable));
		}
		return fragments;
	}

	@Override
	public List<Validation<?>> updateFragment(SwaggerClient artifact, String path, String oldContent, String newContent) {
		if (!CONFIGURATION_PATH.equals(path) && !DEFINITION_PATH.equals(path)) {
			return super.updateFragment(artifact, path, oldContent, newContent);
		}
		List<Validation<?>> validations = new ArrayList<Validation<?>>();
		Entry entry = EAIResourceRepository.getInstance().getEntry(artifact.getId());
		if (!(entry instanceof ResourceEntry)) {
			validations.add(new ValidationMessage(ValidationMessage.Severity.ERROR, "Updating fragments requires a resource-backed swagger client"));
			return validations;
		}
		try {
			if (CONFIGURATION_PATH.equals(path)) {
				SwaggerClient candidate = new SwaggerClient(artifact.getId(), ((ResourceEntry) entry).getContainer(), ((ResourceEntry) entry).getRepository());
				candidate.setConfig(artifact.unmarshal(new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8))));
				validations.addAll(new SwaggerClientManager().save((ResourceEntry) entry, candidate));
			}
			else {
				SwaggerParser.parseJson(new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8)));
				writeResource(artifact, path, newContent);
			}
		}
		catch (Exception e) {
			validations.add(new ValidationMessage(ValidationMessage.Severity.ERROR, e.getMessage() == null ? e.getClass().getName() : e.getMessage()));
		}
		return validations;
	}

	@Override
	public Class<SwaggerClient> getArtifactClass() {
		return SwaggerClient.class;
	}

	@Override
	public String getArtifactType() {
		return ARTIFACT_TYPE;
	}

	@Override
	public String getArtifactCategory() {
		return ARTIFACT_CATEGORY;
	}

	private String readResource(SwaggerClient artifact, String path) {
		try {
			Resource resource = artifact.getDirectory().getChild(path);
			if (!(resource instanceof ReadableResource)) {
				return "";
			}
			try (InputStream input = IOUtils.toInputStream(new ResourceReadableContainer((ReadableResource) resource))) {
				ByteArrayOutputStream output = new ByteArrayOutputStream();
				byte[] buffer = new byte[4096];
				int read;
				while ((read = input.read(buffer)) >= 0) {
					output.write(buffer, 0, read);
				}
				return new String(output.toByteArray(), StandardCharsets.UTF_8);
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private void writeResource(SwaggerClient artifact, String path, String content) throws Exception {
		Resource resource = artifact.getDirectory().getChild(path);
		if (!(resource instanceof WritableResource)) {
			throw new IllegalStateException("Fragment '" + path + "' is not writable");
		}
		try (OutputStream output = IOUtils.toOutputStream(new ResourceWritableContainer((WritableResource) resource))) {
			output.write(content.getBytes(StandardCharsets.UTF_8));
		}
	}

	private class ResourceFragment implements ArtifactFragment {

		private final SwaggerClient artifact;
		private final String path;
		private final String contentType;
		private final String fragmentType;
		private final boolean editable;

		private ResourceFragment(SwaggerClient artifact, String path, String contentType, String fragmentType, boolean editable) {
			this.artifact = artifact;
			this.path = path;
			this.contentType = contentType;
			this.fragmentType = fragmentType;
			this.editable = editable;
		}

		@Override
		public boolean isEditable() {
			return editable;
		}

		@Override
		public boolean isRemovable() {
			return false;
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public String getContent() {
			return readResource(artifact, path);
		}

		@Override
		public String getContentType() {
			return contentType;
		}

		@Override
		public String getArtifactId() {
			return artifact.getId();
		}

		@Override
		public String getFragmentType() {
			return fragmentType;
		}

		@Override
		public Map<String, String> getProperties() {
			return new LinkedHashMap<String, String>();
		}

		@Override
		public Long getLastModified() {
			return getFragmentLastModified(artifact.getId(), path);
		}
	}
}
