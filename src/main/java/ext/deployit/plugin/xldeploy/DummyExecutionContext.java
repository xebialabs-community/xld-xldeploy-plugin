/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.plugin.xldeploy;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.deployit.plugin.api.flow.ITask;
import com.xebialabs.deployit.plugin.api.inspection.InspectionContext;
import com.xebialabs.deployit.plugin.api.services.Repository;

public class DummyExecutionContext implements ExecutionContext {

	public void logOutput(String output) {
	}

	public void logError(String error) {
	}

	public void logError(String error, Throwable t) {
	}

	public Object getAttribute(String name) {
		return null;
	}

	public void setAttribute(String name, Object value) {
	}

	public Repository getRepository() {
		return null;
	}

	public InspectionContext getInspectionContext() {
		return null;
	}

	public ITask getTask() {
		return null;
	}

}
