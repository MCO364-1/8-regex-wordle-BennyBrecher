import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import java.nio.file.*;
import java.util.stream.*;

public class NYTWordleHistoryFetcher {
    private static final HttpClient HTTP = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private static final ObjectMapper M = new ObjectMapper();
    private static final LocalDate START = LocalDate.of(2021, 6, 19);

    public static Map<LocalDate, String> fetchAll() throws Exception {
        Map<LocalDate, String> out = new LinkedHashMap<>();
        for (LocalDate d = START; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            String url = "https://www.nytimes.com/svc/wordle/v2/" + d + ".json";
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .GET()
                    .build();

            HttpResponse<String> res = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            System.out.printf("%s → status %d, %d bytes%n",
                    d, res.statusCode(), res.body().length());

            if (res.statusCode() == 200) {
                String sol = M.readTree(res.body())
                        .get("solution")
                        .asText();
                out.put(d, sol);
            }
        }
        return out;
    }
    public static void main(String[] args) throws Exception {
        var history = fetchAll();
        // 1) CSV option:
        var csvLines = history.entrySet().stream()
                .map(e -> e.getKey() + "," + e.getValue())
                .collect(Collectors.toList());
        Files.write(Path.of("wordle_history.csv"), csvLines);

        // 2) JSON option: (im personally unfamiliar with JSON manipulation but could be useful for future)
        M.writerWithDefaultPrettyPrinter().writeValue(Path.of("wordle_history.json").toFile(), history);
    }
}