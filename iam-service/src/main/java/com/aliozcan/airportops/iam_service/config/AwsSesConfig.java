package com.aliozcan.airportops.iam_service.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@EnableConfigurationProperties({
        AwsSesProperties.class,
        MailProperties.class,
        InvitationEmailProperties.class
})
public class AwsSesConfig {

    @Bean
    public SesV2Client sesV2Client(AwsSesProperties properties) {
        return SesV2Client.builder()
                .region(Region.of(properties.region()))
                .build();
    }
}
