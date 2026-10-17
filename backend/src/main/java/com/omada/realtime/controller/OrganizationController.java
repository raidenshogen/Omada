package com.omada.realtime.controller;

import com.omada.realtime.dto.request.CreateOrganizationRequest;
import com.omada.realtime.dto.response.OrganizationResponse;
import com.omada.realtime.service.OrganizationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service){ this.service = service; }

    @PostMapping
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest req){
        return service.create(req);
    }

    /** Only organizations the caller belongs to (tenant isolation in the service). */
    @GetMapping
    public List<OrganizationResponse> all(){
        return service.findAll();
    }

    @GetMapping("/{id}")
    public OrganizationResponse get(@PathVariable String id){
        return service.findById(id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.isOrgOwner(#id, authentication.name)")
    public OrganizationResponse update(@PathVariable String id, @Valid @RequestBody CreateOrganizationRequest req){
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@workspaceSecurity.isOrgOwner(#id, authentication.name)")
    public void delete(@PathVariable String id){
        service.delete(id);
    }
}
