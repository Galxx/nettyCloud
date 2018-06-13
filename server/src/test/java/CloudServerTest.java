import messages.AuthMsg;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;


public class CloudServerTest extends Assert {

    CloudServer cloudServer;
    CloudClient cloudClient;

    @Before
    public void initial() throws Exception {
        CloudServer.main(new String[]{"31337"});
        NetworkClient.getInstance().connect("localhost", 31337);
        System.out.println("Server running");
    }


    @Test
    public void testADD() {
        cloudClient = new CloudClient("localhost",31337);
        NetworkClient.getInstance().sendObject(new AuthMsg("sb16","123456"));
    }


}
