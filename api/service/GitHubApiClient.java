package api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Client for interacting with GitHub REST API.
 *
 * <p>Provides methods to fetch workflow files, create check runs, and interact with GitHub
 * repositories.
 *
 * @see <a href="https://docs.github.com/rest">GitHub REST API Documentation</a>
 */
@Service
public class GitHubApiClient {

  private static final Logger logger = LoggerFactory.getLogger(GitHubApiClient.class);
  private static final String GITHUB_API_BASE = "https://api.github.com";

  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String githubToken;

  public GitHubApiClient(
      ObjectMapper objectMapper,
      @Value("${obvian.github.token:}") String githubToken) {
    this.httpClient =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    this.objectMapper = objectMapper;
    this.githubToken = githubToken;
  }

  /**
   * Fetch workflow file content from a repository.
   *
   * @param owner Repository owner (username or organization)
   * @param repo Repository name
   * @param path Path to workflow file (e.g., ".github/workflows/ci.yml")
   * @param ref Git reference (branch, tag, or commit SHA)
   * @return Workflow YAML content as string
   * @throws IOException if API request fails or file not found
   */
  public String getWorkflowFile(String owner, String repo, String path, String ref)
      throws IOException {
    String url =
        String.format("%s/repos/%s/%s/contents/%s?ref=%s", GITHUB_API_BASE, owner, repo, path, ref);

    logger.info("Fetching workflow file: owner={}, repo={}, path={}, ref={}", owner, repo, path, ref);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/vnd.github.v3+json")
            .header("Authorization", "Bearer " + githubToken)
            .header("User-Agent", "Obvian-Workflow-Verify")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 404) {
        throw new IOException(
            String.format("Workflow file not found: %s/%s/%s", owner, repo, path));
      }

      if (response.statusCode() != 200) {
        throw new IOException(
            String.format(
                "GitHub API error: status=%d, body=%s", response.statusCode(), response.body()));
      }

      // Parse JSON response
      JsonNode root = objectMapper.readTree(response.body());

      // Check if content is base64 encoded
      String encoding = root.path("encoding").asText();
      if (!"base64".equals(encoding)) {
        throw new IOException("Unexpected encoding: " + encoding);
      }

      // Decode base64 content
      String base64Content = root.path("content").asText().replaceAll("\\s", "");
      byte[] decodedBytes = Base64.getDecoder().decode(base64Content);
      String content = new String(decodedBytes, StandardCharsets.UTF_8);

      logger.info(
          "Successfully fetched workflow file: {} bytes (owner={}, repo={}, path={})",
          content.length(),
          owner,
          repo,
          path);

      return content;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }

  /**
   * List workflow files in a repository.
   *
   * @param owner Repository owner
   * @param repo Repository name
   * @param ref Git reference
   * @return Array of workflow file paths
   * @throws IOException if API request fails
   */
  public String[] listWorkflowFiles(String owner, String repo, String ref) throws IOException {
    String url =
        String.format(
            "%s/repos/%s/%s/contents/.github/workflows?ref=%s", GITHUB_API_BASE, owner, repo, ref);

    logger.info("Listing workflow files: owner={}, repo={}, ref={}", owner, repo, ref);

    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/vnd.github.v3+json")
            .header("Authorization", "Bearer " + githubToken)
            .header("User-Agent", "Obvian-Workflow-Verify")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build();

    try {
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 404) {
        logger.warn("No .github/workflows directory found: {}/{}", owner, repo);
        return new String[0];
      }

      if (response.statusCode() != 200) {
        throw new IOException(
            String.format(
                "GitHub API error: status=%d, body=%s", response.statusCode(), response.body()));
      }

      // Parse JSON array
      JsonNode files = objectMapper.readTree(response.body());
      String[] paths = new String[files.size()];

      for (int i = 0; i < files.size(); i++) {
        paths[i] = files.get(i).path("path").asText();
      }

      logger.info("Found {} workflow files in {}/{}", paths.length, owner, repo);
      return paths;

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Request interrupted", e);
    }
  }
}
