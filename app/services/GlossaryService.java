package services;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.jsslutils.extra.apachehttpclient.SslContextedSecureProtocolSocketFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * This example shows how to work with the
 * IBM InfoSphere Information Governance Catalog REST API
 * In particular it shows how to:
 * 1. Create an Apache Http client
 * 2. Create a GET request for reading the glossary resource
 * 3. Parse the XML response using JDOM to read the top level categories of the
 *    glossary
 * 4. Loop over the top level categories - retrieve the full content of each
 *    category and update its short description
 */
public class GlossaryService {

    public static final String GLOSSARY_URI = "bgrestapi/glossary";

//    static final String HOST = "10.64.116.72";
//    static final String HOST = "119.81.7.66";
    static final String HOST = "services.snbc.io";
    static final int PORT = 9445;
    static final String USER = "isadmin";
    static final String PASS = "Nbc_iis1234";

    public static List<String> findAll() throws Exception {

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(new KeyManager[0], new TrustManager[]{new DefaultTrustManager()}, new SecureRandom());
        SSLContext.setDefault(ctx);

        //
        // 1. Create the HttpClient
        //
        HttpClient client = new HttpClient();

        SslContextedSecureProtocolSocketFactory secureProtocolSocketFactory =
                new SslContextedSecureProtocolSocketFactory(ctx);

        Protocol.registerProtocol("https", new Protocol("https",
                (ProtocolSocketFactory)secureProtocolSocketFactory, 443));

        // Set the Http Basic Authentication Credentials
        client.getParams().setAuthenticationPreemptive(true);
        client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        Credentials creds = new UsernamePasswordCredentials(USER, PASS);
        client.getState().setCredentials(new AuthScope(HOST, PORT, AuthScope.ANY_REALM), creds);

        //
        // 2. Create and execute a Get Method with the URI of the Glossary Resource
        //
        GetMethod getGlossaryMethod = new GetMethod("https://" + HOST + ":" + PORT + "/" + GLOSSARY_URI);
        ByteArrayOutputStream xmlResponse = new ByteArrayOutputStream();
        executeMethod(getGlossaryMethod, client, xmlResponse);

        List<String> results = new ArrayList<>();

        String header = "Term,Category,Description";
        System.out.println(header);
        results.add(header);

        //
        // 3. Parse the XML using jdom
        //
        try {
            Document doc = new SAXBuilder().build(new ByteArrayInputStream(xmlResponse.toByteArray()));
            Element root = doc.getRootElement();

            final Namespace ns = root.getNamespace();

            Element topLevelCategoriesElement = root.getChild("topLevelCategories", ns);
            List topLevelCategoryElements = topLevelCategoriesElement.getChildren("category", ns);

            if (topLevelCategoryElements == null || topLevelCategoryElements.size() == 0) {
                System.out.println("There are no top-level categories");
                return null;
            }

//            System.out.println("There are " + topLevelCategoryElements.size() + " top-level categories");

            // Iterate over the top-level categories
            // Retrieve the full XML representation of each category, edit its short
            //  description and save it.
            for (Iterator it = topLevelCategoryElements.iterator(); it.hasNext(); ) {
                Element topLevelCategoryElement = (Element) it.next();

                // Read the top-level category
                String uri = topLevelCategoryElement.getChildText("uri", ns);
                GetMethod getCategoryMethod = new GetMethod("https://" + HOST + ":" + PORT + "/" + uri);

                // Execute the get method to retrieve the top level category
                ByteArrayOutputStream categoryXml = new ByteArrayOutputStream();
                executeMethod(getCategoryMethod, client, categoryXml);

                // Parse the category XML and update its short description
                Document categoryDoc = new SAXBuilder().build(new ByteArrayInputStream(categoryXml.toByteArray()));
                Element categoryResourceRoot = categoryDoc.getRootElement().getChild("category", ns);

                String category = categoryResourceRoot.getChildText("name", ns);
//                String categoryRow = "category," + categoryResourceRoot.getChildText("name", ns);
//                System.out.println(categoryRow);
//                results.add(categoryRow);

                Element containedTerms = categoryResourceRoot.getChild("containedTerms", ns);

                for (Object obj : containedTerms.getChildren("term", ns)) {
                    Element term = (Element)obj;
                    String termName = term.getChildText("name", ns);

                    String termUri = term.getChildText("uri", ns);
                    GetMethod getTermMethod = new GetMethod("https://" + HOST + ":" + PORT + "/" + termUri);

                    ByteArrayOutputStream termXml = new ByteArrayOutputStream();
                    executeMethod(getTermMethod, client, termXml);

                    Document termDoc = new SAXBuilder().build(new ByteArrayInputStream(termXml.toByteArray()));
                    Element termResourceRoot = termDoc.getRootElement().getChild("term", ns);

                    String shortDescription = termResourceRoot.getChildText("shortDescription", ns);

                    String termRow = String.format("%s,%s,%s", termName, category, shortDescription);
                    System.out.println(termRow);
                    results.add(termRow);
                }

                /*
                String curShortDescription = categoryResourceRoot.getChildText("shortDescription", ns);
                String newShortDescription = "<<<EXAMPLE " + curShortDescription + " EXAMPLE>>>";

                System.out.println("--> Current short description: " + curShortDescription);
                System.out.println("--> Updated short description: " + newShortDescription);
                categoryResourceRoot.getChild("shortDescription", ns).setText(newShortDescription);

                // Generate the XML for the update request
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                new XMLOutputter().output(categoryDoc, os);

                // Create the update method for the category instance
                PutMethod updateMethod = new PutMethod("https://" + HOST + ":" + PORT + "/" + uri);
                updateMethod.setRequestEntity(new StringRequestEntity(os.toString(),
                        "application/xml;charset=UTF-8", "UTF-8"));
                executeMethod(updateMethod, client, null);
                */
            }

        } catch (Exception e) {
            System.err.println("Failed to parse the XML response.");
            e.printStackTrace();
        }
        return results;
    }

    public static void executeMethod(HttpMethod method, HttpClient httpClient, ByteArrayOutputStream output) {

        // Provide a custom retry handler (required)
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(5, false));
        try {
            // Execute the method
            int statusCode = httpClient.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                System.err.println(method.getPath() + " failed: status-code="
                        + statusCode + "; " + method.getStatusText());
                throw new HttpException("Error occurred while executing the http request: ["
                        + statusCode + "]: " + method.getStatusText());
            }

            // return the response stream
            InputStream response = method.getResponseBodyAsStream();
            if (output != null) {
                final int BUF_SIZE = 1000;
                byte[] buf = new byte[BUF_SIZE];
                int len;
                while ((len = response.read(buf)) != -1) {
                    output.write(buf, 0, len);
                }
            }
        } catch (HttpException e) {
            System.err.println("Fatal protocol violation: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Fatal transport error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            method.releaseConnection();
        }
    }

    private static class DefaultTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            //return new X509Certificate[0];
            return null;
        }
    }
}