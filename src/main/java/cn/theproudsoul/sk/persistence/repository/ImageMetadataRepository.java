package cn.theproudsoul.sk.persistence.repository;

import cn.theproudsoul.sk.persistence.model.ImageMetadataModel;
import cn.theproudsoul.sk.web.vo.ImageHistoryVo;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @author TheProudSoul
 */
@Mapper
public interface ImageMetadataRepository {
    @Insert("INSERT IGNORE INTO image_metadata (user_id, image_url,scale_url, origin_name) VALUES (#{model.userId}, #{model.imageUrl}, #{model.scaleUrl}, #{model.originName})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(@Param("model") ImageMetadataModel model);

    @Select("SELECT id id, image_url imageUrl, scale_url scaleUrl FROM image_metadata where user_id= #{userId} ORDER BY create_time desc LIMIT #{offset}, #{limit}")
    List<ImageHistoryVo> getUserImageList(long userId, int offset, int limit);

    @Select("SELECT id id, user_id userId, image_url imageUrl , scale_url scaleUrl FROM image_metadata where id= #{id}")
    ImageMetadataModel getById(long id);

    @Delete("DELETE FROM image_metadata where user_id= #{userId}")
    int deleteAll(long userId);

    @Delete("DELETE FROM image_metadata where id= #{id}")
    int deleteOne(long id);

    @Select("SELECT COUNT(*) FROM image_metadata where user_id= #{userId}")
    int count(long userId);
}
