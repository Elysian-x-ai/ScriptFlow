package com.scriptflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.scriptflow.common.util.StringUtils;
import com.scriptflow.dal.entity.project.NovelChapter;
import com.scriptflow.dal.mapper.project.NovelChapterMapper;
import com.scriptflow.framework.service.BaseService;
import com.scriptflow.framework.service.Converter;
import com.scriptflow.project.dto.ChapterCreateDTO;
import com.scriptflow.project.dto.ChapterUpdateDTO;
import com.scriptflow.project.dto.ChapterVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NovelChapterService extends BaseService<NovelChapter, ChapterVO> {

    private final NovelChapterMapper chapterMapper;

    @Override
    protected BaseMapper<NovelChapter> getMapper() {
        return chapterMapper;
    }

    @Override
    protected Converter<NovelChapter, ChapterVO> getConverter() {
        return this::toVO;
    }

    @Override
    public ChapterVO getById(Long id) {
        NovelChapter chapter = findByIdOrThrow(id);
        return toVOWithContent(chapter);
    }

    public List<ChapterVO> listByProjectId(Long projectId) {
        return list(new LambdaQueryWrapper<NovelChapter>()
                .eq(NovelChapter::getProjectId, projectId)
                .orderByAsc(NovelChapter::getChapterNo));
    }

    @Transactional(rollbackFor = Exception.class)
    public ChapterVO create(ChapterCreateDTO dto) {
        NovelChapter chapter = new NovelChapter();
        chapter.setProjectId(dto.getProjectId());
        chapter.setChapterNo(dto.getChapterNo());
        chapter.setTitle(dto.getTitle());
        chapter.setContent(dto.getContent());
        chapter.setWordCount(dto.getContent().length());
        chapter.setContentHash(md5(dto.getContent()));
        chapterMapper.insert(chapter);
        return toVOWithContent(chapter);
    }

    @Transactional(rollbackFor = Exception.class)
    public ChapterVO update(ChapterUpdateDTO dto) {
        NovelChapter chapter = findByIdOrThrow(dto.getId());
        if (StringUtils.isNotBlank(dto.getTitle())) chapter.setTitle(dto.getTitle());
        if (StringUtils.isNotBlank(dto.getContent())) {
            chapter.setContent(dto.getContent());
            chapter.setWordCount(dto.getContent().length());
            chapter.setContentHash(md5(dto.getContent()));
        }
        if (dto.getChapterNo() != null) chapter.setChapterNo(dto.getChapterNo());
        chapterMapper.updateById(chapter);
        return toVOWithContent(chapter);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        deleteById(id);
    }

    private String md5(String content) {
        if (content == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(32);
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
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
        vo.setContentHash(chapter.getContentHash());
        vo.setCreateTime(chapter.getCreateTime());
        return vo;
    }

    private ChapterVO toVOWithContent(NovelChapter chapter) {
        ChapterVO vo = toVO(chapter);
        vo.setContent(chapter.getContent());
        return vo;
    }
}
