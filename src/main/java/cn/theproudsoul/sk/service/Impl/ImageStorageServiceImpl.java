package cn.theproudsoul.sk.service.Impl;

import cn.theproudsoul.sk.constants.StorageProperties;
import cn.theproudsoul.sk.web.exception.StorageException;
import cn.theproudsoul.sk.persistence.model.ImageMetadataModel;
import cn.theproudsoul.sk.persistence.repository.ImageMetadataRepository;
import cn.theproudsoul.sk.service.ImageStorageService;
import cn.theproudsoul.sk.web.vo.ImageHistoryVo;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * @author TheProudSoul
 */
@Slf4j
@Service
public class ImageStorageServiceImpl implements ImageStorageService {
    private final Path rootLocation;

    private final ImageMetadataRepository imageMetadataRepository;

    private final static String ALLOW_UPLOAD_IMAGE_EXT = "jpg jpeg png jiff gif webp svg ico bmp";
    private final static String SCALE_DIR = "scale";

    @Autowired
    public ImageStorageServiceImpl(StorageProperties properties, ImageMetadataRepository imageMetadataRepository) {
        this.rootLocation = Paths.get(properties.getImageLocation());
        log.info("image location {}", rootLocation);
        this.imageMetadataRepository = imageMetadataRepository;
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }

    @Override
    public String store(MultipartFile file, long userId) {
        String originFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originFilename);
        String newName = UUID.randomUUID().toString();
        if (!checkPicturesExtension(extension)) {
            throw new StorageException("图片类型不支持！");
        }
        long size = file.getSize();
        if (size == 0 || size > MAX_FILE_UPLOAD_SIZE) {
            throw new IllegalArgumentException("文件必须在 0-10M 之间");
        }
        try {
            if (originFilename.contains("..")) {
                // This is a security check
                throw new StorageException("Cannot store file with relative path outside current directory " + originFilename);
            }
//            /*  */
            Path userDirector = this.rootLocation.resolve(String.valueOf(userId));
            Path userScaleDirector = this.rootLocation.resolve(SCALE_DIR).resolve(String.valueOf(userId));
            if (Files.notExists(userDirector)) {
                Files.createDirectories(userDirector);
            }
            if (Files.notExists(userScaleDirector)) {
                Files.createDirectories(userScaleDirector);
            }

            ImageMetadataModel model = new ImageMetadataModel();
            model.setOriginName(originFilename);
            model.setUserId(userId);
            model.setImageUrl("http://47.115.40.131:9999/images/" + userId + "/" + newName + '.' + extension);
            model.setScaleUrl("http://47.115.40.131:9999/images/" + SCALE_DIR + "/" + userId + "/" + newName + '.' + extension);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, userDirector.resolve(newName + '.' + extension),
                        StandardCopyOption.REPLACE_EXISTING);
                Thumbnails.of(userDirector.resolve(newName + '.' + extension).toFile()).size(50, 30).toFile(userScaleDirector.resolve(newName + '.' + extension).toFile());
                imageMetadataRepository.insert(model);
                return imageMetadataRepository.getById(model.getId()).getImageUrl();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new StorageException("Failed to store file " + originFilename, e);
        }
    }

    @Override
    public List<ImageHistoryVo> listAll(long userId, int offset, int limit) {
        return imageMetadataRepository.getUserImageList(userId, offset, limit);
    }

//    private Path load(String filename) {
//        return rootLocation.resolve(filename);
//    }

//    @Override
//    public Path loadAsResource(long user, String filename) {
//        Path file = load(user + File.separator + filename);
//        return file;
//        try {
//            Resource resource = new UrlResource(file.toUri());
//            if (resource.exists() || resource.isReadable()) {
//                return resource;
//            } else {
//                throw new StorageFileNotFoundException(
//                        "Could not read file: " + filename);
//
//            }
//        } catch (MalformedURLException e) {
//            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
//        }
//    }

    @Override
    public boolean deleteAll(long userId) {
        imageMetadataRepository.deleteAll(userId);
        FileSystemUtils.deleteRecursively(rootLocation.resolve(String.valueOf(userId)).toFile());
        return true;
    }

    @Override
    public boolean deleteOne(long userId, long imageId) {
        ImageMetadataModel model = imageMetadataRepository.getById(imageId);
        if (model == null || model.getUserId() != userId) {
            return false;
        }
        Path filePath = rootLocation.resolve(String.valueOf(userId)).resolve(StringUtils.getFilename(model.getImageUrl()));
        try {
            if (Files.exists(filePath)) {
                // https://blog.csdn.net/LuoZheng4698729/article/details/51697648
                Files.delete(filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        imageMetadataRepository.deleteOne(imageId);
        return true;
    }

    @Override
    public int getCount(long user) {
        return imageMetadataRepository.count(user);
    }

    /**
     * 检测图片后缀
     *
     * @param extension 文件后缀
     * @return 是否为合法图片后缀
     */
    private boolean checkPicturesExtension(String extension) {
        if (null == extension) {
            return false;
        }
        return ALLOW_UPLOAD_IMAGE_EXT.contains(extension.toLowerCase());
    }
}
