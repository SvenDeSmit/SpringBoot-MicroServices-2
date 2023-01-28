package be.snife.reactive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import reactor.core.publisher.Flux;

class FluxMonoTest {

	@Test
	void testFlux1() {
		List<Integer> list = Flux.just(1,2,3,4)
				.filter(n -> n % 2 == 0)
				.map(n -> n*2)
				.log() // log previous map function
				.collectList() // stream elements -> list
				.block(); // register subscriber 
				
		assertThat(list).containsExactly(4,8);
	}

}
