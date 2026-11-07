
package team.projectpulse.student;

import team.projectpulse.system.UserUtils;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class StudentSecurityService {

    private final StudentRepository studentRepository;
    private final UserUtils userUtils;

    public StudentSecurityService(StudentRepository studentRepository, UserUtils userUtils) {
        this.studentRepository = studentRepository;
        this.userUtils = userUtils;
    }

    public boolean isStudentSelf(Integer studentId) {
        Integer studentIdFromJwt = this.userUtils.getUserId();
        return studentId.equals(studentIdFromJwt);
    }

    /**
     * Checks whether the current user is an instructor
     * assigned to the student's section.
     */
    public boolean isCurrentUserInstructorOfStudentSection(Integer studentId) {
        Integer instructorIdFromJwt = this.userUtils.getUserId();

        Student student = this.studentRepository.findById(studentId).orElse(null);

        if (student == null || student.getSection() == null) {
            return false;
        }

        return student.getSection().getInstructors().stream()
                .anyMatch(instructor ->
                        instructor.getId().equals(instructorIdFromJwt));
    }

    /**
     * Checks whether the current user is authorized to deactivate
     * the student: either an assigned section instructor or
     * the admin of the student's course.
     */
    public boolean canDeactivateStudent(Integer studentId) {
        Student student = this.studentRepository.findById(studentId).orElse(null);

        if (student == null || student.getSection() == null) {
            return false;
        }

        Integer currentUserId = this.userUtils.getUserId();

        boolean isSectionInstructor = student.getSection().getInstructors()
                .stream()
                .anyMatch(instructor ->
                        instructor.getId().equals(currentUserId));

        boolean isCourseAdmin =
                student.getSection().getCourse() != null
                        && student.getSection().getCourse().getCourseAdmin() != null
                        && student.getSection().getCourse().getCourseAdmin()
                        .getId().equals(currentUserId);

        return isSectionInstructor || isCourseAdmin;
    }
}
