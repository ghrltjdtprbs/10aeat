package com.final_10aeat.domain.member.docs;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.final_10aeat.common.util.member.WithMember;
import com.final_10aeat.docs.RestDocsSupport;
import com.final_10aeat.domain.member.controller.MyPageController;
import com.final_10aeat.domain.member.dto.request.BuildingInfoRequestDto;
import com.final_10aeat.domain.member.dto.response.MyBuildingInfoResponseDto;
import com.final_10aeat.domain.member.dto.response.MyCommentsResponseDto;
import com.final_10aeat.domain.member.dto.response.MyInfoResponseDto;
import com.final_10aeat.domain.member.dto.response.MySaveResponseDto;
import com.final_10aeat.domain.member.service.MyPageService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@WithMember
public class MyPageControllerDocsTest extends RestDocsSupport {

    private MyPageService myPageService;

    @Override
    public Object initController() {
        myPageService = Mockito.mock(MyPageService.class);
        return new MyPageController(myPageService);
    }

    @BeforeEach
    public void setUp(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders
            .standaloneSetup(initController())
            .apply(documentationConfiguration(restDocumentation))
            .build();
    }

    @DisplayName("건물 정보 조회 API 문서화")
    @Test
    void testGetBuildingInfo() throws Exception {
        MyBuildingInfoResponseDto buildingInfo1 = MyBuildingInfoResponseDto.builder()
            .buildingInfoId(1L)
            .officeName("갑을그레이트벨리")
            .dong("101동")
            .ho("201호")
            .build();

        MyBuildingInfoResponseDto buildingInfo2 = MyBuildingInfoResponseDto.builder()
            .buildingInfoId(2L)
            .officeName("갑을그레이트벨리")
            .dong("102동")
            .ho("202호")
            .build();

        List<MyBuildingInfoResponseDto> buildingInfoList = List.of(buildingInfo1, buildingInfo2);

        when(myPageService.getBuildingInfo(Mockito.any())).thenReturn(buildingInfoList);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/my/building/units")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("get-building-info",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("code").description("응답 상태 코드"),
                    fieldWithPath("data[].buildingInfoId").description("동호수 정보 ID"),
                    fieldWithPath("data[].officeName").description("건물 이름"),
                    fieldWithPath("data[].dong").description("동"),
                    fieldWithPath("data[].ho").description("호")
                )
            ));
    }

    @DisplayName("나의 정보 조회 API 문서화")
    @Test
    void testGetMyInfo() throws Exception {
        MyInfoResponseDto myInfoResponse = new MyInfoResponseDto(
            "홍길동", "OWNER", "갑을그레이트벨리"
        );

        when(myPageService.getMyInfo(Mockito.any())).thenReturn(myInfoResponse);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/my/info")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("get-my-info",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("code").description("응답 상태 코드"),
                    fieldWithPath("data.name").description("사용자 이름"),
                    fieldWithPath("data.role").description("사용자 역할"),
                    fieldWithPath("data.officeName").description("건물 이름")
                )
            ));
    }

    @DisplayName("건물 정보 추가 API 문서화")
    @Test
    void testAddBuildingInfo() throws Exception {
        BuildingInfoRequestDto buildingInfoRequest = new BuildingInfoRequestDto(
            "103동", "203호"
        );

        mockMvc.perform(RestDocumentationRequestBuilders.post("/my/building/units")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildingInfoRequest)))
            .andExpect(status().isOk())
            .andDo(document("add-building-info",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                requestFields(
                    fieldWithPath("dong").description("동"),
                    fieldWithPath("ho").description("호")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 상태 코드")
                )
            ));
    }

    @DisplayName("건물 정보 삭제 API 문서화")
    @Test
    void testRemoveBuildingInfo() throws Exception {
        Long buildingInfoId = 1L;

        mockMvc.perform(
                RestDocumentationRequestBuilders.delete("/my/building/units/{buildingInfoId}",
                        buildingInfoId)
                    .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("remove-building-info",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                pathParameters(
                    parameterWithName("buildingInfoId").description("동호수 정보 ID")
                ),
                responseFields(
                    fieldWithPath("code").description("응답 상태 코드")
                )
            ));
    }

    @DisplayName("내 저장된 게시글 조회 API 문서화")
    @Test
    void testGetMySavedArticles() throws Exception {
        MySaveResponseDto savedArticle1 = new MySaveResponseDto(1L, "게시글 제목1", LocalDateTime.now(), "김관리");
        MySaveResponseDto savedArticle2 = new MySaveResponseDto(2L, "게시글 제목2", LocalDateTime.now(), "이관리");
        List<MySaveResponseDto> savedArticles = List.of(savedArticle1, savedArticle2);

        when(myPageService.getMySavedArticles(Mockito.any())).thenReturn(savedArticles);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/my/saved-articles")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("get-my-saved-articles",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("code").description("응답 상태 코드"),
                    fieldWithPath("data[].ArticleId").description("게시글 ID"),
                    fieldWithPath("data[].title").description("게시글 제목"),
                    fieldWithPath("data[].createdAt").description("생성 일시"),
                    fieldWithPath("data[].Writer").description("게시글 작성자")
                )
            ));
    }

    @DisplayName("내 게시글 댓글 조회 API 문서화")
    @Test
    void testGetMyComments() throws Exception {
        MyCommentsResponseDto comment1 = new MyCommentsResponseDto(1L, "댓글 내용1", LocalDateTime.now(), "김소유");
        MyCommentsResponseDto comment2 = new MyCommentsResponseDto(2L, "댓글 내용2", LocalDateTime.now(), "김소유");
        List<MyCommentsResponseDto> comments = List.of(comment1, comment2);

        when(myPageService.getMyComments(Mockito.any())).thenReturn(comments);

        mockMvc.perform(RestDocumentationRequestBuilders.get("/my/comments")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andDo(document("get-my-comments",
                preprocessRequest(prettyPrint()),
                preprocessResponse(prettyPrint()),
                responseFields(
                    fieldWithPath("code").description("응답 상태 코드"),
                    fieldWithPath("data[].articleId").description("관련 게시글 ID"),
                    fieldWithPath("data[].content").description("댓글 내용"),
                    fieldWithPath("data[].createdAt").description("댓글 작성 시간"),
                    fieldWithPath("data[].writer").description("댓글 작성자")
                )
            ));
    }
}
