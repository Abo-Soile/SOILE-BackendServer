package fi.abo.kogni.soile2.http_server.auth;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fi.abo.kogni.soile2.http_server.auth.SoileAuthorization.Roles;
import fi.abo.kogni.soile2.http_server.authentication.utils.UserUtils;
import fi.abo.kogni.soile2.utils.MessageResponseHandler;
import fi.abo.kogni.soile2.utils.SoileCommUtils;
import fi.abo.kogni.soile2.utils.SoileConfigLoader;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.validation.RequestParameters;
import io.vertx.ext.web.validation.ValidationHandler;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * Class that provides Authentication for token carrying requests.
 * 
 * @author Thomas Pfau
 *
 */
public class PasswordReset extends SoileAuthHandler{
	static final Logger LOGGER = LogManager.getLogger(PasswordReset.class);

	private MongoClient client;
	private Vertx vertx;
	private SoileCookieCreationHandler cookieHandler;
	private int resettime = 60 * 60 * 1000; // 1 hour ttl for reset tokens. 

	/**
	 * Default constructor
	 * @param client a Mongo client to retrieve data from the db
	 * @param tokenCreator a jwt Token Creator for auth purposes
	 * @param vertx the Vertx Instance
	 * @param cookieHandler a cookie handler for authentication purposes
	 */
	public PasswordReset(MongoClient client, JWTTokenCreator tokenCreator, Vertx vertx, SoileCookieCreationHandler cookieHandler) {
		super(cookieHandler, tokenCreator);
		this.client = client;
		this.vertx = vertx;
	}

	private Future<JsonObject> getUserData(String userNameOrEmail) {
		Promise<JsonObject> promise = Promise.<JsonObject>promise();
		UserUtils.getUserDataFromCollection(client, userNameOrEmail, res -> {
			res.onSuccess(userData -> {
				promise.complete(userData);
			})
					.onFailure(err -> promise.fail(err));
		});
		return promise.future();
	}

	private Future<Void> checkAndCleanUpTokenForUser(JsonObject userData)
	{
		Promise<Void> promise = Promise.<Void>promise();
		String tokenCollection = SoileConfigLoader.getdbProperty("tokenCollection"); 
		String username = userData.getString(SoileConfigLoader.getUserdbField("usernameField"));
		String usernameField = SoileConfigLoader.getUserdbField("usernameField");
		this.client.find(tokenCollection, new JsonObject().put(usernameField, username))
		.onSuccess(res -> {
			// a token already exists
			if(res.size() > 0)
			{				
				JsonObject item = res.get(0);
				if((System.currentTimeMillis() - item.getInteger("timestamp")) > resettime)
				{
					this.client.removeDocuments(tokenCollection, userData)
					.onSuccess(done -> promise.complete())
					.onFailure(err -> {
						LOGGER.error("Unable to clean up tokens");
						LOGGER.error(err);
						promise.fail(err);
					});
				}
				else {
					promise.fail("Token Already exists");
				}
				
			}
		});
		
		return promise.future();
	}
	
