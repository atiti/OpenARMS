package controllers;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.HttpStatus;

import play.mvc.Controller;
import play.mvc.Http;
import api.entities.ChoiceJSON;
import api.entities.PollInstanceJSON;
import api.entities.PollJSON;
import api.helpers.GsonHelper;
import api.requests.CreatePollInstanceRequest;
import api.requests.CreatePollRequest;
import api.responses.CreatePollInstanceResponse;
import api.responses.CreatePollResponse;

public class CreatePoll extends BaseController {

	public static void index() {
		if (!LoginUser.isLoggedIn()) {
			session.put("page_prior_to_login", request.url);
			LoginUser.showform(null);
		}
		render();
	}

	public static void success(String token) {
		render(token);
	}

	public static void submit(String question, String[] choices, String type, Boolean loginRequired) {
		// Validate that the question and answers are there.
		validation.required(question);
		validation.required(type);
		validation.required(choices);

		// This will be the poll that will be used to hold all values.
		PollJSON poll = new PollJSON();
		// Remove empty lines from the answers
		poll.choices = new LinkedList<ChoiceJSON>();
		if (choices != null) {
			for (String s: choices) {
				if (s != null && !s.isEmpty()) {
					ChoiceJSON choice = new ChoiceJSON();
					choice.text = s;
					poll.choices.add(choice);
				}
			}
		}
		// Change the params to reflect the json version of the submitted.
		params.put("choices[]", GsonHelper.toJson(choices));
		// Validate that we have at least 2 answer options.
		if (poll.choices.size() < 2) {
			validation.addError("choices[]", "validation.required.atLeastTwo");
		}
		
		// TODO: Check if the email provided belongs to an existing user
		// If yes: Redirect the user to the login page - save the poll somewhere temporarily.
		// If no: Create a new user and create the poll, and bind these two together.

		// Finally, we're ready to send it to the server, and see if it likes it or not.
		poll.question = question;
		
		// Default of loginRequired is false
		if (loginRequired == null)
			loginRequired = false;
		poll.loginRequired = loginRequired;
		
		// Default of multiple is false
		if (type.equals("multiple")) {
			poll.multipleAllowed = true;
		} else {
			poll.multipleAllowed = false;
		}

		if (!validation.hasErrors()) {
			try {
				CreatePollResponse response = (CreatePollResponse) APIClient.send(new CreatePollRequest(poll));
				poll = response.poll;
				if(response.statusCode == Http.StatusCode.CREATED && poll != null) {
					PollInstanceJSON pi = new PollInstanceJSON();
					pi.poll_id = response.poll.id;
					pi.start = new Date();
					pi.end = new Date();
					pi.end.setMinutes(pi.start.getMinutes() + 30);
					
					CreatePollInstanceResponse piresp = (CreatePollInstanceResponse) APIClient.send(new CreatePollInstanceRequest(pi));
					if (piresp.statusCode == Http.StatusCode.CREATED) {
						success(poll.token);
					} else {
						throw new Exception(piresp.error_message);
					}
				} else {
					throw new Exception("Something went wrong during Poll creation: "+response.error_message);
				}
			} catch (Exception e) {
				flash.error(e.getMessage());
				params.flash();
				validation.keep();
				index();
			}
		} else {
			params.flash();
			validation.keep();
			index();
		}
	}
}
