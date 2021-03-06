package io.swagger.server.api.verticle;

import java.io.File;
import io.swagger.server.api.MainApiException;
import io.swagger.server.api.MainApiHeader;
import io.swagger.server.api.model.ModelApiResponse;
import io.swagger.server.api.model.Pet;
import io.swagger.server.api.util.ResourceResponse;

import rx.Completable;
import rx.Single;
import io.vertx.rxjava.ext.auth.User;

import java.util.List;
import java.util.Map;

public interface PetApi  {
    //addPet
    Single<ResourceResponse<Void>> addPet(Pet body, User user);
    
    //deletePet
    Single<ResourceResponse<Void>> deletePet(Long petId, String apiKey, User user);
    
    //findPetsByStatus
    Single<ResourceResponse<List<Pet>>> findPetsByStatus(List<String> status, User user);
    
    //findPetsByTags
    Single<ResourceResponse<List<Pet>>> findPetsByTags(List<String> tags, User user);
    
    //getPetById
    Single<ResourceResponse<Pet>> getPetById(Long petId, User user);
    
    //updatePet
    Single<ResourceResponse<Void>> updatePet(Pet body, User user);
    
    //updatePetWithForm
    Single<ResourceResponse<Void>> updatePetWithForm(Long petId, String name, String status, User user);
    
    //uploadFile
    Single<ResourceResponse<ModelApiResponse>> uploadFile(Long petId, String additionalMetadata, File file, User user);
    
}
