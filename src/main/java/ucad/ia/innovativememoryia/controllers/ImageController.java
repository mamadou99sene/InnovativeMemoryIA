package ucad.ia.innovativememoryia.controllers;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
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

}
