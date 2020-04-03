package cn.theproudsoul.justwriteit.repository;

import cn.theproudsoul.justwriteit.web.vo.FileJournalVo;
import cn.theproudsoul.justwriteit.model.FileJournalModel;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author TheProudSoul
 */
@Mapper
public interface FileJournalRepository {
    @Select("SELECT journal_id, path, event_type FROM file_journal where user_id= #{userId} and journal_id > #{journalId}")
    List<FileJournalVo> getLatestList(long userId, long journalId);

    @Select("SELECT journal_id FROM file_journal where user_id= #{userId} and journal_id > #{journalId} and path = #{path}")
    Long getPathLatest(long userId, long journalId, String path);

    @Insert("INSERT INTO file_journal (user_id, journal_id, path, event_type) VALUES (#{model.userId}, #{model.journalId}, #{model.path}, #{model.eventType})")
    int insert(@Param("model") FileJournalModel model);

    @Select("SELECT MAX(journal_id) FROM file_journal where user_id= #{userId}")
    Long getMaxJournalId(long user);
}
