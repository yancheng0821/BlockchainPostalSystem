package com.postal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "fabric")
public class FabricConfig {
    private Map<String, OrgConfig> orgs;
    private String algorithm;

    @Data
    public static class OrgConfig {
        private String walletDirectory;
        private String networkConfigPath;
        private String certificatePath;
        private String privateKeyPath;
        private String mspid;
        private String username;
        private String channelName;
        private Map<String, ContractConfig> contracts;

        @Data
        public static class ContractConfig {
            private String contractName;
        }
    }
}
