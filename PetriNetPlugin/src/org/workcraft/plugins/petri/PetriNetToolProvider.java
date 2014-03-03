package org.workcraft.plugins.petri;

import java.util.ArrayList;

import org.workcraft.gui.graph.tools.ConnectionTool;
import org.workcraft.gui.graph.tools.CustomToolsProvider;
import org.workcraft.gui.graph.tools.DefaultNodeGenerator;
import org.workcraft.gui.graph.tools.GraphEditorTool;
import org.workcraft.gui.graph.tools.CommentGeneratorTool;
import org.workcraft.gui.graph.tools.NodeGeneratorTool;
import org.workcraft.plugins.petri.tools.PetriNetSelectionTool;
import org.workcraft.plugins.petri.tools.PetriNetSimulationTool;

public class PetriNetToolProvider implements CustomToolsProvider {

	@Override
	public Iterable<GraphEditorTool> getTools() {
		ArrayList<GraphEditorTool> result = new ArrayList<GraphEditorTool>();

		result.add(new PetriNetSelectionTool());
		result.add(new CommentGeneratorTool());
		result.add(new ConnectionTool());
		result.add(new NodeGeneratorTool(new DefaultNodeGenerator(Place.class)));
		result.add(new NodeGeneratorTool(new DefaultNodeGenerator(Transition.class)));
		result.add(new PetriNetSimulationTool());

		return result;
	}

}