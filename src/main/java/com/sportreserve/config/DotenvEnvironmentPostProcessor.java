package com.sportreserve.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        FileSystemResource resource = findEnvFile();
        if (resource == null) {
            System.out.println("[Dotenv] No .env file found, skipping");
            return;
        }

        try {
            Properties props = new Properties();
            try (var is = resource.getInputStream()) {
                props.load(is);
            }
            System.out.println("[Dotenv] Loaded " + props.size() + " properties from "
                + resource.getFile().getAbsolutePath());
            environment.getPropertySources().addFirst(
                new PropertiesPropertySource("dotenv", props)
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load .env file", e);
        }
    }

    private FileSystemResource findEnvFile() {
        Path dir = Paths.get(".").toAbsolutePath().normalize();
        for (int i = 0; i < 5; i++) {
            Path candidate = dir.resolve(".env");
            if (Files.exists(candidate)) {
                return new FileSystemResource(candidate.toFile());
            }
            dir = dir.getParent();
            if (dir == null) break;
        }
        return null;
    }
}
