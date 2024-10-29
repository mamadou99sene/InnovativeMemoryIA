package ucad.ia.innovativememoryia.controllers;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ucad.ia.innovativememoryia.services.InnovativeMemoryIAService;

import java.util.List;

@RestController
@RequestMapping("/api/innovativeMemory")
public class InnovativeMemoryIAController {
    private InnovativeMemoryIAService innovativeMemoryIAService;

    public InnovativeMemoryIAController(InnovativeMemoryIAService innovativeMemoryIAService) {
        this.innovativeMemoryIAService = innovativeMemoryIAService;
    }
    @PostMapping("/upload")
    public ResponseEntity<Boolean> uploadFile(@RequestParam("files") List<MultipartFile> files)
    {
        System.out.println("Nombre de fichiers "+files.size());
        boolean uploaded = this.innovativeMemoryIAService.uploadFile(files);
        return ResponseEntity.status(HttpStatus.OK).body(uploaded);
    }
    @GetMapping("/chat")
    public ResponseEntity<String> chat(String question)
    {
        String response = this.innovativeMemoryIAService.chat(question);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
