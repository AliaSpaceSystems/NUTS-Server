package com.alia.nuts;

import com.alia.nuts.db.Job;
import com.alia.nuts.db.OrderTracking;
import com.alia.nuts.db.TrackingData;
import com.alia.nuts.db.User;
import org.hibernate.mapping.Collection;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import io.swagger.v3.oas.annotations.Operation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

@RestController
@CrossOrigin(origins = "http://150.146.128.33:4200")
@RequestMapping("/${server.basePath}")
public class NutsController {
    private static final Logger logger = LoggerFactory.getLogger("NutsController");

    @Value("${prefect.url}") String prefectUrl;
    @Value("${prefect.project}") String prefectProject;
    @Value("${prefect.s2estWF}") String prefectS2estWF;
    @Value("${prefect.s2proWF}") String prefectS2proWF;
    @Value("${prefect.demProWF}") String prefectDemProWF;
    @Value("${prefect.demEstWF}") String prefectDemEstWF;

    UserRepository userRepository;
    OrderTrackingRepository orderTrackingRepository;
    JobRepository jobRepository;
    RestTemplate restTemplate;

    NutsController(UserRepository userRepository,
                   OrderTrackingRepository orderTrackingRepository,
                   JobRepository jobRepository,
                   RestTemplateBuilder restTemplateBuilder) {
        this.userRepository = userRepository;
        this.orderTrackingRepository = orderTrackingRepository;
        this.jobRepository = jobRepository;
        this.restTemplate = restTemplateBuilder.build();
    }

    //FIXME remove unused userId
    //@CrossOrigin(origins = "http://150.146.128.33:4200")
    @GetMapping("/estimate/{userId}")
    public ResponseEntity estimateRequest(@RequestHeader(value = "authorization", defaultValue = "No token found") String token,
                                   @PathVariable int userId,
                                   @RequestParam("datasource") String datasource,
                                   @RequestParam("data") String data,
                                   @RequestParam("start") String startDateStr,
                                   @RequestParam("stop") String stopDateStr,
                                   @RequestParam("layer") String shapeLayer,
                                   @RequestParam("ROI") String shapeROI,
                                   @RequestParam(required = false, value = "UUIDUSER") String uuidUser,
                                   @RequestParam(required = false, value = "UUIDSESSIONE") String uuidSession)
    {

        String[] result = extractFromToken(token);
        if (result != null) {
            logger.info("User from token:  " + result[0] );
            logger.info("Email from token: " + result[1] );
            checkUserInDB(result);
        } else {
            logger.info("Token not found or invalid !");
            //return false;
        }

        if (uuidUser != null &&  uuidSession != null) {
            Optional<OrderTracking> possibleOrder = orderTrackingRepository.findOrderByUserAndSessionParams(uuidUser, uuidSession);
            //Optional<OrderTracking> possibleOrder = orderTrackingRepository.findById(1);
            if( !possibleOrder.isPresent() ) {

                String status = "ESTIMATE";
                String workflowName;
                switch (datasource) {
                    case "DEM":
                        workflowName = prefectDemEstWF;
                        break;
                    case "Sentinel-2":
                        workflowName = prefectS2estWF;
                        break;
                    default:
                        logger.error("Workflow for datasource " + datasource + " not supported ");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Workflow for datasource " + datasource + " not supported ");
                }
                //OrderTable newOrder = generateOrder(result[0], datasource, data, startDateStr, stopDateStr, shapeLayer, shapeROI, status);
                //FIXME check if uuidUser, uuidSession already exists
                OrderTracking newOrder = generateOrder(uuidUser, uuidSession, datasource, data, startDateStr, stopDateStr, shapeLayer, shapeROI, status);
                //return doRequest(newOrder, workflowName);
                return ResponseEntity.ok(doRequest(newOrder, workflowName));
            } else {
                logger.error("Order with user " + uuidUser + " and session " + uuidSession + " already present in DB");
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Order with user " + uuidUser + " and session " + uuidSession + " already present in DB");
            }
        } else { // support for old client estimation without session id
            if (result != null) { //user from token present
                String status = "ESTIMATE";
                String workflowName;
                switch (datasource) {
                    case "DEM":
                        workflowName = prefectDemEstWF;
                        break;
                    case "Sentinel-2":
                        workflowName = prefectS2estWF;
                        break;
                    default:
                        logger.error("Workflow for datasource " + datasource + " not supported ");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Workflow for datasource " + datasource + " not supported ");
                }
                //OrderTable newOrder = generateOrder(result[0], datasource, data, startDateStr, stopDateStr, shapeLayer, shapeROI, status);
                //FIXME check if uuidUser, uuidSession already exists
                OrderTracking newOrder = generateOrder(result[0] , null, datasource, data, startDateStr, stopDateStr, shapeLayer, shapeROI, status);
                //return doRequest(newOrder, workflowName);
                return ResponseEntity.ok(doRequest(newOrder, workflowName));
            } else {
                logger.error("User not identified");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User not identified");
            }
        }

    }

