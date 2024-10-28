package ucad.ia.innovativememoryia.services;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InnovativeMemoryIAService {
    boolean uploadFile(List<Resource> files);
    String chat(String question);
}
