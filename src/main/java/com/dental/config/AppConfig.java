package com.dental.config;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;

@Configuration
public class AppConfig {
  @Bean
  public Clock clock(@Value("${app.zone}") String zone) {
    return Clock.system(ZoneId.of(zone));
  }
}
