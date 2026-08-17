package io.github.wiznick79.qip;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class QipApplicationTests {

    private final ApplicationModules modules = ApplicationModules.of(QipApplication.class);

    @Test
    void verifiesModuleBoundaries() {
        modules.verify();
    }

    @Test
    void discoversThePlannedBusinessModules() {
        assertThat(modules.getModuleByName("assets")).isPresent();
        assertThat(modules.getModuleByName("incidents")).isPresent();
        assertThat(modules.getModuleByName("knowledge")).isPresent();
        assertThat(modules.getModuleByName("investigations")).isPresent();
    }
}
