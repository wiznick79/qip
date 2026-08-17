package io.github.wiznick79.qip.assets.internal.infrastructure.persistence;

import io.github.wiznick79.qip.assets.internal.application.AssetPage;
import io.github.wiznick79.qip.assets.internal.application.AssetRepository;
import io.github.wiznick79.qip.assets.internal.domain.Asset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class JpaAssetRepositoryAdapter implements AssetRepository {

    private static final Sort ASSET_ORDER =
            Sort.by("name").ascending().and(Sort.by("id").ascending());

    private final SpringDataAssetRepository repository;

    JpaAssetRepositoryAdapter(SpringDataAssetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Asset save(Asset asset) {
        return repository.save(AssetJpaEntity.fromDomain(asset)).toDomain();
    }

    @Override
    public Optional<Asset> findById(UUID assetId) {
        return repository.findById(assetId).map(AssetJpaEntity::toDomain);
    }

    @Override
    public boolean existsById(UUID assetId) {
        return repository.existsById(assetId);
    }

    @Override
    public AssetPage findAll(int page, int size) {
        var result = repository.findAll(PageRequest.of(page, size, ASSET_ORDER));
        return new AssetPage(
                result.getContent().stream().map(AssetJpaEntity::toDomain).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements());
    }
}
