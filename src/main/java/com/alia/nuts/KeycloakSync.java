package com.alia.nuts;
import org.killbill.billing.catalog.api.Currency;
import org.killbill.billing.client.KillBillClientException;
import org.killbill.billing.client.KillBillHttpClient;
import org.killbill.billing.client.RequestOptions;
import org.killbill.billing.client.api.gen.AccountApi;
import org.killbill.billing.client.model.gen.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Controller;


import java.util.Map;
import java.util.UUID;

@RabbitListener(queues = "#{'${spring.rabbitmq.queue}'}")
@Controller
public class KeycloakSync {

    @Value("${killbill.url}") String kbServerUrl;
    @Value("${killbill.username}") String kbUsername;
    @Value("${killbill.password}") String kbPassword;
    @Value("${killbill.apiKey}") String kbApiKey;
    @Value("${killbill.apiSecret}") String kbApiSecret;

    private static final Logger logger = LoggerFactory.getLogger("NutsController");

    //FIXME add check for verified email, retry on failure with rabbitMQ
    public void createAccount(Map< String, Object > map) {
        if (map.containsKey("resourcePath")) {
            String resourcePath = map.get("resourcePath").toString();
            String kkUid = resourcePath.substring(resourcePath.indexOf("/") + 1);
            logger.info("CREATE user:" + resourcePath);
            Map< String, Object > rep = parseRepresentation(map);
            if (rep != null && rep.containsKey("username") && rep.containsKey("email")) {
                try {
                    killbillCreateAccount(rep.get("username").toString(), rep.get("email").toString(), kkUid);
                } catch (KillBillClientException e) {
                    logger.info("killbill killbillCreateUser error: " + e.getBillingException().getCode().toString());
                }
            }
        } else {
            logger.info("CREATE user: no resourcePath found!");
        }
    }

    //FIXME add check for verified email, retry on failure with rabbitMQ
    public void updateAccount(Map< String, Object > map) {
        if (map.containsKey("resourcePath")) {
            String resourcePath = map.get("resourcePath").toString();
            String kkUid = resourcePath.substring(resourcePath.indexOf("/") + 1);
            logger.info("UPDATE user:" + resourcePath);
            Map< String, Object > rep = parseRepresentation(map);
            if (rep != null && rep.containsKey("username") && rep.containsKey("email")) {
                try {
                    UUID kbID = getIdFromExternalKey(kkUid);
                    killbillUpdateAccount(kbID, rep.get("username").toString(), rep.get("email").toString());
                } catch (KillBillClientException e) {
                    logger.info("killbill updateUser error: " + e.getBillingException().getCode().toString());
                }
            }
        } else {
            logger.info("UPDATE user: no resourcePath found!");
        }
    }

    //FIXME add check for verified email, retry on failure with rabbitMQ
    // NOTE this do not delete account from database, but close account
    public void deleteAccount(Map< String, Object > map) {
        if (map.containsKey("resourcePath")) {
            String resourcePath = map.get("resourcePath").toString();
            String kkUid = resourcePath.substring(resourcePath.indexOf("/") + 1);
            logger.info("DELETE user:" + resourcePath);
            try {
                UUID kbID = getIdFromExternalKey(kkUid);
                killbillDeleteAccount(kbID);
            } catch (KillBillClientException e) {
                logger.info("killbill deleteUser error: " + e.getBillingException().getCode().toString());
            }
        } else {
            logger.info("DELETE user: no resourcePath found!");
        }
    }

    public Map< String, Object > parseRepresentation(Map< String, Object > map) {
        if (map.containsKey("representation")) {
            String representation = map.get("representation").toString();
            JsonParser springParser = JsonParserFactory.getJsonParser();
            Map< String, Object > rep = springParser.parseMap(representation);
            for (Map.Entry < String, Object > entry: rep.entrySet()) {
                logger.info(entry.getKey() + " = " + entry.getValue());
            }
            return rep;
        } else {
            logger.info("no representation found!");
            return null;
        }
    }

    public UUID getIdFromExternalKey(String extKey) throws KillBillClientException {
        int default_connection_timeout_sec = 10;
        int default_read_timeout_sec = 60;
        int default_request_timeout_sec = default_read_timeout_sec;
        KillBillHttpClient killBillHttpClient = new KillBillHttpClient(kbServerUrl, kbUsername, kbPassword, kbApiKey,
                kbApiSecret, null, null, default_connection_timeout_sec * 1000,
                default_read_timeout_sec * 1000,default_request_timeout_sec * 1000);
        AccountApi accountApi = new AccountApi(killBillHttpClient);
        String createdBy = "Keycloak to KillBill sync";
        String reason = "Retrieve killbill ID from keycloak external id";
        String comment = "Internal use";
        RequestOptions requestOptions =  RequestOptions.builder().withCreatedBy(createdBy).withReason(reason)
                .withComment(comment).build();
        Account result = accountApi.getAccountByKey(extKey, requestOptions);
        return result.getAccountId();
    }

