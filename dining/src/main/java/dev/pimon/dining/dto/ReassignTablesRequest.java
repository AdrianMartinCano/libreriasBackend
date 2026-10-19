package dev.pimon.dining.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReassignTablesRequest(@NotEmpty List<String> tableIds) {}
