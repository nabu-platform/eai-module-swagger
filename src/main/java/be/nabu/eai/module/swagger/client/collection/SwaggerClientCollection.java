package be.nabu.eai.module.swagger.client.collection;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.developer.api.CollectionManager;
import be.nabu.eai.developer.collection.EAICollectionUtils;
import be.nabu.eai.module.swagger.client.SwaggerClient;
import be.nabu.eai.repository.api.Entry;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class SwaggerClientCollection implements CollectionManager {

	private Entry entry;

	public SwaggerClientCollection(Entry entry) {
		this.entry = entry;
	}

	@Override
	public Entry getEntry() {
		return entry;
	}

	@Override
	public boolean hasSummaryView() {
		return true;
	}

	@Override
	public Node getSummaryView() {
		List<Button> buttons = EAICollectionUtils.newViewButton(entry, SwaggerClient.class);
		buttons.add(EAICollectionUtils.newDeleteButton(entry, null));
		List<Entry> services = new ArrayList<Entry>();
		for (Entry child : entry) {
			if (child.isNode() && SwaggerClient.class.isAssignableFrom(child.getNode().getArtifactClass())) {
				services.addAll(EAICollectionUtils.scanForServices(child));
			}
		}
		return EAICollectionUtils.newSummaryTile(entry, "swaggerclient-large.png", services, buttons);
	}
	
}
