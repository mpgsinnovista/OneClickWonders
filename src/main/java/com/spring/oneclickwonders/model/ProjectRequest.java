package com.spring.oneclickwonders.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProjectRequest {

    @NotBlank(message = "Repository name is required")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Invalid repository name")
    private String repositoryName;

    @NotBlank(message = "Description is required")
    private String description;

    private boolean isPrivate = false;

    @NotBlank(message = "Jenkins job name is required")
    private String jenkinsJobName;

    private String buildScript = "mvn clean compile test";
    private String deployScript = "mvn clean package";

    // Constructors
    public ProjectRequest() {
    }

    public ProjectRequest(String repositoryName, String description, boolean isPrivate,
                          String jenkinsJobName, String buildScript, String deployScript) {
        this.repositoryName = repositoryName;
        this.description = description;
        this.isPrivate = isPrivate;
        this.jenkinsJobName = jenkinsJobName;
        this.buildScript = buildScript;
        this.deployScript = deployScript;
    }

    // Getters and Setters
    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }

    public String getJenkinsJobName() { return jenkinsJobName; }
    public void setJenkinsJobName(String jenkinsJobName) { this.jenkinsJobName = jenkinsJobName; }

    public String getBuildScript() { return buildScript; }
    public void setBuildScript(String buildScript) { this.buildScript = buildScript; }

    public String getDeployScript() { return deployScript; }
    public void setDeployScript(String deployScript) { this.deployScript = deployScript; }

}