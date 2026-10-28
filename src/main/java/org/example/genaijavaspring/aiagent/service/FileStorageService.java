package org.example.genaijavaspring.aiagent.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

public interface FileStorageService {

    String save(MultipartFile file) throws IOException;

    Path resolve(String fileName);
}
