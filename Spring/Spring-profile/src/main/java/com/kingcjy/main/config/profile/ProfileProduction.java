package com.kingcjy.main.config.profile;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Profile(value="production")
@PropertySource({"classpath:production/application.yaml"})
public class ProfileProduction {
}
