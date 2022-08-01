package com.alia.nuts;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import org.aspectj.lang.annotation.Around;
import org.json.JSONObject;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.api.gen.AccountApi;
import org.killbill.billing.client.model.gen.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.client.RequestOptions;
import org.asynchttpclient.Response;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/${server.baseBillPath}")
public class KillbillStripe {
    private static final Logger logger = LoggerFactory.getLogger("NutsController");

    @Value("${server.internalUrl}") String internalUrl;
    @Value("${server.externalUrl}") String externalUrl;
    @Value("${server.baseBillPath}") String basePath;

    @Value("${killbill.url}") String kbServerUrl;
    @Value("${killbill.username}") String kbUsername;
    @Value("${killbill.password}") String kbPassword;
    @Value("${killbill.apiKey}") String kbApiKey;
    @Value("${killbill.apiSecret}") String kbApiSecret;
    int default_connection_timeout_sec = 10;
    int default_read_timeout_sec = 60;
    int default_request_timeout_sec = default_read_timeout_sec;

    @GetMapping("/success/")
    public void stripeSuccess() {
        logger.info("Success received");
    }

    @GetMapping("/cancel/")
    public void stripeCancel() {
        logger.info("Cancel received");
    }


    @GetMapping("/create_session/")
    public ResponseEntity<String> createStripeSession(
            @RequestHeader(value = "authorization", defaultValue = "No token found") String token) {


        String[] result = extractFromToken(token);
        if (result != null) {
            logger.info("User from token:  " + result[0] );
            logger.info("Email from token: " + result[1] );
            //checkUserInDB(result);
        } else {
            logger.info("Token not found or invalid !");
            return new ResponseEntity<>("Token not found or invalid !", HttpStatus.UNAUTHORIZED);
        }

        KillBillHttpClient killBillHttpClient = new KillBillHttpClient(kbServerUrl, kbUsername, kbPassword, kbApiKey,
                kbApiSecret, null, null, default_connection_timeout_sec * 1000,
                default_read_timeout_sec * 1000,default_request_timeout_sec * 1000);

        try {
            UUID kbID = getIdFromExternalKey(result[0], killBillHttpClient);
            String success_url = externalUrl + "/" + basePath + "/success?accountId=" + kbID.toString()
                    + "&sessionId={CHECKOUT_SESSION_ID}";
            String cancel_url = externalUrl + "/" + basePath + "/cancel?accountId=" + kbID.toString()
                    + "&sessionId={CHECKOUT_SESSION_ID}";

            final String uri = "/plugins/killbill-stripe/checkout";

            final Multimap<String, String> queryParams = LinkedListMultimap.create();

            queryParams.put("kbAccountId", kbID.toString());
            queryParams.put("successUrl", success_url);

            String createdBy = "Keycloak to KillBill sync";
            String reason = "Delete user from Keycloak";
            String comment = "Delete user from Keycloak realm AppUser";
            RequestOptions baseRequestOptions =  RequestOptions.builder().withCreatedBy(createdBy).withReason(reason)
                    .withComment(comment).build();

            final RequestOptions.RequestOptionsBuilder inputOptionsBuilder = baseRequestOptions.extend();

            inputOptionsBuilder.withQueryParams(queryParams);
            inputOptionsBuilder.withHeader(KillBillHttpClient.HTTP_HEADER_ACCEPT, "application/json");
            inputOptionsBuilder.withHeader(KillBillHttpClient.HTTP_HEADER_CONTENT_TYPE, "application/json");
            final RequestOptions requestOptions = inputOptionsBuilder.build();

            Response response;

            try {
                response = killBillHttpClient.doPost(uri, null, requestOptions);
                logger.info("killbill create_session for user " + result[0] + " : \n" + response.toString());
                return new ResponseEntity<>(response.toString(), HttpStatus.OK);
            } catch (KillBillClientException e) {
                logger.info("killbill /plugins/killbill-stripe/checkout error: " + e.getBillingException().getCode().toString());
                return new ResponseEntity<>("Create Stripe session error !", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } catch (KillBillClientException e) {
            logger.info("killbill user not present error: " + e.getBillingException().getCode().toString());
            return new ResponseEntity<>("Token not mattch killbill user !", HttpStatus.UNAUTHORIZED);
        }
    }

    public UUID getIdFromExternalKey(String extKey, KillBillHttpClient killBillHttpClient ) throws KillBillClientException {
        AccountApi accountApi = new AccountApi(killBillHttpClient);
        String createdBy = "Keycloak to KillBill sync";
        String reason = "Retrieve killbill ID from keycloak external id";
        String comment = "Internal use";
        RequestOptions requestOptions =  RequestOptions.builder().withCreatedBy(createdBy).withReason(reason)
                .withComment(comment).build();
        Account result = accountApi.getAccountByKey(extKey, requestOptions);
        return result.getAccountId();
    }

    //FIXME change String[] to User object
    public String[] extractFromToken(String token) {

        String[] result = null;

        logger.info("authorization header: " + token );

        var parts = token.split(" ");
        logger.debug("header parts: " +  parts.length);
        if (parts.length == 2) {
            var scheme = parts[0];
            var credentials = parts[1];

            logger.debug("header scheme: " +  scheme);

            if (scheme.equals("Bearer")) {
                String[] chunks = credentials.split("\\.");
                Base64.Decoder decoder = Base64.getUrlDecoder();
                logger.debug("jwt chunks: " + chunks.length );
                String header = new String(decoder.decode(chunks[0]));
                String payload = new String(decoder.decode(chunks[1]));
                //String payload2 = new String(decoder.decode(chunks[2]));
                logger.debug("jwt header: " + header );
                logger.debug("jwt payload: " + payload );
                //logger.info("jwt payload2: " + payload2 );
                JSONObject nodeRoot  = new JSONObject(payload);
                logger.debug("jwt user_id: " + nodeRoot.getString("sub"));
                logger.debug("jwt email_verified: " + nodeRoot.getBoolean("email_verified") );
                logger.debug("jwt email: " + nodeRoot.getString("email") );
                if (nodeRoot.getBoolean("email_verified")) {
                    result = new String[] {nodeRoot.getString("sub"), nodeRoot.getString("email")};
                }
            }
        }
        return result;
    }
}
