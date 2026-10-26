package org.example.genaijavaspring.rag.rerank.client.impl;

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.example.genaijavaspring.rag.config.data.RagConfigData;
import org.example.genaijavaspring.rag.rerank.client.RerankerClient;
import org.example.genaijavaspring.rag.rerank.exception.RerankException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Component
public class CohereRerankerClient implements RerankerClient {

    private final RagConfigData.RerankProperties rerankProperties;
    private final ObjectMapper objectMapper;

    public CohereRerankerClient(RagConfigData ragConfigData, ObjectMapper objectMapper) {
        this.rerankProperties = ragConfigData.getRerank();
        this.objectMapper = objectMapper;
    }


    @Override
    public double[] score(String query, List<String> documents) throws RerankException {
        ObjectNode root = getJsonEntityFromRerank(query, documents);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            JsonNode node = executeRerankRequest(root, httpClient);
            return getScores(documents, node);
        } catch (IOException e) {
            throw new RerankException("Exception during rerank operation!", e);
        }
    }

    private ObjectNode getJsonEntityFromRerank(String query, List<String> documents) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", this.rerankProperties.getModel());
        root.put("query", query);
        ArrayNode docs = objectMapper.createArrayNode();
        for (String d : documents) {
            docs.add(d);
        }
        root.set("documents", docs);
        root.put("return_documents", false); // only return the scores
        return root;
    }

    private JsonNode executeRerankRequest(ObjectNode root, CloseableHttpClient httpClient) throws IOException {
        HttpPost httpPostRequest = new HttpPost(this.rerankProperties.getUrl());
        httpPostRequest.addHeader("Authorization", "Bearer " + this.rerankProperties.getApiKey());
        httpPostRequest.addHeader("Content-Type", "application/json");
        httpPostRequest.setEntity(new StringEntity(objectMapper.writeValueAsString(root), StandardCharsets.UTF_8));
        return httpClient.execute(httpPostRequest, response -> {
            int status = response.getCode();
            try (HttpEntity entity = response.getEntity()) {
                if (status < 200 || status >= 300) {
                    String body = entity == null ? "" : EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    throw new RerankException("Rerank request failed with HTTP status " + status + ": " + body);
                }
                return objectMapper.readTree(entity.getContent());
            }
        });
    }

    private double[] getScores(List<String> documents, JsonNode node) {
        JsonNode rerankResults = node.path("results");
        if (!rerankResults.isArray()) {
            throw new RerankException("Rerank response does not contain 'results': " + node);
        }
        double[] scores = new double[documents.size()];
        Arrays.fill(scores, Double.NEGATIVE_INFINITY); // documents not returned by the API sort last
        for (JsonNode rerankResult : rerankResults) {
            int index = rerankResult.path("index").asInt(-1);
            double score = rerankResult.path("relevance_score").asDouble(Double.NEGATIVE_INFINITY);
            if (index >= 0 && index < scores.length) {
                scores[index] = score;
            }
        }
        return scores;
    }
}
