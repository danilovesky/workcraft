/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.plugins.interop;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.workcraft.dom.Connection;
import org.workcraft.dom.Model;
import org.workcraft.exceptions.ModelValidationException;
import org.workcraft.exceptions.SerialisationException;
import org.workcraft.interop.SynchronousExternalProcess;
import org.workcraft.plugins.balsa.BalsaCircuit;
import org.workcraft.plugins.balsa.BreezeComponent;
import org.workcraft.plugins.balsa.HandshakeComponent;
import org.workcraft.plugins.balsa.handshakebuilder.Handshake;
import org.workcraft.plugins.balsa.handshakeevents.TwoWayStg;
import org.workcraft.plugins.balsa.handshakestgbuilder.ActivenessSelector;
import org.workcraft.plugins.balsa.handshakestgbuilder.HandshakeProtocol;
import org.workcraft.plugins.balsa.handshakestgbuilder.InternalHandshakeStgBuilder;
import org.workcraft.plugins.balsa.handshakestgbuilder.TwoSideStg;
import org.workcraft.plugins.balsa.stg.MainStgBuilder;
import org.workcraft.plugins.balsa.stgbuilder.StgBuilder;
import org.workcraft.plugins.balsa.stgmodelstgbuilder.HandshakeNameProvider;
import org.workcraft.plugins.balsa.stgmodelstgbuilder.StgModelStgBuilder;
import org.workcraft.plugins.layout.PetriNetToolsSettings;
import org.workcraft.plugins.stg.STG;
import org.workcraft.util.Export;
import org.workcraft.util.Hierarchy;

public abstract class BalsaToStgExporter {

	private final HandshakeProtocol protocol;
	private final String protocolName;

	public BalsaToStgExporter(HandshakeProtocol protocol, String protocolName)
	{
		this.protocol = protocol;
		this.protocolName = protocolName;
	}

	public void export(Model model, OutputStream out) throws IOException, ModelValidationException, SerialisationException {

		BalsaCircuit balsa = (BalsaCircuit)model;

		boolean useSimpleInternalHandshakes = false;

		if(useSimpleInternalHandshakes)
		{
			STG stgf = buildStgFull(balsa);
			new DotGExporter().export(stgf, out);
		}
		else
		{
			ArrayList<File> tempFiles = new ArrayList<File>();
			for(BreezeComponent component : getComponentsToSave(balsa))
			{
				STG stg = buildStg(balsa, component);

				File tempFile = File.createTempFile("brz_", ".g");
				tempFiles.add(tempFile);

				DotGExporter exporter = new DotGExporter();

				Export.exportToFile(exporter, stg, tempFile);
			}

			String [] args = new String [tempFiles.size() + 3];
			args[0] = PetriNetToolsSettings.getPcompCommand();
			args[1] = "-d";
			args[2] = "-r";
			for(int i=0;i<tempFiles.size();i++)
				args[i+3] = tempFiles.get(i).getPath();

			SynchronousExternalProcess pcomp = new SynchronousExternalProcess(args, ".");

			pcomp.start(10000);

			byte [] outputData = pcomp.getOutputData();
			System.out.println("----- Pcomp errors: -----");
			System.out.print(new String(pcomp.getErrorData()));
			System.out.println("----- End of errors -----");

			if(pcomp.getReturnCode() != 0)
			{
				System.out.println("");
				System.out.println("----- Pcomp output: -----");
				System.out.print(new String(outputData));
				System.out.println("----- End of output -----");

				throw new RuntimeException("Pcomp failed! Return code: " + pcomp.getReturnCode());
			}

			saveData(outputData, out);

			for(File f : tempFiles)
				f.delete();
		}
	}

