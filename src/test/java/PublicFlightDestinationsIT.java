import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.apache.commons.lang3.StringUtils.*;

public class PublicFlightDestinationsIT {

    private static Properties properties = new Properties();
    private static List<Map<String,Object>> publicFlightDestinationResponses;

    @BeforeSuite
    public static void setPublicFlightDestinationResponses() throws IOException {
        InputStream input = new FileInputStream("src/test/resources/.apikey");
        properties.load(input);
        publicFlightDestinationResponses = getPublicFlightAustraliaDestinationResponses();
    }

    @Test
    public void TestAustraliaDestinations() {

        Set australiaCities = publicFlightDestinationResponses.stream()
                .map(destination -> destination.get("city")).collect(Collectors.toSet());
        Assert.assertTrue(australiaCities.contains("Sydney"),"city:Sydney is not included in API response for country:Australia");

        publicFlightDestinationResponses.forEach(destination -> {
            System.out.println("IATA code: " + destination.get("iata") + ", city: " + destination.get("city"));
        });
    }

    @Test
    public void TestAustralianIatas(){
        List<String> nullableAustralianIatas = publicFlightDestinationResponses.stream()
                .filter(destination -> destination.get("iata")==null)
                .map(destination -> (String)destination.get("city"))
                .collect(Collectors.toList());
        if (nullableAustralianIatas.size() > 0) {
            Assert.assertNotNull(null, "IATA code is null for cities: " + String.join(",", nullableAustralianIatas));
        }
    }

    @Test
    public void TestAustralianCities(){
        List<String> nullableAustralianCities = publicFlightDestinationResponses.stream()
                .filter(destination -> destination.get("city")==null)
                .map(destination -> (String)destination.get("iata"))
                .collect(Collectors.toList());
        if (nullableAustralianCities.size() > 0) {
            Assert.assertNotNull(null, "City code is null for IATAs: " + String.join(",", nullableAustralianCities));
        }
    }

    private static List<Map<String,Object>> getPublicFlightAustraliaDestinationResponses() {
        final List<Map<String,Object>> result = new ArrayList<>();
        for(int page = 0; page <= getPublicFlightDestinationsLastPage(); page++) {
            Response pageResponse = getPublicFlightDestinationResponse(page);
            if(result.size()>0 && !isCountryAustralia(pageResponse)) {
                break;
            }else{
                addAustraliaDestinationsToResultList(pageResponse, result);
/*              System.out.println(result);
                System.out.println(page);*/
            }
        }
        return result;
    }

    private static Response getPublicFlightDestinationResponse(int page) {
        return given()
                .header("Accept", "application/json")
                .header("app_id", properties.get("app_id"))
                .header("app_key", properties.get("app_key"))
                .header("ResourceVersion", "v4")
                .queryParam("sort", "country")
                .queryParam("page", page)
                .when()
                .get("https://api.schiphol.nl/public-flights/destinations")
                .then().assertThat().statusCode(200)
                .extract().response();
    }

    private static int getPublicFlightDestinationsLastPage() {
        return Integer.parseInt(substringBefore(substringAfterLast(getPublicFlightDestinationResponse(1).getHeader("link"), "page="), ">"));
    }

    private static void addAustraliaDestinationsToResultList(Response response, List<Map<String,Object>> responseList) {
        List<Map<String,Object>> jsonResponse = response.jsonPath().getList("destinations");
        for (Map<String,Object> destination:jsonResponse) {
            if (isNotBlank((String) destination.get("country")) && equalsIgnoreCase("Australia",(String) destination.get("country"))) {
                responseList.add(destination);
            }
        }
    }
    private static boolean isCountryAustralia(Response response) {
        List<Map<String,Object>> jsonResponse = response.jsonPath().getList("destinations");
        for (Map<String, Object> destination : jsonResponse) {
            if (destination.get("country") != null && destination.get("country").toString().equalsIgnoreCase("Australia")) {
                return true;
            }
        }
        return false;
    }
}