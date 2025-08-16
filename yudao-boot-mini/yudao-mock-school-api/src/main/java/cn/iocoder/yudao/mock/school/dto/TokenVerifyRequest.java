package cn.iocoder.yudao.mock.school.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Token验证请求DTO
 * 
 * @author Claude
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerifyRequest {

    /**
     * 需要验证的token
     */
    @NotBlank(message = "Token不能为空")
    private String token;
}