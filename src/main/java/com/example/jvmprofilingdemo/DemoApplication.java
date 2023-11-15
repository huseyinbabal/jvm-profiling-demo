package com.example.jvmprofilingdemo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}

record Person(byte[] name) {}

@RestController
class LeakController {

	private final List<Person> leakyStorage = new ArrayList<>();

	@GetMapping("/leak")
	public String leak() {
		for (int i = 0; i < 10000; i++) {
			leakyStorage.add(new Person(new byte[1024]));
		}
		return "Leak Triggered";
	}

	@GetMapping("/leak-count")
	public Integer leakCount() {
		return leakyStorage.size();
	}

	@GetMapping("/io")
	public void io() throws URISyntaxException, IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
			.uri(new URI("https://hub.dummyapis.com/delay?seconds=4"))
			.GET()
			.build();
		HttpClient.newBuilder().build().send(request, HttpResponse.BodyHandlers.ofString());
	}
}
