/*
* by ricarrrdo (17-1-2019)
* Twitter Oauth legacy authentication - Sending Direct Messages
* How to use:
* - insert your Twitter api credentials below;
* - compile and run: mvn spring-boot:run -> it will try to send message "hello?!" to user 777777777777777777 (check main).
*
* Note: I'm using Spring deprecated AsyncRestTemplate (if someone knows to use WebClient,
*       please upgrade restASyncClientBody() method, it would be much appreciated!)
 * */

package hello;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.context.request.async.DeferredResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.util.*;

@SpringBootApplication
public class Application {

    //example response:

    /*
     body:{
       "event":{
          "type":"message_create",
          "id":"999999999999999999",
          "created_timestamp":"1547788646014",
          "message_create":{
             "target":{
                "recipient_id":"777777777777777777"
             },
             "sender_id":"888888888888888888888",
             "message_data":{
                "text":"hello??",
                "entities":{
                   "hashtags":[

                   ],
                   "symbols":[

                   ],
                   "user_mentions":[

                   ],
                   "urls":[

                   ]
                }
             }
          }
       }
    }
    */

    /*
        Known response errors:
            {"errors":[{"code":150,"message":"You cannot send messages to users who are not following you."}]}
    */

    //colors
    public static final String RED = "\033[0;31m";     // RED
    public static final String GREEN = "\033[0;32m";   // GREEN
    public static final String WHITE = "\033[0;37m";   // WHITE
    public static final String MAGENTA = "\033[0;35m";  // MAGENTA
    public static final String CYAN = "\033[0;36m";     // CYAN

