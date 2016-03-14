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
import com.xebialabs.deployit.repository.RepositoryService;
import com.xebialabs.deployit.repository.RepositoryServiceHolder;
import com.xebialabs.deployit.repository.WorkDir;
import com.xebialabs.deployit.service.version.exporter.ExporterService;
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overthere.local.LocalConnection;
import com.xebialabs.overthere.local.LocalFile;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyStore;

import static javatests.TestSupport.assertThat;

@SuppressWarnings("serial")
public class ExportDarAndPushToServerStep implements Step {

	private Deployed<?, ?> projectBundle;

	public ExportDarAndPushToServerStep(Deployed<?, ?> projectBundle) {
		this.projectBundle = projectBundle;
	}

	public int getOrder() {
		return 50;
	}

	public String getDescription() {
		String address = projectBundle.getContainer().getProperty(
				"serverAddress");
		String port = projectBundle.getContainer().getProperty("serverPort")
				.toString();
		return "Transfer deployment package to XL Deploy server [" + address
				+ ":" + port + "]";
	}

	public StepExitCode execute(ExecutionContext ctx) throws Exception {
		String bundleId = projectBundle.getDeployable().getId();
		String packageId = getParentId(bundleId);

		// String version = packageId.substring(packageId.lastIndexOf("/")+1,
		// packageId.length());
		// projectBundle.setId(projectBundle.getId()+"-"+version);

		OverthereConnection localConnection = null;
		File createdTmpDir = null;
		try {
			// Export the package dar
			ctx.logOutput("Exporting: " + packageId);
			localConnection = (LocalConnection) LocalConnection
					.getLocalConnection();
			RepositoryService repositoryService = RepositoryServiceHolder
					.getRepositoryService();
			ExporterService exportService = new ExporterService(
					repositoryService);

			OverthereFile workingDirectory = localConnection
					.getWorkingDirectory();
			if (workingDirectory == null) {
				createdTmpDir = createTmpDir(localConnection);
				workingDirectory = new LocalFile(
						(LocalConnection) localConnection, createdTmpDir);
				localConnection.setWorkingDirectory(workingDirectory);
			}
			WorkDir workDir = new WorkDir((LocalFile) workingDirectory);
			LocalFile exportedDar = exportService.exportDar(packageId, workDir);
			ctx.logOutput("Completed export to file: "
					+ exportedDar.getFile().getName());
			// exportedDar.copyTo(workingDirectory);

			// Connect to XL Deploy instance
			boolean useHttps = projectBundle.getContainer().getProperty("useHttps");
			boolean ignoreSSLWarnings = projectBundle.getContainer().getProperty("ignoreSSLWarnings");
			boolean ensureSamePath = projectBundle.getContainer().getProperty("ensureSamePath");

			String server = projectBundle.getContainer().getProperty(
					"serverAddress");
			int port = projectBundle.getContainer().getProperty("serverPort");
			String username = projectBundle.getContainer().getProperty(
					"username");
			String password = projectBundle.getContainer().getProperty(
					"password");
            String protocol ="http";

            //DefaultHttpClient httpclient = null;
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(server, port),
                    new UsernamePasswordCredentials(username, password));
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setDefaultCredentialsProvider(credsProvider)
                    .build();
			try {
				if (useHttps) {
                    protocol ="https";

			    	if(!ignoreSSLWarnings && System.getProperty("javax.net.ssl.trustStore")==null){
			    		ctx.logError("No truststore defined, requiring remotely signed host. Otherwise enable ignore SSL warning setting.");
			    		//httpclient = new DefaultHttpClient();
			    	}
			    	else{
                        SSLConnectionSocketFactory sslsf = null;
                        KeyStore trustStore = null;
                        TrustStrategy trustStrategy = null;

                        if(ignoreSSLWarnings){
                            trustStrategy = new TrustSelfSignedStrategy();
					    }
					    else{
					    	String trustStoreFile = System.getProperty("javax.net.ssl.trustStore").toString();
					    	String trustStorePW = System.getProperty("javax.net.ssl.trustStorePassword").toString();
                            trustStore = KeyStore.getInstance("jks");
						    trustStore.load(new FileInputStream(new File(trustStoreFile)), trustStorePW.toCharArray());
						}

                        SSLContext sslContext = SSLContexts.custom()
                                .useProtocol("TLS")
                                .loadTrustMaterial(trustStore, trustStrategy)
                                .build();
                        sslsf = new SSLConnectionSocketFactory(sslContext);

                        httpclient = HttpClients.custom()
                                .setSSLSocketFactory(sslsf)
                                .setDefaultCredentialsProvider(credsProvider)
                                .build();
			    	}
				}

                if(ensureSamePath){
                    String appId = getParentId(packageId);
					URI getUri = new URI(
							protocol,
                            null,
							server,
                            port,
							"/deployit/repository/ci/" + appId,
							null, null);
					String endpoint = getUri.toString();
                    //String endpoint = protocol + "://" + server + ":" + port + "/deployit/repository/ci/" + URLEncoder.encode(appId, java.nio.charset.StandardCharsets.UTF_8.toString());
                    ctx.logOutput("Checking existence of package path [" + appId + "] with URL: " + endpoint);
					HttpGet httpGet = new HttpGet(endpoint);
                    CloseableHttpResponse response = httpclient.execute(httpGet);
                    if(response.getStatusLine().getStatusCode()!=200){
						ctx.logError("Existence of package path [" + appId + "] could not be determined on target instance.");
						ctx.logError("Target instance returned HTTP Response: " + response.getStatusLine().getStatusCode());
						ctx.logError(response.getStatusLine().getReasonPhrase());
                        return StepExitCode.FAIL;
                    }
				}
				URI postUri = new URI(
						protocol,
						null,
						server,
						port,
						"/deployit/package/upload/Package.dar",
						null, null);
				HttpPost httppost = new HttpPost(postUri.toString());
				File darFile = exportedDar.getFile();

				ctx.logOutput("Uploading file: " + darFile.getAbsolutePath());

				FileBody bin = new FileBody(darFile, ContentType.MULTIPART_FORM_DATA);

				HttpEntity reqEntity = MultipartEntityBuilder.create()
						.addPart("fileData", bin)
						.build();

				httppost.setEntity(reqEntity);

				ctx.logOutput("Executing request " + httppost.getRequestLine());
				CloseableHttpResponse response = httpclient.execute(httppost);
				try {
					ctx.logOutput("----------------------------------------");
					ctx.logOutput(response.getStatusLine().toString());
					HttpEntity resEntity = response.getEntity();
					if (resEntity != null) {
						String responseString = EntityUtils.toString(resEntity);
						ctx.logOutput("Response: " + responseString);
						if(responseString.toLowerCase().contains("already imported")) return StepExitCode.SUCCESS;
					}
					if(!response.getStatusLine().getReasonPhrase().equals("OK")) {
						ctx.logError("DAR transfer was unsuccessful");
						return StepExitCode.FAIL;
					}
					EntityUtils.consume(resEntity);
				} finally {
					response.close();
				}
			} catch (Exception e) {
				ctx.logError("Caught exception in uploading DAR to server.", e);
				return StepExitCode.FAIL;
            } finally {
                httpclient.close();
            }

		} catch (Exception e) {
			ctx.logError("Caught exception in uploading DAR to server.", e);
			return StepExitCode.FAIL;
		} finally {
			removeTmpDir(createdTmpDir);
		}
		ctx.logOutput("DAR transfer completed successfully");
		return StepExitCode.SUCCESS;
	}

	private String getParentId(String id) {
		return id.substring(0, id.lastIndexOf("/"));
	}

	private File createTmpDir(OverthereConnection localConnection) {
		String property = "java.io.tmpdir";
		String tempDir = System.getProperty(property);
		String pathSeparator = localConnection.getHostOperatingSystem()
				.getFileSeparator();
		String workDir = tempDir + pathSeparator + "work"
				+ System.currentTimeMillis();
		File workDirAsFile = new File(workDir);
		workDirAsFile.mkdir();
		return workDirAsFile;
	}

	private void removeTmpDir(File createdTmpDir) {
		if (createdTmpDir != null) {
			if (createdTmpDir.isFile()) {
				createdTmpDir.delete();
			} else if (createdTmpDir.isDirectory()) {
				File[] files = createdTmpDir.listFiles();
				if (files != null) {
					for (File file : files) {
						removeTmpDir(file);
					}
				}
				createdTmpDir.delete();
			}
		}
	}
}
