package io.github.wiznick79.qip.assets.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import io.github.wiznick79.qip.assets.api.AssetType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssetTests {

    private static final UUID ASSET_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final Instant CREATED_AT = Instant.parse("2026-08-17T12:00:00Z");

    @Test
    void normalizesHumanEnteredText() {
        Asset asset = new Asset(ASSET_ID, "  Forming Press 04  ", AssetType.MACHINE, "  PRESS-04  ", CREATED_AT);

        assertThat(asset.name()).isEqualTo("Forming Press 04");
        assertThat(asset.externalReference()).isEqualTo("PRESS-04");
    }

    @Test
    void treatsBlankExternalReferenceAsAbsent() {
        Asset asset = new Asset(ASSET_ID, "Forming Press 04", AssetType.MACHINE, "  ", CREATED_AT);

        assertThat(asset.externalReference()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Asset(ASSET_ID, "  ", AssetType.MACHINE, null, CREATED_AT))
                .withMessage("name must not be blank");
    }

    @Test
    void rejectsNameBeyondApiAndDatabaseLimit() {
        String oversizedName = "A".repeat(121);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Asset(ASSET_ID, oversizedName, AssetType.MACHINE, null, CREATED_AT))
                .withMessage("name must not exceed 120 characters");
    }
}
