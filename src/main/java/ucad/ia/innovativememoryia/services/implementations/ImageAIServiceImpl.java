package ucad.ia.innovativememoryia.services.implementations;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.model.Media;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import ucad.ia.innovativememoryia.services.ImageAIService;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class ImageAIServiceImpl implements ImageAIService {
    @Value("${spring.ai.openai.api-key}")
    private String openAiKey;
    private ChatClient chatClient;

    public ImageAIServiceImpl(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @Override
    public byte[] generateImageDALLE() throws IOException {
        OpenAiImageApi openAiApi = new OpenAiImageApi(openAiKey);
        OpenAiImageModel openAiImageModel = new OpenAiImageModel(openAiApi);
        ImageResponse response = openAiImageModel.call(
                new ImagePrompt("un chat developpeur avec un costume et machine dans une fête avec un café dans sa main ",
                OpenAiImageOptions.builder()
                        .withModel("dall-e-3")
                        .withQuality("hd")
                        .withN(1)
                        .withResponseFormat("b64_json")
                        .withHeight(1024)
                        .withWidth(1024)
                        .build())
        );

        String image = response.getResult().getOutput().getB64Json();
        byte[] decode = Base64.getDecoder().decode(image);
        return decode;
    }

    @Override
    public Flux<String> ocrImage(MultipartFile file) throws IOException {
        Resource resource = file.getResource();
        byte[] data = resource.getContentAsByteArray();
        SystemMessage systemMessage=new SystemMessage("""
        Vous êtes un assistant virtuel pour faire de la multi modal RAG.
        Vous allez analyser les images qui vous seront envoyé par les clients et 
        vous allez faire une description complete de l'image
        """);
        String userMessageText = """
        Analyse l'image et donne moi la description complete de l'image si possible,
        
    """;

        UserMessage userMessage = new UserMessage(userMessageText, List.of(
                new Media(MimeTypeUtils.IMAGE_JPEG, data)
        ));

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return chatClient.prompt(prompt).stream().content();
    }


}
