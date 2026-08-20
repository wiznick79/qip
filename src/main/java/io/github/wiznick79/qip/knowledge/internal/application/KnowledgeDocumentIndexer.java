package io.github.wiznick79.qip.knowledge.internal.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
class KnowledgeDocumentIndexer implements DocumentIndexer {

    private final PassageChunker chunker;
    private final EmbeddingGenerator embeddings;
    private final PassageRepository passages;
    private final Clock clock;
    private final int batchSize;

    KnowledgeDocumentIndexer(
            PassageChunker chunker,
            EmbeddingGenerator embeddings,
            PassageRepository passages,
            Clock clock,
            @Value("${qip.knowledge.embedding-batch-size}") int batchSize) {
        if (batchSize < 1 || batchSize > 256) {
            throw new IllegalArgumentException("embedding batch size must be between 1 and 256");
        }
        this.chunker = chunker;
        this.embeddings = embeddings;
        this.passages = passages;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Override
    public void index(UUID documentId, List<ExtractedPage> pages) {
        List<PassageDraft> drafts = chunker.chunk(pages);
        if (drafts.isEmpty()) {
            throw new DocumentIndexingException("Document produced no searchable passages");
        }

        List<Embedding> vectors = new ArrayList<>(drafts.size());
        for (int start = 0; start < drafts.size(); start += batchSize) {
            int end = Math.min(start + batchSize, drafts.size());
            List<String> texts =
                    drafts.subList(start, end).stream().map(PassageDraft::text).toList();
            List<Embedding> batch = embeddings.embed(texts);
            if (batch.size() != texts.size()) {
                throw new DocumentIndexingException("Embedding model returned an unexpected result count");
            }
            vectors.addAll(batch);
        }
        int dimensions = vectors.getFirst().values().size();
        if (vectors.stream().anyMatch(vector -> vector.values().size() != dimensions)) {
            throw new DocumentIndexingException("Embedding model returned inconsistent dimensions");
        }

        Instant indexedAt = Instant.now(clock);
        List<KnowledgePassage> indexed = new ArrayList<>(drafts.size());
        for (int index = 0; index < drafts.size(); index++) {
            PassageDraft draft = drafts.get(index);
            String hash = sha256(draft.text());
            indexed.add(new KnowledgePassage(
                    passageId(documentId, draft.sequence(), hash),
                    documentId,
                    draft.sequence(),
                    draft.pageNumber(),
                    draft.text(),
                    hash,
                    vectors.get(index),
                    embeddings.modelId(),
                    indexedAt));
        }
        passages.replaceAll(documentId, indexed);
    }

    private static UUID passageId(UUID documentId, int sequence, String textHash) {
        byte[] digest = digest((documentId + ":" + sequence + ":" + textHash).getBytes(StandardCharsets.UTF_8));
        digest[6] = (byte) ((digest[6] & 0x0f) | 0x50);
        digest[8] = (byte) ((digest[8] & 0x3f) | 0x80);
        long most = 0;
        long least = 0;
        for (int index = 0; index < 8; index++) {
            most = (most << 8) | (digest[index] & 0xffL);
            least = (least << 8) | (digest[index + 8] & 0xffL);
        }
        return new UUID(most, least);
    }

    private static String sha256(String value) {
        return HexFormat.of().formatHex(digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] digest(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
