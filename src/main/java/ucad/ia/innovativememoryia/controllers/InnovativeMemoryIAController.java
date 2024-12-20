package ucad.ia.innovativememoryia.controllers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ucad.ia.innovativememoryia.services.InnovativeMemoryIAService;

import java.util.List;

@RestController
@CrossOrigin("*")
@RequestMapping("/api/innovativeMemory")
public class InnovativeMemoryIAController {
    private ChatClient chatClient;
    private InnovativeMemoryIAService innovativeMemoryIAService;

    public InnovativeMemoryIAController(ChatClient.Builder builder, InnovativeMemoryIAService innovativeMemoryIAService) {
        this.chatClient = builder.build();
        this.innovativeMemoryIAService = innovativeMemoryIAService;
    }
    @GetMapping("/chat1")
    public ResponseEntity<String> chat1(@RequestParam String query) {
        try {
            String response = this.chatClient.prompt().user(query).call().content();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Log the error
            System.err.println("Error during chat interaction: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process chat request.");
        }
    }
    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> uploadFile(@RequestParam("files") List<MultipartFile> files)
    {
        System.out.println("Nombre de fichiers "+files.size());
        String response = this.innovativeMemoryIAService.uploadFile(files);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
    @GetMapping(value = "/chat", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> chat(String question)
    {
        String response = this.innovativeMemoryIAService.chat(question);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> transcribeAudio(@RequestPart("file") MultipartFile file) {
        try {
            String result = innovativeMemoryIAService.audio(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Erreur : " + e.getMessage());
        }
    }
}
