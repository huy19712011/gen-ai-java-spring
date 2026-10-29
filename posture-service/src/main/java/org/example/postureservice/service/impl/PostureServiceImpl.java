package org.example.postureservice.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.postureservice.dataaccess.entity.SecurityPostureEntity;
import org.example.postureservice.dataaccess.repository.SecurityPostureRepository;
import org.example.postureservice.service.PostureService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class PostureServiceImpl implements PostureService {

    private final SecurityPostureRepository securityPostureRepository;

    public PostureServiceImpl(SecurityPostureRepository securityPostureRepository) {
        this.securityPostureRepository = securityPostureRepository;
    }

    @Override
    public Map<String, Object> getPosture(String serviceId, String env) {
        SecurityPostureEntity securityPosture = securityPostureRepository.findByServiceIdAndEnvironment(serviceId, env)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Posture not found!"));
        log.info("Found posture for serviceId: {} in env: {}", serviceId, env);
        return getSecurityPostureMap(securityPosture);
    }

    private Map<String, Object> getSecurityPostureMap(SecurityPostureEntity securityPosture) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("serviceId", securityPosture.getServiceId());
        out.put("environment", securityPosture.getEnvironment());
        out.put("internetFacing", securityPosture.isInternetFacing());
        out.put("authn", securityPosture.getAuthn());
        out.put("data", securityPosture.getData());
        out.put("tls", securityPosture.getTls());
        out.put("network", securityPosture.getNetwork());
        out.put("secrets", securityPosture.getSecrets());
        out.put("vulns", securityPosture.getVulns());
        return out;
    }
}
