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

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import static ext.deployit.plugin.xldeploy.HttpClientUtils.*;

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
		String server = projectBundle.getContainer().getProperty("serverAddress");
		return "Removing dar from " + server;
	}

	public StepExitCode execute(ExecutionContext ctx) throws Exception {
		boolean useHttps = projectBundle.getContainer().getProperty("useHttps");
		boolean ignoreSSLWarnings = projectBundle.getContainer().getProperty("ignoreSSLWarnings");
		String server = projectBundle.getContainer().getProperty("serverAddress");
		int port = projectBundle.getContainer().getProperty("serverPort");
		String username = projectBundle.getContainer().getProperty("username");
		String password = projectBundle.getContainer().getProperty("password");
		String protocol = useHttps ? "https" : "http";
		String deployableId = projectBundle.getDeployable().getId();
		String packageId = deployableId.substring(0, deployableId.lastIndexOf("/"));

		ctx.logOutput("Removing " + packageId + " from " + server);

		try (
				CloseableHttpClient httpclient = createClient(useHttps, ctx, ignoreSSLWarnings);
				CloseableHttpResponse response = httpclient.execute(
						createDeleteRequest(ctx, protocol, server, port, packageId),
						createAuthenticatedContext(username, password, protocol, server, port)
				)
		) {
			ctx.logOutput("----------------------------------------");
			ctx.logOutput(response.getStatusLine().toString());
			HttpEntity resEntity = response.getEntity();
			if (resEntity != null) {
				String responseString = EntityUtils.toString(resEntity);
				ctx.logOutput("Response: " + responseString);
			}
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_NO_CONTENT) {
				ctx.logError("DAR removal was unsuccessful");
				return StepExitCode.FAIL;
			}
			EntityUtils.consume(resEntity);
		} catch (Exception e) {
			ctx.logError("Caught exception in removing DAR from server.", e);
			return StepExitCode.FAIL;
		}
		ctx.logOutput("DAR removed successfully");
		return StepExitCode.SUCCESS;
	}

	private HttpDelete createDeleteRequest(final ExecutionContext ctx, final String protocol, final String server, final int port, final String packageId) throws URISyntaxException {
		URI uri = new URI(protocol, null, server, port, "/deployit/repository/ci/" + packageId, null, null);
		HttpDelete httpDelete = new HttpDelete(uri);
		ctx.logOutput("Executing request " + httpDelete.getRequestLine());
		return httpDelete;
	}
}
