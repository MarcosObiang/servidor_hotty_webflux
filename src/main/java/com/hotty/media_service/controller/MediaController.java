package com.hotty.media_service.controller;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import com.hotty.media_service.service.MediaService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/media")
public class MediaController {

    private MediaService mediaService;

    public MediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<String>> uploadMedia(
            @RequestPart("file") FilePart file,
            @RequestParam("userUID") String userId,
            @RequestParam("index") int index,
            @RequestParam("type") String type) {

        return file.content().hasElements()
                .flatMap(hasElements -> {
                    if (!hasElements) {
                        return Mono.just(ResponseEntity.badRequest().body("File is empty"));
                    }

                    return mediaService.uploadFile(file, userId, index, type)
                            .map(response -> ResponseEntity.ok().body(response))
                            .onErrorResume(e -> {
                                e.printStackTrace();
                                return Mono.just(ResponseEntity.internalServerError()
                                        .body("Failed to upload file: " + e.getMessage()));
                            });
                });
    }

    @PostMapping(value = "/upload-from-bytes")
    public Mono<ResponseEntity<String>> uploadFromBytes(
            @RequestParam("fileName") String fileName,
            @RequestBody Flux<org.springframework.core.io.buffer.DataBuffer> dataBufferFlux,
            @RequestParam("contentType") String contentType) {

        return mediaService.uploadFileFromStream(fileName, dataBufferFlux, contentType)
                .map(response -> ResponseEntity.ok().body(response))
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("Failed to upload file from bytes: " + e.getMessage()));
                });
    }

    @GetMapping(value = "/get-media")
    public Mono<ResponseEntity<byte[]>> getMedia(@RequestParam String fileName) {
        return mediaService.getFile(fileName)
                .map(response -> {
                    System.out.println("File retrieved successfully: " + fileName);
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .body(response);
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(null));
                });
    }

    @DeleteMapping(value = "/delete-media")
    public Mono<ResponseEntity<String>> deleteMedia(@RequestParam String fileName) {
        return mediaService.deleteFile(fileName)
                .map(success -> {
                    if (success) {
                        System.out.println("File deleted successfully: " + fileName);
                        return ResponseEntity.ok("Archivo eliminado exitosamente");
                    } else {
                        return ResponseEntity.badRequest().body("Error al eliminar el archivo");
                    }
                })
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return Mono.just(ResponseEntity.internalServerError()
                            .body("Failed to delete file: " + e.getMessage()));
                });
    }
}
