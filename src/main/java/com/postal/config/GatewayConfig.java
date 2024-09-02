package com.postal.config;

import lombok.extern.slf4j.Slf4j;
import org.hyperledger.fabric.gateway.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

@Slf4j
@Configuration
public class GatewayConfig {

    @Autowired
    private FabricConfig fabricConfig;

    private Gateway connectGateway(FabricConfig.OrgConfig config) throws IOException, InvalidKeyException, CertificateException {
        // 使用配置初始化一个网关wallet账户用于连接网络
        Wallet wallet = Wallets.newFileSystemWallet(Paths.get(config.getWalletDirectory()));
        X509Certificate certificate = readX509Certificate(Paths.get(config.getCertificatePath()));
        PrivateKey privateKey = getPrivateKey(Paths.get(config.getPrivateKeyPath()));
        wallet.put(config.getUsername(), Identities.newX509Identity(config.getMspid(), certificate, privateKey));

        // 根据connection.json 获取Fabric网络连接对象
        Gateway.Builder builder = Gateway.createBuilder()
                .identity(wallet, config.getUsername())
                .networkConfig(Paths.get(config.getNetworkConfigPath()));

        // 连接网关
        return builder.connect();
    }

    private Network getNetwork(FabricConfig.OrgConfig config) throws IOException, InvalidKeyException, CertificateException {
        Gateway gateway = connectGateway(config);
        return gateway.getNetwork(config.getChannelName());
    }

    private Contract getContract(Network network, String contractName) {
        return network.getContract(contractName);
    }

    @Bean(name = "org1MyccContract")
    public Contract org1MyccContract() throws IOException, InvalidKeyException, CertificateException {
        FabricConfig.OrgConfig config = fabricConfig.getOrgs().get("org1");
        Network network = getNetwork(config);
        return getContract(network, config.getContracts().get("mycc").getContractName());
    }

    @Bean(name = "org1Mycc2Contract")
    public Contract org1Mycc2Contract() throws IOException, InvalidKeyException, CertificateException {
        FabricConfig.OrgConfig config = fabricConfig.getOrgs().get("org1");
        Network network = getNetwork(config);
        return getContract(network, config.getContracts().get("mycc2").getContractName());
    }

    @Bean(name = "org1Mycc3Contract")
    public Contract org1Mycc3Contract() throws IOException, InvalidKeyException, CertificateException {
        FabricConfig.OrgConfig config = fabricConfig.getOrgs().get("org1");
        Network network = getNetwork(config);
        return getContract(network, config.getContracts().get("mycc3").getContractName());
    }

    @Bean(name = "org2MyccContract")
    public Contract org2MyccContract() throws IOException, InvalidKeyException, CertificateException {
        FabricConfig.OrgConfig config = fabricConfig.getOrgs().get("org2");
        Network network = getNetwork(config);
        return getContract(network, config.getContracts().get("mycc").getContractName());
    }

    @Bean(name = "org2Mycc2Contract")
    public Contract org2Mycc2Contract() throws IOException, InvalidKeyException, CertificateException {
        FabricConfig.OrgConfig config = fabricConfig.getOrgs().get("org2");
        Network network = getNetwork(config);
        return getContract(network, config.getContracts().get("mycc2").getContractName());
    }

    @Bean(name = "org2Mycc3Contract")
    public Contract org2Mycc3Contract() throws IOException, InvalidKeyException, CertificateException {
        FabricConfig.OrgConfig config = fabricConfig.getOrgs().get("org2");
        Network network = getNetwork(config);
        return getContract(network, config.getContracts().get("mycc3").getContractName());
    }

    private static X509Certificate readX509Certificate(final Path certificatePath) throws IOException, CertificateException {
        try (Reader certificateReader = Files.newBufferedReader(certificatePath, StandardCharsets.UTF_8)) {
            return Identities.readX509Certificate(certificateReader);
        }
    }

    private static PrivateKey getPrivateKey(final Path privateKeyPath) throws IOException, InvalidKeyException {
        try (Reader privateKeyReader = Files.newBufferedReader(privateKeyPath, StandardCharsets.UTF_8)) {
            return Identities.readPrivateKey(privateKeyReader);
        }
    }
}
