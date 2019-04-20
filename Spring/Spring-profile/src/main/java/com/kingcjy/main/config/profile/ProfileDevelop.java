package com.kingcjy.main.config.profile;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Profile(value="develop")
@PropertySource({"classpath:develop/application.yaml"})
public class ProfileDevelop {
}
