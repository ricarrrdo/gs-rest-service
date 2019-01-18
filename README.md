<b>Twitter Oauth legacy authentication - Sending Direct Messages (Standalone Maven Spring project) </b>

<b>How to use:</b>
1. Just insert your Twitter API credentials in Application.java:

        auth_consumer_keyA, oauth_tokenA, oauth_consumer_secretA and oauth_token_secretA
  
2. Compile and run: 
    mvn spring-boot:run
    -> it will try to send message "hello?!" to user 777777777777777777  (check main() function in Application.java)
    
    
<b>Notes: </b>
    
      I'm using Spring deprecated AsyncRestTemplate, so if someone knows how to use the new WebClient, 
      please upgrade restASyncClientBody() method - it would be much appreciated!
      
      
      
Have fun!

ricarrrdo, 18-1-2018
