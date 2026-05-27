package com.schedule.aisystem.config;

import com.schedule.aisystem.repository.UserAccountRepository;
import com.schedule.aisystem.service.UniversitySeedService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StartupDataInitializer {

    @Bean
    ApplicationRunner seedDefaultUniversityData(
            UserAccountRepository userAccountRepository,
            UniversitySeedService universitySampleDataService
    ) {
        return args -> {
            if (userAccountRepository.count() == 0) {
                universitySampleDataService.resetWithSampleData();
            }
        };
    }
}
