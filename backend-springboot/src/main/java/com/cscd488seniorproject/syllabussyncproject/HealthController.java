<<<<<<< HEAD:backend-springboot/src/main/java/com/cscd488seniorproject/syllabussync/HealthController.java
package com.cscd488SeniorProject.SyllabusSync;
=======
package com.cscd488seniorproject.syllabussyncproject;
>>>>>>> testing/LocalDBSetup:backend-springboot/src/main/java/com/cscd488seniorproject/syllabussyncproject/HealthController.java

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String healthCheck() {
        return "OK";
    }
    
}
