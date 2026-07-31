package com.kkalchake.enlightenment.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

// Excludes sections/enrolledUsers deliberately: touching those LAZY collections
// outside a transaction would throw LazyInitializationException.
@Data @AllArgsConstructor
public class CourseDto {
    private Long id;
    private String title;
    private String description;

    // Lombok generates isPublic() as the getter for this field, and Jackson's
    // bean-property inference strips a getter's "is" prefix - so by default
    // this would serialize under the key "public", not "isPublic". Putting
    // @JsonProperty on the field itself doesn't fix that: Jackson treats a
    // private field as invisible unless annotated, so an annotated field and
    // an unannotated getter end up as two *separate* properties ("isPublic"
    // from the field, "public" from the getter) - both would appear in the
    // output. Routing the annotation onto the generated getter/setter via
    // onMethod_ instead pins the one property Jackson actually serializes.
    @Getter(onMethod_ = @JsonProperty("isPublic"))
    @Setter(onMethod_ = @JsonProperty("isPublic"))
    private boolean isPublic;

    private String sourceName;
    private String sourceUrl;
    private String sourceLicense;

    // Always an array, never null - CourseService.toDto guarantees List.of()
    // when the underlying column has no content, so the frontend never has to
    // null-check this field.
    private List<String> insights;
}
