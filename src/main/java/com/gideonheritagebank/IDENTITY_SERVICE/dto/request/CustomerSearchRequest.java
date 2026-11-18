package com.gideonheritagebank.IDENTITY_SERVICE.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.gideonheritagebank.IDENTITY_SERVICE.enums.CustomerIdentityStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CustomerSearchRequest {

    @Size(max = 150)
    @Schema(description = "Q.")
    private String q;

    @Size(max = 100)
    @Schema(description = "Name.", example = "Lagos Main Branch")
    private String name;

    @Size(max = 254)
    @Schema(description = "Email.", example = "customer@example.com")
    private String email;

    @Size(max = 30)
    @Schema(description = "Phone number.", example = "+2348012345678")
    private String phoneNumber;

    @Size(max = 12)
    @Schema(description = "Customer number.")
    private String customerNumber;

    @Schema(description = "Status.")
    private CustomerIdentityStatus status;
}
