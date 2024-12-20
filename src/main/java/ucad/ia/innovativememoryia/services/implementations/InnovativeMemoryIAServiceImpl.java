package ucad.ia.innovativememoryia.services.implementations;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.service.OpenAiService;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ucad.ia.innovativememoryia.services.InnovativeMemoryIAService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InnovativeMemoryIAServiceImpl implements InnovativeMemoryIAService {
    private VectorStore vectorStore;
    private JdbcTemplate jdbcTemplate;
    @Value("${spring.ai.openai.api-key}")
    private String OPENAI_KEY;
    private final OpenAiService openAiService;


    public InnovativeMemoryIAServiceImpl(VectorStore vectorStore, JdbcTemplate jdbcTemplate, @Value("${spring.ai.openai.api-key}") String openAiKey) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.openAiService = new OpenAiService(openAiKey);
    }

    @Override
    public String uploadFile(List<MultipartFile> files) {
            initAnkanePgVectore(jdbcTemplate);
        try {
            PdfDocumentReaderConfig config=PdfDocumentReaderConfig.defaultConfig();
            List<Document> allDocuments = new ArrayList<>();
            for (MultipartFile file : files) {
                PagePdfDocumentReader pagePdfDocumentReader = new PagePdfDocumentReader(file.getResource(), config);
                List<Document> documents = pagePdfDocumentReader.get();
                allDocuments.addAll(documents);
               /* for (Document doc : documents) {
                    System.out.println(doc.getContent());
                }*/

            }
            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> chunks = splitter.split(allDocuments);
            vectorStore.accept(chunks);
            String question="generer moi le resumé du document complet en faisant apparaitre les points les plus critiques";
            String response = chat(question);
            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur de traiement de la requete");
        }
    }

    @Override
    public String chat(String question) {
        List<Document> documents = vectorStore.similaritySearch(question);
        String systemMessageTemplate="""
                You are a virtual assistant to make conversation summaries
                in the number of lines desired by the requester or 5 to 10 by default. 
                For the language you will provide, the text according to the language 
                requested by the user or by default the language of the question. 
                If the question is not about the context, answer with I don't know
                CONTEXT: {CONTEXT}
                """;
        Message systemeMessage=new SystemPromptTemplate(systemMessageTemplate).createMessage(Map.of("CONTEXT",documents));
        UserMessage userMessage=new UserMessage(question);
        Prompt prompt=new Prompt(List.of(systemeMessage, userMessage));
        OpenAiApi openAiApi=new OpenAiApi(OPENAI_KEY);
        OpenAiChatModel model=new OpenAiChatModel(openAiApi, OpenAiChatOptions.
                builder().
                withModel(OpenAiApi.ChatModel.GPT_4_O).
                withTemperature(0.5)
                .withStreamUsage(true).
                build());
        ChatResponse chatResponse = model.call(prompt);
        return chatResponse.getResult().getOutput().getContent();
    }

    @Override
    public String audio(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            return "Le fichier audio est vide.";
        }
        String originalFilename = audioFile.getOriginalFilename();
        if (originalFilename == null) {
            return "Nom de fichier invalide.";
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        List<String> supportedFormats = Arrays.asList("flac", "m4a", "mp3", "mp4", "mpeg", "mpga", "oga", "ogg", "wav", "webm");

        if (!supportedFormats.contains(extension)) {
            return "Format de fichier non supporté. Formats acceptés : " + String.join(", ", supportedFormats);
        }
        File convertedFile = null;
        try {
            convertedFile = File.createTempFile("audio", "." + extension);
            try (FileOutputStream fos = new FileOutputStream(convertedFile)) {
                fos.write(audioFile.getBytes());
            }
            CreateTranscriptionRequest request = new CreateTranscriptionRequest();
            request.setModel("whisper-1");
            String response = openAiService.createTranscription(request, convertedFile).getText();
            initAnkanePgVectore(jdbcTemplate);
            List<Document> documents = simpleConvertToDocuments(response);
            TokenTextSplitter textSplitter=new TokenTextSplitter();
            List<Document> chunks = textSplitter.split(documents);
            vectorStore.accept(chunks);
            String question="generer moi le resumé du document complet en faisant apparaitre les points les plus critiques";
            String summurized = chat(question);
            return summurized;

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la transcription de l'audio.";
        } finally {
            // Supprimer le fichier temporaire après utilisation
            if (convertedFile != null && convertedFile.exists()) {
                convertedFile.delete();
            }
        }
    }
    private File convertMultipartFileToFile(MultipartFile file) throws IOException {
        File convFile = File.createTempFile("audio", ".tmp");
        try (FileOutputStream fos = new FileOutputStream(convFile)) {
            fos.write(file.getBytes());
        }
        return convFile;
    }

    private List<Document> simpleConvertToDocuments(String text) {
        return Arrays.stream(text.split("\n"))
                .filter(line -> !line.trim().isEmpty())
                .map(line -> new Document(line, new HashMap<>()))
                .collect(Collectors.toList());
    }

    private  void initAnkanePgVectore(JdbcTemplate jdbcTemplate) {
        boolean extensionExists = jdbcTemplate.query(
                "SELECT 1 FROM pg_extension WHERE extname = 'vector'",
                (rs, rowNum) -> rs.getInt(1)
        ).size() > 0;

        if (!extensionExists) {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
        }

        jdbcTemplate.execute("""
        CREATE TABLE IF NOT EXISTS vector_store (
            id UUID PRIMARY KEY,
            content TEXT,
            embedding VECTOR(1536),
            metadata JSONB
        )
    """);
        jdbcTemplate.update("DELETE FROM vector_store");
    }
}



