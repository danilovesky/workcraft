package org.workcraft.plugins.mpsat.tools;

import org.workcraft.Framework;
import org.workcraft.Tool;
import org.workcraft.plugins.mpsat.MpsatMode;
import org.workcraft.plugins.mpsat.MpsatSettings;
import org.workcraft.plugins.mpsat.MpsatUtilitySettings;

public class MpsatUscChecker extends AbstractMpsatChecker implements Tool {

	public MpsatUscChecker(Framework framework) {
		super(framework);
	}

	@Override
	public String getDisplayName() {
		return "Check for USC [MPSat]";
	}

	@Override
	public MpsatSettings getSettings() {
		return new MpsatSettings(MpsatMode.STG_REACHABILITY, 0,
				MpsatUtilitySettings.getSolutionMode(), MpsatUtilitySettings.getSolutionCount(),
				MpsatSettings.reachUsc);
	}

}