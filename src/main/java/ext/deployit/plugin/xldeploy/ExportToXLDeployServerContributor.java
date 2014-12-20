/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.plugin.xldeploy;

import java.util.ArrayList;
import java.util.List;

import com.xebialabs.deployit.plugin.api.deployment.planning.Contributor;
import com.xebialabs.deployit.plugin.api.deployment.planning.DeploymentPlanningContext;
import com.xebialabs.deployit.plugin.api.deployment.specification.Delta;
import com.xebialabs.deployit.plugin.api.deployment.specification.Deltas;
import com.xebialabs.deployit.plugin.api.deployment.specification.Operation;
import com.xebialabs.deployit.plugin.api.reflect.Type;
import com.xebialabs.deployit.plugin.api.udm.Deployed;

public class ExportToXLDeployServerContributor {
	
	@Contributor
	public void exportDarAndPushToServer(Deltas deltas, DeploymentPlanningContext ctx) {
		List<Deployed<?, ?>> newOrModifiedProjectBundles = getNewOrModifiedProjectBundlesDeployed(deltas);
		
		for (Deployed<?, ?> projectBundle : newOrModifiedProjectBundles) {
			ctx.addStep(new ExportDarAndPushToServerStep(projectBundle));
		}
	}
	private List<Deployed<?, ?>> getNewOrModifiedProjectBundlesDeployed(Deltas deltas) {
		List<Deployed<?, ?>> newOrModifiedProjectBundles = new ArrayList<Deployed<?, ?>>();
		for (Delta delta : deltas.getDeltas()) {
			Type actualDeployedType = getTypeOfDeployedOrPrevious(delta);
			if (Type.valueOf("xldeploy.DeployedDarPackage").equals(actualDeployedType) && operationIsCreateOrModifyOrNoop(delta)
					&& Type.valueOf("xldeploy.Server").equals(getDeployedOrPrevious(delta).getContainer().getType())) {
				newOrModifiedProjectBundles.add(getDeployedOrPrevious(delta));
			}
		}
		return newOrModifiedProjectBundles;
	}
	
	private boolean operationIsCreateOrModifyOrNoop(Delta delta) {
		return delta.getOperation() == Operation.CREATE || delta.getOperation() == Operation.MODIFY || delta.getOperation() == Operation.NOOP;
	}
	
	private Deployed<?, ?> getDeployedOrPrevious(Delta delta) {
		if (delta.getOperation() == Operation.CREATE || delta.getOperation() == Operation.MODIFY || delta.getOperation() == Operation.NOOP) {
			return delta.getDeployed();
		} else {
			return delta.getPrevious();
		}
	}
	
	private Type getTypeOfDeployedOrPrevious(Delta delta) {
		return getDeployedOrPrevious(delta).getType();
	}
	
	/*
	@Contributor
	public void removeDarFromServer(Deltas deltas, DeploymentPlanningContext ctx) {
		List<Deployed<?, ?>> tobeDestroyedProjectBundles = getToBeDestroyedProjectBundlesDeployedToEsa(deltas);
		for (Deployed<?, ?> projectBundle : tobeDestroyedProjectBundles) {
			ctx.addStep(new RemoveDarFromServerStep(projectBundle));
		}
	}

	private List<Deployed<?, ?>> getToBeDestroyedProjectBundlesDeployedToEsa(Deltas deltas) {
		List<Deployed<?, ?>> toBeDestroyedProjectBundles = new ArrayList<Deployed<?, ?>>();
		for (Delta delta : deltas.getDeltas()) {
			Type actualDeployedType = getTypeOfDeployedOrPrevious(delta);
			if (Type.valueOf("xldeploy.DeployedDarPackage").equals(actualDeployedType) && Type.valueOf("xldeploy.Server").equals(getDeployedOrPrevious(delta).getContainer().getType()) && delta.getOperation()==Operation.DESTROY) {
				toBeDestroyedProjectBundles.add(getDeployedOrPrevious(delta));
			}
		}
		return toBeDestroyedProjectBundles;
	}
	*/
}
