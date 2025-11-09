package org.monke.connector;

import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.JSONArray;
import org.monke.connector.config.ConnectorConfig;
import org.monke.connector.util.RelsUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * <p>HTTP client for making requests to the GitHub API. Handles rate limiting and request building.</p>
 * <p>Only handles JSON. Deserialization is API version dependent and is handled within the task.</p>
 * <p>The client follows SRP by only handling requests with rate limitations.</p>
 * <p>Rate limitation state is tracked here for <b>encapsulation</b> since sleep methods requiring it are accessed from the task.</p>
 * <p>For pages greater than 1 for a given timestamp, next issues are fetched through HATEOAS by parsing the returned rels for next URL,
 * as per GitHub API requirements.</p>
 */
@Slf4j
public class HttpClient {

    // API limitations state.
    private Integer xRateLimit = 9999;
    private Integer xRateRemaining = 9999;
    private long xRateReset = Instant.MAX.getEpochSecond();

    // Pagination state.
    private String nextPage = "";

    private final OkHttpClient client;
    private final ConnectorConfig config;


    public HttpClient(ConnectorConfig config, OkHttpClient client) {
        this.client = client;
        this.config = config;
    }

    /**
     * <p>Gets issues after the given timestamp with pagination.</p>
     * <p>Discovers next page URL through HATEOAS if not the first page for given timestamp.</p>
     */
    protected JSONArray fetchIssues(Integer page, Instant since) throws InterruptedException {
        String url = page == 1 ? buildUrl(since) : nextPage;
        Request request = buildRequest(url);

        try (Response response = client.newCall(request).execute()) {
            log.debug("GET {}", request.url());

            // Updates rate limit state.
            Headers headers = response.headers();
            xRateLimit = Integer.parseInt(Objects.requireNonNull(headers.get("X-RateLimit-Limit")));
            xRateRemaining = Integer.parseInt(Objects.requireNonNull(headers.get("X-RateLimit-Remaining")));
            xRateReset = Long.parseLong(Objects.requireNonNull(headers.get("X-RateLimit-Reset")));

            // Discovers next page.
            nextPage = RelsUtils.getNextPage(headers.get("Link"));

            switch (response.code()) {
                case 200 -> {
                    return new JSONArray(Objects.requireNonNull(response.body()).string());
                }
                case 401 ->
                    throw new RuntimeException("Authentication failed : " + response.body().string());
                case 403 -> {
                    log.warn("Rate limit reached : {}/{}. Reset at {}.", xRateRemaining, xRateLimit,
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(xRateReset), ZoneOffset.systemDefault()));
                    sleep();
                    return fetchIssues(page, since);
                }
                default ->
                    throw new RuntimeException("Unexpected response code : " + response.code() + " with message : " + response.body().string());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Request buildRequest(String url) {
        Request.Builder requestBuilder = new Request.Builder()
            .addHeader("Content-Type", "application/json")
            .url(url);

        if (!config.getAuthUsername().isEmpty() && !config.getAuthPassword().isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + config.getAuthUsername() + ":" + config.getAuthPassword());
        }
        return requestBuilder.build();
    }

    /**
     * <p>Simple URL builder without using {@link HttpUrl} methods to keep it straightforward.</p>
     * <p>Builds URL for first page. Next pages for the same timestamp are fetched through HATEOAS.</p>
     */
    private String buildUrl(Instant since) {
        return String.format(
            "https://api.github.com/repos/%s/%s/issues?page=%s&per_page=%s&since=%s&state=all&direction=asc&sort=updated",
            config.getOwner(),
            config.getRepo(),
            1,
            config.getBatchSize(),
            since.toString()
        );
    }

    /**
     * Sleeps until right after (closest superior integer) the rate limit resets.
     */
    public void sleep() throws InterruptedException {
        long sleepTime = (long) Math.ceil((double) (xRateReset - Instant.now().getEpochSecond()) / xRateRemaining);

        log.info("Sleeping for {} seconds.", sleepTime);

        Thread.sleep(1000 * sleepTime);
    }

    /**
     * Sleeps if the remaining requests are 10 or less.
     */
    public void sleepIfNeeded() throws InterruptedException {
        if (0 < xRateRemaining && xRateRemaining <= 10) {
            log.info("Issues fetching : approaching limit soon, {} requests left. Going to sleep...", xRateRemaining);
            sleep();
        }
    }
}
