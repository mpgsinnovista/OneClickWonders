package com.spring.oneclickwonders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubService {
    @Value("${app.github.token}")
    private String githubToken;

    @Value("${app.github.api-url}")
    private String githubApiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GitHubService() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String createRepository(String repositoryName, String description, boolean isPrivate) {
        try {
            // First check if repository already exists
            if (repositoryExists(repositoryName)) {
                throw new RuntimeException("Repository '" + repositoryName + "' already exists. Please choose a different name.");
            }

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("name", repositoryName);
            requestBody.put("description", description);
            requestBody.put("private", isPrivate);
            requestBody.put("auto_init", true); // Creates README.md

            Map<String, Object> response = webClient.post()
                    .uri("/user/repos")
                    .header(HttpHeaders.AUTHORIZATION, "token " + githubToken)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return (String) response.get("html_url");

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 422) {
                // GitHub returns 422 when repository name already exists
                throw new RuntimeException("Repository '" + repositoryName + "' already exists. Please choose a different name.");
            } else if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("GitHub authentication failed. Please check your access token.");
            } else if (e.getStatusCode().value() == 403) {
                throw new RuntimeException("GitHub access forbidden. Check your token permissions.");
            }
            throw new RuntimeException("Failed to create GitHub repository: " + e.getResponseBodyAsString(), e);
        }
    }
    public boolean repositoryExists(String repositoryName) {
        try {
            String currentUser = getCurrentUser();
            webClient.get()
                    .uri("/repos/{owner}/{repo}", currentUser, repositoryName)
                    .header(HttpHeaders.AUTHORIZATION, "token " + githubToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            return true; // If we get here, repo exists
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false; // Repo doesn't exist
            }
            // For other errors, assume it exists to be safe
            return true;
        }
    }

    public void createWebhook(String repositoryName, String jenkinsUrl) {
        try {
            Map<String, Object> webhookConfig = new HashMap<>();
            webhookConfig.put("url", jenkinsUrl + "/github-webhook/");
            webhookConfig.put("content_type", "json");

            Map<String, Object> recreateWebhookquestBody = new HashMap<>();
            recreateWebhookquestBody.put("name", "web");
            recreateWebhookquestBody.put("active", true);
            recreateWebhookquestBody.put("events", new String[]{"push", "pull_request"});
            recreateWebhookquestBody.put("config", webhookConfig);

            webClient.post()
                    .uri("/repos/{owner}/{repo}/hooks", getCurrentUser(), repositoryName)
                    .header(HttpHeaders.AUTHORIZATION, "token " + githubToken)
                    .bodyValue(recreateWebhookquestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

        } catch (WebClientResponseException e) {
            throw new RuntimeException("Failed to create GitHub webhook: " + e.getResponseBodyAsString(), e);
        }
    }

    private String getCurrentUser() {
        try {
            Map<String, Object> response = webClient.get()
                    .uri("/user")
                    .header(HttpHeaders.AUTHORIZATION, "token " + githubToken)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return (String) response.get("login");
        } catch (WebClientResponseException e) {
            throw new RuntimeException("Failed to get current user: " + e.getResponseBodyAsString(), e);
        }
    }
}
