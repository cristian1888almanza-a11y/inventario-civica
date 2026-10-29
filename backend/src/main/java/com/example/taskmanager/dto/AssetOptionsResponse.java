package com.example.taskmanager.dto;

import java.util.List;

public record AssetOptionsResponse(
    Long companyId,
    String companyName,
    List<SiteOption> sites,
    List<ResponsibleOption> responsibles,
    List<CategoryOption> categories
) {
    public record SiteOption(Long id, String name, String department, String city, List<AreaOption> areas) {}
    public record AreaOption(Long id, String name) {}
    public record ResponsibleOption(Long id, String fullName) {}
    public record CategoryOption(Long id, String name) {}
}
