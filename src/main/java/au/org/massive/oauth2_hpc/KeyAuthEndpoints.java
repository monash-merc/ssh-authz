package au.org.massive.oauth2_hpc;

import au.org.massive.oauth2_hpc.ssh.RSAPublicKeyCodec;
import au.org.massive.oauth2_hpc.ssh.SSHCertificateGenerator;
import au.org.massive.oauth2_hpc.ssh.SSHCertificateGenerator.SSHCertType;
import au.org.massive.oauth2_hpc.ssh.SSHCertificateOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller class for the key signing endpoint
 * @author jrigby
 *
 */
@RestController
public class KeyAuthEndpoints {

	private static final Logger log = Logger.getLogger(KeyAuthEndpoints.class.getName());

	private static final Settings settings = Settings.getInstance();

	/**
	 * Key signing endpoint protected by OAuth2.
	 * Accepts a public key and optional valid period and returns a signed certificate.
	 * Json request example for a 10 day certificate: { "public_key": "...", "valid_for": "10" }
	 * The public key is RSA, formatted as with ssh-keygen in base64 (i.e. ~/.ssh/id_rsa.pub)
	 *
	 * @return the certificate
	 */
	@RequestMapping(value="/api/v1/sign_key",
			method={RequestMethod.GET,RequestMethod.POST},
			produces=MediaType.APPLICATION_JSON_VALUE)
	public String authorizeKey(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			Map<String,String> responseMessage = new HashMap<String,String>();
			String remoteHPCUser = (String)SecurityContextHolder.getContext().getAuthentication().getPrincipal();

			try {
				Map<String,Object> data = JsonRequest.processJsonRequest(request);
				String pubKeyString = (String)data.get("public_key");
				try {
					if (pubKeyString == null || pubKeyString.isEmpty()) {
						throw new InvalidKeyException();
					}

					RSAPublicKey publicKey = RSAPublicKeyCodec.decodeKeyFromSSHBase64Format((String)data.get("public_key"));
					RSAPublicKey caPublicKey = settings.getCAPublicKey();
					RSAPrivateKey caPrivateKey = settings.getCAPrivateKey();

					int requestedValidity = settings.getMaxSSHCertValidity();
					try {
						if (data.get("valid_for") != null) {
							Object valid_for = data.get("valid_for");
							requestedValidity = (valid_for instanceof String)?Integer.valueOf((String)valid_for):(int)Math.round((Double)valid_for);
							if (requestedValidity <= 0) {
								throw new JsonSyntaxException("expected integer for \"valid_for\" field");
							} else if (requestedValidity > settings.getMaxSSHCertValidity()) {
								throw new ExceededMaximumCertificateValidityException();
							}
						}
					} catch (NumberFormatException e) {
						throw new JsonSyntaxException(e);
					}

					SSHCertificateOptions certOpts = SSHCertificateOptions.builder()
							.setDefaultOptions()
							.addPrincipal(remoteHPCUser)
							.setKeyId(InetAddress.getLocalHost().getHostName()+"-cert_"+remoteHPCUser)
							.setPubKey(publicKey)
							.setValidDaysFromNow(requestedValidity)
							.setType(SSHCertType.SSH_CERT_TYPE_USER)
							.build();

					String signedCertificate = SSHCertificateGenerator.generateSSHCertificate(certOpts, caPublicKey, caPrivateKey);
					log.info("Signed a certificate for "+remoteHPCUser+" valid for "+requestedValidity+" days.");
					responseMessage.put("user", remoteHPCUser);
					responseMessage.put("certificate", signedCertificate);

				} catch (InvalidKeyException | SignatureException e) {
					log.info("Rejected a signing request for "+remoteHPCUser+" because an invalid public key was provided.");
					responseMessage.put("error", "Malformed public key");
					response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				} catch (ExceededMaximumCertificateValidityException e) {
					log.info("Rejected a signing request for "+remoteHPCUser+" because the validity period exceeded limits.");
					responseMessage.put("error", "Server configured to sign certificates with a maximum duration of "+settings.getMaxSSHCertValidity()+" days");
					response.sendError(HttpServletResponse.SC_BAD_REQUEST);
				}
			} catch (JsonSyntaxException e) {
				log.info("Couldn't understand signing request made by "+remoteHPCUser);
				responseMessage.put("error", "Malformed request");
				response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			}

			Gson gson = new GsonBuilder().disableHtmlEscaping().create();

			return gson.toJson(responseMessage).replace("\\\\", "\\");
		} catch (Exception e) {
			// A catch-all to avoid exposing the exact cause of the error to the client
			e.printStackTrace();
			throw new Exception("Error processing signing request");
		}
	}
}