    //FIXME remove unused userId
    // /nuts/execute/1?datasource=Sentinel-2&data=L2A&start=2021-01-01T00:00:00.000Z&stop=2021-02-01T00:12:12.000Z&layer=NUTS&ROI=Rome
    //@CrossOrigin(origins = "http://150.146.128.33:4200")
    @GetMapping("/execute/{userId}")
    public boolean executeRequest(@RequestHeader(value = "authorization", defaultValue = "No token found") String token,
                                  @RequestParam("datasource") String datasource,
                                  @RequestParam("data") String data,
                                  @RequestParam("start") String startDateStr,
                                  @RequestParam("stop") String stopDateStr,
                                  @RequestParam("layer") String shapeLayer,
                                  @RequestParam("ROI") String shapeROI) {

        String[] result = extractFromToken(token);
        if (result != null) {
            logger.info("User from token:  " + result[0] );
            logger.info("Email from token: " + result[1] );
            checkUserInDB(result);
        } else {
            logger.info("Token not found or invalid !");
            return false;
        }

        String status = "NEW";
        String workflowName;
        switch( datasource ) {
            case "DEM":
                workflowName = prefectDemProWF;
                break;
            case "Sentinel-2":
                workflowName = prefectS2proWF;
                break;
            default:
                logger.error("workflow for datasource " + datasource + " not supported ");
                return false;
        }
        OrderTracking newOrder = generateOrder(result[0], result[1], datasource, data, startDateStr, stopDateStr, shapeLayer, shapeROI, status);
        return doRequest(newOrder, workflowName);
    }

    @GetMapping("/cancelOrder/{userId}/{sessionId}")
    public ResponseEntity cancelOrder(@RequestHeader(value = "authorization", defaultValue = "No token found") String token,
                                  @PathVariable String userId,
                                  @PathVariable String sessionId) {

        String[] result = extractFromToken(token);
        if (result != null) {
            logger.info("User from token:  " + result[0] );
            logger.info("Email from token: " + result[1] );
            checkUserInDB(result);
        } else {
            logger.info("Token not found or invalid !");
            //return false;
        }

        Optional<OrderTracking> possibleOrder = orderTrackingRepository.findOrderByUserAndSessionParams(userId, sessionId);

        //Optional<OrderTracking> possibleOrder = orderTrackingRepository.findById(1);
        if( !possibleOrder.isPresent() ) {
            logger.error("Order with ID " + sessionId + " not found in DB");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order with ID " + sessionId + " not found in DB");
        }
        OrderTracking order = possibleOrder.get();
        //FIXME define meanigfull status
        if( ! (order.getStatus().equals("user notified (estimate)") ||  order.getStatus().equals("estimated") || order.getStatus().equals("ESTIMATE") )) {
            logger.warn("Can not cancel order with status '" + order.getStatus() + "'");
            return ResponseEntity.status(HttpStatus.SEE_OTHER).body("Order with ID " + sessionId + " have status " + order.getStatus());
        }

        order.setStatus("CANCELLED");

        orderTrackingRepository.flush();
        logger.info("Order with ID " + sessionId + " cancelled");


        return ResponseEntity.ok("order cancelled");
    }

