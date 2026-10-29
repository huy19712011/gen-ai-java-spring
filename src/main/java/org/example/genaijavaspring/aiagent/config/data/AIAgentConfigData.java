package org.example.genaijavaspring.aiagent.config.data;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.agent")
public class AIAgentConfigData {

    private String uploadDir;
    private DiagramToolProperties diagramTool;
    private PostureToolProperties postureTool;

    @Data
    public static class DiagramToolProperties {
        private Double temperature;
    }

    @Data
    public static class PostureToolProperties {
        private String url;
        private String env;
    }

}
