package io.github.wiznick79.qip.assets.internal.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAssetRepository extends JpaRepository<AssetJpaEntity, UUID> {}
