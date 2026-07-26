package com.kkalchake.enlightenment.seed;

import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.repository.CourseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// Entry point for the Week 17 curriculum pilot seed. Never active on its own:
// @Profile("seed") means this bean (and the @Value below) is only constructed
// when the "seed" profile is explicitly listed alongside "dev"/"prod", so a
// normal `dev` or `prod` boot - and every test in this repo, none of which
// activate "seed" - never touches this class or requires curriculum.source-path
// to be set.
@Slf4j
@Component
@Profile("seed")
public class CurriculumSeedRunner implements CommandLineRunner {

    // The one curriculum this pilot seeds. Multiple curricula (e.g. "AI Evals
    // From Scratch", "ML From Scratch") are a later follow-up: at that point
    // this becomes a small list of (title, source-path) pairs instead of one
    // constant + one @Value property, matching how PILOT_PHASES below is
    // already written as "small list now, expand later."
    static final String CURRICULUM_TITLE = "AI Engineering From Scratch";

    // Pilot scope: only these 3 of the curriculum's ~20 phases are seeded this
    // week. Expanding to the full phase list is a follow-up, not this week's change.
    static final List<String> PILOT_PHASES = List.of(
            "00-setup-and-tooling", "01-math-foundations", "02-ml-fundamentals");

    static final String ATTRIBUTION =
            "Source: \"AI Engineering from Scratch\" by rohitg00 — "
                    + "https://github.com/rohitg00/ai-engineering-from-scratch / https://aiengineeringfromscratch.com. "
                    + "Licensed under the MIT License (Copyright (c) 2026 Rohit Ghumare).";

    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d+)-");
    private static final Pattern H1_LINE = Pattern.compile("^#\\s+(.*)$");
    private static final Pattern BLURB_LINE = Pattern.compile("^>\\s*(.*)$");

    // Field injection, not constructor injection: this is the only property this
    // class needs, and there's no machine-specific default to commit (matches
    // JwtUtil's @Value field pattern elsewhere in this repo rather than routing
    // a single property through a Lombok-generated constructor).
    @Value("${curriculum.source-path}")
    private String curriculumSourcePath;

    private final CurriculumPhaseSeeder phaseSeeder;
    private final CourseRepository courseRepository;

    public CurriculumSeedRunner(CurriculumPhaseSeeder phaseSeeder, CourseRepository courseRepository) {
        this.phaseSeeder = phaseSeeder;
        this.courseRepository = courseRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        Path root = Path.of(curriculumSourcePath);

        // Find-or-create: the curriculum (Course row) is only ever created once.
        // Every phase seeded below attaches to this same row, whether this is
        // the first run or a later run adding more pilot phases.
        Course course = courseRepository.findByTitle(CURRICULUM_TITLE)
                .orElseGet(() -> {
                    Course c = new Course();
                    c.setTitle(CURRICULUM_TITLE);
                    c.setDescription(ATTRIBUTION);
                    return courseRepository.save(c);
                });

        int seededPhases = 0;
        int seededSections = 0;

        for (String phaseSlug : PILOT_PHASES) {
            Path phaseDir = root.resolve("phases").resolve(phaseSlug);
            CurriculumPhaseSeeder.PhaseSummary summary = phaseSeeder.seedPhase(course, phaseDir);
            if (summary != null) {
                seededPhases++;
                seededSections += summary.sectionCount();
            }
        }

        log.info("Curriculum seed complete: {} phase(s), {} section(s) total.", seededPhases, seededSections);
    }

    // --- Pure parsing helpers -------------------------------------------------
    // Kept static and side-effect-free so CurriculumSeedRunnerTest can exercise
    // them directly against a small fixture tree, with no Spring context and no
    // dependency on the real curriculum repo being checked out.

    /**
     * Parses the leading numeric prefix of a lesson directory name into a 1-based
     * order index (e.g. "01-dev-environment" -> 1, "22-stochastic-processes" -> 22).
     * Returns an empty OptionalInt if the name has no such prefix.
     */
    static OptionalInt parseOrderIndex(String dirName) {
        Matcher m = LEADING_NUMBER.matcher(dirName);
        if (!m.find()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(Integer.parseInt(m.group(1)));
    }

    /**
     * Strips a lesson directory's leading numeric prefix (e.g. "01-dev-environment"
     * -> "dev-environment"). Returns the input unchanged if there is no such prefix.
     */
    static String stripOrderPrefix(String dirName) {
        return LEADING_NUMBER.matcher(dirName).replaceFirst("");
    }

    /**
     * Section.title: the first line of en.md if it's an H1 ("# ..."), stripped of
     * the marker; otherwise the slug (directory name minus numeric prefix)
     * title-cased, e.g. "dev-environment" -> "Dev Environment".
     */
    static String parseSectionTitle(String enMdContent, String dirName) {
        String firstLine = firstLine(enMdContent);
        Matcher m = H1_LINE.matcher(firstLine);
        if (m.matches()) {
            return m.group(1).trim();
        }
        return slugToTitleCase(stripOrderPrefix(dirName));
    }

    static String slugToTitleCase(String slug) {
        String[] words = slug.split("-");
        return java.util.Arrays.stream(words)
                .filter(w -> !w.isEmpty())
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .collect(Collectors.joining(" "));
    }

    /**
     * Course.title: the phase README's first "# " line, minus the marker
     * (e.g. "# Phase 0: Setup & Tooling" -> "Phase 0: Setup & Tooling").
     */
    static String parsePhaseTitle(String readmeContent) {
        String firstLine = firstLine(readmeContent);
        Matcher m = H1_LINE.matcher(firstLine);
        if (!m.matches()) {
            throw new IllegalStateException("Phase README has no '# ' title line");
        }
        return m.group(1).trim();
    }

    /**
     * The phase README's "> " blurb line (the first line matching that prefix),
     * marker stripped. Returns "" if the README has no such line.
     */
    static String parseBlurb(String readmeContent) {
        for (String line : readmeContent.split("\\R")) {
            Matcher m = BLURB_LINE.matcher(line);
            if (m.matches()) {
                return m.group(1).trim();
            }
        }
        return "";
    }

    /**
     * Course.description: the blurb, a blank line, then the fixed attribution
     * string applied to all seeded courses.
     */
    static String buildDescription(String blurb) {
        return blurb + "\n\n" + ATTRIBUTION;
    }

    private static String firstLine(String content) {
        int newlineIndex = content.indexOf('\n');
        String line = newlineIndex == -1 ? content : content.substring(0, newlineIndex);
        // Strip a trailing \r so files with CRLF line endings don't leave it in the title.
        return line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
    }

    static String readFile(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
