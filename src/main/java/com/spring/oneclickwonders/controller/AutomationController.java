package com.spring.oneclickwonders.controller;

import com.spring.oneclickwonders.model.ProjectRequest;
import com.spring.oneclickwonders.model.ProjectResponse;
import com.spring.oneclickwonders.service.GitHubService;
import com.spring.oneclickwonders.service.JenkinsService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AutomationController {
    @Autowired
    private GitHubService gitHubService;

    @Autowired
    private JenkinsService jenkinsService;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("projectRequest", new ProjectRequest());
        return "index";
    }

    @PostMapping("/create-project")
    @ResponseBody
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest request, BindingResult result) {

        if (result.hasErrors()) {
            return ProjectResponse.error("Validation failed: " + result.getAllErrors().get(0).getDefaultMessage());
        }

        String githubRepoUrl = null;
        String jenkinsJobUrl = null;

        try {
            // Step 1: Check if repository already exists
            if (gitHubService.repositoryExists(request.getRepositoryName())) {
                return ProjectResponse.error("GitHub repository '" + request.getRepositoryName() + "' already exists. Please choose a different name.");
            }

            // Step 2: Check if Jenkins job already exists
            if (jenkinsService.jenkinsJobExists(request.getJenkinsJobName())) {
                return ProjectResponse.error("Jenkins job '" + request.getJenkinsJobName() + "' already exists. Please choose a different name.");
            }

            // Step 3: Create GitHub Repository
            githubRepoUrl = gitHubService.createRepository(
                    request.getRepositoryName(),
                    request.getDescription(),
                    request.isPrivate()
            );

            // Step 4: Create Jenkins Job
            jenkinsJobUrl = jenkinsService.createJob(
                    request.getJenkinsJobName(),
                    githubRepoUrl,
                    request.getBuildScript(),
                    request.getDeployScript()
            );

            // Step 5: Create GitHub Webhook (Optional - connects GitHub to Jenkins)
            try {
              //  gitHubService.createWebhook(request.getRepositoryName(), jenkinsUrl);
            } catch (Exception webhookError) {
                // Webhook creation failed but main resources were created successfully
                return ProjectResponse.success(
                        "Project created successfully! Note: Webhook setup failed - you may need to configure it manually.",
                        githubRepoUrl,
                        jenkinsJobUrl
                );
            }

            return ProjectResponse.success(
                    "Project created successfully with automated CI/CD!",
                    githubRepoUrl,
                    jenkinsJobUrl
            );

        } catch (Exception e) {
            // If we created GitHub repo but Jenkins failed, inform user
            if (githubRepoUrl != null && jenkinsJobUrl == null) {
                return ProjectResponse.error("GitHub repository created successfully, but Jenkins job creation failed: " + e.getMessage() +
                        ". You may need to delete the repository manually if desired.");
            }

            return ProjectResponse.error("Failed to create project: " + e.getMessage());
        }
    }
    // New endpoint to check availability
    @PostMapping("/check-availability")
    @ResponseBody
    public Map<String, Object> checkAvailability(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String repoName = request.get("repositoryName");
            String jobName = request.get("jenkinsJobName");

            boolean repoExists = false;
            boolean jobExists = false;

            if (repoName != null && !repoName.trim().isEmpty()) {
                repoExists = gitHubService.repositoryExists(repoName);
            }

            if (jobName != null && !jobName.trim().isEmpty()) {
                jobExists = jenkinsService.jenkinsJobExists(jobName);
            }

            response.put("repositoryExists", repoExists);
            response.put("jenkinsJobExists", jobExists);
            response.put("success", true);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to check availability: " + e.getMessage());
        }

        return response;
    }
}
