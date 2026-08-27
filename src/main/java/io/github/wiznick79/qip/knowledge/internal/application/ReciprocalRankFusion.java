package io.github.wiznick79.qip.knowledge.internal.application;

import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class ReciprocalRankFusion {

    private static final int RANK_CONSTANT = 60;
    private static final double MAXIMUM_SCORE = 2.0 / (RANK_CONSTANT + 1);

    private ReciprocalRankFusion() {}

    static List<RetrievedPassage> fuse(List<RetrievedPassage> semantic, List<RetrievedPassage> lexical, int limit) {
        Map<UUID, Candidate> candidates = new LinkedHashMap<>();
        addRanking(candidates, semantic);
        addRanking(candidates, lexical);
        return candidates.values().stream()
                .sorted(Comparator.comparingDouble(Candidate::score)
                        .reversed()
                        .thenComparing(candidate -> candidate.passage().passageId()))
                .limit(limit)
                .map(candidate -> withScore(candidate.passage(), candidate.score() / MAXIMUM_SCORE))
                .toList();
    }

    private static void addRanking(Map<UUID, Candidate> candidates, List<RetrievedPassage> ranking) {
        for (int index = 0; index < ranking.size(); index++) {
            RetrievedPassage passage = ranking.get(index);
            double contribution = 1.0 / (RANK_CONSTANT + index + 1);
            candidates.compute(
                    passage.passageId(),
                    (id, existing) -> existing == null
                            ? new Candidate(passage, contribution)
                            : new Candidate(existing.passage(), existing.score() + contribution));
        }
    }

    private static RetrievedPassage withScore(RetrievedPassage passage, double score) {
        return new RetrievedPassage(
                passage.passageId(),
                passage.documentId(),
                passage.documentTitle(),
                passage.pageNumber(),
                passage.sequence(),
                passage.text(),
                score);
    }

    private record Candidate(RetrievedPassage passage, double score) {}
}
