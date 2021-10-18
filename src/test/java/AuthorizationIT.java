import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static io.restassured.RestAssured.given;

public class AuthorizationIT {

    private static Properties properties = new Properties();

    @BeforeTest
    public static void setPublicFlightDestinationResponses() throws IOException {
        InputStream input = new FileInputStream("src/test/resources/.apikey");
        properties.load(input);
    }

    @Test
    public void TestAuthorization(){
        String incorrectApiKey = properties.get("app_key") + "a";
        given()
                .header("Accept", "application/json")
                .header("app_id", properties.get("app_id"))
                .header("app_key", incorrectApiKey)
                .header("ResourceVersion", "v4")
                .when()
                .get("https://api.schiphol.nl/public-flights/flights")
                .then().assertThat().statusCode(403);
    }
}
