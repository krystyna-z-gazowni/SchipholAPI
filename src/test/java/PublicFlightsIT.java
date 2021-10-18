import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.apache.commons.lang3.StringUtils.*;

public class PublicFlightsIT {

    private static Properties properties = new Properties();
    private static List<Map<String,Object>> publicFlights;

    @BeforeTest
    public static void setPublicFlights() throws IOException {
        InputStream input = new FileInputStream("src/test/resources/.apikey");
        properties.load(input);
        publicFlights = getPublicFlightsForToday();
    }

    @Test
    public void TestFlightsIata() {

        publicFlights.forEach(flight -> {
            System.out.println("IATA code: " + flight.get("prefixIATA") + " ; Destinations: "
                    + String.join(",", (List)((Map) (flight.get("route"))).get("destinations")) + " ; Departure time: "
                    + flight.get("scheduleTime"));
        });

        List<String> nullablePrefixIata = publicFlights.stream()
                .filter(flight -> flight.get("prefixIATA") == null)
                .map(flight -> (String) flight.get("id"))
                .collect(Collectors.toList());
        if (nullablePrefixIata.size() > 0) {
            Assert.assertNotNull(null, "IATA code is null for flight IDs: " + String.join(",", nullablePrefixIata));
        }
    }

    private static List<Map<String,Object>> getPublicFlightsForToday() {
        final List<Map<String,Object>> result = new ArrayList<>();
        for(int page = 0; page <= getPublicFlightsLastPage(); page++) {
            Response pageResponse = getPublicFlightsResponse(page);
            addFlightsToResultList(pageResponse, result);
        }
        return result;
    }

    private static int getPublicFlightsLastPage() {
        return Integer.parseInt(substringBefore(substringAfterLast(getPublicFlightsResponse(1).getHeader("link"), "page="), ">"));
    }

    private static Response getPublicFlightsResponse(int page) {
        return given()
                .header("Accept", "application/json")
                .header("app_id", properties.get("app_id"))
                .header("app_key", properties.get("app_key"))
                .header("ResourceVersion", "v4")
                .queryParam("page", page)
                .when()
                .get("https://api.schiphol.nl/public-flights/flights")
                .then().assertThat().statusCode(200)
                .extract().response();
    }

    private static void addFlightsToResultList(Response response, List<Map<String,Object>> responseList) {
        response.jsonPath().getList("flights").stream().forEach(map -> {
            responseList.add((Map<String, Object>) map);
        });
    }

}
