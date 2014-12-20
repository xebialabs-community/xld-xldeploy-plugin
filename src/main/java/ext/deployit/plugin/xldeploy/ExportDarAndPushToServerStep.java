/**
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS
 * FOR A PARTICULAR PURPOSE. THIS CODE AND INFORMATION ARE NOT SUPPORTED BY XEBIALABS.
 */
package ext.deployit.plugin.xldeploy;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

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
			String server = projectBundle.getContainer().getProperty(
					"serverAddress");
			int port = projectBundle.getContainer().getProperty("serverPort");
			String username = projectBundle.getContainer().getProperty(
					"username");
			String password = projectBundle.getContainer().getProperty(
					"password");

			DefaultHttpClient httpclient = null;
			try {
				if (useHttps) {
			    	if(!ignoreSSLWarnings && System.getProperty("javax.net.ssl.trustStore")==null){
			    		ctx.logError("No truststore defined, requiring remotely signed host. Otherwise enable ignore SSL warning setting.");
			    		httpclient = new DefaultHttpClient();
			    	}
			    	else{					
					    SSLContext sslCtx = SSLContext.getInstance("TLS");
					    TrustManager[] trustManagers = null;
					    SSLSocketFactory ssf = null;
					    ClientConnectionManager ccm;
					    
					    if(ignoreSSLWarnings){
							X509TrustManager tm = new X509TrustManager() {
								public void checkClientTrusted(X509Certificate[] xcs,
										String string) throws CertificateException {
								}
								public void checkServerTrusted(X509Certificate[] xcs,
										String string) throws CertificateException {
								}
								public X509Certificate[] getAcceptedIssuers() {
									return null;
								}
							};
							trustManagers = new TrustManager[] { tm };
							httpclient = new DefaultHttpClient();
							sslCtx.init(null, trustManagers, null);
							ssf = new SSLSocketFactory(sslCtx,
									SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
							ccm = httpclient.getConnectionManager();						
					    }
					    else{
					    	String trustStoreFile = System.getProperty("javax.net.ssl.trustStore").toString();
					    	String trustStorePW = System.getProperty("javax.net.ssl.trustStorePassword").toString();
						    KeyStore trustStore = KeyStore.getInstance("jks");
						    trustStore.load(new FileInputStream(new File(trustStoreFile)), trustStorePW.toCharArray());
						    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
						    tmf.init(trustStore);
						    trustManagers = tmf.getTrustManagers();
						    sslCtx.init(null, trustManagers, new SecureRandom());
						    ssf = new SSLSocketFactory(sslCtx);
						    httpclient = new DefaultHttpClient();
						    ccm = httpclient.getConnectionManager();
						}
	
						SchemeRegistry sr = ccm.getSchemeRegistry();
						sr.register(new Scheme("https", port, ssf));
						
						httpclient = new DefaultHttpClient(ccm, httpclient.getParams());
			    	}
				}
				else httpclient = new DefaultHttpClient();

				Credentials credentials = new UsernamePasswordCredentials(
						username, password);
				httpclient.getCredentialsProvider().setCredentials(
						new AuthScope(server, port), credentials);
				String protocol ="http://";
				if(useHttps) protocol ="https://";
				HttpPost httppost = new HttpPost(protocol + server + ":"
						+ port + "/deployit/package/upload/Package.dar");
				File darFile = exportedDar.getFile();

				MultipartEntity multiPartEntity = new MultipartEntity();

				FileBody fileBody = new FileBody(darFile,
						"application/octect-stream");
				multiPartEntity.addPart("fileData", fileBody);

				httppost.setEntity(multiPartEntity);

				ctx.logOutput("Uploading file: " + darFile.getAbsolutePath());

				ctx.logOutput("Executing request " + httppost.getRequestLine());
				HttpResponse response = httpclient.execute(httppost);
				ctx.logOutput("----------------------------------------");
				ctx.logOutput(response.getStatusLine().toString());
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					//ctx.logOutput("Response content length: " + resEntity.getContentLength());
					String responseString = EntityUtils.toString(resEntity);
					ctx.logOutput("Response: " + responseString);
					if(responseString.toLowerCase().contains("already imported")) return StepExitCode.SUCCESS;
				}
				if (!response.getStatusLine().getReasonPhrase().equals("OK")) {
					ctx.logError("DAR transfer was unsuccessful");
					return StepExitCode.FAIL;
				}
				EntityUtils.consume(resEntity);
			} catch (Exception e) {
				ctx.logError("Caught exception in uploading DAR to server.", e);
				return StepExitCode.FAIL;
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
