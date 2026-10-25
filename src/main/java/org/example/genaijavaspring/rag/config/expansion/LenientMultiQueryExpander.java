package org.example.genaijavaspring.rag.config.expansion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Multi-query expander that tolerates imperfect LLM output.
 *
 * <p>Unlike Spring AI's {@code MultiQueryExpander}, which discards all variants unless the
 * response contains exactly N newline-separated lines, this implementation:
 * <ul>
 *     <li>strips list markers ("1.", "2)", "-", "*") and surrounding quotes,</li>
 *     <li>trims whitespace and drops blank lines before counting,</li>
 *     <li>removes duplicates and variants identical to the original query,</li>
 *     <li>keeps whatever valid variants were produced (up to N) instead of falling back to the original only,</li>
 *     <li>falls back to the original query only when nothing usable remains.</li>
 * </ul>
 */
@Slf4j
public class LenientMultiQueryExpander implements QueryExpander {

    private static final Pattern LIST_MARKER = Pattern.compile("^\\s*(?:[-*\u2022]+|\\(?\\d+[.)])\\s*");
    private static final Pattern WRAPPING_QUOTES = Pattern.compile("^[\"'\u201C\u201D\u2018\u2019]+|[\"'\u201C\u201D\u2018\u2019]+$");
    private static final int MAX_WORDS_PER_VARIANT = 40; // longer lines are almost certainly prose, not paraphrases

    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;
    private final int numberOfQueries;
    private final boolean includeOriginal;

    public LenientMultiQueryExpander(ChatClient.Builder chatClientBuilder,
                                     PromptTemplate promptTemplate,
                                     int numberOfQueries,
                                     boolean includeOriginal) {
        Assert.notNull(chatClientBuilder, "chatClientBuilder cannot be null");
        Assert.notNull(promptTemplate, "promptTemplate cannot be null");
        Assert.isTrue(numberOfQueries > 0, "numberOfQueries must be greater than 0");

        this.chatClient = chatClientBuilder.build();
        this.promptTemplate = promptTemplate;
        this.numberOfQueries = numberOfQueries;
        this.includeOriginal = includeOriginal;
    }

    @Override
    public List<Query> expand(Query query) {
        Assert.notNull(query, "query cannot be null");

        String prompt = promptTemplate.render(Map.of("number", numberOfQueries, "query", query.text()));

        String response;
        try {
            response = chatClient.prompt().user(prompt).call().content();
        } catch (RuntimeException ex) {
            log.warn("Query expansion call failed, using original query only: {}", ex.getMessage());
            return List.of(query);
        }

        List<String> variants = parseVariants(response, query.text());

        if (variants.isEmpty()) {
            log.warn("Query expansion produced no usable variants for '{}', using original query only", query.text());
            return List.of(query);
        }
        if (variants.size() < numberOfQueries) {
            log.warn("Query expansion produced {} of {} requested variants for '{}'",
                    variants.size(), numberOfQueries, query.text());
        }

        List<Query> expanded = new ArrayList<>(variants.size() + 1);
        if (includeOriginal) {
            expanded.add(query);
        }
        for (String variant : variants) {
            expanded.add(query.mutate().text(variant).build());
        }

        log.debug("Expanded query '{}' into {}", query.text(), expanded.stream().map(Query::text).toList());
        return expanded;
    }

    /**
     * Parses raw LLM output into at most {@code numberOfQueries} clean, unique variants.
     */
    List<String> parseVariants(String response, String originalText) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        seen.add(normalizeForComparison(originalText));

        List<String> variants = new ArrayList<>();
        for (String line : response.split("\\R")) {
            String candidate = LIST_MARKER.matcher(line).replaceFirst("");
            candidate = WRAPPING_QUOTES.matcher(candidate).replaceAll("").strip();

            if (candidate.isEmpty() || candidate.split("\\s+").length > MAX_WORDS_PER_VARIANT) {
                continue;
            }
            if (seen.add(normalizeForComparison(candidate))) {
                variants.add(candidate);
            }
            if (variants.size() == numberOfQueries) {
                break;
            }
        }
        return variants;
    }

    private static String normalizeForComparison(String text) {
        return text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
