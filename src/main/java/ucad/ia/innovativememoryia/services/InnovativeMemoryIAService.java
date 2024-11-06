package ucad.ia.innovativememoryia.services;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface InnovativeMemoryIAService {
    String uploadFile(List<MultipartFile> files);
    String chat(String question);
     String audio(MultipartFile audioFile) throws Exception;
}
