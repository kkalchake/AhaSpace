package com.kkalchake.enlightenment.seed;

import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Phase;
import com.kkalchake.enlightenment.model.Section;
import com.kkalchake.enlightenment.repository.PhaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Per-phase persistence lives in its own Spring bean rather than as a method on
// CurriculumSeedRunner. Reason: @Transactional only takes effect on calls that
// arrive through the bean's Spring proxy. CurriculumSeedRunner.run() invoking a
// method on itself ("this.seedPhase(...)") would bypass that proxy entirely, so
// the annotation would silently do nothing - one failed phase could leave a
// partially-saved Phase. Calling out to a separate injected bean keeps the
// proxy in the call path, so each phase really does commit or roll back as a unit.
//
// A phase now becomes a Phase row under a given Course (the curriculum), not a
// Course row itself - Course is the whole curriculum ("AI Engineering From
// Scratch"), found-or-created once by CurriculumSeedRunner before this class
// is ever called.
@Slf4j
@Component
@RequiredArgsConstructor
public class CurriculumPhaseSeeder {

    private final PhaseRepository phaseRepository;

    @Transactional
    public PhaseSummary seedPhase(Course course, Path phaseDir) throws IOException {
        String readme = CurriculumSeedRunner.readFile(phaseDir.resolve("README.md"));
        String phaseTitle = CurriculumSeedRunner.parsePhaseTitle(readme);

        // Idempotency guard: same pattern as UserRepositoryTestRunner's
        // findByEmail(...).isEmpty() check - look the row up before creating it,
        // so re-running the seed (e.g. after adding a 4th pilot phase) doesn't
        // duplicate phases that already exist.
        if (phaseRepository.existsByTitle(phaseTitle)) {
            log.info("Skipping phase '{}': phase '{}' already exists.", phaseDir.getFileName(), phaseTitle);
            return null;
        }

        Phase phase = new Phase();
        phase.setTitle(phaseTitle);
        phase.setDescription(CurriculumSeedRunner.buildDescription(CurriculumSeedRunner.parseBlurb(readme)));
        phase.setOrderIndex(CurriculumSeedRunner.parseOrderIndex(phaseDir.getFileName().toString())
                .orElse(0));
        phase.setCourse(course);

        int sectionCount = 0;
        for (Path lessonDir : listLessonDirsSorted(phaseDir)) {
            String dirName = lessonDir.getFileName().toString();
            Path enMd = lessonDir.resolve("docs").resolve("en.md");

            if (!Files.exists(enMd)) {
                log.warn("Skipping lesson '{}' in phase '{}': no docs/en.md found.", dirName, phaseDir.getFileName());
                continue;
            }

            OptionalInt orderIndex = CurriculumSeedRunner.parseOrderIndex(dirName);
            if (orderIndex.isEmpty()) {
                log.warn("Skipping lesson '{}' in phase '{}': directory name has no numeric prefix.",
                        dirName, phaseDir.getFileName());
                continue;
            }

            String content = CurriculumSeedRunner.readFile(enMd);

            Section section = new Section();
            section.setTitle(CurriculumSeedRunner.parseSectionTitle(content, dirName));
            section.setOrderIndex(orderIndex.getAsInt());
            section.setContent(content);
            section.setPhase(phase);
            phase.getSections().add(section);
            sectionCount++;
        }

        // Phase.sections is cascade = ALL, so saving the phase persists every
        // attached Section in the same operation - no separate sectionRepository call needed.
        phaseRepository.save(phase);

        log.info("Seeded phase '{}': {} section(s).", phaseTitle, sectionCount);
        return new PhaseSummary(phaseTitle, sectionCount);
    }

    private List<Path> listLessonDirsSorted(Path phaseDir) throws IOException {
        // README.md is excluded by the isDirectory filter, not by name - it's a
        // file, not a directory, so it never reaches the lesson-processing loop.
        try (Stream<Path> entries = Files.list(phaseDir)) {
            return entries
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .collect(Collectors.toList());
        }
    }

    public record PhaseSummary(String phaseTitle, int sectionCount) {
    }
}
