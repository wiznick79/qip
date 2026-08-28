package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.assets.api.AssetCatalog;
import io.github.wiznick79.qip.incidents.api.IncidentCaseCatalog;
import io.github.wiznick79.qip.investigations.api.InvestigationStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InvestigationReportService {

    private final InvestigationManagement investigations;
    private final IncidentCaseCatalog cases;
    private final AssetCatalog assets;
    private final InvestigationReportRenderer renderer;
    private final Clock clock;

    public InvestigationReportService(
            InvestigationManagement investigations,
            IncidentCaseCatalog cases,
            AssetCatalog assets,
            InvestigationReportRenderer renderer,
            Clock clock) {
        this.investigations = investigations;
        this.cases = cases;
        this.assets = assets;
        this.renderer = renderer;
        this.clock = clock;
    }

    public InvestigationReport generate(UUID investigationId) {
        var investigation = investigations.get(investigationId);
        if (investigation.status() != InvestigationStatus.CLOSED) {
            throw new InvalidInvestigationStateException("Only closed investigations can be exported");
        }
        var incidentCase = cases.getCase(investigation.incidentId());
        var asset = assets.getAsset(incidentCase.incident().assetId());
        byte[] content =
                renderer.render(new InvestigationReportData(asset, incidentCase, investigation, Instant.now(clock)));
        return new InvestigationReport("qip-investigation-" + investigation.id() + ".pdf", content);
    }
}
