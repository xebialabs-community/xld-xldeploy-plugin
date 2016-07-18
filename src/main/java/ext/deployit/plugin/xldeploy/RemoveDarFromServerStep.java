/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.plugin.xldeploy;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.deployit.plugin.api.flow.Step;
import com.xebialabs.deployit.plugin.api.flow.StepExitCode;
import com.xebialabs.deployit.plugin.api.udm.Deployed;

@SuppressWarnings("serial")
public class RemoveDarFromServerStep implements Step {
	private Deployed<?, ?> projectBundle;

	public RemoveDarFromServerStep(Deployed<?, ?> projectBundle) {
		this.projectBundle = projectBundle;
	}

	public int getOrder() {
		return 50;
	}

	public String getDescription() {
		String host = projectBundle.getContainer().getProperty("serverAdress");
		return "Removing dar from " + host;
	}

	public StepExitCode execute(ExecutionContext ctx) throws Exception {
		//Initiate FTP parameters
		String host = projectBundle.getContainer().getProperty("serverAdress");
		ctx.logOutput("Removing dar from " + host);

		return StepExitCode.SUCCESS;
	}
}
