package br.com.urlShortner;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UrlData {

    private String originalUrl;
    private Long expirationTime;
}
