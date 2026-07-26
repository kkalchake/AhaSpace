package com.kkalchake.enlightenment.seed;

import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

class CurriculumSeedRunnerTest {

    @Test
    void parseOrderIndex_numericPrefix_returnsParsedIndex() {
        assertEquals(1, CurriculumSeedRunner.parseOrderIndex("01-dev-environment").getAsInt());
        assertEquals(22, CurriculumSeedRunner.parseOrderIndex("22-stochastic-processes").getAsInt());
    }

    @Test
    void parseOrderIndex_noNumericPrefix_returnsEmpty() {
        assertEquals(OptionalInt.empty(), CurriculumSeedRunner.parseOrderIndex("dev-environment"));
        assertEquals(OptionalInt.empty(), CurriculumSeedRunner.parseOrderIndex("README.md"));
    }

    @Test
    void stripOrderPrefix_removesLeadingNumberAndDash() {
        assertEquals("dev-environment", CurriculumSeedRunner.stripOrderPrefix("01-dev-environment"));
        assertEquals("stochastic-processes", CurriculumSeedRunner.stripOrderPrefix("22-stochastic-processes"));
    }

    @Test
    void slugToTitleCase_capitalizesEachWord() {
        assertEquals("Dev Environment", CurriculumSeedRunner.slugToTitleCase("dev-environment"));
        assertEquals("Stochastic Processes", CurriculumSeedRunner.slugToTitleCase("stochastic-processes"));
    }

    @Test
    void parseSectionTitle_h1Present_usesH1StrippedOfMarker() {
        String enMd = "# Dev Environment\n\n> Your tools shape your thinking.\n";
        assertEquals("Dev Environment", CurriculumSeedRunner.parseSectionTitle(enMd, "01-dev-environment"));
    }

    @Test
    void parseSectionTitle_noH1_fallsBackToSlugTitleCase() {
        String enMd = "Some content with no heading marker on the first line.\n";
        assertEquals("Dev Environment", CurriculumSeedRunner.parseSectionTitle(enMd, "01-dev-environment"));
    }

    @Test
    void parsePhaseTitle_readsFirstH1LineMinusMarker() {
        String readme = "# Phase 0: Setup & Tooling\n\n> Get your environment ready for everything that follows.\n";
        assertEquals("Phase 0: Setup & Tooling", CurriculumSeedRunner.parsePhaseTitle(readme));
    }

    @Test
    void parsePhaseTitle_noH1Line_throws() {
        String readme = "> A blurb with no heading above it.\n";
        assertThrows(IllegalStateException.class, () -> CurriculumSeedRunner.parsePhaseTitle(readme));
    }

    @Test
    void parseBlurb_readsBlockquoteLine() {
        String readme = "# Phase 0: Setup & Tooling\n\n> Get your environment ready for everything that follows.\n";
        assertEquals("Get your environment ready for everything that follows.",
                CurriculumSeedRunner.parseBlurb(readme));
    }

    @Test
    void parseBlurb_noBlockquoteLine_returnsEmptyString() {
        String readme = "# Phase 0: Setup & Tooling\n\nNo blurb here.\n";
        assertEquals("", CurriculumSeedRunner.parseBlurb(readme));
    }

    @Test
    void buildDescription_containsBlurbBlankLineThenAttribution() {
        String description = CurriculumSeedRunner.buildDescription("Get your environment ready.");

        assertTrue(description.startsWith("Get your environment ready.\n\n"));
        assertTrue(description.contains(
                "Source: \"AI Engineering from Scratch\" by rohitg00 — "
                        + "https://github.com/rohitg00/ai-engineering-from-scratch / https://aiengineeringfromscratch.com. "
                        + "Licensed under the MIT License (Copyright (c) 2026 Rohit Ghumare)."));
    }
}