    //@CrossOrigin(origins = "http://150.146.128.33:4200")
    @GetMapping("/executeOrder/{userId}/{sessionId}")
    public ResponseEntity executeRequest(@RequestHeader(value = "authorization", defaultValue = "No token found") String token,
                                  @PathVariable String userId,
                                  @PathVariable String sessionId,
                                  @RequestParam("UIDINVOICE") String invoiceId) {

        String[] result = extractFromToken(token);
        if (result != null) {
            logger.info("User from token:  " + result[0] );
            logger.info("Email from token: " + result[1] );
            checkUserInDB(result);
        } else {
            logger.info("Token not found or invalid !");
            //return false;
        }

        Optional<OrderTracking> possibleOrder = orderTrackingRepository.findOrderByUserAndSessionParams(userId, sessionId);

        //Optional<OrderTracking> possibleOrder = orderTrackingRepository.findById(1);
        if( !possibleOrder.isPresent() ) {
            logger.error("Order with ID " + sessionId + " not found in DB");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order with ID " + sessionId + " not found in DB");
        }
        OrderTracking order = possibleOrder.get();

        if( order.getStatus().equals("NEW") ) {
            logger.warn("Order  with ID " + sessionId + " already executed, exiting");
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Order with ID " + sessionId + " already launched");
        }

        logger.info("Execute order " + order.getId());

        for( Job job: order.getJobs()) {
            job.getSourceProducts().clear();
            jobRepository.delete(job);
        }
        order.getJobs().clear();
        order.setStatus("NEW");
        order.setInvoiceId(invoiceId);
        Date now = new Date();
        order.setTsElaborationT1(now);
        jobRepository.flush();
        orderTrackingRepository.flush();
        logger.info("Removed previous estimation");

        String workflowName;
        switch( order.getSourceMission() ) {
            case "DEM":
                workflowName = prefectDemProWF;
                break;
            case "Sentinel-2":
                workflowName = prefectS2proWF;
                break;
            default:
                logger.error("workflow for datasource " + order.getSourceMission() + " not supported ");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("workflow for datasource " + order.getSourceMission() + " not supported ");
        }

        return ResponseEntity.ok(doRequest(order, workflowName));
    }

    @GetMapping("/executeOrderById/{orderId}")
    public ResponseEntity executeRequest(@RequestHeader(value = "authorization", defaultValue = "No token found") String token,
                                         @PathVariable Integer orderId) {

        String[] result = extractFromToken(token);
        if (result != null) {
            logger.info("User from token:  " + result[0] );
            logger.info("Email from token: " + result[1] );
            checkUserInDB(result);
        } else {
            logger.info("Token not found or invalid !");
            //return false;
        }

        Optional<OrderTracking> possibleOrder = orderTrackingRepository.findById(orderId);
        if( !possibleOrder.isPresent() ) {
            logger.error("Order with ID " + orderId + " not found in DB");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Order with ID " + orderId + " not found in DB");
        }
        OrderTracking order = possibleOrder.get();
        if( order.getStatus().equals("NEW") ) {
            logger.warn("Order already executed, exiting");
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Order with ID " + orderId + " already launched");
        }

        logger.info("Execute order " + orderId);

        logger.info("Execute order " + order.getId());

        for( Job job: order.getJobs()) {
            job.getSourceProducts().clear();
            jobRepository.delete(job);
        }
        order.getJobs().clear();
        order.setStatus("NEW");
        Date now = new Date();
        order.setTsElaborationT1(now);
        jobRepository.flush();
        orderTrackingRepository.flush();
        logger.info("Removed previous estimation");

        String workflowName;
        switch( order.getSourceMission() ) {
            case "DEM":
                workflowName = prefectDemProWF;
                break;
            case "Sentinel-2":
                workflowName = prefectS2proWF;
                break;
            default:
                logger.error("workflow for datasource " + order.getSourceMission() + " not supported ");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("workflow for datasource " + order.getSourceMission() + " not supported ");
        }

        return ResponseEntity.ok(doRequest(order, workflowName));
    }

