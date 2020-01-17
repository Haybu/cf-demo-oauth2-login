/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agilehandy.cfdemooauth2login;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.tomcat.util.codec.binary.Base64;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

/**
 * @author Joe Grandja
 */
@Controller
public class MainController {
	private final WebClient webClient;
	private final ObjectMapper objectMapper;

	public MainController(WebClient webClient, ObjectMapper objectMapper) {
		this.webClient = webClient;
		this.objectMapper = objectMapper;
	}

	@GetMapping("/")
	public String index(Model model,
			@RegisteredOAuth2AuthorizedClient("sso") OAuth2AuthorizedClient authorizedClient
			, OAuth2AuthenticationToken auth) {
		model.addAttribute("userName", authorizedClient.getPrincipalName());
		model.addAttribute("clientName", authorizedClient.getClientRegistration().getClientName());
		return "index";
	}

	@GetMapping("/userinfo")
	public String userinfo(Model model, @RegisteredOAuth2AuthorizedClient("sso") OAuth2AuthorizedClient authorizedClient) {
		String userInfoEndpointUri = authorizedClient.getClientRegistration()
				.getProviderDetails().getUserInfoEndpoint().getUri();
		Map userAttributes = this.webClient
				.get()
				.uri(userInfoEndpointUri)
				.attributes(oauth2AuthorizedClient(authorizedClient))
				.retrieve()
				.bodyToMono(Map.class)
				.block();
		model.addAttribute("userAttributes", userAttributes);
		return "userinfo";
	}

	@GetMapping("/info")
	public String authorizationCode(
			Model model,
			OAuth2AuthenticationToken authentication,
			@RegisteredOAuth2AuthorizedClient("sso") OAuth2AuthorizedClient authorizedClient) throws Exception {

		// Display user information
		DefaultOidcUser defaultOidcUser = (DefaultOidcUser) authentication.getPrincipal();

		OidcUserInfo userInfo = defaultOidcUser.getUserInfo();
		if (userInfo != null) {
			model.addAttribute("user_info", toPrettyJsonString(userInfo.getClaims()));
		}

		OidcIdToken idToken = defaultOidcUser.getIdToken();
		model.addAttribute("id_token", toPrettyJsonString(parseToken(idToken.getTokenValue())));

		OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
		if (accessToken != null) {
			String accessTokenValue = accessToken.getTokenValue();
			model.addAttribute("access_token", toPrettyJsonString(parseToken(accessTokenValue)));
		}

		Set<GrantedAuthority> authorities =
				authentication.getAuthorities().stream().collect(Collectors.toSet());
		model.addAttribute("authorities", authorities);

		return "info";
	}

	private Map<String, ?> parseToken(String base64Token) throws IOException {
		String token = base64Token.split("\\.")[1];
		return objectMapper.readValue(Base64.decodeBase64(token), new TypeReference<Map<String, ?>>() {
		});
	}

	private String toPrettyJsonString(Object object) throws Exception {
		return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
	}
}