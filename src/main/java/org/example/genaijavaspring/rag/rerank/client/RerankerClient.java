package org.example.genaijavaspring.rag.rerank.client;

import org.example.genaijavaspring.rag.rerank.exception.RerankException;

import java.util.List;

public interface RerankerClient {

    double[] score(String query, List<String> documents) throws RerankException;
}
