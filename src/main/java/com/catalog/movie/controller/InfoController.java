package com.catalog.movie.controller;

import com.catalog.movie.config.AppProperties;
import com.catalog.movie.dto.InfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.catalog.movie.utils.ApiMappings.INFO;

@Tag(name = "Info")
@RestController
@RequestMapping(INFO)
public class InfoController {

    private final AppProperties appProperties;

    @Autowired
    public InfoController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Operation(summary = "Application name and version (from app.name / app.version)")
    @GetMapping
    public InfoResponse getInfo() {
        return new InfoResponse(appProperties.name(), appProperties.version());
    }
}
