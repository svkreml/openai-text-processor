package svkreml.ai.openaitextprocessor.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;
import svkreml.ai.openaitextprocessor.dto.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Configuration
class AIFunctionConfiguration {
    Map<Owner, List<Pet>> petsByOwner = new HashMap<>();

    public AIFunctionConfiguration() {
        this.petsByOwner.put(new Owner("Maria"), new ArrayList<>(List.of(new Pet("Lapka"))));
        this.petsByOwner.put(new Owner("Ivan"), new ArrayList<>(List.of(new Pet("Copka"), new Pet("Mike"))));
    }

    // @Tool почему-то не работает
//     @Tool(name = "listOwners", description = "List the owners that the pet clinic has")
    @Description("List the owners that the pet clinic has")
    @Bean
    public Function<Void, Owners> listOwners() {
        return request -> {
            List<Owner> owners = petsByOwner.keySet().stream().toList();
            log.info("listOwners: {}", owners);
            return new Owners(owners);
        };
    }

    //     @Tool(name = "addNewPet", description = "Add pet to the database")
    @Description("Add Pet to the database, if Owner do not exist it will be created automatically")
    @Bean
    public Function<PetAndHisOwner, PetAndHisOwner> addNewPet() {
        return request -> {
            Pet pet = request.pet();
            Owner owner = request.owner();
            log.info("addNewPet pet: {}, owner: {}", pet, owner);
            petsByOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(pet);
            return new PetAndHisOwner(pet, owner);
        };
    }

    //      @Tool(name = "getPetsByOwner", description = "Get pets by owner")
    @Description("Get pets by owner")
    @Bean
    public Function<Owner, Pets> getPetsByOwner() {
        return request -> {
            List<Pet> pets = petsByOwner.getOrDefault(request, List.of());
            log.info("getPetsByOwner: {}, owner: {}", pets, request);
            return new Pets(pets);
        };
    }

}