    public void killbillDeleteAccount(UUID kbUid) throws KillBillClientException {
        int default_connection_timeout_sec = 10;
        int default_read_timeout_sec = 60;
        int default_request_timeout_sec = default_read_timeout_sec;
        KillBillHttpClient killBillHttpClient = new KillBillHttpClient(kbServerUrl, kbUsername, kbPassword, kbApiKey,
                kbApiSecret, null, null, default_connection_timeout_sec * 1000,
                default_read_timeout_sec * 1000,default_request_timeout_sec * 1000);
        AccountApi accountApi = new AccountApi(killBillHttpClient);
        String createdBy = "Keycloak to KillBill sync";
        String reason = "Delete user from Keycloak";
        String comment = "Delete user from Keycloak realm AppUser";
        RequestOptions requestOptions =  RequestOptions.builder().withCreatedBy(createdBy).withReason(reason)
                .withComment(comment).build();

        Boolean cancelAllSubscriptions = true; // Will cancel all subscriptions
        Boolean writeOffUnpaidInvoices = true; // Will write off unpaid invoices
        Boolean itemAdjustUnpaidInvoices = false; // Will not adjust unpaid invoices
        Boolean removeFutureNotifications = true; // Will remove future notifications

        accountApi.closeAccount(kbUid, cancelAllSubscriptions, writeOffUnpaidInvoices, itemAdjustUnpaidInvoices,
                removeFutureNotifications, requestOptions);
    }

    public void killbillUpdateAccount(UUID kbUid, String kkUsername, String kkEmail) throws KillBillClientException {
        int default_connection_timeout_sec = 10;
        int default_read_timeout_sec = 60;
        int default_request_timeout_sec = default_read_timeout_sec;
        KillBillHttpClient killBillHttpClient = new KillBillHttpClient(kbServerUrl, kbUsername, kbPassword, kbApiKey,
                kbApiSecret, null, null, default_connection_timeout_sec * 1000,
                default_read_timeout_sec * 1000,default_request_timeout_sec * 1000);
        AccountApi accountApi = new AccountApi(killBillHttpClient);
        String createdBy = "Keycloak to KillBill sync";
        String reason = "Update user from Keycloak";
        String comment = "Update user from Keycloak realm AppUser";
        RequestOptions requestOptions =  RequestOptions.builder().withCreatedBy(createdBy).withReason(reason)
                .withComment(comment).build();
        Account body = new Account();
        body.setName(kkUsername);
        body.setEmail(kkEmail);
        body.setCurrency(Currency.EUR);

        accountApi.updateAccount(kbUid, body, requestOptions);
    }

    public void killbillCreateAccount(String kkUsername, String kkEmail, String kkUid) throws KillBillClientException {
        int default_connection_timeout_sec = 10;
        int default_read_timeout_sec = 60;
        int default_request_timeout_sec = default_read_timeout_sec;
        KillBillHttpClient killBillHttpClient = new KillBillHttpClient(kbServerUrl, kbUsername, kbPassword, kbApiKey,
                kbApiSecret, null, null, default_connection_timeout_sec * 1000,
                default_read_timeout_sec * 1000,default_request_timeout_sec * 1000);
        AccountApi accountApi = new AccountApi(killBillHttpClient);
        String createdBy = "Keycloak to KillBill sync";
        String reason = "New user from Keycloak";
        String comment = "New user from Keycloak realm AppUser";
        RequestOptions requestOptions =  RequestOptions.builder().withCreatedBy(createdBy).withReason(reason)
                .withComment(comment).build();
        Account body = new Account();
        body.setName(kkUsername);
        body.setEmail(kkEmail);
        body.setExternalKey(kkUid);
        body.setCurrency(Currency.EUR);

        Account result = accountApi.createAccount(body, requestOptions);
        logger.info("Result: \n" + result.toString());
    }

    @RabbitHandler
    public void receive(byte[] in) {
        String resp = new String(in);
        logger.info(" [x] Received '" + resp + "'");

        JsonParser springParser = JsonParserFactory.getJsonParser();
        Map< String, Object > map = springParser.parseMap(resp);
//        String mapArray[] = new String[map.size()];
//
//        logger.info("Items found: " + mapArray.length);
//        for (Map.Entry < String, Object > entry: map.entrySet()) {
//            logger.info(entry.getKey() + " = " + entry.getValue());
//        }

        if (map.containsKey("realmId")) {
            String realm = map.get("realmId").toString();
            logger.info("realm is: " + realm);
            if (realm.equals("AppUser")) {
                if (map.containsKey("resourceType")) {
                    String resourceType = map.get("resourceType").toString();
                    if (resourceType.equals("USER")) {
                        if (map.containsKey("operationType")) {
                            String operationType = map.get("operationType").toString();
                            switch (operationType) {
                                case "DELETE":
                                    deleteAccount(map);
                                    break;
                                case "CREATE":
                                    createAccount(map);
                                    break;
                                case "UPDATE":
                                    updateAccount(map);
                                    break;
                                default:
                                    logger.info("unknown operationType: " + operationType);
                            }
                        } else {
                            logger.info("no operationType found!");
                        }
                    } else {
                        logger.info("wrong resourceType!");
                    }
                } else {
                    logger.info("no resourceType found!");
                }
            } else {
                logger.info("wrong realm!");
            }
        } else {
            logger.info("no realmId found!");
        }
    }
}
