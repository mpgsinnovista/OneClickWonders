package com.spring.oneclickwonders.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectResponse {
    private boolean success;
    private String message;
    private String githubRepoUrl;
    private String jenkinsJobUrl;
    private String error;

    // Constructors
    public ProjectResponse() {
    }

    public static ProjectResponse success(String message, String githubRepoUrl, String jenkinsJobUrl) {
        ProjectResponse response = new ProjectResponse();
        response.success = true;
        response.message = message;
        response.githubRepoUrl = githubRepoUrl;
        response.jenkinsJobUrl = jenkinsJobUrl;
        return response;
    }

    public static ProjectResponse error(String error) {
        ProjectResponse response = new ProjectResponse();
        response.success = false;
        response.error = error;
        return response;
    }
}