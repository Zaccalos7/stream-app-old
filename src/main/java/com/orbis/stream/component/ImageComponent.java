package com.orbis.stream.component;

import com.orbis.stream.exceptions.FileReadingException;
import com.orbis.stream.handler.ResponseHandler;
import com.orbis.stream.utilities.ImageUtilities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Component
public class ImageComponent {
    private final LoggerMessageComponent loggerMessageComponent;
    private final ResponseHandler responseHandler;

    private static final String UPLOAD_DIR = "images";
    private static final String IMAGE_NAME= "HOME";

    public ResponseEntity<Map<String, String>> saveImage(MultipartFile image){
        saveImageOnDirectory(image);
        return responseHandler.buildResponse("image.saved", HttpStatus.CREATED);
    }

    private void saveImageOnDirectory(MultipartFile file){

        Path uploadPath = Paths.get(UPLOAD_DIR);

        try{
            if (!Files.exists(uploadPath))
                Files.createDirectories(uploadPath);

            String imageName = file.getOriginalFilename();
            assert imageName != null;
            String[] imageType = imageName.split("\\.");
            int lenght = imageType.length -1;
            //if image name has . before the extension
            String extension = imageType[lenght];

            // i don't save more than one image, if you put another image i overwrite it
            Path filePath = uploadPath.resolve(IMAGE_NAME + "." + extension);

            //delete previous image, maybe the dot of image change and when i replace i didn't delete or
            //retrieve the correct image
            var previousImage = loadImage();
            if(previousImage != null){
                Path pathOfPreviousImage  = previousImage.getResource().getFile().toPath();
                Files.deleteIfExists(pathOfPreviousImage);
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (Exception e) {
            log.error(loggerMessageComponent.printMessage("error.during.save.file"), e);
            throw new FileReadingException("error.during.save.file");
        }

    }

    public ImageUtilities loadImage() {
        File folder = new File(UPLOAD_DIR);
        if(!folder.isDirectory() || !folder.exists() ){
            log.error(loggerMessageComponent.printMessage("folder.not.found"));
            throw new FileReadingException("folder.not.found");
        }
        return loadImageFromDirectory(folder);
    }

    private ImageUtilities loadImageFromDirectory(File folder) {
        File[] files = folder.listFiles();

        if (files == null || files.length == 0) {
            return null;
        }
        File file = files[0];

        try{
            Path path = file.toPath().normalize();
            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                log.error(loggerMessageComponent.printMessage("resource.not.found"));
                throw new FileReadingException("resource.not.found");
            }

            String contentType = Files.probeContentType(path);
            if (contentType == null)
                contentType = "application/octet-stream";


            return ImageUtilities
                    .builder()
                    .contentType(contentType)
                    .resource(resource)
                    .build();
        }catch (IOException e){
            log.error(loggerMessageComponent.printMessage("error.during.loading"), e);
            throw new FileReadingException("error.during.loading");
        }

    }
}
