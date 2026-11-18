package com.gideonheritagebank.IDENTITY_SERVICE.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {

    @Schema(description = "Content.")
    private List<T> content;
    @Schema(description = "Page.")
    private int page;
    @Schema(description = "Size.")
    private int size;
    @Schema(description = "Total elements.")
    private long totalElements;
    @Schema(description = "Total pages.")
    private int totalPages;
    @Schema(description = "First.")
    private boolean first;
    @Schema(description = "Last.")
    private boolean last;
    @Schema(description = "Empty.")
    private boolean empty;

    public static <T> PagedResponse<T> from(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber() + 1) // Convert to 1-based index
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .empty(page.isEmpty())
                .build();
    }
}
