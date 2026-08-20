package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.incidents.api.IncidentCatalog;
import io.github.wiznick79.qip.incidents.api.IncidentSnapshot;
import io.github.wiznick79.qip.investigations.api.AnswerStatus;
import io.github.wiznick79.qip.investigations.api.CitationSnapshot;
import io.github.wiznick79.qip.investigations.api.InvestigationNotFoundException;
import io.github.wiznick79.qip.investigations.api.InvestigationSnapshot;
import io.github.wiznick79.qip.investigations.api.QuestionAnswerSnapshot;
import io.github.wiznick79.qip.investigations.internal.domain.Investigation;
import io.github.wiznick79.qip.investigations.internal.domain.InvestigationQuestion;
import io.github.wiznick79.qip.knowledge.api.KnowledgeQuery;
import io.github.wiznick79.qip.knowledge.api.KnowledgeSearch;
import io.github.wiznick79.qip.knowledge.api.RetrievedPassage;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InvestigationManagement {

    private static final String INSUFFICIENT_MESSAGE =
            "The indexed sources do not provide enough evidence to answer this question.";

    private final InvestigationRepository repository;
    private final IncidentCatalog incidents;
    private final KnowledgeSearch knowledge;
    private final GroundedPromptBuilder prompts;
    private final AnswerGenerator answers;
    private final InvestigationIdGenerator investigationIds;
    private final QuestionIdGenerator questionIds;
    private final Clock clock;
    private final int retrievalLimit;
    private final double minimumRelevanceScore;

    public InvestigationManagement(
            InvestigationRepository repository,
            IncidentCatalog incidents,
            KnowledgeSearch knowledge,
            GroundedPromptBuilder prompts,
            AnswerGenerator answers,
            InvestigationIdGenerator investigationIds,
            QuestionIdGenerator questionIds,
            Clock clock,
            @Value("${qip.investigations.retrieval-limit}") int retrievalLimit,
            @Value("${qip.investigations.minimum-relevance-score}") double minimumRelevanceScore) {
        if (retrievalLimit < 1 || retrievalLimit > 20) {
            throw new IllegalArgumentException("retrieval limit must be between 1 and 20");
        }
        if (!Double.isFinite(minimumRelevanceScore) || minimumRelevanceScore < -1 || minimumRelevanceScore > 1) {
            throw new IllegalArgumentException("minimum relevance score must be between -1 and 1");
        }
        this.repository = repository;
        this.incidents = incidents;
        this.knowledge = knowledge;
        this.prompts = prompts;
        this.answers = answers;
        this.investigationIds = investigationIds;
        this.questionIds = questionIds;
        this.clock = clock;
        this.retrievalLimit = retrievalLimit;
        this.minimumRelevanceScore = minimumRelevanceScore;
    }

    public InvestigationSnapshot create(UUID incidentId) {
        incidents.getIncident(incidentId);
        Instant now = Instant.now(clock);
        Investigation investigation =
                repository.createIfAbsent(new Investigation(investigationIds.nextId(), incidentId, now, now));
        return snapshot(investigation);
    }

    public InvestigationSnapshot get(UUID investigationId) {
        return snapshot(find(investigationId));
    }

    public QuestionAnswerSnapshot ask(UUID investigationId, AskQuestionCommand command) {
        Investigation investigation = find(investigationId);
        IncidentSnapshot incident = incidents.getIncident(investigation.incidentId());
        Instant askedAt = Instant.now(clock);
        InvestigationQuestion processing = repository.startQuestion(new InvestigationQuestion(
                questionIds.nextId(),
                investigation.id(),
                command.question(),
                command.documentIds(),
                AnswerStatus.PROCESSING,
                null,
                List.of(),
                null,
                GroundedPromptBuilder.VERSION,
                0,
                null,
                askedAt,
                null));

        InvestigationQuestion completed;
        int retrievedCount = 0;
        try {
            String retrievalText = command.question() + " " + incident.title() + " "
                    + (incident.description() == null ? "" : incident.description());
            List<RetrievedPassage> retrieved =
                    knowledge.search(new KnowledgeQuery(retrievalText, command.documentIds(), retrievalLimit));
            retrievedCount = retrieved.size();
            List<RetrievedPassage> relevant = retrieved.stream()
                    .filter(passage -> passage.score() >= minimumRelevanceScore)
                    .toList();
            if (relevant.isEmpty()) {
                completed = insufficient(processing, retrievedCount, Instant.now(clock));
            } else {
                GroundedPrompt prompt = prompts.build(command.question(), incident, relevant);
                if (prompt.passages().isEmpty()) {
                    completed = insufficient(processing, retrievedCount, Instant.now(clock));
                } else {
                    completed = validateAnswer(processing, answers.generate(prompt), prompt, retrievedCount);
                }
            }
        } catch (RuntimeException exception) {
            completed = technicalFailure(processing, exception, retrievedCount);
        }
        Investigation updated = new Investigation(
                investigation.id(), investigation.incidentId(), investigation.createdAt(), completed.completedAt());
        return snapshot(repository.completeQuestion(completed, updated));
    }

    private InvestigationQuestion validateAnswer(
            InvestigationQuestion question,
            AnswerGenerationResult generated,
            GroundedPrompt prompt,
            int retrievedCount) {
        Instant completedAt = Instant.now(clock);
        if (!generated.sufficient()) {
            return insufficient(question, retrievedCount, completedAt);
        }
        if (generated.answer() == null
                || generated.answer().isBlank()
                || generated.answer().length() > 4_000) {
            throw new AnswerGenerationException("Generated answer text is invalid");
        }
        if (generated.modelId() == null
                || generated.modelId().isBlank()
                || generated.modelId().length() > 120) {
            throw new AnswerGenerationException("Generated answer model identifier is invalid");
        }
        Map<UUID, RetrievedPassage> allowed = new LinkedHashMap<>();
        prompt.passages().forEach(passage -> allowed.put(passage.passageId(), passage));
        var citedIds = new LinkedHashSet<>(generated.citedPassageIds());
        if (citedIds.isEmpty() || citedIds.stream().anyMatch(id -> !allowed.containsKey(id))) {
            throw new AnswerGenerationException("Generated answer contained invalid citations");
        }
        List<CitationSnapshot> citations = new ArrayList<>(citedIds.size());
        for (UUID citedId : citedIds) {
            RetrievedPassage passage = allowed.get(citedId);
            citations.add(new CitationSnapshot(
                    passage.passageId(),
                    passage.documentId(),
                    passage.documentTitle(),
                    passage.pageNumber(),
                    passage.sequence(),
                    truncate(passage.text(), 500),
                    passage.score()));
        }
        return new InvestigationQuestion(
                question.id(),
                question.investigationId(),
                question.question(),
                question.selectedDocumentIds(),
                AnswerStatus.GROUNDED,
                generated.answer().trim(),
                citations,
                generated.modelId().trim(),
                question.promptVersion(),
                retrievedCount,
                null,
                question.askedAt(),
                completedAt);
    }

    private InvestigationQuestion insufficient(
            InvestigationQuestion question, int retrievedCount, Instant completedAt) {
        return new InvestigationQuestion(
                question.id(),
                question.investigationId(),
                question.question(),
                question.selectedDocumentIds(),
                AnswerStatus.INSUFFICIENT_EVIDENCE,
                INSUFFICIENT_MESSAGE,
                List.of(),
                null,
                question.promptVersion(),
                retrievedCount,
                null,
                question.askedAt(),
                completedAt);
    }

    private InvestigationQuestion technicalFailure(
            InvestigationQuestion question, RuntimeException exception, int retrievedCount) {
        String reason = exception instanceof AnswerGenerationException && exception.getMessage() != null
                ? truncate(exception.getMessage(), 500)
                : "Question answering failed";
        return new InvestigationQuestion(
                question.id(),
                question.investigationId(),
                question.question(),
                question.selectedDocumentIds(),
                AnswerStatus.TECHNICAL_FAILURE,
                null,
                List.of(),
                null,
                question.promptVersion(),
                retrievedCount,
                reason,
                question.askedAt(),
                Instant.now(clock));
    }

    private Investigation find(UUID investigationId) {
        return repository
                .findById(investigationId)
                .orElseThrow(() -> new InvestigationNotFoundException(investigationId));
    }

    private InvestigationSnapshot snapshot(Investigation investigation) {
        return new InvestigationSnapshot(
                investigation.id(),
                investigation.incidentId(),
                repository.findQuestions(investigation.id()).stream()
                        .map(InvestigationManagement::snapshot)
                        .toList(),
                investigation.createdAt(),
                investigation.updatedAt());
    }

    private static QuestionAnswerSnapshot snapshot(InvestigationQuestion question) {
        return new QuestionAnswerSnapshot(
                question.id(),
                question.question(),
                question.selectedDocumentIds(),
                question.status(),
                question.answer(),
                question.citations(),
                question.modelId(),
                question.promptVersion(),
                question.retrievedPassageCount(),
                question.failureReason(),
                question.askedAt(),
                question.completedAt());
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