    //Credentials:
    private final static String urlA = "https://api.twitter.com/1.1/direct_messages/events/new.json";
    private final static String postMethodA = "POST";
    private final static String oauth_consumer_keyA = "insert_yours";
    private final static String oauth_tokenA = "insert_yours";
    private final static String oauth_consumer_secretA = "insert_yours";
    private final static String oauth_token_secretA = "insert_yours";

/*
    //Values taken from https://developer.twitter.com/en/docs/basics/authentication/guides/creating-a-signature.html :
    private final static String urlA = "https://api.twitter.com/1.1/statuses/update.json";
    private final static String postMethodA = "POST";
    private final static String oauth_consumer_keyA = "xvz1evFS4wEEPTGEFPHBog";
    private final static String oauth_tokenA = "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb";
    private final static String oauth_consumer_secretA = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
    private final static String oauth_token_secretA = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";
    private final static String nonceA = "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg";
    private final static String epochA = "1318622958";

    //results from Twitter example to compare with my results:
    private final static String twitterSignatureResultExample = "hCtSmYh+iHYCEqBWrE7C7hYmtUk=";
    private final static String twitterBaseStringExample = "POST&https%3A%2F%2Fapi.twitter.com%2F1.1%2Fstatuses%2Fupdate.json&include_entities%3Dtrue%26oauth_consumer_key%3Dxvz1evFS4wEEPTGEFPHBog%26oauth_nonce%3DkYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg%26oauth_signature_method%3DHMAC-SHA1%26oauth_timestamp%3D1318622958%26oauth_token%3D370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb%26oauth_version%3D1.0%26status%3DHello%2520Ladies%2520%252B%2520Gentlemen%252C%2520a%2520signed%2520OAuth%2520request%2521";
*/

/*
    //Values taken from https://developer.twitter.com/en/docs/basics/authentication/guides/authorizing-a-request :
    private final static String urlA = "https://api.twitter.com/1.1/statuses/update.json";
    private final static String postMethodA = "POST";
    private final static String oauth_consumer_keyA = "xvz1evFS4wEEPTGEFPHBog";
    private final static String oauth_tokenA = "370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb";
    private final static String nonceA = "kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg";
    private final static String epochA = "1318622958";

    //WARNING: assumed values, because these values are not presented in https://developer.twitter.com/en/docs/basics/authentication/guides/authorizing-a-request
    private final static String oauth_consumer_secretA = "kAcSOqF21Fu85e7zjz7ZN2U4ZRhfV3WpwPAoE3Z7kBw";
    private final static String oauth_token_secretA = "LswwdoUaIvS8ltyTt5jkRh4J50vUPVVHtR2YPi5kE";

    //results from Twitter example to compare with my results:
    private final static String twitterSignatureResultExample = "tnnArxj06cWHq44gCs1OSKk/jLY=";
    private final static String twitterSignatureResultExampleENCODED = "tnnArxj06cWHq44gCs1OSKk%2FjLY%3D";
    private final static String twitteHeaderStringExample = "OAuth oauth_consumer_key=\"xvz1evFS4wEEPTGEFPHBog\", oauth_nonce=\"kYjzVBB8Y0ZFabxSWbWovY3uYSQ2pTgmZeNu2VS4cg\", oauth_signature=\"tnnArxj06cWHq44gCs1OSKk%2FjLY%3D\", oauth_signature_method=\"HMAC-SHA1\", oauth_timestamp=\"1318622958\", oauth_token=\"370773112-GmHxMAgYyLbNEtIKZeRNFsMKPR9EyMZeS9weJAEb\", oauth_version=\"1.0\"";

*/

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);

        sendMessageTwitter("hello?!", "777777777777777777");
    }

    //Note: only one recipient per message!
    public static void sendMessageTwitter(String msg, String recipient){

        String bodyContent = stringToJson(createTwitterBody(msg, recipient)).toString();

        HttpHeaders httpHeaders = createOAuthHeader(postMethodA, urlA, msg); //msg might not be needed here
        httpHeaders.set("Content-type", "application/json");

        //debug:
        System.out.println(MAGENTA + "\n\nJson body:\n" + CYAN + bodyContent);

        DeferredResult<ResponseEntity<String>> deferredResult = new DeferredResult<>();
        deferredResult.setResultHandler((Object responseEntity) -> {

            handleDeliveryStatus((ResponseEntity<String>) responseEntity);

        });

        restASyncClientBody(HttpMethod.POST, urlA, httpHeaders, bodyContent, deferredResult);

    }

    public static String createTwitterBody(String text, String recipient) {
        return String.format("{\"event\": {\"type\": \"message_create\", \"message_create\": {\"target\": {\"recipient_id\": \"%s\"}, \"message_data\": {\"text\": \"%s\"}}}}", recipient, text);
    }

    static void handleDeliveryStatus(ResponseEntity<String> responseEntity) {
        //Debug
        printResponseEntity(responseEntity);
    }

    //https://developer.twitter.com/en/docs/basics/authentication/guides/creating-a-signature.html
    public static HttpHeaders createOAuthHeader(String httpMethod, String url, String text) {

        String epochO = getCurrentEpochTimeAsString();
        String nonceO = generateNonce();

        //Map<String, String> map = createMap(oauth_consumer_keyA, oauth_tokenA, nonceA, epochA);
        Map<String, String> map = createMap(oauth_consumer_keyA, oauth_tokenA, nonceO, epochO);

        String parameterString = createParameterString(map);

        String baseString = createOAuthBaseString(httpMethod, urlA, parameterString);

        String signingKey = createOAuthSigningKey(oauth_consumer_secretA, oauth_token_secretA);

        String signatureBase64 = createSignatureBase64(baseString, signingKey);

        String headerString = createHeaderString(map, signatureBase64);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("authorization", headerString);

        return httpHeaders;
    }

    public static  Map<String, String> createMap(String oauth_consumer_key, String oauth_token, String nonce, String epochTime) {
        Map<String, String> map = new TreeMap<>(new Comparator<String>() {
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });

        //NOTE: Do NOT put json body in here!!
        map.put(percentEncode("oauth_consumer_key"), percentEncode(oauth_consumer_key));
        map.put(percentEncode("oauth_nonce"), percentEncode(nonce));
        map.put(percentEncode("oauth_signature_method"), percentEncode("HMAC-SHA1"));
        map.put(percentEncode("oauth_timestamp"), percentEncode(epochTime));
        map.put(percentEncode("oauth_token"), percentEncode(oauth_token));
        map.put(percentEncode("oauth_version"), percentEncode("1.0"));

        //WARNING1:
        //RECREATING TWITTER EXAMPLE:
        //map.put(percentEncode("status"), percentEncode("Hello Ladies + Gentlemen, a signed OAuth request!"));
        //map.put(percentEncode("include_entities"), percentEncode("true"));

        return map;
    }

    public static String createParameterString(Map<String, String> map) {

        //debug:
        printMap(map, "MAP that originates Parameter String:");

        StringBuilder sb = new StringBuilder("");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String a;
            if (sb.toString().equals("")) {
                a = entry.getKey() + "=" + entry.getValue();
            } else {
                a = "&" + entry.getKey() + "=" + entry.getValue();
            }
            sb.append(a);
        }

        //debug
        System.out.println();
        System.out.println(MAGENTA + "Parameter String:\n" + CYAN + sb.toString() + WHITE);

        return sb.toString();
    }

    public static String createHeaderString(Map<String, String> map, String signature) {

        //WARNING2:
        //RECREATING TWITTER EXAMPLE:
        //map.remove("status");
        //map.remove("include_entities");

        //add signature to map:
        map.put(percentEncode("oauth_signature"), percentEncode(signature));

        //debug:
        printMap(map, "MAP that originates Header String:");

        StringBuilder sb2 = new StringBuilder("Oauth ");
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String a;
            if(sb2.toString().equals("Oauth ")) {
                a = entry.getKey() + "=\"" + entry.getValue() + "\"";
            }
            else {
                a = ", " + entry.getKey() + "=\"" + entry.getValue() + "\"";
            }
            sb2.append(a);
        }

        //debug
        String b = percentEncode(signature);
        System.out.println(MAGENTA + "percentEncoded(Signature):\n" + CYAN + b + WHITE);
        System.out.println(MAGENTA + "Signature:\n" + CYAN + signature + WHITE);

        //debug
        /*
        if (signature.equals(twitterSignatureResultExample)) {
            System.out.println(GREEN + "GOOD =D SIGNATURE IS EQUAL TO TWITTER EXAMPLE!");
        }
        else {
            System.out.println(RED + "Signatures do not match =(");
            System.out.println(RED + "Signature string SHOULD BE:\n" + WHITE + twitterSignatureResultExample);
        }

        if (b.equals(twitterSignatureResultExampleENCODED)) {
            System.out.println(GREEN + "GOOD =D SIGNATURE ENCODED IS EQUAL TO TWITTER EXAMPLE!");
        }
        else {
            System.out.println(RED + "And obviously encoded signatures do not match too =(");
        }

        //debug 2
        if (sb2.toString().equals(twitteHeaderStringExample)) {
            System.out.println(GREEN + "GOOD =D HEADER STRING IS EQUAL TO TWITTER EXAMPLE!");
        }
        else {
            System.out.println(RED + "\nHeader strings do not match =(");
            System.out.println(RED + "Header string SHOULD BE:\n" + WHITE + twitteHeaderStringExample);
        }*/

        //debug
        System.out.println();
        System.out.println(MAGENTA + "Header String:\n" + CYAN + sb2.toString() + WHITE);

        return sb2.toString();
    }

    public static void printResponseEntity(ResponseEntity<String> response) {
        System.out.println(" ");
        System.out.println(WHITE + "HTTP RESPONSE:");
        System.out.println("status code: " + response.getStatusCode());
        System.out.println("header: " + response.getHeaders());
        System.out.println("body: " + response.getBody());
        System.out.println(" ");
    }

    public static void restASyncClientBody(final HttpMethod method,
                                       final String uri,
                                       final HttpHeaders headers,
                                       final String body,
                                       DeferredResult<ResponseEntity<String>> deferredResult) {

        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        AsyncRestTemplate restTemplate = new AsyncRestTemplate();

        restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            protected boolean hasError(HttpStatus statusCode) {
                System.out.println(WHITE + "\nHTTP request status code: " + statusCode);
                return false;
            }
        });

        ListenableFuture<ResponseEntity<String>> futureEntity = restTemplate.exchange(uri, method, entity, String.class);
        futureEntity.addCallback(new ListenableFutureCallback<ResponseEntity<String>>() {
            @Override
            public void onSuccess(ResponseEntity<String> result) {
                deferredResult.setResult(result);
            }

            @Override
            public void onFailure(Throwable ex) {
                ///deferredResult.setErrorResult(ex);
                ResponseEntity<String> re = new ResponseEntity<>("error", new HttpHeaders() {{ this.set("error", "service is unavailable this moment"); }}, HttpStatus.SERVICE_UNAVAILABLE);
                deferredResult.setResult(re);
            }
        });
    }

    public static String createOAuthBaseString(String httpMethod, String url, String parameterString) {
        StringBuilder sb = new StringBuilder(httpMethod.toUpperCase());
        sb.append("&");
        sb.append(percentEncode(urlA));
        sb.append("&");
        sb.append(percentEncode(parameterString));

        //debug
        System.out.println();
        System.out.println(MAGENTA + "Signature Base String:\n" + CYAN + sb.toString() + WHITE);

        /* uncomment this for the example case from https://developer.twitter.com/en/docs/basics/authentication/guides/creating-a-signature.html
        if (sb.toString().equals(twitterBaseStringExample)) {
            System.out.println(GREEN + "GOOD =D BASE STRING IS EQUAL TO TWITTER EXAMPLE!");
        }
        else {
            System.out.println(RED + "Signatures do not match =(");
        }*/

        return sb.toString();
    }

    public static String createOAuthSigningKey(String oauth_consumer_secret, String oauth_token_secret) {
        StringBuilder sb = new StringBuilder(percentEncode(oauth_consumer_secret));
        sb.append("&");
        sb.append(percentEncode(percentEncode(oauth_token_secret)));

        //debug
        //System.out.println(">>>>>signingKey: " + sb.toString());

        return sb.toString();
    }

    public static String createSignatureBase64(String baseString, String signingKey) {

        byte[] signature = null;

        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec spec = new SecretKeySpec(signingKey.getBytes(), "HmacSHA1");
            mac.init(spec);
            signature = mac.doFinal(baseString.getBytes());
        }
        catch (Exception e) {
            e.printStackTrace();
            return "error";
        }

        //System.out.println(">>>>>>>>>signature: " + bytesToString(signature));

        String signatureBase64 = bytesBase64Encode(signature);

        return signatureBase64;
    }

    //Taken from http://optimumbrew.com/blog/2015/03/oauth/how-to-generate-oauth-signature-for-twitter-in-core-java :
    public static String percentEncode(String value) {
        String encoded = "";

        try {
            encoded = URLEncoder.encode(value, "UTF-8");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        String sb = "";
        char focus;

        for (int i = 0; i < encoded.length(); i++) {
            focus = encoded.charAt(i);
            if (focus == '*') {
                sb += "%2A";
            }
            else if (focus == '+') {
                sb += "%20";
            }
            else if (focus == '%' && i + 1 < encoded.length() && encoded.charAt(i + 1) == '7' && encoded.charAt(i + 2) == 'E') {
                sb += '~';
                i += 2;
            }
            else {
                sb += focus;
            }
        }
        return sb.toString();
    }

    public static String bytesBase64Encode(byte[] bytes) {
        return new String(Base64.getEncoder().encode(bytes));
    }

    public static long getCurrentEpochTime() {
        return new Date().getTime() / 1000L;
    }

    public static String getCurrentEpochTimeAsString() {
        return Long.toString(getCurrentEpochTime());
    }

    public static String generateRandomLettersString(int length) {

        final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        Random random = new Random();
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            builder.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }

        return builder.toString();
    }

    private static String generateNonce() {
        return generateRandomLettersString(30);
    }

    public static JsonElement stringToJson(String str) {
        return new JsonParser().parse(str);
    }

    public static void printMap(Map<String, String> map, String title) {
        System.out.println();
        System.out.println(MAGENTA + title + CYAN);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
        System.out.println();
    }


}
