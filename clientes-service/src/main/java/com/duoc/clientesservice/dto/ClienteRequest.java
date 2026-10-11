package com.duoc.clientesservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClienteRequest(
        @NotBlank @Pattern(regexp = "[0-9]{7,8}-[0-9Kk]") String rut,
        @NotBlank @Size(max = 120) String nombre,
        @NotBlank @Email @Size(max = 160) String email,
        @Size(max = 30) String telefono,
        @NotBlank @Pattern(regexp = "PERSONA|PYME|PREMIUM") String segmento
) {
}