    private OrderTracking generateOrder(String uuidUser, String uuidSession,
                                        String datasource,
                                        String data,
                                        String startDateStr,
                                        String stopDateStr,
                                        String shapeLayer,
                                        String shapeROI,
                                        String status) {


        //logger.error("OrderTable user ID: " + user.getId());
        //logger.error("OrderTable user name: " + user.getName());
        //logger.error("OrderTable user email: " + user.getEmail());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date startDate;
        Date stopDate;
        try {
            startDate = sdf.parse(startDateStr);
            stopDate = sdf.parse(stopDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
        Date now = new Date();
        OrderTracking order = new OrderTracking();
        order.setUuid_user(uuidUser);
        order.setUuid_session(uuidSession);
        order.setShapeLayer(shapeLayer);
        order.setShapeRoi(shapeROI);
        order.setStartTime(startDate);
        order.setStopTime(stopDate);
        order.setSourceMission(datasource);
        order.setSourceDataType(data);
        order.setStatus(status);
        order.setTsEstimationT1(now);
        orderTrackingRepository.saveAndFlush(order);

        return order;
    }

    //@CrossOrigin(origins = "http://150.146.128.33:4200")
    @RequestMapping("/tracking/{userId}/{sessionId}")
    public TrackingData FrontEndTracking(@RequestHeader(value = "authorization", defaultValue = "No token found") String token,
                                    @PathVariable String userId,
                                    @PathVariable String sessionId) {

//        String[] result = extractFromToken(token);
//        if (result != null) {
//            logger.info("User from token:  " + result[0]);
//            logger.info("Email from token: " + result[1]);
//            checkUserInDB(result);
//        } else {
//            logger.info("Token not found or invalid !");
//            TrackingData empty = null;
//            return empty;
//        }

        Optional<TrackingData> possibleOrder = orderTrackingRepository.findTrackingByUserAndSessionParams(userId, sessionId);

        //Optional<OrderTracking> possibleOrder = orderTrackingRepository.findById(1);
        if (!possibleOrder.isPresent()) {
            logger.error("Order with ID " + sessionId + " not found in DB");
            TrackingData empty = null;
            return empty;
        }
        TrackingData order = possibleOrder.get();

        //logger.info("Send order " + order.getId());


        return order;
    }

    private boolean doRequest(OrderTracking order, String workflowName) {

        logger.info("Launch flow " + workflowName + "for order " + order.getId());

        if( order == null ) {
            return false;
        }
        WebClient webClient = WebClient.create(prefectUrl);
        String flowIdStr = webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                //.bodyValue("{\"query\":\"query { flow(where: { name: { _ilike: \\\""+workflowName+"\\\" } }, order_by: {version: desc}, limit: 1) { id } }\"}")
                .bodyValue("{\"query\":\"query { flow(where: { _and: [" +
                        "{name: { _ilike: \\\""+workflowName+"\\\" } }," +
                        "{ project: {name: {_eq: \\\""+prefectProject+"\\\" }}}" +
                        "]}, order_by: {version: desc}, limit: 1) { id } }\"}")
                .retrieve()

                .bodyToMono(String.class)
                .block();

        if( flowIdStr == null ) {
            return false;
        }
        JSONArray flows = new JSONObject(flowIdStr).getJSONObject("data").getJSONArray("flow");
        if( flows.length() != 1 ) {
            logger.error("Cannot find "+workflowName);
            return false;
        } else {
            logger.info("Flow " + workflowName + " found in Prefect");

        }
        String flowId = flows.getJSONObject(0).getString("id");

        String flowRunStr = webClient.post()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(
                        "{\"query\":" +
                            "\"mutation {" +
                                " create_flow_run(" +
                                    " input: { " +
                                        "flow_id: \\\""+flowId+"\\\"" +
                                        " parameters: \\\"{\\\\\\\"orderid\\\\\\\": "+order.getId()+ "}\\\"" +

                                    "}" +
                                ") {" +
                                    " id" +
                                "}" +
                            "}\"" +
                        "}")
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if( flowRunStr == null ) {
            return false;
        }
        JSONObject result = new JSONObject(flowRunStr);
        if( result.has("data")) {
            JSONObject flowRun = result.getJSONObject("data").getJSONObject("create_flow_run");
            String flowRunId = flowRun.getString("id");
            logger.info("Started flow (run " + flowRunId + ")" );
        } else {
            logger.error("cannot start "+workflowName);
        }
        return true;
    }

    @Operation(summary = "Health check")
    @GetMapping("/hcheck/{msg}")
    @ResponseBody
    public String healtCheck(@RequestHeader(value = "authorization", defaultValue = "No token found") String token,
                             @PathVariable String msg) {
        String[] result = extractFromToken(token);
        if (result != null) {
            logger.info("User from token:  " + result[0] );
            logger.info("Email from token: " + result[1] );
        } else {
            logger.info("Token not found or invalid !");
        }
        logger.info("healtCheck with: " + msg );
        return msg;
    }

    void checkUserInDB(String[] userToCheck) {
        Optional<User> possibleUser = userRepository.findByName(userToCheck[0]);
        if (!possibleUser.isPresent()) {
            logger.debug("User with ID " + userToCheck[0] + " not found in DB");
            User user = new User();
            user.setName(userToCheck[0]);
            user.setEmail(userToCheck[1]);
            userRepository.saveAndFlush(user);
        } else {
            User user = possibleUser.get();
            if (!user.getEmail().equals(userToCheck[1])) {
                logger.debug("User with ID " + userToCheck[0] + " not match email " + userToCheck[1] + " in DB");
                user.setEmail(userToCheck[1]);
                userRepository.saveAndFlush(user);
            }
        }
    }

    //FIXME change String[] to User object
    public String[] extractFromToken(String token) {

        String[] result = null;

        logger.info("authorization header: " + token );

        var parts = token.split(" ");
        logger.info("header parts: " +  parts.length);
        if (parts.length == 2) {
            var scheme = parts[0];
            var credentials = parts[1];

            logger.info("header scheme: " +  scheme);

            if (scheme.equals("Bearer")) {
                String[] chunks = credentials.split("\\.");
                Base64.Decoder decoder = Base64.getUrlDecoder();
                logger.info("jwt chunks: " + chunks.length );
                String header = new String(decoder.decode(chunks[0]));
                String payload = new String(decoder.decode(chunks[1]));
                //String payload2 = new String(decoder.decode(chunks[2]));
                logger.info("jwt header: " + header );
                logger.info("jwt payload: " + payload );
                //logger.info("jwt payload2: " + payload2 );
                JSONObject nodeRoot  = new JSONObject(payload);
                logger.info("jwt user_id: " + nodeRoot.getString("sub"));
                logger.info("jwt email_verified: " + nodeRoot.getBoolean("email_verified") );
                logger.info("jwt email: " + nodeRoot.getString("email") );
                if (true) { //nodeRoot.getBoolean("email_verified")) { //FIXME only verified email can be returned
                    result = new String[] {nodeRoot.getString("sub"), nodeRoot.getString("email")};
                }
            }
        }
        return result;
    }
}
