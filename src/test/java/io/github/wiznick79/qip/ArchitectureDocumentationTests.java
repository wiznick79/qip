package io.github.wiznick79.qip;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

class ArchitectureDocumentationTests {

    @Test
    void generatesVerifiedModuleDiagramsAndCanvases() throws IOException {
        ApplicationModules modules = ApplicationModules.of(QipApplication.class);
        modules.verify();

        new Documenter(modules).writeDocumentation();

        Path output = Path.of("target", "spring-modulith-docs");
        assertThat(output).isDirectory();
        try (var files = Files.list(output)) {
            assertThat(files.map(path -> path.getFileName().toString()))
                    .anyMatch(name -> name.endsWith(".puml"))
                    .anyMatch(name -> name.endsWith(".adoc"));
        }
    }
}
