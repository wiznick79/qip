package io.github.wiznick79.qip.investigations.internal.application;

import io.github.wiznick79.qip.assets.api.AssetSnapshot;
import io.github.wiznick79.qip.incidents.api.IncidentCaseSnapshot;
import io.github.wiznick79.qip.investigations.api.InvestigationSnapshot;
import java.time.Instant;

public record InvestigationReportData(
        AssetSnapshot asset,
        IncidentCaseSnapshot incidentCase,
        InvestigationSnapshot investigation,
        Instant generatedAt) {}
