package com.example.jvmprofilingdemo;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.github.krzysztofslusarski.asyncprofiler.ContinuousAsyncProfilerConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Import(ContinuousAsyncProfilerConfiguration.class)
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

	@GetMapping("/cpu")
	public String cpu() {
		int numThreads = 2; // Use a fixed number of threads to control the load
		long workTime = 100; // Time in milliseconds for which the thread will do the work
		long sleepTime = 100; // Time in milliseconds for which the thread will sleep
		for (int i = 0; i < numThreads; i++) {
			new Thread(new ControlledLoadTask(workTime, sleepTime)).start();
		}
		return "CPU intensive tasks Triggered";
	}
}

class ControlledLoadTask implements Runnable {
	private final long workTime;
	private final long sleepTime;

	public ControlledLoadTask(long workTime, long sleepTime) {
		this.workTime = workTime;
		this.sleepTime = sleepTime;
	}

	@Override
	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			long startTime = System.currentTimeMillis();

			// Work phase
			while (System.currentTimeMillis() - startTime < workTime) {
				double value = Math.random() * Math.random();
				value = Math.sqrt(value) * Math.tan(value); // Some CPU-intensive task
			}

			// Sleep phase
			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				System.out.println("Thread interrupted");
			}
		}
	}
}

@Configuration
class MetricsConfiguration {
	@Bean
	MeterRegistryCustomizer<MeterRegistry> configurer(
		@Value("${spring.application.name}") String applicationName) {
		return (registry) -> registry.config().commonTags("application", applicationName);
	}
}
