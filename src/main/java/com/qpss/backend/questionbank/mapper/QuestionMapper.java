package com.qpss.backend.questionbank.mapper;
import com.qpss.backend.questionbank.Question;
import com.qpss.documentextraction.model.ParsedQuestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
@Mapper(componentModel = "spring")
// Refresh IDE
public interface QuestionMapper {
    QuestionMapper INSTANCE = Mappers.getMapper(QuestionMapper.class);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sourceDocumentId", source = "sourceDocumentId")
    Question toEntity(ParsedQuestion parsed, Long subjectId, Long sessionId, Long sourceDocumentId, String sourceFileName);
}