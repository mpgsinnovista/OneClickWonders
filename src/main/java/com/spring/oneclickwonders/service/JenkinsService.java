package com.spring.oneclickwonders.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class JenkinsService {
    @Value("${app.jenkins.url}")
    private String jenkinsUrl;

    @Value("${app.jenkins.username}")
    private String jenkinsUsername;

    @Value("${app.jenkins.token}")
    private String jenkinsToken;

    private final WebClient webClient;

    public JenkinsService() {
        this.webClient = WebClient.builder().build();
    }

    public String createJob(String jobName, String githubRepoUrl, String buildScript, String deployScript) {
        try {
            // First check if job already exists
            if (jenkinsJobExists(jobName)) {
                throw new RuntimeException("Jenkins job '" + jobName + "' already exists. Please choose a different name.");
            }

            String jobConfig = generateJobConfig(githubRepoUrl, buildScript, deployScript);

            String auth = Base64.getEncoder().encodeToString(
                    (jenkinsUsername + ":" + jenkinsToken).getBytes(StandardCharsets.UTF_8)
            );

            webClient.post()
                    .uri(jenkinsUrl + "/createItem?name=" + jobName)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .header(HttpHeaders.CONTENT_TYPE, "application/xml")
                    .bodyValue(jobConfig)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return jenkinsUrl + "/job/" + jobName;

        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 400) {
                // Jenkins returns 400 when job already exists
                throw new RuntimeException("Jenkins job '" + jobName + "' already exists. Please choose a different name.");
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                throw new RuntimeException("Jenkins authentication failed. Please check your username and API token.");
            } else if (e.getStatusCode().value() == 404) {
                throw new RuntimeException("Jenkins server not found. Please check Jenkins URL: " + jenkinsUrl);
            }
            throw new RuntimeException("Failed to create Jenkins job: " + e.getResponseBodyAsString(), e);
        }
    }

    public boolean jenkinsJobExists(String jobName) {
        try {
            String auth = Base64.getEncoder().encodeToString(
                    (jenkinsUsername + ":" + jenkinsToken).getBytes(StandardCharsets.UTF_8)
            );

            webClient.get()
                    .uri(jenkinsUrl + "/job/" + jobName + "/api/json")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return true; // If we get here, job exists
        } catch (WebClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                return false; // Job doesn't exist
            }
            // For other errors, assume it exists to be safe
            return true;
        }
    }

    private String generateJobConfig(String githubRepoUrl, String buildScript, String deployScript) {
        return """
            <?xml version='1.1' encoding='UTF-8'?>
            <project>
              <actions/>
              <description>Auto-generated CI/CD pipeline</description>
              <keepDependencies>false</keepDependencies>
              <properties>
                <com.coravy.hudson.plugins.github.GithubProjectProperty plugin="github@1.37.0">
                  <projectUrl>%s</projectUrl>
                  <displayName></displayName>
                </com.coravy.hudson.plugins.github.GithubProjectProperty>
              </properties>
              <scm class="hudson.plugins.git.GitSCM" plugin="git@4.11.3">
                <configVersion>2</configVersion>
                <userRemoteConfigs>
                  <hudson.plugins.git.UserRemoteConfig>
                    <url>%s</url>
                  </hudson.plugins.git.UserRemoteConfig>
                </userRemoteConfigs>
                <branches>
                  <hudson.plugins.git.BranchSpec>
                    <name>*/main</name>
                  </hudson.plugins.git.BranchSpec>
                </branches>
                <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
                <submoduleCfg class="empty-list"/>
                <extensions/>
              </scm>
              <canRoam>true</canRoam>
              <disabled>false</disabled>
              <blockBuildWhenDownstreamBuilding>false</blockBuildWhenDownstreamBuilding>
              <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>
              <triggers>
                <com.cloudbees.jenkins.GitHubPushTrigger plugin="github@1.37.0">
                  <spec></spec>
                </com.cloudbees.jenkins.GitHubPushTrigger>
              </triggers>
              <concurrentBuild>false</concurrentBuild>
              <builders>
                <hudson.tasks.Shell>
                  <command>%s</command>
                </hudson.tasks.Shell>
                <hudson.tasks.Shell>
                  <command>%s</command>
                </hudson.tasks.Shell>
              </builders>
              <publishers/>
              <buildWrappers/>
            </project>
            """.formatted(githubRepoUrl, githubRepoUrl, buildScript, deployScript);
    }
}
