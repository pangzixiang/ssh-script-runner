package io.github.pangzixiang.ssh.script.runner.service;

import io.github.pangzixiang.ssh.script.runner.config.AppConfiguration;
import io.github.pangzixiang.ssh.script.runner.exception.AppInitializeException;
import io.github.pangzixiang.ssh.script.runner.exception.SshKeyException;
import io.github.pangzixiang.ssh.script.runner.pojo.KeyFile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.sshd.common.config.keys.loader.KeyPairResourceLoader;
import org.apache.sshd.common.util.security.SecurityUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class SshKeyService {
    private static SshKeyService instance;
    @Getter
    private final File keyDir;
    private final KeyPairResourceLoader keyPairResourceLoader;
    private final KeyPairGenerator keyPairGenerator;
    private final Base64.Encoder base64PubEncoder = Base64.getEncoder();
    private final Base64.Encoder base64PriEncoder = Base64.getMimeEncoder();
    @Getter
    private final Set<KeyPair> keyPairs = new HashSet<>();
    private static final String PRIVATE_KEY_TEMPLATE = "-----BEGIN PRIVATE KEY-----\n%s\n-----END PRIVATE KEY-----";
    private static final String PUBLIC_KEY_TEMPLATE = "ssh-rsa %s sshsr@%s";
    private static final byte[] SSH_RSA_BYTES = "ssh-rsa".getBytes();
    private SshKeyService() {
        AppConfiguration appConfiguration = AppConfiguration.getInstance();
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
        } catch (Exception e) {
            log.error("failed to initialize key generator", e);
            throw new AppInitializeException("failed to initialize RSA key generator", e);
        }
        this.keyDir = new File(appConfiguration.getString("app.dir"), "/key");
        try {
            FileUtils.forceMkdir(this.keyDir);
        } catch (Exception e) {
            log.error("failed to create key dir {}", keyDir.getPath(), e);
            throw new AppInitializeException("failed to create key dir " + keyDir.getPath(), e);
        }
        this.keyPairResourceLoader = SecurityUtils.getKeyPairResourceParser();
        this.loadKeyPairsFromDisk();
    }

    public static synchronized SshKeyService getInstance() {
        if (instance == null) {
            instance = new SshKeyService();
        }
        return instance;
    }

    public void generateKeyPair(String name) {
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        RSAPublicKey pub = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey pri = (RSAPrivateKey) keyPair.getPrivate();
        try {
            ByteArrayOutputStream byteOs = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(byteOs);
            dos.writeInt(SSH_RSA_BYTES.length);
            dos.write(SSH_RSA_BYTES);
            dos.writeInt(pub.getPublicExponent().toByteArray().length);
            dos.write(pub.getPublicExponent().toByteArray());
            dos.writeInt(pub.getModulus().toByteArray().length);
            dos.write(pub.getModulus().toByteArray());
            String publicKeyEncoded = new String(
                    base64PubEncoder.encode(byteOs.toByteArray()));
            FileUtils.writeStringToFile(new File(this.keyDir, name + ".pub"),
                    PUBLIC_KEY_TEMPLATE.formatted(publicKeyEncoded, name),
                    Charset.defaultCharset(),
                    false);
            FileUtils.writeStringToFile(
                    new File(this.keyDir, name + ".pri"),
                    PRIVATE_KEY_TEMPLATE.formatted(base64PriEncoder.encodeToString(pri.getEncoded())),
                    Charset.defaultCharset(),
                    false);
            log.info("Succeeded to generate key pair {}", name);
            loadKeyPairsFromDisk();
        } catch (Exception e) {
            throw new SshKeyException("failed to generate key", e);
        }
    }

    public List<KeyFile> getAllKeyFiles() {
        List<KeyFile> keyFiles = new ArrayList<>();
        for (File keyFile : FileUtils.listFiles(this.keyDir, null, true)) {
            if (keyFile.isFile()) {
                keyFiles.add(KeyFile.builder()
                        .name(keyFile.getName())
                        .lastModified(LocalDateTime.ofInstant(Instant.ofEpochMilli(keyFile.lastModified()), ZoneId.systemDefault()))
                        .build());
            }
        }
        return keyFiles;
    }

    public void deleteKeyFile(String name) {
        File file = new File(this.keyDir, name);
        if (file.exists()) {
            try {
                FileUtils.forceDelete(file);
                loadKeyPairsFromDisk();
            } catch (IOException e) {
                throw new SshKeyException("failed to delete key file " + name, e);
            }
        }
    }

    public String viewKeyFile(String name) {
        File file = new File(this.keyDir, name);
        if (file.exists()) {
            try {
                return FileUtils.readFileToString(file, Charset.defaultCharset());
            } catch (IOException e) {
                throw new SshKeyException("failed to read key file " + name, e);
            }
        }
        throw new SshKeyException("failed to find key file " + name);
    }

    public void loadKeyPairsFromDisk() {
        this.keyPairs.clear();
        Collection<File> keyFiles = FileUtils.listFiles(this.keyDir, null, true);
        for (File keyFile : keyFiles) {
            try {
                Collection<KeyPair> kp = loadKeyPairFromDisk(keyFile.toPath());
                if (kp != null && !kp.isEmpty()) {
                    this.keyPairs.addAll(kp);
                    log.info("loaded keyPairs from disk {}, current size: {}", keyFile.getPath(), keyPairs.size());
                }
            } catch (Exception e) {
                log.error("failed to load key file {}", keyFile.getPath(), e);
            }
        }
    }

    private Collection<KeyPair> loadKeyPairFromDisk(Path keyFilePath) {
        Collection<KeyPair> kp;
        try {
            kp = keyPairResourceLoader.loadKeyPairs(null, keyFilePath, null);
            return kp;
        } catch (Exception e) {
            throw new SshKeyException(e);
        }
    }
}
