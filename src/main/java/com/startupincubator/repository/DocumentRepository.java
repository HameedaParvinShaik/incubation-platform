package com.startupincubator.repository;

import com.startupincubator.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByCreatedAtDesc();

    List<Document> findByFolder(String folder);

    List<Document> findByUploadedBy(String uploadedBy);

    List<Document> findByStatus(String status);

    List<Document> findByFolderIsNull();

    void deleteByFilePath(String filePath);
}