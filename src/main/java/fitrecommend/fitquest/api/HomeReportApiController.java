package fitrecommend.fitquest.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fitrecommend.fitquest.domain.*;
import fitrecommend.fitquest.repository.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HomeReportApiController {

    private final HomeReportJPARepository homeReportJPARepository;
    private final MemberRepository memberRepository;

    private final HomeReportRepository homeReportRepository;

    private final HomeJPARepository homeJPARepository;
    private final HomeRepository homeRepository;

/**
 * 홈트 데이터셋 전체 찍어보기
 * */
    @GetMapping("/hoyeon")
    public ResponseEntity<List<HoyeonDTO>> hoho(){
        List<HoyeonDTO> hoyeons = new ArrayList<>();
        List<Home> hoho = homeJPARepository.findAll();
        for(Home home : hoho){
            HoyeonDTO  hoyeonDTO = new HoyeonDTO();
            hoyeonDTO.setName(home.getVideoName());
            hoyeonDTO.setUrl(home.getUrl());
            hoyeons.add(hoyeonDTO);
        }
        return ResponseEntity.ok(hoyeons);
    }


    @GetMapping("/home/progress/{memberId}") // 홈트운동 진행여부에 리턴
    public ResponseEntity<HomeProgressResponseDto> getHomeProgress(@PathVariable Long memberId){
        Member member = memberRepository.findOne(memberId);

        List<HomeReport> homeReports = homeReportJPARepository.findByMember(member);

        HomeProgressResponseDto homeProgressResponseDto = new HomeProgressResponseDto();

        if (homeReports.get(homeReports.size() - 1).getProgress() == Progress.READY) {
            homeProgressResponseDto.setProgress(Progress.READY);
            homeProgressResponseDto.setNextApi("report");
            return ResponseEntity.ok(homeProgressResponseDto);
        } else if (homeReports.get(homeReports.size() - 1).getProgress() == Progress.INPROGRESS) {
            homeProgressResponseDto.setProgress(Progress.INPROGRESS);
            homeProgressResponseDto.setNextApi("report");
            return ResponseEntity.ok(homeProgressResponseDto);
        }
        else {
            homeProgressResponseDto.setProgress(Progress.COMPLETE);
            LocalDateTime endtime = homeReports.get(homeReports.size() - 1).getEndtime();
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(endtime, now);
            long hoursSinceEnd = duration.toHours();
            if (hoursSinceEnd < 12) {
                homeProgressResponseDto.setNextApi("report");
            } else {
                homeProgressResponseDto.setNextApi("recommend");
            }
            return ResponseEntity.ok(homeProgressResponseDto);
        }
    }

    // 플러터입장
    // 1. nextapi의 값을 확인하고 어떠한 api를 호출할지 결정합니다.
    // 2. Progress의 상태를 확인하고 어떤 화면으로 넘어갈지 결정합니다.
    // report - 보고서를 조회하고 recommend - 운동을 추천해주세요
    // COMPLETE, report - 운동 완료 페이지 이동, 운동보고서 api 호출
    // COMPLETE, recommend - 운동 전 페이지 이동, 운동추천 api호출
    // INPROGRESS, report - 운동 중 페이지 이동, 운동보고서 api호출
    // READY, report - 운동 전 페이지 이동, 운동보고서 api호출
    // NULL, recommend - 운동 전 페이지 이동, 운동 추천 api호출

    @GetMapping("/home/recommend/{memberId}")
    public ResponseEntity<HomeRecommendResponseDto> homeRecommend(@PathVariable Long memberId)throws JsonProcessingException {
        HomeRecommendResponseDto homeRecommendResponseDto = new HomeRecommendResponseDto();
        Member member = memberRepository.findOne(memberId);
        // HomeReport의 Home의 HomeType이 같은 것을 조회하고
        List<HomeReport> homeReports1 = homeReportRepository.findByHomeType(member.getSurvey().getPrefer1()); // 이번에 어떤 운동을 할건데
        List<HomeReport> homeReports2 = homeReportRepository.findByHomeType(member.getSurvey().getPrefer2()); // 이번에 어떤 운동을 할건데

        FlaskRecommendRequestDto requestDto = new FlaskRecommendRequestDto();

        if(homeReports1.size()==0 || homeReports2.size() == 0){
            requestDto.setMemberId(memberId);
            HomePreferDto homePreferDto1 = new HomePreferDto();
            homePreferDto1.setHomeType(member.getSurvey().getPrefer1());
            homePreferDto1.setLastHomeId(null);
            homePreferDto1.setSatisfaction(0);
            requestDto.getHomePreferDtos().add(homePreferDto1);

            HomePreferDto homePreferDto2 = new HomePreferDto();
            homePreferDto2.setHomeType(member.getSurvey().getPrefer2());
            homePreferDto2.setLastHomeId(null);
            homePreferDto2.setSatisfaction(0);
            requestDto.getHomePreferDtos().add(homePreferDto2);
        }
        else{
            HomeReport homeReport1 = homeReports1.get(homeReports1.size()-1);
            HomeReport homeReport2 = homeReports2.get(homeReports2.size()-1);


            requestDto.setMemberId(memberId);
            HomePreferDto homePreferDto1 = new HomePreferDto();
            homePreferDto1.setHomeType(homeReport1.getHome().getType());
            homePreferDto1.setLastHomeId(homeReport1.getHome().getId());
            homePreferDto1.setSatisfaction(homeReport1.getSatisfaction());
            requestDto.getHomePreferDtos().add(homePreferDto1);

            HomePreferDto homePreferDto2 = new HomePreferDto();
            homePreferDto2.setHomeType(homeReport2.getHome().getType());
            homePreferDto2.setLastHomeId(homeReport2.getHome().getId());
            homePreferDto2.setSatisfaction(homeReport2.getSatisfaction());
            requestDto.getHomePreferDtos().add(homePreferDto2);
        }



        // JSON 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String jsonData = objectMapper.writeValueAsString(requestDto);

        // HTTP 요청 보내기
        String url = "http://13.238.5.151:5000/api/v1/ai/home/recommend";  // 플라스크 API의 엔드포인트 URL
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(jsonData, headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<FlaskRecommendResponseDto> responseEntity = restTemplate.postForEntity(url, requestEntity, FlaskRecommendResponseDto.class);

        HomeReport homeReport = new HomeReport();
        homeReport.setMember(member);
        member.getHomeReports().add(homeReport);
        memberRepository.save(member);

        Home home = homeRepository.findOne(responseEntity.getBody().getHomeId());
        homeReport.setHome(home);
        homeReportJPARepository.save(homeReport);
        homeRecommendResponseDto.setType(home.getType());
        homeRecommendResponseDto.setVideoName(home.getVideoName());
        homeRecommendResponseDto.setUrl(home.getUrl());
        return ResponseEntity.ok(homeRecommendResponseDto);

    }

    /**홈아이디 가져와서 디티오 정상출력되는지 확인*/
//    @GetMapping ("/chohoyeon/{homeId}")
//    public ResponseEntity<HomeRecommendResponseDto> zz(@PathVariable Long homeId){
//2023-05-30T13:10:00.702Z ERROR 15460 --- [nio-8080-exec-6] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: java.lang.NullPointerException: Cannot invoke "fitrecommend.fitquest.domain.Survey.getGoal1()" because the return value of "fitrecommend.fitquest.domain.Member.getSurvey()" is null] with root cause
//
//java.lang.NullPointerException: Cannot invoke "fitrecommend.fitquest.domain.Survey.getGoal1()" because the return value of "fitrecommend.fitquest.domain.Member.getSurvey()" is null
//        HomeReport homeReport = new HomeReport();
//        HomeRecommendResponseDto homeRecommendResponseDto = new HomeRecommendResponseDto();
//
//        Home home = homeRepository.findOne(homeId);
//        homeReport.setHome(home);
//        homeReportJPARepository.save(homeReport);
//        homeRecommendResponseDto.setType(home.getType());
//        homeRecommendResponseDto.setVideoName(home.getVideoName());
//        homeRecommendResponseDto.setUrl(home.getUrl());
//        return ResponseEntity.ok(homeRecommendResponseDto);
//    }


    @PostMapping("/home/report/save") // 현재 데이터베이스 있는 그대로 시간만 추가하면된다.
    public ResponseEntity<HomeReportSaveResponse> homeReportSave(@RequestBody HomeReportSaveRequest homeReportSaveRequest){
        HomeReportSaveResponse homeReportSaveResponse = new HomeReportSaveResponse();
        Member member = memberRepository.findOne(homeReportSaveRequest.getMemberId());
        List<HomeReport> homeAllReports = homeReportJPARepository.findByMember(member);
        HomeReport homeReport = homeAllReports.get(homeAllReports.size()-1);
        homeReport.setStarttime(homeReportSaveRequest.getStartTime());
        homeReport.setProgress(Progress.INPROGRESS);
        homeReportJPARepository.save(homeReport);
        homeReportSaveResponse.state = "Success";
        return ResponseEntity.ok(homeReportSaveResponse);
    }

    @PostMapping("/home/report/complete") // 끝난시간 저장하고 끝?
    public ResponseEntity<HomeReportCompleteResponse> homeReportComplete(@RequestBody HomeReportCompleteRequest homeReportCompleteRequest){
        HomeReportCompleteResponse homeReportCompleteResponse = new HomeReportCompleteResponse();
        Member member = memberRepository.findOne(homeReportCompleteRequest.getMemberId());
        List<HomeReport> homeAllReports = homeReportJPARepository.findByMember(member);
        HomeReport homeReport = homeAllReports.get(homeAllReports.size()-1);
        homeReport.setEndtime(homeReportCompleteRequest.getEndTime());
        homeReport.setDuration(homeReportCompleteRequest.getDuration());
        homeReport.setProgress(Progress.COMPLETE);
        homeReportJPARepository.save(homeReport);
        homeReportCompleteResponse.setState("Success");
        return ResponseEntity.ok(homeReportCompleteResponse);
    }


    @PostMapping("/home/report/satisfaction")
    public ResponseEntity<HomeReportSatisfactionResponse> homeReportSaveSatisfaction(@RequestBody HomeReportSatisfactionRequest homeReportSatisfactionRequest){
        HomeReportSatisfactionResponse homeReportSatisfactionResponse = new HomeReportSatisfactionResponse();
        Member member = memberRepository.findOne(homeReportSatisfactionRequest.getMemberId());
        List<HomeReport> homeAllReports = homeReportJPARepository.findByMember(member);
        HomeReport homeReport = homeAllReports.get(homeAllReports.size()-1);
        homeReport.setName(homeReportSatisfactionRequest.getReportName());
        homeReport.setSatisfaction(homeReportSatisfactionRequest.getSatisfaction());
        homeReportJPARepository.save(homeReport);
        homeReportSatisfactionResponse.setState("Success");
        return ResponseEntity.ok(homeReportSatisfactionResponse);
    }


    @Data
    public class HomeProgressResponseDto{
        private Progress progress;
        private String nextApi;

        public HomeProgressResponseDto(){

        }
    }

    @Data
    public static class HomeRecommendResponseDto{
        private HomeType type;
        private String videoName;
        private String url;
    }

    @Data
    public static class FlaskRecommendRequestDto{
        private Long memberId;
        private List<HomePreferDto> homePreferDtos = new ArrayList<>();
    }

    @Data
    public static class HomePreferDto{
        private HomeType homeType;
        private Long lastHomeId;
        private Integer satisfaction;
    }


    @Data
    public static class FlaskRecommendResponseDto{
        private Long homeId;
    }

    @Data
    public static class HomeReportSaveRequest{
        private Long memberId;
        private LocalDateTime startTime;

        public HomeReportSaveRequest(){

        }
    }

    @Data
    public static class HomeReportSaveResponse{
        private String state;
        public HomeReportSaveResponse(){

        }
    }

    @Data
    public static class HomeReportCompleteRequest{

        private Long memberId;
        private LocalDateTime endTime;
        private Integer duration;
        public HomeReportCompleteRequest(){

        }
    }

    @Data
    public static class HomeReportCompleteResponse{
        private String state;
        public HomeReportCompleteResponse(){
        }
    }

    @Data
    public static class HomeReportSatisfactionRequest{
        private Long memberId;
        private String reportName;
        private Integer satisfaction;

        public HomeReportSatisfactionRequest(){
        }
    }

    @Data
    public class HomeReportSatisfactionResponse{
        private String state;
        public HomeReportSatisfactionResponse(){
        }
    }

    @Data
    private class HoyeonDTO {
        private String url;
        private String name;

    }

    @Data
    private class HoyeonDTOs {
        private List<HoyeonDTO> hoyeonDTOs = new ArrayList<>();
    }

    //



}
