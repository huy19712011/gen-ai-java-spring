package org.example.genaijavaspring.rag.config.postprocessor;

import org.example.genaijavaspring.rag.config.data.PgVectorStoreConfigData;
import org.example.genaijavaspring.rag.config.data.RagConfigData;
import org.example.genaijavaspring.rag.config.data.RagConstants;
import org.example.genaijavaspring.rag.config.postprocessor.record.Target;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Component
public class NeighbourStitchPostProcessor implements DocumentPostProcessor {

    private final JdbcTemplate jdbcTemplate;
    private final String ragTableName;
    private final RagConfigData ragConfigData;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    public NeighbourStitchPostProcessor(JdbcTemplate jdbcTemplate,
                                        PgVectorStoreConfigData pgVectorStoreConfigData,
                                        RagConfigData ragConfigData) {

        this.jdbcTemplate = jdbcTemplate;
        this.ragTableName = pgVectorStoreConfigData.getTableNameForRag();
        this.ragConfigData = ragConfigData;
    }

    @Override
    public List<Document> process(Query query, List<Document> documents) {

        if (documents.isEmpty()) {
            return documents;
        }

        Set<Target> targets = getTargetDocuments(documents);
        List<Document> neighbours = getNeighbourDocuments(targets);
        List<Document> merged = getMergedDocuments(documents, neighbours);

        return dedupBySourceIndexPreserveOrder(merged);
    }

    private Set<Target> getTargetDocuments(List<Document> documents) {
        Set<Target> targets = new LinkedHashSet<>();

        for (var document : documents) {
            String source = String.valueOf(document.getMetadata().get(RagConstants.SOURCE));
            Object indexObj = document.getMetadata().get(RagConstants.CHUNK_INDEX);
            if (source == null || indexObj == null) {
                continue;
            }
            int index = (indexObj instanceof Number) ? ((Number) indexObj).intValue()
                    : Integer.parseInt(String.valueOf(indexObj));
            for (int delta = 1; delta <= ragConfigData.getRadius(); delta++) {
                targets.add(new Target(source, index - delta));
                targets.add(new Target(source, index + delta));
            }
        }

        return targets;
    }

    private List<Document> getNeighbourDocuments(Set<Target> targets) {

        List<Document> neighbours = new ArrayList<>();
        for (Target target : targets) {
            neighbours.addAll(findChunk(target));
        }
        return neighbours;
    }

    /**
     * Exact metadata lookup of a single chunk.
     * <p>
     * A similarity search is not used here: pgvector applies the metadata filter after the HNSW
     * index scan, so an arbitrary query vector (e.g. "__neighbour__") usually returns no row for
     * a specific source/chunk_index once the index is used.
     */
    private List<Document> findChunk(Target target) {
        String sql = """
                SELECT id, content, metadata
                FROM %s
                WHERE metadata->>'source' = ?
                  AND (metadata->>'chunk_index')::int = ?
                """.formatted(ragTableName);
        return jdbcTemplate.query(sql, (rs, rowNum) -> toDocument(rs), target.source(), target.index());
    }

    @SuppressWarnings("unchecked")
    private Document toDocument(ResultSet rs) throws SQLException {
        Map<String, Object> metadata = (Map<String, Object>) jsonMapper.readValue(rs.getString("metadata"), Map.class);
        return Document.builder()
                .id(rs.getString("id"))
                .text(rs.getString("content"))
                .metadata(metadata)
                .build();
    }

    private List<Document> getMergedDocuments(List<Document> documents, List<Document> neighbours) {
        List<Document> merged = new ArrayList<>(documents.size() + neighbours.size());
        merged.addAll(documents);
        merged.addAll(neighbours);
        return merged;
    }

    private List<Document> dedupBySourceIndexPreserveOrder(List<Document> merged) {
        Set<String> seen = new HashSet<>();
        List<Document> outputs = new ArrayList<>(merged.size());
        for (var document : merged) {
            String source = String.valueOf(document.getMetadata().get(RagConstants.SOURCE));
            Object indexObj = document.getMetadata().get(RagConstants.CHUNK_INDEX);
            String key = source + "#" + indexObj;
            if (seen.add(key)) {
                outputs.add(document);
            }
        }
        return outputs;
    }
}
