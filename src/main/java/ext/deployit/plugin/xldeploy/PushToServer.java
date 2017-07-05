/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.plugin.xldeploy;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;
import com.xebialabs.deployit.plugin.api.flow.StepExitCode;
import com.xebialabs.overthere.local.LocalFile;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import static ext.deployit.plugin.xldeploy.HttpClientUtils.*;

@SuppressWarnings("serial")
public class PushToServer {

	public StepExitCode execute(ExecutionContext ctx, String appId, LocalFile exportedDar, String server, int port, String username, String password, String protocol, boolean ignoreSSLWarnings, boolean ensureSamePath, boolean useHttps) throws Exception {
		try (CloseableHttpClient httpclient = createClient(useHttps, ctx, ignoreSSLWarnings)) {

			HttpClientContext clientContext = createAuthenticatedContext(username, password, protocol, server, port);

			if (ensureSamePath) {
				try (CloseableHttpResponse response = httpclient.execute(
						createGetRequest(ctx, protocol, server, port, appId),
						clientContext
				)) {
					if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
						ctx.logError("Existence of package path [" + appId + "] could not be determined on target instance.");
						ctx.logError("Target instance returned HTTP Response: " + response.getStatusLine().getStatusCode());
						ctx.logError(response.getStatusLine().getReasonPhrase());
						return StepExitCode.FAIL;
					}
				}
			}

			try (CloseableHttpResponse response = httpclient.execute(
					createPostRequest(ctx, protocol, server, port, exportedDar),
					clientContext
			)) {
				ctx.logOutput("----------------------------------------");
				ctx.logOutput(response.getStatusLine().toString());
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					String responseString = EntityUtils.toString(resEntity);
					ctx.logOutput("Response: " + responseString);
					if (responseString.toLowerCase().contains("already imported")) return StepExitCode.SUCCESS;
				}
				if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
					ctx.logError("DAR transfer was unsuccessful");
					return StepExitCode.FAIL;
				}
				EntityUtils.consume(resEntity);
			}
		} catch (Exception e) {
			ctx.logError("Caught exception in uploading DAR to server.", e);
			return StepExitCode.FAIL;
		}

		ctx.logOutput("DAR transfer completed successfully");
		return StepExitCode.SUCCESS;
	}

	private HttpPost createPostRequest(final ExecutionContext ctx, final String protocol, final String server, final int port, final LocalFile exportedDar) throws URISyntaxException {
		URI postUri = new URI(protocol, null, server, port, "/deployit/package/upload/Package.dar", null, null);
		HttpPost httppost = new HttpPost(postUri.toString());
		File darFile = exportedDar.getFile();

		ctx.logOutput("Uploading file: " + darFile.getAbsolutePath());

		FileBody bin = new FileBody(darFile, ContentType.MULTIPART_FORM_DATA);

		HttpEntity reqEntity = MultipartEntityBuilder.create().addPart("fileData", bin).build();

		httppost.setEntity(reqEntity);

		ctx.logOutput("Executing request " + httppost.getRequestLine());
		return httppost;
	}

	private HttpGet createGetRequest(final ExecutionContext ctx, final String protocol, final String server, final int port, final String appId) throws URISyntaxException {
		URI getUri = new URI(protocol, null, server, port, "/deployit/repository/ci/" + appId, null, null);
		String endpoint = getUri.toString();
		ctx.logOutput("Checking existence of package path [" + appId + "] with URL: " + endpoint);
		return new HttpGet(endpoint);
	}

}
