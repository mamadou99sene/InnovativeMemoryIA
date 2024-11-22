package ucad.ia.innovativememoryia.services;

import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;

public interface ImageAIService {
    public byte[] generateImageDALLE() throws IOException;
    public Flux<String> ocrImage(MultipartFile file) throws IOException;
}
