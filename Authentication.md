There are three different authentication mechanisms for the soile platform. Two are for users with an account and one is for anonymous usage. 
Authenticated users will need to login via the /login endpoint. This api end-point accepts a request with a json containing username, password and a flag whether the login should be remembered for more than one session. In both instances, the user will receive a JWT token upon login that can be used for all future calls to the api. In addition, if the remembered flag is set to 1, a cookie will be created that can be used to authenticate for future calls. 
So for registered users there is a cookie authentication as well as a JWT Token authentication available.

For unregistered users, a token authentication is available against some Project execution end-points (in particular the ones needed to run an experiment). 
To use this type of authentication, the Authorization header needs to contain the token. This Authentication needs to be provided as the Authorization header, and is associated with exactly one participant in a project. 


