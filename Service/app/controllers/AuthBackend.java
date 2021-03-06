package controllers;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import models.User;
import models.UserAuthBinding;
import play.Logger;
import play.Play;
import play.mvc.Http;
import play.mvc.Http.Header;
import api.requests.AuthenticateUserRequest;
import api.requests.GenerateAuthChallengeRequest;
import api.responses.GenerateAuthChallengeResponse;

/**
 * Abstract controller which specifies authentication method.
 * @author OpenARMS Service team
 */
public abstract class AuthBackend {
	/**
	 * Method that authorize the user to access the system.
	 * @return true if user authorized and false otherwise
	 * @throws Exception 
	 */
	public static User authenticate(AuthenticateUserRequest req) throws Exception {
		throw new Exception("Use a concrete AuthBackend to authenticate.");
	}
	
	/**
	 * Method that resets the password of the user and sends it to user via email.
	 * @throws Exception 
	 */
	public static void resetPassword() throws Exception {
		throw new Exception("This AuthBackend does not support resetting the password");
	}
	
	public static Class<? extends UserAuthBinding> getBindingClass() throws Exception {
		throw new Exception("This AuthBackend does not support resetting the password");
	}
	
	public static GenerateAuthChallengeResponse generateChallenge(GenerateAuthChallengeRequest req) throws Exception {
		throw new Exception("This AuthBackend does not support challenges");
	}
	
	public static Class<? extends GenerateAuthChallengeRequest> getChallengeRequestClass() throws Exception {
		throw new Exception("This AuthBackend does not support challenges");
	}
	
	public static Class<? extends GenerateAuthChallengeResponse> getChallengeResponseClass() throws Exception {
		throw new Exception("This AuthBackend does not support challenges");
	}

	public static Class<? extends AuthenticateUserRequest> getAuthenticateUserRequestClass() throws Exception {
		throw new Exception("This AuthBackend does not support authentication requests");
	}

	public static List<Class<? extends AuthBackend>> backends;
	/**
	 * Get a list of advailable authentication backends, based on the 'openarms.authentication_backends'
	 * parameter in the configuration files.
	 * Note: This method is a one-shot function, as it caches the result once it has parsed the configuration.
	 * @return A list of classes that all extend the AuthBackend class.
	 */
	public static List<Class<? extends AuthBackend>> advailableBackends() {
		if(backends == null) {
			backends = new LinkedList<Class<? extends AuthBackend>>();
			String backendsString = (String) Play.configuration.get("openarms.authentication_backends");
			if(!backendsString.isEmpty()) {
				for(String className: backendsString.split(",")) {
					try {
						Class<?> clazz = Play.classloader.loadClass(className);
						if(!AuthBackend.class.isAssignableFrom(clazz)) {
							System.err.println("Tried loading an authentication backend ("+className+") which is not a subclass of AuthBackend.");
						} else {
							backends.add((Class<? extends AuthBackend>) clazz);
						}
					} catch (ClassNotFoundException e) {
						System.err.println("Couldn't load the "+className+" authentication backend: "+e.getMessage());
					}
				}
			}
		}
		return backends;
	}

	/**
	 * Get the authentication backend class based on a string representation of the class.
	 * @param backend The string representation of the backend, fx: 'controllers.AuthBackend'
	 * @return The class that corresponds with the backend parameter specified as argument
	 * if the backend parameter does not match a valid authentication backend on the system
	 * null is returned.
	 */
	public static Class<? extends AuthBackend> getBackend(String backend) {
		for(Class clazz: AuthBackend.advailableBackends()) {
			if(clazz.getCanonicalName().equals(backend)) {
				return clazz;
			}
		}
		return null;
	}
	
	/**
	 * Get the user that is requesting, based on the HTTP authorization header.
	 * @return
	 */
	public static User getCurrentUser() {
		User u = null;		
		Header header = Http.Request.current().headers.get("authorization");
	    if (header != null) {
	    	String user = Http.Request.current().user;
	    	String secret = Http.Request.current().password;
	    	Logger.debug("getCurrentUser() called with user = %s and secret = %s.", user, secret);
	    	
	    	u = User.findById(Long.parseLong(user));
	    	if (u != null && u.secret != null && u.secret.equals(secret)) {
	    		return u;
	    	} else {
		    	Logger.debug("getCurrentUser() called but the user was not found or the secret was wrong ...");
	    		return null;
	    	}
	    } else {
	    	Logger.debug("getCurrentUser() called but HTTP authorization header not set.");
	    	return null;
	    }
	}
	
	/**
	 * This is secret generator, which generates long number converted to string.
	 * Need to decide the format and size of secret.
	 * @return secret converted to string
	 */
	public static String generateSecret() {
		String SECRET_CHARSET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
		Random random = new Random();
		String result = "";
		for (int i = 0; i < 50; i++) {
			result += SECRET_CHARSET.charAt(random.nextInt(SECRET_CHARSET.length()-1));
		}
		return result;
	} 
}
