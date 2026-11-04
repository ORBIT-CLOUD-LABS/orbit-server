package com.orbit.server.global.external.cdn;

import static org.assertj.core.api.Assertions.assertThat;

import com.orbit.server.domain.edge.entity.Edges;
import com.orbit.server.domain.edge.service.EdgeCatalog;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;

/**
 * 배포 환경처럼 Edge 목록을 application.yml 밖의 YAML 파일로 넣을 수 있는지 확인한다.
 *
 * <p>spring.config.import 는 SpringApplication 이 환경을 준비할 때만 처리되므로 ApplicationContextRunner 가 아니라 실제
 * SpringApplication 을 띄운다. 배포 환경에서는 같은 값을 환경 변수 {@code SPRING_CONFIG_IMPORT} 로 준다.
 */
class ExternalEdgeConfigTest {

    @Test
    @DisplayName("spring.config.import 로 지정한 파일의 Edge 목록을 읽는다")
    void loadsEdgesFromImportedFile() throws IOException {
        try (ConfigurableApplicationContext context = runWithImportedFile()) {
            Edges edges = context.getBean(EdgeCatalog.class).edges();

            assertThat(edges.all()).hasSize(2);
            assertThat(edges.defaultEdge().id()).isEqualTo("edge-daejeon");
        }
    }

    @Test
    @DisplayName("가져온 파일의 값이 application.yml 의 기본값보다 우선한다")
    void importedFileOverridesDefaults() throws IOException {
        try (ConfigurableApplicationContext context = runWithImportedFile()) {
            assertThat(context.getBean(CdnProperties.class).fallbackCount()).isEqualTo(1);
        }
    }

    // ConfigMap 을 파일로 마운트하는 배포 방식과 같게 classpath: 가 아니라 file: 로 가져온다
    private static ConfigurableApplicationContext runWithImportedFile() throws IOException {
        Path file = new ClassPathResource("cdn/application-cdn.yml").getFile().toPath();

        return new SpringApplicationBuilder(CdnConfig.class, ConfiguredEdgeCatalog.class)
                .web(WebApplicationType.NONE)
                .run("--spring.config.import=file:" + file, "--orbit.cdn.base-url=http://cdn.test");
    }
}
