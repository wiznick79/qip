package io.github.wiznick79.qip.assets.internal.infrastructure.web;

import io.github.wiznick79.qip.assets.api.AssetSnapshot;
import io.github.wiznick79.qip.assets.internal.application.AssetManagement;
import io.github.wiznick79.qip.assets.internal.application.CreateAssetCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/assets")
class AssetController {

    private final AssetManagement assets;

    AssetController(AssetManagement assets) {
        this.assets = assets;
    }

    @PostMapping
    ResponseEntity<AssetResponse> create(@Valid @RequestBody CreateAssetRequest request) {
        AssetSnapshot created =
                assets.createAsset(new CreateAssetCommand(request.name(), request.type(), request.externalReference()));
        return ResponseEntity.created(URI.create("/api/assets/" + created.id())).body(AssetResponse.from(created));
    }

    @GetMapping("/{assetId}")
    AssetResponse get(@PathVariable UUID assetId) {
        return AssetResponse.from(assets.getAsset(assetId));
    }

    @GetMapping
    AssetPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        var result = assets.listAssets(page, size);
        return new AssetPageResponse(
                result.items().stream().map(AssetResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements());
    }
}
