package ext.deployit.plugin.xldeploy;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import com.xebialabs.deployit.plugin.api.flow.ExecutionContext;

public abstract class HttpClientUtils {

	public static CloseableHttpClient createClient(final boolean useHttps, final ExecutionContext ctx, final boolean ignoreSSLWarnings) throws GeneralSecurityException, IOException {
		if (useHttps) {
			if (!ignoreSSLWarnings && System.getProperty("javax.net.ssl.trustStore") == null) {
				ctx.logError("No truststore defined, requiring remotely signed host. Otherwise enable ignore SSL warning setting.");
			} else {
				return createSecureClient(ignoreSSLWarnings);
			}
		}
		return HttpClients.custom().build();
	}

	public static CloseableHttpClient createSecureClient(final boolean ignoreSSLWarnings) throws GeneralSecurityException, IOException {
		SSLContextBuilder sslContextBuilder = SSLContexts.custom().useProtocol("TLS");

		if (ignoreSSLWarnings) {
			sslContextBuilder.loadTrustMaterial(new TrustSelfSignedStrategy());
		} else {
			String trustStoreFile = System.getProperty("javax.net.ssl.trustStore");
			char[] trustStorePW = System.getProperty("javax.net.ssl.trustStorePassword").toCharArray();
			sslContextBuilder.loadTrustMaterial(new File(trustStoreFile), trustStorePW);
		}
		SSLContext sslContext = sslContextBuilder.build();

		return HttpClients.custom().setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext)).build();
	}

	public static HttpClientContext createAuthenticatedContext(final String username, final String password, final String scheme, final String server, final int port) {
		HttpClientContext context = HttpClientContext.create();
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(server, port), new UsernamePasswordCredentials(username, password));
		context.setCredentialsProvider(credsProvider);
		BasicAuthCache authCache = new BasicAuthCache();
		authCache.put(new HttpHost(server, port, scheme), new BasicScheme());
		context.setAuthCache(authCache);
		return context;
	}
}
