package ucad.ia.innovativememoryia;

import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ucad.ia.innovativememoryia.services.InnovativeMemoryIAService;

import java.io.File;

@SpringBootApplication
public class InnovativeMemoryIaApplication {

    public static void main(String[] args) {
        SpringApplication.run(InnovativeMemoryIaApplication.class, args);
    }
    //@Bean
    CommandLineRunner commandLineRunner(InnovativeMemoryIAService innovativeMemoryIAService, @Value("${spring.ai.openai.api-key}") String apiKey)
    {
        return args -> {
            OpenAiService openAiService=new OpenAiService(apiKey);
            CreateTranscriptionRequest request = new CreateTranscriptionRequest();
            request.setModel("whisper-1");
            File file=new File("C:\\Users\\FIS-TS\\Downloads\\vocal.wav");
            String text = openAiService.createTranscription(request, file).getText();
            System.out.println(text);
        };
    }

}
