package com.wyp.agent.controller;

import com.wyp.agent.constant.FileConstant;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/files")
public class FileController {

    @GetMapping("/pdf/{fileName}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String fileName) {
        Path pdfDir = Path.of(FileConstant.FILE_SAVE_DIR, "pdf").toAbsolutePath().normalize();
        Path pdfPath = pdfDir.resolve(fileName).normalize();
        if (!pdfPath.startsWith(pdfDir)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid file path");
        }
        try {
            Resource resource = new UrlResource(pdfPath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResponseStatusException(NOT_FOUND, "PDF not found");
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(resource.getFilename(), StandardCharsets.UTF_8)
                            .build()
                            .toString())
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new ResponseStatusException(NOT_FOUND, "PDF not found", e);
        }
    }
}