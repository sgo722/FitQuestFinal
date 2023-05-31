package fitrecommend.fitquest.repository;

import fitrecommend.fitquest.domain.Exercise;
import fitrecommend.fitquest.domain.Gym;
import fitrecommend.fitquest.domain.GymReport;
import fitrecommend.fitquest.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ExerciseJPARepository extends JpaRepository<Exercise, Long> {


    Exercise findByGymReportAndGymId(GymReport gymReport, Long gymId);




}
