package fitrecommend.fitquest.api;

import fitrecommend.fitquest.domain.Gym;
import fitrecommend.fitquest.domain.GymType;
import fitrecommend.fitquest.domain.Progress;
import fitrecommend.fitquest.repository.GymJPARepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GymApiController {

    private final GymJPARepository gymJPARepository;

    @GetMapping("/gym/all")
    public ResponseEntity<List<GymResponseDto>> getGym(){
        List<Gym> gyms = gymJPARepository.findAll();
        List<GymResponseDto> gymDtos = new ArrayList<>();
        int i=1;
        for (Gym gym : gyms) {
            GymResponseDto gymDto = new GymResponseDto();
            gymDto.setGymId(gym.getId());
            gymDto.setName(gym.getName());
            gymDto.setType(gym.getType());
            gymDto.setUrl(gym.getUrl());
            i++;

            gymDtos.add(gymDto);
            if(i > 43)break; // 계속 반복문 돌아서 그냥 43개에서 멈춰버림
        }
        return ResponseEntity.ok(gymDtos);
    }



    @Data
    public static class GymResponseDto{
        private Long gymId;
        private String name;
        private GymType type;
        private String url;

        public GymResponseDto(){

        }
    }

}
