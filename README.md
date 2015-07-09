# SSH-AuthZ

## Building
Building is easy with Maven - simply clone the repository and run `mvn package` from the repository root directory, where the pom.xml file is. Maven will fetch all dependencies and produce a runnable jar package in `/target`.

## Running
Sample configuration files are located in `/config_example`. After customising, these files must be found in the classpath of the application. To start the server, run `java -jar ssh-authz-{VERSION}.jar`, with `{VERSION}` replaced with the current version (e.g. 0.0.1-SNAPSHOT). Consider piping the output of the command to a log file and creating a startup script (e.g. init or upstart) to run server in the background on startup.

## Apache HTTPd setup
SSH-AuthZ expect to have its `/oauth/authorize` endpoint protected by mod\_shib and matched users based on the mail header (email address) sent after the user is authenticated. This must be done by Apache HTTPd before the request reaches SSH-AuthZ. Configuring mod\_shib is beyond the scope of this readme, however the endpoints themselves may be protected with configuration similar to the following:
```
    <Location /oauth/token>
        ProxyPass ajp://localhost:9000/oauth/token
    </Location>
    <Location /oauth/token_key>
        ProxyPass ajp://localhost:9000/oauth/token_key
    </Location>
	<Location /oauth/authorize>
        AuthType Shibboleth
        ShibRequireSession On
        ShibUseHeaders On
        require valid-user
	    ProxyPass ajp://localhost:9000/oauth/authorize
    </Location>
    <Location /oauth/static>
	    ProxyPass ajp://localhost:9000/oauth/static
    </Location>
	<Location /api>
        ProxyPass ajp://localhost:9000/api
    </Location>
```

## Certificate signing
Public key signing is performed by the `/api/v1/sign_key` endpoint. To sign a key, a POST request containing a JSON string containing the public key and an optional 'valid_for' value that specifies the number of days the certificate is valid for. If valid\_for is excluded, the certificate defaults to the maximum allowed time. The public key should be given as the output from `ssh-keygen` for RSA keys. Note: valid\_for must be a string value.

Example request:
```
{
    "public_key": "ssh-rsa AAAAB3NzaC1yc2EAAA...GKFnu7e7odat+/czRl jrigby@monash.edu.au",
    "valid_for": "7"
}
```

Example response:
```
{
    "certificate": "ssh-rsa-cert-v01@openssh.com AAAAHHNzaC1yc2EtY2hRh...k+KbzaRv4qWcGHYfAi9HhD jrigby@monash.edu.au"
}
```
