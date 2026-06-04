package com.sportreserve.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.util.Properties;

public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        FileSystemResource resource = new FileSystemResource(".env");
        if (!resource.exists()) {
            resource = new FileSystemResource("../.env");
        }
        if (resource.exists()) {
            Properties props = new Properties();
            try (var is = resource.getInputStream()) {
                props.load(is);
                environment.getPropertySources().addFirst(
                    new PropertiesPropertySource("dotenv", props)
                );
            } catch (IOException e) {
                throw new RuntimeException("Failed to load .env file", e);
            }
        }
    }
}
