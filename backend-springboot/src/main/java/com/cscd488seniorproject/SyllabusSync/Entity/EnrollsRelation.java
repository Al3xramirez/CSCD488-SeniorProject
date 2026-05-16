import jakarta.persistence.*;

@Entity
@Table(name = "enrollsrelation")
public class EnrollsRelation {

    @EmbeddedId
    private EnrollmentId id;

    // Map UserID (part of composite key)
    @MapsId("userID")
    @ManyToOne
    @JoinColumn(name = "UserID")
    private UserAccount user;

    // Map ClassCode+Quarter+Year (part of composite key)
    @MapsId("classCode")
    @MapsId("quarter")
    @MapsId("year")
    @ManyToOne
    @JoinColumns({
        @JoinColumn(name = "ClassCode", referencedColumnName = "classCode"),
        @JoinColumn(name = "Quarter", referencedColumnName = "quarter"),
        @JoinColumn(name = "Year", referencedColumnName = "year")
    })
    private Course course;

    public EnrollsRelation() {}

    public EnrollsRelation(UserAccount user, Course course) {
        this.user = user;
        this.course = course;
        this.id = new EnrollmentId(
                user.getUserID(),
                course.getId().getClassCode(),
                course.getId().getQuarter(),
                course.getId().getYear()
        );
    }
}