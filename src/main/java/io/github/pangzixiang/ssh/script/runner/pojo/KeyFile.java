package io.github.pangzixiang.ssh.script.runner.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class KeyFile {
    private String name;
    private LocalDateTime lastModified;
}
