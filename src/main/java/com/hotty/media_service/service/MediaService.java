package com.hotty.media_service.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Procesa imagen removiendo metadatos EXIF para cumplimiento LGPD
 * - Elimina geolocalización (Art. 6º, VII)  
 * - Elimina identificación de dispositivo
 * - Mantiene solo metadatos técnicos necesarios
 */
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    public Mono<String> uploadFile(FilePart file, String userId, int index, String fileName) {
        return DataBufferUtils.join(file.content())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return Mono.fromCallable(() -> {
                        try (InputStream inputStream = new ByteArrayInputStream(bytes);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

                            BufferedImage image = ImageIO.read(inputStream);
                            if (image == null) {
                                throw new IllegalArgumentException("Archivo no es una imagen válida");
                            }

                            String formatName = detectFormat(bytes, file.headers().getContentType().toString());

                            ImageWriter writer = ImageIO.getImageWritersByFormatName(formatName).next();
                            ImageWriteParam writeParam = writer.getDefaultWriteParam();

                            // Solo JPEG permite compresión explícita
                            if ("jpeg".equalsIgnoreCase(formatName) || "jpg".equalsIgnoreCase(formatName)) {
                                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                                writeParam.setCompressionQuality(0.8f);
                            }

                            try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
                                writer.setOutput(ios);
                                writer.write(null, new IIOImage(image, null, null), writeParam);
                            }
                            writer.dispose();

                            byte[] outputBytes = baos.toByteArray();

                            try (InputStream finalInputStream = new ByteArrayInputStream(outputBytes)) {
                                minioClient.putObject(
                                        PutObjectArgs.builder()
                                                .bucket(bucketName)
                                                .object(fileName)
                                                .stream(finalInputStream, outputBytes.length, -1)
                                                .contentType(file.headers().getContentType().toString())
                                                .build());
                            }

                            log.info("LGPD: Metadatos EXIF eliminados para usuario: {} archivo: {}", userId, fileName);

                            return fileName;
                        }
                    });
                });
    }

    // Método auxiliar para detectar formato de imagen
    private String detectFormat(byte[] imageBytes, String contentType) {
        try (InputStream is = new ByteArrayInputStream(imageBytes)) {
            BufferedImage img = ImageIO.read(is);
            if (img == null)
                return "jpg"; // fallback

            String format = contentType.split("/")[1]; // ej: image/png -> png
            if ("jpeg".equalsIgnoreCase(format) || "jpg".equalsIgnoreCase(format)
                    || "png".equalsIgnoreCase(format)
                    || "bmp".equalsIgnoreCase(format)
                    || "gif".equalsIgnoreCase(format)) {
                return format;
            }
            return "jpg"; // fallback seguro
        } catch (Exception e) {
            return "jpg";
        }
    }

    public Mono<String> uploadFileFromBytes(String fileName, byte[] base64FileData, String contentType) {
        return Mono.fromCallable(() -> {
            try {

                try (InputStream inputStream = new ByteArrayInputStream(base64FileData)) {
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(fileName)
                                    .stream(inputStream, base64FileData.length, -1)
                                    .contentType(contentType)
                                    .build());
                    return fileName;
                }
            } catch (Exception e) {
                throw new RuntimeException("Error uploading file from bytes: " + fileName, e);
            }
        });
    }

    public Mono<String> uploadFileFromStream(String fileName, Flux<DataBuffer> dataBufferFlux, String contentType) {
        return DataBufferUtils.join(dataBufferFlux)
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    return Mono.fromCallable(() -> {
                        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
                            minioClient.putObject(
                                    PutObjectArgs.builder()
                                            .bucket(bucketName)
                                            .object(fileName)
                                            .stream(inputStream, bytes.length, -1)
                                            .contentType(contentType)
                                            .build());
                            return fileName;
                        } catch (Exception e) {
                            throw new RuntimeException("Error uploading file from stream: " + fileName, e);
                        }
                    });
                });
    }

    // Alternativa con streaming real para archivos muy grandes
    public Mono<String> uploadFileFromStreamAdvanced(String fileName, Flux<DataBuffer> dataBufferFlux,
            String contentType) {
        return Mono.fromCallable(() -> {
            try {
                // Crear un PipedInputStream/PipedOutputStream para streaming real
                java.io.PipedInputStream pipedInputStream = new java.io.PipedInputStream();
                java.io.PipedOutputStream pipedOutputStream = new java.io.PipedOutputStream(pipedInputStream);

                // Procesar el flux en un hilo separado
                dataBufferFlux
                        .doOnNext(dataBuffer -> {
                            try {
                                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                dataBuffer.read(bytes);
                                pipedOutputStream.write(bytes);
                                DataBufferUtils.release(dataBuffer);
                            } catch (IOException e) {
                                throw new RuntimeException("Error writing to stream", e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                pipedOutputStream.close();
                            } catch (IOException e) {
                                throw new RuntimeException("Error closing stream", e);
                            }
                        })
                        .subscribe();

                // Subir usando el InputStream
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(pipedInputStream, -1, 10485760) // 10MB part size
                                .contentType(contentType)
                                .build());

                return fileName;
            } catch (Exception e) {
                throw new RuntimeException("Error uploading file from advanced stream: " + fileName, e);
            }
        });
    }

    public Mono<byte[]> getFile(String filename) {
        return Mono.fromCallable(() -> {
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucketName).object(filename).build())) {

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    byteArrayOutputStream.write(buffer, 0, length);
                }
                return byteArrayOutputStream.toByteArray();
            } catch (MinioException | IOException e) {
                throw new RuntimeException("Error al obtener el archivo desde MinIO", e);
            }
        })
                .onErrorMap(RuntimeException.class,
                        e -> new RuntimeException("Error al obtener el archivo desde MinIO: " + filename, e));
    }

    public Mono<Boolean> deleteFile(String filename) {
        return Mono.fromCallable(() -> {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(bucketName)
                                .object(filename)
                                .build());
                return true;
            } catch (MinioException | IOException e) {
                throw new RuntimeException("Error al eliminar el archivo desde MinIO", e);
            }
        })
                .onErrorMap(RuntimeException.class,
                        e -> new RuntimeException("Error al eliminar el archivo desde MinIO: " + filename, e))
                .onErrorReturn(false);
    }

    private String generateUid() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder uid = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = (int) (Math.random() * chars.length());
            uid.append(chars.charAt(idx));
        }
        return uid.toString();
    }
}
