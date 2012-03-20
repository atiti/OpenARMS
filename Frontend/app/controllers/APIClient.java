package controllers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import play.Logger;
import play.Play;
import play.mvc.Controller;
import api.helpers.GsonHelper;
import api.requests.Request;
import api.responses.Response;

public class APIClient extends Controller {
	
	public static APIClient singleton;
	
	public HttpHost host;
	
	/**
	 * Constructing a new API client, try to reuse these.
	 */
	public APIClient() {
		this(Play.configuration.getProperty("openarms.service_host"), Integer.parseInt(Play.configuration.getProperty("openarms.service_port")));
	}
	
	public APIClient(String hostname, int port) {
		host = new HttpHost(hostname, port);
	}
	
	public static APIClient getInstance() {
		if(singleton == null) {
			singleton = new APIClient();
		}
		return singleton;
	}
	
	private HttpRequestBase getBaseRequest(api.requests.Request request) throws Exception {
		HttpRequestBase httpRequest;
		if(request.getHttpMethod().equals(Request.Method.GET)) {
			httpRequest = new HttpGet();
		} else if(request.getHttpMethod().equals(Request.Method.POST)) {
			httpRequest = new HttpPost();
		} else if(request.getHttpMethod().equals(Request.Method.PUT)) {
			httpRequest = new HttpPut();
		} else if(request.getHttpMethod().equals(Request.Method.DELETE)) {
			httpRequest = new HttpDelete();
		} else {
			throw new Exception("Unknown HTTP-method of the API-request.");
		}
		return httpRequest;
	}
	
	private Response sendRequest(Request request) throws Exception {
		DefaultHttpClient client = new DefaultHttpClient();
		
		String json = GsonHelper.toJson(request);
		HttpRequestBase httpRequest = getBaseRequest(request);
		ByteArrayEntity bae = new ByteArrayEntity(json.getBytes());
		
		// Adding the payload
		if(httpRequest instanceof HttpPost) {
			((HttpPost) httpRequest).setEntity(bae);
		} else if(httpRequest instanceof HttpPut) {
			((HttpPut) httpRequest).setEntity(bae);
		}
		// Setting the URI.
		httpRequest.setURI(URI.create(request.getURL()));
		
		// TODO: Remember to set the encoding of the request.
		Logger.debug("APIClient sends: %s to %s", json, host.toString());
		
		HttpResponse httpResponse = client.execute(host, httpRequest);
		HttpEntity httpResponseEntity = httpResponse.getEntity();
		// Check the response content-type.
		if(httpResponseEntity.getContentType().getValue().startsWith("application/json")) {
			BufferedReader br = new BufferedReader(new InputStreamReader(httpResponseEntity.getContent()));
			String responseJson = "";
			String line;
			while ((line = br.readLine()) != null) {
				responseJson += line;
				Logger.debug("APIClient receives: %s", line);
			}
			Response response = GsonHelper.fromJson(responseJson, request.getExpectedResponseClass());
			response.statusCode = httpResponse.getStatusLine().getStatusCode();
			return response;
		} else {
			throw new Exception("Http response didn't have the application/json content-type.");
		}
	}
	
	public static Response send(Request request) throws Exception {
		return getInstance().sendRequest(request);
	}
}
