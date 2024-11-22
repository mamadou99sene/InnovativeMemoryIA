package ucad.ia.innovativememoryia.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import ucad.ia.innovativememoryia.services.ImageAIService;

import java.io.IOException;
@RestController
public class ImageController {
    private ImageAIService imageAIService;

    public ImageController(ImageAIService imageAIService) {
        this.imageAIService = imageAIService;
    }

    @GetMapping(path="/generateImage", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] generateImageDALLE() throws IOException {
        return this.imageAIService.generateImageDALLE();
    }
    @GetMapping(value = "/ocr", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Flux<String>> ocr(@RequestParam("image") MultipartFile image) throws IOException {
        Flux<String> description = this.imageAIService.ocrImage(image);
        return ResponseEntity.status(HttpStatus.OK).body(description);
    }


}
