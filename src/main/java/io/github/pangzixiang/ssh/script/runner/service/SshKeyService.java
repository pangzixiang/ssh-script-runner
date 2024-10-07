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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyPair;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
    @Getter
    private final Set<KeyPair> keyPairs = new HashSet<>();
    private SshKeyService() {
        AppConfiguration appConfiguration = AppConfiguration.getInstance();
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
    public List<KeyFile> getAllKeyFiles() {
        List<KeyFile> keyFiles = new ArrayList<>();
        for (File keyFile: FileUtils.listFiles(this.keyDir, null, true)) {
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
