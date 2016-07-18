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
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

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
		boolean ensureSamePath = projectBundle.getContainer().getProperty("ensureSamePath");
		String server = projectBundle.getContainer().getProperty("serverAddress");
		int port = projectBundle.getContainer().getProperty("serverPort");
		String username = projectBundle.getContainer().getProperty("username");
		String password = projectBundle.getContainer().getProperty("password");
		String protocol ="http";
		String deployableId = projectBundle.getDeployable().getId();
		String packageId = deployableId.substring(0, deployableId.lastIndexOf("/"));

		ctx.logOutput("Removing " + packageId + " from " + server);

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(server, port), new UsernamePasswordCredentials(username, password));
		CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		try {
			if (useHttps) {
				protocol ="https";

				if (!ignoreSSLWarnings && System.getProperty("javax.net.ssl.trustStore")==null) {
					ctx.logError("No truststore defined, requiring remotely signed host. Otherwise enable ignore SSL warning setting.");
				} else {
					SSLConnectionSocketFactory sslsf = null;
					KeyStore trustStore = null;
					TrustStrategy trustStrategy = null;

					if (ignoreSSLWarnings) {
						trustStrategy = new TrustSelfSignedStrategy();
					} else {
						String trustStoreFile = System.getProperty("javax.net.ssl.trustStore").toString();
						String trustStorePW = System.getProperty("javax.net.ssl.trustStorePassword").toString();
						trustStore = KeyStore.getInstance("jks");
						trustStore.load(new FileInputStream(new File(trustStoreFile)), trustStorePW.toCharArray());
					}

					SSLContext sslContext = SSLContexts.custom().useProtocol("TLS").loadTrustMaterial(trustStore, trustStrategy).build();
					sslsf = new SSLConnectionSocketFactory(sslContext);

					httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).setDefaultCredentialsProvider(credsProvider).build();
				}
			}

			URI uri = new URI(protocol, null, server, port, "/deployit/repository/ci/" + packageId, null, null);
			HttpDelete httpDelete = new HttpDelete(uri);
			ctx.logOutput("Executing request " + httpDelete.getRequestLine());
			CloseableHttpResponse response = httpclient.execute(httpDelete);
			try {
				ctx.logOutput("----------------------------------------");
				ctx.logOutput(response.getStatusLine().toString());
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					String responseString = EntityUtils.toString(resEntity);
					ctx.logOutput("Response: " + responseString);
				}
				if (response.getStatusLine().getStatusCode() != 204) {
					ctx.logError("DAR removal was unsuccessful");
					return StepExitCode.FAIL;
				}
				EntityUtils.consume(resEntity);
			} finally {
				response.close();
			}
		} catch (Exception e) {
			ctx.logError("Caught exception in removing DAR from server.", e);
			return StepExitCode.FAIL;
		} finally {
			httpclient.close();
		}
		ctx.logOutput("DAR removed successfully");
		return StepExitCode.SUCCESS;
	}
}
