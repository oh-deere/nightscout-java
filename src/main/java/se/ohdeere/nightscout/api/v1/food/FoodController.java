package se.ohdeere.nightscout.api.v1.food;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import se.ohdeere.nightscout.api.auth.AuthHelper;
import se.ohdeere.nightscout.storage.JsonValue;
import se.ohdeere.nightscout.storage.food.Food;
import se.ohdeere.nightscout.storage.food.FoodRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FoodController {

	private final FoodRepository foodRepository;

	FoodController(FoodRepository foodRepository) {
		this.foodRepository = foodRepository;
	}

	@GetMapping({ "/api/v1/food", "/api/v1/food.json" })
	List<FoodDto> getFood(@RequestParam(defaultValue = "100") int count) {
		AuthHelper.requirePermission("food", "read");
		return this.foodRepository.findLatest(Math.min(count, 1000)).stream().map(FoodDto::from).toList();
	}

	@PostMapping({ "/api/v1/food", "/api/v1/food.json" })
	ResponseEntity<FoodDto> postFood(@RequestBody FoodDto dto) {
		AuthHelper.requirePermission("food", "create");
		Food saved = this.foodRepository.save(dto.toFood());
		return ResponseEntity.ok(FoodDto.from(saved));
	}

	@DeleteMapping("/api/v1/food/{id}")
	ResponseEntity<Void> deleteFood(@PathVariable UUID id) {
		AuthHelper.requirePermission("food", "delete");
		this.foodRepository.deleteById(id);
		return ResponseEntity.ok().build();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record FoodDto(@JsonProperty("_id") String id, String name, String category, Double portion, String unit,
			Double carbs, Double fat, Double protein) {

		static FoodDto from(Food f) {
			return new FoodDto(f.id() != null ? f.id().toString() : null, f.name(), f.category(), f.portion(), f.unit(),
					f.carbs(), f.fat(), f.protein());
		}

		Food toFood() {
			return new Food(null, java.time.Instant.now(), this.name, this.category, this.portion, this.unit,
					this.carbs, this.fat, this.protein, JsonValue.empty());
		}

	}

}
