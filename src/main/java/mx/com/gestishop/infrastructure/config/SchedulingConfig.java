package mx.com.gestishop.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa las tareas programadas (@Scheduled). Sin esta anotación, el proceso nocturno de
 * vencimientos (VencimientoScheduler) nunca se ejecutaría.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}

