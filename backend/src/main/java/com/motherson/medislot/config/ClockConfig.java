package com.motherson.medislot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** One clock for the whole app so "today" / "now" are always in the hospitals' time zone. */
@Configuration
public class ClockConfig {
    @Bean
    public Clock clock(AppProperties props) {
        return Clock.system(ZoneId.of(props.timezone()));
    }
}
