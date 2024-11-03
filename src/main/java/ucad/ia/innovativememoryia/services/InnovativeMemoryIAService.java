package ucad.ia.innovativememoryia.services;

import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InnovativeMemoryIAService {
    Boolean uploadFile(List<MultipartFile> files);
    String chat(String question);
     String audio(MultipartFile audioFile);
}
