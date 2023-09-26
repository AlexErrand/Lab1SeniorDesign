import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SendText {
    public static final String ACCOUNT_SID = "ACf5200104419f75389ab80c169cd5b6c0";
    public static final String AUTH_TOKEN = "";

    // function that will be used in the main program
    public static void sendATextToPhone(String body) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message message = Message.creator(new PhoneNumber("+13195310040"), new PhoneNumber("+18775666567"), body).create();
        System.out.println(message.getSid());
    }

    // main function here is for testing purposes
    public static void main(String[] args) {
        Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
        Message message = Message.creator(new PhoneNumber("+13195310040"), new PhoneNumber("+18775666567"), "hi brandon").create();
        System.out.println(message.getSid());
    }
}
