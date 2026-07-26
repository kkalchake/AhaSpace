package com.kkalchake.enlightenment.seed;

import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Phase;
import com.kkalchake.enlightenment.model.Section;
import com.kkalchake.enlightenment.repository.PhaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Covers the directory-tree-dependent behavior that CurriculumSeedRunnerTest's
// pure string-based tests don't touch: lesson directory traversal/sorting,
// skipping lessons that lack docs/en.md or have a malformed order prefix, and
// the idempotency guard. The fixture is a small directory tree built fresh in
// @TempDir per test - never the real external curriculum repo.
@ExtendWith(MockitoExtension.class)
class CurriculumPhaseSeederTest {

    @Mock
    private PhaseRepository phaseRepository;

    private CurriculumPhaseSeeder phaseSeeder;
    private Course testCourse;

    @BeforeEach
    void setUp() {
        phaseSeeder = new CurriculumPhaseSeeder(phaseRepository);
        testCourse = new Course();
        testCourse.setId(1L);
        testCourse.setTitle("AI Engineering From Scratch");
    }

    @Test
    void seedPhase_buildsPhaseWithSectionsSortedAndSkipsInvalidLessons(@TempDir Path tempDir) throws IOException {
        Path phaseDir = tempDir.resolve("00-setup-and-tooling");
        Files.createDirectories(phaseDir);
        writeFile(phaseDir.resolve("README.md"),
                "# Phase 0: Setup & Tooling\n\n> Get your environment ready for everything that follows.\n");

        writeLesson(phaseDir, "02-git-and-collaboration", "# Git And Collaboration\n\nBody text.\n");
        writeLesson(phaseDir, "01-dev-environment", "# Dev Environment\n\nBody text.\n");

        // Malformed prefix: no leading digits - must be skipped, not abort the phase.
        Files.createDirectories(phaseDir.resolve("setup-no-prefix").resolve("docs"));
        writeFile(phaseDir.resolve("setup-no-prefix").resolve("docs").resolve("en.md"), "# Should Be Skipped\n");

        // Missing docs/en.md entirely - must be skipped, not abort the phase.
        Files.createDirectories(phaseDir.resolve("03-missing-docs"));

        when(phaseRepository.existsByTitle("Phase 0: Setup & Tooling")).thenReturn(false);

        CurriculumPhaseSeeder.PhaseSummary summary = phaseSeeder.seedPhase(testCourse, phaseDir);

        assertNotNull(summary);
        assertEquals("Phase 0: Setup & Tooling", summary.phaseTitle());
        assertEquals(2, summary.sectionCount());

        ArgumentCaptor<Phase> phaseCaptor = ArgumentCaptor.forClass(Phase.class);
        verify(phaseRepository).save(phaseCaptor.capture());
        Phase savedPhase = phaseCaptor.getValue();

        assertEquals("Phase 0: Setup & Tooling", savedPhase.getTitle());
        assertEquals(0, savedPhase.getOrderIndex());
        assertSame(testCourse, savedPhase.getCourse());
        assertTrue(savedPhase.getDescription().startsWith("Get your environment ready for everything that follows.\n\n"));
        assertTrue(savedPhase.getDescription().contains("Licensed under the MIT License"));

        List<Section> sections = savedPhase.getSections().stream()
                .sorted(Comparator.comparingInt(Section::getOrderIndex))
                .toList();
        assertEquals(2, sections.size());
        assertEquals(1, sections.get(0).getOrderIndex());
        assertEquals("Dev Environment", sections.get(0).getTitle());
        assertEquals(2, sections.get(1).getOrderIndex());
        assertEquals("Git And Collaboration", sections.get(1).getTitle());
    }

    @Test
    void seedPhase_phaseAlreadyExists_skipsAndReturnsNull(@TempDir Path tempDir) throws IOException {
        Path phaseDir = tempDir.resolve("00-setup-and-tooling");
        Files.createDirectories(phaseDir);
        writeFile(phaseDir.resolve("README.md"), "# Phase 0: Setup & Tooling\n\n> Blurb.\n");

        when(phaseRepository.existsByTitle("Phase 0: Setup & Tooling")).thenReturn(true);

        CurriculumPhaseSeeder.PhaseSummary summary = phaseSeeder.seedPhase(testCourse, phaseDir);

        assertNull(summary);
        verify(phaseRepository, org.mockito.Mockito.never()).save(any());
    }

    private void writeLesson(Path phaseDir, String dirName, String enMdContent) throws IOException {
        Path docsDir = phaseDir.resolve(dirName).resolve("docs");
        Files.createDirectories(docsDir);
        writeFile(docsDir.resolve("en.md"), enMdContent);
    }

    private void writeFile(Path path, String content) throws IOException {
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }
}