	private STG buildStgFull(BalsaCircuit balsa) {
		STG stg = new STG();

		Iterable<BreezeComponent> components = getComponentsToSave(balsa);

		Map<Connection, TwoWayStg> internalHandshakes = new HashMap<Connection, TwoWayStg>();

		for(BreezeComponent component : components)
		{
			Map<String, Handshake> fullHandshakes = new HashMap<String, Handshake>(component.getHandshakes());

			MainStgBuilder.addDataPathHandshakes(fullHandshakes, component.getUnderlyingComponent());

			HandshakeNameProvider nameProvider = getNamesProvider(balsa, component, fullHandshakes);

			StgModelStgBuilder stgBuilder = new StgModelStgBuilder(stg, nameProvider);

			Map<String, Handshake> external = new HashMap<String, Handshake>();
			Map<String, TwoWayStg> internal = new HashMap<String, TwoWayStg>();

			for(String name : fullHandshakes.keySet())
			{
				Connection connection = getInternalConnection(balsa, components, component, fullHandshakes.get(name));
				if(connection == null)
					external.put(name, fullHandshakes.get(name));
				else
				{
					if(!internalHandshakes.containsKey(connection))
					{
						TwoWayStg internalStg = buildInternalStg(fullHandshakes.get(name), stgBuilder);
						internalHandshakes.put(connection, internalStg);
					}

					internal.put(name, internalHandshakes.get(connection));
				}
			}

			Map<String, TwoSideStg> handshakeStgs = MainStgBuilder.buildHandshakes(external, protocol, stgBuilder);

			for(String name : internal.keySet())
				handshakeStgs.put(name, ActivenessSelector.direct(internal.get(name), fullHandshakes.get(name).isActive()));

			MainStgBuilder.buildStg(component.getUnderlyingComponent(), handshakeStgs, stgBuilder);
		}

		return stg;
	}

	private Connection getInternalConnection(BalsaCircuit balsa, Iterable<BreezeComponent> components, BreezeComponent component, Handshake handshake) {
		HandshakeComponent hs = component.getHandshakeComponents().get(handshake);
		if(hs==null)
			return null;
		Connection connection = balsa.getConnection(hs);
		if(connection == null)
			return null;

		if(!contains(components, balsa.getConnectedHandshake(hs).getOwner()))
			return null;
		return connection;
	}

	private TwoWayStg buildInternalStg(Handshake handshake, StgBuilder stg) {
		return handshake.buildStg(new InternalHandshakeStgBuilder(stg));
	}

	private boolean contains(Iterable<BreezeComponent> components, BreezeComponent component)
	{
		for(BreezeComponent c : components)
			if(component == c)
				return true;
		return false;
	}

	protected Iterable<BreezeComponent> getComponentsToSave(BalsaCircuit balsa) {
		return Hierarchy.getDescendantsOfType(balsa.getRoot(), BreezeComponent.class);
	}

	private static void saveData(byte [] outputData, OutputStream out) throws IOException
	{
		out.write(outputData);
	}


	private STG buildStg(final BalsaCircuit circuit, final BreezeComponent breezeComponent) {
		STG stg = new STG();

		Map<String, Handshake> fullHandshakes = new HashMap<String, Handshake>(breezeComponent.getHandshakes());

		MainStgBuilder.addDataPathHandshakes(fullHandshakes, breezeComponent.getUnderlyingComponent());

		HandshakeNameProvider nameProvider = getNamesProvider(circuit, breezeComponent, fullHandshakes);

		StgModelStgBuilder stgBuilder = new StgModelStgBuilder(stg, nameProvider);

		Map<String, TwoSideStg> handshakeStgs = MainStgBuilder.buildHandshakes(fullHandshakes, protocol, stgBuilder);

		MainStgBuilder.buildStg(breezeComponent.getUnderlyingComponent(), handshakeStgs, stgBuilder);
		return stg;
	}

	private HandshakeNameProvider getNamesProvider(final BalsaCircuit circuit, final BreezeComponent breezeComponent, final Map<String, Handshake> handshakes) {
		final HashMap<Object, String> names;

		names = new HashMap<Object, String>();

		for(Entry<String, Handshake> entry : handshakes.entrySet())
			names.put(entry.getValue(), "c" + circuit.getNodeID(breezeComponent) + "_" + entry.getKey());

		for(Entry<Handshake, HandshakeComponent> entry : breezeComponent.getHandshakeComponents().entrySet())
		{
			Connection connection = circuit.getConnection(entry.getValue());
			if(connection != null)
				names.put(entry.getKey(), "cn_" + circuit.getNodeID(connection));
		}
		names.put(breezeComponent.getUnderlyingComponent(), "c" + circuit.getNodeID(breezeComponent));

		HandshakeNameProvider nameProvider = new HandshakeNameProvider()
		{
			public String getName(Object handshake) {
				String result = names.get(handshake);
				if(result == null)
					throw new IndexOutOfBoundsException("No name found for the given handshake");
				return result;
			}
		};
		return nameProvider;
	}

	public String getDescription() {
		return "STG using "+protocolName+" protocol (.g)";
	}

	public String getExtenstion() {
		return ".g";
	}

	public boolean isApplicableTo(Model model) {
		return model instanceof BalsaCircuit;
	}

}