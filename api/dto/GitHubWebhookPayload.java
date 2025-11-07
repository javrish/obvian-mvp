package api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * GitHub webhook payload for pull_request events.
 *
 * <p>Represents the JSON payload sent by GitHub when a pull request event occurs.
 *
 * @see <a href="https://docs.github.com/webhooks/event-payloads/#pull_request">GitHub Webhook
 *     Documentation</a>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubWebhookPayload {

  private String action;
  private PullRequest pullRequest;
  private Repository repository;

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  @JsonProperty("pull_request")
  public PullRequest getPullRequest() {
    return pullRequest;
  }

  public void setPullRequest(PullRequest pullRequest) {
    this.pullRequest = pullRequest;
  }

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class PullRequest {
    private long number;
    private String title;
    private Head head;
    private Base base;

    public long getNumber() {
      return number;
    }

    public void setNumber(long number) {
      this.number = number;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public Head getHead() {
      return head;
    }

    public void setHead(Head head) {
      this.head = head;
    }

    public Base getBase() {
      return base;
    }

    public void setBase(Base base) {
      this.base = base;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Head {
    private String ref;
    private String sha;
    private Repository repo;

    public String getRef() {
      return ref;
    }

    public void setRef(String ref) {
      this.ref = ref;
    }

    public String getSha() {
      return sha;
    }

    public void setSha(String sha) {
      this.sha = sha;
    }

    public Repository getRepo() {
      return repo;
    }

    public void setRepo(Repository repo) {
      this.repo = repo;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Base {
    private String ref;
    private String sha;

    public String getRef() {
      return ref;
    }

    public void setRef(String ref) {
      this.ref = ref;
    }

    public String getSha() {
      return sha;
    }

    public void setSha(String sha) {
      this.sha = sha;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Repository {
    private String name;
    private String fullName;
    private Owner owner;
    private boolean isPrivate;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    @JsonProperty("full_name")
    public String getFullName() {
      return fullName;
    }

    public void setFullName(String fullName) {
      this.fullName = fullName;
    }

    public Owner getOwner() {
      return owner;
    }

    public void setOwner(Owner owner) {
      this.owner = owner;
    }

    @JsonProperty("private")
    public boolean isPrivate() {
      return isPrivate;
    }

    public void setPrivate(boolean isPrivate) {
      this.isPrivate = isPrivate;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Owner {
    private String login;
    private String type;

    public String getLogin() {
      return login;
    }

    public void setLogin(String login) {
      this.login = login;
    }

    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }
  }
}
