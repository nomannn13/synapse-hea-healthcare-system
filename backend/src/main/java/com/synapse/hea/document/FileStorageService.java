package com.synapse.hea.document;

import java.io.IOException;
import java.nio.file.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {
  private final Path root;

  public FileStorageService(@Value("${app.storage.root:./data/uploads}") String root) {
    this.root = Paths.get(root).toAbsolutePath().normalize();
    try {
      Files.createDirectories(this.root);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not initialize document storage", ex);
    }
  }

  public void store(String storedName, MultipartFile file) {
    Path target = root.resolve(storedName).normalize();
    if (!target.getParent().equals(root)) throw new IllegalArgumentException("Invalid storage path");
    try {
      Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException ex) {
      throw new IllegalStateException("Could not store document", ex);
    }
  }

  public Resource load(String storedName) {
    try {
      Path target = root.resolve(storedName).normalize();
      if (!target.getParent().equals(root)) throw new IllegalArgumentException("Invalid storage path");
      Resource resource = new UrlResource(target.toUri());
      if (!resource.exists() || !resource.isReadable()) throw new IllegalStateException("Stored file is unavailable");
      return resource;
    } catch (java.net.MalformedURLException ex) {
      throw new IllegalStateException("Stored file is unavailable", ex);
    }
  }

  public void delete(String storedName) {
    try {
      Files.deleteIfExists(root.resolve(storedName).normalize());
    } catch (IOException ex) {
      throw new IllegalStateException("Could not delete stored document", ex);
    }
  }
}
