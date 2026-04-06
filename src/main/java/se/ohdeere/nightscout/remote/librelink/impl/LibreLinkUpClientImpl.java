package se.ohdeere.nightscout.remote.librelink.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.ohdeere.nightscout.remote.librelink.LibreLinkUpClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Component;

@Component
class LibreLinkUpClientImpl implements LibreLinkUpClient {

	private static final Logger LOG = LoggerFactory.getLogger(LibreLinkUpClientImpl.class);

	private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU OS 17_4.1 like Mac OS X) "
			+ "AppleWebKit/536.26 (KHTML, like Gecko) Version/17.4.1 Mobile/10A5355d Safari/8536.25";

	private static final String VERSION = "4.16.0";

	private final HttpClient httpClient;

	private final ObjectMapper objectMapper;

	LibreLinkUpClientImpl(ObjectMapper objectMapper) {
		this.httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
		this.objectMapper = objectMapper;
	}

	@Override
	public Optional<AuthTicket> login(String email, String password, String apiHost) {
		try {
			String body = this.objectMapper.writeValueAsString(new LoginRequest(email, password));
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://" + apiHost + "/llu/auth/login"))
				.header("User-Agent", USER_AGENT)
				.header("Content-Type", "application/json;charset=UTF-8")
				.header("version", VERSION)
				.header("product", "llu.ios")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();

			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode json = this.objectMapper.readTree(response.body());

			// Handle region redirect
			if (json.has("data") && json.get("data").has("redirect") && json.get("data").get("redirect").asBoolean()) {
				String newRegion = json.get("data").get("region").asText();
				String newHost = regionToHost(newRegion);
				LOG.info("Redirected to region: {} ({})", newRegion, newHost);
				return login(email, password, newHost);
			}

			if (json.has("data") && json.get("data").has("authTicket")) {
				JsonNode ticket = json.get("data").get("authTicket");
				String userId = json.get("data").get("user").get("id").asText();
				return Optional
					.of(new AuthTicket(ticket.get("token").asText(), ticket.get("expires").asLong(), userId, apiHost));
			}

			LOG.warn("LibreLink Up login failed: {}", response.body());
			return Optional.empty();
		}
		catch (Exception ex) {
			LOG.error("LibreLink Up login error", ex);
			return Optional.empty();
		}
	}

	@Override
	public List<Connection> getConnections(AuthTicket ticket, String apiHost) {
		try {
			HttpRequest request = authenticatedRequest(ticket, apiHost, "/llu/connections").GET().build();
			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode json = this.objectMapper.readTree(response.body());
			List<Connection> connections = new ArrayList<>();
			if (json.has("data")) {
				for (JsonNode node : json.get("data")) {
					connections.add(new Connection(node.get("patientId").asText(),
							node.has("firstName") ? node.get("firstName").asText() : "",
							node.has("lastName") ? node.get("lastName").asText() : ""));
				}
			}
			return connections;
		}
		catch (Exception ex) {
			LOG.error("Failed to get LibreLink Up connections", ex);
			return List.of();
		}
	}

	@Override
	public GraphData getGraph(AuthTicket ticket, String apiHost, String patientId) {
		try {
			HttpRequest request = authenticatedRequest(ticket, apiHost, "/llu/connections/" + patientId + "/graph")
				.GET()
				.build();
			HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			JsonNode json = this.objectMapper.readTree(response.body());

			GlucoseItem current = null;
			List<GlucoseItem> graphData = new ArrayList<>();

			if (json.has("data")) {
				JsonNode data = json.get("data");
				// Current measurement from connection
				if (data.has("connection") && data.get("connection").has("glucoseMeasurement")) {
					current = parseGlucoseItem(data.get("connection").get("glucoseMeasurement"));
				}
				// Historical graph data
				if (data.has("graphData")) {
					for (JsonNode node : data.get("graphData")) {
						graphData.add(parseGlucoseItem(node));
					}
				}
			}

			return new GraphData(current, graphData);
		}
		catch (Exception ex) {
			LOG.error("Failed to get LibreLink Up graph data", ex);
			return new GraphData(null, List.of());
		}
	}

	private GlucoseItem parseGlucoseItem(JsonNode node) {
		return new GlucoseItem(node.has("FactoryTimestamp") ? node.get("FactoryTimestamp").asText() : null,
				node.has("Timestamp") ? node.get("Timestamp").asText() : null,
				node.has("ValueInMgPerDl") ? node.get("ValueInMgPerDl").asInt() : node.get("Value").asInt(),
				node.has("TrendArrow") ? node.get("TrendArrow").asInt() : null,
				node.has("isHigh") && node.get("isHigh").asBoolean(),
				node.has("isLow") && node.get("isLow").asBoolean());
	}

	private HttpRequest.Builder authenticatedRequest(AuthTicket ticket, String apiHost, String path) {
		return HttpRequest.newBuilder()
			.uri(URI.create("https://" + apiHost + path))
			.header("User-Agent", USER_AGENT)
			.header("Content-Type", "application/json;charset=UTF-8")
			.header("version", VERSION)
			.header("product", "llu.ios")
			.header("Authorization", "Bearer " + ticket.token())
			.header("account-id", sha256(ticket.userId()));
	}

	private static String regionToHost(String region) {
		return switch (region.toUpperCase()) {
			case "US" -> "api-us.libreview.io";
			case "EU" -> "api-eu.libreview.io";
			case "EU2" -> "api-eu2.libreview.io";
			case "DE" -> "api-de.libreview.io";
			case "FR" -> "api-fr.libreview.io";
			case "CA" -> "api-ca.libreview.io";
			case "AU" -> "api-au.libreview.io";
			case "AE" -> "api-ae.libreview.io";
			case "AP" -> "api-ap.libreview.io";
			case "JP" -> "api-jp.libreview.io";
			case "LA" -> "api-la.libreview.io";
			default -> "api-eu.libreview.io";
		};
	}

	private static String sha256(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder();
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		}
		catch (Exception ex) {
			throw new RuntimeException("SHA-256 not available", ex);
		}
	}

	record LoginRequest(String email, String password) {
	}

}
