import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

public class SendText {
    public static final String ACCOUNT_SID = "ACf5200104419f75389ab80c169cd5b6c0";
    public static final String AUTH_TOKEN = "5aa6bbba131cc143ee39a9101c23c2cd";

    public void sendATextToBrandon(){
        Twilio.init(ACCOUNT_SID,AUTH_TOKEN);
        Message message = Message.creator(new PhoneNumber("+13195310040"),new PhoneNumber("+18775666567"),"THE TEMPERATURE IS TOO HIGH EVACUATE THE BUILDING").create();
        System.out.println(message.getSid());

    }

    public static void main(String[] args) {
        Twilio.init(ACCOUNT_SID,AUTH_TOKEN);
        Message message = Message.creator(new PhoneNumber("+13195310040"),new PhoneNumber("+18775666567"),"hi brandon").create();
        System.out.println(message.getSid());
    }
}