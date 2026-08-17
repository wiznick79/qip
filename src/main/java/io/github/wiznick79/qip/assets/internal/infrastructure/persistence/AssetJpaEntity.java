package io.github.wiznick79.qip.assets.internal.infrastructure.persistence;

import io.github.wiznick79.qip.assets.api.AssetType;
import io.github.wiznick79.qip.assets.internal.domain.Asset;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assets")
class AssetJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 40)
    private AssetType type;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AssetJpaEntity() {}

    private AssetJpaEntity(Asset asset) {
        id = asset.id();
        name = asset.name();
        type = asset.type();
        externalReference = asset.externalReference();
        createdAt = asset.createdAt();
    }

    static AssetJpaEntity fromDomain(Asset asset) {
        return new AssetJpaEntity(asset);
    }

    Asset toDomain() {
        return new Asset(id, name, type, externalReference, createdAt);
    }
}
