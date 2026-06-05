package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.scriptflow.common.exception.BusinessException;
import com.scriptflow.common.result.ResultCode;
import com.scriptflow.common.util.StringUtils;
import com.scriptflow.dal.entity.project.NovelChapter;
import com.scriptflow.dal.mapper.project.NovelChapterMapper;
import com.scriptflow.project.dto.ChapterCreateDTO;
import com.scriptflow.project.dto.ChapterUpdateDTO;
import com.scriptflow.project.dto.ChapterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Novel chapter management service.
 */
@Service
@RequiredArgsConstructor
public class NovelChapterService {

    private final NovelChapterMapper chapterMapper;

    public List<ChapterVO> listByProjectId(Long projectId) {
        return chapterMapper.selectList(
                        new LambdaQueryWrapper<NovelChapter>()
                                .eq(NovelChapter::getProjectId, projectId)
                                .orderByAsc(NovelChapter::getChapterNo))
                .stream()
                .map(this::toVO)
                .toList();
    }

    public ChapterVO getById(Long id) {
        NovelChapter chapter = chapterMapper.selectById(id);
        if (chapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Chapter not found");
        }
        return toVOWithContent(chapter);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChapterVO create(ChapterCreateDTO dto) {
        NovelChapter chapter = new NovelChapter();
        chapter.setProjectId(dto.getProjectId());
        chapter.setChapterNo(dto.getChapterNo());
        chapter.setTitle(dto.getTitle());
        chapter.setContent(dto.getContent());
        chapter.setWordCount(dto.getContent().length());
        chapterMapper.insert(chapter);
        return toVOWithContent(chapter);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChapterVO update(ChapterUpdateDTO dto) {
        NovelChapter chapter = chapterMapper.selectById(dto.getId());
        if (chapter == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "Chapter not found");
        }
        if (StringUtils.isNotBlank(dto.getTitle())) chapter.setTitle(dto.getTitle());
        if (StringUtils.isNotBlank(dto.getContent())) {
            chapter.setContent(dto.getContent());
            chapter.setWordCount(dto.getContent().length());
        }
        if (dto.getChapterNo() != null) chapter.setChapterNo(dto.getChapterNo());
        chapterMapper.updateById(chapter);
        return toVOWithContent(chapter);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        chapterMapper.deleteById(id);
    }

    private ChapterVO toVO(NovelChapter chapter) {
        ChapterVO vo = new ChapterVO();
        vo.setId(chapter.getId());
        vo.setProjectId(chapter.getProjectId());
        vo.setChapterNo(chapter.getChapterNo());
        vo.setTitle(chapter.getTitle());
        vo.setContentPreview(StringUtils.truncate(chapter.getContent(), 200));
        vo.setWordCount(chapter.getWordCount());
        vo.setSummary(chapter.getSummary());
        vo.setCreateTime(chapter.getCreateTime());
        return vo;
    }

    private ChapterVO toVOWithContent(NovelChapter chapter) {
        ChapterVO vo = toVO(chapter);
        vo.setContent(chapter.getContent());
        return vo;
    }
}
