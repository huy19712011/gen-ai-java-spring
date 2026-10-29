package org.example.postureservice.controller;

import org.example.postureservice.service.PostureService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/posture")
public class PostureController {

    private final PostureService postureService;
    public PostureController(PostureService postureService) {
        this.postureService = postureService;
    }

    @GetMapping("/{serviceId}")
    public Map<String, Object> getPostureByServiceIdAndEnv(@PathVariable String serviceId,
                                                           @RequestParam(defaultValue = "prod") String env) {
        return postureService.getPosture(serviceId, env);
    }
}