	private void generateUniqueToken(Promise<String> fut, JsonObject userData) {
		// First, check, if there is already a recent request for this user
		// we will not generate multiple tokens for a user, thus making sure 
		//that you cannot spam a user with reset mails 
		String username = userData.getString(SoileConfigLoader.getUserdbField("usernameField"));
		String usernameField = SoileConfigLoader.getUserdbField("usernameField");
		String tokenCollection = SoileConfigLoader.getdbProperty("tokenCollection"); 
		this.checkAndCleanUpTokenForUser(userData)
		.onSuccess(yay -> {
			java.security.SecureRandom random = new java.security.SecureRandom();
			byte[] tokenBytes = new byte[64];
			random.nextBytes(tokenBytes);
			String token = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);		
			JsonObject query = new JsonObject().put("token", token);		
			this.client.find(tokenCollection, query).onComplete(ar -> {
				if (ar.succeeded() && (ar.result() == null || ar.result().isEmpty())) {
					query.put(usernameField,username);
					query.put("timestamp", System.currentTimeMillis());
					client.save(tokenCollection, query)
							.onSuccess(done -> {
								fut.complete(token);
							})
							.onFailure(err -> {
								fut.fail(err);
							});
	
				} else {
					// Token exists, generate a new one and try again
					random.nextBytes(tokenBytes);
					generateUniqueToken(fut, userData);
				}
			});
		})
		.onFailure(err -> {
			LOGGER.error(err);
			fut.fail(err.getMessage());
		});
	}
	/**
	 * Test auth route
	 * @param ctx the routing context
	 */
	public void request_reset(RoutingContext ctx)
	{
		RequestParameters params = ctx.get(ValidationHandler.REQUEST_CONTEXT_KEY);		
		JsonObject body = params.body().getJsonObject();				
		String emailOrUserName = body.getString("emailOrUserName");
		createOneTimeLoginAndSendMail(emailOrUserName)
		.onFailure( err -> {
			LOGGER.error(err.getMessage());
			LOGGER.warn("Got a invalid request for a password reset for user " + emailOrUserName);
		}
		)
		.onComplete(done -> {
			// In any case we will respond with "request received" and not give out further information.
			ctx.response().putHeader("content-type","text/plain");
			ctx.response().end("Request received");
		});
				
	}	
	
	public Future<Void> createOneTimeLoginAndSendMail(String userNameOrEmail) {
		Promise<Void> promise = Promise.<Void>promise();
		Promise<String> token_promise = Promise.<String>promise();
		this.getUserData(userNameOrEmail)
				.onSuccess(userData -> {
					this.generateUniqueToken(token_promise, userData);
					token_promise.future().onSuccess(token -> {
						String email = SoileConfigLoader.getUserdbField("userEmailField");
						if (email == null) {
							promise.fail("No Email found");
							return;
						}
						sendMail(email, token)
								.onSuccess(res -> promise.complete())
								.onFailure(err -> promise.fail(err));
					})
							.onFailure(err -> promise.fail(err));
				})
				.onFailure(err -> promise.fail(err.getMessage()));
		return promise.future();
	}

	private Future<User> getUserForToken(String authToken) {

		Promise<User> userPromise = Promise.<User>promise();
		var query = new JsonObject().put("token", authToken);
		client.findOneAndDelete(SoileConfigLoader.getdbProperty("tokenCollection"), query)
				.onSuccess(res -> {
					String userName = res.getString(SoileConfigLoader.getUserdbField("usernameField"));
					if (userName == null) {
						userPromise.fail("No username in token");
						return;
					}
					Long timestamp = res.getLong("timestamp");
					if (timestamp == null || (System.currentTimeMillis() - timestamp) > 2 * 60 * 60 * 1000) {
						userPromise.fail("Token expired");
						return;
					}
					UserUtils.getUserDataFromCollection(client, userName, dbresult -> {
						dbresult.onSuccess(userData -> {
							User user = UserUtils.buildUserForDBEntry(userData, userName);
							userPromise.complete(user);
						})
								.onFailure(err -> userPromise.fail(err));

					});
				})
				.onFailure(err -> userPromise.fail(err));
		return userPromise.future();
	}
	
	@Override
	public void authenticate(RoutingContext context, Handler<AsyncResult<User>> handler) {
		HttpServerRequest request = context.request();
		String token = request.getParam("token");
		if (token == null) {
			handler.handle(Future.failedFuture("Missing Token"));			
		} else {
			getUserForToken(token)
					.onSuccess(user -> {
						user.principal().put("storeCookie", true);
						LOGGER.debug("Requesting update of cookie");
						cookieHandler.updateCookie(context, user)
								.onSuccess(cookieSaved -> {
									LOGGER.debug("Handling returned");
									// only complete the userPromise once the
									// cookie has been stored successfully.
									request.resume();
									handler.handle(Future.succeededFuture(user));									
								})
								.onFailure(err -> handler.handle(Future.failedFuture(err)));
					})
					.onFailure(err -> handler.handle(Future.failedFuture(err)));
		}
	}

	private Future<Void> sendMail(String mailAddress, String token) {
		Promise<Void> promise = Promise.<Void>promise();
		this.vertx.executeBlocking(future -> {
			try {
				String domain = SoileConfigLoader.getServerProperty("domain");
				String subject = "SOILE Password Reset";
				String from = "no-reply@" + domain;
				String content = "Here is your one time login link for SOILE that you can use for a password reset.\n"
						+ "https://" + domain + "/password/one_time_auth?token=" + token + "\n\n"
						+ "Yours,\nSOILE Team\n";
				Properties props = new Properties();
				props.put("mail.smtp.host", domain);
				props.put("mail.smtp.port", 25);
				props.put("mail.smtp.auth", "false");
				props.put("mail.smtp.starttls.enable", "false");
				Session session = Session.getInstance(props);
				Message message = new MimeMessage(session);
				message.setFrom(new InternetAddress(from));
				message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailAddress));
				message.setSubject(subject);
				message.setText(content);
				Transport.send(message);
				future.complete();
			} catch (Exception e) {
				LOGGER.error("Failed to send password reset email", e);
				future.fail(e);
			}
		}, res -> {
			if (res.succeeded()) {
				promise.complete();
			} else {
				promise.fail(res.cause());
			}
		});
		return promise.future();

	}
}
